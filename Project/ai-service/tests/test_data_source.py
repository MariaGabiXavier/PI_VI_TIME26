from datetime import datetime, timedelta, timezone
from pathlib import Path

import forecasting.data_source as ds
from forecasting.config import Settings


def _settings(source="firestore", cache=600):
    return Settings(source, Path("/nope.csv"), Path("/nope.json"), cache, 60, 7, False)


class FakeDoc:
    def __init__(self, data):
        self._data = data

    def to_dict(self):
        return self._data


class FakeQuery:
    def __init__(self, docs, log):
        self.docs, self.log = docs, log

    def where(self, filter=None):
        self.log.append(("where", filter.field_path, filter.op_string))
        return self

    def select(self, fields):
        self.log.append(("select", list(fields)))
        return self

    def stream(self):
        self.log.append(("stream",))
        return [FakeDoc(d) for d in self.docs]


class FakeDb:
    def __init__(self, docs):
        self.docs, self.log = docs, []

    def collection(self, name):
        self.log.append(("collection", name))
        return FakeQuery(self.docs, self.log)


def test_firestore_loader_and_cache(monkeypatch):
    base = datetime(2026, 10, 7, 12, tzinfo=timezone.utc)  # 09h em Brasília
    docs = [
        {"environmentId": "ref", "environmentName": "Refeitório", "capacity": 120,
         "timestamp": base, "occupancyPercent": 80},
        {"environmentId": "ref", "environmentName": "Refeitório", "capacity": 120,
         "timestamp": base + timedelta(hours=1), "occupancyPercent": 90},
        {"environmentId": "ref", "environmentName": "Refeitório", "capacity": 120,
         "timestamp": base + timedelta(hours=1), "occupancyPercent": 91},   # duplicado
    ]
    fake = FakeDb(docs)
    from firebase_admin import firestore
    monkeypatch.setattr(firestore, "client", lambda app: fake)
    monkeypatch.setattr(ds, "get_firebase_app", lambda settings: object())
    ds.clear_cache()

    df = ds.load_readings(_settings())
    assert len(df) == 2                                   # duplicado removido (fica o último)
    assert df["ts"].iloc[0].hour == 9                     # convertido para o horário local
    assert df["occupancyPercent"].tolist() == [80, 91]
    assert ("collection", "occupancy_readings") in fake.log
    assert ("where", "timestamp", ">=") in fake.log

    reads = fake.log.count(("stream",))
    ds.load_readings(_settings())                         # dentro do cache: não lê de novo
    assert fake.log.count(("stream",)) == reads
    ds.load_readings(_settings(cache=0))                  # cache expirado: lê de novo
    assert fake.log.count(("stream",)) == reads + 1
    ds.clear_cache()


def test_missing_columns():
    import pandas as pd
    import pytest
    with pytest.raises(ValueError):
        ds.standardize(pd.DataFrame({"environmentId": ["a"]}))
