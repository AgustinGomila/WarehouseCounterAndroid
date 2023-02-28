package com.dacosys.warehouseCounter.retrofit

import android.util.Log
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshiConverterFactory
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.okHttp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import retrofit2.Retrofit
import java.net.URL

/**
 * DynamicRetrofit permite actualizar la URL de Retrofit en la instancia de Koin
 */
class DynamicRetrofit {
    private fun buildRetrofit() = Retrofit.Builder().baseUrl(getUrl()).client(okHttp)
        .addConverterFactory(moshiConverterFactory).build()

    var api: Retrofit
        private set

    /**
     * Reconstruye la instancia de Retrofit.
     */
    init {
        api = buildRetrofit()
    }

    companion object {
        private var protocol: String = ""
        private var host: String = ""
        var apiUrl: String = ""

        fun getUrl(): String {
            return "${protocol}://${host}/"
        }

        /**
         * Reset
         * Limpia los datos de conexión para que sean regenerados en la siguiente invocación.
         */
        fun reset() {
            protocol = ""
            host = ""
            apiUrl = ""
        }

        /**
         * Prepare
         * Inicializa la instancia de Retrofit si es necesario.
         * @return True si se recreó la instancia sin errores.
         */
        fun prepare(): Boolean {
            return if (protocol.isEmpty() || host.isEmpty()) refresh() else true
        }

        /**
         * Prepare
         * Inicializa la instancia de Retrofit con una URL específica. Recordar llamar a [reset] al
         * terminar de usar la conexión, para que se regenere la URL del cliente la próxima vez.
         * @param protocol
         * @param host
         * @param apiUrl
         * @return True si se recreó la instancia sin errores.
         */
        fun prepare(protocol: String, host: String, apiUrl: String = ""): Boolean {
            this.protocol = protocol
            this.host = host
            this.apiUrl = apiUrl

            try {
                Log.d(
                    this::class.java.simpleName,
                    "Base URL: ${Companion.protocol}://${Companion.host}/ (Api URL: ${apiUrl.ifEmpty { "Vacío" }})"
                )

                // Refrescamos la instancia de Retrofit
                DynamicRetrofit()
            } catch (e: Exception) {
                Log.e(this::class.java.simpleName, e.message.toString())
                reset()
                return false
            }

            return true
        }

        private fun refresh(): Boolean {
            val url: URL?
            try {
                url = URL(settingRepository.urlPanel.value.toString())

                this.protocol = url.protocol
                this.host = url.host
                if (url.path.isNotEmpty()) apiUrl = "${url.path}/"

                Log.d(
                    this::class.java.simpleName,
                    "Base URL: ${protocol}://${host}/ (Api URL: ${apiUrl.ifEmpty { "Vacío" }})"
                )

                // Refrescamos la instancia de Retrofit
                DynamicRetrofit()
            } catch (e: Exception) {
                Log.e(this::class.java.simpleName, e.message.toString())
                reset()
                return false
            }

            return true
        }
    }
}