package com.dacosys.warehouseCounter

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.dacosys.warehouseCounter.data.token.TokenObject
import com.dacosys.warehouseCounter.retrofit.APIService
import com.dacosys.warehouseCounter.retrofit.DynamicRetrofit
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.SettingsViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import id.pahlevikun.jotter.Jotter
import id.pahlevikun.jotter.event.ActivityEvent
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext.get
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by Agustin on 24/01/2017.
 */

class WarehouseCounterApp : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        sApplication = this

        startKoin {
            androidContext(this@WarehouseCounterApp)
            modules(koinAppModule())
        }

        // Setup ImageControl context
        com.dacosys.imageControl.Statics.ImageControl().setAppContext(this)
    }

    private fun koinAppModule() = module {
        single { sharedPreferences() }
        single { SettingsRepository(get()) }

        viewModel { SettingsViewModel(get()) }

        single { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
        single { MoshiConverterFactory.create(get()) }
        factory {
            // Connection Timeouts
            OkHttpClient.Builder()
                .connectTimeout(settingViewModel().connectionTimeout.toLong(), TimeUnit.SECONDS)
                .build()
        }
        single { DynamicRetrofit() }
        single { retrofit().api.create(APIService::class.java) }

        single {
            // Eventos del ciclo de vida de las actividades
            // que nos interesa interceptar para conectar y
            // desconectar los medios de lectura de códigos.
            Jotter.Builder(this@WarehouseCounterApp).setLogEnable(true)
                .setActivityEventFilter(listOf(ActivityEvent.CREATE,
                    ActivityEvent.RESUME,
                    ActivityEvent.PAUSE,
                    ActivityEvent.DESTROY))
                //.setFragmentEventFilter(listOf(FragmentEvent.VIEW_CREATE, FragmentEvent.PAUSE))
                .setJotterListener(JotterListener).build().startListening()
        }
    }

    companion object {
        fun context(): Context {
            return get().get()
        }

        fun moshi(): Moshi {
            return get().get()
        }

        fun okHttp(): OkHttpClient {
            return get().get()
        }

        fun moshiConverterFactory(): MoshiConverterFactory {
            return get().get()
        }

        fun settingRepository(): SettingsRepository {
            return get().get()
        }

        fun retrofit(): DynamicRetrofit {
            return get().get()
        }

        fun apiService(): APIService {
            return get().get()
        }

        fun settingViewModel(): SettingsViewModel {
            return get().get()
        }

        private fun applicationName(): String {
            val applicationInfo = context().applicationInfo
            val stringId = applicationInfo.labelRes
            return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString()
            else context().getString(stringId)
        }

        fun sharedPreferences(): SharedPreferences {
            return context().getSharedPreferences("${applicationName()}_prefs", MODE_PRIVATE)
        }

        fun cleanPrefs(): Boolean {
            return try {
                sharedPreferences().edit().clear().apply()
                true
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
                false
            }
        }

        var Token: TokenObject = TokenObject()

        fun cleanToken() {
            Token = TokenObject("", "")
        }

        private var sApplication: Application? = null
    }
}