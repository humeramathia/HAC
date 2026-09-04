from __future__ import annotations

from typing import Annotated, Any

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth as fb_auth

from firebase_client import get_db

bearer_scheme = HTTPBearer(auto_error=False)


class CurrentUser:
    def __init__(self, uid: str, role: str, member: dict[str, Any]):
        self.uid = uid
        self.role = role
        self.member = member

    @property
    def is_admin(self) -> bool:
        return self.role == "Admin"


def member_payload(uid: str, data: dict[str, Any] | None) -> dict[str, Any]:
    data = data or {}
    return {
        "memberId": uid,
        "firstName": data.get("firstName", ""),
        "lastName": data.get("lastName", ""),
        "email": data.get("email", ""),
        "role": data.get("role", "Member"),
        "emailVerified": bool(data.get("emailVerified", False)),
        "dateJoined": data.get("dateJoined", ""),
    }


def profile_payload(uid: str, data: dict[str, Any] | None) -> dict[str, Any]:
    data = data or {}
    return {
        "profileId": uid,
        "memberId": data.get("memberId", uid),
        "experienceLevel": data.get("experienceLevel", ""),
        "bowType": data.get("bowType", ""),
        "division": data.get("division", ""),
        "emergencyContact": data.get("emergencyContact", ""),
    }


def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer_scheme)] = None,
) -> CurrentUser:
    if credentials is None or not credentials.credentials:
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")
    token = credentials.credentials.strip()
    if not token:
        raise HTTPException(status_code=401, detail="Missing token")
    try:
        decoded = fb_auth.verify_id_token(token)
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    uid = decoded.get("uid")
    if not uid:
        raise HTTPException(status_code=401, detail="Invalid token")
    snap = get_db().collection("members").document(uid).get()
    if not snap.exists:
        raise HTTPException(status_code=401, detail="Member record not found")
    data = snap.to_dict() or {}
    return CurrentUser(uid=uid, role=data.get("role", "Member"), member=member_payload(uid, data))


def require_admin(user: Annotated[CurrentUser, Depends(get_current_user)]) -> CurrentUser:
    if not user.is_admin:
        raise HTTPException(status_code=403, detail="Admin only")
    return user
