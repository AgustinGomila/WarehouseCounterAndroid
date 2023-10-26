package com.dacosys.warehouseCounter.data.ktor.v2.impl

import android.util.Base64
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.currentProxy
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderPackage
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.GetItem
import com.dacosys.warehouseCounter.data.ktor.v2.functions.itemCode.GetItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetRack
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.BARCODE_LABEL_TEMPLATE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ITEM_CODE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ITEM_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.RACK_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_AREA_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_PATH
import com.dacosys.warehouseCounter.data.room.entity.user.User
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.data.settings.SettingsViewModel
import com.dacosys.warehouseCounter.misc.CurrentUser
import com.dacosys.warehouseCounter.scanners.scanCode.testDispatcherModule
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import io.github.cdimascio.dotenv.DotenvBuilder
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.Proxy.NO_PROXY
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ApiRequestTest : KoinTest {

    private lateinit var sv: SettingsViewModel
    private lateinit var apiRequest: ApiRequest

    @Before
    fun setUp() {
        val env = DotenvBuilder()
            .directory("/assets")
            .filename("env")
            .load()

        val userId = env["USER_ID_TEST"]
        val username = env["USER_NAME_TEST"]
        val password = env["PASSWORD_TEST"]
        val apiUrl = env["API_TEST_URL"]

        CurrentUser.set(User(userId = userId.toLong(), name = username, password = password))

        stopKoin() // to remove 'A Koin Application has already been started'
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(koinAppModule(), testDispatcherModule))
        }

        sv = getKoin().get()
        sv.urlPanel = apiUrl

        apiRequest = getKoin().get()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun getDatabase(): Unit = runTest {

        var r: DatabaseData? = null
        val lock = CountDownLatch(1)

        val versionDb = "-v2"

        apiRequest.getDatabase(version = versionDb, callback = {
            if (it.response != null) r = it.response
            if (it.onEvent != null) sendEvent(it.onEvent)
            else lock.countDown()
        })

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfItems(): Unit = runTest {

        var r: ListResponse<Item>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<Item>(
            objPath = ITEM_PATH,
            listName = Item.ITEM_LIST_KEY,
            action = GetItem.defaultAction,
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is Item) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfRacks(): Unit = runTest {

        var r: ListResponse<Rack>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<Rack>(
            objPath = RACK_PATH,
            listName = Rack.RACK_LIST_KEY,
            action = GetRack.defaultAction,
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is Rack) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfWarehouses(): Unit = runTest {

        var r: ListResponse<Warehouse>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<Warehouse>(
            objPath = WAREHOUSE_PATH,
            listName = Warehouse.WAREHOUSE_LIST_KEY,
            action = GetWarehouse.defaultAction,
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is Warehouse) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfAreas(): Unit = runTest {

        var r: ListResponse<WarehouseArea>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<WarehouseArea>(
            objPath = WAREHOUSE_AREA_PATH,
            listName = WarehouseArea.WAREHOUSE_AREA_LIST_KEY,
            action = GetWarehouse.defaultAction,
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is WarehouseArea) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfBarcodeLabelTemplate(): Unit = runTest {

        var r: ListResponse<BarcodeLabelTemplate>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<BarcodeLabelTemplate>(
            objPath = BARCODE_LABEL_TEMPLATE_PATH,
            listName = BarcodeLabelTemplate.BARCODE_LABEL_TEMPLATE_LIST_KEY,
            action = arrayListOf(),
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is BarcodeLabelTemplate) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfOrderResponse(): Unit = runTest {

        var r: ListResponse<OrderResponse>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<OrderResponse>(
            objPath = ApiRequest.ORDER_PATH,
            listName = OrderResponse.ORDER_RESPONSE_LIST_KEY,
            action = arrayListOf(),
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is OrderResponse) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfOrderPackage(): Unit = runTest {

        var r: ListResponse<OrderPackage>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<OrderPackage>(
            objPath = ApiRequest.ORDER_PACKAGE_PATH,
            listName = OrderPackage.ORDER_PACKAGE_LIST_KEY,
            action = arrayListOf(),
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is OrderPackage) assert(true)
        else assert(false)
    }

    @Test
    fun getListOfItemCodes(): Unit = runTest {

        var r: ListResponse<ItemCode>? = ListResponse()
        val lock = CountDownLatch(1)

        apiRequest.getListOf<ItemCode>(
            objPath = ITEM_CODE_PATH,
            listName = ItemCode.ITEM_CODES_LIST_KEY,
            action = GetItemCode.defaultAction,
            filter = arrayListOf(),
            pagination = ApiPaginationParam.defaultPagination,
            callback = {
                if (it.response != null) r = it.response
                if (it.onEvent != null) sendEvent(it.onEvent)
                lock.countDown()
            }
        )

        lock.await(sv.connectionTimeout.toLong(), TimeUnit.MILLISECONDS)

        println(r)

        if (r != null && r?.items?.first() is ItemCode) assert(true)
        else assert(false)
    }

    private fun sendEvent(event: SnackBarEventData?) {
        if (event != null) sendEvent(event.text, event.snackBarType)
    }

    private fun sendEvent(msg: String, type: SnackBarType) {
        println("${type.description}: $msg")
    }

    private fun koinAppModule() = module {
        single { PreferenceManager.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext()) }
        single { SettingsRepository() }

        viewModel { SettingsViewModel() }

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
            HttpClient(OkHttp) {
                engine {
                    config {
                        followRedirects(true)
                        connectTimeout(settingsVm.connectionTimeout.toLong(), TimeUnit.SECONDS)
                        proxy(currentProxy)
                    }
                }
                install(ContentNegotiation) {
                    json(get())
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

        /** API Version 2 */
        single { APIServiceImpl() }
        single { ApiRequest() }
    }
}