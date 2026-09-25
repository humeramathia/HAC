# Habibia Archery Club Management System

Android club app for [Habibia Archery Club](https://haclub.co.za/). Members sign in, check the calendar, record practice and league scores, read resources, and manage their profile. Admins manage members, events, competitions, announcements, resources, and view progress.

This is an academic team project. The Android UI talks only to a Python API. The API is the only process that reads or writes Cloud Firestore.

**Presentation:** [https://youtu.be/tm-dFHZ1HUg](https://youtu.be/tm-dFHZ1HUg)  
**Progress report:** [docs/Progress-Report-Axion-Innovations-Meeting.pdf](docs/Progress-Report-Axion-Innovations-Meeting.pdf)

---

## Contents

1. [Team](#team)
2. [What you are looking at](#what-you-are-looking-at)
3. [Architecture](#architecture)
4. [Tech stack](#tech-stack)
5. [Features](#features)
6. [What is live vs still dummy](#what-is-live-vs-still-dummy)
7. [Folder structure](#folder-structure)
8. [Android app internals](#android-app-internals)
9. [Python API internals](#python-api-internals)
10. [Firestore data model](#firestore-data-model)
11. [Prerequisites](#prerequisites)
12. [Run the Android app](#run-the-android-app)
13. [Run the API on your machine](#run-the-api-on-your-machine)
14. [Test accounts](#test-accounts)
15. [Create an account](#create-an-account)
16. [Seed Firestore](#seed-firestore)
17. [Host the API on Render](#host-the-api-on-render)
18. [Design system](#design-system)
19. [Team rules](#team-rules)
20. [Automated testing](#automated-testing)
21. [Common problems](#common-problems)
22. [Further reading](#further-reading)

---

## Team

| Name | Student number |
|---|---|
| Humera Mathia | ST10276384 |
| David Minlah | ST10442249 |
| Imraan Noordien | ST10434419 |
| Siddiq Malik | ST10455630 |

Repo: [github.com/humeramathia/HAC](https://github.com/humeramathia/HAC)

---

## What you are looking at

Three pieces of software, one product:

| Layer | Role | Lives in |
|---|---|---|
| Kotlin Android app | Screens, navigation, forms, HTTP calls | `app/` |
| Python FastAPI | Auth, roles, validation, score maths, Firestore | `backend/` |
| Cloud Firestore + Firebase Auth | Documents and passwords | `firebase/` |

The phone never talks to Firestore. It never uses the Firebase Android SDK. Every signed-in request is JSON over HTTPS to FastAPI, with `Authorization: Bearer <Firebase ID token>`.

If you only clone and run the Android app, it already points at the hosted API:

`https://habibia-api-2riv.onrender.com`

You do not need to run uvicorn unless you are changing Python.

---

## Architecture

```
┌──────────────────────────────────────┐
│  Android app                         │
│  Kotlin + XML Fragments              │
│  HabibiaApi (HttpURLConnection)      │
└──────────────────┬───────────────────┘
                   │ HTTPS JSON
                   │ Bearer token
                   ▼
┌──────────────────────────────────────┐
│  FastAPI  (Render or local uvicorn)  │
│  routers → scoring → firebase_admin  │
└──────────────────┬───────────────────┘
                   │ Admin SDK
                   ▼
┌──────────────────────────────────────┐
│  Firebase                            │
│  Auth (email / password)             │
│  Firestore (8 collections)           │
└──────────────────────────────────────┘
```

**Request path for a typical screen**

1. Fragment calls `apiInBackground { HabibiaApi.get("/events") }`.
2. `HabibiaApi` attaches the stored Firebase ID token and hits `BASE_URL + path`.
3. FastAPI verifies the token in `deps.py`, loads `members/{uid}`, and checks the role.
4. The router reads or writes Firestore using the field names in `firebase/SCHEMA.md`.
5. The fragment parses JSON into the Kotlin data classes in `HabibiaData.kt`.

**Why this split**

- The app must not invent totals. Python calculates `totalScore`, `averageArrow`, tens, Xs, highest/lowest end.
- Role checks (`Member` vs `Admin`) happen on the server, not only in the UI.
- Passwords stay in Firebase Auth. They are never stored in Firestore.

---

## Tech stack

### Android

| Item | What we use |
|---|---|
| Language | Kotlin |
| UI | XML layouts, Fragments, one `MainActivity` |
| Components | Material Design 3, AppCompat, ConstraintLayout |
| HTTP | One client: `HabibiaApi` (`HttpURLConnection` + `org.json`) |
| Min / target SDK | 24 / 36 |
| Java compatibility | 11 |
| Android Gradle Plugin | 9.0.1 |
| Package | `com.example.hacprototype` |

There is no Retrofit, no Room, no Firebase Android SDK, and no second HTTP client. Do not add one.

### Backend

| Item | What we use |
|---|---|
| Language | Python 3.12 |
| Framework | FastAPI + Uvicorn |
| Auth verify | `firebase-admin` ID tokens |
| Auth sign-in / verify email | Firebase Identity Toolkit REST (`httpx`) |
| Config | `python-dotenv` locally, Render env vars in production |

### Data and hosting

| Item | What we use |
|---|---|
| Database | Cloud Firestore project `habibia-archery-club` |
| Auth | Firebase Authentication, email + password |
| API host | Render free web service (`render.yaml`) |
| Local API | `uvicorn main:app --host 0.0.0.0 --port 8000` |

---

## Features

### Member

- Splash, branded login, register, email verification
- Home dashboard (greeting, shortcuts; unread notification dot from the API)
- Club calendar (events + competitions, type chips, search)
- Event and competition details
- Announcements
- Notifications (mark as read)
- Scores hub → practice or league setup → end-by-end entry → save → history → details → progress
- Beginner resources
- Profile and edit profile
- Logout (clears the token)

### Admin

- Dashboard counts from the API
- Manage members (list, delete)
- Member practice / league progress
- Manage events, competitions, announcements, resources (list + create + edit + delete)
- Statistics
- Logout

### Scoring rules the API enforces

- Each arrow is `0–10`. An X is `{ "value": 10, "isX": true }`.
- Practice: distance and end size are chosen on the setup screen.
- League: always 60 arrows, 6 per end, 10 ends. Distance can change.
- The app sends arrows. Python stores the calculated totals. Do not send `totalScore` as the only value.

---

## What is live vs still dummy

Most screens load and save through `HabibiaApi`. A few home tiles still use in-memory `HabibiaDummyData` so the dashboard layout stays stable.

| Area | Source |
|---|---|
| Login, register, email verify, logout | API |
| Profile / edit profile | API |
| Calendar, event details, competitions | API |
| Announcements, notifications, resources | API |
| Record / list / detail / progress scores | API |
| Admin dashboard counts, manage CRUD, stats | API |
| Home featured event, home score tiles, home upcoming competition | Dummy data |
| **Demo Admin** button | Skips login and opens the admin UI locally |

Demo buttons stay on the login screen on purpose.

---

## Folder structure

```text
Prototype/
├── README.md                          ← you are here
├── render.yaml                        ← Render deploy settings
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
│
├── app/                               ← Android application
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml        ← INTERNET + cleartext for local API
│       ├── java/com/example/hacprototype/
│       │   ├── MainActivity.kt        ← single activity, fragment host
│       │   ├── HabibiaData.kt         ← models + session + leftover dummy lists
│       │   ├── HabibiaApi.kt          ← only HTTP client
│       │   ├── HabibiaUi.kt           ← navigation helpers, snackbars, chips
│       │   ├── HabibiaFragments.kt    ← auth, member, admin (except scores)
│       │   └── HabibiaScoreFragments.kt
│       └── res/
│           ├── layout/                ← one XML per screen / row
│           ├── drawable/              ← logo, buttons, nav icons
│           ├── menu/menu_member_bottom.xml
│           ├── values/                ← colours, dimens, strings, theme
│           └── values-night/
│
├── backend/                           ← FastAPI
│   ├── main.py                        ← app, CORS, routers, / and /tester
│   ├── deps.py                        ← Bearer token → CurrentUser / admin
│   ├── firebase_client.py             ← Admin SDK init
│   ├── scoring.py                     ← totals and league rules
│   ├── requirements.txt
│   ├── .env.example                   ← copy to .env locally (gitignored)
│   ├── API.md                         ← full endpoint contract
│   ├── static/tester.html             ← browser API tester
│   ├── tests/                         ← pytest
│   └── routers/
│       ├── auth.py                    ← register, login, verify, logout
│       ├── profile.py                 ← /me and /me/profile
│       ├── events.py
│       ├── competitions.py
│       ├── scores.py                  ← /score-sessions
│       ├── admin.py                   ← members and their progress
│       └── content.py                 ← announcements, notifications, resources
│
├── .github/workflows/test.yml         ← CI (pytest + Gradle unit tests)
├── docs/                              ← progress report and other project documents
└── firebase/
    ├── SCHEMA.md                      ← the only allowed fields
    ├── firestore.rules
    ├── firestore.indexes.json
    └── seed/
        ├── seed_firestore.py          ← upload seed JSON
        ├── members.json
        ├── memberProfiles.json
        ├── events.json
        ├── competitions.json
        ├── scoreSessions.json
        ├── announcements.json
        ├── notifications.json
        └── resources.json
```

Secrets are gitignored. Do not commit:

- `backend/.env`
- `firebase/serviceAccount.json` or `firebase/serviceAccount.json.json`
- `firebase/seed/uids.local.json`
- `google-services.json`

---

## Android app internals

### Navigation

`MainActivity` owns one `fragmentContainer`. After a short splash it shows `LoginFragment`. Successful member login opens `MemberHostFragment` (bottom nav). Successful admin login opens `AdminDashboardFragment`.

Member tabs:

| Tab | Fragment |
|---|---|
| Home | `MemberDashboardFragment` |
| Calendar | `ClubCalendarFragment` |
| Scores | `ScoresFragment` |
| Resources | `BeginnerResourcesFragment` |
| Profile | `ProfileFragment` |

Shared UI helpers live in `HabibiaUi.kt`: `goTo()`, `openMemberApp()`, `openAdminApp()`, `showMessage()`.

`HabibiaSession` holds the token, the logged-in member id, admin flag, and which detail row is selected. Logout calls `HabibiaSession.clearAuth()`.

### Talking to the API

`HabibiaApi` is the only HTTP client.

```kotlin
HabibiaApi.HOSTED_URL = "https://habibia-api-2riv.onrender.com"
// If HOSTED_URL is blank, the emulator uses http://10.0.2.2:8000
```

Call it from a Fragment like this (the trailing lambda is `onOk`):

```kotlin
apiInBackground(
    work = { HabibiaApi.get("/events") },
    onError = { showMessage(it) }
) { json ->
    // parse and bind
}
```

Timeouts are 50 seconds so a sleeping Render instance can wake up.

### Key Kotlin files

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Host activity, back behaviour, splash → login |
| `HabibiaData.kt` | Data classes, `HabibiaSession`, leftover dummy lists |
| `HabibiaApi.kt` | GET / POST / PUT / DELETE |
| `HabibiaFragments.kt` | Auth, calendar, profile, admin CRUD |
| `HabibiaScoreFragments.kt` | Practice/league entry, history, progress, admin member progress |
| `HabibiaUi.kt` | Shared navigation and formatting |

---

## Python API internals

`backend/main.py` loads `.env`, initialises Firebase, mounts routers, and exposes:

- `GET /` → `{ "ok": true, "docs": "/docs", "tester": "/tester" }`
- `GET /docs` → Swagger
- `GET /tester` → simple HTML caller

`deps.py` verifies the Firebase ID token, loads `members/{uid}`, and exposes `require_admin`.

`firebase_client.py` accepts credentials in this order:

1. `FIREBASE_SERVICE_ACCOUNT` env var (full JSON, used on Render)
2. `FIREBASE_SERVICE_ACCOUNT_FILE` or `/etc/secrets/serviceAccount.json`
3. `firebase/serviceAccount.json` on disk (local)

### Endpoint map

Full request/response shapes are in [`backend/API.md`](backend/API.md). Summary:

| Method | Path | Who |
|---|---|---|
| POST | `/auth/register` | public |
| POST | `/auth/login` | public |
| POST | `/auth/resend-verification` | public |
| POST | `/auth/confirm-verification` | public |
| POST | `/auth/logout` | signed-in |
| GET | `/me`, `/me/profile` | signed-in |
| PUT | `/me/profile` | signed-in |
| GET / POST / PUT / DELETE | `/events`, `/competitions` | GET signed-in; writes admin |
| POST GET | `/score-sessions` | member (owner) |
| GET | `/admin/members...` | admin |
| GET POST PUT DELETE | `/announcements`, `/resources` | GET signed-in; writes admin |
| GET / PUT | `/notifications`, `/notifications/{id}/read` | owner |

---

## Firestore data model

Do not add collections. These eight cover the app. Field-level types are in [`firebase/SCHEMA.md`](firebase/SCHEMA.md).

| Collection | Document ID | Purpose |
|---|---|---|
| `members` | Auth UID | name, email, role, emailVerified, dateJoined |
| `memberProfiles` | Auth UID | experience, bow, division, emergency contact |
| `events` | auto-id | club calendar events |
| `competitions` | auto-id | competitions |
| `scoreSessions` | auto-id | one finished practice or league round |
| `announcements` | auto-id | club posts |
| `notifications` | auto-id | per-member inbox (`memberId`) |
| `resources` | auto-id | beginner links |

`role` is `Member` or `Admin`. That value is what routes the app after login.

Passwords are not in Firestore.

---

## Prerequisites

### Everyone running the app

- Android Studio (current stable)
- Android SDK with an emulator (API 24+) or a physical phone
- Internet (the default API is on Render)

### Anyone changing the backend

- Python 3.12
- A Firebase service account JSON in `firebase/` (gitignored)
- `backend/.env` copied from `backend/.env.example`

### Anyone reseeding data

- Real Firebase Auth UIDs in `firebase/seed/uids.local.json` (gitignored)

---

## Run the Android app

This is the normal path. The app already uses the hosted API.

1. Clone the repo and open the **Prototype** folder in Android Studio (the folder that contains `settings.gradle.kts`).
2. Trust the Gradle project and wait for sync.
3. Start an emulator or plug in a phone.
4. Run the `app` configuration.
5. Wait on the first login if Render was asleep (30–50 seconds is normal).

On a **physical phone** the hosted HTTPS URL works. `http://10.0.2.2:8000` is emulator-only and will fail on a real device.

To force the local API instead, clear `HOSTED_URL` in `HabibiaApi.kt` so it falls back to `http://10.0.2.2:8000`, then start uvicorn (next section).

---

## Run the API on your machine

Only needed when you change Python or Firestore wiring.

```powershell
cd backend
copy .env.example .env
# put FIREBASE_WEB_API_KEY in .env
# put firebase/serviceAccount.json (or serviceAccount.json.json) in firebase/
py -m pip install -r requirements.txt
py -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Check it in a browser: [http://127.0.0.1:8000](http://127.0.0.1:8000) and [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs).

From the **emulator**, that same process is `http://10.0.2.2:8000` (`10.0.2.2` is the host loopback).

If the emulator still cannot connect while uvicorn is up:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```

---

## Test accounts

These emails exist in the seed data. **Demo Member** on the login screen uses the member account.

| Role | Email | Password |
|---|---|---|
| Member | `member@habibia.co.za` | `Member123` |
| Admin | `admin@habibia.co.za` | Firebase Auth password (not stored in git) |

**Demo Admin** does not call `/auth/login`. It opens the admin screens with whatever dummy member data is already in memory. Use a real admin login when you need API writes to succeed.

---

## Create an account

1. Login → Create account.
2. The app `POST`s `/auth/register`. Firebase Auth creates the user. Firestore gets `members/{uid}` and `memberProfiles/{uid}` with `emailVerified: false`.
3. Firebase sends a verification email (`VERIFY_EMAIL`).
4. Open the email and tap the link.
5. Back in the app, continue on the verification screen (`/auth/confirm-verification`) or log in again.

Unverified accounts cannot use the rest of the app. Seed accounts are already verified.

---

## Seed Firestore

Use this after a new Firebase project, or to reset club data.

1. Create the two Auth users in the Firebase console (`member@habibia.co.za` and `admin@habibia.co.za`).
2. Copy their UIDs into `firebase/seed/uids.local.json`:

```json
{
  "MEMBER_UID": "the-member-auth-uid",
  "ADMIN_UID": "the-admin-auth-uid"
}
```

3. Place the service account JSON under `firebase/`.
4. From the repo root:

```powershell
py firebase/seed/seed_firestore.py
```

The script replaces `MEMBER_UID` / `ADMIN_UID` placeholders in the JSON files and writes the eight collections.

---

## Host the API on Render

`render.yaml` already describes the service.

1. Push the branch to GitHub.
2. On [render.com](https://render.com) create a Blueprint (or a Python web service) from this repo.
3. Root directory: `backend`
4. Build: `pip install -r requirements.txt`
5. Start: `uvicorn main:app --host 0.0.0.0 --port $PORT`
6. Environment variables (never commit these):

| Name | Value |
|---|---|
| `FIREBASE_WEB_API_KEY` | Web API key from Firebase project settings (same as `backend/.env`) |
| `FIREBASE_SERVICE_ACCOUNT` | The service account JSON as **one line**, starting with `{` |

To copy the JSON as one line on Windows:

```powershell
py -c "import json; print(json.dumps(json.load(open('firebase/serviceAccount.json.json'))))" | Set-Clipboard
```

7. After deploy, set `HabibiaApi.HOSTED_URL` to the `https://….onrender.com` URL (no trailing slash) and rebuild the app.

The current production URL is `https://habibia-api-2riv.onrender.com`.

The free instance sleeps when idle. The first request after that can take 30–50 seconds.

---

## Design system

Colours follow [haclub.co.za](https://haclub.co.za/):

| Token | Hex | Use |
|---|---|---|
| Primary green | `#9ABA55` | Buttons, accents, selected states |
| Secondary blue | `#528FD0` | Headings, links, secondary actions |
| Alert red | `#FE3B3A` | Errors, delete, unread |
| Background | `#FAFBFC` | Screen backgrounds |
| Surface | `#FFFFFF` | Cards and inputs |

Assets:

- `app/src/main/res/drawable/logo.png` — club logo
- `app/src/main/res/drawable/desgin.png` — geometric edge decoration

Shared tokens live in `values/colors.xml`, `values/styles.xml`, and `values/themes.xml`.

---

## Team rules

1. **Kotlin = UI + JSON only.** Fragments call `HabibiaApi`. They do not import Firebase and they do not calculate official totals for storage.
2. **Python = rules + Firestore.** Login checks, roles, score maths, and document writes stay in FastAPI.
3. **Schema first.** If a field is not in `firebase/SCHEMA.md`, do not add it in Python or Kotlin until the group agrees.
4. **One HTTP client.** All network code goes through `HabibiaApi` / `apiInBackground`. Do not add Retrofit, Volley, or a second `HttpURLConnection` helper.
5. **Keep Demo buttons.** Do not remove Demo Member or Demo Admin.
6. **Do not invent IDs on admin create.** Let Firestore / the API assign document IDs.
7. **Do not commit secrets.** `.env`, service accounts, and `uids.local.json` stay local.

Suggested work split (also in `backend/API.md`):

| Area | Code |
|---|---|
| Auth and profile | `backend/routers/auth.py`, `profile.py`, login/register fragments |
| Calendar | `events.py`, `competitions.py`, calendar fragments |
| Scoring | `scoring.py`, `scores.py`, `HabibiaScoreFragments.kt` |
| Admin and comms | `admin.py`, `content.py`, manage-* fragments |

---

## Automated testing

The repo has two suites. Neither needs a running emulator or a live Firestore project.

### Python API (`backend/tests`)

pytest covers scoring maths, payload helpers, Firebase credential parsing, and HTTP routes against an in-memory fake database.

```powershell
cd backend
py -m pip install -r requirements.txt
py -m pytest
```

`HABIBIA_TESTING=1` is set by `tests/conftest.py` so importing the app does not call Firebase.

| File | What it checks |
|---|---|
| `tests/test_scoring.py` | Totals, X vs 10, league must be 60 arrows |
| `tests/test_deps.py` | Member/admin payloads and the 403 admin gate |
| `tests/test_firebase_client.py` | JSON vs path, wrapped quotes, `https:/` repair |
| `tests/test_api_routes.py` | Health, events auth, notification scoping, save practice |

### Android JVM tests (`app/src/test`)

JUnit tests run on the desktop, not on an emulator.

```powershell
.\gradlew.bat test
```

In Android Studio: **app → test → java → com.example.hacprototype → Run Tests**.

| File | What it checks |
|---|---|
| `ScoreSessionTest.kt` | Session totals, X labels, league 60-arrow constants |
| `DisplayFormatTest.kt` | ISO dates, averages, improvement sign |

The instrumented test in `app/src/androidTest` still checks the package name on a device or emulator (`.\gradlew.bat connectedDebugAndroidTest`).

### CI

`.github/workflows/test.yml` runs pytest and `./gradlew test` on every push and pull request.

---

## Common problems

**Login spins, then “failed to connect to /10.0.2.2 (port 8000)”**  
The app is on the local emulator URL and uvicorn is not running. Either start the API, or keep `HOSTED_URL` set to the Render URL and rebuild.

**First login on the hosted API is very slow**  
Render free tier is waking up. Wait once; later requests are fast.

**“FIREBASE_SERVICE_ACCOUNT is not valid JSON” on Render**  
Paste the whole service account file as one line starting with `{`. Do not wrap it in extra quotes.

**Verification email never arrives**  
The API must send `requestType: VERIFY_EMAIL`. Check spam. Use **Resend** on the verification screen. Confirm `FIREBASE_WEB_API_KEY` is the Firebase **Web** API key.

**Gradle `FileHasher Access is denied`**  
Windows Controlled folder access is blocking writes under Documents. Allow Android Studio / Java, or move the project out of a protected folder.

**Release build on a real phone cannot reach `10.0.2.2`**  
That address only exists inside the emulator. Use the Render HTTPS URL.

**Admin API calls say you do not have permission**  
You used **Demo Admin**, which does not store a token. Log in as the real admin account.

---

## Further reading

| File | What it covers |
|---|---|
| [`backend/API.md`](backend/API.md) | Endpoint contract, auth header, score body |
| [`firebase/SCHEMA.md`](firebase/SCHEMA.md) | Collections, field types, example documents |
| [`firebase/firestore.rules`](firebase/firestore.rules) | Who may read/write if anything ever hit Firestore directly |
| [`backend/static/tester.html`](backend/static/tester.html) | Browser caller at `/tester` |

---

## Licence / academic use

Developed for academic assessment as a software development project for Habibia Archery Club.
