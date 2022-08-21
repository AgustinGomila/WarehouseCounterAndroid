package com.dacosys.warehouseCounter.itemCategory.activities

import com.dacosys.warehouseCounter.itemCategory.`object`.ItemCategory

interface ItemCategoryChangedObserver {
    fun onItemCategoryChanged(w: ItemCategory?)
}