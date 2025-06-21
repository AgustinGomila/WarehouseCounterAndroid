package com.dacosys.warehouseCounter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.dacosys.warehouseCounter.data.ktor.v1.impl.DacoServiceImpl
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.data.ktor.v2.sync.Sync
import com.dacosys.warehouseCounter.data.ktor.v2.sync.SyncViewModel
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.data.settings.SettingsViewModel
import com.dacosys.warehouseCounter.scanners.jotter.Jotter
import io.ktor.client.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.GlobalContext.startKoin
import java.net.Proxy
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * Created by Agustin on 24/01/2017.
 */

class WarehouseCounterApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WarehouseCounterApp)
            modules(appModule)
        }
        initialize()
    }

    private fun initialize() {
        /** Star listening activities events **/
        jotter.startListening()
    }

    companion object {
        val context: Context by lazy { get().get() }
        val jotter: Jotter by lazy { get().get() }
        val settingsRepository: SettingsRepository by lazy { get().get() }
        val settingsVm: SettingsViewModel by lazy { get().get() }
        val syncVm: SyncViewModel by lazy { get().get() }
        val pair: Pair<SSLSocketFactory, X509TrustManager> by lazy { get().get() }
        val httpClient: HttpClient by lazy { get().get() }
        val apiServiceV1: com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl by lazy { get().get() }
        val dacoService: DacoServiceImpl by lazy { get().get() }
        val apiServiceV2: com.dacosys.warehouseCounter.data.ktor.v2.impl.APIServiceImpl by lazy { get().get() }
        val currentProxy: Proxy by lazy { get().get() }
        val json: Json by lazy { get().get() }
        val sharedPreferences: SharedPreferences by lazy { get().get() }
        val apiRequest: ApiRequest by lazy { get().get() }
        val sync: Sync by lazy { get().get() }

        val applicationName: String
            get() {
                val applicationInfo = context.applicationInfo
                val stringId = applicationInfo.labelRes
                return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
                else context.getString(stringId)
            }
    }
}
