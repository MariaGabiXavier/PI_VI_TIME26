
"""Gera insights práticos de ocupação para o FlowCampus."""
from __future__ import annotations

from datetime import datetime, timedelta
from typing import List


WEEKDAYS = [
    "segunda-feira", "terça-feira", "quarta-feira",
    "quinta-feira", "sexta-feira", "sábado", "domingo"
]

HIGH_OCCUPANCY = 70.0
VERY_HIGH_OCCUPANCY = 85.0
MIN_DIFFERENCE = 10.0


def day_label(moment, now: datetime) -> str:
    diff = (moment.date() - now.date()).days

    if diff == 0:
        return "hoje"
    if diff == 1:
        return "amanhã"

    return WEEKDAYS[moment.weekday()]


def _icon_for(name: str) -> str:
    lowered = name.lower()

    if "ônibus" in lowered or "onibus" in lowered:
        return "BUS"
    if "biblioteca" in lowered or "livraria" in lowered:
        return "BOOK"
    if "refeit" in lowered or "praça" in lowered or "alimenta" in lowered:
        return "FOOD"
    if "sala" in lowered or "estudo" in lowered or "lab" in lowered:
        return "HOME"

    return "CLOCK"


def _parse(timestamp: str) -> datetime:
    return datetime.fromisoformat(timestamp)


def _point_time(point: dict, now: datetime) -> str:
    moment = _parse(point["timestamp"])
    return f"{day_label(moment, now)} às {moment.hour:02d}h"


def _get_candidates(forecasts: List[dict], now: datetime) -> list:
    """Obtém previsões das próximas 24 horas."""

    limit = now + timedelta(hours=24)
    candidates = []

    for forecast in forecasts:
        for point in forecast.get("points", []):
            moment = _parse(point["timestamp"])

            if now < moment < limit:
                candidates.append({
                    "forecast": forecast,
                    "point": point,
                    "moment": moment,
                    "occupancy": float(
                        point["predictedOccupancyPercent"]
                    ),
                })

    return candidates


def _occupancy_insight(candidates: list, now: datetime) -> dict | None:
    """Alerta sobre o maior nível de ocupação previsto."""

    if not candidates:
        return None

    peak = max(candidates, key=lambda item: item["occupancy"])

    name = peak["forecast"]["environmentName"]
    pct = peak["occupancy"]
    when = _point_time(peak["point"], now)

    if pct >= VERY_HIGH_OCCUPANCY:
        text = (
            f"Atenção: {name} deve atingir ocupação muito alta "
            f"{when}, chegando a aproximadamente {pct:.0f}% "
            f"da capacidade. Se possível, evite esse horário."
        )
        tone = "RED"

    elif pct >= HIGH_OCCUPANCY:
        text = (
            f"{name} deve ficar bastante movimentado {when}, "
            f"com previsão de {pct:.0f}% de ocupação. "
            f"Considere outro horário."
        )
        tone = "AMBER"

    else:
        text = (
            f"O maior movimento previsto é em {name} {when}, "
            f"com aproximadamente {pct:.0f}% de ocupação."
        )
        tone = "GREEN"

    return {
        "text": text,
        "highlight": f"{pct:.0f}% de ocupação",
        "icon": _icon_for(name),
        "tone": tone,
    }


