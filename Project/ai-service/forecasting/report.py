"""Relatório no terminal (sem subir a API): treina, mostra o MAE de cada modelo,
quem venceu, as previsões e os insights.

  python -m forecasting.report
  python -m forecasting.report --now 2026-10-07T09:30:00-03:00 --horizon 24
"""
from __future__ import annotations

import argparse
import sys

from .config import get_settings
from .data_source import load_readings
from .service import parse_now, run_forecast

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass


def main() -> None:
    parser = argparse.ArgumentParser(description="Treina KNN, SVM e Random Forest e compara o MAE")
    parser.add_argument("--now", default=None, help="horário de referência (ISO 8601)")
    parser.add_argument("--horizon", type=int, default=48, help="horas à frente")
    args = parser.parse_args()

    settings = get_settings()
    now = parse_now(args.now)
    result = run_forecast(load_readings(settings), now, args.horizon,
                          validation_days=settings.validation_days)

    print(f"Referência (agora): {result['now']}")
    print(f"Treino: {result['trainingRows']} linhas | Validação: {result['validationRows']} linhas "
          f"(últimos {result['validationDays']} dias)\n")
    print(f"{'Modelo':<16}{'MAE (p.p.)':>12}{'tempo':>9}")
    for model in sorted(result["models"], key=lambda m: m["mae"]):
        mark = "  <- escolhido" if model["isBest"] else ""
        print(f"{model['label']:<16}{model['mae']:>12.2f}{model['trainSeconds']:>8.2f}s{mark}")
    print(f"{'Semana passada':<16}{result['baselineMae']:>12.2f}   (referência: repetir a mesma hora de 7 dias atrás)\n")

    print(f"Previsão das próximas {args.horizon}h:")
    for env in result["forecasts"]:
        if env["peak"]:
            print(f"  {env['environmentName']:<18} pico {env['peak']['label']} {env['peak']['hour']:02d}h "
                  f"({env['peak']['occupancyPercent']}%)  |  melhor horário "
                  f"{env['bestTime']['label']} {env['bestTime']['hour']:02d}h ({env['bestTime']['occupancyPercent']}%)")
        else:
            print(f"  {env['environmentName']:<18} sem horário de funcionamento nesta janela")

    print("\nInsights:")
    for item in result["insights"]:
        print(f"  [{item['tone']}/{item['icon']}] {item['text']}")
    print(f"\nTempo total: {result['totalSeconds']}s")


if __name__ == "__main__":
    main()
