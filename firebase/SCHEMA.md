# Habibia Firestore Schema
Scanned from the current Android prototype: every member screen and every admin screen.

Database: Cloud Firestore  
Auth: Firebase Authentication (email + password)  
Backend: Python FastAPI reads/writes these collections  
App package: `com.example.hacprototype`

Do not add extra collections. These 8 cover the whole app.

---

## 1. What each side of the app needs

### Member screens

| Screen | Data required |
|---|---|
| Login / Register / Email verify | email, password (Auth only), firstName, lastName, role, emailVerified |
| Home | firstName, next event, next competition, latest/average/highest score, unread notification count |
| Calendar | events + competitions, filter by type, search title/location |
| Event details | title, date, time, location, description, type |
| Competitions | name, date, venue, deadline, status, description |
| Scores hub | latest, average, highest, practice count, league count |
| Practice setup + entry | title, distance, arrowsPerEnd, numberOfEnds, date, notes, each arrow 0–10 or X |
| League setup + entry | title, distance, date, notes, 60 arrows / 6 per end / 10 ends, each arrow |
| Practice / League history | session cards: date, distance, score, average, 10s, Xs, ranking |
| Score details | all session fields + every end and arrow |
| Progress graphs | session totals over dates |
| Resources | title, category, description, link |
| Profile / Edit profile | name, email, dateJoined, experience, bowType, division, emergencyContact |
| Notifications | title, message, dateSent, isRead |
| Announcements | title, content, datePosted |

### Admin screens

| Screen | Data required |
|---|---|
| Dashboard | count of members, events, competitions, scoreSessions |
| Member Progress list | each member name, email, latest score, practice count, league count |
| Member Progress detail | that member’s Practice and League sessions, graphs, end breakdown |
| Manage Members | name, email, role; delete member; open progress |
| Manage Events | create/edit/delete title, date, time, location, description, type |
| Manage Competitions | create/edit/delete name, date, deadline, venue, description, status |
| Manage Announcements | create/edit/delete title, content, date |
| Manage Resources | create/edit/delete title, category, description, link |
| Statistics | counts: members, events, competitions, scoreSessions, resources, announcements |

Password is never stored in Firestore. Firebase Auth stores it.

The old single `Score` number is not a collection. Use `scoreSessions` only.

---

## 2. Collections

| Collection | Document ID | Firestore types |
|---|---|---|
| `members` | Auth UID | strings + boolean |
| `memberProfiles` | Auth UID | strings |
| `events` | auto-id | strings |
| `competitions` | auto-id | strings |
| `scoreSessions` | auto-id | strings, numbers, boolean, arrays, maps |
| `announcements` | auto-id | strings |
| `notifications` | auto-id | strings + boolean |
| `resources` | auto-id | strings |

---

## 3. Field types

### `members/{uid}`

| Field | Firestore type | App type | Values |
|---|---|---|---|
| firstName | string | String | |
| lastName | string | String | |
| email | string | String | login email |
| role | string | String | `Member` or `Admin` |
| emailVerified | boolean | Boolean | true after verify screen |
| dateJoined | string | String | `YYYY-MM-DD` |

Used by: login routing, home greeting, profile, admin member list, admin delete member.

### `memberProfiles/{uid}`

| Field | Firestore type | App type | Values |
|---|---|---|---|
| memberId | string | String | same as uid |
| experienceLevel | string | String | Beginner / Intermediate / Advanced |
| bowType | string | String | Recurve / Compound |
| division | string | String | e.g. Recurve Women |
| emergencyContact | string | String | phone |

Used by: profile and edit profile only.

### `events/{eventId}`

| Field | Firestore type | App type | Values |
|---|---|---|---|
| title | string | String | |
| description | string | String | |
| eventDate | string | String | `YYYY-MM-DD` |
| eventTime | string | String | `HH:mm` |
| location | string | String | |
| type | string | String | `Practice` or `Event` |
| createdBy | string | String | admin uid |

Used by: member calendar + details, home featured event, admin manage events.  
Calendar chips: All / Practice / Events / Competitions. Competitions come from the other collection.

### `competitions/{competitionId}`

| Field | Firestore type | App type | Values |
|---|---|---|---|
| competitionName | string | String | |
| competitionDate | string | String | `YYYY-MM-DD` |
| registrationDeadline | string | String | `YYYY-MM-DD` |
| venue | string | String | |
| description | string | String | |
| status | string | String | `UPCOMING`, `REGISTRATION OPEN`, `CLOSED` |
| createdBy | string | String | admin uid |

Used by: home upcoming competition, calendar, competition list/details, admin manage competitions.

