from __future__ import annotations

from fastapi import HTTPException

LEAGUE_ARROWS_PER_END = 6
LEAGUE_ENDS = 10
LEAGUE_TOTAL_ARROWS = 60


def calculate_session(body: dict) -> dict:
    session_type = str(body.get("type", "")).upper()
    if session_type not in ("PRACTICE", "LEAGUE"):
        raise HTTPException(status_code=400, detail="type must be PRACTICE or LEAGUE")

    try:
        arrows_per_end = int(body.get("arrowsPerEnd"))
        number_of_ends = int(body.get("numberOfEnds"))
        distance = int(body.get("distanceMeters"))
    except (TypeError, ValueError):
        raise HTTPException(status_code=400, detail="distanceMeters, arrowsPerEnd and numberOfEnds must be numbers")

    if arrows_per_end < 1 or number_of_ends < 1:
        raise HTTPException(status_code=400, detail="arrowsPerEnd and numberOfEnds must be at least 1")

    if session_type == "LEAGUE":
        if arrows_per_end != LEAGUE_ARROWS_PER_END or number_of_ends != LEAGUE_ENDS:
            raise HTTPException(
                status_code=400,
                detail="League rounds must be 6 arrows per end and 10 ends (60 arrows)",
            )

    ends_in = body.get("ends")
    if not isinstance(ends_in, list) or len(ends_in) != number_of_ends:
        raise HTTPException(status_code=400, detail=f"ends must contain exactly {number_of_ends} items")

    built_ends = []
    tens = 0
    xs = 0
    for index, raw_end in enumerate(ends_in, start=1):
        arrows_in = raw_end.get("arrows") if isinstance(raw_end, dict) else None
        if not isinstance(arrows_in, list) or len(arrows_in) != arrows_per_end:
            raise HTTPException(
                status_code=400,
                detail=f"end {index} must contain exactly {arrows_per_end} arrows",
            )
        arrows = []
        end_total = 0
        for arrow in arrows_in:
            try:
                value = int(arrow.get("value"))
            except (TypeError, ValueError, AttributeError):
                raise HTTPException(status_code=400, detail="each arrow value must be a number 0-10")
            is_x = bool(arrow.get("isX", False))
            if value < 0 or value > 10:
                raise HTTPException(status_code=400, detail="arrow value must be 0-10")
            if is_x and value != 10:
                raise HTTPException(status_code=400, detail="X arrows must have value 10")
            arrows.append({"value": value, "isX": is_x})
            end_total += value
            if value == 10:
                tens += 1
            if is_x:
                xs += 1
        end_number = int(raw_end.get("endNumber", index)) if isinstance(raw_end, dict) else index
        built_ends.append({"endNumber": end_number, "total": end_total, "arrows": arrows})

    if session_type == "LEAGUE" and arrows_per_end * number_of_ends != LEAGUE_TOTAL_ARROWS:
        raise HTTPException(status_code=400, detail="League rounds must total 60 arrows")

    totals = [item["total"] for item in built_ends]
    total_score = sum(totals)
    total_arrows = arrows_per_end * number_of_ends
    max_score = total_arrows * 10
    average = 0.0 if total_arrows == 0 else round(total_score / total_arrows, 2)

    ranking = body.get("ranking")
    field_size = body.get("fieldSize")
    league_name = body.get("leagueName")
    if session_type == "PRACTICE":
        ranking = None
        field_size = None
        league_name = None

    return {
        "type": session_type,
        "title": str(body.get("title", "")).strip() or "Session",
        "distanceMeters": distance,
        "arrowsPerEnd": arrows_per_end,
        "numberOfEnds": number_of_ends,
        "date": str(body.get("date", "")).strip(),
        "notes": str(body.get("notes", "") or ""),
        "ends": built_ends,
        "totalScore": total_score,
        "maxScore": max_score,
        "averageArrow": average,
        "tensCount": tens,
        "xCount": xs,
        "highestEnd": max(totals) if totals else 0,
        "lowestEnd": min(totals) if totals else 0,
        "ranking": ranking,
        "fieldSize": field_size,
        "leagueName": league_name,
    }


def serialize_session(doc_id: str, data: dict) -> dict:
    payload = dict(data)
    payload["sessionId"] = doc_id
    return payload


def session_progress(doc_id: str, data: dict) -> dict:
    ends = data.get("ends") or []
    labels = [f"End {item.get('endNumber', i)}" for i, item in enumerate(ends, start=1)]
    end_totals = [int(item.get("total", 0)) for item in ends]
    return {
        "sessionId": doc_id,
        "labels": labels,
        "endTotals": end_totals,
        "totalScore": data.get("totalScore", sum(end_totals)),
        "averageArrow": data.get("averageArrow", 0),
    }


def member_type_progress(member_id: str, session_type: str, sessions: list[dict]) -> dict:
    ordered = sorted(sessions, key=lambda item: item.get("date", ""))
    return {
        "memberId": member_id,
        "type": session_type,
        "labels": [item.get("date", "") for item in ordered],
        "totals": [item.get("totalScore", 0) for item in ordered],
        "sessions": [
            {
                "sessionId": item.get("sessionId"),
                "date": item.get("date"),
                "totalScore": item.get("totalScore"),
                "averageArrow": item.get("averageArrow"),
            }
            for item in ordered
        ],
    }
