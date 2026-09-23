# ========================================
# START OF CODE
# ========================================

"""Firebase Admin + Firestore initialisation.

Accepts credentials as inline JSON (Render env var) or as a file path
(local `firebase/serviceAccount.json`, or a Render secret file). JSON is
detected by a leading `{` so a pasted service-account object is never
treated as a filename.
"""

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
    # Browsers sometimes save the download as serviceAccount.json.json.
    _PROJECT_DIR / "firebase" / "serviceAccount.json.json",
    Path("/etc/secrets/serviceAccount.json"),
]

db = None


def _clean_env(raw: str) -> str:
    """Strip BOM/quotes that hosting dashboards wrap around pasted JSON."""
    cleaned = raw.strip().lstrip("\ufeff")
    if len(cleaned) >= 2 and cleaned[0] == cleaned[-1] and cleaned[0] in {"'", '"'}:
        inner = cleaned[1:-1].strip()
        if inner.startswith("{") or inner.startswith("/"):
            cleaned = inner
    return cleaned


def _repair_account(data: dict) -> dict:
    """Fix Render-mangled URLs: `https:/` (one slash) is not a valid issuer/client URL.

    Some env-var UIs collapse `https://` to `https:/`. Firebase then rejects
    the certificate. Only rewrite values that are already broken so a correct
    `https://` token_uri / auth_uri is left alone.
    """
    for key, value in list(data.items()):
        if isinstance(value, str) and value.startswith("https:/") and not value.startswith("https://"):
            data[key] = "https://" + value[len("https:/") :]
    return data


def _account_from_text(raw: str) -> dict | None:
    """Parse FIREBASE_SERVICE_ACCOUNT as JSON, or as a path only when it is not JSON.

    A leading `{` means inline JSON — never open that string as a file.
    Short non-JSON values may be a path to the downloaded key file.
    """
    cleaned = _clean_env(raw)
    if not cleaned:
        return None
    if not cleaned.startswith("{") and len(cleaned) < 512:
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
    return _repair_account(data)


def _credential() -> credentials.Base:
    """Prefer the env-var JSON, then fall back to known file locations."""
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
    """Create the Admin app once; reuse it if uvicorn reloads the module."""
    global db
    if firebase_admin._apps:
        db = firestore.client()
        return db
    firebase_admin.initialize_app(_credential())
    db = firestore.client()
    return db


def get_db():
    """Lazy-init helper so routers can import `get_db` before startup finishes."""
    if db is None:
        init_firebase()
    return db

# ========================================
# END OF CODE
# ========================================
