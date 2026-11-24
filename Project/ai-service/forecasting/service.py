"""Orquestra tudo: dados -> treino dos 3 modelos -> melhor -> previsão -> insights."""
from __future__ import annotations

import time
from datetime import datetime
from typing import List, Optional

import pandas as pd

from .data_source import LOCAL_TZ
from .insights import build_insights, day_label
from .ml_pipeline import (forecast_hours, prepare_observed, train_and_select)


def parse_now(value: Optional[str]) -> datetime:
    """Horário de referência. Padrão: agora. (?now=2026-10-12T08:00:00-03:00 serve
    para simular outro horário em testes/demonstrações.)"""
    if not value:
        return datetime.now(LOCAL_TZ)
    parsed = datetime.fromisoformat(value.strip().replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=LOCAL_TZ)
    return parsed.astimezone(LOCAL_TZ)


def _peak_info(point: dict, now: datetime) -> dict:
    return {"timestamp": point["ts"].isoformat(), "hour": int(point["ts"].hour),
            "occupancyPercent": point["occupancy"], "label": day_label(point["ts"], now)}


def run_forecast(readings: pd.DataFrame, now: datetime, horizon_hours: int = 48,
                 environment_id: Optional[str] = None, validation_days: int = 7) -> dict:
    started = time.perf_counter()

    observed = prepare_observed(readings, now)
    training = train_and_select(observed, validation_days)       # treina os 3 modelos
    predictions = forecast_hours(training, observed, now, horizon_hours)

    latest = (
    observed.sort_values("ts")
    .drop_duplicates(subset=["environmentId"], keep="last")
    .set_index("environmentId")
)

    forecasts: List[dict] = []
    # Retorna previsões para todo ambiente com ao menos uma leitura observada,
    # inclusive os que ainda não têm 8 dias para participar do treinamento.
    environment_ids = sorted(observed["environmentId"].dropna().unique().tolist())
    for env_id in environment_ids:
        points = predictions.get(env_id, [])
        # Os indicadores de pico e melhor horário representam somente o
        # restante do dia atual. O horizonte de 48h continua disponível em
        # "points" para o gráfico e demais análises.
        today_points = [
            point for point in points
            if point["ts"].date() == now.date() and point["ts"] > now
        ]
        capacity = int(latest.loc[env_id, "capacity"]) if env_id in latest.index else 0
        forecasts.append({
            "environmentId": env_id,
            "environmentName": str(latest.loc[env_id, "environmentName"]),
            "capacity": capacity,
            "points": [{
                "timestamp": p["ts"].isoformat(),
                "hour": int(p["ts"].hour),
                "predictedOccupancyPercent": p["occupancy"],
                "predictedPeople": int(round(p["occupancy"] / 100 * capacity)),
            } for p in points],
            "peak": _peak_info(max(today_points, key=lambda p: p["occupancy"]), now) if today_points else None,
            "bestTime": _peak_info(min(today_points, key=lambda p: p["occupancy"]), now) if today_points else None,
        })

    insights = build_insights(forecasts, training, now)          # sempre sobre todos os ambientes
    if environment_id:
        forecasts = [f for f in forecasts if f["environmentId"] == environment_id]

    return {
        "generatedAt": datetime.now(LOCAL_TZ).isoformat(),
        "now": now.isoformat(),
        "horizonHours": horizon_hours,
        "metric": "MAE",
        "trainingRows": training.train_rows,
        "validationRows": training.validation_rows,
        "validationDays": training.validation_days,
        "trainingSeconds": training.seconds,
        "totalSeconds": round(time.perf_counter() - started, 3),
        "bestModel": training.best_key,
        "bestModelLabel": training.best_label,
        "baselineMae": training.baseline_mae,
        "models": [{"key": s.key, "label": s.label, "mae": s.mae,
                    "trainSeconds": s.train_seconds, "isBest": s.key == training.best_key}
                   for s in training.scores],
        "forecasts": forecasts,
        "insights": insights,
    }
