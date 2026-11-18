
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime, date, timedelta
from zoneinfo import ZoneInfo
import random


# ==========================================
# CONEXÃO COM O FIREBASE
# ==========================================

cred = credentials.Certificate(
    "../../VisaoComputacional/flowcampus-admin.json"
)

if not firebase_admin._apps:
    firebase_admin.initialize_app(cred)

db = firestore.client()


# ==========================================
# AMBIENTES
# ==========================================

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


# ==========================================
# CONFIGURAÇÕES
# ==========================================

inicio = date(2026, 11, 17)
fim = date(2026, 11, 18)

fuso = ZoneInfo("America/Sao_Paulo")

# Horários das leituras simuladas: 07h até 22h
horarios = range(7, 23)

random.seed(14)


# ==========================================
# CONTROLE DOS CADASTROS
# ==========================================

operacoes = []

leituras_criadas = 0
leituras_preservadas = 0

estatisticas_criadas = 0
estatisticas_preservadas = 0


# ==========================================
# GERAÇÃO DOS DADOS
# ==========================================

for ambiente in ambientes:

    ambiente_id = ambiente["id"]
    nome = ambiente["name"]
    capacidade = ambiente["capacity"]

    dia = inicio

    print(f"\nGerando dados: {nome}")

    while dia <= fim:

        date_key = dia.strftime("%Y-%m-%d")

        leituras_do_dia = []

        for hora in horarios:

            # ID único para cada ambiente e horário
            documento_id = (
                f"{ambiente_id}_{dia.strftime('%Y%m%d')}_{hora:02d}"
            )

            referencia = (
                db.collection("occupancy_readings")
                .document(documento_id)
            )

            documento_existente = referencia.get()

            if documento_existente.exists:

                # Preserva a leitura existente
                dados = documento_existente.to_dict()

                leituras_preservadas += 1

                print(f"Leitura já existe: {documento_id}")

            else:

                # Simula variações de ocupação ao longo do dia
                if 11 <= hora <= 14:
                    ocupacao = random.randint(55, 90)
                elif 17 <= hora <= 20:
                    ocupacao = random.randint(45, 85)
                elif 7 <= hora <= 9:
                    ocupacao = random.randint(15, 45)
                else:
                    ocupacao = random.randint(20, 65)

                pessoas = round(
                    capacidade * ocupacao / 100
                )

                entradas = random.randint(1, 15)

                espera = round(
                    random.uniform(0.5, 4.0), 1
                )

                timestamp = datetime(
                    dia.year,
                    dia.month,
                    dia.day,
                    hora,
                    0,
                    0,
                    tzinfo=fuso
                )

                dados = {
                    "environmentId": ambiente_id,
                    "environmentName": nome,
                    "people": pessoas,
                    "capacity": capacidade,
                    "occupancyPercent": ocupacao,
                    "entries": entradas,
                    "waitMinutes": espera,
                    "timestamp": timestamp,
                    "source": "simulated"
                }

                operacoes.append((referencia, dados))

                leituras_criadas += 1

                print(
                    f"Leitura criada: {documento_id} "
                    f"| Pessoas: {pessoas} "
                    f"| Ocupação: {ocupacao}%"
                )

            # Inclui leituras novas e existentes
            leituras_do_dia.append(dados)

        # ======================================
        # ESTATÍSTICAS DIÁRIAS
        # ======================================

        documento_id = f"{ambiente_id}_{date_key}"

        referencia_diaria = (
            db.collection("daily_stats")
            .document(documento_id)
        )

        if referencia_diaria.get().exists:

            # Preserva o documento diário existente
            estatisticas_preservadas += 1

            print(f"Estatística diária já existe: {documento_id}")

        else:

            ocupacoes = [
                leitura.get("occupancyPercent", 0)
                for leitura in leituras_do_dia
            ]

            esperas = [
                leitura.get("waitMinutes", 0)
                for leitura in leituras_do_dia
            ]

            entradas = [
                leitura.get("entries", 0)
                for leitura in leituras_do_dia
            ]

            pessoas = [
                leitura.get("people", 0)
                for leitura in leituras_do_dia
            ]

            # Identifica o horário de maior ocupação
            indice_pico = ocupacoes.index(max(ocupacoes))

            horario_pico = list(horarios)[indice_pico]

            data_timestamp = datetime(
                dia.year,
                dia.month,
                dia.day,
                0,
                0,
                0,
                tzinfo=fuso
            )

            documento = {
                "environmentId": ambiente_id,
                "environmentName": nome,
                "dateKey": date_key,
                "date": data_timestamp,
                "totalVisitors": sum(entradas),
                "capacity": capacidade,
                "avgOccupancyPercent": round(
                    sum(ocupacoes) / len(ocupacoes), 1
                ),
                "avgWaitMinutes": round(
                    sum(esperas) / len(esperas), 1
                ),
                "peakHour": horario_pico,
                "peakOccupancyPercent": max(ocupacoes),
                "peakPeople": max(pessoas),
                "source": "simulated"
            }

            operacoes.append((referencia_diaria, documento))

            estatisticas_criadas += 1

            print(f"Estatística diária criada: {documento_id}")

        dia += timedelta(days=1)


# ==========================================
# GRAVAÇÃO NO FIREBASE
# ==========================================

# O Firestore permite no máximo 500 operações
# por lote. Aqui usamos lotes de até 400.

for i in range(0, len(operacoes), 400):

    lote = db.batch()

    grupo = operacoes[i:i + 400]

    for referencia, documento in grupo:
        lote.set(referencia, documento)

    lote.commit()

    print(f"\nLote enviado: {len(grupo)} documentos")


# ==========================================
# RESULTADO
# ==========================================

print("\n================================")
print("CADASTRO FINALIZADO!")
print("================================")

print(f"Ambientes: {len(ambientes)}")
print(f"Leituras horárias criadas: {leituras_criadas}")
print(f"Leituras horárias preservadas: {leituras_preservadas}")
print(f"Estatísticas diárias criadas: {estatisticas_criadas}")
print(f"Estatísticas diárias preservadas: {estatisticas_preservadas}")
print(f"Total de documentos enviados: {len(operacoes)}")
