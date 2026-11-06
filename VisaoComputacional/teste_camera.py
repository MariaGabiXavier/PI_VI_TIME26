
import cv2

# Acessar a câmera padrão do notebook
#camera = cv2.VideoCapture(0)

# Acessar a câmera do celular
URL_CAMERA = "http://192.168.15.30:8080/video"
camera = cv2.VideoCapture(URL_CAMERA)

if not camera.isOpened():
    print("Erro: não foi possível acessar a câmera.")
    exit()

print("Câmera iniciada! Pressione Q para fechar.")

while True:
    sucesso, imagem = camera.read()

    if not sucesso:
        print("Erro ao capturar imagem.")
        break

    cv2.imshow("FlowCampus - Teste da Webcam", imagem)

    # Pressione Q para encerrar
    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

camera.release()
cv2.destroyAllWindows()
