"""Formato (JSON) da resposta da API. Os nomes em camelCase são os que o app Kotlin lê."""
from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel


class ModelScoreOut(BaseModel):
    key: str
    label: str
    mae: float
    trainSeconds: float
    isBest: bool


class ForecastPoint(BaseModel):
    timestamp: str
    hour: int
    predictedOccupancyPercent: int
    predictedPeople: int


class MomentOut(BaseModel):
    timestamp: str
    hour: int
    occupancyPercent: int
    label: str  # "hoje", "amanhã" ou o dia da semana


class EnvironmentForecast(BaseModel):
    environmentId: str
    environmentName: str
    capacity: int
    points: List[ForecastPoint]
    peak: Optional[MomentOut] = None
    bestTime: Optional[MomentOut] = None


class InsightOut(BaseModel):
    text: str
    highlight: str
    icon: str
    tone: str


class ForecastResponse(BaseModel):
    generatedAt: str
    now: str
    horizonHours: int
    metric: str
    trainingRows: int
    validationRows: int
    validationDays: int
    trainingSeconds: float
    totalSeconds: float
    bestModel: str
    bestModelLabel: str
    baselineMae: float
    models: List[ModelScoreOut]
    forecasts: List[EnvironmentForecast]
    insights: List[InsightOut]
