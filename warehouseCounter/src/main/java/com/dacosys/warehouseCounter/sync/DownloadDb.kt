package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dataBaseHelper.DataBaseHelper.Companion.copyDataBase
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.retrofit.functions.SendItemCode
import com.dacosys.warehouseCounter.sync.DownloadDb.DownloadStatus.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
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
        this.progressStatus = status
        val progressStatusDesc = (progressStatus as ProgressStatus).description
        val registryDesc = context().getString(R.string.item_codes)

        if (progressStatus == ProgressStatus.crashed || downloadStatus == CRASHED) {
            ErrorLog.writeLog(null,
                this::class.java.simpleName,
                "$progressStatusDesc: $registryDesc, $msg")
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
            ErrorLog.writeLog(null,
                this::class.java.simpleName,
                "${downloadStatus.name}: ${fileType.name}, $msg")

            // Si falla en el Timefile puede ser por no tener conexión.
            // No mostrar error
            if (fileType == FileType.TIMEFILE) {
                onEventData(SnackBarEventData(context().getString(R.string.offline_mode),
                    SnackBarType.INFO))
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
    private var downloadStatus: DownloadStatus? = null
    private var progressStatus: ProgressStatus? = null

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

        this.mCallback = callBack
        this.onEventData = onEventData

        this.timeFilename = timeFileUrl.substringAfterLast('/')
        this.dbFilename = dbFileUrl.substringAfterLast('/')

        this.destinationTimeFile = File("${context().cacheDir.absolutePath}/$timeFilename")
        this.destinationDbFile = File("${context().cacheDir.absolutePath}/$dbFilename")

        this.completeDbFileUrl = "${sv.urlPanel}/$dbFileUrl"
        this.completeTimeFileUrl = "${sv.urlPanel}/$timeFileUrl"
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
            val it = doInBackground()
            postExecute(it)
        }
    }

    private var deferred: Deferred<Boolean>? = null
    private suspend fun doInBackground(): Boolean {
        var result = false
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await() ?: false
        }
        return result
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        if (!Statics.isOnline()) {
            mCallback?.onDownloadDbTask(CANCELED)
            return@withContext false
        }

        mCallback?.onDownloadDbTask(STARTING)
        return@withContext goForrest()
    }

    private fun goForrest(): Boolean {
        try {
            if (dbFilename.isEmpty() || timeFilename.isEmpty() || destinationTimeFile == null || destinationDbFile == null) {
                errorMsg = context().getString(R.string.database_name_is_invalid)
                mCallback?.onDownloadDbTask(CRASHED)
                return false
            }

            /////////////////////////////////////
            // region: Enviar datos pendientes //

            // Si aún no está loggeado y hay datos por enviar, no descargar la base de datos
            val itemCodeArray = ItemCodeDbHelper().selectToUpload()
            if (Statics.currentUserId > 0 && itemCodeArray.isNotEmpty()) {
                progressStatus = null
                try {
                    thread {
                        val task = SendItemCode()
                        task.addParams(this, itemCodeArray)
                        task.execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                }

                // Espera hasta que salga del SyncUpload
                loop@ while (true) {
                    when (progressStatus) {
                        ProgressStatus.finished -> break@loop
                        ProgressStatus.crashed, ProgressStatus.canceled,
                        -> {
                            errorMsg = context().getString(R.string.error_trying_to_send_item_codes)
                            mCallback?.onDownloadDbTask(CRASHED)
                            return false
                        }
                    }
                }
            }
            // endregion: Enviar datos pendientes //
            ////////////////////////////////////////

            // Leer el archivo antiguo de fecha de creación de la base de datos
            // en el servidor, si la esta fecha es igual a la del archivo del servidor,
            // no hace falta descargar la base de datos.
            if (destinationTimeFile!!.exists()) {
                oldDateTimeStr = getDateTimeStr()
                destinationTimeFile!!.delete()
            }

            var downloadTask = DownloadFileTask()
            downloadStatus = null
            downloadTask.addParams(UrlDestParam(url = completeTimeFileUrl,
                destination = destinationTimeFile!!), this, FileType.TIMEFILE)
            downloadTask.execute()

            var crashNr = 0
            while (true) {
                if (downloadStatus != null && fileType != null) {
                    // Si se cancela, sale
                    if (downloadStatus == CANCELED) {
                        mCallback?.onDownloadDbTask(CANCELED)
                        return false
                    } else if (downloadStatus == FINISHED) {

                        // Si estamos descargando el archivo de la fecha
                        // y termina de descargarse salir del loop para poder hacer
                        // las comparaciones

                        if (fileType == FileType.TIMEFILE) {
                            break
                        } else {
                            downloadStatus = null
                            downloadTask = DownloadFileTask()
                            downloadTask.addParams(UrlDestParam(url = completeTimeFileUrl,
                                destination = destinationTimeFile!!), this, FileType.TIMEFILE)
                            downloadTask.execute()
                        }
                    } else if (downloadStatus == CRASHED) {

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
                            return false
                        }
                    }
                }
                // Poner un timer o algo que salga de acá
            }

            //Read text from file
            currentDateTimeStr = getDateTimeStr()
            if (oldDateTimeStr == currentDateTimeStr) {
                Log.d(this::class.java.simpleName,
                    context().getString(R.string.is_not_necessary_to_download_the_database))

                mCallback?.onDownloadDbTask(FINISHED)
                return true
            }

            // Eliminar la base de datos antigua
            if (destinationDbFile!!.exists()) {
                destinationDbFile!!.delete()
            }

            try {
                downloadStatus = null
                downloadTask = DownloadFileTask()
                downloadTask.addParams(UrlDestParam(url = completeDbFileUrl,
                    destination = destinationDbFile!!), this, FileType.DBFILE)
                downloadTask.execute()

                while (true) {
                    if (downloadStatus != null && fileType != null) {
                        // Si se cancela, sale
                        if (downloadStatus == CANCELED || downloadStatus == CRASHED) {
                            errorMsg =
                                context().getString(R.string.error_downloading_the_database_from_the_server)
                            mCallback?.onDownloadDbTask(CRASHED)
                            return false
                        } else if (downloadStatus == FINISHED) {
                            break
                        }
                    }
                }
            } catch (ex: Exception) {
                errorMsg = "${
                    context().getString(R.string.exception_error)
                } (Download database): ${ex.message}"
                mCallback?.onDownloadDbTask(CRASHED)
                return false
            }

            Statics.downloadDbRequired = false
            copyDataBase(destinationDbFile)
        } catch (ex: Exception) {
            errorMsg = context().getString(R.string.error_downloading_the_database)
            mCallback?.onDownloadDbTask(CRASHED)
            return false
        }

        mCallback?.onDownloadDbTask(FINISHED)
        return true
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
    }
}