package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlContent
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.ktor.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.network.RequestResult
import com.dacosys.warehouseCounter.network.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.concurrent.thread

class GetPtlOrderContent(
    private val orderId: Long,
    private val warehouseAreaId: Long,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<PtlContent>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<PtlContent> = ArrayList()

    fun execute() {
        // Función que prosigue al resultado de GetToken
        fun onEvent(it: RequestResult) {
            if (it.status == ResultStatus.SUCCESS) {

                // Proseguimos con la búsqueda...
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
                // Token inválido.
                sendEvent(context.getString(R.string.invalid_or_expired_token), SnackBarType.ERROR)
            }
        }

        if (!validUrl()) {
            sendEvent(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            return
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        ktorApiService.getPtlOrderContent(body = getBody(), callback = {
            r = ArrayList(it.contents)
            if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(it.details, SnackBarType.INFO)
        })
    }

    private fun getBody(): JSONObject {
        // BODY ////////////////////////////
        val ptlQuery = JSONObject()
        ptlQuery.put("orderId", orderId).put("warehouseAreaId", warehouseAreaId)

        // Token DATA //////////////////
        val jsonParam = JSONObject()
        jsonParam.put("userToken", Token.token).put("ptlQuery", ptlQuery)

        return jsonParam
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}