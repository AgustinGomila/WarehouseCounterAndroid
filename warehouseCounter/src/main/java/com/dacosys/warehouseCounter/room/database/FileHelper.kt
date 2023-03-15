package com.dacosys.warehouseCounter.room.database

import android.annotation.SuppressLint
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_NAME
import java.io.*
import kotlin.math.min

class FileHelper {
    companion object {
        private const val IMAGE_CONTROL_DATABASE_NAME = "imagecontrol.sqlite"

        fun removeDataBases() {
            removeImageControlDataBase()
            removeLocalDataBase()
        }

        private fun removeImageControlDataBase() {
            // Path to the just created empty db
            val outFileName = context.getDatabasePath(IMAGE_CONTROL_DATABASE_NAME).toString()

            try {
                Log.i("IC DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeICDataBase", e)
            }
        }

        private fun removeLocalDataBase() {
            // Path to the just created empty db
            val outFileName = context.getDatabasePath(DATABASE_NAME).toString()

            try {
                Log.i("Local DataBase", "Eliminando: $outFileName")
                val f = File(outFileName)
                if (f.exists()) {
                    f.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, "removeLocalDataBase", e)
            }
        }

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.<br>
         * <br>
         * Callers should check whether the path is local before assuming it
         * represents a local file.
         *
         * @param uri     The Uri to query.
         */

        @SuppressLint("NewApi")
        fun getPath(uri: Uri): String? {
            val selection: String?
            val selectionArgs: Array<String>?

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                val fullPath = getPathFromExtSD(split)
                return if (fullPath !== "") {
                    fullPath
                } else {
                    null
                }
            }

            // DownloadsProvider
            if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(
                        uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        val fileName: String = cursor.getString(0)
                        val path: String = Environment.getExternalStorageDirectory()
                            .toString() + "/Download/" + fileName
                        if (!TextUtils.isEmpty(path)) {
                            return path
                        }
                    }
                } finally {
                    cursor?.close()
                }
                val id: String = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:".toRegex(), "")
                    }
                    val contentUriPrefixesToTry = arrayOf(
                        "content://downloads/public_downloads", "content://downloads/my_downloads"
                    )
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        return try {
                            val contentUri: Uri = ContentUris.withAppendedId(
                                Uri.parse(contentUriPrefix), java.lang.Long.valueOf(id)
                            )
                            getDataColumn(contentUri, "", null)
                        } catch (e: NumberFormatException) {
                            //In Android 8 and Android P the id is not a number
                            uri.path!!.replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "")
                        }
                    }
                }
            }

            // MediaProvider
            if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }
                selection = "_id=?"
                selectionArgs = arrayOf(split[1])
                return getDataColumn(
                    contentUri!!, selection, selectionArgs
                )
            }
            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri)
            }
            if (isWhatsAppFile(uri)) {
                return getFilePathForWhatsApp(uri)
            }
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                if (isGoogleDriveUri(uri)) {
                    return getDriveFilePath(uri)
                }
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // return getFilePathFromURI(context,uri);
                    copyFileToInternalStorage(uri, "userfiles")
                    // return getRealPathFromURI(context,uri);
                } else {
                    getDataColumn(uri, "", null)
                }
            }
            if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
            return null
        }

        private fun fileExists(filePath: String): Boolean {
            val file = File(filePath)
            return file.exists()
        }

        private fun getPathFromExtSD(pathData: Array<String>): String {
            val type = pathData[0]
            val relativePath = "/" + pathData[1]
            var fullPath: String

            // on my Sony devices (4.4.4 & 5.1.1), `type` is a dynamic string
            // something like "71F8-2C0A", some kind of unique id per storage
            // don't know any API that can get the root path of that storage based on its id.
            //
            // so no "primary" type, but let the check here for other devices
            if ("primary".equals(type, ignoreCase = true)) {
                fullPath = Environment.getExternalStorageDirectory().toString() + relativePath
                if (fileExists(fullPath)) {
                    return fullPath
                }
            }

            // Environment.isExternalStorageRemovable() is `true` for external and internal storage
            // so we cannot relay on it.
            //
            // instead, for each possible path, check if file exists
            // we'll start with secondary storage as this could be our (physically) removable sd card
            fullPath = System.getenv("SECONDARY_STORAGE")?.toString() + relativePath
            if (fileExists(fullPath)) {
                return fullPath
            }
            fullPath = System.getenv("EXTERNAL_STORAGE")?.toString() + relativePath
            return if (fileExists(fullPath)) {
                fullPath
            } else fullPath
        }

        private fun getDriveFilePath(uri: Uri): String? {
            val returnUri: Uri = uri
            val returnCursor: Cursor = context.contentResolver.query(
                returnUri, null, null, null, null
            ) ?: return null
            /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
            val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name: String = returnCursor.getString(nameIndex)
            val size = returnCursor.getLong(sizeIndex).toString()
            val file = File(context.cacheDir, name)

            try {
                val inputStream: InputStream =
                    context.contentResolver.openInputStream(uri) ?: return null
                val outputStream = FileOutputStream(file)
                var read: Int
                val maxBufferSize = 1 * 1024 * 1024
                val bytesAvailable: Int = inputStream.available()

                //int bufferSize = 1024;
                val bufferSize = min(bytesAvailable, maxBufferSize)
                val buffers = ByteArray(bufferSize)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                Log.e("File Size", "Size " + file.length())
                inputStream.close()
                outputStream.close()
                Log.e("File Path", "Path " + file.path)
                Log.e("File Size", "Size " + file.length())
            } catch (e: java.lang.Exception) {
                Log.e("Exception", e.message ?: "")
            }
            returnCursor.close()
            return file.path
        }

        /***
         * Used for Android Q+
         * @param uri
         * @param newDirName if you want to create a directory, you can set this variable
         * @return
         */
        private fun copyFileToInternalStorage(
            uri: Uri,
            newDirName: String,
        ): String? {
            val returnUri: Uri = uri
            val returnCursor: Cursor = context.contentResolver.query(
                returnUri, arrayOf(
                    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
                ), null, null, null
            ) ?: return null

            /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
            val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name: String = returnCursor.getString(nameIndex)
            val size = returnCursor.getLong(sizeIndex).toString()

            val output = if (newDirName != "") {
                val dir = File(context.filesDir.toString() + "/" + newDirName)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                File(context.filesDir.toString() + "/" + newDirName + "/" + name)
            } else {
                File(context.filesDir.toString() + "/" + name)
            }

            try {
                val inputStream: InputStream =
                    context.contentResolver.openInputStream(uri) ?: return null
                val outputStream = FileOutputStream(output)
                var read: Int
                val bufferSize = 1024
                val buffers = ByteArray(bufferSize)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream.close()
                outputStream.close()
            } catch (e: java.lang.Exception) {
                Log.e("Exception", e.message ?: "")
            }
            returnCursor.close()
            return output.path
        }

        private fun getFilePathForWhatsApp(uri: Uri): String? {
            return copyFileToInternalStorage(uri, "whatsapp")
        }

        private fun getDataColumn(
            uri: Uri,
            selection: String,
            selectionArgs: Array<String>?,
        ): String? {
            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)
            try {
                cursor = context.contentResolver.query(
                    uri, projection, selection, selectionArgs, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val index: Int = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

        private fun isWhatsAppFile(uri: Uri): Boolean {
            return "com.whatsapp.provider.media" == uri.authority
        }

        private fun isGoogleDriveUri(uri: Uri): Boolean {
            return "com.google.android.apps.docs.storage" == uri.authority || "com.google.android.apps.docs.storage.legacy" == uri.authority
        }

        fun copyDbToDocuments(): Boolean {
            try {
                val dbFile = File(context.getDatabasePath(DATABASE_NAME).toString())

                //Open your local db as the input stream
                val myInput = FileInputStream(dbFile)

                // Path to the just created empty db
                val outDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!outDir.exists()) {
                    outDir.mkdir()
                }

                val outFile = File(outDir, DATABASE_NAME)
                if (outFile.exists()) {
                    outFile.delete()
                }

                outFile.createNewFile()

                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFile)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()

                return true
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, this::class.java.simpleName, "DB write failed: $e")
                return false
            }
        }

        /**
         * Copies your database from your local assets-folder to the just created empty database in the
         * system folder, from where it can be accessed and handled.
         * This is done by transfering bytestream.
         */
        @Throws(IOException::class)
        private fun copyDataBase() {
            //Open your local db as the input stream
            val myInput = context.assets.open(DATABASE_NAME)

            // Path to the just created empty db
            val outFileName = context.getDatabasePath(DATABASE_NAME).toString()

            try {
                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFileName)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                e.printStackTrace()
                ErrorLog.writeLog(null, this::class.java.simpleName, "DB write failed: $e")
            }
        }

        fun copyDataBase(inputDbFile: File?): Boolean {
            if (inputDbFile == null) return false

            Log.d(
                this::class.java.simpleName, context.getString(R.string.copying_database)
            )

            //Open your local db as the input stream
            val myInput = FileInputStream(inputDbFile)

            // Path to the just created empty db
            val outFileName = context.getDatabasePath(DATABASE_NAME).toString()

            val file = File(outFileName)
            if (file.exists()) {
                Log.d(this::class.java.simpleName, "Eliminando base de datos antigua: $outFileName")
                file.delete()
            }

            Log.d(
                this::class.java.simpleName,
                "${context.getString(R.string.origin)}: ${inputDbFile.absolutePath}"
            )
            Log.d(
                this::class.java.simpleName,
                "${context.getString(R.string.destination)}: $outFileName"
            )

            try {
                //Open the empty db as the output stream
                val myOutput = FileOutputStream(outFileName)

                //transfer bytes from the inputfile to the outputfile
                val buffer = ByteArray(1024)
                var length: Int
                while (run {
                        length = myInput.read(buffer)
                        length
                    } > 0) {
                    myOutput.write(buffer, 0, length)
                }

                //Close the streams
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                ErrorLog.writeLog(
                    null, this::class.java.simpleName, "${
                        context.getString(R.string.exception_error)
                    } (Copy database): ${e.message}"
                )
                return false
            }

            Log.d(this::class.java.simpleName, context.getString(R.string.copy_ok))

            return true
        }
    }
}