def _recommendation_insight(
    candidates: list,
    now: datetime
) -> dict | None:
    """Indica outro ambiente ou horário menos movimentado."""

    if len(candidates) < 2:
        return None

    by_time = {}

    for item in candidates:
        by_time.setdefault(item["moment"], []).append(item)

    comparisons = []

    # Primeiro, compara ambientes no mesmo horário.
    for moment, items in by_time.items():
        if len(items) < 2:
            continue

        ordered = sorted(items, key=lambda item: item["occupancy"])
        difference = ordered[-1]["occupancy"] - ordered[0]["occupancy"]

        if difference >= MIN_DIFFERENCE:
            comparisons.append((
                difference,
                ordered[0],
                ordered[-1],
                "environment",
            ))

    if comparisons:
        difference, quietest, busiest, _ = max(
            comparisons,
            key=lambda item: item[0]
        )

        quiet_name = quietest["forecast"]["environmentName"]
        busy_name = busiest["forecast"]["environmentName"]
        pct = quietest["occupancy"]
        when = _point_time(quietest["point"], now)

        return {
            "text": (
                f"Uma alternativa ao {busy_name} é a {quiet_name}. "
                f"{when.capitalize()}, a previsão é de {pct:.0f}% "
                f"de ocupação, contra {busiest['occupancy']:.0f}% "
                f"no outro ambiente."
            ),
            "highlight": quiet_name,
            "icon": _icon_for(quiet_name),
            "tone": "GREEN",
        }

    # Se não houver diferença relevante entre ambientes,
    # compara horários do mesmo ambiente no mesmo dia.
    by_environment_day = {}

    for item in candidates:
        key = (
            item["forecast"]["environmentId"],
            item["moment"].date(),
        )
        by_environment_day.setdefault(key, []).append(item)

    alternatives = []

    for items in by_environment_day.values():
        if len(items) < 2:
            continue

        ordered = sorted(items, key=lambda item: item["occupancy"])
        difference = ordered[-1]["occupancy"] - ordered[0]["occupancy"]

        if difference >= MIN_DIFFERENCE:
            alternatives.append((
                difference,
                ordered[0],
                ordered[-1],
            ))

    if not alternatives:
        return None

    difference, quietest, busiest = max(
        alternatives,
        key=lambda item: item[0]
    )

    name = quietest["forecast"]["environmentName"]
    pct = quietest["occupancy"]
    when = _point_time(quietest["point"], now)

    return {
        "text": (
            f"Quer encontrar {name} mais tranquilo? "
            f"A previsão indica {when}, com {pct:.0f}% de ocupação, "
            f"uma redução de {difference:.0f} pontos percentuais "
            f"em relação ao horário de maior movimento."
        ),
        "highlight": when,
        "icon": "CLOCK",
        "tone": "GREEN",
    }


def _trend_insight(candidates: list, now: datetime) -> dict | None:
    """Identifica aumento ou diminuição da ocupação nas próximas horas."""

    by_environment = {}

    for item in candidates:
        env_id = item["forecast"]["environmentId"]
        by_environment.setdefault(env_id, []).append(item)

    trends = []

    for items in by_environment.values():
        if len(items) < 3:
            continue

        items.sort(key=lambda item: item["moment"])

        first = items[0]
        last = items[-1]

        difference = last["occupancy"] - first["occupancy"]

        if abs(difference) < MIN_DIFFERENCE:
            continue

        trends.append((
            abs(difference),
            first,
            last,
            difference,
        ))

    if not trends:
        return None

    _, first, last, difference = max(
        trends,
        key=lambda item: item[0]
    )

    name = first["forecast"]["environmentName"]

    if difference > 0:
        text = (
            f"A movimentação em {name} deve aumentar nas próximas horas, "
            f"passando de {first['occupancy']:.0f}% para "
            f"{last['occupancy']:.0f}% de ocupação."
        )
        tone = "AMBER"
        icon = "TREND_UP"

    else:
        text = (
            f"A movimentação em {name} deve diminuir nas próximas horas, "
            f"passando de {first['occupancy']:.0f}% para "
            f"{last['occupancy']:.0f}% de ocupação. "
            f"Pode ser uma boa opção mais tarde."
        )
        tone = "GREEN"
        icon = "TREND_DOWN"

    return {
        "text": text,
        "highlight": f"{last['occupancy']:.0f}% de ocupação",
        "icon": icon,
        "tone": tone,
    }


def build_insights(
    forecasts: List[dict],
    training,
    now: datetime
) -> List[dict]:
    """Gera até três insights práticos para o usuário."""

    candidates = _get_candidates(forecasts, now)

    insights = []

    occupancy = _occupancy_insight(candidates, now)
    if occupancy:
        insights.append(occupancy)

    recommendation = _recommendation_insight(candidates, now)
    if recommendation:
        insights.append(recommendation)

    trend = _trend_insight(candidates, now)
    if trend:
        insights.append(trend)

    return insights[:3]
