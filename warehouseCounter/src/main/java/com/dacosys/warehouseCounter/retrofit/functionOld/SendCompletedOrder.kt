package com.dacosys.warehouseCounter.retrofit.functionOld

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.getDeviceData
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.model.user.User
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.*
import javax.net.ssl.HttpsURLConnection


class SendCompletedOrder {
    interface TaskSendOrderRequestEnded {
        // Define data you like to return from AysncTask
        fun onTaskSendOrderRequestEnded(
            status: ProgressStatus,
            msg: String,
        )
    }

    var mCallback: TaskSendOrderRequestEnded? = null

    private val api = "api"
    private val urlPanel = "${settingViewModel().urlPanel}/$api"
    private val urlRequest = "${urlPanel}/order/send"
    private var orderRequestArray: ArrayList<OrderRequest> = ArrayList()
    private var filesSuccess: ArrayList<String> = ArrayList()

    private var currentUser: User? = null
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    fun addParams(
        callback: TaskSendOrderRequestEnded,
        orderRequestArray: ArrayList<OrderRequest>,
    ) {
        this.mCallback = callback
        this.orderRequestArray = orderRequestArray
    }

    private fun postExecute(result: Boolean) {
        var isOk = true
        if (result) {
            val currentDir = Statics.getCompletedPath()
            for (f in filesSuccess) {
                val filePath = currentDir.absolutePath + File.separator + f
                val fl = File(filePath)
                if (!fl.delete()) {
                    isOk = false
                    break
                }
            }
        }

        if (!isOk) {
            mCallback?.onTaskSendOrderRequestEnded(
                status = ProgressStatus.crashed,
                msg = context().getString(R.string.an_error_occurred_while_deleting_counts)
            )
        } else {
            mCallback?.onTaskSendOrderRequestEnded(status = progressStatus, msg = msg)
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        progressStatus = ProgressStatus.starting
        currentUser = Statics.getCurrentUser()

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
        progressStatus = ProgressStatus.running
        return@withContext goForrest()
    }

    private fun goForrest(): Boolean {
        var isOk: Boolean
        val url: URL
        var connection: HttpURLConnection? = null

        try {
            //Create connection
            url = URL(urlRequest)
            val sv = settingViewModel()
            connection = if (sv.useProxy) {
                val authenticator = object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            sv.proxyUser,
                            sv.proxyPass.toCharArray()
                        )
                    }
                }
                Authenticator.setDefault(authenticator)

                val proxy =
                    Proxy(Proxy.Type.HTTP, InetSocketAddress(sv.proxy, sv.proxyPort))
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

            val jsonParam = JSONObject()

            // USER DATA //////////////////
            val userAuthData = JSONObject()
            userAuthData.put("username", currentUser!!.name).put("password", currentUser!!.password)
            jsonParam.put("userauthdata", userAuthData)
            // END USER DATA //////////////

            // COLLECTOR DATA //////////////////
            val collectorData = getDeviceData()
            jsonParam.put("collectorData", collectorData)
            // END COLLECTOR DATA //////////////

            // Todas las órdenes
            val orArrayJson = JSONObject()
            for ((index, or) in orderRequestArray.withIndex()) {
                val orJson = OrderRequest.toJson(or)
                orArrayJson.put("order$index", JSONObject(orJson))
                filesSuccess.add(or.filename)
            }
            jsonParam.put("orders", orArrayJson)
            // Fin Todas las órdenes

            val utf8JsonString = jsonParam.toString().toByteArray(charset("UTF8"))
            println(jsonParam.toString())

            val wr = DataOutputStream(connection.outputStream)
            wr.write(utf8JsonString, 0, utf8JsonString.size)

            wr.flush()
            wr.close()

            isOk = getResponse(connection)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                context().getString(R.string.exception_error)
            }: ${ex.message}"
            isOk = false
        } finally {
            connection?.disconnect()
        }

        return isOk
    }

    private fun getResponse(connection: HttpURLConnection): Boolean {
        //Get Response
        val inputStream = connection.inputStream
        val rd = BufferedReader(InputStreamReader(inputStream))

        val response = StringBuilder()
        rd.forEachLine { l ->
            response.append(l)
            response.append(System.getProperty("line.separator"))
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

            progressStatus = ProgressStatus.finished
            msg = context().getString(R.string.ok)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                context().getString(R.string.exception_error)
            }: ${ex.message}"
        }

        return progressStatus != ProgressStatus.crashed
    }
}