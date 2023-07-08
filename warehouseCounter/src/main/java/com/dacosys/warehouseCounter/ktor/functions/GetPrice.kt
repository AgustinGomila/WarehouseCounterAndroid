package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.price.Price
import com.dacosys.warehouseCounter.dto.search.SearchObject
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.network.RequestResult
import com.dacosys.warehouseCounter.network.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class GetPrice(
    private val searchObject: SearchObject,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Price>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: ArrayList<Price> = ArrayList()

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
        ktorApiService.getPrices(body = searchObject, callback = {
            r = it
            if (it.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
        })
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        val event = SnackBarEventData(msg, type)
        sendEvent(event)
    }

    private fun sendEvent(event: SnackBarEventData) {
        onEvent.invoke(event)
        if (event.snackBarType in SnackBarType.getFinish() || event.snackBarType == SnackBarType.INFO) {
            onFinish.invoke(r)
        }
    }
}