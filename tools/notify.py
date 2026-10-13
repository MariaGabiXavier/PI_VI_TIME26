"""
Cria uma notificação no Firestore e envia o push para os celulares.

Quem decidir "tem que avisar" (regras de lotação, Visão Computacional, IA)
só precisa chamar create_notification(...). O app não precisa mudar.

Como usar:
    pip install firebase-admin
    coloque o arquivo serviceAccountKey.json nesta pasta (tools)
    python tools/notify.py

A chave de serviço (arquivo .json) é secreta: NUNCA coloque no GitHub.
"""

import os

import firebase_admin
from firebase_admin import credentials, firestore, messaging

# Tem que ser igual ao que está no app (NotificationsPush.kt)
TOPIC = "alerts"
CHANNEL_ID = "flowcampus_alerts"

# A chave de serviço fica ao lado deste arquivo, com este nome
# (o nome já está no .gitignore, então ela não vai pro GitHub)
KEY_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "serviceAccountKey.json")

if not firebase_admin._apps:
    if os.path.exists(KEY_PATH):
        firebase_admin.initialize_app(credentials.Certificate(KEY_PATH))
    else:
        # Se não achar o arquivo, usa a chave indicada em GOOGLE_APPLICATION_CREDENTIALS
        firebase_admin.initialize_app()

db = firestore.client()


def send_push_to_topic(title, body, environment_id="", notification_id=""):
    """Envia o push para todos os celulares inscritos no tópico."""
    message = messaging.Message(
        topic=TOPIC,
        notification=messaging.Notification(title=title, body=body),
        # data só aceita texto
        data={
            "environmentId": environment_id,
            "notificationId": notification_id,
        },
        android=messaging.AndroidConfig(
            priority="high",
            notification=messaging.AndroidNotification(channel_id=CHANNEL_ID),
        ),
    )
    return messaging.send(message)


def create_notification(
        title,
        description,
        category="ALERT",       # "ALERT" ou "UPDATE"
        icon="OCCUPANCY",       # OCCUPANCY, WAIT_TIME, QUIET_HOURS, EMPTY_ROOM, FORECAST
        tone="RED",             # RED, ORANGE, BLUE, GREEN, PURPLE
        environment_id="",      # id do documento em "environments", ex: "refeitorio"
        source="simulated",     # depois: "computer_vision" ou "ai"
        send_push=True,
):
    """Grava a notificação no Firestore (aparece no app) e envia o push."""
    _, doc_ref = db.collection("notifications").add(
        {
            "title": title,
            "description": description,
            "category": category,
            "icon": icon,
            "tone": tone,
            "environmentId": environment_id,
            "source": source,
            "createdAt": firestore.SERVER_TIMESTAMP,
        }
    )

    if send_push:
        send_push_to_topic(title, description, environment_id, doc_ref.id)

    return doc_ref.id


if __name__ == "__main__":
    # Teste rápido: cria uma notificação de exemplo e manda o push
    notification_id = create_notification(
        title="Refeitório Cheio",
        description="Capacidade máxima atingida! Considere ir em outro ambiente/horário.",
        category="ALERT",
        icon="OCCUPANCY",
        tone="RED",
        environment_id="refeitorio",
    )
    print("Notificação criada:", notification_id)