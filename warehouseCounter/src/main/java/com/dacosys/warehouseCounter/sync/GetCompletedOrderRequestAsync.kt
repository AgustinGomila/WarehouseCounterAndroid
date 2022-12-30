package com.dacosys.warehouseCounter.sync

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequest
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequest.CREATOR.getCompletedOrderRequests
import kotlinx.coroutines.*
import java.sql.SQLException

class GetCompletedOrderRequest {
    interface CompletedOrderRequestListener {
        // Define data you like to return from AysncTask
        fun onCompletedOrderRequestResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        )
    }

    // region VARIABLES de comunicación entre procesos
    private var mCallback: CompletedOrderRequestListener? = null
    private var taskCode: Int = -1
    // endregion

    // region VARIABLES para resultados de la consulta
    private var itemArray = ArrayList<OrderRequest>()
    // endregion

    fun addParams(
        listener: CompletedOrderRequestListener,
        TASK_CODE: Int,
    ) {
        this.mCallback = listener
        this.taskCode = TASK_CODE
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch { doInBackground() }
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
        try {
            Log.d(this::class.java.simpleName, "Obteniendo órdenes completadas...")

            mCallback?.onCompletedOrderRequestResult(ProgressStatus.finished,
                getCompletedOrderRequests(),
                taskCode,
                context().getString(R.string.ok))
            return@withContext true
        } catch (ex: SQLException) {
            ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
            mCallback?.onCompletedOrderRequestResult(ProgressStatus.crashed,
                itemArray,
                taskCode,
                ex.toString())
            return@withContext false
        }
    }
}