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
    Path(os.getenv("FIREBASE_SERVICE_ACCOUNT_FILE", "").strip())
    if os.getenv("FIREBASE_SERVICE_ACCOUNT_FILE", "").strip()
    else None,
    _PROJECT_DIR / "firebase" / "serviceAccount.json",
    _PROJECT_DIR / "firebase" / "serviceAccount.json.json",
    Path("/etc/secrets/serviceAccount.json"),
]

db = None


def _clean_env(raw: str) -> str:
    cleaned = raw.strip().lstrip("\ufeff")
    if len(cleaned) >= 2 and cleaned[0] == cleaned[-1] and cleaned[0] in {"'", '"'}:
        inner = cleaned[1:-1].strip()
        if inner.startswith("{") or inner.startswith("/"):
            cleaned = inner
    return cleaned


def _account_from_text(raw: str) -> dict | None:
    cleaned = _clean_env(raw)
    if not cleaned:
        return None
    path = Path(cleaned)
    if path.is_file():
        cleaned = path.read_text(encoding="utf-8").lstrip("\ufeff")
    try:
        data = json.loads(cleaned)
    except json.JSONDecodeError as error:
        preview = cleaned[:12].replace("\n", " ")
        raise RuntimeError(
            "FIREBASE_SERVICE_ACCOUNT is not valid JSON. In Render, paste the full "
            "service account file as one line starting with { and ending with }. "
            f"Value starts with {preview!r}. ({error})"
        ) from error
    if not isinstance(data, dict) or data.get("type") != "service_account":
        raise RuntimeError(
            "FIREBASE_SERVICE_ACCOUNT must be the service account JSON object "
            "(it should include \"type\": \"service_account\")."
        )
    return data


def _credential() -> credentials.Base:
    raw = os.getenv("FIREBASE_SERVICE_ACCOUNT", "")
    parsed = _account_from_text(raw) if raw.strip() else None
    if parsed:
        return credentials.Certificate(parsed)
    for path in _SERVICE_ACCOUNT_CANDIDATES:
        if path is not None and path.is_file():
            return credentials.Certificate(str(path))
    raise RuntimeError(
        "Missing Firebase credentials. On Render set FIREBASE_SERVICE_ACCOUNT to "
        "the service account JSON as one line, or upload it as a secret file."
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
