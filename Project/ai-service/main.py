"""FlowCampus AI - API de previsão de ocupação e geração de insights.

Endpoints
---------
GET  /health         verifica se o servidor está no ar
POST /predict        prevê a ocupação da PRÓXIMA hora de UM ambiente (seu endpoint original)
GET  /api/forecast   prevê as próximas horas de TODOS os ambientes + insights

Nos dois: a CADA chamada os 3 modelos (KNN, SVM e Random Forest) são treinados
do zero com o histórico do Firestore (coleção occupancy_readings), comparados
pelo MAE em dados que não viram, e o de menor erro é o usado.

Rodar:  uvicorn main:app --host 0.0.0.0 --port 8000 --reload
Docs:   http://localhost:8000/docs
"""
import logging
from datetime import datetime
from math import ceil
from typing import Optional

import pandas as pd
from fastapi import Depends, FastAPI, Header, HTTPException, Query
from pydantic import BaseModel, Field

from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.neighbors import KNeighborsRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.svm import SVR

from forecasting.config import Settings, get_settings
from forecasting.data_source import LOCAL_TZ, get_firebase_app, load_readings
from forecasting.ml_pipeline import NotEnoughDataError
from forecasting.nowcast import build_nowcast_table, normalize_name
from forecasting.schemas import ForecastResponse
from forecasting.service import parse_now, run_forecast

logger = logging.getLogger("flowcampus.ai")

app = FastAPI(
    title="FlowCampus AI",
    description="Previsao de ocupacao e geracao de insights",
    version="2.0.0",
)

NUMERIC_FEATURES = [
    "hour",
    "day_of_week",
    "is_weekend",
    "current_occupancy",
    "current_visitors",
    "current_wait_minutes",
]

FEATURES = ["environment"] + NUMERIC_FEATURES
TARGET = "target_next_occupancy"


class PredictionRequest(BaseModel):
    environment: str = Field(min_length=1)
    hour: int = Field(ge=0, le=23)
    day_of_week: int = Field(ge=0, le=6)
    is_weekend: int = Field(ge=0, le=1)
    current_occupancy: float = Field(ge=0, le=100)
    current_visitors: int = Field(ge=0)
    current_wait_minutes: float = Field(ge=0)


def authenticate(authorization: Optional[str] = Header(default=None),
                 settings: Settings = Depends(get_settings)) -> Optional[dict]:
    """Se REQUIRE_AUTH=true no .env, exige o token do Firebase Auth (usuário logado)."""
    if not settings.require_auth:
        return None
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="Envie o token: Authorization: Bearer <idToken>")
    from firebase_admin import auth
    try:
        return auth.verify_id_token(authorization[7:].strip(), app=get_firebase_app(settings))
    except Exception as error:
        raise HTTPException(status_code=401, detail="Token invalido ou expirado") from error


def load_data():
    """Monta a tabela de treino a partir do Firestore (ou do CSV do seed, em modo teste)."""
    settings = get_settings()

    try:
        readings = load_readings(settings)
        data = build_nowcast_table(readings, datetime.now(LOCAL_TZ))
    except (FileNotFoundError, ValueError) as error:
        raise HTTPException(status_code=500, detail=str(error))

    if len(data) < 20:
        raise HTTPException(
            status_code=400,
            detail=(
                "Sao necessarios pelo menos 20 registros validos "
                "para este prototipo. Recomendamos muito mais dados "
                "para uma avaliacao confiavel."
            ),
        )

    return data


def create_pipeline(model, model_name):
    preprocessing = ColumnTransformer(
        transformers=[
            (
                "environment",
                OneHotEncoder(handle_unknown="ignore"),
                ["environment"],
            ),
            (
                "numeric",
                StandardScaler(),
                NUMERIC_FEATURES,
            ),
        ]
    )

    return Pipeline(
        steps=[
            ("preprocessing", preprocessing),
            ("model", model),
        ]
    )


def make_models(training_rows):
    return {
        "KNN": KNeighborsRegressor(
            n_neighbors=min(5, training_rows)
        ),
        "SVM": SVR(
            kernel="rbf",
            C=10.0,
            epsilon=2.0,
        ),
        "Random Forest": RandomForestRegressor(
            n_estimators=150,
            random_state=42,
            min_samples_leaf=2,
        ),
    }


