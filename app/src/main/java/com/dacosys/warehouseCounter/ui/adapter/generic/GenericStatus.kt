package com.dacosys.warehouseCounter.ui.adapter.generic

import com.dacosys.warehouseCounter.ui.adapter.item.ItemStatus
import com.dacosys.warehouseCounter.ui.adapter.item.ItemStatus.*

enum class GenericStatus(val id: Long) {
    STATUS_A(0), STATUS_B(1), STATUS_C(2), STATUS_D(3)
}

fun GenericStatus.toItemStatus(): ItemStatus {
    return when (this) {
        GenericStatus.STATUS_A -> ACTIVE_LOT_ENABLED
        GenericStatus.STATUS_B -> ACTIVE_LOT_DISABLED
        GenericStatus.STATUS_C -> INACTIVE_LOT_ENABLED
        GenericStatus.STATUS_D -> INACTIVE_LOT_DISABLED
    }
}

fun List<GenericStatus>.toItemStatusList(): List<ItemStatus> {
    return this.map { it.toItemStatus() }
}