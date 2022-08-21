package com.dacosys.warehouseCounter.client.activities

import com.dacosys.warehouseCounter.client.`object`.Client

interface ClientChangedObserver {
    fun onClientChanged(w: Client?)
}