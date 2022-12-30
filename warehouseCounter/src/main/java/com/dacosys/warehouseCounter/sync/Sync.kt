package com.dacosys.warehouseCounter.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.orderRequest.`object`.OrderRequest
import com.dacosys.warehouseCounter.retrofit.functions.GetNewOrder
import com.dacosys.warehouseCounter.retrofit.functions.GetNewOrder.NewOrderListener
import com.dacosys.warehouseCounter.sync.GetCompletedOrderRequest.CompletedOrderRequestListener
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
    companion object : NewOrderListener, CompletedOrderRequestListener {
        override fun onNewOrderResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        ) {
            if (TASK_CODE == TASK_CODE_INTERNAL_NEW) {
                if (status == ProgressStatus.finished || status == ProgressStatus.success || status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    syncNewOrderRequestStatus = NOT_RUNNING
                }

                if (status == ProgressStatus.finished || status == ProgressStatus.success) {
                    Log.d(this::class.java.simpleName, msg)
                } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    Log.e(this::class.java.simpleName, msg)
                }

                try {
                    mNewOrderCallback!!.onNewOrderResult(status, itemArray, TASK_CODE_PARENT, msg)
                } catch (ex: java.lang.Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                }
            }
        }

        override fun onCompletedOrderRequestResult(
            status: ProgressStatus,
            itemArray: ArrayList<OrderRequest>,
            TASK_CODE: Int,
            msg: String,
        ) {
            if (TASK_CODE == TASK_CODE_INTERNAL_COMPLETED) {
                if (status == ProgressStatus.finished || status == ProgressStatus.success || status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    syncCompletedOrderRequestStatus = NOT_RUNNING
                }

                if (status == ProgressStatus.finished || status == ProgressStatus.success) {
                    Log.d(this::class.java.simpleName, msg)
                } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                    Log.e(this::class.java.simpleName, msg)
                }

                try {
                    mCompletedOrderCallback!!.onCompletedOrderRequestResult(status,
                        itemArray,
                        TASK_CODE_PARENT,
                        msg)
                } catch (ex: java.lang.Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                }
            }
        }

        override fun hashCode(): Int {
            var result = listener.hashCode()
            result = 31 * result + TASK_CODE_PARENT
            return result
        }

        private lateinit var listener: Any

        // Id de tareas internas de la clase
        private var TASK_CODE_PARENT: Int = 0
        private var TASK_CODE_INTERNAL_NEW: Int = 0
        private var TASK_CODE_INTERNAL_COMPLETED: Int = 0

        // Thread status
        private var syncNewOrderRequestStatus: Int = 0
        private var syncCompletedOrderRequestStatus: Int = 0

        private const val NOT_RUNNING = 0
        private const val RUNNING = 1

        private var timer: Timer? = null
        private var timerTask: TimerTask? = null
        private val handler = Handler(Looper.getMainLooper())

        // Callbacks
        private var mNewOrderCallback: NewOrderListener? = null
        private var mCompletedOrderCallback: CompletedOrderRequestListener? = null

        //To stop timer
        fun stopTimer() {
            Log.d(this::class.java.simpleName, "Deteniendo sincronizador de órdenes")
            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()

                mNewOrderCallback = null
                mCompletedOrderCallback = null
                TASK_CODE_PARENT = 0
            }
        }

        //To start timer
        fun startTimer(
            newListener: NewOrderListener,
            completedListener: CompletedOrderRequestListener,
            TASK_CODE: Int,
        ) {
            Log.d(this::class.java.simpleName, "Iniciando sincronizador de órdenes...")
            timer = Timer()

            mNewOrderCallback = newListener
            mCompletedOrderCallback = completedListener
            TASK_CODE_PARENT = TASK_CODE

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
            if (syncNewOrderRequestStatus == NOT_RUNNING) {
                syncNewOrderRequestStatus = RUNNING

                TASK_CODE_INTERNAL_NEW = Statics.generateTaskCode()
                try {
                    thread {
                        val task = GetNewOrder()
                        task.addParams(this, TASK_CODE_INTERNAL_NEW)
                        task.execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(null,
                        this::class.java.simpleName,
                        "$TASK_CODE_PARENT: ${ex.message.toString()}")
                    syncNewOrderRequestStatus = NOT_RUNNING
                }
            }
        }

        private fun getCompletedOrderRequest() {
            if (syncCompletedOrderRequestStatus == NOT_RUNNING) {
                syncCompletedOrderRequestStatus = RUNNING

                TASK_CODE_INTERNAL_COMPLETED = Statics.generateTaskCode()
                try {
                    thread {
                        val task = GetCompletedOrderRequest()
                        task.addParams(this, TASK_CODE_INTERNAL_COMPLETED)
                        task.execute()
                    }
                } catch (ex: Exception) {
                    ErrorLog.writeLog(null,
                        this::class.java.simpleName,
                        "$TASK_CODE_PARENT: ${ex.message.toString()}")
                    syncCompletedOrderRequestStatus = NOT_RUNNING
                }
            }
        }
    }
}