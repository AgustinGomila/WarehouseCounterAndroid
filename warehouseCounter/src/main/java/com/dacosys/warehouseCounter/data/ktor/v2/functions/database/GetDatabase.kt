package com.dacosys.warehouseCounter.data.ktor.v2.functions.database

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV2
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.data.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.data.room.database.WcDatabase
import com.dacosys.warehouseCounter.misc.utils.NetworkState
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.getFinish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GetDatabase
/**
 * Get a [DatabaseData] for the current user.
 *
 * @property onEvent Event to update the state of the UI according to the progress of the operation.
 * @property onFinish If the operation is successful it returns a [DatabaseData] else null.
 */(
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (DatabaseData?) -> Unit,
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var r: DatabaseData? = null

    fun execute() {
        if (!NetworkState.isOnline()) {
            sendEvent(context.getString(R.string.connection_error), SnackBarType.ERROR)
            return
        }
        sendEvent(context.getString(R.string.searching_database), SnackBarType.RUNNING)
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }

    fun cancel() {
        if (scope.isActive) scope.cancel()
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        var version = ""
        @Suppress("KotlinConstantConditions") if (WcDatabase.DATABASE_VERSION > 1) {
            version = "-v${WcDatabase.DATABASE_VERSION}"
        }

        apiServiceV2.getDatabase(
            version = version,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                else {
                    if (r != null) sendEvent(context.getString(R.string.ok), SnackBarType.SUCCESS)
                    else sendEvent(context.getString(R.string.no_results), SnackBarType.INFO)
                }
            })
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
