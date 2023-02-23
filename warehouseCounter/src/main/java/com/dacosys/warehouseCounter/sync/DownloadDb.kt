package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.network.SendItemCode
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.database.FileHelper
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_NAME
import com.dacosys.warehouseCounter.sync.DownloadDb.DownloadStatus.*
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.concurrent.thread


class DownloadDb : SendItemCode.TaskSendItemCodeEnded, DownloadFileTask.OnDownloadFileTask {

    interface DownloadDbTask {
        // Define data you like to return from AysncTask
        fun onDownloadDbTask(downloadStatus: DownloadStatus)

        fun onDownloadFileTask(
            msg: String,
            fileType: FileType,
            downloadStatus: DownloadStatus,
            progress: Int?,
        )
    }

    override fun onTaskSendItemCodeEnded(status: ProgressStatus, msg: String) {
        uploadStatus = status
        val progressStatusDesc = (uploadStatus as ProgressStatus).description
        val registryDesc = context().getString(R.string.item_codes)

        if (uploadStatus == ProgressStatus.crashed || downloadStatus == CRASHED) {
            ErrorLog.writeLog(
                null, this::class.java.simpleName, "$progressStatusDesc: $registryDesc, $msg"
            )
            onEventData(SnackBarEventData(msg, ERROR))
        } else {
            Log.d(this::class.java.simpleName, "$progressStatusDesc: $registryDesc, $msg")
        }
    }

    override fun onDownloadFileTask(
        msg: String,
        fileType: FileType,
        downloadStatus: DownloadStatus,
        progress: Int?,
        bytesCompleted: Long?,
        bytesTotal: Long?,
    ) {
        this.fileType = fileType
        this.downloadStatus = downloadStatus

        if (downloadStatus == CRASHED) {
            ErrorLog.writeLog(
                null, this::class.java.simpleName, "${downloadStatus.name}: ${fileType.name}, $msg"
            )

            // Si falla en el Timefile puede ser por no tener conexión.
            // No mostrar error
            if (fileType == FileType.TIMEFILE) {
                onEventData(
                    SnackBarEventData(
                        context().getString(R.string.offline_mode), SnackBarType.INFO
                    )
                )
            } else {
                onEventData(SnackBarEventData(msg, ERROR))
            }
        }

        Log.d(this::class.java.simpleName, "${downloadStatus.name}: ${fileType.name}, $msg")
        mCallback?.onDownloadFileTask(msg, fileType, downloadStatus, progress)
    }

    /**
     * Tiene el tipo de archivo que se está descargando.
     * De esto depende la lógica para la secuencia de descargas.
     */
    enum class FileType(val id: Int) {
        TIMEFILE(1), DBFILE(2)
    }

    /**
     * Tiene los diferentes estados durante una descarga
     */
    enum class DownloadStatus(val id: Int) {
        STARTING(1), DOWNLOADING(2), CANCELED(3), FINISHED(4), CRASHED(5), INFO(6)
    }

    /////////////////////
    // region Privadas //
    private var errorMsg = ""

    @Volatile
    private var downloadStatus: DownloadStatus? = null

    @Volatile
    private var uploadStatus: ProgressStatus? = null

    private var timeFilename = ""
    private var dbFilename = ""
    private var oldDateTimeStr: String = ""
    private var currentDateTimeStr: String = ""
    private var fileType: FileType? = null

    private var mCallback: DownloadDbTask? = null
    // endregion Privadas //
    ////////////////////////

    ///////////////////////
    // region Constantes //
    private var destinationTimeFile: File? = null
    private var destinationDbFile: File? = null

    private var onEventData: (SnackBarEventData) -> Unit = {}
    private var completeDbFileUrl = ""
    private var completeTimeFileUrl = ""

    // endregion Constantes //
    //////////////////////////

