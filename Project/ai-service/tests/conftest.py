import sys
from datetime import datetime, timedelta
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from forecasting.data_source import LOCAL_TZ  # noqa: E402


def make_readings(days=40, envs=(("lab", "Lab A2", 40), ("ref", "Refeitório", 120)), seed=1):
    """Histórico sintético: padrão semanal + ruído. Termina em 2026-10-07 23h."""
    rng = np.random.default_rng(seed)
    end = datetime(2026, 10, 7, 23, tzinfo=LOCAL_TZ)
    rows = []
    for env_id, name, capacity in envs:
        for h in range(days * 24):
            moment = end - timedelta(hours=h)
            if not 8 <= moment.hour <= 19:
                continue
            base = 0.15 + 0.7 * np.exp(-((moment.hour - 12.5) ** 2) / 4)
            base *= 0.4 if moment.weekday() >= 5 else 1.0
            ratio = float(np.clip(base * rng.normal(1, 0.05), 0.02, 1))
            rows.append({"environmentId": env_id, "environmentName": name, "capacity": capacity,
                         "timestamp": moment.astimezone(tz=None).isoformat(),
                         "occupancyPercent": round(ratio * 100)})
    return pd.DataFrame(rows)


@pytest.fixture
def readings():
    from forecasting.data_source import standardize
    return standardize(make_readings())
