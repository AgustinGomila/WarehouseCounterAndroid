package com.dacosys.warehouseCounter.ui.adapter

// Parámetros del filtro
data class FilterOptions(
    var filterString: String = "",
    var showAllOnFilterEmpty: Boolean = true,
)