package com.example.warehouseCounter.data.ktor.v1.functions

import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.apiServiceV1
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.data.ktor.v1.impl.APIServiceImpl.Companion.validUrl
import com.example.warehouseCounter.data.ktor.v1.service.DbLocationResult
import com.example.warehouseCounter.data.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.example.warehouseCounter.misc.objects.status.ProgressStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        apiServiceV1.getDbLocation(version = version, callback = {
            val db = it.firstOrNull() ?: com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData()
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
        result: com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData = com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData(),
        msg: String = "",
    ) {
        onEvent.invoke(
            DbLocationResult(
                status = status, result = result, msg = msg
            )
        )
    }
}
