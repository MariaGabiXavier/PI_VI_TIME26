import os
from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO
from collections import deque
import time

import firebase_admin
from firebase_admin import credentials, firestore

BASE_DIR = Path(__file__).resolve().parent


# ==========================================
# CONFIGURACAO FIREBASE
# ==========================================

CAMINHO_CHAVE = os.getenv(
    "GOOGLE_APPLICATION_CREDENTIALS",
    str(BASE_DIR / "flowcampus-admin.json")
)

AMBIENTE_ID = "0FxxRMIHhOblsb62y8vk"

INTERVALO_ENVIO = 30

try:
    credencial = credentials.Certificate(CAMINHO_CHAVE)

    firebase_admin.initialize_app(credencial)

    db = firestore.client()

    # Busca todos os ambientes cadastrados no app
    ambientes = list(db.collection("environments").stream())

    if len(ambientes) == 0:
        raise Exception("Nenhum ambiente cadastrado no Firebase.")

    print("Ambientes cadastrados:")

    for i in range(len(ambientes)):
        nome = ambientes[i].to_dict()["name"]
        print(f"{i + 1} - {nome}")

    escolha = int(input("Qual ambiente esta camera vai monitorar? "))

    ambiente_doc = ambientes[escolha - 1]

    AMBIENTE_ID = ambiente_doc.id

    dados_ambiente = ambiente_doc.to_dict()

    NOME_AMBIENTE = dados_ambiente["name"]
    CAPACIDADE = int(dados_ambiente["capacity"])

    print("Firebase conectado!")
    print(f"Ambiente: {NOME_AMBIENTE}")
    print(f"Capacidade: {CAPACIDADE}")

except Exception as erro:
    print(f"Erro ao conectar ao Firebase: {erro}")
    exit()


# ==========================================
# CARREGAR MODELO YOLO POSE
# ==========================================

print("Carregando modelo de deteccao...")

modelo = YOLO(str(BASE_DIR / "yolo11s-pose.pt"))
print("Modelo carregado com sucesso!")


# ==========================================
# ABRIR CAMERA
# ==========================================

#camera do PC
#camera = cv2.VideoCapture(0, cv2.CAP_DSHOW)

#Camera celular
URL_CAMERA = input("Endereco da camera (ex: http://192.168.15.30:8080/video): ")
camera = cv2.VideoCapture(URL_CAMERA)

if not camera.isOpened():
    print("Erro ao acessar a webcam!")
    exit()

camera.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

print("Monitoramento iniciado!")
print("Pressione Q para encerrar.")


# ==========================================
# CONFIGURACOES DA DETECCAO
# ==========================================

historico = deque(maxlen=7)

CONFIANCA_DETECCAO = 0.35
CONFIANCA_PONTOS = 0.35
MINIMO_PONTOS = 5

conexoes = [
    (5, 6), (5, 7), (7, 9),
    (6, 8), (8, 10),
    (5, 11), (6, 12),
    (11, 12),
    (11, 13), (13, 15),
    (12, 14), (14, 16)
]


# ==========================================
# VALIDAR PESSOA
# ==========================================

def pessoa_valida(pontos, confiancas):

    validos = confiancas >= CONFIANCA_PONTOS

    quantidade = int(np.sum(validos))

    if quantidade < MINIMO_PONTOS:
        return False

    nariz = validos[0]

    ombro_esquerdo = validos[5]
    ombro_direito = validos[6]

    quadril_esquerdo = validos[11]
    quadril_direito = validos[12]

    ombros = ombro_esquerdo and ombro_direito

    referencia_corporal = (
        nariz or
        (quadril_esquerdo and quadril_direito)
    )

    return ombros and referencia_corporal


# ==========================================
# DESENHAR ESQUELETO
# ==========================================

