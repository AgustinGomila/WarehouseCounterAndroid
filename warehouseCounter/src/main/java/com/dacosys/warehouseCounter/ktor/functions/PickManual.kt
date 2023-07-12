package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.apiParam.ApiParam
import com.dacosys.warehouseCounter.dto.apiParam.PtlQuery
import com.dacosys.warehouseCounter.dto.ptlOrder.PickItem
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.ktor.functions.GetToken.Companion.Token
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.network.RequestResult
import com.dacosys.warehouseCounter.network.ResultStatus
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import kotlin.concurrent.thread

class PickManual(
    private val orderId: Long,
    private val warehouseAreaId: Long,
    private val itemId: Long,
    private val qty: Int,
    private val onFinish: (ArrayList<PickItem>) -> Unit,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var currentUser: User

    var r: ArrayList<PickItem> = ArrayList()

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

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Chequeamos que el Token sea válido
                thread { GetToken { onEvent(it) }.execute(false) }
            } else {
                // Token inválido.
                sendEvent(context.getString(R.string.invalid_user), SnackBarType.ERROR)
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val body = ApiParam(
            userToken = Token.token,
            ptlQuery = PtlQuery(orderId = orderId, warehouseAreaId = warehouseAreaId, itemId = itemId, qty = qty)
        )
        ktorApiService.pickManual(body = body, callback = {
            it.contents.forEach { s -> r.add(s) }
            if (r.any()) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
            else sendEvent(it.details, SnackBarType.INFO)
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