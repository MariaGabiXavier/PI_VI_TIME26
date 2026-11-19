
"""Carrega e unifica leituras simuladas e das câmeras do FlowCampus.

Fonte simulada: occupancy_readings
Fonte das câmeras: camera_readings

Cada linha devolvida representa um ambiente em uma hora.
As capturas repetidas das câmeras são agrupadas pela mediana.
"""
from __future__ import annotations

import logging
import threading
import time
from datetime import datetime, timedelta, timezone

import pandas as pd

from .config import Settings


logger = logging.getLogger("flowcampus.ai")

# Horário de Brasília
LOCAL_TZ = timezone(timedelta(hours=-3))

SIMULATED_COLLECTION = "occupancy_readings"
CAMERA_COLLECTION = "camera_readings"

REQUIRED = [
    "environmentId",
    "environmentName",
    "capacity",
    "timestamp",
    "occupancyPercent",
]

OPTIONAL = [
    "people",
    "entries",
    "waitMinutes",
]

CAMERA_REQUIRED = [
    "environmentId",
    "environmentName",
    "people",
    "occupancyPercent",
    "timestamp",
]

_cache = {"df": None, "loaded_at": 0.0}
_lock = threading.Lock()
_firebase_app = None


def get_firebase_app(settings: Settings):
    """Inicializa o Firebase Admin uma única vez."""
    global _firebase_app

    if _firebase_app is None:
        import firebase_admin
        from firebase_admin import credentials

        try:
            _firebase_app = firebase_admin.get_app()

        except ValueError:
            if not settings.firebase_key_path.exists():
                raise FileNotFoundError(
                    f"Chave do Firebase não encontrada em "
                    f"{settings.firebase_key_path}. "
                    "Ajuste FIREBASE_KEY_PATH no .env."
                )

            cred = credentials.Certificate(
                str(settings.firebase_key_path)
            )

            _firebase_app = firebase_admin.initialize_app(cred)

    return _firebase_app


def standardize(raw: pd.DataFrame) -> pd.DataFrame:
    """Padroniza as leituras simuladas."""

    if raw.empty:
        return pd.DataFrame(
            columns=[
                "environmentId",
                "environmentName",
                "capacity",
                "ts",
                "occupancyPercent",
                "people",
                "entries",
                "waitMinutes",
            ]
        )

    missing = [
        column
        for column in REQUIRED
        if column not in raw.columns
    ]

    if missing:
        raise ValueError(
            f"Faltam colunas nos dados simulados: {missing}"
        )

    df = raw.reindex(
        columns=REQUIRED + OPTIONAL
    ).copy()

    df["ts"] = (
        pd.to_datetime(df["timestamp"], utc=True)
        .dt.tz_convert(LOCAL_TZ)
        .dt.floor("h")
    )

    df["occupancyPercent"] = pd.to_numeric(
        df["occupancyPercent"],
        errors="coerce"
    )

    df["capacity"] = pd.to_numeric(
        df["capacity"],
        errors="coerce"
    )

    for column in OPTIONAL:
        df[column] = pd.to_numeric(
            df[column],
            errors="coerce"
        )

    df["people"] = df["people"].fillna(
        df["capacity"] * df["occupancyPercent"] / 100
    )

    df["entries"] = df["entries"].fillna(0)
    df["waitMinutes"] = df["waitMinutes"].fillna(0)

    df = df.dropna(
        subset=[
            "environmentId",
            "occupancyPercent",
            "capacity",
            "ts",
        ]
    )

    df = df.drop(columns=["timestamp"])
    df = df.sort_values("ts")
    df = df.drop_duplicates(
        subset=["environmentId", "ts"],
        keep="last"
    )

    return df.reset_index(drop=True)


