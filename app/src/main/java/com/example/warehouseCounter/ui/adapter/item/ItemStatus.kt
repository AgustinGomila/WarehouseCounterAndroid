package com.example.warehouseCounter.ui.adapter.item

import com.example.warehouseCounter.ui.adapter.generic.GenericStatus
import com.example.warehouseCounter.ui.adapter.item.ItemStatus.ACTIVE_LOT_DISABLED
import com.example.warehouseCounter.ui.adapter.item.ItemStatus.ACTIVE_LOT_ENABLED
import com.example.warehouseCounter.ui.adapter.item.ItemStatus.INACTIVE_LOT_DISABLED
import com.example.warehouseCounter.ui.adapter.item.ItemStatus.INACTIVE_LOT_ENABLED

enum class ItemStatus(val id: Long) {
    ACTIVE_LOT_ENABLED(0),
    ACTIVE_LOT_DISABLED(1),
    INACTIVE_LOT_ENABLED(2),
    INACTIVE_LOT_DISABLED(3);
}

fun ItemStatus.toGenericStatus(): GenericStatus {
    return when (this) {
        ACTIVE_LOT_ENABLED -> GenericStatus.STATUS_A
        ACTIVE_LOT_DISABLED -> GenericStatus.STATUS_B
        INACTIVE_LOT_ENABLED -> GenericStatus.STATUS_C
        INACTIVE_LOT_DISABLED -> GenericStatus.STATUS_D
    }
}

fun List<ItemStatus>.toGenericStatusList(): List<GenericStatus> {
    return this.map { it.toGenericStatus() }
}