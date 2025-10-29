package com.beforbike.app

import android.os.ParcelUuid
import java.util.UUID

object GattProfile {
    // Main service UUID
    val UUID_SERVICE_TRANSFER: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

    // Only ONE characteristic to receive JSON with all data
    val UUID_CHAR_DATA: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

    // Standard UUID for Client Characteristic Configuration Descriptor (CCCD)
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // ParcelUuid for advertising use (include service in advertisement)
    val SERVICE_TRANSFER_PARCEL_UUID: ParcelUuid = ParcelUuid(UUID_SERVICE_TRANSFER)
}
