package com.dacosys.warehouseCounter.configuration

import com.dacosys.imageControl.wsObject.UserAuthResultObject
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
import kotlinx.coroutines.*

class ImageControlCheckUser(private var onSnackBarEvent: (SnackBarEventData) -> Unit = {}) {
    private suspend fun onUiEvent(it: SnackBarEventData) {
        withContext(Dispatchers.Main) {
            onSnackBarEvent.invoke(it)
        }
    }

    private fun postExecute(result: UserAuthResultObject?): UserAuthResultObject? {
        var fReturn = false
        var fError = false

        when (result) {
            null -> fError = true
            else -> fReturn = result.access
        }

        scope.launch {
            onUiEvent(SnackBarEventData(
                when {
                    fError -> context()
                        .getString(R.string.connection_error)
                    !fReturn -> context()
                        .getString(R.string.incorrect_username_password_combination)
                    else -> context().getString(R.string.ok)
                },
                when {
                    fError -> SnackBarType.ERROR
                    !fReturn -> SnackBarType.ERROR
                    else -> SnackBarType.SUCCESS
                }
            ))
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

    private var deferred: Deferred<UserAuthResultObject?>? = null
    private suspend fun doInBackground(): UserAuthResultObject? {
        var result: UserAuthResultObject? = null
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await()
        }
        return postExecute(result)
    }

    private suspend fun suspendFunction(): UserAuthResultObject? = withContext(Dispatchers.IO) {
        return@withContext try {
            Statics.setupImageControl()
            com.dacosys.imageControl.Statics.getWebservice().imageControlUserCheck()
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(null, this::class.java.simpleName, ex)
            null
        }
    }
}