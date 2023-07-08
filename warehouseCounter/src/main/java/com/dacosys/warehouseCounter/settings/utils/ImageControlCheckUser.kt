package com.dacosys.warehouseCounter.settings.utils

import com.dacosys.imageControl.ImageControl.Companion.webservice
import com.dacosys.imageControl.dto.UserAuthResult
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*

class ImageControlCheckUser(private var onSnackBarEvent: (SnackBarEventData) -> Unit = {}) {
    private suspend fun onUiEvent(it: SnackBarEventData) {
        withContext(Dispatchers.Main) {
            onSnackBarEvent.invoke(it)
        }
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch {
            coroutineScope {
                withContext(Dispatchers.Default) { suspendFunction() }
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        return@withContext try {
            settingViewModel.setupImageControl()
            val r = webservice.imageControlUserCheck()

            postExecute(r)
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(null, this::class.java.simpleName, ex)
        }
    }

    private fun postExecute(result: UserAuthResult?): UserAuthResult? {
        var fReturn = false
        var fError = false

        when (result) {
            null -> fError = true
            else -> fReturn = result.access
        }

        scope.launch {
            onUiEvent(
                SnackBarEventData(
                    when {
                        fError -> context.getString(R.string.connection_error)
                        !fReturn -> context.getString(R.string.incorrect_username_password_combination)
                        else -> context.getString(R.string.ok)
                    }, when {
                        fError -> SnackBarType.ERROR
                        !fReturn -> SnackBarType.ERROR
                        else -> SnackBarType.SUCCESS
                    }
                )
            )
        }
        return result
    }
}