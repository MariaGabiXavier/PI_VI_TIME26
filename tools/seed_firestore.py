#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
FlowCampus - gerador de dados FICTICIOS para o Firestore
=========================================================

Gera um historico simulado de lotacao dos ambientes do campus e envia ao
Firestore. Enquanto a visao computacional nao existe, e essa base que o app
(tela Analises) e o modelo de IA vao usar.

Colecoes criadas / atualizadas
------------------------------
environments        -> os 5 ambientes (so cria os que ainda nao existem; os que
                       voce ja cadastrou pelo app sao reaproveitados pelo nome)
occupancy_readings  -> leitura HORARIA de cada ambiente (base para treinar a IA)
daily_stats         -> resumo DIARIO por ambiente (a tela Analises le daqui)
ai_insights         -> frases de "Insights da IA" (calculadas a partir dos dados)

Todo documento gerado tem  source = "simulated".  Quando as cameras reais
entrarem, grave com source = "camera" e rode  --clean  para apagar so os
simulados.

Uso
---
  pip install firebase-admin

  python seed_firestore.py --dry-run                      # so gera CSV local
  python seed_firestore.py --key serviceAccountKey.json   # envia ao Firestore
  python seed_firestore.py --key serviceAccountKey.json --clean   # apaga simulados

Rodar de novo e seguro: os IDs sao fixos, entao so sobrescreve (nao duplica).
Rodar em outro dia estende o historico ate "hoje".
"""

import argparse
import csv
import math
import random
import sys
import unicodedata
from datetime import datetime, timedelta, timezone
from pathlib import Path

try:  # evita erro de acento no terminal do Windows
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

# Brasilia: sem horario de verao desde 2019, entao um offset fixo basta.
TZ = timezone(timedelta(hours=-3))
SOURCE = "simulated"

COL_ENV = "environments"
COL_READINGS = "occupancy_readings"
COL_DAILY = "daily_stats"
COL_INSIGHTS = "ai_insights"

# --------------------------------------------------------------------------
# Perfil de cada ambiente
#   hours      -> (primeira hora, ultima hora + 1) em que ha leitura
#   base       -> ocupacao minima (fracao da capacidade)
#   peaks      -> (hora central, largura, altura) de cada pico do dia
#   week       -> multiplicador por dia da semana (0=segunda ... 6=domingo)
#   turnover   -> entradas por hora para cada pessoa presente (quanto maior,
#                 mais rapido o giro: ponto de onibus > refeitorio > biblioteca)
#   wait_scale -> espera (min) quando o ambiente esta 100% cheio
# --------------------------------------------------------------------------
WEEK_NORMAL = {0: 1.0, 1: 1.0, 2: 1.0, 3: 1.0, 4: 0.85, 5: 0.45, 6: 0.10}

# Tendencia: o campus vem ficando mais movimentado. Sem isso toda semana seria
# igual e "Semana" ficaria identica a "Mes".
# chave = quantas semanas atras (0 = ultimos 7 dias, 1 = semana anterior, ...)
WEEK_TREND = {0: 1.12, 1: 1.00, 2: 0.90, 3: 0.88}
WEEK_TREND_OLDER = 0.85

ENVIRONMENTS = [
    dict(
        key="refeitorio", name="Refeitório", category="RESTAURANT",
        building="Prédio de Convivência", capacity=120,
        description="Praça de alimentação do campus.",
        hours=(10, 20), base=0.52, peaks=[(12.5, 1.2, 0.60), (18.5, 1.0, 0.35)],
        week=WEEK_NORMAL, turnover=1.4, wait_scale=15,
        recent_peak_shift=-0.6,  # pico do almoco comecando mais cedo na ultima semana
    ),
    dict(
        key="biblioteca", name="Biblioteca", category="LIBRARY",
        building="Biblioteca Central", capacity=80,
        description="Biblioteca e salas de leitura.",
        hours=(7, 22), base=0.20, peaks=[(14.0, 2.0, 0.28), (19.0, 1.6, 0.22)],
        week={0: 1.0, 1: 1.4, 2: 1.0, 3: 1.0, 4: 1.0, 5: 0.5, 6: 0.10},  # terca +40%
        turnover=0.35, wait_scale=4,
        recent_boost=1.25,  # epoca de provas: ultimas 2 semanas mais cheias
    ),
    dict(
        key="ponto-de-onibus", name="Ponto de ônibus", category="BUS",
        building="Portaria Principal", capacity=70,
        description="Ponto de ônibus em frente ao campus.",
        hours=(6, 23), base=0.40,
        peaks=[(7.0, 0.9, 0.35), (12.0, 0.8, 0.15), (17.8, 1.0, 0.40), (22.0, 0.6, 0.25)],
        week=WEEK_NORMAL, turnover=1.8, wait_scale=12,
    ),
    dict(
        key="lab-a2", name="Lab A2", category="LAB",
        building="Prédio de Laboratórios", capacity=40,
        description="Laboratório de informática A2.",
        hours=(8, 22), base=0.28, peaks=[(10.5, 1.3, 0.35), (15.0, 1.5, 0.30), (19.5, 1.2, 0.25)],
        week=WEEK_NORMAL, turnover=0.45, wait_scale=3,
    ),
    dict(
        key="sala-estudo-7a", name="Sala Estudo - 7A", category="STUDY",
        building="Prédio 7", capacity=80,
        description="Sala de estudo em grupo.",
        hours=(8, 22), base=0.30, peaks=[(15.0, 2.0, 0.85)],  # esvazia a noite
        week=WEEK_NORMAL, turnover=0.40, wait_scale=2,
    ),
]


# --------------------------------------------------------------------------
# Geracao
# --------------------------------------------------------------------------
def norm(text):
    """minusculo e sem acento, para comparar nomes de ambientes."""
    text = unicodedata.normalize("NFD", text or "")
    return "".join(c for c in text if unicodedata.category(c) != "Mn").strip().lower()


def hour_ratio(env, hour, shifted):
    ratio = env["base"]
    for i, (center, width, height) in enumerate(env["peaks"]):
        if i == 0 and shifted:
            center += env.get("recent_peak_shift", 0.0)
        ratio += height * math.exp(-((hour - center) ** 2) / (2 * width * width))
    return ratio


def generate_environment_days(env, env_id, env_name, capacity, today, days, seed):
    readings, dailies = [], []
    first_day = today - timedelta(days=days - 1)
    start, end = env["hours"]

    for offset in range(days):
        day = first_day + timedelta(days=offset)
        date_key = day.isoformat()
        shifted = (today - day).days < 7  # "essa semana"
        weeks_back = (today - day).days // 7
        trend = WEEK_TREND.get(weeks_back, WEEK_TREND_OLDER)
        if weeks_back <= 1:
            trend *= env.get("recent_boost", 1.0)
        rng = random.Random(f"{seed}|{env['key']}|{date_key}")  # deterministico
        day_factor = env["week"][day.weekday()] * trend * rng.gauss(1, 0.05)
        day_readings = []

        for hour in range(start, end):
            ratio = hour_ratio(env, hour, shifted) * day_factor * rng.gauss(1, 0.07)
            ratio = min(max(ratio, 0.02), 1.0)
            people = round(ratio * capacity)
            entries = max(0, round(people * env["turnover"] * rng.gauss(1, 0.10)))
            wait = max(0.0, env["wait_scale"] * ratio ** 2 * rng.gauss(1, 0.12))
            moment = datetime(day.year, day.month, day.day, hour, 0, tzinfo=TZ)

            day_readings.append({
                "environmentId": env_id,
                "environmentName": env_name,
                "timestamp": moment,
                "dateKey": date_key,
                "hour": hour,
                "dayOfWeek": day.isoweekday(),  # 1=segunda ... 7=domingo
                "people": people,
                "capacity": capacity,
                "occupancyPercent": round(people / capacity * 100),
                "entries": entries,
                "waitMinutes": round(wait, 1),
                "source": SOURCE,
            })

        readings.extend(day_readings)

        total = sum(r["entries"] for r in day_readings)
        weighted_wait = (
            sum(r["waitMinutes"] * r["entries"] for r in day_readings) / total
            if total else 0.0
        )
        peak = max(day_readings, key=lambda r: r["people"])
        dailies.append({
            "environmentId": env_id,
            "environmentName": env_name,
            "dateKey": date_key,
            "date": datetime(day.year, day.month, day.day, tzinfo=TZ),
            "totalVisitors": total,
            "avgOccupancyPercent": round(
                sum(r["occupancyPercent"] for r in day_readings) / len(day_readings)),
            "peakOccupancyPercent": peak["occupancyPercent"],
            "peakHour": peak["hour"],
            "peakPeople": peak["people"],
            "avgWaitMinutes": round(weighted_wait, 1),
            "capacity": capacity,
            "source": SOURCE,
        })

    return readings, dailies


def mean(values):
    values = list(values)
    return sum(values) / len(values) if values else 0.0


def build_insights(readings, dailies, today):
    """Insights simples calculados dos proprios dados (a IA substitui isso depois)."""
    insights = []
    now = datetime.now(TZ)

    def by_env(rows, env_id):
        return [r for r in rows if r["environmentId"] == env_id]

    def is_weekday(date_key):
        return datetime.fromisoformat(date_key).isoweekday() <= 5

    # 1) Biblioteca: terca-feira x demais dias uteis
    lib_id = ENV_IDS["biblioteca"]
    lib = [d for d in by_env(dailies, lib_id) if is_weekday(d["dateKey"])]
    tue = mean(d["avgOccupancyPercent"] for d in lib
               if datetime.fromisoformat(d["dateKey"]).isoweekday() == 2)
    other = mean(d["avgOccupancyPercent"] for d in lib
                 if datetime.fromisoformat(d["dateKey"]).isoweekday() != 2)
    if other:
        pct = round((tue / other - 1) * 100)
        if pct >= 10:
            highlight = f"{pct}% mais cheia"
            insights.append(("biblioteca-terca", {
                "icon": "BOOK", "tone": "BLUE", "highlight": highlight,
                "text": f"A biblioteca costuma ficar {highlight} na terça-feira.",
            }))

    # 2) Refeitorio: centro de massa do almoco (10h-15h) desta semana x 3 semanas antes
    ref = by_env(readings, ENV_IDS["refeitorio"])

    def lunch_center(day_rows):
        rows = [r for r in day_rows if 10 <= r["hour"] <= 15]
        if not rows:
            return None
        floor = min(r["people"] for r in rows)  # descarta a ocupacao "de fundo"
        total = sum(r["people"] - floor for r in rows)
        if total <= 0:
            return None
        return sum(r["hour"] * (r["people"] - floor) for r in rows) / total

    def centers(date_from, date_to):
        result = []
        for key in sorted({r["dateKey"] for r in ref}):
            if date_from <= key <= date_to and is_weekday(key):
                c = lunch_center([r for r in ref if r["dateKey"] == key])
                if c is not None:
                    result.append(c)
        return result

    recent = centers((today - timedelta(days=6)).isoformat(), today.isoformat())
    before = centers((today - timedelta(days=27)).isoformat(),
                     (today - timedelta(days=7)).isoformat())
    if recent and before:
        shift = (mean(recent) - mean(before)) * 60
        minutes = int(round(abs(shift) / 5) * 5)
        if minutes >= 10:
            direction = "mais cedo" if shift < 0 else "mais tarde"
            highlight = f"{minutes} min {direction}"
            insights.append(("refeitorio-pico", {
                "icon": "CLOCK", "tone": "RED" if shift < 0 else "AMBER",
                "highlight": highlight,
                "text": f"O horário de pico do refeitório está começando {highlight} essa semana.",
            }))

    # 3) Sala de estudo: ocupacao depois das 18h x antes das 18h (dias uteis)
    study = [r for r in by_env(readings, ENV_IDS["sala-estudo-7a"]) if is_weekday(r["dateKey"])]
    day_avg = mean(r["occupancyPercent"] for r in study if r["hour"] < 18)
    night_avg = mean(r["occupancyPercent"] for r in study if r["hour"] >= 18)
    if day_avg and night_avg / day_avg <= 0.7:
        insights.append(("sala-estudo-noite", {
            "icon": "HOME", "tone": "GREEN", "highlight": "menos usada",
            "text": "A sala de Estudo é constantemente menos usada após às 18:00.",
        }))

    # createdAt decrescente: o 1o da lista fica no topo (a tela ordena por createdAt desc)
    docs = []
    for i, (doc_id, data) in enumerate(insights):
        data.update({"createdAt": now - timedelta(seconds=i), "source": SOURCE})
        docs.append((doc_id, data))
    return docs


# --------------------------------------------------------------------------
# Previa do que o app vai mostrar (mesma conta da tela Analises)
# --------------------------------------------------------------------------
def preview(dailies, today):
    windows = {"Hoje": 0, "Semana": 6, "Mês": 29}
    print("\n=== Prévia do que a tela Análises vai mostrar ===")
    for label, back in windows.items():
        start = (today - timedelta(days=back)).isoformat()
        rows = [d for d in dailies if start <= d["dateKey"] <= today.isoformat()]
        visitors = sum(d["totalVisitors"] for d in rows)
        wait = (sum(d["avgWaitMinutes"] * d["totalVisitors"] for d in rows) / visitors
                if visitors else 0)
        per_env = {}
        for d in rows:
            per_env.setdefault(d["environmentName"], []).append(d["avgOccupancyPercent"])
        ranking = sorted(((mean(v), n) for n, v in per_env.items()), reverse=True)
        print(f"\n[{label}] visitantes={visitors:,}".replace(",", ".")
              + f" | espera média={round(wait)} min | mais cheia={ranking[0][1]}")
        for value, name in ranking:
            print(f"    {name:<18} {round(value):>3}")


# --------------------------------------------------------------------------
# Firestore
# --------------------------------------------------------------------------
def connect(key_path):
    import firebase_admin
    from firebase_admin import credentials, firestore

    path = Path(key_path)
    if not path.exists():
        sys.exit(f"Chave não encontrada: {path}\n"
                 "Baixe em Console Firebase > Configurações do projeto > "
                 "Contas de serviço > Gerar nova chave privada.")
    firebase_admin.initialize_app(credentials.Certificate(str(path)))
    return firestore.client()


def resolve_environments(db):
    """Retorna {key: (doc_id, nome, capacidade)} reaproveitando o que ja existe."""
    resolved, existing = {}, {}

    if db is not None:
        for doc in db.collection(COL_ENV).stream():
            data = doc.to_dict() or {}
            existing[norm(data.get("name", ""))] = (doc.id, data)

    for env in ENVIRONMENTS:
        found = existing.get(norm(env["name"]))
        if found:
            doc_id, data = found
            resolved[env["key"]] = (doc_id, data.get("name", env["name"]),
                                    int(data.get("capacity") or env["capacity"]))
            print(f"  = {env['name']}: já existe ({doc_id}), reaproveitando")
            continue

        doc_id = env["key"]
        if db is not None:
            from firebase_admin import firestore
            ref = db.collection(COL_ENV).document(doc_id)
            if ref.get().exists:  # id ocupado por outro ambiente: usa id automatico
                ref = db.collection(COL_ENV).document()
                doc_id = ref.id
            hours = f"{env['hours'][0]:02d}:00 - {env['hours'][1]:02d}:00"
            ref.set({
                "name": env["name"], "category": env["category"],
                "building": env["building"], "capacity": env["capacity"],
                "operatingHours": hours, "description": env["description"],
                "createdBy": "seed", "createdAt": firestore.SERVER_TIMESTAMP,
            })
            print(f"  + {env['name']}: criado ({doc_id})")
        resolved[env["key"]] = (doc_id, env["name"], env["capacity"])

    return resolved


def commit_all(db, operations, label):
    """operations: lista de (colecao, doc_id, dados). Lotes de 400 (limite e 500)."""
    size = 400
    for start in range(0, len(operations), size):
        batch = db.batch()
        for collection, doc_id, data in operations[start:start + size]:
            batch.set(db.collection(collection).document(doc_id), data)
        batch.commit()
        print(f"  {label}: {min(start + size, len(operations))}/{len(operations)}")


def clean_simulated(db):
    from google.cloud.firestore_v1.base_query import FieldFilter

    for collection in (COL_READINGS, COL_DAILY, COL_INSIGHTS):
        refs = [d.reference for d in db.collection(collection)
                .where(filter=FieldFilter("source", "==", SOURCE)).stream()]
        for start in range(0, len(refs), 400):
            batch = db.batch()
            for ref in refs[start:start + 400]:
                batch.delete(ref)
            batch.commit()
        print(f"  {collection}: {len(refs)} documentos simulados apagados")
    print("Os ambientes (environments) não foram tocados.")


# --------------------------------------------------------------------------
# Principal
# --------------------------------------------------------------------------
ENV_IDS = {}


def main():
    parser = argparse.ArgumentParser(description="Dados fictícios do FlowCampus")
    parser.add_argument("--key", default="serviceAccountKey.json",
                        help="chave de conta de serviço do Firebase (JSON)")
    parser.add_argument("--days", type=int, default=60, help="dias de histórico (mín. 28)")
    parser.add_argument("--seed", type=int, default=2026, help="semente do gerador")
    parser.add_argument("--out", default="seed_output", help="pasta dos CSVs locais")
    parser.add_argument("--dry-run", action="store_true", help="não envia ao Firestore")
    parser.add_argument("--clean", action="store_true",
                        help="apaga os documentos simulados (source = simulated)")
    args = parser.parse_args()

    if args.days < 28:
        sys.exit("--days precisa ser >= 28 (os insights comparam 4 semanas).")

    db = None if args.dry_run else connect(args.key)

    if args.clean:
        if db is None:
            sys.exit("--clean precisa de --key (sem --dry-run).")
        clean_simulated(db)
        return

    today = datetime.now(TZ).date()
    print("Ambientes:")
    resolved = resolve_environments(db)
    ENV_IDS.update({key: value[0] for key, value in resolved.items()})

    readings, dailies = [], []
    for env in ENVIRONMENTS:
        env_id, env_name, capacity = resolved[env["key"]]
        r, d = generate_environment_days(env, env_id, env_name, capacity,
                                         today, args.days, args.seed)
        readings += r
        dailies += d

    insights = build_insights(readings, dailies, today)

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    csv_path = out / "occupancy_readings.csv"
    columns = ["environmentId", "environmentName", "timestamp", "dateKey", "hour",
               "dayOfWeek", "people", "capacity", "occupancyPercent", "entries",
               "waitMinutes", "source"]
    with open(csv_path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        for row in readings:
            writer.writerow({**row, "timestamp": row["timestamp"].isoformat()})

    print(f"\nGerado: {len(readings)} leituras horárias, {len(dailies)} resumos diários, "
          f"{len(insights)} insights  (CSV: {csv_path})")
    for doc_id, data in insights:
        print(f"  insight {doc_id}: {data['text']}")
    preview(dailies, today)

    if args.dry_run:
        print("\n--dry-run: nada foi enviado ao Firestore.")
        return

    print("\nEnviando ao Firestore:")
    commit_all(db, [(COL_DAILY, f"{d['environmentId']}_{d['dateKey']}", d) for d in dailies],
               COL_DAILY)
    commit_all(db, [(COL_INSIGHTS, doc_id, data) for doc_id, data in insights], COL_INSIGHTS)
    commit_all(db, [(COL_READINGS, f"{r['environmentId']}_{r['dateKey']}_{r['hour']:02d}", r)
                    for r in readings], COL_READINGS)
    print("\nPronto! Confira no Console do Firebase > Firestore Database.")


if __name__ == "__main__":
    main()
