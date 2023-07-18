package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.imageControl.network.upload.SendPending
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.syncViewModel
import com.dacosys.warehouseCounter.ktor.v2.functions.SendItemCodeArray
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
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
import kotlin.concurrent.thread


class DownloadDb : DownloadFileTask.OnDownloadFileTask {

    interface DownloadDbTask {
        fun onDownloadDbTask(downloadStatus: DownloadStatus)

        fun onDownloadFileTask(
            msg: String,
            fileType: FileType,
            downloadStatus: DownloadStatus,
            progress: Int?,
        )
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

            /** If it fails to file the DB creation date, it may be due to no connection.
             * Don't show the error message.
             */
            if (fileType == FileType.TIME_FILE) {
                onEventData(
                    SnackBarEventData(
                        context.getString(R.string.offline_mode), SnackBarType.INFO
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
        TIME_FILE(1), DB_FILE(2)
    }

    /**
     * Tiene los diferentes estados durante una descarga
     */
    enum class DownloadStatus(val id: Int) {
        STARTING(1), DOWNLOADING(2), CANCELED(3), FINISHED(4), CRASHED(5), INFO(6), COPYING(7)
    }

    /////////////////////
    // region Privadas //
    private var errorMsg = ""

    @Volatile
    private var downloadStatus: DownloadStatus? = null

    @Volatile
    private var uploadStatus: ProgressStatus? = null

    private var oldDateTimeStr: String = ""
    private var currentDateTimeStr: String = ""
    private var fileType: FileType? = null

    private var mCallback: DownloadDbTask? = null
    // endregion Privadas //
    ////////////////////////

    ///////////////////////
    // region Constantes //
    private var destTimeFile: File? = null
    private var destDbFile: File? = null

    private val timeFileName: String = "android.wc.time.txt"

    private var onEventData: (SnackBarEventData) -> Unit = {}

    private var dbFileUrl = ""
    private var timeFileUrl = ""

    // endregion Constantes //
    //////////////////////////

    fun addParams(
        callBack: DownloadDbTask,
        timeFileUrl: String, //"android.wc.time.txt",
        dbFileUrl: String, //"android.wc.sqlite.txt",
        onEventData: (SnackBarEventData) -> Unit = {},
    ) {
        mCallback = callBack
        this.onEventData = onEventData

        this.dbFileUrl = dbFileUrl
        this.timeFileUrl = timeFileUrl

        destTimeFile = File("${context.cacheDir.absolutePath}/${timeFileName}")
        destDbFile = File("${context.cacheDir.absolutePath}/${DATABASE_NAME}")
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        if (Statics.downloadDbRequired) deleteTimeFile()

        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        if (!Statics.isOnline()) {
            mCallback?.onDownloadDbTask(CANCELED)
            return@withContext
        }

        mCallback?.onDownloadDbTask(STARTING)
        goForrest()
    }

    private fun goForrest() {
        try {
            val destTimeFile = destTimeFile
            val destDbFile = destDbFile

            if (dbFileUrl.isEmpty() || timeFileUrl.isEmpty() || destTimeFile == null || destDbFile == null) {
                errorMsg = context.getString(R.string.database_name_is_invalid)
                mCallback?.onDownloadDbTask(CRASHED)
                return
            }

            // Enviar datos pendientes si existen
            sendPendingData()

            if (uploadStatus == ProgressStatus.crashed) return

            // Leer el archivo antiguo de fecha de creación de la base de datos
            // en el servidor, si la fecha es igual a la del archivo del servidor,
            // no hace falta descargar la base de datos.
            if (destTimeFile.exists()) {
                oldDateTimeStr = getDateTimeStr()
                destTimeFile.delete()
            }

            downloadStatus = null

            var downloadTask = DownloadFileTask()
            downloadTask.addParams(
                UrlDestParam(
                    url = timeFileUrl, destination = destTimeFile
                ), this, FileType.TIME_FILE
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

                        if (fileType == FileType.TIME_FILE) {
                            break
                        } else {
                            downloadStatus = null
                            downloadTask = DownloadFileTask()
                            downloadTask.addParams(
                                UrlDestParam(
                                    url = timeFileUrl, destination = destTimeFile
                                ), this, FileType.TIME_FILE
                            )
                            downloadTask.execute()
                        }
                    }

                    CRASHED -> {

                        // Si no existe el archivo con la fecha en el servidor
                        // es porque aún no se creó la base de datos.

                        if (fileType == FileType.TIME_FILE) {
                            // Si falla al bajar la fecha
                            crashNr++
                        }

                        if (crashNr > 1) {
                            // Si ya falló dos veces en bajar la fecha, a la mierda.
                            errorMsg = context.getString(R.string.failed_to_get_the_db_creation_date_from_the_server)
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
            if (!Statics.DOWNLOAD_DB_ALWAYS) {
                if (oldDateTimeStr == currentDateTimeStr) {
                    Log.d(
                        this::class.java.simpleName,
                        context.getString(R.string.is_not_necessary_to_download_the_database)
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
                        url = dbFileUrl, destination = destDbFile
                    ), this, FileType.DB_FILE
                )
                downloadTask.execute()

                while (true) {
                    if (downloadStatus == null || fileType == null) continue

                    when (downloadStatus) {
                        CANCELED, CRASHED -> {
                            // Si se cancela o choca, sale
                            errorMsg = context.getString(R.string.error_downloading_the_database_from_the_server)
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
                    context.getString(R.string.exception_error)
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
            FileHelper.copyDataBase(this.destDbFile, mCallback)

        } catch (ex: Exception) {
            errorMsg = context.getString(R.string.error_downloading_the_database)
            mCallback?.onDownloadDbTask(CRASHED)
            return
        }

        mCallback?.onDownloadDbTask(FINISHED)
        return
    }

    /**
     * Send pending data
     * Enviar los datos pendientes y las imágenes
     */
    private fun sendPendingData() {
        // Si está autentificado y hay datos por enviar,
        // enviar la información y después descargar la base de datos
        uploadStatus = ProgressStatus.unknown

        if (Statics.currentUserId > 0) {
            ItemCodeCoroutines.getToUpload {
                try {
                    thread {
                        SendItemCodeArray(
                            payload = it,
                            onEvent = { onEventData(it) },
                            onFinish = { it2 ->
                                uploadStatus =
                                    if (it2.size == it.size) ProgressStatus.finished
                                    else ProgressStatus.crashed
                            }).execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                }

                /** Wait until I finish sending all the Item Codes.
                 * You have to finish sending the pending data to continue with the download.
                 */
                loop@ while (true) {
                    when (uploadStatus) {
                        ProgressStatus.finished -> break@loop
                        ProgressStatus.crashed, ProgressStatus.canceled,
                        -> {
                            errorMsg = context.getString(R.string.error_trying_to_send_item_codes)
                            mCallback?.onDownloadDbTask(CRASHED)
                            return@getToUpload
                        }
                    }
                }
            }

            // Enviar las imágenes pendientes...
            if (uploadStatus == ProgressStatus.finished && settingViewModel.useImageControl) {
                SendPending(onProgress = { syncViewModel.setUploadImagesProgress(it) })
            }
        }
    }

    private fun getDateTimeStr(): String {
        var dateTime = ""
        //Read text from file
        try {
            val br = BufferedReader(FileReader(destTimeFile!!.absolutePath))
            while (true) {
                dateTime = br.readLine() ?: break
            }
            br.close()
        } catch (ex: Exception) {
            errorMsg = "${
                context.getString(R.string.failed_to_get_the_date_from_the_file)
            }: ${ex.message}"
        }
        return dateTime
    }

    private fun deleteTimeFile() {
        destTimeFile = File(context.cacheDir.absolutePath + "/" + timeFileName)
        if (destTimeFile?.exists() == true) {
            destTimeFile?.delete()
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
    }
}