def generate_insight(current, predicted):
    difference = predicted - current

    if predicted >= 70:
        message = (
            "A ocupacao prevista e alta. "
            "Considere verificar outro horario ou ambiente."
        )
    elif predicted >= 40:
        message = (
            "A ocupacao prevista e moderada. "
            "Pode haver movimento significativo."
        )
    else:
        message = (
            "A ocupacao prevista e baixa. "
            "Este pode ser um periodo mais tranquilo."
        )

    if difference >= 10:
        message += " A ocupacao deve aumentar em relacao ao momento atual."
    elif difference <= -10:
        message += " A ocupacao deve diminuir em relacao ao momento atual."
    else:
        message += " A ocupacao deve permanecer relativamente estavel."

    return message


@app.get("/health")
def health():
    return {"status": "ok", "service": "FlowCampus AI"}


@app.post("/predict")
def predict(request: PredictionRequest, _user: Optional[dict] = Depends(authenticate)):
    data = load_data()

    # O nome do ambiente precisa existir no historico (ignora acento e maiuscula).
    known = {normalize_name(name): name for name in data["environment"].unique()}
    environment = known.get(normalize_name(request.environment))
    if environment is None:
        raise HTTPException(
            status_code=404,
            detail=f"Ambiente '{request.environment}' nao encontrado. Validos: {sorted(known.values())}",
        )

    # Separacao cronologica: os registros mais recentes
    # ficam reservados para avaliar os modelos.
    test_size = max(1, ceil(len(data) * 0.20))
    train_size = len(data) - test_size

    training_data = data.iloc[:train_size]
    validation_data = data.iloc[train_size:]

    X_train = training_data[FEATURES]
    y_train = training_data[TARGET]

    X_validation = validation_data[FEATURES]
    y_validation = validation_data[TARGET]

    scores = {}
    trained_models = {}

    models = make_models(len(training_data))

    # Treina e avalia os tres modelos usando a mesma divisao.
    for name, estimator in models.items():
        pipeline = create_pipeline(estimator, name)
        pipeline.fit(X_train, y_train)

        validation_predictions = pipeline.predict(X_validation)

        mae = mean_absolute_error(
            y_validation,
            validation_predictions,
        )

        scores[name] = round(float(mae), 4)
        trained_models[name] = estimator

    # Seleciona o menor erro de validacao.
    best_name = min(scores, key=scores.get)

    # Treina novamente o vencedor com todo o historico.
    best_pipeline = create_pipeline(
        trained_models[best_name],
        best_name,
    )

    best_pipeline.fit(data[FEATURES], data[TARGET])

    input_data = pd.DataFrame(
        [
            {
                "environment": environment,
                "hour": request.hour,
                "day_of_week": request.day_of_week,
                "is_weekend": request.is_weekend,
                "current_occupancy": request.current_occupancy,
                "current_visitors": request.current_visitors,
                "current_wait_minutes": request.current_wait_minutes,
            }
        ]
    )

    prediction = float(best_pipeline.predict(input_data)[0])

    # A ocupacao percentual deve ficar entre 0 e 100.
    prediction = max(0.0, min(100.0, prediction))

    return {
        "environment": environment,
        "predicted_occupancy": round(prediction, 1),
        "selected_model": best_name,
        "mae_by_model": scores,
        "selected_model_mae": scores[best_name],
        "validation_records": len(validation_data),
        "training_records": len(data),
        "insight": generate_insight(
            request.current_occupancy,
            prediction,
        ),
    }


@app.get("/api/forecast", response_model=ForecastResponse)
def forecast(
    horizon_hours: int = Query(48, ge=1, le=168, description="Quantas horas a frente prever"),
    environment_id: Optional[str] = Query(None, description="Filtra a lista 'forecasts' por ambiente"),
    now: Optional[str] = Query(None, description="Simula outro horario (ISO 8601)"),
    _user: Optional[dict] = Depends(authenticate),
    settings: Settings = Depends(get_settings),
):
    """A CADA chamada: treina KNN, SVM e Random Forest, mede o MAE em dados nao vistos,
    escolhe o de menor erro e usa ele para prever as proximas horas e gerar os insights."""
    try:
        readings = load_readings(settings)
        return run_forecast(readings, parse_now(now), horizon_hours,
                            environment_id, settings.validation_days)
    except NotEnoughDataError as error:
        raise HTTPException(status_code=503, detail=str(error)) from error
    except (FileNotFoundError, ValueError) as error:
        raise HTTPException(status_code=500, detail=str(error)) from error
    except Exception as error:
        logger.exception("Falha ao gerar a previsao")
        raise HTTPException(status_code=500, detail=f"Erro ao gerar a previsao: {error}") from error
