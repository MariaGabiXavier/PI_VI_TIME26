"""Transforma as previsões em frases curtas para a seção "Insights da IA"."""
from __future__ import annotations

from datetime import datetime, timedelta
from typing import Dict, List

WEEKDAYS = ["segunda-feira", "terça-feira", "quarta-feira", "quinta-feira",
            "sexta-feira", "sábado", "domingo"]


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
    if "sala" in lowered or "estudo" in lowered:
        return "HOME"
    return "CLOCK"


def _comma(value: float) -> str:
    return f"{value:.1f}".replace(".", ",")


def _parse(timestamp: str) -> datetime:
    return datetime.fromisoformat(timestamp)


def build_insights(forecasts: List[dict], training, now: datetime) -> List[dict]:
    """3 frases: pico previsto (próximas 24h), melhor horário nesse mesmo dia e
    qual modelo a IA escolheu."""
    insights: List[dict] = []
    limit = now + timedelta(hours=24)

    candidates = []
    for forecast in forecasts:
        window = [p for p in forecast["points"] if _parse(p["timestamp"]) < limit]
        if window:
            candidates.append((forecast, max(window, key=lambda p: p["predictedOccupancyPercent"])))

    if candidates:
        busiest, peak = max(candidates, key=lambda c: c[1]["predictedOccupancyPercent"])
        name = busiest["environmentName"]
        pct = peak["predictedOccupancyPercent"]
        peak_moment = _parse(peak["timestamp"])
        label = day_label(peak_moment, now)
        when = f"{label} às {peak['hour']:02d}h"

        if pct >= 70:
            text = f"{name} deve lotar {when}, com cerca de {pct}% da capacidade."
            tone = "RED"
        elif pct >= 40:
            text = f"O pico previsto em {name} é {when}, com cerca de {pct}% da capacidade."
            tone = "AMBER"
        else:
            text = f"{name} deve ficar tranquilo: o pico previsto é {when}, com cerca de {pct}%."
            tone = "GREEN"
        insights.append({"text": text, "highlight": f"{pct}% da capacidade" if pct >= 40 else when,
                         "icon": _icon_for(name), "tone": tone})

        # Melhor horário = o mais vazio NO MESMO DIA do pico (recomendação útil:
        # "no dia em que vai lotar, vá neste horário").
        same_day = [p for p in busiest["points"]
                    if _parse(p["timestamp"]).date() == peak_moment.date()
                    and p["hour"] != peak["hour"]]
        if same_day:
            best = min(same_day, key=lambda p: p["predictedOccupancyPercent"])
            best_when = f"{label} às {best['hour']:02d}h"
            insights.append({
                "text": (f"Para evitar fila em {name}, o melhor horário previsto é "
                         f"{best_when}, com cerca de {best['predictedOccupancyPercent']}% de lotação."),
                "highlight": best_when, "icon": "CLOCK", "tone": "GREEN",
            })

    others = [s.label for s in training.scores]
    insights.append({
        "text": (f"A IA comparou {', '.join(others[:-1])} e {others[-1]} e escolheu "
                 f"{training.best_label}, com erro médio (MAE) de "
                 f"{_comma(min(s.mae for s in training.scores))} p.p."),
        "highlight": training.best_label, "icon": "BULB", "tone": "BLUE",
    })
    return insights
