package com.dacosys.warehouseCounter.ktor.v2.impl

import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeCodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderLocation.CREATOR.ORDER_LOCATION_LIST_KEY
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderMovePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.ACTION_FILTER
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam.Companion.ACTION_FILTER_LIKE
import com.dacosys.warehouseCounter.misc.Statics
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URL

class ApiRequest {
    /**
     * Get the barcodes for the desired list of objects IDs.
     *
     * @param objPath Partial path of the object functions in the API.
     * @param params [BarcodeParam] with the list of IDs, the template and the printer options.
     * @return [ListResponse]<[Barcode]>
     */
    suspend fun getBarcodeOf(objPath: String, params: BarcodeParam): List<Barcode> {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.post {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$objPath/$BARCODE_PATH")
            }
            /** Body content */
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        println(response.status.description)
        val r = ArrayList<Barcode>()
        try {
            when (response.status.value) {
                in 200..299 -> {
                    val bodyStr = response.bodyAsText()
                    if (BuildConfig.DEBUG) println(bodyStr)

                    val jsonObject = json.parseToJsonElement(bodyStr).jsonObject

                    for (j in jsonObject) {
                        when (j.key) {
                            Barcode.BARCODE_LIST_KEY -> {
                                val collection = j.value as JsonArray
                                collection.mapTo(r) { json.decodeFromJsonElement(it) }
                            }
                        }
                    }
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Get the barcodes for the desired code object.
     *
     * @param objPath Partial path of the object functions in the API.
     * @param params [BarcodeCodeParam] with the code, the template and the printer options.
     * @return [ListResponse]<[Barcode]>
     */
    suspend fun getBarcodeByCodeOf(objPath: String, params: BarcodeCodeParam): List<Barcode> {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.post {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$objPath/$BARCODE_CODE_PATH")
            }
            /** Body content */
            contentType(ContentType.Application.Json)
            setBody(params)
        }

        println(response.status.description)
        val r = ArrayList<Barcode>()
        try {
            when (response.status.value) {
                in 200..299 -> {
                    val bodyStr = response.bodyAsText()
                    if (BuildConfig.DEBUG) println(bodyStr)

                    val jsonObject = json.parseToJsonElement(bodyStr).jsonObject

                    for (j in jsonObject) {
                        when (j.key) {
                            Barcode.BARCODE_LIST_KEY -> {
                                val collection = j.value as JsonArray
                                collection.mapTo(r) { json.decodeFromJsonElement(it) }
                            }
                        }
                    }
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Get a list of [T]
     *
     * @param T DTO Clazz
     * @param objPath Partial path of the object functions in the API.
     * @param listName Key of the item list in the Json.
     * @param action API actions
     * @param filter API filters (optional)
     * @return [ListResponse]<[T]>
     */
    suspend inline fun <reified T> getListOf(
        objPath: String,
        listName: String,
        action: ArrayList<ApiActionParam> = arrayListOf(),
        filter: ArrayList<ApiFilterParam> = arrayListOf()
    ): ListResponse<T> {
        val url = URL(apiUrl)

        /** We build the parameters (query actions) */
        val params = Parameters.build {
            action.forEach {
                if (it.action.isNotEmpty()) append(
                    it.action, it.extension.joinToString(EXT_SEPARATOR)
                )
            }
            filter.forEach {
                val col = it.columnName
                val like = if (it.like) "[$ACTION_FILTER_LIKE]" else ""
                val value = it.value
                if (col.isNotEmpty())
                    this.append("$ACTION_FILTER[${col}]${like}", value)
            }
        }

        val urlComplete = "${url.path}/$VERSION_PATH/$objPath/"
        if (BuildConfig.DEBUG) {
            println("URL: $urlComplete")
            println("PARAMS: $params")
        }

        /** HTTP Get function */
        val response = httpClient.get {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path(urlComplete)
                parameters.appendAll(params)
            }
        }

        /** Our response */
        val r = ListResponse<T>()
        try {
            println(response.status.description)
            when (response.status.value) {
                in 200..299 -> {
                    val bodyStr = response.bodyAsText()
                    if (BuildConfig.DEBUG) println(bodyStr)

                    val jsonObject = json.parseToJsonElement(bodyStr).jsonObject

                    for (j in jsonObject) {
                        when (j.key) {
                            ListResponse.LINKS_KEY -> {
                                r.links = json.decodeFromJsonElement(j.value)
                            }

                            ListResponse.META_KEY -> {
                                r.meta = json.decodeFromJsonElement(j.value)
                            }

                            listName -> {
                                val collection = j.value as JsonArray
                                collection.mapTo(r.items) { json.decodeFromJsonElement(it) }
                            }
                        }
                    }
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        } catch (e: Exception) {
            println(e.message)
        }
        return r
    }

    /**
     * Create a new object of the desired [T] type.
     *
     * @param T DTO Clazz of the response
     * @param objPath Partial path of the object functions in the API.
     * @param payload Payload object
     * @return [T] Object created
     */
    suspend inline fun <reified T> create(
        objPath: String, payload: Any,
    ): T {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.post {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$objPath/$CREATE_PATH")
            }
            /** Payload content */
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        println(response.status.description)
        val entityClass: Class<T> = T::class.java
        var r: T = entityClass.getDeclaredConstructor().newInstance()

        try {
            when (response.status.value) {
                in 200..299 -> {
                    r = response.body<T>()
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Move an order to a desired destination defined in the [payload].
     *
     * @param [payload] Payload object with the order ID, the destination ID and other optional parameters.
     * @return [OrderResponse] with the order moved
     */
    suspend fun moveOrder(payload: OrderMovePayload): OrderResponse {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.post {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$ORDER_PATH/$MOVE_PATH")
            }
            /** Payload content */
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        println(response.status.description)
        var r = OrderResponse()

        try {
            when (response.status.value) {
                in 200..299 -> {
                    r = response.body<OrderResponse>()
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Send an item code relation defined in the [payload].
     *
     * @param [payload] Payload object with the item ID, the code and the represented amount.
     * @return [ItemCodeResponse] with diverse data
     */
    suspend fun sendItemCode(payload: ItemCodePayload): ItemCodeResponse? {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.post {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$ITEM_CODE_PATH/$CREATE_PATH")
            }
            /** Payload content */
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        println(response.status.description)
        var r: ItemCodeResponse? = null

        try {
            when (response.status.value) {
                in 200..299 -> {
                    r = response.body<ItemCodeResponse>()
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * View an object defined by [T] and ID.
     *
     * @param T DTO Clazz of the response. **Make sure the response class has a parameterless constructor**.
     * @param objPath Partial path of the object functions in the API.
     * @param id Object ID.
     * @param action List of parameters.
     * @return The desired [T] object.
     */
    suspend inline fun <reified T> view(objPath: String, id: Long, action: ArrayList<ApiActionParam>): T {
        val url = URL(apiUrl)
        val columnName = "id"

        /** We build the parameters (query actions) */
        val params = Parameters.build {
            action.forEach {
                if (it.action.isNotEmpty()) append(
                    it.action, it.extension.joinToString(EXT_SEPARATOR)
                )
            }
            append(columnName, id.toString())
        }

        val urlComplete = "${url.path}/$VERSION_PATH/$objPath/$VIEW_PATH"
        if (BuildConfig.DEBUG) {
            println("URL: $urlComplete")
            println("PARAMS: $params")
        }

        /** HTTP Get function */
        val response = httpClient.get {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path(urlComplete)
                parameters.appendAll(params)
            }
        }

        println(response.status.description)

        /** The generic class is required to have a parameterless constructor. */
        val entityClass: Class<T> = T::class.java
        var r: T = entityClass.getDeclaredConstructor().newInstance()

        try {
            when (response.status.value) {
                in 200..299 -> {
                    r = response.body<T>()
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Get the database data for the current user
     * @param version Version of the Database (empty value is for V1)
     * @return [DatabaseData] with the client data
     */
    suspend fun getDatabase(version: String): DatabaseData {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.get {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$DATABASE_PATH/$DB_PATH$version")
            }
        }

        println(response.status.description)
        var r = DatabaseData()

        try {
            when (response.status.value) {
                in 200..299 -> {
                    r = response.body<DatabaseData>()
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        }
        return r
    }

    /**
     * Get a list of order location
     *
     * @param filter API Filters
     * @return List of [OrderLocation]
     */
    suspend fun getListOfOrderLocation(
        filter: ArrayList<ApiFilterParam>,
    ): List<OrderLocation> {
        val url = URL(apiUrl)

        /** We build the parameters (query actions) */
        val params = Parameters.build {
            filter.forEach {
                val col = it.columnName
                val like = if (it.like) "[$ACTION_FILTER_LIKE]" else ""
                val value = it.value
                if (col.isNotEmpty())
                    this.append("$ACTION_FILTER[${col}]${like}", value)
            }
        }

        val urlComplete = "${url.path}/$VERSION_PATH/$ORDER_LOCATION_PATH"
        if (BuildConfig.DEBUG) {
            println("URL: $urlComplete")
            println("PARAMS: $params")
        }

        /** HTTP Get function */
        val response = httpClient.get {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path(urlComplete)
                parameters.appendAll(params)
            }
        }

        /** Our response */
        val r: ArrayList<OrderLocation> = arrayListOf()
        try {
            println(response.status.description)
            when (response.status.value) {
                in 200..299 -> {
                    val bodyStr = response.bodyAsText()
                    if (BuildConfig.DEBUG) println(bodyStr)

                    val jsonObject = json.parseToJsonElement(bodyStr).jsonObject
                    if (jsonObject.containsKey(ORDER_LOCATION_LIST_KEY)) {
                        val jsonArray = jsonObject[ORDER_LOCATION_LIST_KEY]?.jsonArray

                        if (jsonArray != null) {
                            for (j in jsonArray.listIterator()) {
                                r.add(json.decodeFromJsonElement(j))
                            }
                        }
                    }
                }

                422 -> {}
                401 -> {}
            }
        } catch (e: JsonConvertException) {
            println(e.message)
        } catch (e: SerializationException) {
            println(e.message)
        } catch (e: NullPointerException) {
            println(e.message)
        } catch (e: Exception) {
            println(e.message)
        }

        return r.toList()
    }

    companion object {
        val apiUrl by lazy { WarehouseCounterApp.settingViewModel.urlPanel }

        const val EXT_SEPARATOR = ","

        const val VERSION_PATH = "v2"

        const val ITEM_PATH = "item"
        const val ITEM_CODE_PATH = "item-code"
        const val ORDER_PATH = "order"
        const val RACK_PATH = "rack"
        const val WAREHOUSE_PATH = "warehouse"
        const val WAREHOUSE_AREA_PATH = "warehouse-area"
        const val ORDER_PACKAGE_PATH = "order-package"
        const val ORDER_LOCATION_PATH = "order-location"

        const val DATABASE_PATH = "database"
        const val DB_PATH = "location"

        const val BARCODE_PATH = "barcode"
        const val BARCODE_CODE_PATH = "barcode-code"
        const val BARCODE_LABEL_TEMPLATE_PATH = "barcode-label-template"

        const val VIEW_PATH = "view"
        const val MOVE_PATH = "move"
        const val CREATE_PATH = "create"

        fun validUrl(): Boolean {
            val url = URL(apiUrl)
            return url.protocol.isNotEmpty() && url.host.isNotEmpty()
        }
    }
}
