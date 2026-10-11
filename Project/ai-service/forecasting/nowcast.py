"""Tabela de treino do /predict: "dado o que está acontecendo AGORA, quanto
estará a ocupação na PRÓXIMA hora?"  (mesmas colunas do seu occupancy_data.csv)

Cada linha = uma leitura horária de um ambiente. O alvo (target_next_occupancy)
é a ocupação da leitura da hora seguinte do mesmo ambiente.
"""
from __future__ import annotations

import unicodedata
from datetime import datetime

import pandas as pd

from .ml_pipeline import prepare_observed


def normalize_name(text: str) -> str:
    """minúsculo e sem acento: 'Refeitório' == 'refeitorio'."""
    text = unicodedata.normalize("NFD", text or "")
    return "".join(c for c in text if unicodedata.category(c) != "Mn").strip().lower()


def build_nowcast_table(readings: pd.DataFrame, now: datetime) -> pd.DataFrame:
    observed = prepare_observed(readings, now)   # só o que já aconteceu (sem "futuro" do seed)

    target = observed[["environmentId", "ts", "occupancyPercent"]].copy()
    target["ts"] = target["ts"] - pd.Timedelta(hours=1)          # a leitura da hora seguinte
    target = target.rename(columns={"occupancyPercent": "target_next_occupancy"})

    table = observed.merge(target, on=["environmentId", "ts"], how="inner")
    weekday = table["ts"].dt.dayofweek

    return pd.DataFrame({
        "timestamp": table["ts"],
        "environment": table["environmentName"].astype(str),
        "hour": table["ts"].dt.hour,
        "day_of_week": weekday,                                    # 0 = segunda
        "is_weekend": (weekday >= 5).astype(int),
        "current_occupancy": table["occupancyPercent"].astype(float),
        "current_visitors": table["entries"].round().astype(int),
        "current_wait_minutes": table["waitMinutes"].astype(float),
        "target_next_occupancy": table["target_next_occupancy"].astype(float),
    }).sort_values("timestamp").reset_index(drop=True)
