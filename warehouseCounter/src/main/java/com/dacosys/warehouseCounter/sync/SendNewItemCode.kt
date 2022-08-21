package com.dacosys.warehouseCounter.sync

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode
import com.dacosys.warehouseCounter.itemCode.dbHelper.ItemCodeDbHelper
import com.dacosys.warehouseCounter.user.`object`.User
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.*

class SendNewItemCode {
    interface TaskSendItemCodeEnded {
        // Define data you like to return from AysncTask
        fun onTaskSendItemCodeEnded(
            status: ProgressStatus,
            msg: String,
        )
    }

    var mCallback: TaskSendItemCodeEnded? = null

    private val urlRequest = Statics.urlPanel + "/webjson/item-code/send"
    private var itemCodeArray: ArrayList<ItemCode> = ArrayList()
    private var itemCodeSuccess: ArrayList<ItemCode> = ArrayList()

    private var currentUser: User? = null
    private var progressStatus = ProgressStatus.unknown
    private var msg = ""

    fun addParams(
        listener: TaskSendItemCodeEnded,
        itemCodeArray: ArrayList<ItemCode>,
    ) {
        this.mCallback = listener
        this.itemCodeArray = itemCodeArray
    }

    private fun preExecute() {
        progressStatus = ProgressStatus.starting
        currentUser = Statics.getCurrentUser()
    }

    private fun postExecute(result: Boolean) {
        var isOk = true
        if (result) {
            val icDbHelper = ItemCodeDbHelper()

            for (i in itemCodeSuccess) {
                if (!icDbHelper.updateTransferred(i.itemId, i.code)) {
                    isOk = false
                    break
                }
            }
        }

        if (!isOk) {
            mCallback?.onTaskSendItemCodeEnded(
                ProgressStatus.crashed,
                Statics.WarehouseCounter.getContext()
                    .getString(R.string.an_error_occurred_while_updating_item_codes)
            )
        } else {
            mCallback?.onTaskSendItemCodeEnded(progressStatus, msg)
        }
    }

    fun execute() {
        preExecute()
        val it = doInBackground()
        postExecute(it)
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

            // USER DATA //////////////////
            val userAuthData = JSONObject()
            userAuthData
                .put("username", currentUser!!.name)
                .put("password", currentUser!!.password)

            val jsonParam = JSONObject()
            jsonParam.put("userauthdata", userAuthData)
            // END USER DATA //////////////

            // COLLECTOR DATA //////////////////
            val collectorData = Statics.getDeviceData()
            jsonParam.put("collectorData", collectorData)
            // END COLLECTOR DATA //////////////

            // Todos los ItemCodes ////////
            val icArrayJson = JSONObject()
            for ((index, itemCode) in itemCodeArray.withIndex()) {
                val icJson = ItemCode.toJson(itemCode)
                val jsonObj = JSONObject(icJson)
                icArrayJson.put("itemCode$index", jsonObj)
                itemCodeSuccess.add(itemCode)
            }

            jsonParam.put("itemCodes", icArrayJson)
            // Fin Todos los ItemCodes ///

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
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
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
            msg = Statics.WarehouseCounter.getContext().getString(R.string.ok)
        } catch (ex: Exception) {
            progressStatus = ProgressStatus.crashed
            msg = "${
                Statics.WarehouseCounter.getContext().getString(R.string.exception_error)
            }: ${ex.message}"
        }

        return progressStatus != ProgressStatus.crashed
    }
}