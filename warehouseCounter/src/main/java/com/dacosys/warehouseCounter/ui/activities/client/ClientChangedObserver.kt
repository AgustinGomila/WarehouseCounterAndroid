package com.dacosys.warehouseCounter.ui.activities.client

import com.dacosys.warehouseCounter.model.client.Client

interface ClientChangedObserver {
    fun onClientChanged(w: Client?)
}