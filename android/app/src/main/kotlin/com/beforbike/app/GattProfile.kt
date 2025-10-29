package com.beforbike.app

import android.os.ParcelUuid
import java.util.UUID

object GattProfile {
    // UUID do Serviço principal
    val UUID_SERVICE_TRANSFER: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

    // Apenas UMA característica para receber o JSON com todos os dados
    val UUID_CHAR_DATA: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

    // UUID padrão para o Client Characteristic Configuration Descriptor (CCCD)
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ParcelUuid para uso em advertising (incluir o serviço no anúncio)
    val SERVICE_TRANSFER_PARCEL_UUID: ParcelUuid = ParcelUuid(UUID_SERVICE_TRANSFER)
}
