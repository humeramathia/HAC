"""Firebase Admin + Firestore. Local file or FIREBASE_SERVICE_ACCOUNT env var."""

from __future__ import annotations

import json
import os
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

_BACKEND_DIR = Path(__file__).resolve().parent
_PROJECT_DIR = _BACKEND_DIR.parent
_SERVICE_ACCOUNT_CANDIDATES = [
    _PROJECT_DIR / "firebase" / "serviceAccount.json",
    _PROJECT_DIR / "firebase" / "serviceAccount.json.json",
]

db = None


def _credential() -> credentials.Base:
    raw = os.getenv("FIREBASE_SERVICE_ACCOUNT", "").strip()
    if raw:
        return credentials.Certificate(json.loads(raw))
    for path in _SERVICE_ACCOUNT_CANDIDATES:
        if path.exists():
            return credentials.Certificate(str(path))
    raise RuntimeError(
        "Missing Firebase credentials. Put firebase/serviceAccount.json in the repo "
        "or set FIREBASE_SERVICE_ACCOUNT to the JSON string."
    )


def init_firebase():
    global db
    if firebase_admin._apps:
        db = firestore.client()
        return db
    firebase_admin.initialize_app(_credential())
    db = firestore.client()
    return db


def get_db():
    if db is None:
        init_firebase()
    return db
