package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.dto.error.ErrorObject
import com.dacosys.warehouseCounter.dto.price.Price
import com.dacosys.warehouseCounter.dto.price.PriceList
import com.dacosys.warehouseCounter.dto.search.SearchObject
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class GetPrice(
    private val searchObject: SearchObject,
    private val onEvent: (SnackBarEventData) -> Unit = { },
    private val onFinish: (ArrayList<Price>) -> Unit,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

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
                SnackBarEventData(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            )
            return
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private suspend fun suspendFunction() = withContext(Dispatchers.IO) {
        val tempInst = apiService.getPrices(body = searchObject)

        Log.i(
            this::class.java.simpleName,
            moshi.adapter(SearchObject::class.java).toJson(searchObject)
        )

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

                /**
                 * Comprobamos si es una respuesta del tipo JsonArray
                 */
                try {
                    val jsonArray = moshi.adapter(List::class.java).fromJsonValue(resp)
                    if (jsonArray?.any() == true) {
                        val r: ArrayList<PriceList> = ArrayList()
                        jsonArray.mapNotNullTo(r) {
                            moshi.adapter(PriceList::class.java).fromJsonValue(it)
                        }

                        val priceArray: ArrayList<Price> = ArrayList()
                        r.flatMapTo(priceArray) { it.prices }

                        onFinish.invoke(priceArray)
                    } else {
                        onEvent.invoke(
                            SnackBarEventData(
                                context.getString(R.string.no_results), SnackBarType.INFO
                            )
                        )
                    }
                } catch (ex: JsonDataException) {
                    Log.e(this.javaClass.simpleName, ex.toString())
                    onEvent.invoke(
                        SnackBarEventData(
                            context.getString(R.string.invalid_prices), SnackBarType.ERROR
                        )
                    )
                    return
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })
    }
}