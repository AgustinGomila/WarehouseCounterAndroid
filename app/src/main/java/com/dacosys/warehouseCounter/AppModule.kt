package com.dacosys.warehouseCounter

import android.util.Base64
import androidx.preference.PreferenceManager
import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.currentProxy
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.pair
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v1.impl.DacoServiceImpl
import com.dacosys.warehouseCounter.data.ktor.v1.impl.TrustFactory
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.data.ktor.v2.sync.Sync
import com.dacosys.warehouseCounter.data.ktor.v2.sync.SyncViewModel
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.data.settings.SettingsViewModel
import com.dacosys.warehouseCounter.misc.CurrentUser
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.scanners.jotter.Jotter
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.scanners.jotter.event.ActivityEvent.Companion.scannerListenerEvents
import com.dacosys.warehouseCounter.ui.adapter.order.OrderViewModel
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
    single { com.dacosys.warehouseCounter.data.ktor.v1.impl.APIServiceImpl() }
    /** Client packages API Service Version 1 */
    single { DacoServiceImpl() }

    /** API Version 2 */
    single { com.dacosys.warehouseCounter.data.ktor.v2.impl.APIServiceImpl() }
    single { ApiRequest() }

    /** Synchronization */
    single { Sync.Builder().build() }

    /** Setup ImageControl app identification */
    single { ImageControl.Builder(Statics.INTERNAL_IMAGE_CONTROL_APP_ID).build() }

    /** Jotter! */
    single {
        Jotter.Builder(androidApplication())
            .setLogEnable(true)
            .setActivityEventFilter(scannerListenerEvents)
            .setLifecycleListener(ScannerManager)
            .build()
    }
}