# ========================================
# START OF CODE
# ========================================

"""Club scoring rules: totals, X vs 10, and the fixed league layout."""

import pytest
from fastapi import HTTPException

from scoring import calculate_session, member_type_progress, serialize_session, session_progress


def _end(end_number: int, values: list[int], xs: set[int] | None = None) -> dict:
    xs = xs or set()
    return {
        "endNumber": end_number,
        "arrows": [
            {"value": value, "isX": index in xs}
            for index, value in enumerate(values)
        ],
    }


def test_practice_totals_and_x_count():
    result = calculate_session(
        {
            "type": "PRACTICE",
            "title": "Saturday Practice",
            "distanceMeters": 18,
            "arrowsPerEnd": 3,
            "numberOfEnds": 2,
            "date": "2026-09-12",
            "notes": "grouping",
            "ranking": 1,
            "fieldSize": 8,
            "leagueName": "should be dropped",
            "ends": [
                _end(1, [10, 9, 8], xs={0}),
                _end(2, [7, 6, 5]),
            ],
        }
    )

    assert result["totalScore"] == 45
    assert result["maxScore"] == 60
    assert result["averageArrow"] == 7.5
    assert result["tensCount"] == 1
    assert result["xCount"] == 1
    assert result["highestEnd"] == 27
    assert result["lowestEnd"] == 18
    assert result["ranking"] is None
    assert result["fieldSize"] is None
    assert result["leagueName"] is None
    assert result["ends"][0]["total"] == 27


def test_league_requires_sixty_arrows():
    with pytest.raises(HTTPException) as error:
        calculate_session(
            {
                "type": "LEAGUE",
                "distanceMeters": 18,
                "arrowsPerEnd": 6,
                "numberOfEnds": 9,
                "date": "2026-09-12",
                "ends": [_end(1, [9, 9, 9, 9, 9, 9])] * 9,
            }
        )
    assert error.value.status_code == 400
    assert "60 arrows" in error.value.detail


def test_x_must_score_ten():
    with pytest.raises(HTTPException) as error:
        calculate_session(
            {
                "type": "PRACTICE",
                "distanceMeters": 18,
                "arrowsPerEnd": 1,
                "numberOfEnds": 1,
                "date": "2026-09-12",
                "ends": [{"endNumber": 1, "arrows": [{"value": 9, "isX": True}]}],
            }
        )
    assert error.value.status_code == 400


def test_arrow_out_of_range_is_rejected():
    with pytest.raises(HTTPException) as error:
        calculate_session(
            {
                "type": "PRACTICE",
                "distanceMeters": 18,
                "arrowsPerEnd": 1,
                "numberOfEnds": 1,
                "date": "2026-09-12",
                "ends": [{"endNumber": 1, "arrows": [{"value": 11, "isX": False}]}],
            }
        )
    assert "0-10" in error.value.detail


def test_serialize_session_adds_id_without_mutating_source():
    stored = {"title": "Practice"}
    payload = serialize_session("abc", stored)
    assert payload["sessionId"] == "abc"
    assert "sessionId" not in stored


def test_session_progress_aligns_labels_with_end_totals():
    chart = session_progress(
        "s1",
        {
            "ends": [{"endNumber": 1, "total": 50}, {"endNumber": 2, "total": 48}],
            "totalScore": 98,
            "averageArrow": 8.17,
        },
    )
    assert chart["labels"] == ["End 1", "End 2"]
    assert chart["endTotals"] == [50, 48]


def test_member_progress_is_sorted_by_date():
    chart = member_type_progress(
        "m1",
        "PRACTICE",
        [
            {"sessionId": "b", "date": "2026-09-10", "totalScore": 400, "averageArrow": 8.0},
            {"sessionId": "a", "date": "2026-09-01", "totalScore": 380, "averageArrow": 7.6},
        ],
    )
    assert chart["labels"] == ["2026-09-01", "2026-09-10"]
    assert chart["totals"] == [380, 400]

# ========================================
# END OF CODE
# ========================================
