package com.dacosys.warehouseCounter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.ktor.APIServiceImpl
import com.dacosys.warehouseCounter.ktor.DacoServiceImpl
import com.dacosys.warehouseCounter.misc.Statics.Companion.INTERNAL_IMAGE_CONTROL_APP_ID
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.SettingsViewModel
import id.pahlevikun.jotter.Jotter
import id.pahlevikun.jotter.event.ActivityEvent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
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

        // Eventos del ciclo de vida de las actividades
        // que nos interesa interceptar para conectar y
        // desconectar los medios de lectura de códigos.
        Jotter.Builder(this@WarehouseCounterApp).setLogEnable(true).setActivityEventFilter(
            listOf(
                ActivityEvent.CREATE,
                ActivityEvent.RESUME,
                ActivityEvent.PAUSE,
                ActivityEvent.DESTROY
            )
        )
            //.setFragmentEventFilter(listOf(FragmentEvent.VIEW_CREATE, FragmentEvent.PAUSE))
            .setJotterListener(JotterListener).build().startListening()

        // Setup ImageControl context
        ImageControl().create(
            context = applicationContext,
            id = INTERNAL_IMAGE_CONTROL_APP_ID
        )
    }

    private fun koinAppModule() = module {
        single { sharedPreferences }
        single { SettingsRepository() }

        viewModel { SettingsViewModel() }

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
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        encodeDefaults = true
                        ignoreUnknownKeys = true
                    })
                }
            }
        }
        single { APIServiceImpl() }
        single { DacoServiceImpl() }
    }

    companion object {
        val context: Context
            get() = get().get()

        val settingRepository: SettingsRepository
            get() = get().get()

        val settingViewModel: SettingsViewModel
            get() = get().get()

        val httpClient: HttpClient
            get() = get().get()

        val ktorApiService: APIServiceImpl
            get() = get().get()

        val ktorDacoService: DacoServiceImpl
            get() = get().get()

        val currentProxy: Proxy
            get() = get().get()

        fun applicationName(): String {
            val applicationInfo = context.applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
            else context.getString(stringId)
        }

        val sharedPreferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(context)
    }
}