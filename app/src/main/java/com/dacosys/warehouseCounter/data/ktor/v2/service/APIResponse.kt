package com.dacosys.warehouseCounter.data.ktor.v2.service

import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData

data class APIResponse<T>(val response: T? = null, val onEvent: SnackBarEventData? = null)