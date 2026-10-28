"""Notificações automáticas do FlowCampus.

A cada rodada, o notificador:
  1. lê a ocupação mais recente de cada ambiente;
  2. compara com o que já avisou antes (guardado em notification_state);
  3. quando algo muda (lotou, fila crescendo, ficou vazio) cria a notificação
     no Firestore (aparece na tela Alertas) e manda o push para os celulares;
  4. de tempos em tempos pede à IA a previsão das próximas horas e avisa
     quando ela prevê um pico de lotação.

Rodar (dentro de Project/ai-service, com o .venv ativo):
    cd Project/ai-service
    source .venv/bin/activate
    -escolher uma das opcoes:
    python -m forecasting.notifier --dry-run     # só mostra o que enviaria
    python -m forecasting.notifier               # uma rodada de verdade
    python -m forecasting.notifier --loop        # fica rodando sozinho


Rode em UM computador só: dois notificadores ao mesmo tempo podem duplicar avisos.
"""
from __future__ import annotations

import argparse
import logging
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

import pandas as pd

from .config import Settings, get_settings
from .data_source import COLLECTION, LOCAL_TZ, get_firebase_app, load_readings
from .ml_pipeline import NotEnoughDataError
from .service import parse_now, run_forecast

logger = logging.getLogger("flowcampus.notifier")

# Têm que ser iguais aos do app (NotificationsPush.kt) e do tools/notify.py
TOPIC = "alerts"
CHANNEL_ID = "flowcampus_alerts"

NOTIFICATIONS_COLLECTION = "notifications"
STATE_COLLECTION = "notification_state"

# --- Regras (ajuste aqui) -------------------------------------------------
HIGH_AT = 70                 # % a partir da qual a lotação é "alta"
LOW_AT = 40                  # abaixo disso a lotação é "baixa"
EMPTY_AT = 25                # abaixo disso o ambiente está "vazio"
WAIT_AT = 10                 # minutos de espera que viram aviso de fila
COOLDOWN_MINUTES = 90        # tempo mínimo entre dois avisos iguais do mesmo ambiente
FORECAST_HORIZON_HOURS = 3   # quantas horas à frente a previsão é considerada
RECENT_HOURS = 3             # janela usada para achar a leitura mais recente
STALE_AFTER_HOURS = 2        # leitura mais velha que isso é ignorada
ACTIVE_FROM_HOUR = 7         # só envia avisos entre esses horários (horário local)
ACTIVE_UNTIL_HOUR = 22


def level_for(percent: int) -> str:
    if percent >= HIGH_AT:
        return "HIGH"
    if percent >= LOW_AT:
        return "MEDIUM"
    return "LOW"


# --------------------------------------------------------------------------
# Estado: lembra o que já foi avisado (para não repetir a cada rodada)
# --------------------------------------------------------------------------
class StateStore:
    """Guarda o estado de cada ambiente. Sem banco (db=None), guarda só na memória."""

    def __init__(self, db=None):
        self.db = db
        self.memory: Dict[str, dict] = {}

    def get(self, env_id: str) -> dict:
        if self.db is None:
            return dict(self.memory.get(env_id, {}))
        snapshot = self.db.collection(STATE_COLLECTION).document(env_id).get()
        data = snapshot.to_dict() if snapshot.exists else None
        return data or {}

    def save(self, env_id: str, state: dict, changes: dict,
             sent_kinds: List[str], now: datetime) -> None:
        merged = dict(state)
        merged.update(changes)

        last_sent = dict(state.get("lastSent", {}))
        for kind in sent_kinds:
            last_sent[kind] = now
        merged["lastSent"] = last_sent
        merged["updatedAt"] = now

        if self.db is None:
            self.memory[env_id] = merged
        else:
            self.db.collection(STATE_COLLECTION).document(env_id).set(merged)


def cooled_down(state: dict, kind: str, now: datetime) -> bool:
    """True se já passou tempo suficiente desde o último aviso desse tipo."""
    last = state.get("lastSent", {}).get(kind)
    if last is None:
        return True
    return now - last >= timedelta(minutes=COOLDOWN_MINUTES)


