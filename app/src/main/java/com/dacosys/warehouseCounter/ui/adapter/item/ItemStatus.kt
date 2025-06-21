package com.dacosys.warehouseCounter.ui.adapter.item

import com.dacosys.warehouseCounter.ui.adapter.generic.GenericStatus
import com.dacosys.warehouseCounter.ui.adapter.item.ItemStatus.*

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