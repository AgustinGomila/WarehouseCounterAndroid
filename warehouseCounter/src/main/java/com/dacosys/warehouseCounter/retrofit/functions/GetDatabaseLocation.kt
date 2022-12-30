package com.dacosys.warehouseCounter.retrofit.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*
import javax.net.ssl.HttpsURLConnection

class GetDatabaseLocation {
    interface DatabaseLocationEnded {
        // Define data you like to return from AysncTask
        fun onDatabaseLocationEnded(
            status: ProgressStatus,
            timeFileUrl: String,
            dbFileUrl: String,
            msg: String,
        )
    }

    var mCallback: DatabaseLocationEnded? = null

    private val api = "api"
    private val urlPanel = "${settingViewModel().urlPanel}/$api"
    private val urlRequest = "${urlPanel}/database/location"
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    private var timeFileUrl: String = ""
    private var dbFileUrl: String = ""

    fun addParams(callback: DatabaseLocationEnded) {
        this.mCallback = callback
    }

    private fun postExecute() {
        mCallback?.onDatabaseLocationEnded(status = progressStatus,
            timeFileUrl = timeFileUrl,
            dbFileUrl = dbFileUrl,
            msg = msg)
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        progressStatus = ProgressStatus.starting

        scope.launch {
            doInBackground()
            postExecute()
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
            progressStatus = ProgressStatus.canceled
            msg = context().getString(R.string.no_connection)
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
            val sv = settingViewModel()
            connection = if (sv.useProxy) {
                val authenticator = object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(sv.proxyUser, sv.proxyPass.toCharArray())
                    }
                }
                Authenticator.setDefault(authenticator)

                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(sv.proxy, sv.proxyPort))
                url.openConnection(proxy) as HttpsURLConnection
            } else {
                url.openConnection() as HttpsURLConnection
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
                context().getString(R.string.success_response)
            } else {
                context().getString(R.string.client_has_no_software_packages)
            }
        } catch (ex: JSONException) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                context().getString(R.string.exception_error)
            }: ${ex.message}"
        }
        return progressStatus != ProgressStatus.crashed
    }
}