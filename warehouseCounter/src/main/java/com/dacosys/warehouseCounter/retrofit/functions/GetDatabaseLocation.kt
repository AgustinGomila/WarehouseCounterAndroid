package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.moshi.database.DatabaseData
import com.dacosys.warehouseCounter.moshi.error.ErrorObject
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.DbLocationResult
import com.dacosys.warehouseCounter.room.database.WcDatabase.Companion.DATABASE_VERSION
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetDatabaseLocation(private val onEvent: (DbLocationResult) -> Unit) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        if (!DynamicRetrofit.prepare()) {
            sendEvent(
                status = ProgressStatus.crashed, msg = context.getString(R.string.invalid_url)
            )
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
        if (DATABASE_VERSION > 1) {
            version = "-v$DATABASE_VERSION"
        }

        val dbLocInst = apiService.getDbLocation(version = version)

        dbLocInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.raw().toString())
                    sendEvent(
                        status = ProgressStatus.crashed, msg = response.message()
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi.adapter(ErrorObject::class.java).fromJsonValue(resp)
                    Log.e(this.javaClass.simpleName, errorObject?.error.toString())
                    sendEvent(
                        status = ProgressStatus.crashed, msg = errorObject?.error.toString()
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de DatabaseData
                 */
                var p = DatabaseData()
                for (dataCont in (resp as Map<*, *>).entries) {
                    p = moshi.adapter(DatabaseData::class.java).fromJsonValue(dataCont.value)
                        ?: continue
                    break
                }

                sendEvent(
                    status = ProgressStatus.finished,
                    result = p,
                    msg = if (p.dbDate.isNotEmpty() && p.dbFile.isNotEmpty()) {
                        context.getString(R.string.success_response)
                    } else {
                        context.getString(R.string.database_name_is_invalid)
                    }
                )
                return
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                sendEvent(
                    status = ProgressStatus.crashed, msg = t.toString()
                )
            }
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