def standardize_camera(raw: pd.DataFrame) -> pd.DataFrame:
    """Agrupa as capturas das câmeras por ambiente e hora."""

    columns = [
        "environmentId",
        "environmentName",
        "ts",
        "cameraPeople",
        "cameraOccupancy",
        "cameraCapacity",
    ]

    if raw.empty:
        return pd.DataFrame(columns=columns)

    missing = [
        column
        for column in CAMERA_REQUIRED
        if column not in raw.columns
    ]

    if missing:
        raise ValueError(
            f"Faltam campos nas leituras das câmeras: {missing}"
        )

    df = raw.copy()

    df["environmentName"] = (
        df["environmentName"]
        .fillna("Ambiente")
    )

    df["ts"] = (
        pd.to_datetime(df["timestamp"], utc=True)
        .dt.tz_convert(LOCAL_TZ)
        .dt.floor("h")
    )

    df["people"] = pd.to_numeric(
        df["people"],
        errors="coerce"
    )

    df["occupancyPercent"] = pd.to_numeric(
        df["occupancyPercent"],
        errors="coerce"
    )

    if "capacity" not in df.columns:
        df["capacity"] = pd.NA

    df["capacity"] = pd.to_numeric(
        df["capacity"],
        errors="coerce"
    )

    df = df.dropna(
        subset=[
            "environmentId",
            "people",
            "occupancyPercent",
            "ts",
        ]
    )

    # Várias capturas da mesma hora não representam
    # pessoas diferentes. Por isso usamos a mediana.
    grouped = (
        df.groupby(
            ["environmentId", "ts"],
            as_index=False
        )
        .agg(
            environmentName=("environmentName", "last"),
            cameraPeople=("people", "median"),
            cameraOccupancy=("occupancyPercent", "median"),
            cameraCapacity=("capacity", "median"),
        )
    )

    # Quando a câmera não informa a capacidade,
    # estimamos pela relação entre pessoas e ocupação.
    estimated_capacity = (
        grouped["cameraPeople"] * 100
        / grouped["cameraOccupancy"].replace(0, pd.NA)
    )

    grouped["cameraCapacity"] = (
        grouped["cameraCapacity"]
        .fillna(estimated_capacity)
        .fillna(0)
    )

    return grouped[columns].reset_index(drop=True)


def combine_sources(
    simulated: pd.DataFrame,
    camera: pd.DataFrame
) -> pd.DataFrame:
    """Combina as duas fontes em uma leitura por ambiente e hora."""

    if simulated.empty and camera.empty:
        return pd.DataFrame(
            columns=[
                "environmentId",
                "environmentName",
                "capacity",
                "ts",
                "occupancyPercent",
                "entries",
                "waitMinutes",
            ]
        )

    simulated = simulated.rename(
        columns={
            "environmentName": "simulatedName",
            "capacity": "simulatedCapacity",
            "people": "simulatedPeople",
            "occupancyPercent": "simulatedOccupancy",
        }
    )

    merged = simulated.merge(
        camera,
        on=["environmentId", "ts"],
        how="outer",
    )

    merged["environmentName"] = (
        merged["simulatedName"]
        .fillna(merged["environmentName"])
        .fillna("Ambiente")
    )

    merged["capacity"] = (
        merged["simulatedCapacity"].fillna(0)
        + merged["cameraCapacity"].fillna(0)
    )

    simulated_people = merged["simulatedPeople"].fillna(0)
    camera_people = merged["cameraPeople"].fillna(0)

    total_people = simulated_people + camera_people

    has_simulated = merged["simulatedOccupancy"].notna()
    has_camera = merged["cameraOccupancy"].notna()

    # Se as duas fontes representam áreas distintas do mesmo
    # ambiente, somamos as pessoas e calculamos a ocupação.
    calculated_occupancy = (
        total_people * 100
        / merged["capacity"].replace(0, pd.NA)
    )


    has_capacity = merged["capacity"] > 0

    # Converte explicitamente para float64 e calcula
    # a ocupação sem atribuições incompatíveis de tipos.
    occupancy = pd.Series(
        0.0,
        index=merged.index,
        dtype="float64"
    )

    calculated_occupancy = pd.to_numeric(
        calculated_occupancy,
        errors="coerce"
    ).astype("float64")

    occupancy = occupancy.where(
        ~has_capacity,
        calculated_occupancy
    )


    # Sem capacidade conhecida, preservamos a ocupação
    # informada pela única fonte disponível.
    only_simulated = has_simulated & ~has_camera
    only_camera = has_camera & ~has_simulated
    both_without_capacity = (
        has_simulated & has_camera & ~has_capacity
    )

    occupancy.loc[only_simulated] = (
        merged.loc[only_simulated, "simulatedOccupancy"]
    )

    occupancy.loc[only_camera] = (
        merged.loc[only_camera, "cameraOccupancy"]
    )

    occupancy.loc[both_without_capacity] = (
        merged.loc[both_without_capacity, [
            "simulatedOccupancy",
            "cameraOccupancy",
        ]].max(axis=1)
    )

    result = pd.DataFrame({
        "environmentId": merged["environmentId"],
        "environmentName": merged["environmentName"],
        "capacity": merged["capacity"],
        "ts": merged["ts"],
        "occupancyPercent": occupancy.clip(0, 100),
        "entries": merged["entries"].fillna(0),
        "waitMinutes": merged["waitMinutes"].fillna(0),
        "people": total_people,
    })

    result = result.dropna(
        subset=[
            "environmentId",
            "ts",
            "occupancyPercent",
        ]
    )

    return (
        result.sort_values(["environmentId", "ts"])
        .reset_index(drop=True)
    )


