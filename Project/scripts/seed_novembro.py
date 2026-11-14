
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, date, timedelta
from zoneinfo import ZoneInfo
import random


cred = credentials.Certificate("../../VisaoComputacional/flowcampus-admin.json")

if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()

ambientes = [
    {
        "id": "biblioteca",
        "name": "Biblioteca",
        "capacity": 80
    },
    {
        "id": "lab-a2",
        "name": "Lab A2",
        "capacity": 40
    },
    {
        "id": "ponto-de-onibus",
        "name": "Ponto de ônibus",
        "capacity": 70
    },
    {
        "id": "refeitorio",
        "name": "Refeitório",
        "capacity": 120
    },
    {
        "id": "sala-estudo-7a",
        "name": "Sala Estudo - 7A",
        "capacity": 80
    }
]

inicio = date(2026, 11, 1)
fim = date(2026, 11, 14)

fuso = ZoneInfo("America/Sao_Paulo")

random.seed(14)

batch = db.batch()

criados = 0
ignorados = 0

for ambiente in ambientes:

    ambiente_id = ambiente["id"]
    nome = ambiente["name"]
    capacidade = ambiente["capacity"]

    dia = inicio

    print(f"\nGerando dados: {nome}")

    while dia <= fim:

        date_key = dia.strftime("%Y-%m-%d")

        documento_id = f"{ambiente_id}_{date_key}"

        referencia = (
            db.collection("daily_stats")
            .document(documento_id)
        )

        # Preservar documentos existentes
        if referencia.get().exists:
            print(f"Já existe: {documento_id}")
            ignorados += 1
            dia += timedelta(days=1)
            continue


        visitantes = random.randint(
            capacidade * 2,
            capacidade * 5
        )

        ocupacao = random.randint(25, 70)

        pico_ocupacao = random.randint(
            max(ocupacao, 40), 90
        )

        espera = round(random.uniform(0.5, 4.0), 1)

        horario_pico = random.randint(10, 16)

        pico_pessoas = round(
            capacidade * pico_ocupacao / 100
        )

        data_timestamp = datetime(
            dia.year,
            dia.month,
            dia.day,
            0, 0, 0,
            tzinfo=fuso
        )

        documento = {
            "environmentId": ambiente_id,
            "environmentName": nome,
            "dateKey": date_key,
            "date": data_timestamp,
            "totalVisitors": visitantes,
            "capacity": capacidade,
            "avgOccupancyPercent": ocupacao,
            "avgWaitMinutes": espera,
            "peakHour": horario_pico,
            "peakOccupancyPercent": pico_ocupacao,
            "peakPeople": pico_pessoas,
            "source": "simulated"
        }

        batch.set(referencia, documento)

        criados += 1

        print(f"Criado: {documento_id}")

        dia += timedelta(days=1)


if criados > 0:
    batch.commit()

print("\n================================")
print("CADASTRO FINALIZADO!")
print("================================")
print(f"Ambientes: {len(ambientes)}")
print(f"Documentos criados: {criados}")
print(f"Documentos preservados: {ignorados}")
