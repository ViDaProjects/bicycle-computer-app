package com.beforbike.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class RideDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // --- Definição do Schema (baseado em cria_banco.sql) ---
    companion object {
        const val DATABASE_VERSION = 2 // Incremente se mudar o schema
        const val DATABASE_NAME = "Rides.db"
        private const val TAG = "RideDbHelper"

        // Constants for easy access from other files
        const val TABLE_RIDES = "rides"
        const val TABLE_POWER = "Power"
        const val TABLE_MAPDATA = "MapDATA"
        const val TABLE_VELOCITY = "Velocity"
        const val TABLE_CADENCE = "Cadence"

        const val COLUMN_RIDES_RIDE_NAME = "Ride_name"
        const val COLUMN_START_TIME = "StartTime"
        const val COLUMN_END_TIME = "EndTime"
        const val COLUMN_DURATION = "Duration"
        const val COLUMN_MAX_VELOCITY = "MaxVelocity"
        const val COLUMN_MEAN_VELOCITY = "MeanVelocity"
        const val COLUMN_CALORIES = "Calories"
        const val COLUMN_DISTANCE = "Distance"
        const val COLUMN_MAPDATA_ID = "MapDataID"
        const val COLUMN_VELOCITY_ID = "VelocityID"

        const val COLUMN_RIDE_ID = "ride_id"
        const val COLUMN_POWER = "power"
        const val COLUMN_TIMESTAMP = "timestamp_ocorrencia"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_ALTITUDE = "altitude"
        const val COLUMN_VELOCITY = "velocity"
        const val COLUMN_CADENCE = "cadence"

        object RidesEntry : BaseColumns {
            const val TABLE_NAME = "rides"
            const val COLUMN_NAME_RIDE_NAME = "Ride_name" // DEVE SER ÚNICO
            const val COLUMN_NAME_START_TIME = "StartTime"
            const val COLUMN_NAME_END_TIME = "EndTime"
            // Outras colunas de resumo
            const val COLUMN_NAME_DURATION = "Duration"
            const val COLUMN_NAME_MAX_VELOCITY = "MaxVelocity"
            const val COLUMN_NAME_MEAN_VELOCITY = "MeanVelocity"
            const val COLUMN_NAME_CALORIES = "Calories"
            const val COLUMN_NAME_DISTANCE = "Distance"
            const val COLUMN_NAME_MAPDATA_ID = "MapDataID" // Provavelmente não usado
            const val COLUMN_NAME_VELOCITY_ID = "VelocityID" // Provavelmente não usado
        }

        object PowerEntry : BaseColumns {
            const val TABLE_NAME = "Power"
            const val COLUMN_NAME_RIDE_ID = "ride_id" // Chave estrangeira para rides._ID
            const val COLUMN_NAME_POWER = "power"
            const val COLUMN_NAME_TIMESTAMP = "timestamp_ocorrencia"
        }

        object MapDataEntry : BaseColumns {
            const val TABLE_NAME = "MapDATA"
            const val COLUMN_NAME_RIDE_ID = "ride_id" // Chave estrangeira para rides._ID
            const val COLUMN_NAME_LATITUDE = "latitude"
            const val COLUMN_NAME_LONGITUDE = "longitude"
            const val COLUMN_NAME_ALTITUDE = "altitude"
            const val COLUMN_NAME_TIMESTAMP = "timestamp_ocorrencia"
        }

        object VelocityEntry : BaseColumns {
            const val TABLE_NAME = "Velocity"
            const val COLUMN_NAME_RIDE_ID = "ride_id" // Chave estrangeira para rides._ID
            const val COLUMN_NAME_VELOCITY = "velocity"
            const val COLUMN_NAME_TIMESTAMP = "timestamp_ocorrencia"
        }

        object CadenceEntry : BaseColumns {
            const val TABLE_NAME = "Cadence"
            const val COLUMN_NAME_RIDE_ID = "ride_id" // Chave estrangeira para rides._ID
            const val COLUMN_NAME_CADENCE = "cadence"
            const val COLUMN_NAME_TIMESTAMP = "timestamp_ocorrencia"
        }

        // --- Comandos SQL de Criação ---
        private const val SQL_CREATE_RIDES =
            "CREATE TABLE ${RidesEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY," + // Usa o ID vindo do JSON
                    "${RidesEntry.COLUMN_NAME_RIDE_NAME} TEXT UNIQUE NOT NULL," +
                    "${RidesEntry.COLUMN_NAME_START_TIME} TEXT," +
                    "${RidesEntry.COLUMN_NAME_END_TIME} TEXT," +
                    "${RidesEntry.COLUMN_NAME_DURATION} REAL, " + // Use REAL para floats
                    "${RidesEntry.COLUMN_NAME_MAX_VELOCITY} REAL, " +
                    "${RidesEntry.COLUMN_NAME_MEAN_VELOCITY} REAL, " +
                    "${RidesEntry.COLUMN_NAME_CALORIES} REAL, " +
                    "${RidesEntry.COLUMN_NAME_DISTANCE} REAL, " +
                    "${RidesEntry.COLUMN_NAME_MAPDATA_ID} INTEGER, " +
                    "${RidesEntry.COLUMN_NAME_VELOCITY_ID} INTEGER)"

        private const val SQL_CREATE_POWER =
            "CREATE TABLE ${PowerEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," + // ID interno da tabela Power
                    "${PowerEntry.COLUMN_NAME_RIDE_ID} INTEGER NOT NULL," +
                    "${PowerEntry.COLUMN_NAME_POWER} REAL," + // Use REAL
                    "${PowerEntry.COLUMN_NAME_TIMESTAMP} TEXT NOT NULL," +
                    "FOREIGN KEY (${PowerEntry.COLUMN_NAME_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE)" // ON DELETE CASCADE apaga dados relacionados se a ride for deletada

        private const val SQL_CREATE_MAPDATA =
            "CREATE TABLE ${MapDataEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${MapDataEntry.COLUMN_NAME_RIDE_ID} INTEGER NOT NULL," +
                    "${MapDataEntry.COLUMN_NAME_LATITUDE} REAL NOT NULL," +
                    "${MapDataEntry.COLUMN_NAME_LONGITUDE} REAL NOT NULL," +
                    "${MapDataEntry.COLUMN_NAME_ALTITUDE} REAL," +
                    "${MapDataEntry.COLUMN_NAME_TIMESTAMP} TEXT NOT NULL," +
                    "FOREIGN KEY (${MapDataEntry.COLUMN_NAME_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE)"

        private const val SQL_CREATE_VELOCITY =
            "CREATE TABLE ${VelocityEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${VelocityEntry.COLUMN_NAME_RIDE_ID} INTEGER NOT NULL," +
                    "${VelocityEntry.COLUMN_NAME_VELOCITY} REAL NOT NULL," +
                    "${VelocityEntry.COLUMN_NAME_TIMESTAMP} TEXT NOT NULL," +
                    "FOREIGN KEY (${VelocityEntry.COLUMN_NAME_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE)"

        private const val SQL_CREATE_CADENCE =
            "CREATE TABLE ${CadenceEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${CadenceEntry.COLUMN_NAME_RIDE_ID} INTEGER NOT NULL," +
                    "${CadenceEntry.COLUMN_NAME_CADENCE} REAL NOT NULL," +
                    "${CadenceEntry.COLUMN_NAME_TIMESTAMP} TEXT NOT NULL," +
                    "FOREIGN KEY (${CadenceEntry.COLUMN_NAME_RIDE_ID}) REFERENCES ${RidesEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE)"

        // --- Comandos SQL de Exclusão ---
        private const val SQL_DELETE_RIDES = "DROP TABLE IF EXISTS ${RidesEntry.TABLE_NAME}"
        private const val SQL_DELETE_POWER = "DROP TABLE IF EXISTS ${PowerEntry.TABLE_NAME}"
        private const val SQL_DELETE_MAPDATA = "DROP TABLE IF EXISTS ${MapDataEntry.TABLE_NAME}"
        private const val SQL_DELETE_VELOCITY = "DROP TABLE IF EXISTS ${VelocityEntry.TABLE_NAME}"
        private const val SQL_DELETE_CADENCE = "DROP TABLE IF EXISTS ${CadenceEntry.TABLE_NAME}"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true) // Ativa chaves estrangeiras
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Cria as tabelas APENAS na primeira vez que o DB é acessado
        Log.i(TAG, "Criando tabelas do banco de dados (onCreate)...")
        db.execSQL(SQL_CREATE_RIDES)
        db.execSQL(SQL_CREATE_POWER)
        db.execSQL(SQL_CREATE_MAPDATA)
        db.execSQL(SQL_CREATE_VELOCITY)
        db.execSQL(SQL_CREATE_CADENCE)
        Log.i(TAG, "Tabelas criadas com sucesso.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Atualizando banco de dados da v$oldVersion para v$newVersion. Dados antigos serão perdidos.")
        // Política simples: apaga tudo e recria. Para produção, use ALTER TABLE.
        db.execSQL(SQL_DELETE_CADENCE)
        db.execSQL(SQL_DELETE_POWER)
        db.execSQL(SQL_DELETE_MAPDATA)
        db.execSQL(SQL_DELETE_VELOCITY)
        db.execSQL(SQL_DELETE_RIDES) // Apaga rides por último por causa das FKs
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(TAG, "Fazendo downgrade do banco de dados da v$oldVersion para v$newVersion. Dados antigos serão perdidos.")
        onUpgrade(db, oldVersion, newVersion) // Usa a mesma lógica destrutiva
    }

    // --- Funções de Ajuda (Helpers) ---

    private fun getCurrentTimestamp(): String {
        // Formato consistente com o SQL original
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    // --- Funções de INSERÇÃO ---

    /**
     * Verifica se uma corrida com o ID fornecido existe. Se não, cria uma nova.
     * Retorna true se a corrida existe ou foi criada com sucesso, false caso contrário.
     * ESSENCIAL para garantir a integridade da chave estrangeira.
     */
    fun ensureRideExists(rideIdFromDevice: Long, defaultNameBase: String = "Corrida Recebida"): Boolean {
        if (rideIdFromDevice <= 0) {
            Log.e(TAG, "ensureRideExists: ID da corrida inválido ($rideIdFromDevice).")
            return false
        }
        val db = this.readableDatabase // Usa readable para checar
        var exists = false

        // Verifica se o ID já existe
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID), // Coluna que queremos verificar
            "${BaseColumns._ID} = ?", // Clausula WHERE
            arrayOf(rideIdFromDevice.toString()), // Argumento
            null, null, null, "1" // Limit 1
        )
        cursor.use { // Garante que o cursor seja fechado
            if (it.moveToFirst()) {
                exists = true
            }
        }

        if (exists) {
            // Log.d(TAG, "ensureRideExists: Ride ID $rideIdFromDevice já existe.")
            return true // Corrida já existe
        } else {
            // Corrida não existe, TENTA criar
            Log.i(TAG, "ensureRideExists: Ride ID $rideIdFromDevice não encontrado. Tentando criar...")
            val writableDb = this.writableDatabase
            val startTime = getCurrentTimestamp() // Usa o momento atual como start time
            val rideName = "$defaultNameBase $rideIdFromDevice @ $startTime" // Nome único

            val values = ContentValues().apply {
                put(BaseColumns._ID, rideIdFromDevice) // USA O ID VINDO DO JSON
                put(RidesEntry.COLUMN_NAME_RIDE_NAME, rideName)
                put(RidesEntry.COLUMN_NAME_START_TIME, startTime)
                putNull(RidesEntry.COLUMN_NAME_END_TIME)
                // Inicializa outros campos
                put(RidesEntry.COLUMN_NAME_DURATION, 0.0)
                put(RidesEntry.COLUMN_NAME_DISTANCE, 0.0)
                // Inicializa outros resumos como nulos ou 0
                putNull(RidesEntry.COLUMN_NAME_MAX_VELOCITY)
                putNull(RidesEntry.COLUMN_NAME_MEAN_VELOCITY)
                putNull(RidesEntry.COLUMN_NAME_CALORIES)
                putNull(RidesEntry.COLUMN_NAME_MAPDATA_ID)
                putNull(RidesEntry.COLUMN_NAME_VELOCITY_ID)
            }
            try {
                // Usa insert com CONFLICT_IGNORE. Se já existir (criado por outra thread), não faz nada.
                val result = writableDb.insertWithOnConflict(RidesEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if (result != -1L) {
                    Log.i(TAG, "Nova entrada na tabela 'rides' criada para ID: $rideIdFromDevice")
                    return true // Criado com sucesso
                } else {
                    // Se deu conflito IGNORE, significa que já existe.
                    Log.w(TAG, "ensureRideExists: Conflito IGNORE ao inserir ID $rideIdFromDevice. Assumindo que já existe.")
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro CRÍTICO ao tentar criar entrada na tabela 'rides' para ID $rideIdFromDevice: ${e.message}")
                return false // Falha ao criar
            }
        }
    }


    /** Insere uma nova leitura de potência. Retorna true se sucesso. */
    fun insertPower(rideId: Long, power: Float, timestamp: Long? = null): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase
        val timestampStr = timestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(it)) } ?: getCurrentTimestamp()
        val values = ContentValues().apply {
            put(PowerEntry.COLUMN_NAME_RIDE_ID, rideId)
            put(PowerEntry.COLUMN_NAME_POWER, power)
            put(PowerEntry.COLUMN_NAME_TIMESTAMP, timestampStr)
        }
        val result = db.insert(PowerEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir Power para ride $rideId")
        return result != -1L
    }

    /** Insere uma nova leitura de velocidade. Retorna true se sucesso. */
    fun insertVelocity(rideId: Long, velocity: Float): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(VelocityEntry.COLUMN_NAME_RIDE_ID, rideId)
            put(VelocityEntry.COLUMN_NAME_VELOCITY, velocity)
            put(VelocityEntry.COLUMN_NAME_TIMESTAMP, getCurrentTimestamp())
        }
        val result = db.insert(VelocityEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir Velocity para ride $rideId (FK existe?)")
        return result != -1L
    }

    /** Insere uma nova leitura de mapa. Retorna true se sucesso. */
    fun insertMapData(rideId: Long, lat: Float, lon: Float, alt: Float?, timestamp: Long? = null): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase
        val timestampStr = timestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(it)) } ?: getCurrentTimestamp()
        val values = ContentValues().apply {
            put(MapDataEntry.COLUMN_NAME_RIDE_ID, rideId)
            put(MapDataEntry.COLUMN_NAME_LATITUDE, lat)
            put(MapDataEntry.COLUMN_NAME_LONGITUDE, lon)
            if (alt != null && alt.isFinite()) {
                put(MapDataEntry.COLUMN_NAME_ALTITUDE, alt)
            } else {
                putNull(MapDataEntry.COLUMN_NAME_ALTITUDE)
            }
            put(MapDataEntry.COLUMN_NAME_TIMESTAMP, timestampStr)
        }
        val result = db.insert(MapDataEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir MapData para ride $rideId")
        return result != -1L
    }

    /** Insere uma nova leitura de velocidade. Retorna true se sucesso. */
    fun insertVelocity(rideId: Long, velocity: Float, timestamp: Long? = null): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase
        val timestampStr = timestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(it)) } ?: getCurrentTimestamp()
        val values = ContentValues().apply {
            put(VelocityEntry.COLUMN_NAME_RIDE_ID, rideId)
            put(VelocityEntry.COLUMN_NAME_VELOCITY, velocity)
            put(VelocityEntry.COLUMN_NAME_TIMESTAMP, timestampStr)
        }
        val result = db.insert(VelocityEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir Velocity para ride $rideId")
        return result != -1L
    }

    /** Insere uma nova leitura de cadência. Retorna true se sucesso. */
    fun insertCadence(rideId: Long, cadence: Float, timestamp: Long? = null): Boolean {
        if (rideId <= 0) return false
        val db = this.writableDatabase
        val timestampStr = timestamp?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(it)) } ?: getCurrentTimestamp()
        val values = ContentValues().apply {
            put(CadenceEntry.COLUMN_NAME_RIDE_ID, rideId)
            put(CadenceEntry.COLUMN_NAME_CADENCE, cadence)
            put(CadenceEntry.COLUMN_NAME_TIMESTAMP, timestampStr)
        }
        val result = db.insert(CadenceEntry.TABLE_NAME, null, values)
        if (result == -1L) Log.w(TAG, "Falha ao inserir Cadence para ride $rideId")
        return result != -1L
    }

    // --- Funções de CONSULTA ---

    /**
     * Retorna uma lista de IDs de corridas existentes.
     */
    fun getAllRideIds(): List<Long> {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID),
            null, null, null, null, null
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
     * Retorna os dados de uma corrida específica.
     */
    fun getRideData(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase
        val cursor = db.query(
            RidesEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID, RidesEntry.COLUMN_NAME_RIDE_NAME,
                    RidesEntry.COLUMN_NAME_START_TIME, RidesEntry.COLUMN_NAME_END_TIME,
                    RidesEntry.COLUMN_NAME_DURATION, RidesEntry.COLUMN_NAME_MAX_VELOCITY,
                    RidesEntry.COLUMN_NAME_MEAN_VELOCITY, RidesEntry.COLUMN_NAME_CALORIES,
                    RidesEntry.COLUMN_NAME_DISTANCE),
            "${BaseColumns._ID} = ?",
            arrayOf(rideId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return mapOf(
                    "id" to it.getLong(0),
                    "name" to it.getString(1),
                    "startTime" to it.getString(2),
                    "endTime" to it.getString(3),
                    "duration" to it.getDouble(4),
                    "maxVelocity" to it.getDouble(5),
                    "meanVelocity" to it.getDouble(6),
                    "calories" to it.getDouble(7),
                    "distance" to it.getDouble(8)
                )
            }
        }
        return null
    }

    /**
     * Calcula estatísticas de uma corrida baseada nos dados armazenados nas tabelas.
     */
    fun calculateRideStatistics(rideId: Long): Map<String, Any?>? {
        val db = this.readableDatabase
        var duration = 0.0
        var distance = 0.0
        var maxVelocity = 0.0
        var meanVelocity = 0.0
        var calories = 0.0
        var startTime: Long? = null
        var endTime: Long? = null

        Log.d(TAG, "Calculating statistics for ride $rideId")

        // Calcular velocidade máxima e média
        val velocityCursor = db.query(
            VelocityEntry.TABLE_NAME,
            arrayOf(VelocityEntry.COLUMN_NAME_VELOCITY),
            "${VelocityEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, null
        )
        val velocities = mutableListOf<Double>()
        velocityCursor.use {
            while (it.moveToNext()) {
                val vel = it.getDouble(0)
                velocities.add(vel)
                if (vel > maxVelocity) maxVelocity = vel
            }
        }
        if (velocities.isNotEmpty()) {
            meanVelocity = velocities.average()
        }

        // Calcular duração baseada no primeiro e último timestamp dos dados GPS
        val mapDataCursor = db.query(
            MapDataEntry.TABLE_NAME,
            arrayOf(MapDataEntry.COLUMN_NAME_TIMESTAMP),
            "${MapDataEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, "${MapDataEntry.COLUMN_NAME_TIMESTAMP} ASC"
        )
        mapDataCursor.use {
            if (it.moveToFirst()) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    startTime = dateFormat.parse(it.getString(0))?.time
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao parsear startTime: ${e.message}")
                }
            }
            if (it.moveToLast()) {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    endTime = dateFormat.parse(it.getString(0))?.time
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao parsear endTime: ${e.message}")
                }
            }
        }

        // Calcular duração baseada nos timestamps GPS
        if (startTime != null && endTime != null) {
            duration = (endTime!! - startTime!!) / 1000.0 // em segundos
        }

        // Calcular calorias totais baseado no tempo e potência média
        // Fórmula simplificada: ~500 calorias por hora para ciclismo moderado
        val powerCursor = db.query(
            PowerEntry.TABLE_NAME,
            arrayOf(PowerEntry.COLUMN_NAME_POWER, PowerEntry.COLUMN_NAME_TIMESTAMP),
            "${PowerEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, PowerEntry.COLUMN_NAME_TIMESTAMP + " ASC"
        )
        val powerReadings = mutableListOf<Pair<Double, String>>()
        powerCursor.use {
            while (it.moveToNext()) {
                powerReadings.add(Pair(it.getDouble(0), it.getString(1)))
            }
        }
        if (powerReadings.isNotEmpty()) {
            val totalTimeHours = duration / 3600.0 // converter segundos para horas
            val avgPower = powerReadings.map { it.first }.average()
            // Estimativa: 1 watt médio ≈ 0.24 calorias por hora (860 cal/kWh ÷ 3600)
            calories = avgPower * totalTimeHours * 0.24
        } else {
            // Fallback: estimativa baseada apenas no tempo (500 cal/hora)
            val totalTimeHours = duration / 3600.0
            calories = 500.0 * totalTimeHours
        }

        // Calcular distância aproximada baseada em pontos GPS
        val locationCursor = db.query(
            MapDataEntry.TABLE_NAME,
            arrayOf(MapDataEntry.COLUMN_NAME_LATITUDE, MapDataEntry.COLUMN_NAME_LONGITUDE),
            "${MapDataEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, "${MapDataEntry.COLUMN_NAME_TIMESTAMP} ASC"
        )
        var prevLat = 0.0
        var prevLon = 0.0
        var firstPoint = true
        locationCursor.use {
            while (it.moveToNext()) {
                val lat = it.getDouble(0)
                val lon = it.getDouble(1)
                if (firstPoint) {
                    // Initialize with first point
                    prevLat = lat
                    prevLon = lon
                    firstPoint = false
                } else {
                    // Cálculo aproximado de distância em km usando fórmula de Haversine
                    val dLat = Math.toRadians(lat - prevLat)
                    val dLon = Math.toRadians(lon - prevLon)
                    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                            Math.cos(Math.toRadians(prevLat)) * Math.cos(Math.toRadians(lat)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2)
                    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                    val segmentDistance = 6371 * c // Raio da Terra em km
                    distance += segmentDistance
                    prevLat = lat
                    prevLon = lon
                }
            }
        }

        val result = mapOf(
            "duration" to duration,
            "distance" to distance,
            "maxVelocity" to maxVelocity,
            "meanVelocity" to meanVelocity,
            "calories" to calories,
            "startTime" to startTime,
            "endTime" to endTime
        ).also {
            Log.d(TAG, "Final stats for ride $rideId: duration=$duration, distance=$distance, calories=$calories")
        }

        // Atualizar o banco de dados com os valores calculados
        updateRideSummary(rideId, distance, duration, calories, maxVelocity, meanVelocity)

        return result
    }

    /**
     * Retorna todos os dados de velocidade de uma corrida.
     */
    fun getRideVelocities(rideId: Long): List<Pair<String, Double>> {
        val db = this.readableDatabase
        val cursor = db.query(
            VelocityEntry.TABLE_NAME,
            arrayOf(VelocityEntry.COLUMN_NAME_TIMESTAMP, VelocityEntry.COLUMN_NAME_VELOCITY),
            "${VelocityEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, VelocityEntry.COLUMN_NAME_TIMESTAMP + " ASC"
        )
        val velocities = mutableListOf<Pair<String, Double>>()
        cursor.use {
            while (it.moveToNext()) {
                velocities.add(Pair(it.getString(0), it.getDouble(1)))
            }
        }
        return velocities
    }

    /**
     * Atualiza o resumo de uma corrida com dados calculados
     */
    fun updateRideSummary(rideId: Long, distance: Double, duration: Double, calories: Double, maxVelocity: Double? = null, meanVelocity: Double? = null) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(RidesEntry.COLUMN_NAME_DISTANCE, distance)
            put(RidesEntry.COLUMN_NAME_DURATION, duration)
            put(RidesEntry.COLUMN_NAME_CALORIES, calories)
            put(RidesEntry.COLUMN_NAME_END_TIME, getCurrentTimestamp())
            if (maxVelocity != null) {
                put(RidesEntry.COLUMN_NAME_MAX_VELOCITY, maxVelocity)
            }
            if (meanVelocity != null) {
                put(RidesEntry.COLUMN_NAME_MEAN_VELOCITY, meanVelocity)
            }
        }
        val rowsAffected = db.update(
            RidesEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(rideId.toString())
        )
        Log.d(TAG, "Updated ride summary for ride $rideId: $rowsAffected rows affected")
    }

    /**
     * Retorna todos os dados de mapa de uma corrida.
     */
    fun getRideMapData(rideId: Long): List<Triple<String, Double, Double>> {
        val db = this.readableDatabase
        val cursor = db.query(
            MapDataEntry.TABLE_NAME,
            arrayOf(MapDataEntry.COLUMN_NAME_TIMESTAMP, MapDataEntry.COLUMN_NAME_LATITUDE, MapDataEntry.COLUMN_NAME_LONGITUDE),
            "${MapDataEntry.COLUMN_NAME_RIDE_ID} = ?",
            arrayOf(rideId.toString()),
            null, null, MapDataEntry.COLUMN_NAME_TIMESTAMP + " ASC"
        )
        val mapData = mutableListOf<Triple<String, Double, Double>>()
        cursor.use {
            while (it.moveToNext()) {
                mapData.add(Triple(it.getString(0), it.getDouble(1), it.getDouble(2)))
            }
        }
        return mapData
    }
}
