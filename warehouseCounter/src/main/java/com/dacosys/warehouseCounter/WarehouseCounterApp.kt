package com.dacosys.warehouseCounter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.PreferenceManager
import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.ktor.v1.impl.DacoServiceImpl
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.INTERNAL_IMAGE_CONTROL_APP_ID
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.SettingsViewModel
import com.dacosys.warehouseCounter.sync.SyncViewModel
import id.pahlevikun.jotter.Jotter
import id.pahlevikun.jotter.event.ActivityEvent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Proxy.NO_PROXY
import java.util.concurrent.TimeUnit

/**
 * Created by Agustin on 24/01/2017.
 */

class WarehouseCounterApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WarehouseCounterApp)
            modules(koinAppModule())
        }

        /**
         * Life cicle events of the activities
         * that we are interested in intercepting to connect and disconnect the code reading means.
         */
        Jotter.Builder(this@WarehouseCounterApp)
            .setLogEnable(true)
            .setActivityEventFilter(
                listOf(
                    ActivityEvent.CREATE,
                    ActivityEvent.RESUME,
                    ActivityEvent.PAUSE,
                    ActivityEvent.DESTROY
                )
            )
            .setJotterListener(JotterListener).build().startListening()

        /** Setup ImageControl context and app identification */
        ImageControl().create(
            context = applicationContext, id = INTERNAL_IMAGE_CONTROL_APP_ID
        )
    }

    private fun koinAppModule() = module {
        single { PreferenceManager.getDefaultSharedPreferences(context) }
        single { SettingsRepository() }

        viewModel { SettingsViewModel() }
        viewModel { SyncViewModel() }

        single {
            val sv = settingViewModel
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

        single {
            Json {
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
        }

        // Ktor
        single {
            HttpClient(OkHttp) {
                engine {
                    config {
                        followRedirects(true)
                        connectTimeout(settingViewModel.connectionTimeout.toLong(), TimeUnit.SECONDS)
                        proxy(currentProxy)
                    }
                }
                install(ContentNegotiation) {
                    json(json)
                }
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(
                                username = Statics.currentUserName,
                                password = String(Base64.decode(Statics.currentPass, Base64.DEFAULT))
                            )
                        }
                    }
                }
            }
        }

        /** Services for the different versions of the API and the Client Configuration service */
        /** API Version 1 */
        single { com.dacosys.warehouseCounter.ktor.v1.impl.APIServiceImpl() }
        /** Client packages API Service Version 1 */
        single { DacoServiceImpl() }

        /** API Version 2 */
        single { com.dacosys.warehouseCounter.ktor.v2.impl.APIServiceImpl() }
        single { ApiRequest() }
    }

    companion object {
        val context: Context
            get() = get().get()

        val settingRepository: SettingsRepository
            get() = get().get()

        val settingViewModel: SettingsViewModel
            get() = get().get()

        val syncViewModel: SyncViewModel
            get() = get().get()

        val httpClient: HttpClient
            get() = get().get()

        val ktorApiServiceV1: com.dacosys.warehouseCounter.ktor.v1.impl.APIServiceImpl
            get() = get().get()

        val ktorDacoService: DacoServiceImpl
            get() = get().get()

        val ktorApiServiceV2: com.dacosys.warehouseCounter.ktor.v2.impl.APIServiceImpl
            get() = get().get()

        val currentProxy: Proxy
            get() = get().get()

        val json: Json
            get() = get().get()

        val sharedPreferences: SharedPreferences
            get() = get().get()

        val apiRequest: ApiRequest
            get() = get().get()

        fun applicationName(): String {
            val applicationInfo = context.applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
            else context.getString(stringId)
        }
    }
}