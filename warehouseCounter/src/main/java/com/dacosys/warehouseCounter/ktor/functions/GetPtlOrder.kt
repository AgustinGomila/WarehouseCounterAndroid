package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.dto.ptlOrder.PtlOrder
import com.dacosys.warehouseCounter.ktor.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.network.result.RequestResult
import com.dacosys.warehouseCounter.network.result.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread

class GetPtlOrder(
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<PtlOrder>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<PtlOrder> = ArrayList()

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
        ktorApiService.getPtlOrder(body = getBody(), callback = {
            r = it
            if (it.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
        })
    }

    private fun validUrl(): Boolean {
        val url = URL(settingRepository.urlPanel.value.toString())
        return url.protocol.isNotEmpty() && url.host.isNotEmpty()
    }

    private fun getBody(): JSONObject {
        // Token DATA //////////////////
        val jsonParam = JSONObject()
        jsonParam.put("userToken", Token.token)
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