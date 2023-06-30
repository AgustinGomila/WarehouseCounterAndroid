package com.dacosys.warehouseCounter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.dacosys.imageControl.ImageControl
import com.dacosys.warehouseCounter.ktor.KtorAPIService
import com.dacosys.warehouseCounter.ktor.KtorAPIServiceImpl
import com.dacosys.warehouseCounter.misc.Statics.Companion.INTERNAL_IMAGE_CONTROL_APP_ID
import com.dacosys.warehouseCounter.misc.Statics.Companion.WC_ROOT_PATH
import com.dacosys.warehouseCounter.retrofit.APIService
import com.dacosys.warehouseCounter.retrofit.DacoService
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.retrofit.HostInterceptor
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.SettingsViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import id.pahlevikun.jotter.Jotter
import id.pahlevikun.jotter.event.ActivityEvent
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import retrofit2.converter.moshi.MoshiConverterFactory
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
            appRothPath = WC_ROOT_PATH,
            id = INTERNAL_IMAGE_CONTROL_APP_ID
        )
    }

    private fun koinAppModule() = module {
        single { sharedPreferences }
        single { SettingsRepository() }

        viewModel { SettingsViewModel() }

        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { MoshiConverterFactory.create(get()) }

        factory {
            val sv = settingViewModel

            // Proxy
            val proxy = if (sv.useProxy) {
                val authenticator = object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication =
                        PasswordAuthentication(sv.proxyUser, sv.proxyPass.toCharArray())
                }
                Authenticator.setDefault(authenticator)
                Proxy(Proxy.Type.HTTP, InetSocketAddress(sv.proxy, sv.proxyPort))
            } else {
                NO_PROXY
            }

            // Connection Timeouts
            OkHttpClient.Builder().connectTimeout(sv.connectionTimeout.toLong(), TimeUnit.SECONDS)
                .addInterceptor(HostInterceptor()).proxy(proxy).build()
        }

        single { DynamicRetrofit() }
        single { retrofit.api.create(APIService::class.java) }
        single { retrofit.api.create(DacoService::class.java) }
    }

    companion object {
        val context: Context
            get() = get().get()

        val moshi: Moshi
            get() = get().get()

        val okHttp: OkHttpClient
            get() = get().get()

        val moshiConverterFactory: MoshiConverterFactory
            get() = get().get()

        val settingRepository: SettingsRepository
            get() = get().get()

        val retrofit: DynamicRetrofit
            get() = get().get()

        val apiService: APIService
            get() = get().get()

        val dacoService: DacoService
            get() = get().get()

        val settingViewModel: SettingsViewModel
            get() = get().get()

        fun applicationName(): String {
            val applicationInfo = context.applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
            else context.getString(stringId)
        }

        val sharedPreferences: SharedPreferences
            get() = context.getSharedPreferences("${applicationName()}_prefs", MODE_PRIVATE)
    }
}