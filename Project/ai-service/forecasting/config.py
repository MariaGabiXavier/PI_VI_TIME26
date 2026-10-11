"""Configurações lidas do arquivo .env (veja .env.example)."""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


def _path(value: str) -> Path:
    path = Path(value)
    return path if path.is_absolute() else (BASE_DIR / path).resolve()


@dataclass(frozen=True)
class Settings:
    data_source: str
    csv_path: Path
    firebase_key_path: Path
    cache_seconds: int
    history_days: int
    validation_days: int
    require_auth: bool


def get_settings() -> Settings:
    return Settings(
        data_source=os.getenv("DATA_SOURCE", "csv").strip().lower(),
        csv_path=_path(os.getenv("DATA_CSV_PATH", "../../tools/seed_output/occupancy_readings.csv")),
        firebase_key_path=_path(os.getenv("FIREBASE_KEY_PATH", "../../tools/serviceAccountKey.json")),
        cache_seconds=int(os.getenv("DATA_CACHE_SECONDS", "600")),
        history_days=int(os.getenv("HISTORY_DAYS", "60")),
        validation_days=int(os.getenv("VALIDATION_DAYS", "7")),
        require_auth=os.getenv("REQUIRE_AUTH", "false").strip().lower() == "true",
    )
