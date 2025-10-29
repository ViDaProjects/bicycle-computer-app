# BLE Test - Envio de Dados via BLE

Este script simplificado lê dados de atividade de ciclismo de um arquivo JSONL, comprime-os e envia via Bluetooth Low Energy (BLE) para o app Android "Be For Bike".

## Funcionalidades

- ✅ **Leitura de Arquivo**: Lê dados de `test_data.jsonl`
- ✅ **Compressão GZIP**: Sempre ativa para otimização
- ✅ **Detecção Automática**: Encontra dispositivos Android via BLE
- ✅ **Dados Fragmentados**: Suporte a chunks para limites BLE

## Pré-requisitos

### Python
- Python 3.7 ou superior

### Dependências
Instale as dependências com:
```bash
pip install -r requirements.txt
```

### Arquivo de Dados
- Arquivo `test_data.jsonl` na mesma pasta com dados de atividade

### Android App
- O app Android deve estar rodando e com o serviço BLE ativo
- O dispositivo Android deve ter Bluetooth habilitado
- O app deve estar anunciando o serviço BLE

## Como Usar

### Execução Simples
```bash
python ble_test.py
```

## Formato dos Dados

Os dados são enviados em **JSONL** (JSON Lines):
```json
{"ride_id": "99", "timestamp": "2024-01-01T12:00:00.000000", "latitude": -23.550520, "longitude": -46.633308, "altitude": 760.0, "velocity": 0.0, "cadence": 0.0, "power": 0.0}
{"ride_id": "99", "timestamp": "2024-01-01T12:00:01.000000", "latitude": -23.550529, "longitude": -46.633317, "altitude": 760.5, "velocity": 25.3, "cadence": 92.1, "power": 185.7}
```

### Campos dos Dados

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `ride_id` | string | UUID único da atividade |
| `timestamp` | string | Timestamp ISO 8601 |
| `latitude` | float | Latitude GPS |
| `longitude` | float | Longitude GPS |
| `altitude` | float | Altitude em metros |
| `velocity` | float | Velocidade em km/h |
| `cadence` | float | Cadência em RPM |
| `power` | float | Potência em watts |

## Detecção de Dispositivos

O script automaticamente detecta dispositivos Android por:

1. Nome do dispositivo contendo "android" ou "be for bike"
2. Serviços BLE anunciados (UUID específico do app)

## Compressão

Os dados são sempre comprimidos com GZIP para otimização:
- Redução típica de 70-80% no tamanho
- O app Android deve descomprimir automaticamente

## Limitações BLE

- Dados são divididos em chunks de 512 bytes
- Pausa de 100ms entre chunks para estabilidade
- MTU BLE limita o tamanho dos pacotes
