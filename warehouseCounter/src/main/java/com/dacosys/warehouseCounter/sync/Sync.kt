package com.dacosys.warehouseCounter.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.ktor.functions.GetNewOrder
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
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
    companion object {
        // Thread status
        @get:Synchronized
        private var syncNewOrderStatus: DownloadStatus = DownloadStatus.NOT_RUNNING

        @get:Synchronized
        private var syncCompletedOrderStatus: DownloadStatus = DownloadStatus.NOT_RUNNING

        enum class DownloadStatus(val id: Int) { NOT_RUNNING(1), RUNNING(2) }

        private var timer: Timer? = null
        private var timerTask: TimerTask? = null
        private val handler = Handler(Looper.getMainLooper())

        @get:Synchronized
        private var ticks = 0

        //To stop timer
        fun stopSync() {
            Log.d(this::class.java.simpleName, "Deteniendo sincronizador de órdenes")
            if (timer != null) {
                timer?.cancel()
                timer?.purge()
            }
        }

        //To start timer
        fun startSync(
            onNewOrders: (ArrayList<OrderRequest>) -> Unit = {},
            onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
            onTimerTick: (Int) -> Unit = {},
        ) {
            Log.d(this::class.java.simpleName, "Iniciando sincronizador de órdenes...")

            timer = Timer()
            val interval = settingViewModel.wcSyncInterval

            timerTask = object : TimerTask() {
                override fun run() {
                    ticks++
                    onTimerTick(ticks)

                    if (ticks < interval.toLong()) return
                    goSync(onNewOrders, onCompletedOrders)
                }

                override fun cancel(): Boolean {
                    stopSync()
                    return super.cancel()
                }
            }

            // Primera ejecución
            if (ticks == 0) goSync(onNewOrders, onCompletedOrders)

            timer?.scheduleAtFixedRate(
                timerTask, 100, 1000
            )
        }

        private fun goSync(
            onNewOrders: (ArrayList<OrderRequest>) -> Unit = {},
            onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
        ) {
            ticks = 0

            handler.post {
                getNewOrderRequest(onNewOrders)
                getCompletedOrderRequest(onCompletedOrders)
            }
        }
        // endregion

        private fun getNewOrderRequest(onNewOrders: (ArrayList<OrderRequest>) -> Unit = {}) {
            if (syncNewOrderStatus != DownloadStatus.NOT_RUNNING) return
            syncNewOrderStatus = DownloadStatus.RUNNING

            thread {
                try {
                    GetNewOrder(onEvent = { }, onFinish = {
                        if (!it.any()) return@GetNewOrder
                        onNewOrders.invoke(it)
                    }).execute()
                } catch (ex: Exception) {
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex.message.toString())
                    syncNewOrderStatus = DownloadStatus.NOT_RUNNING
                }
            }
        }

        private fun getCompletedOrderRequest(
            onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
        ) {
            if (syncCompletedOrderStatus != DownloadStatus.NOT_RUNNING) return
            syncCompletedOrderStatus = DownloadStatus.RUNNING

            thread {
                try {
                    onCompletedOrders.invoke(getCompletedOrders())
                } catch (ex: java.lang.Exception) {
                    ErrorLog.writeLog(
                        null, this::class.java.simpleName, ex.message.toString()
                    )
                } finally {
                    syncCompletedOrderStatus = DownloadStatus.NOT_RUNNING
                }
            }
        }
    }
}