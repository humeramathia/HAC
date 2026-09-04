from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from deps import CurrentUser, get_current_user, require_admin
from firebase_client import get_db

router = APIRouter(prefix="/competitions", tags=["competitions"])
STATUSES = {"UPCOMING", "REGISTRATION OPEN", "CLOSED"}


class CompetitionBody(BaseModel):
    competitionName: str = Field(min_length=1)
    competitionDate: str = Field(min_length=1)
    registrationDeadline: str = Field(min_length=1)
    venue: str = Field(min_length=1)
    description: str = ""
    status: str = "UPCOMING"


def _payload(doc_id: str, data: dict) -> dict:
    return {
        "competitionId": doc_id,
        "competitionName": data.get("competitionName", ""),
        "competitionDate": data.get("competitionDate", ""),
        "registrationDeadline": data.get("registrationDeadline", ""),
        "venue": data.get("venue", ""),
        "description": data.get("description", ""),
        "status": data.get("status", "UPCOMING"),
        "createdBy": data.get("createdBy", ""),
    }


def _validate_status(status: str) -> str:
    if status not in STATUSES:
        raise HTTPException(status_code=400, detail="status must be UPCOMING, REGISTRATION OPEN, or CLOSED")
    return status


@router.get("")
def list_competitions(_user: CurrentUser = Depends(get_current_user)):
    items = [_payload(doc.id, doc.to_dict() or {}) for doc in get_db().collection("competitions").stream()]
    items.sort(key=lambda item: item.get("competitionDate", ""))
    return {"competitions": items}


@router.post("", status_code=201)
def create_competition(body: CompetitionBody, user: CurrentUser = Depends(require_admin)):
    payload = {
        "competitionName": body.competitionName.strip(),
        "competitionDate": body.competitionDate.strip(),
        "registrationDeadline": body.registrationDeadline.strip(),
        "venue": body.venue.strip(),
        "description": body.description.strip(),
        "status": _validate_status(body.status),
        "createdBy": user.uid,
    }
    ref = get_db().collection("competitions").document()
    ref.set(payload)
    return _payload(ref.id, payload)


@router.put("/{competition_id}")
def update_competition(competition_id: str, body: CompetitionBody, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("competitions").document(competition_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Competition not found")
    payload = {
        "competitionName": body.competitionName.strip(),
        "competitionDate": body.competitionDate.strip(),
        "registrationDeadline": body.registrationDeadline.strip(),
        "venue": body.venue.strip(),
        "description": body.description.strip(),
        "status": _validate_status(body.status),
    }
    ref.update(payload)
    return _payload(competition_id, {**(ref.get().to_dict() or {}), **payload})


@router.delete("/{competition_id}")
def delete_competition(competition_id: str, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("competitions").document(competition_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Competition not found")
    ref.delete()
    return {"ok": True}
