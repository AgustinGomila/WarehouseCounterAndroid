package com.example.warehouseCounter

import android.util.Base64
import androidx.preference.PreferenceManager
import com.dacosys.imageControl.ImageControl
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.currentProxy
import com.example.warehouseCounter.WarehouseCounterApp.Companion.json
import com.example.warehouseCounter.WarehouseCounterApp.Companion.pair
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v1.impl.DacoServiceImpl
import com.example.warehouseCounter.data.ktor.v1.impl.TrustFactory
import com.example.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.example.warehouseCounter.data.ktor.v2.sync.Sync
import com.example.warehouseCounter.data.ktor.v2.sync.SyncViewModel
import com.example.warehouseCounter.data.settings.SettingsRepository
import com.example.warehouseCounter.data.settings.SettingsViewModel
import com.example.warehouseCounter.misc.CurrentUser
import com.example.warehouseCounter.misc.Statics
import com.example.warehouseCounter.scanners.deviceLifecycle.DeviceLifecycle
import com.example.warehouseCounter.scanners.deviceLifecycle.ScannerManager
import com.example.warehouseCounter.scanners.deviceLifecycle.event.ActivityEvent.Companion.scannerListenerEvents
import com.example.warehouseCounter.ui.adapter.order.OrderViewModel
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Proxy.NO_PROXY
import java.util.concurrent.TimeUnit

val appModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(context) }
    single { SettingsRepository() }

    viewModel { SettingsViewModel() }
    viewModel { SyncViewModel() }
    viewModel { OrderViewModel() }

    /** Proxy */
    single {
        val sv = settingsVm
        if (sv.useProxy) {
            val authenticator = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication =
                    PasswordAuthentication(sv.proxyUser, sv.proxyPass.toCharArray())
            }
            Authenticator.setDefault(authenticator)
            Proxy(Proxy.Type.HTTP, InetSocketAddress(sv.proxy, sv.proxyPort))
        } else {
            NO_PROXY
        }
    }

    /** Json instance */
    single {
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    /** Ktor Client */
    single {
        TrustFactory.getTrustFactoryManager(context)
    }

    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    connectTimeout(settingsVm.connectionTimeout.toLong(), TimeUnit.SECONDS)
                    proxy(currentProxy)
                    sslSocketFactory(pair.first, pair.second)
                }
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(
                            username = CurrentUser.name,
                            password = String(Base64.decode(CurrentUser.password, Base64.DEFAULT))
                        )
                    }
                }
            }
        }
    }

    /** Services for the different versions of the API and the Client Configuration service */
    /** API Version 1 */
    single { com.example.warehouseCounter.data.ktor.v1.impl.APIServiceImpl() }
    /** Client packages API Service Version 1 */
    single { DacoServiceImpl() }

    /** API Version 2 */
    single { com.example.warehouseCounter.data.ktor.v2.impl.APIServiceImpl() }
    single { ApiRequest() }

    /** Synchronization */
    single { Sync.Builder().build() }

    /** Setup ImageControl app identification */
    single { ImageControl.Builder(Statics.INTERNAL_IMAGE_CONTROL_APP_ID).build() }

    /** DeviceLifecycle! */
    single {
        DeviceLifecycle.Builder(androidApplication())
            .setLogEnable(true)
            .setActivityEventFilter(scannerListenerEvents)
            .setLifecycleListener(ScannerManager)
            .build()
    }
}