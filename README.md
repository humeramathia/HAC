# Habibia Archery Club Management System

Front-end Android prototype for Habibia Archery Club (HAC).

This project demonstrates the UI, navigation and user flows for a club management app. It uses local dummy data only and does not include a backend, database, Firebase, APIs or authentication services.

## Team

- Humera Mathia - ST10276384
- David Minlah - ST10442249
- Imraan Noordien - ST10434419
- Siddiq Malik - ST10455630

## Tech Stack

- Kotlin
- XML layouts
- Fragments
- Material Design 3
- Android Studio
- Local / in-memory dummy data

## Features

### Member
- Splash and branded login
- Registration and email verification (prototype flow)
- Home dashboard
- Club calendar and event details
- Competitions
- Score tracking and progress
- Beginner resources
- Profile and edit profile
- Notifications

### Admin
- Admin dashboard
- Manage members
- Manage events
- Manage competitions
- Manage announcements
- Manage resources
- Statistics

## Design System

Brand colours aligned with [haclub.co.za](https://haclub.co.za/):

| Token | Colour | Use |
|-------|--------|-----|
| Primary green | `#9ABA55` | Buttons, accents, selected states |
| Secondary blue | `#528FD0` | Headings, links, secondary actions |
| Alert red | `#FE3B3A` | Errors, delete, unread |
| Background | `#FAFBFC` | Screen backgrounds |
| Surface | `#FFFFFF` | Cards and inputs |

Assets used:
- `logo.png` – Habibia Archery Club logo
- `desgin.png` – geometric edge decoration pattern

## How to Run

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run on an emulator or physical device.
4. Use **Demo Member** or **Demo Admin** on the login screen to explore the prototype.

## Project Structure

```text
Prototype/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/hacprototype/
│       │   │   ├── MainActivity.kt
│       │   │   ├── HabibiaData.kt
│       │   │   ├── HabibiaFragments.kt
│       │   │   └── HabibiaUi.kt
│       │   └── res/
│       │       ├── color/
│       │       │   └── habibia_bottom_nav_color.xml
│       │       ├── drawable/
│       │       │   ├── logo.png
│       │       │   ├── desgin.png
│       │       │   ├── bg_habibia_input.xml
│       │       │   ├── bg_habibia_button_*.xml
│       │       │   ├── fade_to_*.xml
│       │       │   ├── ic_nav_*.xml
│       │       │   └── ...
│       │       ├── layout/
│       │       │   ├── activity_main.xml
│       │       │   ├── fragment_splash.xml
│       │       │   ├── fragment_login.xml
│       │       │   ├── fragment_member_host.xml
│       │       │   ├── fragment_member_dashboard.xml
│       │       │   ├── fragment_scores.xml
│       │       │   ├── fragment_admin_dashboard.xml
│       │       │   ├── include_edge_decor*.xml
│       │       │   ├── item_*.xml
│       │       │   └── ...
│       │       ├── menu/
│       │       │   └── menu_member_bottom.xml
│       │       ├── mipmap-*/
│       │       ├── values/
│       │       │   ├── colors.xml
│       │       │   ├── dimens.xml
│       │       │   ├── strings.xml
│       │       │   ├── styles.xml
│       │       │   └── themes.xml
│       │       ├── values-night/
│       │       │   └── themes.xml
│       │       └── xml/
│       ├── test/
│       │   └── java/com/example/hacprototype/
│       │       └── ExampleUnitTest.kt
│       └── androidTest/
│           └── java/com/example/hacprototype/
│               └── ExampleInstrumentedTest.kt
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew.bat
```

## Key Source Files

| File | Description |
|------|-------------|
| `MainActivity.kt` | Host activity and fragment navigation |
| `HabibiaData.kt` | Data models and local dummy data |
| `HabibiaFragments.kt` | App screens and clickable prototype flows |
| `HabibiaUi.kt` | Shared UI helpers (snackbars, chips, formatting) |
| `colors.xml` / `styles.xml` / `themes.xml` | Habibia design system |
| `menu_member_bottom.xml` | Member bottom navigation |

## Member Navigation

Bottom navigation:

- Home
- Calendar
- Scores
- Resources
- Profile

## Prototype Notes

- This is a UI/UX prototype only.
- All interactions are simulated locally.
- No Firebase, SQL, Room, APIs or cloud services are used.
- No real authentication or data persistence is implemented.

## Licence / Academic Use

Developed for academic assessment as a software development project for Habibia Archery Club.
