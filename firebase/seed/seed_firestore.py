"""Upload firebase/seed/*.json into the live Firestore project."""

from __future__ import annotations

import json
from pathlib import Path

import firebase_admin
from firebase_admin import credentials, firestore

SEED_DIR = Path(__file__).resolve().parent
FIREBASE_DIR = SEED_DIR.parent
UIDS_PATH = SEED_DIR / "uids.local.json"
SERVICE_ACCOUNT_CANDIDATES = [
    FIREBASE_DIR / "serviceAccount.json",
    FIREBASE_DIR / "serviceAccount.json.json",
]

COLLECTIONS = [
    "members",
    "memberProfiles",
    "events",
    "competitions",
    "scoreSessions",
    "announcements",
    "notifications",
    "resources",
]


def load_uids() -> tuple[str, str]:
    if not UIDS_PATH.exists():
        raise SystemExit(
            "Missing firebase/seed/uids.local.json\n"
            'Create it with: {"MEMBER_UID": "...", "ADMIN_UID": "..."}'
        )
    data = json.loads(UIDS_PATH.read_text(encoding="utf-8"))
    member_uid = str(data.get("MEMBER_UID", "")).strip()
    admin_uid = str(data.get("ADMIN_UID", "")).strip()
    if not member_uid or not admin_uid:
        raise SystemExit("uids.local.json must contain MEMBER_UID and ADMIN_UID")
    if "MEMBER_UID" in member_uid or "ADMIN_UID" in admin_uid:
        raise SystemExit("Replace the placeholders with the real Auth UIDs")
    return member_uid, admin_uid


def replace_uids(value, member_uid: str, admin_uid: str):
    if isinstance(value, str):
        return value.replace("MEMBER_UID", member_uid).replace("ADMIN_UID", admin_uid)
    if isinstance(value, list):
        return [replace_uids(item, member_uid, admin_uid) for item in value]
    if isinstance(value, dict):
        return {
            key: replace_uids(item, member_uid, admin_uid)
            for key, item in value.items()
        }
    return value


def load_collection(name: str, member_uid: str, admin_uid: str) -> dict:
    path = SEED_DIR / f"{name}.json"
    raw = json.loads(path.read_text(encoding="utf-8"))
    replaced = replace_uids(raw, member_uid, admin_uid)
    documents = {}
    for doc_id, fields in replaced.items():
        real_id = (
            member_uid if doc_id == member_uid or doc_id == "MEMBER_UID"
            else admin_uid if doc_id == admin_uid or doc_id == "ADMIN_UID"
            else doc_id
        )
        documents[real_id] = fields
    return documents


def find_service_account() -> Path:
    for path in SERVICE_ACCOUNT_CANDIDATES:
        if path.exists():
            return path
    raise SystemExit(
        "Missing firebase/serviceAccount.json\n"
        "Firebase Console → Project settings → Service accounts → Generate new private key"
    )


def main() -> None:
    service_account_path = find_service_account()
    member_uid, admin_uid = load_uids()
    cred = credentials.Certificate(str(service_account_path))
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    for name in COLLECTIONS:
        documents = load_collection(name, member_uid, admin_uid)
        for doc_id, fields in documents.items():
            db.collection(name).document(doc_id).set(fields)
            print(f"wrote {name}/{doc_id}")

    print("Done. Refresh Firestore -> Data.")


if __name__ == "__main__":
    main()
