package com.example.warehouseCounter.data.ktor.v2.service

import com.example.warehouseCounter.ui.snackBar.SnackBarEventData

data class APIResponse<T>(val response: T? = null, val onEvent: SnackBarEventData? = null)