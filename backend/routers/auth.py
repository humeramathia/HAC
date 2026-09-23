from datetime import date
import os

import httpx
from fastapi import APIRouter, Depends, HTTPException
from firebase_admin import auth as fb_auth
from pydantic import BaseModel, Field

from deps import CurrentUser, get_current_user, member_payload
from firebase_client import get_db

router = APIRouter(prefix="/auth", tags=["auth"])


class RegisterBody(BaseModel):
    firstName: str = Field(min_length=1)
    lastName: str = Field(min_length=1)
    email: str = Field(min_length=3)
    password: str = Field(min_length=6)


class LoginBody(BaseModel):
    email: str = Field(min_length=3)
    password: str = Field(min_length=1)


def _web_api_key() -> str:
    api_key = os.getenv("FIREBASE_WEB_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="FIREBASE_WEB_API_KEY is missing. Copy backend/.env.example to backend/.env",
        )
    return api_key


def _sign_in(email: str, password: str) -> dict:
    response = httpx.post(
        f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={_web_api_key()}",
        json={"email": email, "password": password, "returnSecureToken": True},
        timeout=20,
    )
    payload = response.json()
    if response.status_code != 200 or not payload.get("localId") or not payload.get("idToken"):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    return payload


def _send_verification_email(id_token: str) -> None:
    response = httpx.post(
        f"https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key={_web_api_key()}",
        json={"requestType": "VERIFY_EMAIL", "idToken": id_token},
        timeout=20,
    )
    if response.status_code != 200:
        payload = response.json() if response.headers.get("content-type", "").startswith("application/json") else {}
        message = (
            payload.get("error", {}).get("message")
            if isinstance(payload, dict)
            else None
        )
        raise HTTPException(
            status_code=400,
            detail=message or "Could not send verification email",
        )


def _login_payload(uid: str, token: str) -> dict:
    snap = get_db().collection("members").document(uid).get()
    if not snap.exists:
        raise HTTPException(status_code=401, detail="Member record not found")
    member = member_payload(uid, snap.to_dict())
    return {"token": token, "role": member["role"], "member": member}


def _sync_email_verified(uid: str, auth_verified: bool, stored: dict) -> dict:
    if auth_verified and not stored.get("emailVerified"):
        get_db().collection("members").document(uid).update({"emailVerified": True})
        stored = {**stored, "emailVerified": True}
    return stored


@router.post("/register", status_code=201)
def register(body: RegisterBody):
    try:
        user = fb_auth.create_user(email=body.email, password=body.password)
    except Exception as exc:
        message = str(exc)
        if "EMAIL_EXISTS" in message or "already exists" in message.lower():
            raise HTTPException(status_code=400, detail="Email already registered")
        raise HTTPException(status_code=400, detail="Could not create account")

    uid = user.uid
    member = {
        "firstName": body.firstName.strip(),
        "lastName": body.lastName.strip(),
        "email": str(body.email),
        "role": "Member",
        "emailVerified": False,
        "dateJoined": date.today().isoformat(),
    }
    profile = {
        "memberId": uid,
        "experienceLevel": "",
        "bowType": "",
        "division": "",
        "emergencyContact": "",
    }
    db = get_db()
    db.collection("members").document(uid).set(member)
    db.collection("memberProfiles").document(uid).set(profile)
    signed_in = _sign_in(str(body.email), body.password)
    _send_verification_email(signed_in["idToken"])
    return {"memberId": uid, "email": member["email"], "emailSent": True}


@router.post("/resend-verification")
def resend_verification(body: LoginBody):
    signed_in = _sign_in(str(body.email), body.password)
    try:
        auth_user = fb_auth.get_user(signed_in["localId"])
    except Exception:
        raise HTTPException(status_code=401, detail="Member record not found")
    if auth_user.email_verified:
        get_db().collection("members").document(signed_in["localId"]).update({"emailVerified": True})
        return {"ok": True, "alreadyVerified": True}
    _send_verification_email(signed_in["idToken"])
    return {"ok": True, "alreadyVerified": False}


@router.post("/confirm-verification")
def confirm_verification(body: LoginBody):
    signed_in = _sign_in(str(body.email), body.password)
    uid = signed_in["localId"]
    try:
        auth_user = fb_auth.get_user(uid)
    except Exception:
        raise HTTPException(status_code=401, detail="Member record not found")
    if not auth_user.email_verified:
        raise HTTPException(status_code=401, detail="Please tap the verification link in your email first")
    snap = get_db().collection("members").document(uid).get()
    if not snap.exists:
        raise HTTPException(status_code=401, detail="Member record not found")
    _sync_email_verified(uid, True, snap.to_dict() or {})
    return _login_payload(uid, signed_in["idToken"])


@router.post("/login")
def login(body: LoginBody):
    signed_in = _sign_in(str(body.email), body.password)
    uid = signed_in["localId"]
    token = signed_in["idToken"]
    snap = get_db().collection("members").document(uid).get()
    if not snap.exists:
        raise HTTPException(status_code=401, detail="Member record not found")
    stored = snap.to_dict() or {}
    try:
        auth_verified = bool(fb_auth.get_user(uid).email_verified)
    except Exception:
        auth_verified = False
    if not auth_verified and not stored.get("emailVerified"):
        raise HTTPException(status_code=401, detail="Please verify your email first")
    _sync_email_verified(uid, auth_verified, stored)
    return _login_payload(uid, token)


@router.post("/logout")
def logout(_user: CurrentUser = Depends(get_current_user)):
    return {"ok": True}
