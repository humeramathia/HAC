from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from deps import CurrentUser, get_current_user, require_admin
from firebase_client import get_db

router = APIRouter(prefix="/events", tags=["events"])
EVENT_TYPES = {"Practice", "Event"}


class EventBody(BaseModel):
    title: str = Field(min_length=1)
    description: str = ""
    eventDate: str = Field(min_length=1)
    eventTime: str = Field(min_length=1)
    location: str = Field(min_length=1)
    type: str = "Event"


def _event_payload(doc_id: str, data: dict) -> dict:
    return {
        "eventId": doc_id,
        "title": data.get("title", ""),
        "description": data.get("description", ""),
        "eventDate": data.get("eventDate", ""),
        "eventTime": data.get("eventTime", ""),
        "location": data.get("location", ""),
        "type": data.get("type", "Event"),
        "createdBy": data.get("createdBy", ""),
    }


def _validate_type(event_type: str) -> str:
    if event_type not in EVENT_TYPES:
        raise HTTPException(status_code=400, detail="type must be Practice or Event")
    return event_type


@router.get("")
def list_events(_user: CurrentUser = Depends(get_current_user)):
    items = [_event_payload(doc.id, doc.to_dict() or {}) for doc in get_db().collection("events").stream()]
    items.sort(key=lambda item: (item.get("eventDate", ""), item.get("eventTime", "")))
    return {"events": items}


@router.post("", status_code=201)
def create_event(body: EventBody, user: CurrentUser = Depends(require_admin)):
    payload = {
        "title": body.title.strip(),
        "description": body.description.strip(),
        "eventDate": body.eventDate.strip(),
        "eventTime": body.eventTime.strip(),
        "location": body.location.strip(),
        "type": _validate_type(body.type),
        "createdBy": user.uid,
    }
    ref = get_db().collection("events").document()
    ref.set(payload)
    return _event_payload(ref.id, payload)


@router.put("/{event_id}")
def update_event(event_id: str, body: EventBody, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("events").document(event_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Event not found")
    payload = {
        "title": body.title.strip(),
        "description": body.description.strip(),
        "eventDate": body.eventDate.strip(),
        "eventTime": body.eventTime.strip(),
        "location": body.location.strip(),
        "type": _validate_type(body.type),
    }
    ref.update(payload)
    return _event_payload(event_id, {**(ref.get().to_dict() or {}), **payload})


@router.delete("/{event_id}")
def delete_event(event_id: str, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("events").document(event_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Event not found")
    ref.delete()
    return {"ok": True}
