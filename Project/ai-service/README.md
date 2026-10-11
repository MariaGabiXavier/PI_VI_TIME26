# FlowCampus AI (pasta `Project/ai-service`)

Servidor Python (FastAPI) que **treina KNN, SVM e Random Forest a cada requisição**,
compara o **MAE** dos três em dados que eles não viram, escolhe o de **menor erro** e usa
esse modelo para prever a lotação e gerar os insights do app.

## Como rodar

```powershell
cd Project\ai-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env          # depois edite o .env (DATA_SOURCE=firestore)
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Teste no navegador: <http://localhost:8000/docs> (abre uma página para chamar os endpoints).
Relatório no terminal (sem subir o servidor): `python -m forecasting.report`
Testes automáticos: `pip install -r requirements-dev.txt` e `pytest`.

## De onde vêm os dados

Coleção **`occupancy_readings`** do Firestore (uma leitura por hora de cada ambiente,
criada pelo `tools/seed_firestore.py`). `DATA_SOURCE=csv` lê o CSV do seed (para testar sem Firebase).
Os **dados** ficam em cache `DATA_CACHE_SECONDS` (padrão 10 min) para não gastar a cota de
leituras do plano gratuito (50 mil/dia). Os **modelos** são retreinados em TODA requisição.

## Como a IA funciona (`/api/forecast`)

1. Lê o histórico e ignora leituras "do futuro" (o seed gera o dia inteiro).
2. Features: ambiente, hora, dia da semana, fim de semana e a ocupação da **mesma hora
   há 7 e 14 dias**. O modelo aprende a **correção** em relação à semana passada.
3. **Validação por tempo**: treina com o passado e mede o MAE nos **últimos 7 dias**.
4. Treina KNN, SVM (SVR) e Random Forest, calcula o MAE de cada um e **escolhe o menor**.
5. Retreina o vencedor com todo o histórico e prevê as próximas horas de cada ambiente.
6. Gera os insights (pico previsto, melhor horário, modelo escolhido).

`baselineMae` na resposta é a referência "repetir a mesma hora da semana passada".

## Endpoints

| Método | Rota | Para quê |
|---|---|---|
| GET | `/health` | servidor no ar? |
| POST | `/predict` | ocupação da **próxima hora** de **um** ambiente (seu endpoint original) |
| GET | `/api/forecast?horizon_hours=48` | previsão das próximas horas de **todos** os ambientes + insights (tela Análises) |

Resposta de `/api/forecast` (resumo): `bestModelLabel`, `models[] {label, mae, isBest}`,
`baselineMae`, `forecasts[] {environmentName, points[] {hour, predictedOccupancyPercent,
predictedPeople}, peak, bestTime}` e `insights[] {text, highlight, icon, tone}`.
Parâmetros opcionais: `environment_id` (filtra `forecasts`) e `now` (simula outro horário).

## Usar em outra tela do app (Kotlin)

```kotlin
val result = AiRepository().fetchForecast()          // suspend: chame dentro de LaunchedEffect
result.onSuccess { forecast ->
    val refeitorio = forecast.forEnvironment("Refeitório")   // pontos por hora, pico, melhor horário
}
```

## Segurança

- `serviceAccountKey.json` **nunca** vai para o Git (já está no `.gitignore`).
- Antes de publicar o servidor, use `REQUIRE_AUTH=true`: ele passa a exigir o token do Firebase Auth
  (o app já envia). Em produção use HTTPS e tire `usesCleartextTraffic` do AndroidManifest.
