# ========================================
# START OF CODE
# ========================================

"""HTTP route tests against an in-memory database (no live Firestore)."""

from deps import get_current_user, require_admin


def test_root_health(api_client):
    response = api_client.get("/")
    assert response.status_code == 200
    assert response.json()["ok"] is True


def test_events_require_a_signed_in_user(api_client):
    response = api_client.get("/events")
    assert response.status_code == 401


def test_member_can_list_events(api_client, member_user, fake_db):
    from main import app

    fake_db.collection("events").document("e1").set(
        {
            "title": "Saturday Practice",
            "description": "",
            "eventDate": "2026-09-12",
            "eventTime": "09:00",
            "location": "Habibia",
            "type": "Practice",
            "createdBy": "admin-1",
        }
    )
    app.dependency_overrides[get_current_user] = lambda: member_user
    response = api_client.get("/events")
    assert response.status_code == 200
    assert response.json()["events"][0]["title"] == "Saturday Practice"


def test_member_cannot_create_an_event(api_client, member_user):
    from main import app

    app.dependency_overrides[get_current_user] = lambda: member_user
    response = api_client.post(
        "/events",
        json={
            "title": "Extra Practice",
            "description": "",
            "eventDate": "2026-09-20",
            "eventTime": "10:00",
            "location": "Habibia",
            "type": "Practice",
        },
    )
    assert response.status_code == 403


def test_admin_can_create_an_event(api_client, admin_user):
    from main import app

    app.dependency_overrides[get_current_user] = lambda: admin_user
    app.dependency_overrides[require_admin] = lambda: admin_user
    response = api_client.post(
        "/events",
        json={
            "title": "Junior Day",
            "description": "Coaching",
            "eventDate": "2026-09-25",
            "eventTime": "10:00",
            "location": "Habibia Archery Club",
            "type": "Event",
        },
    )
    assert response.status_code == 201
    body = response.json()
    assert body["title"] == "Junior Day"
    assert body["eventId"]


def test_notifications_are_scoped_to_the_caller(api_client, member_user, fake_db):
    from main import app

    fake_db.collection("notifications").document("n1").set(
        {
            "memberId": "member-1",
            "title": "Yours",
            "message": "Hello",
            "dateSent": "2026-09-01",
            "isRead": False,
        }
    )
    fake_db.collection("notifications").document("n2").set(
        {
            "memberId": "someone-else",
            "title": "Not yours",
            "message": "Hidden",
            "dateSent": "2026-09-02",
            "isRead": False,
        }
    )
    app.dependency_overrides[get_current_user] = lambda: member_user
    response = api_client.get("/notifications")
    assert response.status_code == 200
    titles = [item["title"] for item in response.json()["notifications"]]
    assert titles == ["Yours"]


def test_member_can_save_a_practice_session(api_client, member_user):
    from main import app

    app.dependency_overrides[get_current_user] = lambda: member_user
    response = api_client.post(
        "/score-sessions",
        json={
            "type": "PRACTICE",
            "title": "Unit test practice",
            "distanceMeters": 18,
            "arrowsPerEnd": 3,
            "numberOfEnds": 1,
            "date": "2026-09-12",
            "notes": "",
            "ends": [
                {
                    "endNumber": 1,
                    "arrows": [
                        {"value": 10, "isX": True},
                        {"value": 9, "isX": False},
                        {"value": 8, "isX": False},
                    ],
                }
            ],
        },
    )
    assert response.status_code == 201
    body = response.json()
    assert body["totalScore"] == 27
    assert body["memberId"] == "member-1"
    assert body["xCount"] == 1

# ========================================
# END OF CODE
# ========================================