def _read_firestore(settings: Settings) -> pd.DataFrame:
    """Busca as duas coleções no Firestore e unifica os dados."""

    from firebase_admin import firestore
    from google.cloud.firestore_v1.base_query import FieldFilter

    db = firestore.client(
        get_firebase_app(settings)
    )

    cutoff = (
        datetime.now(timezone.utc)
        - timedelta(days=settings.history_days)
    )

    # ------------------------------------------
    # LEITURAS SIMULADAS
    # ------------------------------------------

    simulated_query = (
        db.collection(SIMULATED_COLLECTION)
        .where(
            filter=FieldFilter(
                "timestamp",
                ">=",
                cutoff
            )
        )
        .select(REQUIRED + OPTIONAL)
    )

    simulated_rows = [
        doc.to_dict()
        for doc in simulated_query.stream()
    ]

    simulated = standardize(
        pd.DataFrame(
            simulated_rows,
            columns=REQUIRED + OPTIONAL
        )
    )

    # ------------------------------------------
    # LEITURAS DAS CÂMERAS
    # ------------------------------------------

    camera_query = (
        db.collection(CAMERA_COLLECTION)
        .where(
            filter=FieldFilter(
                "timestamp",
                ">=",
                cutoff
            )
        )
        .select(CAMERA_REQUIRED + ["capacity"])
    )

    camera_rows = [
        doc.to_dict()
        for doc in camera_query.stream()
    ]

    camera = standardize_camera(
        pd.DataFrame(camera_rows)
    )

    # ------------------------------------------
    # UNIFICAÇÃO
    # ------------------------------------------

    combined = combine_sources(
        simulated,
        camera
    )

    logger.info(
        "Dados carregados do Firestore: "
        "%s leituras simuladas | "
        "%s capturas de câmera | "
        "%s registros horários unificados",
        len(simulated),
        len(camera_rows),
        len(combined),
    )

    return combined


def _read_csv(settings: Settings) -> pd.DataFrame:
    """Mantém o modo CSV para testes locais."""

    if not settings.csv_path.exists():
        raise FileNotFoundError(
            f"CSV não encontrado em {settings.csv_path}. "
            "Ajuste DATA_CSV_PATH no .env."
        )

    return standardize(
        pd.read_csv(settings.csv_path)
    )


def load_readings(
    settings: Settings,
    force: bool = False
) -> pd.DataFrame:
    """Devolve os dados unificados, respeitando o tempo de cache."""

    with _lock:
        age = time.time() - _cache["loaded_at"]

        if (
            force
            or _cache["df"] is None
            or age > settings.cache_seconds
        ):

            if settings.data_source == "firestore":
                _cache["df"] = _read_firestore(settings)

            elif settings.data_source == "csv":
                _cache["df"] = _read_csv(settings)

            else:
                raise ValueError(
                    "DATA_SOURCE deve ser 'csv' ou 'firestore'."
                )

            _cache["loaded_at"] = time.time()

        return _cache["df"].copy()


def clear_cache() -> None:
    """Limpa o cache dos dados."""

    with _lock:
        _cache["df"] = None
        _cache["loaded_at"] = 0.0
