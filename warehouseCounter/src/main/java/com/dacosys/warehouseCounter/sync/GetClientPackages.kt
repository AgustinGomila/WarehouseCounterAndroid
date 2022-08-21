package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*
import javax.net.ssl.HttpsURLConnection

class GetClientPackages {
    interface TaskGetPackagesEnded {
        // Define data you like to return from AysncTask
        fun onTaskGetPackagesEnded(
            status: ProgressStatus,
            result: ArrayList<JSONObject>,
            clientEmail: String,
            clientPassword: String,
            msg: String,
        )
    }

    var mCallback: TaskGetPackagesEnded? = null

    private val urlRequest = "https://config.dacosys.com/configuration/retrieve"

    private var email = ""
    private var password = ""
    private var installationCode = ""

    private var progressStatus = ProgressStatus.unknown
    private var jsonObjArray: ArrayList<JSONObject> = ArrayList()
    private var msg = ""

    fun addParams(
        callback: TaskGetPackagesEnded,
        email: String,
        password: String,
        installationCode: String,
    ) {
        this.mCallback = callback
        this.email = email
        this.password = password
        this.installationCode = installationCode
    }

    private fun preExecute() {
        progressStatus = ProgressStatus.starting
    }

    private fun postExecute(result: Boolean): Boolean {
        mCallback?.onTaskGetPackagesEnded(
            progressStatus,
            jsonObjArray,
            email,
            password,
            msg
        )
        return result
    }

    fun execute(): Boolean {
        preExecute()
        val result = doInBackground()
        return postExecute(result)
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
        return@withContext getPackages()
    }

    private fun getPackages(): Boolean {
        progressStatus = ProgressStatus.running

        val url: URL
        var connection: HttpsURLConnection? = null

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

            val authDataCont = JSONObject()
            authDataCont
                .put("version", "1")
                .put("email", email)
                .put("password", password)

            val jsonParam = JSONObject()
            jsonParam.put("authdata", authDataCont)

            // Mostrar el resultado en logcat
            val utf8JsonString = jsonParam.toString().toByteArray(charset("UTF8"))
            println(utf8JsonString.toString())

            val wr = DataOutputStream(connection.outputStream)
            wr.write(utf8JsonString, 0, utf8JsonString.size)

            wr.flush()
            wr.close()

            jsonObjArray.clear()
            jsonObjArray = getResponse(connection)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
            }: ${ex.message}"
            return false
        } finally {
            connection?.disconnect()
        }
        return true
    }

    private fun getResponse(connection: HttpsURLConnection): ArrayList<JSONObject> {
        //Get Response
        val result: ArrayList<JSONObject> = ArrayList()
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

            if (jsonObj.has("error")) {
                val error = jsonObj.getJSONObject("error")

                val code = error.getString("code")
                val name = error.getString("name")
                val description = error.getString("description")

                progressStatus = ProgressStatus.crashed
                msg = String.format("%s (%s): %s", name, code, description)
                return ArrayList()
            }

            if (jsonObj.has("packages")) {
                val jsonPackages = jsonObj.getJSONObject("packages")

                for (k in jsonPackages.keys()) {
                    val jsonPack = jsonPackages.getJSONObject(k)
                    if (installationCode.trim().isNotEmpty()) {
                        val tempInstallationCode = jsonPack.getString("installation_code")
                        if (tempInstallationCode != installationCode) {
                            continue
                        }
                    }

                    val productId = jsonPack.getInt("product_version_id")
                    if (jsonPack.getInt("active") == 1 &&
                        (productId == Statics.APP_VERSION_ID || productId == Statics.APP_VERSION_ID_IMAGECONTROL)
                    ) {
                        result.add(jsonPack)
                    }
                }
            }
            progressStatus = ProgressStatus.finished
            msg = if (result.size > 0) {
                Statics.WarehouseCounter.getContext().getString(R.string.success_response)
            } else {
                Statics.WarehouseCounter.getContext()
                    .getString(R.string.client_has_no_software_packages)
            }
        } catch (ex: JSONException) {
            Log.e(this::class.java.simpleName, ex.toString())

            progressStatus = ProgressStatus.crashed
            msg = "${
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
            }: ${ex.message}"
        }

        return result
    }
}
