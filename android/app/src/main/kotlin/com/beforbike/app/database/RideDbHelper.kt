package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import android.database.Cursor

class RideDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // --- Schema Definition (2 Tables: Summary + Full Telemetry) ---
    companion object {
        // INCREMENTADO para 10 para forçar a atualização do schema
        const val DATABASE_VERSION = 10 //
        const val DATABASE_NAME = "BikeRides.db"
        private const val TAG = "RideDbHelper"

        // --- Tabela 1: Rides (Geral/Resumo) ---
        object RidesEntry : BaseColumns {
            const val TABLE_NAME = "Rides"
            const val COLUMN_RIDE_ID = "ride_id" // ID único (ex: 123)
            const val COLUMN_START_TIME = "start_time"
            const val COLUMN_END_TIME = "end_time"
            const val COLUMN_TOTAL_DISTANCE_KM = "total_distance_km"
            const val COLUMN_AVG_VELOCITY_KMH = "avg_velocity_kmh"
            const val COLUMN_AVG_POWER = "avg_power"
            const val COLUMN_AVG_CADENCE = "avg_cadence"
            const val COLUMN_CALORIES = "calories"
        }

        // --- Tabela 2: TelemetryData (Dados Brutos COMPLETOS) ---
        object TelemetryEntry : BaseColumns {
            const val TABLE_NAME = "TelemetryData"
            const val COLUMN_TELEMETRY_ID = "_id" // Chave primária interna
            const val COLUMN_RIDE_ID = "ride_id" // Chave estrangeira para Rides

            // Campos do PacketInfo (info)
            const val COLUMN_PACKET_DATE = "packet_date"
            const val COLUMN_PACKET_TIME = "packet_time"

            // Campos do GpsData (gps)
            const val COLUMN_GPS_TIMESTAMP = "gps_timestamp" //
            const val COLUMN_LATITUDE = "latitude" //
            const val COLUMN_LONGITUDE = "longitude" //
            const val COLUMN_ALTITUDE = "altitude" //
            const val COLUMN_GPS_SPEED = "gps_speed" //
            const val COLUMN_DIRECTION = "direction" //
            const val COLUMN_FIX_SATELLITES = "fix_satellites" //
            const val COLUMN_FIX_QUALITY = "fix_quality" //

            // Campos do CrankData (crank)
            const val COLUMN_POWER = "power" //
            const val COLUMN_CADENCE = "cadence" //
            const val COLUMN_JOULES = "joules" //
            const val COLUMN_CRANK_CALORIES = "crank_calories" //
            const val COLUMN_CRANK_SPEED_MS = "crank_speed_ms" //
            const val COLUMN_CRANK_SPEED = "crank_speed" //
            const val COLUMN_CRANK_DISTANCE = "crank_distance" //
        }

        // --- SQL Creation Commands ---
        private const val SQL_CREATE_RIDES =
            "CREATE TABLE ${RidesEntry.TABLE_NAME} (" +
                    "${RidesEntry.COLUMN_RIDE_ID} INTEGER PRIMARY KEY," +
                    "${RidesEntry.COLUMN_START_TIME} TEXT NOT NULL," +
                    "${RidesEntry.COLUMN_END_TIME} TEXT," +
                    "${RidesEntry.COLUMN_TOTAL_DISTANCE_KM} REAL," +
                    "${RidesEntry.COLUMN_AVG_VELOCITY_KMH} REAL," +
                    "${RidesEntry.COLUMN_AVG_POWER} REAL," +
                    "${RidesEntry.COLUMN_AVG_CADENCE} REAL," +
                    "${RidesEntry.COLUMN_CALORIES} REAL)"

        private const val SQL_CREATE_TELEMETRY_DATA =
            "CREATE TABLE ${TelemetryEntry.TABLE_NAME} (" +
                    "${TelemetryEntry.COLUMN_TELEMETRY_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${TelemetryEntry.COLUMN_RIDE_ID} INTEGER NOT NULL," +
                    // Campos do Info
                    "${TelemetryEntry.COLUMN_PACKET_DATE} TEXT," +
                    "${TelemetryEntry.COLUMN_PACKET_TIME} TEXT," +
                    // Campos do GpsData
                    "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} TEXT," +
                    "${TelemetryEntry.COLUMN_LATITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_LONGITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_ALTITUDE} REAL," +
                    "${TelemetryEntry.COLUMN_GPS_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_DIRECTION} REAL," +
                    "${TelemetryEntry.COLUMN_FIX_SATELLITES} INTEGER," +
                    "${TelemetryEntry.COLUMN_FIX_QUALITY} INTEGER," +
                    // Campos do CrankData
                    "${TelemetryEntry.COLUMN_POWER} REAL," +
                    "${TelemetryEntry.COLUMN_CADENCE} REAL," +
                    "${TelemetryEntry.COLUMN_JOULES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_CALORIES} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED_MS} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_SPEED} REAL," +
                    "${TelemetryEntry.COLUMN_CRANK_DISTANCE} REAL," +
                    "FOREIGN KEY (${TelemetryEntry.COLUMN_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${RidesEntry.COLUMN_RIDE_ID}) ON DELETE CASCADE)"

        // --- SQL Deletion Commands ---
        private const val SQL_DELETE_RIDES = "DROP TABLE IF EXISTS ${RidesEntry.TABLE_NAME}"
        private const val SQL_DELETE_TELEMETRY_DATA = "DROP TABLE IF EXISTS ${TelemetryEntry.TABLE_NAME}"
        // Nomes antigos
        private const val SQL_DELETE_OLD_POWER = "DROP TABLE IF EXISTS Power"
        private const val SQL_DELETE_OLD_MAPDATA = "DROP TABLE IF EXISTS Localization"
        private const val SQL_DELETE_OLD_VELOCITY = "DROP TABLE IF EXISTS Velocity"
        private const val SQL_DELETE_OLD_CADENCE = "DROP TABLE IF EXISTS Cadence"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Criando tabelas do banco de dados (v10)...")
        db.execSQL(SQL_CREATE_RIDES)
        db.execSQL(SQL_CREATE_TELEMETRY_DATA)
        Log.i(TAG, "Tabelas criadas com sucesso.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Atualizando banco de dados da v$oldVersion para v$newVersion. Dados antigos serão perdidos.")
        // Apaga TODAS as tabelas (novas e antigas)
        db.execSQL(SQL_DELETE_TELEMETRY_DATA) // Apaga TelemetryData (v9) ou TelemetryData (v10)
        db.execSQL(SQL_DELETE_OLD_POWER) //
        db.execSQL(SQL_DELETE_OLD_MAPDATA) //
        db.execSQL(SQL_DELETE_OLD_VELOCITY) //
        db.execSQL(SQL_DELETE_OLD_CADENCE) //
        db.execSQL(SQL_DELETE_RIDES) // Apaga Rides por último
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Fazendo downgrade do banco de dados da v$oldVersion para v$newVersion. Dados antigos serão perdidos.")
        onUpgrade(db, oldVersion, newVersion)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    // --- INSERT Functions ---

    /**
     * Garante que a "Corrida Geral" (Tabela 1) exista.
     */
    fun ensureRideExists(rideIdFromDevice: Long, startTime: String?): Boolean {
        if (rideIdFromDevice <= 0) {
            Log.e(TAG, "ensureRideExists: ID da corrida inválido ($rideIdFromDevice).")
            return false
        }
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(RidesEntry.COLUMN_RIDE_ID, rideIdFromDevice)
            put(RidesEntry.COLUMN_START_TIME, startTime ?: getCurrentTimestamp())
        }

        try {
            val result = db.insertWithOnConflict(RidesEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (result == -1L) {
                // Log.d(TAG, "ensureRideExists: Ride ID $rideIdFromDevice já existe.")
            } else {
                Log.i(TAG, "Nova entrada na tabela 'Rides' criada para ID: $rideIdFromDevice")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Erro CRÍTICO ao tentar criar entrada na tabela 'Rides' para ID $rideIdFromDevice: ${e.message}")
            return false
        }
    }


    /**
     * Insere um ponto de telemetria completo (Tabela 2).
     */
    fun insertTelemetryData(
        rideId: Long,
        infoMap: Map<String, Any?>,
        gpsMap: Map<String, Any?>,
        crankMap: Map<String, Any?>? // Pode ser nulo
    ): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put(TelemetryEntry.COLUMN_RIDE_ID, rideId)

            // Dados do Info
            put(TelemetryEntry.COLUMN_PACKET_DATE, infoMap["date"] as? String)
            put(TelemetryEntry.COLUMN_PACKET_TIME, infoMap["time"] as? String)

            // Dados do GpsData (gps)
            put(TelemetryEntry.COLUMN_GPS_TIMESTAMP, gpsMap["timestamp"] as? String) //
            put(TelemetryEntry.COLUMN_LATITUDE, gpsMap["latitude"] as? Double) //
            put(TelemetryEntry.COLUMN_LONGITUDE, gpsMap["longitude"] as? Double) //
            put(TelemetryEntry.COLUMN_ALTITUDE, gpsMap["altitude"] as? Double) //
            put(TelemetryEntry.COLUMN_GPS_SPEED, gpsMap["speed"] as? Double) //
            put(TelemetryEntry.COLUMN_DIRECTION, gpsMap["direction"] as? Double) //
            put(TelemetryEntry.COLUMN_FIX_SATELLITES, gpsMap["fix_satellites"] as? Int) //
            put(TelemetryEntry.COLUMN_FIX_QUALITY, gpsMap["fix_quality"] as? Int) //

            // Dados do CrankData (crank) (apenas se crankMap não for nulo)
            crankMap?.let {
                put(TelemetryEntry.COLUMN_POWER, it["power"] as? Double) //
                put(TelemetryEntry.COLUMN_CADENCE, it["cadence"] as? Double) //
                put(TelemetryEntry.COLUMN_JOULES, it["joules"] as? Double) //
                put(TelemetryEntry.COLUMN_CRANK_CALORIES, it["calories"] as? Double) //
                put(TelemetryEntry.COLUMN_CRANK_SPEED_MS, it["speed_ms"] as? Double) //
                put(TelemetryEntry.COLUMN_CRANK_SPEED, it["speed"] as? Double) //
                put(TelemetryEntry.COLUMN_CRANK_DISTANCE, it["distance"] as? Double) //
            }
        }

        val result = db.insert(TelemetryEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir TelemetryData para ride $rideId")
        return result != -1L
    }

    // --- Funções de CONSULTA E CÁLCULO ---

    /**
     * Retorna uma lista de IDs de corridas (Tabela 1).
     */
    fun getAllRideIds(): List<Long> {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(RidesEntry.COLUMN_RIDE_ID),
            null, null, null, null,
            "${RidesEntry.COLUMN_START_TIME} DESC"
        )
        val rideIds = mutableListOf<Long>()
        cursor.use {
            while (it.moveToNext()) {
                rideIds.add(it.getLong(0))
            }
        }
        return rideIds
    }

    /**
     * Retorna os dados de resumo de uma corrida (Tabela 1).
     * Se os totais não estiverem calculados (distância ou calorias zero), calcula e atualiza.
     */
    fun getRideSummary(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            null, // Pega todas as colunas
            "${RidesEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val map = mutableMapOf<String, Any?>()
                for (i in 0 until it.columnCount) {
                    // --- CORREÇÃO AQUI ---
                    when (it.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> map[it.getColumnName(i)] = null
                        Cursor.FIELD_TYPE_INTEGER -> map[it.getColumnName(i)] = it.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> map[it.getColumnName(i)] = it.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> map[it.getColumnName(i)] = it.getString(i)
                        Cursor.FIELD_TYPE_BLOB -> map[it.getColumnName(i)] = it.getBlob(i)
                        else -> map[it.getColumnName(i)] = null // Padrão
                    }
                    // --- FIM DA CORREÇÃO ---
                }

                // Verifica se os totais estão calculados
                val totalDistance = map[RidesEntry.COLUMN_TOTAL_DISTANCE_KM] as? Double ?: 0.0
                val totalCalories = map[RidesEntry.COLUMN_CALORIES] as? Double ?: 0.0
                if (totalDistance == 0.0 && totalCalories == 0.0) {
                    // Calcula estatísticas se não estiverem presentes
                    val stats = calculateRideStatistics(rideId)
                    if (stats != null) {
                        updateRideSummary(rideId, stats)
                        // Atualiza o map com os novos valores
                        map[RidesEntry.COLUMN_TOTAL_DISTANCE_KM] = stats["total_distance_km"]
                        map[RidesEntry.COLUMN_AVG_VELOCITY_KMH] = stats["avg_velocity_kmh"]
                        map[RidesEntry.COLUMN_AVG_POWER] = stats["avg_power"]
                        map[RidesEntry.COLUMN_AVG_CADENCE] = stats["avg_cadence"]
                        map[RidesEntry.COLUMN_CALORIES] = stats["calories"]
                        map[RidesEntry.COLUMN_END_TIME] = stats["end_time"]
                    }
                }

                return map
            }
        }
        return null
    }

    /**
     * Calcula as estatísticas de resumo (para Tabela 1)
     * lendo todos os pontos da Tabela 2.
     * * Esta é uma operação PESADA. Chame-a apenas no final da corrida.
     */
    fun calculateRideStatistics(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase

        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            arrayOf( // Apenas as colunas necessárias para o cálculo
                TelemetryEntry.COLUMN_GPS_TIMESTAMP, //
                TelemetryEntry.COLUMN_LATITUDE,
                TelemetryEntry.COLUMN_LONGITUDE,
                TelemetryEntry.COLUMN_GPS_SPEED, //
                TelemetryEntry.COLUMN_CRANK_SPEED, //
                TelemetryEntry.COLUMN_POWER,
                TelemetryEntry.COLUMN_CADENCE,
                TelemetryEntry.COLUMN_CRANK_CALORIES, // Adicionado para somar calorias
                TelemetryEntry.COLUMN_CRANK_DISTANCE // Adicionado para verificar distância cumulativa
            ),
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_GPS_TIMESTAMP} ASC" // Ordena por tempo
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            Log.w(TAG, "calculateRideStatistics: Nenhum dado de telemetria encontrado para ride $rideId")
            return null
        }

        var totalDistanceKm = 0.0
        val velocities = mutableListOf<Double>()
        val powers = mutableListOf<Double>()
        val cadences = mutableListOf<Double>()
        var totalCalories = 0.0

        var startTime: Long? = null
        var endTime: Long? = null
        var prevLat = 0.0
        var prevLon = 0.0
        var firstGpsPoint = true

        // Tenta usar o formato com milissegundos
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        cursor.use {
            do {
                // --- Tempo e Duração ---
                val timestampStr = it.getString(it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_TIMESTAMP))
                try {
                    val timestamp = dateFormat.parse(timestampStr)?.time
                    if (timestamp != null) {
                        if (startTime == null) startTime = timestamp
                        endTime = timestamp
                    }
                } catch (e: Exception) {
                    // Tenta formato sem milissegundos se o primeiro falhar
                    try {
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timestampStr)?.time
                        if (ts != null) {
                            if (startTime == null) startTime = ts
                            endTime = ts
                        }
                    } catch (e2: Exception) {
                        Log.w(TAG, "Formato de timestamp inválido: $timestampStr")
                    }
                }

                // --- GPS e Distância ---
                val latIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_LATITUDE)
                val lonIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_LONGITUDE)
                if (!it.isNull(latIdx) && !it.isNull(lonIdx)) {
                    val lat = it.getDouble(latIdx)
                    val lon = it.getDouble(lonIdx)

                    if (firstGpsPoint) {
                        prevLat = lat
                        prevLon = lon
                        firstGpsPoint = false
                    } else {
                        // Cálculo de Haversine
                        val dLat = Math.toRadians(lat - prevLat)
                        val dLon = Math.toRadians(lon - prevLon)
                        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                Math.cos(Math.toRadians(prevLat)) * Math.cos(Math.toRadians(lat)) *
                                Math.sin(dLon / 2) * Math.sin(dLon / 2)
                        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                        totalDistanceKm += (6371 * c) // Raio da Terra em km
                        prevLat = lat
                        prevLon = lon
                    }
                }

                // --- Velocidade (Usa GPS_SPEED como primário) ---
                val velIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_GPS_SPEED)
                val crankVelIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_SPEED)

                if (!it.isNull(velIdx)) {
                    val velMetersPerSec = it.getDouble(velIdx)
                    velocities.add(velMetersPerSec * 3.6) // Converte m/s para km/h
                } else if (!it.isNull(crankVelIdx)) {
                    // Fallback para a velocidade do crank (assumindo km/h)
                    velocities.add(it.getDouble(crankVelIdx))
                }

                // --- Potência ---
                val powerIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_POWER)
                if (!it.isNull(powerIdx)) {
                    powers.add(it.getDouble(powerIdx))
                }

                // --- Cadência ---
                val cadIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CADENCE)
                if (!it.isNull(cadIdx)) {
                    cadences.add(it.getDouble(cadIdx))
                }

                // --- Calorias cumulativas ---
                val calIdx = it.getColumnIndexOrThrow(TelemetryEntry.COLUMN_CRANK_CALORIES)
                if (!it.isNull(calIdx)) {
                    totalCalories = it.getDouble(calIdx) // Último valor é o total
                }

            } while (it.moveToNext())
        }

        // --- Calcula Médias e Totais ---
        val durationSec = if (startTime != null && endTime != null) (endTime!! - startTime!!) / 1000.0 else 0.0
        val durationHours = durationSec / 3600.0

        val avgVelocityKmh = if (velocities.isNotEmpty()) velocities.average() else 0.0
        val avgPower = if (powers.isNotEmpty()) powers.average() else 0.0
        val avgCadence = if (cadences.isNotEmpty()) cadences.average() else 0.0

        // If calories not received via BLE (cumulative == 0), calculate from power
        val calculatedCalories = if (totalCalories == 0.0) avgPower * durationHours * 3.6 else totalCalories

        val result = mapOf(
            "ride_id" to rideId,
            "start_time" to (startTime?.let { dateFormat.format(Date(it)) } ?: getCurrentTimestamp()),
            "end_time" to (endTime?.let { dateFormat.format(Date(it)) } ?: getCurrentTimestamp()),
            "total_distance_km" to totalDistanceKm,
            "avg_velocity_kmh" to avgVelocityKmh,
            "avg_power" to avgPower,
            "avg_cadence" to avgCadence,
            "calories" to calculatedCalories
        )

        Log.d(TAG, "Estatísticas calculadas para $rideId: $result")
        return result
    }

    /**
     * Atualiza a Tabela 1 (Rides) com os dados de resumo calculados.
     */
    fun updateRideSummary(rideId: Long, stats: Map<String, Any?>): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            stats["end_time"]?.let { put(RidesEntry.COLUMN_END_TIME, it as String) }
            stats["total_distance_km"]?.let { put(RidesEntry.COLUMN_TOTAL_DISTANCE_KM, it as Double) }
            stats["avg_velocity_kmh"]?.let { put(RidesEntry.COLUMN_AVG_VELOCITY_KMH, it as Double) }
            stats["avg_power"]?.let { put(RidesEntry.COLUMN_AVG_POWER, it as Double) }
            stats["avg_cadence"]?.let { put(RidesEntry.COLUMN_AVG_CADENCE, it as Double) }
            stats["calories"]?.let { put(RidesEntry.COLUMN_CALORIES, it as Double) }
        }

        if (values.size() == 0) {
            Log.w(TAG, "updateRideSummary: Nenhum dado válido para atualizar no ride $rideId")
            return false
        }

        val result = db.update(
            RidesEntry.TABLE_NAME,
            values,
            "${RidesEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString())
        )

        Log.i(TAG, "Tabela 'Rides' (ID: $rideId) atualizada com estatísticas. Linhas afetadas: $result")
        return result > 0
    }

    /**
     * Retorna todos os dados de telemetria brutos de uma corrida (Tabela 2).
     */
    fun getRideTelemetryData(rideId: Long): List<Map<String, Any?>> {
        val db = this.readableDatabase
        val cursor = db.query(
            TelemetryEntry.TABLE_NAME,
            null, // Pega todas as colunas
            "${TelemetryEntry.COLUMN_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null,
            "${TelemetryEntry.COLUMN_TELEMETRY_ID} ASC"
        )

        val results = mutableListOf<Map<String, Any?>>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val map = mutableMapOf<String, Any?>()
                    for (i in 0 until it.columnCount) {
                        // --- CORREÇÃO AQUI ---
                        when (it.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> map[it.getColumnName(i)] = null
                            Cursor.FIELD_TYPE_INTEGER -> map[it.getColumnName(i)] = it.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> map[it.getColumnName(i)] = it.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> map[it.getColumnName(i)] = it.getString(i)
                            Cursor.FIELD_TYPE_BLOB -> map[it.getColumnName(i)] = it.getBlob(i)
                            else -> map[it.getColumnName(i)] = null // Padrão
                        }
                        // --- FIM DA CORREÇÃO ---
                    }
                    results.add(map)
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun deleteRide(rideId: Long) {
        val db = writableDatabase
        db.delete(RidesEntry.TABLE_NAME, "${RidesEntry.COLUMN_RIDE_ID} = ?", arrayOf(rideId.toString()))
        // TelemetryData will be deleted automatically due to ON DELETE CASCADE
    }
}