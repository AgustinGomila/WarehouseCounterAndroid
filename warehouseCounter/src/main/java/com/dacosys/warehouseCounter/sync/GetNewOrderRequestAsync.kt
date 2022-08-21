package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequest
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*


class GetNewOrderRequest {
    interface NewOrderRequestListener {
        // Define data you like to return from AysncTask
        fun onNewOrderRequestResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        )
    }

    // region VARIABLES de comunicación entre procesos
    private var mCallback: NewOrderRequestListener? = null
    private var taskCode: Int = -1
    // endregion

    private val urlRequest = "${Statics.urlPanel}/webjson/order/get"
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    // region VARIABLES para resultados de la consulta
    private var itemArray = ArrayList<OrderRequest>()
    // endregion

    fun addParams(
        listener: NewOrderRequestListener,
        TASK_CODE: Int,
    ) {
        this.mCallback = listener
        this.taskCode = TASK_CODE
    }

    private fun preExecute() {
        progressStatus = ProgressStatus.starting
    }

    private fun postExecute() {
        mCallback?.onNewOrderRequestResult(
            progressStatus,
            itemArray,
            taskCode,
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
            msg = "${
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
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
                Statics.WarehouseCounter.getContext().getString(R.string.ok)
            } else {
                Statics.WarehouseCounter.getContext().getString(R.string.no_new_counts)
            }
        } catch (ex: JSONException) {
            Log.e(this::class.java.simpleName, ex.toString())

            progressStatus = ProgressStatus.crashed
            msg =
                "${
                    Statics.WarehouseCounter.getContext()
                        .getString(R.string.an_error_occurred_while_requesting_new_counts)
                }: ${ex.message}"
        }
        return progressStatus != ProgressStatus.crashed
    }
}