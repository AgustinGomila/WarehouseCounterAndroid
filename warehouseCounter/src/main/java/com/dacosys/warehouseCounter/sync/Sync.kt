package com.dacosys.warehouseCounter.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.network.GetNewOrder
import java.util.*
import kotlin.concurrent.thread

/**
 * Esta clase se encarga de ejecutar cada cierto período
 * de tiempo dos procesos:
 * 1. Solicitar nuevas órdenes de arqueo (en la web)
 * 2. Solicitar los arqueos completados (en el dispositivo)
 *
 * Cada tarea es independiente y devuelve su resultado en cuanto finaliza
 * a quien esté escuchando (callbacks).
 *
 * Se inicializa con starTimer:
 *  Requiere un context (para comunicarse con la UI, etc),
 *  2 Callbacks (para las entradas y salidas) y
 *  el TASK_CODE (para el callback).
 *
 * Se puede detener mediante stopTimer.
 */
class Sync {
    companion object : GetNewOrder.NewOrderListener {
        override fun onNewOrderResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        ) {
            if (TASK_CODE == taskCodeInternalNew) {
                if (status == ProgressStatus.finished || status == ProgressStatus.success || status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    syncNewOrderStatus = NOT_RUNNING
                }

                if (status == ProgressStatus.finished || status == ProgressStatus.success) {
                    Log.d(this::class.java.simpleName, msg)
                } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    Log.e(this::class.java.simpleName, msg)
                }

                try {
                    mNewOrderCallback!!.onNewOrderResult(status, itemArray, taskCodeParent, msg)
                } catch (ex: java.lang.Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                }
            }
        }

        override fun hashCode(): Int {
            var result = listener.hashCode()
            result = 31 * result + taskCodeParent
            return result
        }

        private lateinit var listener: Any

        // Id de tareas internas de la clase
        private var taskCodeParent: Int = 0
        private var taskCodeInternalNew: Int = 0

        // Thread status
        private var syncNewOrderStatus: Int = 0
        private var syncCompletedOrderStatus: Int = 0

        private const val NOT_RUNNING = 0
        private const val RUNNING = 1

        private var timer: Timer? = null
        private var timerTask: TimerTask? = null
        private val handler = Handler(Looper.getMainLooper())

        // Callbacks
        private var mNewOrderCallback: GetNewOrder.NewOrderListener? = null
        private var mCompletedOrderCallback: CompletedOrderListener? = null

        //To stop timer
        fun stopTimer() {
            Log.d(this::class.java.simpleName, "Deteniendo sincronizador de órdenes")
            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()

                mNewOrderCallback = null
                mCompletedOrderCallback = null
                taskCodeParent = 0
            }
        }

        //To start timer
        fun startTimer(
            newListener: GetNewOrder.NewOrderListener,
            completedListener: CompletedOrderListener,
            TASK_CODE: Int,
        ) {
            Log.d(this::class.java.simpleName, "Iniciando sincronizador de órdenes...")
            timer = Timer()

            mNewOrderCallback = newListener
            mCompletedOrderCallback = completedListener
            taskCodeParent = TASK_CODE

            val t = settingViewModel().wcSyncInterval
            timerTask = object : TimerTask() {
                override fun run() {
                    handler.post {
                        getNewOrderRequest()
                        getCompletedOrderRequest()
                    }
                }
            }
            timer!!.schedule(timerTask, 0, t.toLong() * 1000)
        }
        // endregion

        private fun getNewOrderRequest() {
            if (syncNewOrderStatus != NOT_RUNNING) return
            syncNewOrderStatus = RUNNING

            taskCodeInternalNew = Statics.generateTaskCode()
            try {
                thread {
                    val task = GetNewOrder()
                    task.addParams(this, taskCodeInternalNew)
                    task.execute()
                }
            } catch (ex: Exception) {
                ErrorLog.writeLog(
                    null, this::class.java.simpleName, "$taskCodeParent: ${ex.message.toString()}"
                )
                syncNewOrderStatus = NOT_RUNNING
            }
        }

        private fun getCompletedOrderRequest() {
            if (syncCompletedOrderStatus != NOT_RUNNING) return
            syncCompletedOrderStatus = RUNNING

            try {
                thread {
                    try {
                        mCompletedOrderCallback!!.onCompletedOrderRequestResult(
                            ProgressStatus.success, getCompletedOrders(), taskCodeParent, "Ok"
                        )
                    } catch (ex: java.lang.Exception) {
                        ErrorLog.writeLog(
                            null, this::class.java.simpleName, ex.message.toString()
                        )
                    } finally {
                        syncCompletedOrderStatus = NOT_RUNNING
                    }
                }
            } catch (ex: Exception) {
                ErrorLog.writeLog(
                    null, this::class.java.simpleName, "$taskCodeParent: ${ex.message.toString()}"
                )
                syncCompletedOrderStatus = NOT_RUNNING
            }
        }
    }
}