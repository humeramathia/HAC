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
    return {"memberId": uid, "email": member["email"]}


@router.post("/login")
def login(body: LoginBody):
    api_key = os.getenv("FIREBASE_WEB_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="FIREBASE_WEB_API_KEY is missing. Copy backend/.env.example to backend/.env",
        )
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={api_key}"
    response = httpx.post(
        url,
        json={"email": str(body.email), "password": body.password, "returnSecureToken": True},
        timeout=20,
    )
    payload = response.json()
    if response.status_code != 200:
        raise HTTPException(status_code=401, detail="Invalid email or password")

    uid = payload.get("localId")
    token = payload.get("idToken")
    if not uid or not token:
        raise HTTPException(status_code=401, detail="Invalid email or password")

    snap = get_db().collection("members").document(uid).get()
    if not snap.exists:
        raise HTTPException(status_code=401, detail="Member record not found")
    member = member_payload(uid, snap.to_dict())
    return {"token": token, "role": member["role"], "member": member}


@router.post("/logout")
def logout(_user: CurrentUser = Depends(get_current_user)):
    return {"ok": True}
