# ========================================
# START OF CODE
# ========================================

"""Signed-in member identity (`members`) and archery profile (`memberProfiles`).

Name/email live on the members document because login and admin lists need
them. Bow type, division, and emergency contact are a separate profile
document so scoring and calendar queries do not carry those fields.
"""

from fastapi import APIRouter, Depends, HTTPException
from firebase_admin import auth as fb_auth
from pydantic import BaseModel

from deps import CurrentUser, get_current_user, member_payload, profile_payload
from firebase_client import get_db

router = APIRouter(tags=["profile"])


class ProfileUpdateBody(BaseModel):
    firstName: str
    lastName: str
    email: str
    experienceLevel: str = ""
    bowType: str = ""
    division: str = ""
    emergencyContact: str = ""


@router.get("/me")
def get_me(user: CurrentUser = Depends(get_current_user)):
    """Return the members row already loaded during token verification."""
    return user.member


@router.get("/me/profile")
def get_profile(user: CurrentUser = Depends(get_current_user)):
    """Return archery fields; empty defaults if the profile doc is missing."""
    snap = get_db().collection("memberProfiles").document(user.uid).get()
    return profile_payload(user.uid, snap.to_dict() if snap.exists else {})


@router.put("/me/profile")
def update_profile(body: ProfileUpdateBody, user: CurrentUser = Depends(get_current_user)):
    """Write both documents. If email changed, update Auth so login stays in sync."""
    db = get_db()
    db.collection("members").document(user.uid).update(
        {
            "firstName": body.firstName.strip(),
            "lastName": body.lastName.strip(),
            "email": str(body.email),
        }
    )
    db.collection("memberProfiles").document(user.uid).set(
        {
            "memberId": user.uid,
            "experienceLevel": body.experienceLevel.strip(),
            "bowType": body.bowType.strip(),
            "division": body.division.strip(),
            "emergencyContact": body.emergencyContact.strip(),
        },
        merge=True,
    )
    if str(body.email) != user.member.get("email"):
        try:
            fb_auth.update_user(user.uid, email=str(body.email))
        except Exception:
            raise HTTPException(status_code=400, detail="Could not update login email")
    member_snap = db.collection("members").document(user.uid).get()
    profile_snap = db.collection("memberProfiles").document(user.uid).get()
    return {
        "member": member_payload(user.uid, member_snap.to_dict()),
        "profile": profile_payload(user.uid, profile_snap.to_dict()),
    }

# ========================================
# END OF CODE
# ========================================
