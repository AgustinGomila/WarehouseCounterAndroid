package com.example.warehouseCounter.data.ktor.v2.sync

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.warehouseCounter.BuildConfig
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.io.IOFunc.Companion.getCompletedOrders
import com.example.warehouseCounter.data.io.IOFunc.Companion.getPendingJsonOrders
import com.example.warehouseCounter.data.io.IOFunc.Companion.getPendingPath
import com.example.warehouseCounter.data.io.IOFunc.Companion.removeOrdersFiles
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.example.warehouseCounter.data.ktor.v2.dto.order.OrderStatus
import com.example.warehouseCounter.data.ktor.v2.functions.order.GetOrder
import com.example.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.example.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.example.warehouseCounter.data.room.entity.orderRequest.AddOrder
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import java.util.*
import kotlin.concurrent.thread
import com.example.warehouseCounter.data.room.entity.orderRequest.OrderRequest as OrderRequestRoom


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

    @Synchronized
    private fun getSyncNewOrderStatus(): DownloadStatus {
        return syncNewOrderStatus
    }

    @Synchronized
    private fun setSyncNewOrderStatus(state: DownloadStatus) {
        syncNewOrderStatus = state
    }

    @get:Synchronized
    private var syncCompletedOrderStatus: DownloadStatus = DownloadStatus.NOT_RUNNING

    @Synchronized
    private fun getSyncCompletedOrderStatus(): DownloadStatus {
        return syncCompletedOrderStatus
    }

    @Synchronized
    private fun setSyncCompletedOrderStatus(state: DownloadStatus) {
        syncCompletedOrderStatus = state
    }

    private enum class DownloadStatus { NOT_RUNNING, RUNNING }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private val handler = Handler(Looper.getMainLooper())

    @get:Synchronized
    private var ticks = 0

    fun onNewOrders(`val`: (ArrayList<OrderRequest>) -> Unit) {
        onNewOrders = `val`
    }

    fun onCompletedOrders(`val`: (ArrayList<OrderRequest>) -> Unit) {
        onCompletedOrders = `val`
    }

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
        if (getSyncNewOrderStatus() != DownloadStatus.NOT_RUNNING) return
        setSyncNewOrderStatus(DownloadStatus.RUNNING)

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
                    action = GetOrder.defaultAction,
                    onEvent = { }, // TODO: Elevar mensajes de eventos
                    onFinish = {
                        onPendingOrdersReceived(
                            remoteOrders = it,
                            onPendingOrders = onNewOrders
                        )
                    }
                ).execute()

            } catch (ex: Exception) {
                ErrorLog.writeLog(null, tag, ex.message.toString())
                setSyncNewOrderStatus(DownloadStatus.NOT_RUNNING)
            }
        }
    }

    @get:Synchronized
    private var isProcessDone = false

    @Synchronized
    private fun getProcessState(): Boolean {
        return isProcessDone
    }

    @Synchronized
    private fun setProcessState(state: Boolean) {
        isProcessDone = state
    }

    @set:Synchronized
    var ordersToUpdate: ArrayList<OrderRequest> = arrayListOf()

    @set:Synchronized
    var ordersToRemove: ArrayList<OrderRequest> = arrayListOf()

    @set:Synchronized
    var ordersToAdd: ArrayList<OrderResponse> = arrayListOf()

    private fun onPendingOrdersReceived(
        remoteOrders: ArrayList<OrderResponse>,
        onPendingOrders: (ArrayList<OrderRequest>) -> Unit = {}
    ) {
        ordersToAdd.clear()
        ordersToRemove.clear()
        ordersToUpdate.clear()

        if (remoteOrders.isNotEmpty()) {

            setProcessState(false)

            val localOrders = getLocalOrders()
            val localJsonPendingOrders = getPendingJsonOrders()

            for ((index, remoteOrder) in remoteOrders.withIndex()) {

                val localOrder = localOrders.firstOrNull { it.orderRequestId == remoteOrder.id }
                if (localOrder != null) {

                    val filename = localJsonPendingOrders.firstOrNull { it.roomId == localOrder.id }?.filename
                    if (!filename.isNullOrEmpty()) {

                        OrderRequestCoroutines.getByIdAsKtor(
                            id = localOrder.id,
                            content = remoteOrder.contentToKtor(),
                            filename = filename,
                            onResult = { order ->
                                if (order != null) {
                                    if (order.completed == true) ordersToRemove.add(order)
                                    else ordersToUpdate.add(order)
                                }
                                if (index == remoteOrders.lastIndex) setProcessState(true)
                            })
                    } else {
                        if (index == remoteOrders.lastIndex) setProcessState(true)
                    }
                } else {
                    ordersToAdd.add(remoteOrder)
                    if (index == remoteOrders.lastIndex) setProcessState(true)
                }
            }

            val startTime = System.currentTimeMillis()
            while (!getProcessState()) {
                if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                    setProcessState(true)
                }
            }
        }

        if (ordersToRemove.any()) {
            val tempRemove = ArrayList(ordersToRemove)
            synchronized(tempRemove) {
                if (tempRemove.isNotEmpty()) {
                    removeOrders(tempRemove)
                }
            }
        }

        if (ordersToUpdate.any()) {
            val tempUpdate = ArrayList(ordersToUpdate)
            synchronized(tempUpdate) {
                if (tempUpdate.isNotEmpty()) {
                    updateOrders(tempUpdate)
                }
            }
        }

        if (ordersToAdd.any()) {
            val tempAdd = ArrayList(ordersToAdd)
            synchronized(tempAdd) {
                if (tempAdd.isEmpty()) {
                    onPendingOrders(arrayListOf())
                    setSyncNewOrderStatus(DownloadStatus.NOT_RUNNING)
                } else {
                    addOrders(
                        toAdd = tempAdd,
                        onResult = {
                            onPendingOrders(it)
                            setSyncNewOrderStatus(DownloadStatus.NOT_RUNNING)
                        }
                    )
                }
            }
        } else {
            onPendingOrders(arrayListOf())
            setSyncNewOrderStatus(DownloadStatus.NOT_RUNNING)
        }
    }

    @set:Synchronized
    var tempLocalOrders: ArrayList<OrderRequestRoom> = arrayListOf()

    private fun getLocalOrders(): ArrayList<OrderRequestRoom> {
        setProcessState(false)

        tempLocalOrders.clear()

        OrderRequestCoroutines.getAll {
            tempLocalOrders = ArrayList(it)
            setProcessState(true)
        }
        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        return tempLocalOrders
    }

    @set:Synchronized
    var tempNewOrders: ArrayList<OrderRequest> = arrayListOf()

    private fun addOrders(
        toAdd: ArrayList<OrderResponse>,
        onResult: (ArrayList<OrderRequest>) -> Unit
    ) {
        setProcessState(false)

        tempNewOrders.clear()

        for ((index, order) in toAdd.withIndex()) {

            AddOrder(
                orderResponse = order,
                onEvent = { }, // TODO: Elevar mensajes de eventos
                onNewId = { newId, filename ->

                    Log.d(tag, "Adding order: $filename / $newId")

                    if (newId != 0L) {
                        OrderRequestCoroutines.getByIdAsKtor(
                            id = newId,
                            filename = filename,
                            onResult = { newOrder ->
                                if (newOrder != null) tempNewOrders.add(newOrder)
                                if (index == toAdd.lastIndex) setProcessState(true)
                            })
                    } else {
                        if (index == toAdd.lastIndex) setProcessState(true)
                    }
                })
        }

        val startTime = System.currentTimeMillis()
        while (!getProcessState()) {
            if (System.currentTimeMillis() - startTime == (settingsVm.connectionTimeout * 1000).toLong()) {
                setProcessState(true)
            }
        }

        onResult(tempNewOrders)
    }

    private fun removeOrders(toRemove: ArrayList<OrderRequest>) {
        val filesToRemove = ArrayList(toRemove.map { it.filename })
        val idList = toRemove.mapNotNull { it.roomId }

        Log.d(tag, "Removing orders: ${filesToRemove.joinToString(", ")} / ${idList.joinToString(", ")}")

        removeOrdersFiles(
            path = getPendingPath(),
            filesToRemove = filesToRemove,
            sendEvent = { eventData ->
                if (SnackBarType.SUCCESS.equals(eventData.snackBarType)) {
                    OrderRequestCoroutines.removeById(
                        idList = idList,
                        onResult = { })
                }
            })
    }

    private fun updateOrders(toUpdate: ArrayList<OrderRequest>) {
        for (order in toUpdate) {

            if (BuildConfig.DEBUG) {
                val orderRoomId = order.roomId.toString()
                val orderRequestId = order.orderRequestId.toString()
                val orderFilename = order.filename
                Log.d(tag, "Updating order: $orderRoomId / $orderRequestId / $orderFilename")
            }

            OrderRequestCoroutines.update(
                orderRequest = order,
                onEvent = { }, // TODO: Elevar mensajes de eventos
                onFilename = { }
            )
        }
    }

    private fun getCompletedOrderRequest(
        onCompletedOrders: (ArrayList<OrderRequest>) -> Unit = {},
    ) {
        if (getSyncCompletedOrderStatus() != DownloadStatus.NOT_RUNNING) return
        setSyncCompletedOrderStatus(DownloadStatus.RUNNING)

        thread {
            setSyncCompletedOrderStatus(
                try {
                    val orders = getCompletedOrders()
                    onCompletedOrders(orders)
                    DownloadStatus.NOT_RUNNING
                } catch (ex: java.lang.Exception) {
                    ErrorLog.writeLog(null, tag, ex.message.toString())
                    DownloadStatus.NOT_RUNNING
                }
            )
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

        @Suppress("unused")
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
