# ========================================
# START OF CODE
# ========================================

"""Credential parsing: JSON vs path, wrapped quotes, and Render https:/ repair."""

import json

import pytest

from firebase_client import _account_from_text, _clean_env, _repair_account


def test_clean_env_strips_wrapping_quotes_from_json():
    raw = '\'{"type":"service_account"}\''
    assert _clean_env(raw).startswith("{")


def test_account_from_text_parses_inline_json():
    raw = json.dumps(
        {
            "type": "service_account",
            "project_id": "habibia-archery-club",
            "token_uri": "https:/oauth2.googleapis.com/token",
        }
    )
    parsed = _account_from_text(raw)
    assert parsed["project_id"] == "habibia-archery-club"
    assert parsed["token_uri"] == "https://oauth2.googleapis.com/token"


def test_account_from_text_does_not_treat_json_as_a_file_path():
    raw = json.dumps({"type": "service_account", "project_id": "demo"})
    parsed = _account_from_text(raw)
    assert parsed["type"] == "service_account"


def test_account_from_text_rejects_non_service_account_json():
    with pytest.raises(RuntimeError, match="service_account"):
        _account_from_text('{"type":"user"}')


def test_repair_account_leaves_valid_https_alone():
    data = {"auth_uri": "https://accounts.google.com/o/oauth2/auth"}
    assert _repair_account(data)["auth_uri"] == "https://accounts.google.com/o/oauth2/auth"

# ========================================
# END OF CODE
# ========================================
