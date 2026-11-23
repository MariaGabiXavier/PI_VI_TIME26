
"""Treino, avaliação (MAE) e previsão com KNN, SVM (SVR) e Random Forest.

Os três modelos são treinados com o histórico e comparados pelo MAE.
O vencedor é retreinado com todos os dados disponíveis e utilizado
para prever a ocupação das próximas horas.
"""
from __future__ import annotations

import time
from dataclasses import dataclass, field
from datetime import datetime
from typing import Callable, Dict, List, Tuple

import numpy as np
import pandas as pd
from sklearn.compose import TransformedTargetRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.neighbors import KNeighborsRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVR


MIN_TRAIN_ROWS = 300
MIN_VALIDATION_ROWS = 30

# Quantidade mínima de registros para utilizar a mediana histórica.
MIN_WEEKDAY_HOUR_SAMPLES = 2
MIN_HOUR_SAMPLES = 3


class NotEnoughDataError(Exception):
    """Histórico insuficiente para treinar e validar os modelos."""


# ---------------------------------------------------------------------------
# Os três modelos
# ---------------------------------------------------------------------------
def _make_knn():
    return Pipeline([
        ("scale", StandardScaler()),
        ("model", KNeighborsRegressor(n_neighbors=10, weights="distance")),
    ])


def _make_svm():
    return TransformedTargetRegressor(
        regressor=Pipeline([
            ("scale", StandardScaler()),
            ("model", SVR(kernel="rbf", C=10.0, epsilon=0.05, gamma="scale")),
        ]),
        transformer=StandardScaler(),
    )


def _make_random_forest():
    return RandomForestRegressor(
        n_estimators=200,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1,
    )


MODEL_FACTORIES: Dict[str, Tuple[str, Callable]] = {
    "knn": ("KNN", _make_knn),
    "svm": ("SVM", _make_svm),
    "random_forest": ("Random Forest", _make_random_forest),
}


@dataclass
class ModelScore:
    key: str
    label: str
    mae: float
    train_seconds: float


@dataclass
class TrainingResult:
    best_key: str
    best_label: str
    scores: List[ModelScore]
    baseline_mae: float
    train_rows: int
    validation_rows: int
    validation_days: int
    env_ids: List[str]
    model: object = field(repr=False, default=None)
    seconds: float = 0.0


# ---------------------------------------------------------------------------
# Preparação dos dados
# ---------------------------------------------------------------------------
def floor_hour(moment: datetime) -> datetime:
    return moment.replace(minute=0, second=0, microsecond=0)


def prepare_observed(readings: pd.DataFrame, now: datetime) -> pd.DataFrame:
    """Mantém somente as leituras que já aconteceram."""
    limit = pd.Timestamp(floor_hour(now))

    observed = readings[readings["ts"] <= limit].copy()

    return observed.sort_values(
        ["environmentId", "ts"]
    ).reset_index(drop=True)


def add_lags(observed: pd.DataFrame) -> pd.DataFrame:
    """Acrescenta a ocupação da mesma hora há 7 e 14 dias."""
    keys = observed[["environmentId", "ts", "occupancyPercent"]]
    out = observed.copy()

    for days in (7, 14):
        shifted = keys.copy()
        shifted["ts"] = shifted["ts"] + pd.Timedelta(days=days)
        shifted = shifted.rename(
            columns={"occupancyPercent": f"lag_{days}d"}
        )

        out = out.merge(
            shifted,
            on=["environmentId", "ts"],
            how="left",
        )

    # Quando não há 14 dias, utiliza o histórico de 7 dias.
    out["lag_14d"] = out["lag_14d"].fillna(out["lag_7d"])

    return out


