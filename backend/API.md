# Habibia Backend Contract
**Python API + Firestore + Kotlin app**

The backend is Python.  
Firestore is only the database.  
The Android app must not read or write Firestore directly.

```
Kotlin Android app
        │  HTTPS JSON
        ▼
Python FastAPI
        │
        ▼
Cloud Firestore
```

## Team rule

- Frontend (Kotlin): screens, navigation, send/receive JSON
- Backend (Python): login checks, roles, score totals, validation, Firestore access
- Database (Firestore): store documents using `firebase/SCHEMA.md`

If a field is not in `firebase/SCHEMA.md`, do not add it in Python or Kotlin until the group agrees.

---

## Base URL

Local emulator: `http://10.0.2.2:8000`  
Hosted: `https://habibia-api-2riv.onrender.com` (set in `HabibiaApi.HOSTED_URL`)

### Host on Render (no local uvicorn)

The repo includes `render.yaml`. After you push this branch to GitHub:

1. Open [render.com](https://render.com) and sign in with GitHub.
2. New → Blueprint, pick `humeramathia/HAC`, and apply `render.yaml`.
   Or New → Web Service, connect the same repo, and use:
   - Root Directory: `backend`
   - Runtime: Python
   - Build: `pip install -r requirements.txt`
   - Start: `uvicorn main:app --host 0.0.0.0 --port $PORT`
3. Add environment variables (do not commit these):
   - `FIREBASE_WEB_API_KEY` — same value as `backend/.env`
   - `FIREBASE_SERVICE_ACCOUNT` — the service account JSON as one line
4. Deploy, then put the `https://….onrender.com` URL in `HabibiaApi.HOSTED_URL`.

The free instance sleeps after idle time. The first request can take 30–50 seconds.

All requests use JSON.  
Protected routes send:

```
Authorization: Bearer <token>
```

---

## Endpoints

### Auth

| Method | Path | Who | Body | Success |
|---|---|---|---|---|
| POST | `/auth/register` | public | firstName, lastName, email, password | member created |
| POST | `/auth/login` | public | email, password | token, role, member |
| POST | `/auth/logout` | signed-in | | ok |

Login response:

```json
{
  "token": "jwt-or-firebase-token",
  "role": "Member",
  "member": {
    "memberId": "UID",
    "firstName": "Aaliyah",
    "lastName": "Smit",
    "email": "member@habibia.co.za",
    "role": "Member"
  }
}
```

### Profile

| Method | Path | Who |
|---|---|---|
| GET | `/me` | signed-in |
| GET | `/me/profile` | signed-in |
| PUT | `/me/profile` | signed-in |

### Events and competitions

| Method | Path | Who |
|---|---|---|
| GET | `/events` | signed-in |
| POST | `/events` | admin |
| PUT | `/events/{id}` | admin |
| DELETE | `/events/{id}` | admin |
| GET | `/competitions` | signed-in |
| POST | `/competitions` | admin |
| PUT | `/competitions/{id}` | admin |
| DELETE | `/competitions/{id}` | admin |

### Scoring

The app sends arrows. Python calculates totals.  
The app must not send `totalScore` as the only value.

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/score-sessions` | member | create finished Practice or League session |
| GET | `/score-sessions?type=PRACTICE` | member | own history |
| GET | `/score-sessions?type=LEAGUE` | member | own history |
| GET | `/score-sessions/{id}` | owner or admin | details + ends |
| GET | `/score-sessions/{id}/progress` | owner or admin | graph stats |

Create session body:

```json
{
  "type": "PRACTICE",
  "title": "Saturday Practice",
  "distanceMeters": 18,
  "arrowsPerEnd": 6,
  "numberOfEnds": 10,
  "date": "2026-09-12",
  "notes": "Optional",
  "ends": [
    {
      "endNumber": 1,
      "arrows": [
        { "value": 9, "isX": false },
        { "value": 10, "isX": true }
      ]
    }
  ]
}
```

Python must calculate and save:

- end totals
- totalScore
- maxScore
- averageArrow
- tensCount
- xCount
- highestEnd
- lowestEnd

League create must use 60 arrows, 6 per end, 10 ends. Distance can change.

### Admin member progress

| Method | Path | Who |
|---|---|---|
| GET | `/admin/members` | admin |
| GET | `/admin/members/{memberId}` | admin |
| GET | `/admin/members/{memberId}/score-sessions?type=PRACTICE` | admin |
| GET | `/admin/members/{memberId}/score-sessions?type=LEAGUE` | admin |
| GET | `/admin/members/{memberId}/progress?type=PRACTICE` | admin |
| GET | `/admin/members/{memberId}/progress?type=LEAGUE` | admin |

### Comms and resources

| Method | Path | Who |
|---|---|---|
| GET | `/announcements` | signed-in |
| POST / PUT / DELETE | `/announcements` | admin |
| GET | `/notifications` | signed-in, own only |
| PUT | `/notifications/{id}/read` | owner |
| GET | `/resources` | signed-in |
| POST / PUT / DELETE | `/resources` | admin |

---

## Suggested coding split

| Person | Work |
|---|---|
| Python auth | `/auth`, `/me`, `/me/profile` |
| Python calendar | `/events`, `/competitions` |
| Python scoring | `/score-sessions` and calculations |
| Python admin | `/admin/members...`, announcements, resources, notifications |
| Kotlin | call these endpoints from the existing Fragments |

---

## What Python stores in Firestore

Python reads and writes the collections in `firebase/SCHEMA.md`.  
Kotlin never uses collection names. Kotlin only uses these API paths.
