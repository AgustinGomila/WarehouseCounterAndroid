package com.example.warehouseCounter.ui.adapter

// Parámetros del filtro
data class FilterOptions(
    var filterString: String = "",
    var showAllOnFilterEmpty: Boolean = true,
)