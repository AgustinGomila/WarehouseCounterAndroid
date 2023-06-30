package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.error.ErrorObject
import com.dacosys.warehouseCounter.dto.user.AuthData
import com.dacosys.warehouseCounter.dto.user.UserAuthData.Companion.userAuthTag
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class SendItemCode(
    private val itemCodeArray: ArrayList<ItemCode>,
    private val onEvent: (SnackBarEventData) -> Unit = { },
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var currentUser: User

    fun execute() {
        // Función que prosigue al resultado de GetToken
        fun onEvent(it: RequestResult) {
            if (it.status == ResultStatus.SUCCESS) {

                // Proseguimos con la búsqueda...
                scope.launch {
                    coroutineScope {
                        withContext(Dispatchers.IO) { suspendFunction() }
                    }
                }
            } else {
                // Token inválido.
                onEvent.invoke(
                    SnackBarEventData(
                        context.getString(R.string.invalid_or_expired_token), SnackBarType.ERROR
                    )
                )
            }
        }

        if (!DynamicRetrofit.prepare()) {
            onEvent.invoke(
                SnackBarEventData(
                    context.getString(R.string.invalid_url), SnackBarType.ERROR
                )
            )
            return
        }

        Statics.getCurrentUser { user ->
            if (user != null) {
                currentUser = user

                // Chequeamos que el Token sea válido
                thread { GetToken { onEvent(it) }.execute(false) }
            } else {
                // Token inválido.
                onEvent.invoke(
                    SnackBarEventData(
                        context.getString(R.string.invalid_user), SnackBarType.ERROR
                    )
                )
            }
        }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val body = getBody()

        val tempInst = apiService.sendItemCode(body = body)

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    onEvent.invoke(SnackBarEventData(response.message(), SnackBarType.ERROR))
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi.adapter(ErrorObject::class.java).fromJsonValue(resp)
                    onEvent.invoke(
                        SnackBarEventData(
                            errorObject?.error.toString(), SnackBarType.ERROR
                        )
                    )
                    return
                }

                // Actualizamos los ItemCode enviados
                updateTransferred()
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })
    }

    private val itemCodeToUpdate: ArrayList<ItemCode> = ArrayList()

    /**
     * Update transferred
     * Actualizar los ItemCode enviados en la base de datos local
     */
    private fun updateTransferred() {
        for (itemCode in itemCodeToUpdate) {
            ItemCodeCoroutines().updateTransferred(
                itemId = itemCode.itemId ?: 0L, code = itemCode.code ?: ""
            )
        }

        onEvent.invoke(SnackBarEventData(context.getString(R.string.ok), SnackBarType.SUCCESS))
    }

    private fun getBody(): RequestBody {
        // BODY ////////////////////////////
        val jsonParam = JSONObject()

        // User Auth DATA //////////////////
        jsonParam.put(userAuthTag, AuthData().apply {
            username = currentUser.name
            password = currentUser.password ?: ""
        })

        // Collector DATA //////////////////
        val collectorData = Statics.getDeviceData()
        jsonParam.put("collectorData", collectorData)

        // Todos los ItemCodes ////////////
        val icArrayJson = JSONObject()
        for ((index, itemCode) in itemCodeArray.withIndex()) {
            icArrayJson.put("itemCode$index", itemCode)

            // Actualizamos la lista de códigos a transferir
            itemCodeToUpdate.add(itemCode)
        }
        jsonParam.put("itemCodes", icArrayJson)
        // Fin Todos los ItemCodes ////////

        return RequestBody.create("application/json".toMediaTypeOrNull(), jsonParam.toString())
    }
}