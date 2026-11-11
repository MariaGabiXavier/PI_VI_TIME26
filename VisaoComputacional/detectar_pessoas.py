
import cv2
import numpy as np
from ultralytics import YOLO
from collections import deque

# ==========================================
# CONFIGURACOES DO FLOWCAMPUS
# ==========================================

MODELO = "yolo11s-pose.pt"

CONFIANCA_DETECCAO = 0.35
CONFIANCA_PONTOS = 0.35

# Quantidade minima de pontos corporais validos
MINIMO_PONTOS = 5

# Historico para estabilizar a contagem
historico = deque(maxlen=7)

# ==========================================
# CARREGAR MODELO
# ==========================================

print("Carregando modelo de deteccao de pose...")

try:
    modelo = YOLO(MODELO)
    print("Modelo carregado com sucesso!")

except Exception as erro:
    print(f"Erro ao carregar modelo: {erro}")
    exit()

# ==========================================
# ABRIR CAMERA
# ==========================================

#camera do PC
camera = cv2.VideoCapture(0, cv2.CAP_DSHOW)

#Camera do celular
#URL_CAMERA = "http://192.168.15.30:8080/video"
#camera = cv2.VideoCapture(URL_CAMERA)

if not camera.isOpened():
    print("Erro: nao foi possivel acessar a webcam.")
    exit()

camera.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

print("FlowCampus iniciado!")
print("Pressione Q para encerrar.")

# ==========================================
# CONEXOES DO ESQUELETO HUMANO (COCO)
# ==========================================

conexoes = [
    (5, 6), (5, 7), (7, 9),
    (6, 8), (8, 10),
    (5, 11), (6, 12),
    (11, 12),
    (11, 13), (13, 15),
    (12, 14), (14, 16)
]

# ==========================================
# VALIDAR ESTRUTURA CORPORAL
# ==========================================

def pessoa_valida(pontos, confiancas):

    # Verificar quantidade de pontos confiaveis
    validos = confiancas >= CONFIANCA_PONTOS

    quantidade = int(np.sum(validos))

    if quantidade < MINIMO_PONTOS:
        return False

    # Indices do modelo COCO:
    # 0 = nariz
    # 5 = ombro esquerdo
    # 6 = ombro direito
    # 11 = quadril esquerdo
    # 12 = quadril direito

    nariz = validos[0]
    ombro_esquerdo = validos[5]
    ombro_direito = validos[6]

    quadril_esquerdo = validos[11]
    quadril_direito = validos[12]

    # Exigir os dois ombros e uma referencia
    # adicional de estrutura corporal.
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
# MONITORAMENTO
# ==========================================

try:

    while True:

        sucesso, imagem = camera.read()

        if not sucesso:
            print("Falha ao capturar imagem.")
            break

        # Executar deteccao de pose
        resultados = modelo(
            imagem,
            conf=CONFIANCA_DETECCAO,
            verbose=False
        )

        quantidade_pessoas = 0

        for resultado in resultados:

            if resultado.keypoints is None:
                continue

            if resultado.keypoints.conf is None:
                continue

            for i in range(len(resultado.boxes)):

                classe = int(resultado.boxes.cls[i])

                # Somente classe pessoa
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

                # Validar estrutura corporal
                if not pessoa_valida(pontos, confiancas):
                    continue

                quantidade_pessoas += 1

                x1, y1, x2, y2 = map(
                    int,
                    resultado.boxes.xyxy[i].cpu().numpy()
                )

                # Desenhar caixa da pessoa
                cv2.rectangle(
                    imagem,
                    (x1, y1),
                    (x2, y2),
                    (0, 255, 0),
                    2
                )

                cv2.putText(
                    imagem,
                    "Pessoa validada",
                    (x1, max(y1 - 10, 20)),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.6,
                    (0, 255, 0),
                    2
                )

                # Desenhar pontos corporais
                desenhar_esqueleto(
                    imagem,
                    pontos,
                    confiancas
                )

        # ==================================
        # ESTABILIZAR CONTAGEM
        # ==================================

        historico.append(quantidade_pessoas)

        contagem_estavel = int(
            np.median(list(historico))
        )

        # ==================================
        # EXIBIR RESULTADO
        # ==================================

        cv2.rectangle(
            imagem,
            (10, 10),
            (480, 65),
            (0, 0, 0),
            -1
        )

        cv2.putText(
            imagem,
            f"Pessoas: {contagem_estavel}",
            (25, 48),
            cv2.FONT_HERSHEY_SIMPLEX,
            1,
            (0, 255, 0),
            2
        )

        cv2.imshow(
            "FlowCampus - Monitoramento Inteligente",
            imagem
        )

        if cv2.waitKey(1) & 0xFF == ord("q"):
            break

except Exception as erro:
    print(f"Erro durante o monitoramento: {erro}")

finally:
    camera.release()
    cv2.destroyAllWindows()
    print("Monitoramento encerrado.")
