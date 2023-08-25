package com.dacosys.warehouseCounter.settings.utils

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.core.content.FileProvider
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.File

class DownloadController(private val view: View) {

    companion object {
        private const val FILE_NAME = "warehouseCounter-release.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"

        private const val APK_URL =
            "http://resources.dacosys.com/Warehouse_Counter/Milestone12/installers/android/warehouseCounter-release.apk"
    }

    fun enqueueDownload() {
        val context = context

        var destination =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME

        val uri = Uri.parse("$FILE_BASE_PATH$destination")

        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(APK_URL)
        val request = DownloadManager.Request(downloadUri)

        request.setMimeType(MIME_TYPE)
        request.setTitle(context.getString(R.string.apk_is_downloading))
        request.setDescription(context.getString(R.string.downloading_))

        // set destination
        request.setDestinationUri(uri)

        showInstallOption(context, destination)

        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        showSnackBar(context.getString(R.string.downloading_), SnackBarType.INFO)
    }

    private fun showInstallOption(
        context: Context,
        destination: String,
    ) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                installAPK(context, destination)
                context.unregisterReceiver(this)
            }
        }

        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    fun installAPK(context: Context, destination: String) {
        val file = File(destination)
        if (file.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uriFromFile(context, destination), MIME_TYPE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                showSnackBar(context.getString(R.string.error_opening_the_file), SnackBarType.ERROR)
            }
        } else {
            showSnackBar(context.getString(R.string.file_not_found), SnackBarType.ERROR)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(view, text, snackBarType)
    }

    private fun uriFromFile(context: Context, destination: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                File(destination)
            )
        } else {
            Uri.fromFile(File(destination))
        }
    }
}