# --------------------------------------------------------------------------
# Envio: grava a notificação no Firestore e manda o push
# --------------------------------------------------------------------------
class Sender:
    def __init__(self, db, dry_run: bool):
        self.db = db
        self.dry_run = dry_run

    def send(self, message: dict, environment_id: str, source: str) -> None:
        if self.dry_run:
            logger.info("[dry-run] (%s) %s -> %s", environment_id, message["title"],
                        message["description"])
            return

        from firebase_admin import firestore, messaging

        _, doc_ref = self.db.collection(NOTIFICATIONS_COLLECTION).add({
            "title": message["title"],
            "description": message["description"],
            "category": message["category"],
            "icon": message["icon"],
            "tone": message["tone"],
            "environmentId": environment_id,
            "source": source,
            "createdAt": firestore.SERVER_TIMESTAMP,
        })
        logger.info("Notificação criada (%s): %s", doc_ref.id, message["title"])

        # Se o push falhar, a notificação continua na lista do app
        try:
            messaging.send(messaging.Message(
                topic=TOPIC,
                notification=messaging.Notification(
                    title=message["title"], body=message["description"]),
                data={"environmentId": environment_id, "notificationId": doc_ref.id},
                android=messaging.AndroidConfig(
                    priority="high",
                    notification=messaging.AndroidNotification(channel_id=CHANNEL_ID),
                ),
            ))
        except Exception:
            logger.exception("O push não foi enviado (a notificação foi gravada)")


# --------------------------------------------------------------------------
# Leitura da ocupação atual
# --------------------------------------------------------------------------
def read_current(settings: Settings, db, now: datetime) -> Dict[str, dict]:
    """Última leitura (até agora) de cada ambiente, dentro da janela recente."""
    start = now - timedelta(hours=RECENT_HOURS)

    if settings.data_source == "csv":
        frame = load_readings(settings)
        frame = frame[(frame["ts"] >= start) & (frame["ts"] <= now)]
        rows = frame.to_dict("records")
    else:
        from google.cloud.firestore_v1.base_query import FieldFilter

        query = (db.collection(COLLECTION)
                 .where(filter=FieldFilter("timestamp", ">=", start))
                 .where(filter=FieldFilter("timestamp", "<=", now)))
        rows = []
        for doc in query.stream():
            data = doc.to_dict()
            data["ts"] = pd.Timestamp(data["timestamp"]).tz_convert(LOCAL_TZ)
            rows.append(data)

    latest: Dict[str, dict] = {}
    for row in rows:
        env_id = row["environmentId"]
        if env_id not in latest or row["ts"] > latest[env_id]["ts"]:
            latest[env_id] = row
    return latest


# --------------------------------------------------------------------------
# Regras
# --------------------------------------------------------------------------
def current_rules(reading: dict, state: dict) -> Tuple[List[Tuple[str, dict]], dict]:
    """Avisos baseados na ocupação de agora. Só avisa quando algo MUDA."""
    name = str(reading["environmentName"])
    percent = int(round(float(reading["occupancyPercent"])))
    wait = float(reading.get("waitMinutes") or 0)
    level = level_for(percent)
    known = bool(state)          # já vimos esse ambiente antes?

    found: List[Tuple[str, dict]] = []

    if level == "HIGH" and state.get("level") != "HIGH":
        found.append(("high", {
            "title": f"Lotação alta em {name}",
            "description": (f"{percent}% da capacidade ocupada. "
                            "Considere ir em outro ambiente ou horário."),
            "category": "ALERT", "icon": "OCCUPANCY", "tone": "RED",
        }))

    if wait >= WAIT_AT and not state.get("waitHigh", False):
        found.append(("wait", {
            "title": f"Fila em {name}",
            "description": f"O tempo de espera está aumentando. Atualmente ~{wait:.0f} min.",
            "category": "ALERT", "icon": "WAIT_TIME", "tone": "ORANGE",
        }))

    if known and percent <= EMPTY_AT and not state.get("isEmpty", False):
        found.append(("empty", {
            "title": f"{name} com pouca gente",
            "description": f"Só {percent}% da capacidade ocupada. É o momento perfeito para ir!",
            "category": "UPDATE", "icon": "EMPTY_ROOM", "tone": "GREEN",
        }))

    new_state = {"level": level, "waitHigh": wait >= WAIT_AT, "isEmpty": percent <= EMPTY_AT}
    return found, new_state


def forecast_rule(forecast: dict, current_percent: int, state: dict,
                  now: datetime) -> Optional[Tuple[dict, dict]]:
    """Aviso baseado na previsão da IA: pico de lotação nas próximas horas."""
    limit = now + timedelta(hours=FORECAST_HORIZON_HOURS)
    points = [p for p in forecast["points"]
              if now < datetime.fromisoformat(p["timestamp"]) <= limit]
    if not points:
        return None

    peak = max(points, key=lambda p: p["predictedOccupancyPercent"])
    percent = peak["predictedOccupancyPercent"]

    # Só avisa se vai lotar e o ambiente ainda NÃO está lotado agora
    if percent < HIGH_AT or current_percent >= HIGH_AT:
        return None

    key = peak["timestamp"][:13]            # "2026-10-12T14": um aviso por pico
    if state.get("forecastKey") == key:
        return None

    message = {
        "title": "Previsão de Pico",
        "description": (f"A IA prevê lotação alta em {forecast['environmentName']} às "
                        f"{peak['hour']:02d}h (cerca de {percent}% da capacidade). "
                        "Planeje-se para ir mais cedo ou mais tarde."),
        "category": "UPDATE", "icon": "FORECAST", "tone": "PURPLE",
    }
    return message, {"forecastKey": key}


