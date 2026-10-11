"""Carrega as leituras de ocupação (CSV local ou Firestore) em um DataFrame.

Colunas devolvidas:
  environmentId, environmentName, capacity, ts (horário local, com fuso),
  occupancyPercent, entries, waitMinutes
"""
from __future__ import annotations

import threading
import time
from datetime import datetime, timedelta, timezone
from typing import Optional

import pandas as pd

from .config import Settings

# Horário de Brasília (sem horário de verão desde 2019)
LOCAL_TZ = timezone(timedelta(hours=-3))

COLLECTION = "occupancy_readings"
REQUIRED = ["environmentId", "environmentName", "capacity", "timestamp", "occupancyPercent"]
# opcionais (usadas pelo /predict); se faltarem, viram 0
OPTIONAL = ["entries", "waitMinutes"]

_cache = {"df": None, "loaded_at": 0.0}
_lock = threading.Lock()
_firebase_app = None


def standardize(raw: pd.DataFrame) -> pd.DataFrame:
    """Valida colunas, converte o fuso e remove duplicados."""
    missing = [c for c in REQUIRED if c not in raw.columns]
    if missing:
        raise ValueError(f"Faltam colunas nos dados de ocupação: {missing}")

    df = raw.reindex(columns=REQUIRED + OPTIONAL).copy()
    df["ts"] = pd.to_datetime(df["timestamp"], utc=True).dt.tz_convert(LOCAL_TZ)
    df["occupancyPercent"] = pd.to_numeric(df["occupancyPercent"], errors="coerce")
    df["capacity"] = pd.to_numeric(df["capacity"], errors="coerce")
    for column in OPTIONAL:
        df[column] = pd.to_numeric(df[column], errors="coerce").fillna(0.0)
    df = df.dropna(subset=["occupancyPercent", "environmentId"])
    df = df.drop(columns=["timestamp"])
    df = df.drop_duplicates(subset=["environmentId", "ts"], keep="last")
    return df.reset_index(drop=True)


def get_firebase_app(settings: Settings):
    """Inicializa o Firebase Admin uma única vez (usado pelo Firestore e pelo login)."""
    global _firebase_app
    if _firebase_app is None:
        import firebase_admin
        from firebase_admin import credentials

        try:
            _firebase_app = firebase_admin.get_app()
        except ValueError:
            if not settings.firebase_key_path.exists():
                raise FileNotFoundError(
                    f"Chave do Firebase não encontrada em {settings.firebase_key_path}. "
                    "Ajuste FIREBASE_KEY_PATH no .env."
                )
            cred = credentials.Certificate(str(settings.firebase_key_path))
            _firebase_app = firebase_admin.initialize_app(cred)
    return _firebase_app


def _read_csv(settings: Settings) -> pd.DataFrame:
    if not settings.csv_path.exists():
        raise FileNotFoundError(
            f"CSV não encontrado em {settings.csv_path}. Rode o seed_firestore.py "
            "(--dry-run) ou ajuste DATA_CSV_PATH no .env."
        )
    return standardize(pd.read_csv(settings.csv_path))


def _read_firestore(settings: Settings) -> pd.DataFrame:
    from firebase_admin import firestore
    from google.cloud.firestore_v1.base_query import FieldFilter

    db = firestore.client(get_firebase_app(settings))
    cutoff = datetime.now(timezone.utc) - timedelta(days=settings.history_days)
    query = (
        db.collection(COLLECTION)
        .where(filter=FieldFilter("timestamp", ">=", cutoff))
        .select(REQUIRED + OPTIONAL)
    )
    rows = [doc.to_dict() for doc in query.stream()]
    return standardize(pd.DataFrame(rows, columns=REQUIRED + OPTIONAL))


def load_readings(settings: Settings, force: bool = False) -> pd.DataFrame:
    """Devolve as leituras. Os dados (não os modelos!) ficam em cache por
    DATA_CACHE_SECONDS para não gastar a cota de leituras do Firestore."""
    with _lock:
        age = time.time() - _cache["loaded_at"]
        if force or _cache["df"] is None or age > settings.cache_seconds:
            if settings.data_source == "firestore":
                _cache["df"] = _read_firestore(settings)
            elif settings.data_source == "csv":
                _cache["df"] = _read_csv(settings)
            else:
                raise ValueError("DATA_SOURCE deve ser 'csv' ou 'firestore'.")
            _cache["loaded_at"] = time.time()
        return _cache["df"].copy()


def clear_cache() -> None:
    with _lock:
        _cache["df"] = None
        _cache["loaded_at"] = 0.0
