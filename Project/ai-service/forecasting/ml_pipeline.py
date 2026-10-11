"""Treino, avaliação (MAE) e previsão com KNN, SVM (SVR) e Random Forest.

Ideia em uma frase: a cada requisição os 3 modelos são treinados do zero com o
histórico, comparados pelo MAE em dados que eles NÃO viram (os últimos dias), o
de menor erro é escolhido, retreinado com todo o histórico e usado para prever
a ocupação das próximas horas.

O que o modelo prevê
--------------------
Alvo: ocupação (% da capacidade) de um ambiente em uma hora.
Para facilitar o aprendizado, o modelo prevê a CORREÇÃO em relação à mesma hora
da semana passada:  previsão = ocupação_há_7_dias + correção_aprendida.

Variáveis de entrada (features)
-------------------------------
- ambiente (uma coluna 0/1 para cada ambiente)
- hora do dia e dia da semana (em seno/cosseno, porque 23h e 0h são "vizinhas")
- fim de semana (0/1)
- ocupação na mesma hora há 7 dias e há 14 dias (e a diferença entre elas)
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


class NotEnoughDataError(Exception):
    """Histórico insuficiente para treinar e validar os modelos."""


# ---------------------------------------------------------------------------
# Os três modelos (hiperparâmetros fixos e simples)
# ---------------------------------------------------------------------------
def _make_knn():
    # KNN: olha as 10 horas mais parecidas no histórico e faz a média
    # (as mais próximas pesam mais). Precisa de dados na mesma escala.
    return Pipeline([
        ("scale", StandardScaler()),
        ("model", KNeighborsRegressor(n_neighbors=10, weights="distance")),
    ])


def _make_svm():
    # SVM para regressão (SVR) com kernel RBF. Entradas e alvo padronizados.
    return TransformedTargetRegressor(
        regressor=Pipeline([
            ("scale", StandardScaler()),
            ("model", SVR(kernel="rbf", C=10.0, epsilon=0.05, gamma="scale")),
        ]),
        transformer=StandardScaler(),
    )


def _make_random_forest():
    # Random Forest: 200 árvores de decisão que "votam" na previsão.
    return RandomForestRegressor(
        n_estimators=200, min_samples_leaf=2, random_state=42, n_jobs=-1
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
    baseline_mae: float          # "ocupação da semana passada" como previsão
    train_rows: int
    validation_rows: int
    validation_days: int
    env_ids: List[str]
    model: object = field(repr=False, default=None)  # vencedor, treinado com tudo
    seconds: float = 0.0


# ---------------------------------------------------------------------------
# Preparação dos dados
# ---------------------------------------------------------------------------
def floor_hour(moment: datetime) -> datetime:
    return moment.replace(minute=0, second=0, microsecond=0)


def prepare_observed(readings: pd.DataFrame, now: datetime) -> pd.DataFrame:
    """Só o que já aconteceu até agora. Leituras "do futuro" (o seed gera o dia
    inteiro) ficam de fora, senão o modelo estaria colando."""
    limit = pd.Timestamp(floor_hour(now))
    observed = readings[readings["ts"] <= limit].copy()
    return observed.sort_values(["environmentId", "ts"]).reset_index(drop=True)


def add_lags(observed: pd.DataFrame) -> pd.DataFrame:
    """Acrescenta a ocupação da mesma hora há 7 e 14 dias (lag_7d, lag_14d)."""
    keys = observed[["environmentId", "ts", "occupancyPercent"]]
    out = observed.copy()
    for days in (7, 14):
        shifted = keys.copy()
        shifted["ts"] = shifted["ts"] + pd.Timedelta(days=days)
        shifted = shifted.rename(columns={"occupancyPercent": f"lag_{days}d"})
        out = out.merge(shifted, on=["environmentId", "ts"], how="left")
    out["lag_14d"] = out["lag_14d"].fillna(out["lag_7d"])
    return out


def build_features(frame: pd.DataFrame, env_ids: List[str]) -> pd.DataFrame:
    hour = frame["ts"].dt.hour.to_numpy()
    weekday = frame["ts"].dt.dayofweek.to_numpy()  # 0 = segunda
    env = frame["environmentId"].to_numpy()
    lag7 = frame["lag_7d"].to_numpy(dtype=float)
    lag14 = frame["lag_14d"].to_numpy(dtype=float)

    columns = {f"env_{e}": (env == e).astype(float) for e in env_ids}
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
# Treino + escolha do melhor modelo
# ---------------------------------------------------------------------------
def train_and_select(observed: pd.DataFrame, validation_days: int = 7) -> TrainingResult:
    started = time.perf_counter()

    data = add_lags(observed).dropna(subset=["lag_7d"])
    if data.empty:
        raise NotEnoughDataError(
            "Ainda não há 8 dias de histórico para calcular a ocupação da semana passada."
        )

    env_ids = sorted(data["environmentId"].unique().tolist())
    last = data["ts"].max()
    span_days = (last - data["ts"].min()).days
    val_days = max(1, min(validation_days, span_days // 4))

    cut = last - pd.Timedelta(days=val_days)
    train = data[data["ts"] <= cut]
    valid = data[data["ts"] > cut]
    if len(train) < MIN_TRAIN_ROWS or len(valid) < MIN_VALIDATION_ROWS:
        raise NotEnoughDataError(
            f"Dados insuficientes: {len(train)} linhas de treino e {len(valid)} de "
            f"validação (mínimo {MIN_TRAIN_ROWS} e {MIN_VALIDATION_ROWS}). "
            "Rode o seed_firestore.py para gerar mais histórico."
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
        model.fit(x_train, y_train - lag_train)                      # aprende a correção
        predicted = np.clip(lag_valid + model.predict(x_valid), 0, 100)
        mae = mean_absolute_error(y_valid, predicted)                # erro nos dias NÃO vistos
        scores.append(ModelScore(key, label, round(float(mae), 3),
                                 round(time.perf_counter() - t0, 3)))

    best = min(scores, key=lambda s: s.mae)                          # menor MAE vence

    # O vencedor é retreinado com TODO o histórico para prever o futuro
    final = MODEL_FACTORIES[best.key][1]()
    final.fit(build_features(data, env_ids),
              data["occupancyPercent"].to_numpy(dtype=float) - data["lag_7d"].to_numpy(dtype=float))

    return TrainingResult(
        best_key=best.key,
        best_label=best.label,
        scores=scores,
        baseline_mae=round(float(mean_absolute_error(y_valid, lag_valid)), 3),
        train_rows=len(train),
        validation_rows=len(valid),
        validation_days=val_days,
        env_ids=env_ids,
        model=final,
        seconds=round(time.perf_counter() - started, 3),
    )


# ---------------------------------------------------------------------------
# Previsão das próximas horas
# ---------------------------------------------------------------------------
def forecast_hours(result: TrainingResult, observed: pd.DataFrame,
                   now: datetime, horizon_hours: int) -> Dict[str, List[dict]]:
    """Devolve, para cada ambiente, a ocupação prevista (%) das próximas horas
    em que ele costuma funcionar."""
    known = dict(zip(zip(observed["environmentId"], observed["ts"]),
                     observed["occupancyPercent"]))

    last_seen = observed["ts"].max()
    recent = observed[observed["ts"] >= last_seen - pd.Timedelta(days=14)]
    open_hours = {env: set(group["ts"].dt.hour)
                  for env, group in recent.groupby("environmentId")}

    first = pd.Timestamp(floor_hour(now)) + pd.Timedelta(hours=1)
    rows = []
    for env_id in result.env_ids:
        for k in range(horizon_hours):
            moment = first + pd.Timedelta(hours=k)
            if moment.hour not in open_hours.get(env_id, set()):
                continue
            lag7 = known.get((env_id, moment - pd.Timedelta(days=7)))
            if lag7 is None:
                continue  # sem a semana passada não há como prever esta hora
            lag14 = known.get((env_id, moment - pd.Timedelta(days=14)), lag7)
            rows.append({"environmentId": env_id, "ts": moment,
                         "lag_7d": float(lag7), "lag_14d": float(lag14)})

    if not rows:
        return {}

    frame = pd.DataFrame(rows)
    delta = result.model.predict(build_features(frame, result.env_ids))
    frame["predicted"] = np.clip(frame["lag_7d"].to_numpy() + delta, 0, 100)

    output: Dict[str, List[dict]] = {}
    for env_id, group in frame.groupby("environmentId"):
        output[env_id] = [
            {"ts": row.ts, "occupancy": int(round(row.predicted))}
            for row in group.sort_values("ts").itertuples()
        ]
    return output
