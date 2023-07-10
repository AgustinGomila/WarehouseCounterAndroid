package com.dacosys.warehouseCounter.sync

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dacosys.imageControl.network.upload.UploadImagesProgress
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest

class SyncViewModel : ViewModel() {
    val syncCompletedOrders: MutableLiveData<ArrayList<OrderRequest>> = MutableLiveData()
    val syncNewOrders: MutableLiveData<ArrayList<OrderRequest>> = MutableLiveData()
    val syncTimer: MutableLiveData<Int?> = MutableLiveData()
    val uploadImagesProgress: MutableLiveData<UploadImagesProgress?> = MutableLiveData()

    @Suppress("unused")
    fun getSyncCompleted(): ArrayList<OrderRequest> {
        return syncCompletedOrders.value ?: ArrayList()
    }

    fun setSyncCompleted(it: ArrayList<OrderRequest>) {
        syncCompletedOrders.postValue(it)
    }

    @Suppress("unused")
    fun getSyncNew(): ArrayList<OrderRequest> {
        return syncNewOrders.value ?: ArrayList()
    }

    fun setSyncNew(it: ArrayList<OrderRequest>) {
        syncNewOrders.postValue(it)
    }

    @Suppress("unused")
    fun getSyncTimer(): Int {
        return syncTimer.value ?: 0
    }

    fun setSyncTimer(it: Int) {
        syncTimer.postValue(it)
    }

    @Suppress("unused")
    fun getUploadImagesProgress(): UploadImagesProgress? {
        return uploadImagesProgress.value
    }

    fun setUploadImagesProgress(it: UploadImagesProgress) {
        uploadImagesProgress.postValue(it)
    }
}