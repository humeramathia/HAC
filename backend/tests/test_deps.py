# ========================================
# START OF CODE
# ========================================

"""Payload helpers and admin-gate behaviour without a live Firebase token."""

import pytest
from fastapi import HTTPException

from deps import CurrentUser, member_payload, profile_payload, require_admin


def test_member_payload_fills_missing_fields_and_defaults_role():
    payload = member_payload("uid-1", {"firstName": "Aaliyah"})
    assert payload["memberId"] == "uid-1"
    assert payload["firstName"] == "Aaliyah"
    assert payload["role"] == "Member"
    assert payload["emailVerified"] is False


def test_profile_payload_uses_uid_when_member_id_missing():
    payload = profile_payload("uid-2", {"bowType": "Recurve"})
    assert payload["profileId"] == "uid-2"
    assert payload["memberId"] == "uid-2"
    assert payload["bowType"] == "Recurve"


def test_require_admin_rejects_members():
    member = CurrentUser("m1", "Member", member_payload("m1", {"role": "Member"}))
    with pytest.raises(HTTPException) as error:
        require_admin(member)
    assert error.value.status_code == 403


def test_require_admin_allows_admins():
    admin = CurrentUser("a1", "Admin", member_payload("a1", {"role": "Admin"}))
    assert require_admin(admin) is admin

# ========================================
# END OF CODE
# ========================================