def build_features(frame: pd.DataFrame, env_ids: List[str]) -> pd.DataFrame:
    hour = frame["ts"].dt.hour.to_numpy()
    weekday = frame["ts"].dt.dayofweek.to_numpy()
    env = frame["environmentId"].to_numpy()

    lag7 = frame["lag_7d"].to_numpy(dtype=float)
    lag14 = frame["lag_14d"].to_numpy(dtype=float)

    columns = {
        f"env_{e}": (env == e).astype(float)
        for e in env_ids
    }

    columns["hour_sin"] = np.sin(2 * np.pi * hour / 24)
    columns["hour_cos"] = np.cos(2 * np.pi * hour / 24)
    columns["dow_sin"] = np.sin(2 * np.pi * weekday / 7)
    columns["dow_cos"] = np.cos(2 * np.pi * weekday / 7)
    columns["is_weekend"] = (weekday >= 5).astype(float)
    columns["lag_7d"] = lag7
    columns["lag_14d"] = lag14
    columns["lag_diff"] = lag7 - lag14

    return pd.DataFrame(columns, index=frame.index)


# ---------------------------------------------------------------------------
# Treino e escolha do melhor modelo
# ---------------------------------------------------------------------------
def train_and_select(
    observed: pd.DataFrame,
    validation_days: int = 7,
) -> TrainingResult:

    started = time.perf_counter()

    data = add_lags(observed).dropna(subset=["lag_7d"])

    if data.empty:
        raise NotEnoughDataError(
            "Ainda não há 8 dias de histórico para calcular "
            "a ocupação da semana passada."
        )

    env_ids = sorted(data["environmentId"].unique().tolist())

    last = data["ts"].max()
    span_days = (last - data["ts"].min()).days
    val_days = max(1, min(validation_days, span_days // 4))

    cut = last - pd.Timedelta(days=val_days)

    train = data[data["ts"] <= cut]
    valid = data[data["ts"] > cut]

    if (
        len(train) < MIN_TRAIN_ROWS
        or len(valid) < MIN_VALIDATION_ROWS
    ):
        raise NotEnoughDataError(
            f"Dados insuficientes: {len(train)} linhas de treino e "
            f"{len(valid)} de validação (mínimo "
            f"{MIN_TRAIN_ROWS} e {MIN_VALIDATION_ROWS}). "
            "É necessário mais histórico."
        )

    x_train = build_features(train, env_ids)
    x_valid = build_features(valid, env_ids)

    y_train = train["occupancyPercent"].to_numpy(dtype=float)
    y_valid = valid["occupancyPercent"].to_numpy(dtype=float)

    lag_train = train["lag_7d"].to_numpy(dtype=float)
    lag_valid = valid["lag_7d"].to_numpy(dtype=float)

    scores: List[ModelScore] = []

    for key, (label, factory) in MODEL_FACTORIES.items():
        t0 = time.perf_counter()

        model = factory()

        # O modelo aprende a correção em relação à semana passada.
        model.fit(x_train, y_train - lag_train)

        predicted = np.clip(
            lag_valid + model.predict(x_valid),
            0,
            100,
        )

        mae = mean_absolute_error(y_valid, predicted)

        scores.append(
            ModelScore(
                key=key,
                label=label,
                mae=round(float(mae), 3),
                train_seconds=round(time.perf_counter() - t0, 3),
            )
        )

    # O modelo com menor MAE vence.
    best = min(scores, key=lambda s: s.mae)

    # Retreinamento do vencedor com todo o histórico.
    final = MODEL_FACTORIES[best.key][1]()

    final.fit(
        build_features(data, env_ids),
        data["occupancyPercent"].to_numpy(dtype=float)
        - data["lag_7d"].to_numpy(dtype=float),
    )

    return TrainingResult(
        best_key=best.key,
        best_label=best.label,
        scores=scores,
        baseline_mae=round(
            float(mean_absolute_error(y_valid, lag_valid)),
            3,
        ),
        train_rows=len(train),
        validation_rows=len(valid),
        validation_days=val_days,
        env_ids=env_ids,
        model=final,
        seconds=round(time.perf_counter() - started, 3),
    )


# ---------------------------------------------------------------------------
# Histórico alternativo para previsão
# ---------------------------------------------------------------------------
def historical_baseline(
    environment_history: pd.DataFrame,
    moment: pd.Timestamp,
) -> float | None:
    """Busca uma referência histórica confiável para o horário."""

    same_weekday_hour = environment_history[
        (environment_history["ts"].dt.dayofweek == moment.dayofweek)
        & (environment_history["ts"].dt.hour == moment.hour)
    ]

    if len(same_weekday_hour) >= MIN_WEEKDAY_HOUR_SAMPLES:
        return float(
            same_weekday_hour["occupancyPercent"].median()
        )

    same_hour = environment_history[
        environment_history["ts"].dt.hour == moment.hour
    ]

    if len(same_hour) >= MIN_HOUR_SAMPLES:
        return float(
            same_hour["occupancyPercent"].median()
        )

    return None


# ---------------------------------------------------------------------------
# Previsão das próximas horas
# ---------------------------------------------------------------------------
def forecast_hours(
    result: TrainingResult,
    observed: pd.DataFrame,
    now: datetime,
    horizon_hours: int,
) -> Dict[str, List[dict]]:
    """Prevê a ocupação das próximas horas de funcionamento de cada ambiente."""

    if observed.empty:
        return {}

    # Histórico organizado por ambiente e horário.
    known = dict(
        zip(
            zip(observed["environmentId"], observed["ts"]),
            observed["occupancyPercent"],
        )
    )

    reference = pd.Timestamp(floor_hour(now))
    recent_start = reference - pd.Timedelta(days=14)

    first = reference + pd.Timedelta(hours=1)

    rows = []

    for env_id in result.env_ids:

        environment_history = observed[
            observed["environmentId"] == env_id
        ].copy()

        if environment_history.empty:
            continue

        # O gráfico do FlowCampus tem uma grade padronizada para todos
        # os ambientes: intervalos de 07:00 até 22:00 (07-08 ... 22-23).
        # Não filtrar pela presença de leituras recentes em cada hora,
        # pois isso fazia cada ambiente receber quantidades diferentes de pontos.
        for k in range(horizon_hours):

            moment = first + pd.Timedelta(hours=k)

            if moment.hour not in range(7, 23):
                continue

            lag7 = known.get(
                (env_id, moment - pd.Timedelta(days=7))
            )

            lag14 = known.get(
                (env_id, moment - pd.Timedelta(days=14))
            )

            # Prioridade: 7 dias, 14 dias e histórico alternativo.
            if lag7 is not None:
                baseline = float(lag7)

                lag14_feature = (
                    float(lag14)
                    if lag14 is not None
                    else baseline
                )

            elif lag14 is not None:
                baseline = float(lag14)
                lag14_feature = baseline

            else:
                baseline = historical_baseline(
                    environment_history,
                    moment,
                )

                # Fallback determinístico do próprio ambiente: quando não há
                # amostras suficientes para aquele horário, usa a mediana
                # histórica geral do ambiente. Não cria dados aleatórios.
                if baseline is None:
                    baseline = float(
                        environment_history["occupancyPercent"].median()
                    )

                lag14_feature = baseline

            rows.append({
                "environmentId": env_id,
                "ts": moment,
                "lag_7d": baseline,
                "lag_14d": lag14_feature,
            })

    if not rows:
        return {}

    frame = pd.DataFrame(rows)

    # Previsão da correção aprendida pelo modelo vencedor.
    features = build_features(frame, result.env_ids)

    delta = result.model.predict(features)

    frame["predicted"] = np.clip(
        frame["lag_7d"].to_numpy(dtype=float) + delta,
        0,
        100,
    )

    output: Dict[str, List[dict]] = {}

    for env_id, group in frame.groupby("environmentId"):

        output[env_id] = [
            {
                "ts": row.ts,
                "occupancy": int(round(row.predicted)),
            }
            for row in group.sort_values("ts").itertuples()
        ]

    return output
