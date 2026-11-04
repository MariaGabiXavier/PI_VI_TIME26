\# FlowCampus - Visão Computacional



Este módulo utiliza visão computacional para detectar pessoas e acompanhar a ocupação dos ambientes do FlowCampus.



\## Arquivos



\- `detectar\_pessoas\_firebase.py`: detecta pessoas pela câmera e envia os dados ao Firebase.

\- `detectar\_pessoas.py`: versão de detecção sem integração com Firebase.

\- `teste\_camera.py`: teste de funcionamento da câmera.

\- `yolo11s-pose.pt`: modelo utilizado para detecção.

\- `requirements.txt`: bibliotecas necessárias.



\## Instalação



Com o Python instalado, execute na pasta principal do projeto:



```bash

python -m pip install -r VisaoComputacional/requirements.txt

```



\## Configuração do Firebase



É necessário possuir uma chave de conta de serviço do Firebase.



Coloque o arquivo `flowcampus-admin.json` dentro desta pasta ou configure a variável de ambiente `GOOGLE\_APPLICATION\_CREDENTIALS` apontando para sua chave.



\*\*Importante:\*\* nunca envie o arquivo de credenciais ao GitHub.



\## Execução



Para iniciar a detecção com Firebase:



```bash

python VisaoComputacional/detectar\_pessoas\_firebase.py

```



Para testar a câmera:



```bash

python VisaoComputacional/teste\_camera.py

```

