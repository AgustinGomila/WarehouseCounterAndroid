package com.dacosys.warehouseCounter.ui.activities.itemCategory

import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory

interface ItemCategoryChangedObserver {
    fun onItemCategoryChanged(w: ItemCategory?)
}