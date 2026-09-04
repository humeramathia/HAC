from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

load_dotenv(Path(__file__).resolve().parent / ".env")

from firebase_client import init_firebase
from routers import admin, auth, competitions, content, events, profile, scores

init_firebase()

STATIC_DIR = Path(__file__).resolve().parent / "static"

app = FastAPI(
    title="Habibia Archery Club API",
    swagger_ui_parameters={"persistAuthorization": True},
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(profile.router)
app.include_router(events.router)
app.include_router(competitions.router)
app.include_router(scores.router)
app.include_router(admin.router)
app.include_router(content.announcements_router)
app.include_router(content.notifications_router)
app.include_router(content.resources_router)


@app.get("/")
def root():
    return {"ok": True, "docs": "/docs", "tester": "/tester"}


@app.get("/tester")
def tester():
    return FileResponse(STATIC_DIR / "tester.html")
