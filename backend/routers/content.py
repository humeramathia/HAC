from datetime import date

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from deps import CurrentUser, get_current_user, require_admin
from firebase_client import get_db

announcements_router = APIRouter(prefix="/announcements", tags=["announcements"])
notifications_router = APIRouter(prefix="/notifications", tags=["notifications"])
resources_router = APIRouter(prefix="/resources", tags=["resources"])

RESOURCE_CATEGORIES = {"Safety", "Equipment", "Technique", "Scoring", "Getting Started"}


class AnnouncementBody(BaseModel):
    title: str = Field(min_length=1)
    content: str = Field(min_length=1)
    datePosted: str | None = None


class ResourceBody(BaseModel):
    title: str = Field(min_length=1)
    category: str
    description: str = ""
    resourceLink: str = ""


def _announcement(doc_id: str, data: dict) -> dict:
    return {
        "announcementId": doc_id,
        "title": data.get("title", ""),
        "content": data.get("content", ""),
        "datePosted": data.get("datePosted", ""),
        "createdBy": data.get("createdBy", ""),
    }


def _notification(doc_id: str, data: dict) -> dict:
    return {
        "notificationId": doc_id,
        "memberId": data.get("memberId", ""),
        "title": data.get("title", ""),
        "message": data.get("message", ""),
        "dateSent": data.get("dateSent", ""),
        "isRead": bool(data.get("isRead", False)),
    }


def _resource(doc_id: str, data: dict) -> dict:
    return {
        "resourceId": doc_id,
        "title": data.get("title", ""),
        "category": data.get("category", ""),
        "description": data.get("description", ""),
        "resourceLink": data.get("resourceLink", ""),
        "createdBy": data.get("createdBy", ""),
    }


@announcements_router.get("")
def list_announcements(_user: CurrentUser = Depends(get_current_user)):
    items = [_announcement(doc.id, doc.to_dict() or {}) for doc in get_db().collection("announcements").stream()]
    items.sort(key=lambda item: item.get("datePosted", ""), reverse=True)
    return {"announcements": items}


@announcements_router.post("", status_code=201)
def create_announcement(body: AnnouncementBody, user: CurrentUser = Depends(require_admin)):
    payload = {
        "title": body.title.strip(),
        "content": body.content.strip(),
        "datePosted": (body.datePosted or date.today().isoformat()).strip(),
        "createdBy": user.uid,
    }
    ref = get_db().collection("announcements").document()
    ref.set(payload)
    return _announcement(ref.id, payload)


@announcements_router.put("/{announcement_id}")
def update_announcement(announcement_id: str, body: AnnouncementBody, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("announcements").document(announcement_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Announcement not found")
    payload = {
        "title": body.title.strip(),
        "content": body.content.strip(),
        "datePosted": (body.datePosted or date.today().isoformat()).strip(),
    }
    ref.update(payload)
    return _announcement(announcement_id, {**(ref.get().to_dict() or {}), **payload})


@announcements_router.delete("/{announcement_id}")
def delete_announcement(announcement_id: str, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("announcements").document(announcement_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Announcement not found")
    ref.delete()
    return {"ok": True}


@notifications_router.get("")
def list_notifications(user: CurrentUser = Depends(get_current_user)):
    items = []
    for doc in get_db().collection("notifications").stream():
        data = doc.to_dict() or {}
        if data.get("memberId") != user.uid:
            continue
        items.append(_notification(doc.id, data))
    items.sort(key=lambda item: item.get("dateSent", ""), reverse=True)
    return {"notifications": items}


@notifications_router.put("/{notification_id}/read")
def mark_read(notification_id: str, user: CurrentUser = Depends(get_current_user)):
    ref = get_db().collection("notifications").document(notification_id)
    snap = ref.get()
    if not snap.exists:
        raise HTTPException(status_code=404, detail="Notification not found")
    data = snap.to_dict() or {}
    if data.get("memberId") != user.uid and not user.is_admin:
        raise HTTPException(status_code=403, detail="Not allowed to update this notification")
    ref.update({"isRead": True})
    return _notification(notification_id, {**data, "isRead": True})


@resources_router.get("")
def list_resources(_user: CurrentUser = Depends(get_current_user)):
    items = [_resource(doc.id, doc.to_dict() or {}) for doc in get_db().collection("resources").stream()]
    items.sort(key=lambda item: item.get("title", ""))
    return {"resources": items}


@resources_router.post("", status_code=201)
def create_resource(body: ResourceBody, user: CurrentUser = Depends(require_admin)):
    if body.category not in RESOURCE_CATEGORIES:
        raise HTTPException(status_code=400, detail="Invalid resource category")
    payload = {
        "title": body.title.strip(),
        "category": body.category,
        "description": body.description.strip(),
        "resourceLink": body.resourceLink.strip(),
        "createdBy": user.uid,
    }
    ref = get_db().collection("resources").document()
    ref.set(payload)
    return _resource(ref.id, payload)


@resources_router.put("/{resource_id}")
def update_resource(resource_id: str, body: ResourceBody, _user: CurrentUser = Depends(require_admin)):
    if body.category not in RESOURCE_CATEGORIES:
        raise HTTPException(status_code=400, detail="Invalid resource category")
    ref = get_db().collection("resources").document(resource_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Resource not found")
    payload = {
        "title": body.title.strip(),
        "category": body.category,
        "description": body.description.strip(),
        "resourceLink": body.resourceLink.strip(),
    }
    ref.update(payload)
    return _resource(resource_id, {**(ref.get().to_dict() or {}), **payload})


@resources_router.delete("/{resource_id}")
def delete_resource(resource_id: str, _user: CurrentUser = Depends(require_admin)):
    ref = get_db().collection("resources").document(resource_id)
    if not ref.get().exists:
        raise HTTPException(status_code=404, detail="Resource not found")
    ref.delete()
    return {"ok": True}
