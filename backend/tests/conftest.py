# ========================================
# START OF CODE
# ========================================

"""Shared pytest fixtures. HABIBIA_TESTING must be set before main.py imports."""

from __future__ import annotations

import os

import pytest

os.environ["HABIBIA_TESTING"] = "1"

from deps import CurrentUser, member_payload
from firebase_client import get_db
from tests.fakes import FakeDb


def _user(uid: str, role: str) -> CurrentUser:
    member = member_payload(
        uid,
        {
            "firstName": "Test",
            "lastName": role,
            "email": f"{uid}@habibia.co.za",
            "role": role,
            "emailVerified": True,
            "dateJoined": "2026-01-01",
        },
    )
    return CurrentUser(uid=uid, role=role, member=member)


@pytest.fixture
def fake_db():
    return FakeDb()


@pytest.fixture
def member_user() -> CurrentUser:
    return _user("member-1", "Member")


@pytest.fixture
def admin_user() -> CurrentUser:
    return _user("admin-1", "Admin")


@pytest.fixture
def api_client(fake_db, monkeypatch):
    """FastAPI test client with Firestore replaced by FakeDb."""
    from fastapi.testclient import TestClient

    from main import app

    monkeypatch.setattr("firebase_client.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.events.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.competitions.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.content.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.scores.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.admin.get_db", lambda: fake_db)
    monkeypatch.setattr("routers.profile.get_db", lambda: fake_db)
    monkeypatch.setattr("deps.get_db", lambda: fake_db)
    get_db  # keep import used so a later refactor is obvious

    with TestClient(app) as client:
        yield client
    app.dependency_overrides.clear()

# ========================================
# END OF CODE
# ========================================
