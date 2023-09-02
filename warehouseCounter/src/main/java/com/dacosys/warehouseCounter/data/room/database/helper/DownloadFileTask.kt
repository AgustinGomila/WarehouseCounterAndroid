package com.dacosys.warehouseCounter.data.room.database.helper

import android.content.Context
import android.os.PowerManager
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import kotlinx.coroutines.*
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.System.lineSeparator
import java.net.HttpURLConnection
import java.net.URL


class DownloadFileTask {
    private var mWakeLock: PowerManager.WakeLock? = null
    private var urlDestination: UrlDestParam? = null
    private var mCallback: OnDownloadFileTask? = null
    private var fileType: DownloadDb.FileType? = null

    interface OnDownloadFileTask {
        fun onDownloadFileTask(
            msg: String,
            fileType: DownloadDb.FileType,
            downloadStatus: DownloadDb.DownloadStatus,
            progress: Int?,
            bytesCompleted: Long?,
            bytesTotal: Long?,
        )
    }

    fun addParams(
        urlDestination: UrlDestParam,
        listener: OnDownloadFileTask,
        fileType: DownloadDb.FileType,
    ) {
        this.urlDestination = urlDestination
        this.fileType = fileType
        this.mCallback = listener
    }

    private fun preExecute() {
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        mWakeLock?.acquire(3 * 60 * 1000L /*3 minutes*/)
    }

    private fun postExecute(result: Boolean): Boolean {
        mWakeLock?.release()

        if (fileType != null) {
            if (result) {
                mCallback?.onDownloadFileTask(
                    msg = "${context.getString(R.string.download_ok)}: ${fileType.toString()}",
                    fileType = fileType!!,
                    downloadStatus = DownloadDb.DownloadStatus.FINISHED,
                    progress = null,
                    bytesCompleted = null,
                    bytesTotal = null
                )
            } else {
                mCallback?.onDownloadFileTask(
                    msg = "${context.getString(R.string.download_error)}: ${fileType.toString()}",
                    fileType = fileType!!,
                    downloadStatus = DownloadDb.DownloadStatus.CRASHED,
                    progress = null,
                    bytesCompleted = null,
                    bytesTotal = null
                )
            }
        }

        return result
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        preExecute()
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
        return@withContext getDownloadTaskResult()
    }

    private fun getDownloadTaskResult(): Boolean {
        mCallback?.onDownloadFileTask(
            msg = context.getString(R.string.starting_download),
            fileType = fileType!!,
            downloadStatus = DownloadDb.DownloadStatus.STARTING,
            progress = null,
            bytesCompleted = null,
            bytesTotal = null
        )

        val destination = urlDestination!!.destination
        val urlStr = urlDestination!!.url

        if (destination.exists()) {
            mCallback?.onDownloadFileTask(
                msg = "${context.getString(R.string.destination_already_exists)}: $destination",
                fileType = fileType!!,
                downloadStatus = DownloadDb.DownloadStatus.INFO,
                progress = null,
                bytesCompleted = null,
                bytesTotal = null
            )
            return true
        }

        mCallback?.onDownloadFileTask(
            msg = "${context.getString(R.string.destination)}: $destination${lineSeparator()}URL: $urlStr",
            fileType = fileType!!,
            downloadStatus = DownloadDb.DownloadStatus.INFO,
            progress = null,
            bytesCompleted = null,
            bytesTotal = null
        )

        var input: InputStream? = null
        var output: OutputStream? = null

        var connection: HttpURLConnection? = null

        try {
            val url = URL(urlStr)

            mCallback?.onDownloadFileTask(
                msg = "${context.getString(R.string.opening_connection)}: $urlStr",
                fileType = fileType!!,
                downloadStatus = DownloadDb.DownloadStatus.INFO,
                progress = null,
                bytesCompleted = null,
                bytesTotal = null
            )
            connection = url.openConnection() as HttpURLConnection
            connection.connect()

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                mCallback?.onDownloadFileTask(
                    msg = "${context.getString(R.string.error_connecting_to)} $urlStr: Server returned HTTP ${connection.responseCode} ${connection.responseMessage}",
                    fileType = fileType!!,
                    downloadStatus = DownloadDb.DownloadStatus.CRASHED,
                    progress = null,
                    bytesCompleted = null,
                    bytesTotal = null
                )
                return false
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            val fileLength = connection.contentLength
            mCallback?.onDownloadFileTask(
                msg = "${context.getString(R.string.file_length)}: $fileLength",
                fileType = fileType!!,
                downloadStatus = DownloadDb.DownloadStatus.INFO,
                progress = null,
                bytesCompleted = null,
                bytesTotal = null
            )

            // Crear un nuevo archivo
            if (destination.exists()) {
                destination.delete()
            }
            destination.createNewFile()

            // download the file
            input = connection.inputStream
            output = FileOutputStream(destination)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            do {
                count = input.read(data)
                if (count == -1) {
                    break
                }

                // allow canceling with back button
                if (deferred?.isCancelled == true) {
                    mCallback?.onDownloadFileTask(
                        msg = context
                            .getString(R.string.download_canceled),
                        fileType = fileType!!,
                        downloadStatus = DownloadDb.DownloadStatus.CANCELED,
                        progress = null,
                        bytesCompleted = null,
                        bytesTotal = null
                    )
                    input.close()
                    return false
                }

                total += count.toLong()
                // publishing the progress....

                if (fileLength > 0) {
                    // only if total length is known
                    mCallback?.onDownloadFileTask(
                        msg = context.getString(R.string.downloading_),
                        fileType = fileType!!,
                        downloadStatus = DownloadDb.DownloadStatus.DOWNLOADING,
                        progress = (total * 100 / fileLength).toInt(),
                        bytesCompleted = total,
                        bytesTotal = fileLength.toLong()
                    )
                }
                output.write(data, 0, count)
            } while (true)
        } catch (e: Exception) {
            mCallback?.onDownloadFileTask(
                msg = "${context.getString(R.string.exception_when_downloading)}: ${e.message}",
                fileType = fileType!!,
                downloadStatus = DownloadDb.DownloadStatus.CRASHED,
                progress = null,
                bytesCompleted = null,
                bytesTotal = null
            )
            return false
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }
            connection?.disconnect()
        }
        return true
    }
}
