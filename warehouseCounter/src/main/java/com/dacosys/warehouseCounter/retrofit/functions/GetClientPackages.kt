package com.dacosys.warehouseCounter.retrofit.functions

import android.util.Log
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.dacoService
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID
import com.dacosys.warehouseCounter.misc.Statics.Companion.APP_VERSION_ID_IMAGECONTROL
import com.dacosys.warehouseCounter.moshi.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.moshi.clientPackage.ClientAuthData
import com.dacosys.warehouseCounter.moshi.clientPackage.Package
import com.dacosys.warehouseCounter.moshi.error.ErrorData
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.result.PackagesResult
import com.dacosys.warehouseCounter.sync.ProgressStatus
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GetClientPackages(private val onEvent: (PackagesResult) -> Unit) {
    private var myEmail = ""
    private var myPassword = ""
    private var myInstallationCode = ""
    private var progressStatus = ProgressStatus.unknown

    fun addParams(
        email: String,
        password: String,
        installationCode: String,
    ) {
        this.myEmail = email
        this.myPassword = password
        this.myInstallationCode = installationCode
    }

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    fun cancel() {
        scope.cancel()
    }

    fun execute() {
        progressStatus = ProgressStatus.starting

        scope.launch {
            doInBackground()
        }
    }

    private suspend fun doInBackground() {
        coroutineScope {
            withContext(Dispatchers.IO) { suspendFunction() }
        }
    }

    private suspend fun suspendFunction(): Boolean = withContext(Dispatchers.IO) {
        setupRetrofit()

        // Llamado a la función del servidor
        val packages = dacoService().getClientPackage(body = getBody())

        packages.enqueue(object : Callback<Any?> {
            override fun onResponse(call: Call<Any?>, response: Response<Any?>) {
                val resp = response.body()
                if (resp == null) {
                    Log.e(this.javaClass.simpleName, response.raw().toString())
                    sendEvent(
                        status = ProgressStatus.crashed,
                        result = ArrayList(),
                        msg = response.message()
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta de Error predefinida
                 */
                val errorObject = moshi().adapter(ErrorData::class.java).fromJsonValue(resp)
                if (errorObject != null && errorObject.code.isNotEmpty()) {
                    Log.e(this.javaClass.simpleName, errorObject.description)
                    sendEvent(
                        status = ProgressStatus.crashed,
                        result = ArrayList(),
                        msg = errorObject.description
                    )
                    return
                }

                /**
                 * Comprobamos si es una respuesta del tipo colección de Packages
                 */
                if (!isMapOfStringsToAny(resp)) {
                    sendEvent(
                        status = ProgressStatus.crashed,
                        result = ArrayList(),
                        msg = context().getString(R.string.client_has_no_software_packages)
                    )
                    return
                }

                // Colección de paquetes para el cliente
                val allClientPackage: ArrayList<Package> = ArrayList()

                for (allPackages in (resp as Map<*, *>).entries) {
                    val packageMap = allPackages.value as Map<*, *>
                    for (pack in packageMap.values) {
                        val p = moshi().adapter(Package::class.java).fromJsonValue(pack) ?: continue

                        val installationCode = p.installationCode
                        if (myInstallationCode.isNotEmpty() && installationCode != myInstallationCode) {
                            continue
                        }

                        val productId = p.productVersionId
                        if (p.active == 1 && (productId == APP_VERSION_ID || productId == APP_VERSION_ID_IMAGECONTROL)) {
                            allClientPackage.add(p)
                        }
                    }
                }

                sendEvent(
                    status = ProgressStatus.finished,
                    result = allClientPackage,
                    msg = context().getString(R.string.successful_connection)
                )
                return
            }

            override fun onFailure(call: Call<Any?>, t: Throwable) {
                Log.e(this.javaClass.simpleName, t.toString())
                sendEvent(
                    status = ProgressStatus.crashed, result = ArrayList(), msg = t.toString()
                )
            }
        })
        return@withContext true
    }

    private fun setupRetrofit() {
        // Datos de conexión
        val host = "config.dacosys.com"
        val protocol = "https"

        // Configuración y refresco de la conexión
        DynamicRetrofit.start(protocol = protocol, host = host)
    }

    private fun getBody(): AuthDataCont {
        // Autentificación del Ciente
        val authData = ClientAuthData().apply {
            this.version = "1"
            this.email = myEmail
            this.password = myPassword
        }
        return AuthDataCont().apply { this.authData = authData }
    }

    fun isMapOfStringsToAny(value: Any?): Boolean {
        if (value !is Map<*, *>) return false
        for ((key, v) in value) {
            if (key !is String) return false
            if (v !is Any) return false
        }
        return true
    }

    private fun sendEvent(
        status: ProgressStatus = ProgressStatus.unknown,
        result: ArrayList<Package> = ArrayList(),
        msg: String = "",
    ) {
        onEvent.invoke(
            PackagesResult(
                status = status,
                result = result,
                clientEmail = myEmail,
                clientPassword = myPassword,
                msg = msg
            )
        )
    }
}