package com.dacosys.warehouseCounter.retrofit.functionOld

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*
import javax.net.ssl.HttpsURLConnection


class GetNewOrder {
    interface NewOrderListener {
        // Define data you like to return from AysncTask
        fun onNewOrderResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        )
    }

    // region VARIABLES de comunicación entre procesos
    private var mCallback: NewOrderListener? = null
    private var taskCode: Int = -1
    // endregion

    private val api = "api"
    private val urlPanel = "${settingViewModel().urlPanel}/$api"
    private val urlRequest = "${urlPanel}/order/get"
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    // region VARIABLES para resultados de la consulta
    private var itemArray = ArrayList<OrderRequest>()
    // endregion

    fun addParams(
        listener: NewOrderListener,
        TASK_CODE: Int,
    ) {
        this.mCallback = listener
        this.taskCode = TASK_CODE
    }

    private fun postExecute() {
        mCallback?.onNewOrderResult(
            status = progressStatus,
            itemArray = itemArray,
            TASK_CODE = taskCode,
            msg = msg
        )
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

        Log.d(this::class.java.simpleName, "Solicitando nuevas órdenes...")
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

            val wr = DataOutputStream(connection.outputStream)

            wr.flush()
            wr.close()

            getResponse(connection)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                context().getString(R.string.exception_error)
            }: ${ex.message}"
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

            if (jsonObj.has("orders")) {
                val orderJsonArray = jsonObj.getJSONArray("orders")
                for (i in 0 until orderJsonArray.length()) {
                    val obj = orderJsonArray.getJSONObject(i)
                    val or = OrderRequest.fromJson(obj.toString())
                    if (or != null) {
                        itemArray.add(or)
                    }
                }
            }

            progressStatus = ProgressStatus.finished
            msg = if (itemArray.isNotEmpty()) {
                context().getString(R.string.ok)
            } else {
                context().getString(R.string.no_new_counts)
            }
        } catch (ex: JSONException) {
            Log.e(this::class.java.simpleName, ex.toString())

            progressStatus = ProgressStatus.crashed
            msg = "${
                context().getString(R.string.an_error_occurred_while_requesting_new_counts)
            }: ${ex.message}"
        }
        return progressStatus != ProgressStatus.crashed
    }
}