    fun addParams(
        callBack: DownloadDbTask,
        timeFileUrl: String, //"android.wc.time.txt",
        dbFileUrl: String, //"android.wc.sqlite.txt",
        onEventData: (SnackBarEventData) -> Unit = {},
    ) {
        val sv = settingViewModel()

        mCallback = callBack
        this.onEventData = onEventData

        timeFilename = timeFileUrl.substringAfterLast('/')
        dbFilename = dbFileUrl.substringAfterLast('/')

        destinationTimeFile = File("${context().cacheDir.absolutePath}/$timeFilename")
        destinationDbFile = File("${context().cacheDir.absolutePath}/$dbFilename")

        completeDbFileUrl = "${sv.urlPanel}/$dbFileUrl"
        completeTimeFileUrl = "${sv.urlPanel}/$timeFileUrl"
    }

    private fun postExecute(result: Boolean) {
        if (result) {
            Log.d(this::class.java.simpleName, context().getString(R.string.ok))
        } else {
            onEventData(SnackBarEventData(errorMsg, ERROR))
            ErrorLog.writeLog(null, this::class.java.simpleName, errorMsg)
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        if (Statics.downloadDbRequired) deleteTimeFile()

        scope.launch {
            coroutineScope {
                withContext(Dispatchers.Default) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        if (!Statics.isOnline()) {
            mCallback?.onDownloadDbTask(CANCELED)
            return@withContext false
        }

        mCallback?.onDownloadDbTask(STARTING)
        goForrest()
    }

    private fun goForrest() {
        try {
            val destTimeFile = destinationTimeFile
            val destDbFile = destinationDbFile

            if (dbFilename.isEmpty() || timeFilename.isEmpty() || destTimeFile == null || destDbFile == null) {
                errorMsg = context().getString(R.string.database_name_is_invalid)
                mCallback?.onDownloadDbTask(CRASHED)
                return
            }

            /////////////////////////////////////
            // region: Enviar datos pendientes //

            // Si aún no está loggeado y hay datos por enviar, no descargar la base de datos
            if (Statics.currentUserId > 0) {
                uploadStatus = null

                ItemCodeCoroutines().getToUpload {
                    try {
                        thread {
                            val task = SendItemCode()
                            task.addParams(this, it)
                            task.execute()
                        }
                    } catch (ex: Exception) {
                        ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                    }

                    // Espera hasta que salga del SyncUpload
                    // Tiene que terminar de enviar los datos pendientes para continuar con la descarga
                    loop@ while (true) {
                        when (uploadStatus) {
                            ProgressStatus.finished -> break@loop
                            ProgressStatus.crashed, ProgressStatus.canceled,
                            -> {
                                errorMsg =
                                    context().getString(R.string.error_trying_to_send_item_codes)
                                mCallback?.onDownloadDbTask(CRASHED)
                                return@getToUpload
                            }
                        }
                    }
                }
            }

            if (uploadStatus == ProgressStatus.crashed) return
            // endregion: Enviar datos pendientes //
            ////////////////////////////////////////

            // Leer el archivo antiguo de fecha de creación de la base de datos
            // en el servidor, si la esta fecha es igual a la del archivo del servidor,
            // no hace falta descargar la base de datos.
            if (destTimeFile.exists()) {
                oldDateTimeStr = getDateTimeStr()
                destTimeFile.delete()
            }

            downloadStatus = null

            var downloadTask = DownloadFileTask()
            downloadTask.addParams(
                UrlDestParam(
                    url = completeTimeFileUrl, destination = destTimeFile
                ), this, FileType.TIMEFILE
            )
            downloadTask.execute()

            var crashNr = 0
            while (true) {
                if (downloadStatus == null || fileType == null) continue

                when (downloadStatus) {
                    CANCELED -> {

                        // Si se cancela, sale

                        mCallback?.onDownloadDbTask(CANCELED)
                        return
                    }
                    FINISHED -> {

                        // Si estamos descargando el archivo de la fecha
                        // y termina de descargarse salir del loop para poder hacer
                        // las comparaciones

                        if (fileType == FileType.TIMEFILE) {
                            break
                        } else {
                            downloadStatus = null
                            downloadTask = DownloadFileTask()
                            downloadTask.addParams(
                                UrlDestParam(
                                    url = completeTimeFileUrl, destination = destTimeFile
                                ), this, FileType.TIMEFILE
                            )
                            downloadTask.execute()
                        }
                    }
                    CRASHED -> {

                        // Si no existe el archivo con la fecha en el servidor
                        // es porque aún no se creó la base de datos.

                        if (fileType == FileType.TIMEFILE) {
                            // Si falla al bajar la fecha
                            crashNr++
                        }

                        if (crashNr > 1) {
                            // Si ya falló dos veces en bajar la fecha, a la mierda.
                            errorMsg =
                                context().getString(R.string.failed_to_get_the_db_creation_date_from_the_server)
                            mCallback?.onDownloadDbTask(CRASHED)
                            return
                        }
                    }
                    else -> {}
                }
                // TODO: Poner un Timer que salga de acá
            }

            //Read text from file
            currentDateTimeStr = getDateTimeStr()

            // True solo en DEBUG
            if (!Statics.downloadDbAlways) {
                if (oldDateTimeStr == currentDateTimeStr) {
                    Log.d(
                        this::class.java.simpleName,
                        context().getString(R.string.is_not_necessary_to_download_the_database)
                    )

                    mCallback?.onDownloadDbTask(FINISHED)
                    return
                }
            }

            // Eliminar la base de datos antigua
            if (destDbFile.exists()) {
                destDbFile.delete()
            }

            try {
                downloadStatus = null
                downloadTask = DownloadFileTask()
                downloadTask.addParams(
                    UrlDestParam(
                        url = completeDbFileUrl, destination = destDbFile
                    ), this, FileType.DBFILE
                )
                downloadTask.execute()

                while (true) {
                    if (downloadStatus == null || fileType == null) continue

                    when (downloadStatus) {
                        CANCELED, CRASHED -> {
                            // Si se cancela o choca, sale
                            errorMsg =
                                context().getString(R.string.error_downloading_the_database_from_the_server)
                            mCallback?.onDownloadDbTask(CRASHED)
                            return
                        }
                        FINISHED -> {
                            break
                        }
                        else -> {}
                    }
                }
            } catch (ex: Exception) {
                errorMsg = "${
                    context().getString(R.string.exception_error)
                } (Download database): ${ex.message}"
                mCallback?.onDownloadDbTask(CRASHED)
                return
            }

            Statics.downloadDbRequired = false

            // Detener la instancia actual de la base de datos de Room.
            WcDatabase.cleanInstance()

            // Borrar la base de datos actual de Room del almacenamiento del dispositivo.
            // Copiar la nueva base de datos descargada desde la web a la ubicación de la base de datos anterior.
            // Se iniciará una nueva instancia de la base de datos de Room utilizando la base de datos actualizada la próxima vez que se la invoque.
            FileHelper.copyDataBase(destinationDbFile)

        } catch (ex: Exception) {
            errorMsg = context().getString(R.string.error_downloading_the_database)
            mCallback?.onDownloadDbTask(CRASHED)
            return
        }

        mCallback?.onDownloadDbTask(FINISHED)
        return
    }


    private fun getDateTimeStr(): String {
        var dateTime = ""
        //Read text from file
        try {
            val br = BufferedReader(FileReader(destinationTimeFile!!.absolutePath))
            while (true) {
                dateTime = br.readLine() ?: break
            }
            br.close()
        } catch (ex: Exception) {
            errorMsg = "${
                context().getString(R.string.failed_to_get_the_date_from_the_file)
            }: ${ex.message}"
        }
        return dateTime
    }

    private fun deleteTimeFile() {
        destinationTimeFile = File(context().cacheDir.absolutePath + "/" + timeFilename)
        if (destinationTimeFile?.exists() == true) {
            destinationTimeFile?.delete()
        }
    }

    companion object {
        fun statusEnd(): ArrayList<DownloadStatus> {
            val r = ArrayList<DownloadStatus>()
            r.add(CANCELED)
            r.add(FINISHED)
            r.add(CRASHED)
            return r
        }

        @Throws(SQLException::class)
        fun getConnection(dbPath: String): Connection? {
            var connection: Connection? = null
            try {
                Class.forName("org.sqlite.JDBC")
                connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
            return connection
        }

        /**
         * Convert db schema
         * Esta función intenta convertir los tipos de SQLite en Room.
         */
        fun convertDbSchema() {
            val myTag = "ConvertDbSchema"
            Log.d(myTag, "Intentando convertir base de datos SQLite a Room...")
            val connection =
                getConnection(context().getDatabasePath(DATABASE_NAME).absolutePath) ?: return

            val tables = connection.metaData.getTables(
                /* catalog = */ null,
                /* schemaPattern = */ null,
                /* tableNamePattern = */ null,
                /* types = */ arrayOf("TABLE")
            )

            while (tables.next()) {

                val tableName = tables.getString("TABLE_NAME")
                Log.i(myTag, "Convirtiendo $tableName")

                val columns = connection.metaData.getColumns(
                    /* catalog = */ null,
                    /* schemaPattern = */ null,
                    /* tableNamePattern = */ tableName,
                    /* columnNamePattern = */ null
                )

                val newColumns = mutableListOf<String>()
                while (columns.next()) {
                    val columnName = columns.getString("COLUMN_NAME")

                    // Convert data types
                    val newDataType = when (val dataType = columns.getString("TYPE_NAME")) {
                        "INT" -> "INTEGER"
                        "INTEGER" -> "INTEGER"
                        "TINYINT" -> "INTEGER"
                        "SMALLINT" -> "INTEGER"
                        "MEDIUMINT" -> "INTEGER"
                        "BIGINT" -> "INTEGER"
                        "UNSIGNED BIGINT" -> "INTEGER"
                        "INT2" -> "INTEGER"
                        "INT8" -> "INTEGER"

                        "REAL" -> "REAL"
                        "DOUBLE" -> "REAL"
                        "DOUBLE PRECISION" -> "REAL"
                        "FLOAT" -> "REAL"

                        "CHARACTER" -> "TEXT"
                        "VARCHAR" -> "TEXT"
                        "VARYING CHARACTER" -> "TEXT"
                        "NCHAR" -> "TEXT"
                        "NATIVE CHARACTER" -> "TEXT"
                        "NVARCHAR" -> "TEXT"
                        "TEXT" -> "TEXT"
                        "CLOB" -> "TEXT"

                        "BLOB" -> "BLOB"

                        "NUMERIC" -> "NUMERIC"
                        "DECIMAL" -> "NUMERIC"
                        "BOOLEAN" -> "NUMERIC"
                        "DATE" -> "NUMERIC"
                        "DATETIME" -> "NUMERIC"
                        else -> dataType
                    }

                    val newColumn = "$columnName $newDataType"
                    newColumns.add(newColumn)
                }

                val newSchema = newColumns.joinToString(", ")
                val alterTable = "ALTER TABLE $tableName RENAME TO temp_$tableName;"
                val createTable = "CREATE TABLE $tableName ($newSchema);"
                val copyData = "INSERT INTO $tableName SELECT * FROM temp_$tableName;"
                val dropTempTable = "DROP TABLE temp_$tableName;"

                val statements = listOf(alterTable, createTable, copyData, dropTempTable)
                statements.forEach {
                    Log.i(myTag, it)
                    connection.createStatement().execute(it)
                }
            }

            connection.close()
        }
    }
}