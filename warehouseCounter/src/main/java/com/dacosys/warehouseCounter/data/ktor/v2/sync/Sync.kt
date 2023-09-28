package com.dacosys.warehouseCounter.data.ktor.v2.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getCompletedOrders
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getPendingOrders
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.removePendingOrder
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderStatus
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines.update
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.AddOrder
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.util.*
import kotlin.concurrent.thread

/**
 * Esta clase se encarga de ejecutar cada cierto período de tiempo dos procesos:
 * 1. Solicitar nuevas órdenes de arqueo (en la web)
 * 2. Solicitar los arqueos completados (en el dispositivo)
 */
class Sync private constructor(builder: Builder) {
    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    private var onNewOrders: (ArrayList<OrderRequest>) -> Unit = {}
    private var onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {}
    private var onTimerTick: (Int) -> Unit = {}

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

    fun stopSync() {
        Log.d(tag, "Deteniendo sincronizador de órdenes")
        if (timer != null) {
            timer?.cancel()
            timer?.purge()
        }
    }

    fun resetSync() {
        ticks = 0
    }

    fun forceSync() {
        goSync(onNewOrders, onCompletedOrders)
    }

    fun startSync() {
        Log.d(tag, "Iniciando sincronizador de órdenes...")

        timer = Timer()
        val interval = settingsVm.wcSyncInterval

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

        /** Forzamos la sincronización inicial */
        if (ticks == 0) goSync(onNewOrders, onCompletedOrders)

        timer?.scheduleAtFixedRate(
            timerTask, 100, 1000
        )
    }

    private fun goSync(
        onNewOrders: (ArrayList<OrderRequest>) -> Unit = {},
        onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
    ) {
        resetSync()

        handler.post {
            getNewOrderRequest(onNewOrders)
            getCompletedOrderRequest(onCompletedOrders)
        }
    }

    private fun getNewOrderRequest(onNewOrders: (ArrayList<OrderRequest>) -> Unit = {}) {
        if (syncNewOrderStatus != DownloadStatus.NOT_RUNNING) return
        syncNewOrderStatus = DownloadStatus.RUNNING

        thread {
            try {
                val filter: ArrayList<ApiFilterParam> = arrayListOf()
                filter.add(
                    ApiFilterParam(
                        columnName = ApiFilterParam.EXTENSION_STATUS_ID,
                        value = OrderStatus.pending.id.toString()
                    )
                )

                GetOrder(
                    filter = filter,
                    onEvent = { },
                    onFinish = { onPendingOrdersReceived(it, onNewOrders) }
                ).execute()

            } catch (ex: Exception) {
                ErrorLog.writeLog(null, tag, ex.message.toString())
            } finally {
                syncNewOrderStatus = DownloadStatus.NOT_RUNNING
            }
        }
    }

    private fun onPendingOrdersReceived(
        pending: ArrayList<OrderResponse>,
        onPendingOrders: (ArrayList<OrderRequest>) -> Unit = {}
    ) {
        val ordersToAdd: ArrayList<OrderResponse> = arrayListOf()
        val filesToRemove: ArrayList<String> = arrayListOf()

        if (pending.isNotEmpty()) {
            val localPendingOrdersId: List<Pair<Long, String>> =
                getPendingOrders().map { Pair(it.orderRequestId ?: 0L, it.filename) }

            for (order in pending) {
                val localPending = localPendingOrdersId.firstOrNull { par -> par.first == order.id }
                if (localPending != null) {
                    if (!order.finishDate.isNullOrEmpty()) {
                        filesToRemove.add(localPending.second)
                    } else {
                        OrderRequestCoroutines.getByIdAsKtor(localPending.first) { localOrder ->
                            if (localOrder != null) {
                                localOrder.orderRequestId = order.id
                                localOrder.contents = order.contentToKtor()
                                update(orderRequest = localOrder, onEvent = { })
                            }
                        }
                    }
                } else {
                    ordersToAdd.add(order)
                }
            }
        }

        if (filesToRemove.isNotEmpty()) {
            for (filename in filesToRemove) {
                Log.d(tag, "Removing completed order: $filename")
                removePendingOrder(filename)
            }
        }

        if (ordersToAdd.isEmpty()) {
            onPendingOrders(arrayListOf())
            return
        }

        var isDone = false

        val newOrderList: ArrayList<OrderRequest> = arrayListOf()
        for ((index, order) in ordersToAdd.withIndex()) {

            AddOrder(
                clientId = order.clientId,
                clientName = "",
                description = order.description,
                orderRequestType = order.orderType,
                onEvent = {
                    if (it.snackBarType == SnackBarType.ERROR) {
                        isDone = index == ordersToAdd.lastIndex
                    }
                },
                onNewId = { newId ->
                    OrderRequestCoroutines.getByIdAsKtor(newId) { newOrder ->
                        if (newOrder != null) {

                            newOrder.orderRequestId = order.id
                            newOrder.contents = order.contentToKtor()
                            update(orderRequest = newOrder, onEvent = { })
                            newOrderList.add(newOrder)
                        }
                        isDone = index == ordersToAdd.lastIndex
                    }
                })
        }

        val startTime = System.currentTimeMillis()
        while (!isDone) {
            if (System.currentTimeMillis() - startTime == settingsVm.connectionTimeout.toLong()) {
                isDone = true
            }
        }

        onPendingOrders(newOrderList)
    }

    private fun getCompletedOrderRequest(
        onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
    ) {
        if (syncCompletedOrderStatus != DownloadStatus.NOT_RUNNING) return
        syncCompletedOrderStatus = DownloadStatus.RUNNING

        thread {
            try {
                val orders = getCompletedOrders()
                onCompletedOrders(orders)
            } catch (ex: java.lang.Exception) {
                ErrorLog.writeLog(null, tag, ex.message.toString())
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