### `scoreSessions/{sessionId}`

This is the scoring database. One document = one finished Practice or League session.

| Field | Firestore type | App type | Values |
|---|---|---|---|
| memberId | string | String | archer Auth UID |
| type | string | enum | `PRACTICE` or `LEAGUE` |
| title | string | String | e.g. Saturday Practice |
| distanceMeters | number | Int | 10, 15, 18, 20, 25, 30, 40, 50, 60, 70 or custom |
| arrowsPerEnd | number | Int | Practice 3/6/custom. League always 6 |
| numberOfEnds | number | Int | Practice 5/8/10/12/custom. League always 10 |
| date | string | String | `YYYY-MM-DD` |
| notes | string | String | optional |
| totalScore | number | Int | calculated |
| maxScore | number | Int | arrows × 10 |
| averageArrow | number | Double | calculated |
| tensCount | number | Int | arrows with value 10, including X |
| xCount | number | Int | arrows with isX true |
| highestEnd | number | Int | |
| lowestEnd | number | Int | |
| ranking | number or null | Int? | League dummy rank, e.g. 2 |
| fieldSize | number or null | Int? | League field size, e.g. 12 |
| leagueName | string or null | String? | League only |
| ends | array of maps | List | see below |

Each `ends` item:

| Field | Firestore type | App type |
|---|---|---|
| endNumber | number | Int |
| total | number | Int |
| arrows | array of maps | List |

Each arrow:

| Field | Firestore type | App type | Rule |
|---|---|---|---|
| value | number | Int | 0 to 10 only |
| isX | boolean | Boolean | X = `{ value: 10, isX: true }` |

Used by:
- Member record / history / details / graphs
- Home latest, average, highest
- Admin member progress and statistics

Python calculates totals. Do not let the app send only one final score.

### `announcements/{announcementId}`

| Field | Firestore type | App type |
|---|---|---|
| title | string | String |
| content | string | String |
| datePosted | string | String `YYYY-MM-DD` |
| createdBy | string | String |

Used by: member announcements list, admin manage announcements.

### `notifications/{notificationId}`

| Field | Firestore type | App type |
|---|---|---|
| memberId | string | String |
| title | string | String |
| message | string | String |
| dateSent | string | String |
| isRead | boolean | Boolean |

Used by: home unread dot, notification inbox. Member sees only their `memberId`. Admin can create them.

### `resources/{resourceId}`

| Field | Firestore type | App type | Category values |
|---|---|---|---|
| title | string | String | |
| category | string | String | `Safety`, `Equipment`, `Technique`, `Scoring`, `Getting Started` |
| description | string | String | |
| resourceLink | string | String | URL |
| createdBy | string | String | |

Used by: member resources + details, admin manage resources.

---

## 4. Relationships

```
members/{uid}
    1 ── 1  memberProfiles/{uid}
    1 ── *  scoreSessions.memberId
    1 ── *  notifications.memberId

events            standalone, admin writes, members read
competitions      standalone, admin writes, members read
announcements     standalone, admin writes, members read
resources         standalone, admin writes, members read
```

`members.role` decides Member app vs Admin app.

---

## 5. Example documents

### Member

```json
{
  "firstName": "Aaliyah",
  "lastName": "Smit",
  "email": "member@habibia.co.za",
  "role": "Member",
  "emailVerified": true,
  "dateJoined": "2024-01-10"
}
```

### Practice session (shortened)

```json
{
  "memberId": "MEMBER_UID",
  "type": "PRACTICE",
  "title": "Saturday Practice",
  "distanceMeters": 18,
  "arrowsPerEnd": 6,
  "numberOfEnds": 10,
  "date": "2026-09-12",
  "notes": "Strong grouping",
  "totalScore": 524,
  "maxScore": 600,
  "averageArrow": 8.73,
  "tensCount": 12,
  "xCount": 3,
  "highestEnd": 55,
  "lowestEnd": 49,
  "ranking": null,
  "fieldSize": null,
  "leagueName": null,
  "ends": [
    {
      "endNumber": 1,
      "total": 52,
      "arrows": [
        { "value": 9, "isX": false },
        { "value": 8, "isX": false },
        { "value": 10, "isX": false },
        { "value": 9, "isX": false },
        { "value": 7, "isX": false },
        { "value": 9, "isX": false }
      ]
    }
  ]
}
```

Full seed documents are in `firebase/seed/`.

---

## 6. Share with the team

Send this file plus:

- `firebase/firestore.rules`
- `firebase/firestore.indexes.json`
- `firebase/seed/`
- `backend/API.md`

One person creates the Firebase project and collections from this schema. Everyone else codes to these field names only.