def desenhar_esqueleto(imagem, pontos, confiancas):

    for inicio, fim in conexoes:

        if (
            confiancas[inicio] >= CONFIANCA_PONTOS
            and confiancas[fim] >= CONFIANCA_PONTOS
        ):

            x1, y1 = map(int, pontos[inicio])
            x2, y2 = map(int, pontos[fim])

            cv2.line(
                imagem,
                (x1, y1),
                (x2, y2),
                (0, 255, 255),
                2
            )

    for i, ponto in enumerate(pontos):

        if confiancas[i] >= CONFIANCA_PONTOS:

            x, y = map(int, ponto)

            cv2.circle(
                imagem,
                (x, y),
                4,
                (255, 0, 0),
                -1
            )


# ==========================================
# LOOP PRINCIPAL
# ==========================================

ultima_gravacao = 0

try:

    while True:

        sucesso, frame = camera.read()

        if not sucesso:
            print("Erro ao capturar imagem da camera.")
            break

        resultados = modelo(
            frame,
            conf=CONFIANCA_DETECCAO,
            verbose=False
        )

        resultado = resultados[0]

        quantidade_pessoas = 0

        # Verificar se existem pontos corporais
        if (
            resultado.keypoints is not None
            and resultado.keypoints.conf is not None
            and resultado.boxes is not None
        ):

            for i in range(len(resultado.boxes)):

                classe = int(resultado.boxes.cls[i].item())

                # Classe 0 representa pessoa
                if classe != 0:
                    continue

                pontos = (
                    resultado.keypoints.xy[i]
                    .cpu()
                    .numpy()
                )

                confiancas = (
                    resultado.keypoints.conf[i]
                    .cpu()
                    .numpy()
                )

                # Validar se realmente parece uma pessoa
                if pessoa_valida(pontos, confiancas):

                    quantidade_pessoas += 1

                    x1, y1, x2, y2 = map(
                        int,
                        resultado.boxes.xyxy[i].cpu().numpy()
                    )

                    cv2.rectangle(
                        frame,
                        (x1, y1),
                        (x2, y2),
                        (0, 255, 0),
                        2
                    )

                    desenhar_esqueleto(
                        frame,
                        pontos,
                        confiancas
                    )

                    cv2.putText(
                        frame,
                        "Pessoa validada",
                        (x1, max(y1 - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        (0, 255, 0),
                        2
                    )

        # ==================================
        # SUAVIZAR CONTAGEM
        # ==================================

        historico.append(quantidade_pessoas)

        quantidade_estavel = int(
            np.median(historico)
        )

        porcentagem = (
            quantidade_estavel / CAPACIDADE
        ) * 100

        # ==================================
        # ENVIAR DADOS PARA O FIREBASE
        # ==================================

        agora = time.monotonic()

        if agora - ultima_gravacao >= INTERVALO_ENVIO:

            try:

                db.collection("camera_readings").add({

                    "environmentId": AMBIENTE_ID,

                    "environmentName": NOME_AMBIENTE,

                    "people": quantidade_estavel,

                    "occupancyPercent": round(
                        porcentagem, 2
                    ),

                    "source": "computer_vision",

                    "timestamp": firestore.SERVER_TIMESTAMP

                })

                ultima_gravacao = agora

                print(
                    f"Firebase atualizado: "
                    f"{quantidade_estavel} pessoas "
                    f"({porcentagem:.1f}%)"
                )

            except Exception as erro:

                print(
                    f"Erro ao enviar dados para o Firebase: {erro}"
                )

        # ==================================
        # INFORMACOES NA TELA
        # ==================================

        cv2.putText(
            frame,
            f"Ambiente: {NOME_AMBIENTE}",
            (20, 35),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (255, 255, 255),
            2
        )

        cv2.putText(
            frame,
            f"Pessoas: {quantidade_estavel}",
            (20, 75),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (0, 255, 0),
            2
        )

        cv2.putText(
            frame,
            f"Ocupacao: {porcentagem:.1f}%",
            (20, 115),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.8,
            (0, 255, 255),
            2
        )

        cv2.imshow(
            f"FlowCampus - {NOME_AMBIENTE}",
            frame
        )

        # Encerrar ao pressionar Q
        if cv2.waitKey(1) & 0xFF == ord("q"):
            break


finally:

    camera.release()

    cv2.destroyAllWindows()

    print("Monitoramento encerrado.")