# --------------------------------------------------------------------------
# Uma rodada
# --------------------------------------------------------------------------
def run_once(settings: Settings, store: StateStore, sender: Sender,
             now: datetime, with_forecast: bool) -> int:
    in_active_hours = ACTIVE_FROM_HOUR <= now.hour < ACTIVE_UNTIL_HOUR
    current = read_current(settings, sender.db, now)
    sent = 0

    if not current:
        logger.warning("Nenhuma leitura nas últimas %dh. Os dados simulados acabaram? "
                       "Rode o seed_firestore.py para estender o histórico até hoje.",
                       RECENT_HOURS)

    for env_id, reading in current.items():
        age = now - reading["ts"].to_pydatetime()
        if age > timedelta(hours=STALE_AFTER_HOURS):
            logger.warning("Leitura de %s está velha (%s). Ignorando.",
                           reading["environmentName"], age)
            continue

        state = store.get(env_id)
        found, changes = current_rules(reading, state)

        sent_kinds: List[str] = []
        if in_active_hours:
            for kind, message in found:
                if cooled_down(state, kind, now):
                    sender.send(message, env_id, "rules")
                    sent_kinds.append(kind)
                else:
                    logger.info("Aviso '%s' de %s em espera (cooldown).", kind, env_id)

        store.save(env_id, state, changes, sent_kinds, now)
        sent += len(sent_kinds)

    if with_forecast and current:
        sent += run_forecast_step(settings, store, sender, current, now, in_active_hours)

    logger.info("Rodada concluída: %d notificação(ões).", sent)
    return sent


def run_forecast_step(settings: Settings, store: StateStore, sender: Sender,
                      current: Dict[str, dict], now: datetime,
                      in_active_hours: bool) -> int:
    try:
        readings = load_readings(settings, force=True)
        result = run_forecast(readings, now, FORECAST_HORIZON_HOURS + 1, None,
                              settings.validation_days)
    except NotEnoughDataError as error:
        logger.warning("IA sem dados suficientes: %s", error)
        return 0
    except Exception:
        logger.exception("Falha ao pedir a previsão à IA")
        return 0

    logger.info("IA: modelo escolhido = %s", result["bestModelLabel"])
    sent = 0

    for forecast in result["forecasts"]:
        env_id = forecast["environmentId"]
        reading = current.get(env_id)
        if reading is None:
            continue

        state = store.get(env_id)
        current_percent = int(round(float(reading["occupancyPercent"])))
        outcome = forecast_rule(forecast, current_percent, state, now)
        if outcome is None:
            continue

        message, changes = outcome
        sent_kinds: List[str] = []
        if in_active_hours and cooled_down(state, "forecast", now):
            sender.send(message, env_id, "ai")
            sent_kinds.append("forecast")
            sent += 1

        store.save(env_id, state, changes, sent_kinds, now)

    return sent


# --------------------------------------------------------------------------
# Linha de comando
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(description="Notificações automáticas do FlowCampus")
    parser.add_argument("--loop", action="store_true", help="repete sem parar")
    parser.add_argument("--interval-minutes", type=int, default=5,
                        help="intervalo entre rodadas com --loop (padrão: 5)")
    parser.add_argument("--forecast-every-minutes", type=int, default=360,
                        help="de quanto em quanto tempo consulta a IA (padrão: 360)")
    parser.add_argument("--no-forecast", action="store_true",
                        help="não consulta a IA (só as regras de ocupação)")
    parser.add_argument("--dry-run", action="store_true",
                        help="só mostra o que enviaria; não grava nem manda push")
    parser.add_argument("--now", default=None,
                        help="simula outro horário, ex: 2026-10-11T12:00:00-03:00")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

    settings = get_settings()

    db = None
    if not (args.dry_run and settings.data_source == "csv"):
        from firebase_admin import firestore
        db = firestore.client(get_firebase_app(settings))

    store = StateStore(None if args.dry_run else db)
    sender = Sender(db, args.dry_run)

    last_forecast = 0.0
    while True:
        now = parse_now(args.now)
        due = (not args.no_forecast
               and time.time() - last_forecast >= args.forecast_every_minutes * 60)
        try:
            run_once(settings, store, sender, now, due)
        except Exception:
            logger.exception("Falha na rodada")
        if due:
            last_forecast = time.time()       # mesmo se falhar: não insiste e gasta cota

        if not args.loop:
            break
        time.sleep(args.interval_minutes * 60)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nEncerrado.")