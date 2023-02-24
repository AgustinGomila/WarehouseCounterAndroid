package com.dacosys.warehouseCounter.ui.activities.client

import com.dacosys.warehouseCounter.room.entity.client.Client

interface ClientChangedObserver {
    fun onClientChanged(w: Client?)
}