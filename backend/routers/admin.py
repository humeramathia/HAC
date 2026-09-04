from fastapi import APIRouter, Depends, HTTPException, Query
from firebase_admin import auth as fb_auth

from deps import CurrentUser, member_payload, require_admin
from firebase_client import get_db
from scoring import member_type_progress, serialize_session

router = APIRouter(prefix="/admin", tags=["admin"])


def _require_type(session_type: str) -> str:
    value = session_type.upper()
    if value not in ("PRACTICE", "LEAGUE"):
        raise HTTPException(status_code=400, detail="type must be PRACTICE or LEAGUE")
    return value


def _sessions_for(member_id: str, session_type: str) -> list[dict]:
    items = []
    for doc in get_db().collection("scoreSessions").stream():
        data = doc.to_dict() or {}
        if data.get("memberId") != member_id or data.get("type") != session_type:
            continue
        items.append(serialize_session(doc.id, data))
    items.sort(key=lambda item: item.get("date", ""), reverse=True)
    return items


@router.get("/members")
def list_members(_user: CurrentUser = Depends(require_admin)):
    members = [
        member_payload(doc.id, doc.to_dict())
        for doc in get_db().collection("members").stream()
    ]
    members.sort(key=lambda item: (item.get("lastName", ""), item.get("firstName", "")))
    return {"members": members}


@router.get("/members/{member_id}")
def get_member(member_id: str, _user: CurrentUser = Depends(require_admin)):
    snap = get_db().collection("members").document(member_id).get()
    if not snap.exists:
        raise HTTPException(status_code=404, detail="Member not found")
    return member_payload(member_id, snap.to_dict())


@router.get("/members/{member_id}/score-sessions")
def member_sessions(
    member_id: str,
    type: str = Query(..., alias="type"),
    _user: CurrentUser = Depends(require_admin),
):
    session_type = _require_type(type)
    member = get_db().collection("members").document(member_id).get()
    if not member.exists:
        raise HTTPException(status_code=404, detail="Member not found")
    return {"sessions": _sessions_for(member_id, session_type)}


@router.get("/members/{member_id}/progress")
def member_progress(
    member_id: str,
    type: str = Query(..., alias="type"),
    _user: CurrentUser = Depends(require_admin),
):
    session_type = _require_type(type)
    member = get_db().collection("members").document(member_id).get()
    if not member.exists:
        raise HTTPException(status_code=404, detail="Member not found")
    sessions = _sessions_for(member_id, session_type)
    return member_type_progress(member_id, session_type, sessions)


@router.delete("/members/{member_id}")
def delete_member(member_id: str, user: CurrentUser = Depends(require_admin)):
    if member_id == user.uid:
        raise HTTPException(status_code=400, detail="You cannot delete your own admin account")
    db = get_db()
    snap = db.collection("members").document(member_id).get()
    if not snap.exists:
        raise HTTPException(status_code=404, detail="Member not found")
    for collection in ("scoreSessions", "notifications"):
        for doc in db.collection(collection).stream():
            data = doc.to_dict() or {}
            if data.get("memberId") == member_id:
                doc.reference.delete()
    db.collection("memberProfiles").document(member_id).delete()
    db.collection("members").document(member_id).delete()
    try:
        fb_auth.delete_user(member_id)
    except Exception:
        pass
    return {"ok": True}
