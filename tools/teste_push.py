import firebase_admin
from firebase_admin import credentials, messaging

firebase_admin.initialize_app(credentials.Certificate("serviceAccountKey.json"))

resposta = messaging.send(messaging.Message(
    topic="alerts",
    notification=messaging.Notification(
        title="Teste de push",
        body="Se você está lendo isso, o push funciona!",
    ),
    android=messaging.AndroidConfig(
        priority="high",
        notification=messaging.AndroidNotification(channel_id="flowcampus_alerts"),
    ),
))
print("Enviado:", resposta)