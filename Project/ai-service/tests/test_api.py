from fastapi.testclient import TestClient

import main
from forecasting.config import Settings, get_settings

client = TestClient(main.app)
NOW = "2026-10-07T09:30:00-03:00"


def _use(monkeypatch, readings, require_auth=False):
    monkeypatch.setattr(main, "load_readings", lambda settings: readings.copy())
    base = get_settings()
    settings = Settings(base.data_source, base.csv_path, base.firebase_key_path, base.cache_seconds,
                        base.history_days, base.validation_days, require_auth)
    main.app.dependency_overrides[get_settings] = lambda: settings


def test_health():
    assert client.get("/health").json()["status"] == "ok"


def test_forecast_endpoint(monkeypatch, readings):
    _use(monkeypatch, readings)
    response = client.get("/api/forecast", params={"now": NOW, "horizon_hours": 24})
    assert response.status_code == 200
    body = response.json()
    assert body["bestModelLabel"] in {"KNN", "SVM", "Random Forest"}
    assert body["insights"] and body["forecasts"]


def test_validation_error(monkeypatch, readings):
    _use(monkeypatch, readings)
    assert client.get("/api/forecast", params={"horizon_hours": 0}).status_code == 422


def test_auth_required(monkeypatch, readings):
    _use(monkeypatch, readings, require_auth=True)
    assert client.get("/api/forecast", params={"now": NOW}).status_code == 401
    assert client.get("/api/forecast", headers={"Authorization": "Basic abc"}).status_code == 401


def test_not_enough_data_returns_503(monkeypatch, readings):
    _use(monkeypatch, readings.head(40))
    assert client.get("/api/forecast", params={"now": NOW}).status_code == 503


def test_predict_uses_three_models_and_accepts_accentless_name(monkeypatch, readings):
    _use(monkeypatch, readings)
    body = {"environment": "refeitorio", "hour": 11, "day_of_week": 2, "is_weekend": 0,
            "current_occupancy": 55, "current_visitors": 40, "current_wait_minutes": 3}
    response = client.post("/predict", json=body)
    assert response.status_code == 200, response.text
    data = response.json()
    assert data["environment"] == "Refeitório"
    assert set(data["mae_by_model"]) == {"KNN", "SVM", "Random Forest"}
    assert data["selected_model"] == min(data["mae_by_model"], key=data["mae_by_model"].get)
    assert 0 <= data["predicted_occupancy"] <= 100


def test_predict_unknown_environment_is_404(monkeypatch, readings):
    _use(monkeypatch, readings)
    body = {"environment": "Piscina", "hour": 11, "day_of_week": 2, "is_weekend": 0,
            "current_occupancy": 55, "current_visitors": 40, "current_wait_minutes": 3}
    response = client.post("/predict", json=body)
    assert response.status_code == 404 and "Refeitório" in response.text
