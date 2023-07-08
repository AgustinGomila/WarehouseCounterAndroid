package com.dacosys.warehouseCounter.ktor.functions

import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.ktorApiService
import com.dacosys.warehouseCounter.dto.database.DatabaseData
import com.dacosys.warehouseCounter.ktor.APIServiceImpl.Companion.validUrl
import com.dacosys.warehouseCounter.network.DbLocationResult
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*

class GetDatabaseLocation(private val onEvent: (DbLocationResult) -> Unit) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        if (!validUrl()) {
            sendEvent(status = ProgressStatus.crashed, msg = context.getString(R.string.invalid_url))
            return
        }

        scope.launch {
            coroutineScope {
                withContext(Dispatchers.IO) { suspendFunction() }
            }
        }
    }


    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        var version = ""
        @Suppress("KotlinConstantConditions") if (DATABASE_VERSION > 1) {
            version = "-v$DATABASE_VERSION"
        }

        ktorApiService.getDbLocation(version = version, callback = {
            val db = it.firstOrNull() ?: DatabaseData()
            sendEvent(
                status = ProgressStatus.finished,
                result = db,
                msg = if (db.dbDate.isNotEmpty() && db.dbFile.isNotEmpty()) {
                    context.getString(R.string.success_response)
                } else {
                    context.getString(R.string.database_name_is_invalid)
                }
            )
        })
    }

    private fun sendEvent(
        status: ProgressStatus = ProgressStatus.unknown,
        result: DatabaseData = DatabaseData(),
        msg: String = "",
    ) {
        onEvent.invoke(
            DbLocationResult(
                status = status, result = result, msg = msg
            )
        )
    }
}