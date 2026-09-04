from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field

from deps import CurrentUser, get_current_user
from firebase_client import get_db
from scoring import calculate_session, serialize_session, session_progress

router = APIRouter(prefix="/score-sessions", tags=["scores"])


class ArrowBody(BaseModel):
    value: int
    isX: bool = False


class EndBody(BaseModel):
    endNumber: int | None = None
    arrows: list[ArrowBody]


class ScoreSessionBody(BaseModel):
    type: str
    title: str = ""
    distanceMeters: int
    arrowsPerEnd: int
    numberOfEnds: int
    date: str
    notes: str = ""
    ranking: int | None = None
    fieldSize: int | None = None
    leagueName: str | None = None
    ends: list[EndBody] = Field(min_length=1)


def _load_own_or_admin(session_id: str, user: CurrentUser) -> tuple[str, dict[str, Any]]:
    snap = get_db().collection("scoreSessions").document(session_id).get()
    if not snap.exists:
        raise HTTPException(status_code=404, detail="Score session not found")
    data = snap.to_dict() or {}
    if data.get("memberId") != user.uid and not user.is_admin:
        raise HTTPException(status_code=403, detail="Not allowed to view this session")
    return snap.id, data


def _sessions_for(member_id: str, session_type: str) -> list[dict]:
    items = []
    for doc in get_db().collection("scoreSessions").stream():
        data = doc.to_dict() or {}
        if data.get("memberId") != member_id:
            continue
        if session_type and data.get("type") != session_type:
            continue
        items.append(serialize_session(doc.id, data))
    items.sort(key=lambda item: item.get("date", ""), reverse=True)
    return items


@router.post("", status_code=201)
def create_session(body: ScoreSessionBody, user: CurrentUser = Depends(get_current_user)):
    calculated = calculate_session(body.model_dump())
    calculated["memberId"] = user.uid
    ref = get_db().collection("scoreSessions").document()
    ref.set(calculated)
    return serialize_session(ref.id, calculated)


@router.get("")
def list_sessions(
    type: str = Query(..., alias="type"),
    user: CurrentUser = Depends(get_current_user),
):
    session_type = type.upper()
    if session_type not in ("PRACTICE", "LEAGUE"):
        raise HTTPException(status_code=400, detail="type must be PRACTICE or LEAGUE")
    return {"sessions": _sessions_for(user.uid, session_type)}


@router.get("/{session_id}/progress")
def get_progress(session_id: str, user: CurrentUser = Depends(get_current_user)):
    doc_id, data = _load_own_or_admin(session_id, user)
    return session_progress(doc_id, data)


@router.get("/{session_id}")
def get_session(session_id: str, user: CurrentUser = Depends(get_current_user)):
    doc_id, data = _load_own_or_admin(session_id, user)
    return serialize_session(doc_id, data)
