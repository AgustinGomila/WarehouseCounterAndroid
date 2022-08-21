package com.dacosys.warehouseCounter.sync

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*

class GetDatabaseLocation {
    interface TaskGetDatabaseLocationEnded {
        // Define data you like to return from AysncTask
        fun onTaskGetDatabaseLocationEnded(
            status: ProgressStatus,
            timeFileUrl: String,
            dbFileUrl: String,
            msg: String,
        )
    }

    var mCallback: TaskGetDatabaseLocationEnded? = null

    private val urlRequest = "${Statics.urlPanel}/webjson/database/location"
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    private var timeFileUrl: String = ""
    private var dbFileUrl: String = ""

    fun addParams(callback: TaskGetDatabaseLocationEnded) {
        this.mCallback = callback
    }

    private fun preExecute() {
        progressStatus = ProgressStatus.starting
    }

    private fun postExecute() {
        mCallback?.onTaskGetDatabaseLocationEnded(
            progressStatus,
            timeFileUrl,
            dbFileUrl,
            msg
        )
    }

    fun execute() {
        preExecute()
        doInBackground()
        postExecute()
    }

    private var deferred: Deferred<Boolean>? = null
    private fun doInBackground(): Boolean {
        var result = false
        runBlocking {
            deferred = async { suspendFunction() }
            result = deferred?.await() ?: false
        }
        return result
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        if (!Statics.isOnline()) {
            progressStatus = ProgressStatus.canceled
            msg = Statics.WarehouseCounter.getContext().getString(R.string.no_connection)
            return@withContext false
        }

        progressStatus = ProgressStatus.running
        return@withContext goForrest()
    }

    private fun goForrest(): Boolean {
        val url: URL
        var connection: HttpURLConnection? = null

        try {
            //Create connection
            url = URL(urlRequest)

            connection = if (Statics.useProxy) {
                val authenticator = object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            Statics.proxyUser,
                            Statics.proxyPass.toCharArray()
                        )
                    }
                }
                Authenticator.setDefault(authenticator)

                val proxy = Proxy(
                    Proxy.Type.HTTP, InetSocketAddress(
                        Statics.proxy,
                        Statics.proxyPort
                    )
                )
                url.openConnection(proxy) as HttpURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }

            connection.doOutput = true
            connection.doInput = true
            //connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            //connection.useCaches = false

            val wr = DataOutputStream(connection.outputStream)

            wr.flush()
            wr.close()

            getResponse(connection)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = ex.message.toString()
        } finally {
            connection?.disconnect()
        }

        return true
    }

    private fun getResponse(connection: HttpURLConnection): Boolean {
        //Get Response
        val inputStream = connection.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream))

        val response = StringBuilder()
        rd.forEachLine { l ->
            response.append(l)
            response.append('\r')
        }

        rd.close()

        try {
            val jsonObj = JSONObject(response.toString())
            println(jsonObj.toString())

            if (jsonObj.has("error")) {
                val error = jsonObj.getJSONObject("error")

                val code = error.getString("code")
                val name = error.getString("name")
                val description = error.getString("description")

                progressStatus = ProgressStatus.crashed
                msg = String.format("%s (%s): %s", name, code, description)
                return false
            }

            if (jsonObj.has("database")) {
                val jsonDb = jsonObj.getJSONObject("database")
                dbFileUrl = jsonDb.getString("db_file")
                timeFileUrl = jsonDb.getString("db_file_date")
            }

            progressStatus = ProgressStatus.finished
            msg = if (timeFileUrl.isEmpty() || dbFileUrl.isEmpty()) {
                Statics.WarehouseCounter.getContext().getString(R.string.success_response)
            } else {
                Statics.WarehouseCounter.getContext()
                    .getString(R.string.client_has_no_software_packages)
            }
        } catch (ex: JSONException) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
            }: ${ex.message}"
        }
        return progressStatus != ProgressStatus.crashed
    }
}