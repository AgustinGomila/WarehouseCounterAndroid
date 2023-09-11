package com.dacosys.warehouseCounter.data.ktor.v2.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import java.util.*
import kotlin.concurrent.thread

/**
 * Esta clase se encarga de ejecutar cada cierto período de tiempo dos procesos:
 * 1. Solicitar nuevas órdenes de arqueo (en la web)
 * 2. Solicitar los arqueos completados (en el dispositivo)
 */
class Sync private constructor(builder: Builder) {
    private var onNewOrders: (ArrayList<OrderRequest>) -> Unit = {}
    private var onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {}
    private var onTimerTick: (Int) -> Unit = {}

    // Thread status
    @get:Synchronized
    private var syncNewOrderStatus: DownloadStatus = DownloadStatus.NOT_RUNNING

    @get:Synchronized
    private var syncCompletedOrderStatus: DownloadStatus = DownloadStatus.NOT_RUNNING

    private enum class DownloadStatus { NOT_RUNNING, RUNNING }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private val handler = Handler(Looper.getMainLooper())

    @get:Synchronized
    private var ticks = 0

    @Suppress("unused")
    fun onNewOrders(`val`: (ArrayList<OrderRequest>) -> Unit) {
        onNewOrders = `val`
    }

    @Suppress("unused")
    fun onCompletedOrders(`val`: (ArrayList<OrderRequest>) -> Unit) {
        onCompletedOrders = `val`
    }

    @Suppress("unused")
    fun onTimerTick(`val`: (Int) -> Unit) {
        onTimerTick = `val`
    }

    //To stop timer
    fun stopSync() {
        Log.d(this::class.java.simpleName, "Deteniendo sincronizador de órdenes")
        if (timer != null) {
            timer?.cancel()
            timer?.purge()
        }
    }

    fun forceSync() {
        goSync(onNewOrders, onCompletedOrders)
    }

    //To start timer
    fun startSync() {
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
                /** TODO: Hacer la función que traiga nuevos pedidos desde la API */
                // GetNewOrder(onEvent = { }, onFinish = { onNewOrders.invoke(it) }).execute()
                onNewOrders.invoke(arrayListOf())
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

    init {
        this.onNewOrders = builder.onNewOrders
        this.onCompletedOrders = builder.onCompletedOrders
        this.onTimerTick = builder.onTimerTick
    }

    class Builder {
        fun build(): Sync {
            return Sync(this)
        }

        internal var onNewOrders: (ArrayList<OrderRequest>) -> Unit = {}
        internal var onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {}
        internal var onTimerTick: (Int) -> Unit = {}

        @Suppress("unused", "unused")
        fun onNewOrders(`val`: (ArrayList<OrderRequest>) -> Unit): Builder {
            onNewOrders = `val`
            return this
        }

        @Suppress("unused")
        fun onCompletedOrders(`val`: (ArrayList<OrderRequest>) -> Unit): Builder {
            onCompletedOrders = `val`
            return this
        }

        @Suppress("unused")
        fun onTimerTick(`val`: (Int) -> Unit): Builder {
            onTimerTick = `val`
            return this
        }
    }
}
