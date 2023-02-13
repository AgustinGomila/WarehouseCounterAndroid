package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.model.error.ErrorObject
import com.dacosys.warehouseCounter.model.price.Price
import com.dacosys.warehouseCounter.model.price.PriceList
import com.dacosys.warehouseCounter.retrofit.result.RequestResult
import com.dacosys.warehouseCounter.retrofit.result.ResultStatus
import com.dacosys.warehouseCounter.retrofit.search.SearchPrice
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread

class GetPrice(
    private val searchPrice: SearchPrice,
    private val onEvent: ((SnackBarEventData) -> Unit)? = null,
    private val onFinish: (ArrayList<Price>) -> Unit,
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun execute() {
        fun onEvent(it: RequestResult) {
            if (it.status == ResultStatus.SUCCESS) {
                // Proseguimos con la búsqueda...
                scope.launch { doInBackground() }
            } else {
                // Token inválido.
                onEvent?.invoke(
                    SnackBarEventData(
                        context().getString(R.string.invalid_or_expired_token),
                        SnackBarType.ERROR
                    )
                )
            }
        }

        // Chequeamos que el Token sea válido
        thread { GetToken { onEvent(it) }.execute(false) }
    }

    private var deferred: Deferred<Boolean>? = null
    private suspend fun doInBackground(): Boolean {
        var result = false
        coroutineScope {
            deferred = async { suspendFunction() }
            result = deferred?.await() ?: false
        }
        return result
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        val sv = settingViewModel()
        val baseUrl: String
        var apiUrl = ""

        try {
            val url = URL(sv.urlPanel)
            baseUrl = "${url.protocol}://${url.host}/"
            if (url.path.isNotEmpty()) apiUrl = "${url.path}/"
        } catch (e: MalformedURLException) {
            onEvent?.invoke(
                SnackBarEventData(
                    context().getString(R.string.url_malformed),
                    SnackBarType.ERROR
                )
            )
            Log.e(this::class.java.simpleName, e.toString())
            return@withContext false
        }

        Log.d(this::class.java.simpleName,
            "Base URL: $baseUrl (Api URL: ${apiUrl.ifEmpty { "Vacío" }})"
        )

        val tempInst = apiService().getPrices(apiUrl = apiUrl, body = searchPrice)

        Log.i(
            this::class.java.simpleName,
            moshi().adapter(SearchPrice::class.java).toJson(searchPrice)
        )

        tempInst.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.message())
                    onEvent?.invoke(SnackBarEventData(response.message(), SnackBarType.ERROR))
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                if (ErrorObject.isError(resp)) {
                    val errorObject = moshi().adapter(ErrorObject::class.java).fromJsonValue(resp)
                    onEvent?.invoke(
                        SnackBarEventData(
                            errorObject?.error.toString(),
                            SnackBarType.ERROR
                        )
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo JsonArray
                 */
                try {
                    val jsonArray = moshi().adapter(List::class.java).fromJsonValue(resp)
                    if (jsonArray?.any() == true) {
                        val r: ArrayList<PriceList> = ArrayList()
                        jsonArray.mapNotNullTo(r) {
                            moshi().adapter(PriceList::class.java).fromJsonValue(it)
                        }

                        val priceArray: ArrayList<Price> = ArrayList()
                        r.flatMapTo(priceArray) { it.prices }

                        onFinish.invoke(priceArray)
                    } else {
                        onEvent?.invoke(
                            SnackBarEventData(
                                context().getString(R.string.no_results),
                                SnackBarType.INFO
                            )
                        )
                    }
                } catch (ex: JsonDataException) {
                    Log.e(this.javaClass.simpleName, ex.toString())
                    onEvent?.invoke(
                        SnackBarEventData(
                            context().getString(R.string.invalid_prices),
                            SnackBarType.ERROR
                        )
                    )
                    return
                }
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                onEvent?.invoke(SnackBarEventData(t.toString(), SnackBarType.ERROR))
            }
        })

        return@withContext true
    }
}