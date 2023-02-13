package com.dacosys.warehouseCounter.ui.activities.itemCategory

import com.dacosys.warehouseCounter.model.itemCategory.ItemCategory

interface ItemCategoryChangedObserver {
    fun onItemCategoryChanged(w: ItemCategory?)
}