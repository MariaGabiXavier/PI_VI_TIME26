from datetime import datetime

import pandas as pd
import pytest

from forecasting.data_source import LOCAL_TZ, standardize
from forecasting.ml_pipeline import (NotEnoughDataError, prepare_observed, train_and_select)
from forecasting.service import parse_now, run_forecast

NOW = datetime(2026, 10, 7, 9, 30, tzinfo=LOCAL_TZ)


def test_three_models_are_trained_and_lowest_mae_wins(readings):
    observed = prepare_observed(readings, NOW)
    result = train_and_select(observed)
    assert {s.key for s in result.scores} == {"knn", "svm", "random_forest"}
    assert result.best_label == min(result.scores, key=lambda s: s.mae).label
    assert all(s.mae >= 0 for s in result.scores)


def test_future_readings_are_not_used(readings):
    observed = prepare_observed(readings, NOW)
    assert observed["ts"].max() <= pd.Timestamp(NOW.replace(minute=0))
    assert len(observed) < len(readings)


def test_forecast_shape_and_values(readings):
    out = run_forecast(readings, NOW, horizon_hours=48)
    assert out["metric"] == "MAE" and len(out["models"]) == 3
    assert sum(m["isBest"] for m in out["models"]) == 1
    assert 1 <= len(out["insights"]) <= 3
    for env in out["forecasts"]:
        assert env["points"], "deve haver pontos previstos"
        for p in env["points"]:
            assert 0 <= p["predictedOccupancyPercent"] <= 100
            assert 0 <= p["predictedPeople"] <= env["capacity"]
        assert env["peak"]["occupancyPercent"] == max(p["predictedOccupancyPercent"] for p in env["points"])


def test_environment_filter_keeps_global_insights(readings):
    out = run_forecast(readings, NOW, 24, environment_id="lab")
    assert [f["environmentId"] for f in out["forecasts"]] == ["lab"]
    assert len(out["insights"]) >= 2


def test_not_enough_data():
    tiny = pd.DataFrame({
        "environmentId": ["a"] * 10, "environmentName": ["A"] * 10, "capacity": [10] * 10,
        "timestamp": pd.date_range("2026-10-01", periods=10, freq="h", tz="UTC").astype(str),
        "occupancyPercent": range(10)})
    with pytest.raises(NotEnoughDataError):
        run_forecast(standardize(tiny), datetime(2026, 10, 2, tzinfo=LOCAL_TZ))


def test_parse_now_accepts_z_and_naive():
    assert parse_now("2026-10-07T12:00:00Z").hour == 9          # UTC -> Brasília
    assert parse_now("2026-10-07T12:00:00").hour == 12          # sem fuso = horário local
