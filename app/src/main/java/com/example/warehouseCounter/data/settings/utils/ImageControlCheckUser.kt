package com.example.warehouseCounter.data.settings.utils

import com.dacosys.imageControl.ImageControl.Companion.imageControl
import com.dacosys.imageControl.dto.UserAuthResult
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.misc.imageControl.ImageControl.Companion.setupImageControl
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.ui.snackBar.SnackBarEventData
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            setupImageControl()
            val r = imageControl.webservice.imageControlUserCheck()

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
