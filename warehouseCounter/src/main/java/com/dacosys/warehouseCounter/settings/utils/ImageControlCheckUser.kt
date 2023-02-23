package com.dacosys.warehouseCounter.settings.utils

import com.dacosys.imageControl.ImageControl.Companion.webservice
import com.dacosys.imageControl.network.webService.moshi.UserAuthResult
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.misc.Statics
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
                        fError -> context().getString(R.string.connection_error)
                        !fReturn -> context().getString(R.string.incorrect_username_password_combination)
                        else -> context().getString(R.string.ok)
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

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        scope.launch { doInBackground() }
    }

    private var deferred: Deferred<UserAuthResult?>? = null
    private suspend fun doInBackground(): UserAuthResult? {
        var result: UserAuthResult? = null
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await()
        }
        return postExecute(result)
    }

    private suspend fun suspendFunction(): UserAuthResult? = withContext(Dispatchers.IO) {
        return@withContext try {
            Statics.setupImageControl()
            webservice.imageControlUserCheck()
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(null, this::class.java.simpleName, ex)
            null
        }
    }
}