#!/usr/bin/env python3
"""
Script simplificado para envio de dados de atividade via BLE para o app Android.

Este script lê dados de um arquivo JSONL, comprime-os e os envia via BLE.
"""

import asyncio
import json
import gzip
from typing import List, Dict, Any, Optional
''
try:
    from bleak import BleakClient, BleakScanner
except ImportError:
    print("Erro: bleak não está instalado. Instale com: pip install bleak")
    exit(1)


class BleRideSender:
    """Cliente BLE para enviar dados de atividade."""

    # UUIDs do serviço BLE (do GattProfile.kt)
    SERVICE_UUID = "12345678-1234-5678-1234-56789abcdef0"
    CHARACTERISTIC_UUID = "12345678-1234-5678-1234-56789abcdef0"

    def __init__(self, compressed: bool = False):
        self.compressed = compressed
        self.client: Optional[BleakClient] = None

    async def find_android_device(self) -> Optional[str]:
        """Procura por dispositivos Android com o serviço BLE."""
        print("Procurando dispositivos BLE...")

        devices = await BleakScanner.discover(timeout=10.0)

        print(f"Dispositivos encontrados: {len(devices)}")
        for device in devices:
            print(f"  - Nome: '{device.name}', Endereço: {device.address}")
            if hasattr(device, 'metadata') and device.metadata:
                print(f"    UUIDs: {device.metadata.get('uuids', [])}")

        for device in devices:
            if device.name and ("android" in device.name.lower() or "be for bike" in device.name.lower()):
                print(f"Encontrado dispositivo Android: {device.name} ({device.address})")
                return device.address

            # Também verifica pelos serviços anunciados
            if hasattr(device, 'metadata') and device.metadata:
                service_uuids = device.metadata.get('uuids', [])
                if self.SERVICE_UUID in service_uuids:
                    print(f"Encontrado dispositivo com serviço BLE: {device.name or 'Desconhecido'} ({device.address})")
                    return device.address

        print("Nenhum dispositivo Android encontrado")
        return None

    async def connect(self, device_address: str) -> bool:
        """Conecta ao dispositivo BLE."""
        try:
            print(f"Conectando ao dispositivo {device_address}...")
            self.client = BleakClient(device_address)
            await self.client.connect()
            print("Conectado com sucesso!")
            return True
        except Exception as e:
            print(f"Erro ao conectar: {e}")
            return False

    async def send_data(self, data_points: List[Dict[str, Any]]) -> bool:
        """Envia os dados via BLE."""
        if not self.client or not self.client.is_connected:
            print("Cliente BLE não conectado")
            return False

        try:
            # Converte dados para JSONL
            jsonl_data = "\n".join(json.dumps(point) for point in data_points)
            print(f"Dados JSONL gerados: {len(jsonl_data)} caracteres")

            # Comprime se solicitado
            if self.compressed:
                compressed_data = gzip.compress(jsonl_data.encode('utf-8'))
                print(f"Dados comprimidos: {len(compressed_data)} bytes")
                data_to_send = compressed_data
            else:
                data_to_send = jsonl_data.encode('utf-8')

            # Divide em chunks se necessário (BLE tem limite de MTU)
            chunk_size = 512  # Tamanho seguro para BLE
            chunks = [data_to_send[i:i + chunk_size] for i in range(0, len(data_to_send), chunk_size)]

            print(f"Enviando {len(chunks)} chunk(s) de dados...")

            for i, chunk in enumerate(chunks):
                print(f"Enviando chunk {i + 1}/{len(chunks)} ({len(chunk)} bytes)...")

                # Envia chunk via BLE
                await self.client.write_gatt_char(
                    self.CHARACTERISTIC_UUID,
                    chunk,
                    response=True
                )

                # Pequena pausa entre chunks
                await asyncio.sleep(0.1)

            print("Todos os dados enviados com sucesso!")
            return True

        except Exception as e:
            print(f"Erro ao enviar dados: {e}")
            return False

    async def disconnect(self):
        """Desconecta do dispositivo BLE."""
        if self.client and self.client.is_connected:
            await self.client.disconnect()
            print("Desconectado")


def get_user_device_address():
    """Obtém endereço MAC do usuário se ele souber."""
    print("\nNenhum dispositivo Android encontrado automaticamente.")
    print("Se souber o endereço MAC do seu telefone Android, digite-o (ex: AA:BB:CC:DD:EE:FF).")
    print("Caso contrário, pressione Enter para sair: ")
    address = input("Endereço MAC: ").strip().upper()
    if address and len(address.split(':')) == 6:
        return address
    return None

async def main():
    """Função principal."""
    print("Iniciando envio de dados de atividade via BLE...")

    # Lê dados do arquivo JSONL
    jsonl_file = "test_data.jsonl"
    try:
        with open(jsonl_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        data_points = [json.loads(line.strip()) for line in lines if line.strip()]
        print(f"Dados lidos de {jsonl_file}: {len(data_points)} pontos")
        if data_points:
            ride_id = data_points[0].get("ride_id", "desconhecido")
            print(f"ID da atividade: {ride_id}")
    except FileNotFoundError:
        print(f"Erro: Arquivo {jsonl_file} não encontrado.")
        return
    except json.JSONDecodeError as e:
        print(f"Erro ao ler JSONL: {e}")
        return

    # Inicializa sender BLE com compressão
    sender = BleRideSender(compressed=True)

    try:
        # Encontra dispositivo automaticamente
        device_address = await sender.find_android_device()

        # Se não encontrou, permite entrada manual
        if not device_address:
            device_address = get_user_device_address()
            if not device_address:
                return

        # Conecta
        if not await sender.connect(device_address):
            return

        # Envia dados
        success = await sender.send_data(data_points)

        if success:
            print("\n✅ Dados enviados com sucesso!")
            print(f"📍 Pontos: {len(data_points)}")
        else:
            print("\n❌ Falha ao enviar dados")

    finally:
        await sender.disconnect()


if __name__ == "__main__":
    asyncio.run(main())
