package com.dacosys.warehouseCounter.data.ktor.v2.impl

import arrow.core.Either
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.httpClient
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.ErrorResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.Links
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.Meta
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeCodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderMovePayload
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_FILTER
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam.Companion.ACTION_FILTER_LIKE
import com.dacosys.warehouseCounter.data.ktor.v2.service.APIResponse
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.net.URL

class ApiRequest {
    /**
     * Procesa una respuesta HTTP que contiene un objeto de tipo [T] de acuerdo con el formato
     * especificado y realiza acciones personalizadas en caso de éxito o error.
     *
     * @param response La respuesta HTTP que se va a procesar.
     * @param success La función que se ejecutará cuando la respuesta se procese correctamente.
     *                Debe tomar un objeto [T] y devolver un objeto [T].
     * @param error La función que se ejecutará cuando se produzca un error en el procesamiento de la respuesta.
     *              Debe tomar un objeto [ErrorResponse] y devolver un objeto [ErrorResponse].
     * @return Un [Either] que contiene un objeto [T] en caso de éxito o un objeto [ErrorResponse] en caso de error.
     *
     * @reified T El tipo de objeto esperado en la respuesta.
     */
    suspend inline fun <reified T : Any> handleHttpResponse(
        response: HttpResponse,
        success: (T) -> T,
        error: (ErrorResponse) -> ErrorResponse,
    ): Either<T, ErrorResponse> {

        val content = response.bodyAsText()

        if (BuildConfig.DEBUG) {
            println(response.status.description)
            println(content)
        }

        return try {
            if (content.isEmpty()) {
                Either.Right(error(ErrorResponse(context.getString(R.string.unknown_error))))
            } else {
                if (response.status in HttpStatusCode.OK..HttpStatusCode.MultiStatus) {
                    Either.Left(success(json.decodeFromString<T>(content)))
                } else {
                    Either.Right(error(json.decodeFromString<ErrorResponse>(content)))
                }
            }
        } catch (e: JsonConvertException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.json_conversion_failed))))
        } catch (e: SerializationException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.serialization_failed))))
        } catch (e: NullPointerException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.null_error))))
        } catch (e: Exception) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.unknown_error))))
        }
    }

    /**
     * Procesa una respuesta HTTP que contiene una lista de elementos de tipo [T] de acuerdo con el formato
     * especificado y realiza acciones personalizadas en caso de éxito o error.
     *
     * @param response La respuesta HTTP que se va a procesar.
     * @param success La función que se ejecutará cuando la respuesta se procese correctamente.
     *                Debe tomar una lista de elementos [T] y devolver una lista de elementos [T].
     * @param error La función que se ejecutará cuando se produzca un error en el procesamiento de la respuesta.
     *              Debe tomar un objeto [ErrorResponse] y devolver un objeto [ErrorResponse].
     * @param listKey La clave que se utilizará para extraer la lista de elementos de la respuesta HTTP.
     * @return Un [Either] que contiene una lista de elementos [T] en caso de éxito o un objeto [ErrorResponse] en caso de error.
     *
     * @reified T El tipo de elemento esperado en la lista.
     */
    suspend inline fun <reified T : Any> handleHttpListResponse(
        response: HttpResponse,
        success: (List<T>) -> List<T>,
        error: (ErrorResponse) -> ErrorResponse,
        listKey: String,
    ): Either<List<T>, ErrorResponse> {

        val content = response.bodyAsText()

        if (BuildConfig.DEBUG) {
            println(response.status.description)
            println(content)
        }

        return try {
            if (content.isEmpty()) {
                Either.Right(error(ErrorResponse(context.getString(R.string.unknown_error))))
            } else {
                if (response.status in HttpStatusCode.OK..HttpStatusCode.MultiStatus) {
                    val jsonObject = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val r = mutableListOf<T>()
                    for (j in jsonObject) {
                        when (j.key) {
                            listKey -> {
                                val collection = j.value as JsonArray
                                collection.mapTo(r) { it2 -> json.decodeFromJsonElement(it2) }
                            }
                        }
                    }
                    Either.Left(success(r))
                } else {
                    Either.Right(error(json.decodeFromString<ErrorResponse>(content)))
                }
            }
        } catch (e: JsonConvertException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.json_conversion_failed))))
        } catch (e: SerializationException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.serialization_failed))))
        } catch (e: NullPointerException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.null_error))))
        } catch (e: Exception) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.unknown_error))))
        }
    }

    /**
     * Procesa una respuesta HTTP que contiene una lista de elementos de tipo [T] de acuerdo con el formato
     * especificado y realiza acciones personalizadas en caso de éxito o error.
     *
     * @param response La respuesta HTTP que se va a procesar.
     * @param success La función que se ejecutará cuando la respuesta se procese correctamente.
     *                Debe tomar un objeto [ListResponse] de elementos [T] y devolver un objeto [ListResponse] de elementos [T].
     * @param error La función que se ejecutará cuando se produzca un error en el procesamiento de la respuesta.
     *              Debe tomar un objeto [ErrorResponse] y devolver un objeto [ErrorResponse].
     * @param listKey La clave que se utilizará para extraer la lista de elementos de la respuesta HTTP.
     * @return Un [Either] que contiene un objeto [ListResponse] de elementos [T] en caso de éxito o un objeto [ErrorResponse] en caso de error.
     *
     * @reified T El tipo de elemento esperado en la lista.
     */
    suspend inline fun <reified T : Any> handleHttpListResponseResponse(
        response: HttpResponse,
        success: (ListResponse<T>) -> ListResponse<T>,
        error: (ErrorResponse) -> ErrorResponse,
        listKey: String,
    ): Either<ListResponse<T>, ErrorResponse> {

        val content = response.bodyAsText()

        if (BuildConfig.DEBUG) {
            println(response.status.description)
            println(content)
        }

        return try {
            if (content.isEmpty()) {
                Either.Right(error(ErrorResponse(context.getString(R.string.unknown_error))))
            } else {
                if (response.status in HttpStatusCode.OK..HttpStatusCode.MultiStatus) {
                    val jsonObject = json.parseToJsonElement(response.bodyAsText()).jsonObject
                    val r = ListResponse<T>()

                    for (j in jsonObject) {
                        when (j.key) {
                            ListResponse.LINKS_KEY -> {
                                r.links = json.decodeFromJsonElement<Links>(j.value)
                            }

                            ListResponse.META_KEY -> {
                                r.meta = json.decodeFromJsonElement<Meta>(j.value)
                            }

                            listKey -> {
                                val collection = j.value as JsonArray
                                val tList = mutableListOf<T>()
                                collection.mapTo(tList) { it2 -> json.decodeFromJsonElement(it2) }
                                r.items = ArrayList(tList)
                            }
                        }
                    }
                    Either.Left(success(r))
                } else {
                    Either.Right(error(json.decodeFromString<ErrorResponse>(content)))
                }
            }
        } catch (e: JsonConvertException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.json_conversion_failed))))
        } catch (e: SerializationException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.serialization_failed))))
        } catch (e: NullPointerException) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.null_error))))
        } catch (e: Exception) {
            Either.Right(error(ErrorResponse(e.message ?: context.getString(R.string.unknown_error))))
        }
    }

    /**
     * Obtiene una lista de códigos de barras relacionados con el objeto especificado en [objPath] utilizando los parámetros proporcionados en [params].
     *
     * @param objPath la ruta al objeto para el cual se desea obtener códigos de barras relacionados.
     * @param params los parámetros que controlan la consulta de códigos de barras.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de obtención de códigos de barras.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es una lista de objetos [Barcode].
     */
    suspend fun getBarcodeOf(objPath: String, params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit) {

        suspend fun httpResponse(): HttpResponse {
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
            return response
        }

        val result = handleHttpListResponse<Barcode>(
            response = httpResponse(),
            listKey = Barcode.BARCODE_LIST_KEY,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Obtiene una lista de códigos de barras relacionados con el objeto especificado en [objPath] utilizando el código de barras proporcionado en [params].
     *
     * @param objPath la ruta al objeto para el cual se desea obtener códigos de barras relacionados.
     * @param params los parámetros que contienen el código de barras para la consulta.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de obtención de códigos de barras.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es una lista de objetos [Barcode].
     */
    suspend fun getBarcodeByCodeOf(
        objPath: String, params: BarcodeCodeParam, callback: (APIResponse<List<Barcode>>) -> Unit
    ) {
        suspend fun httpResponse(): HttpResponse {
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
            return response
        }

        val result = handleHttpListResponse<Barcode>(
            response = httpResponse(),
            listKey = Barcode.BARCODE_LIST_KEY,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Obtiene una lista de objetos del tipo especificado [T] desde el recurso en [objPath], opcionalmente aplicando acciones y filtros.
     *
     * @param T el tipo de objeto que se espera recibir en la lista.
     * @param objPath la ruta al recurso desde el cual se obtendrán los objetos.
     * @param listName el nombre de la lista en la respuesta que contiene los objetos.
     * @param action una lista de parámetros de acción que pueden aplicarse a la solicitud (opcional).
     * @param filter una lista de parámetros de filtro que controlan la consulta de objetos (opcional).
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de obtención de la lista de objetos.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es una lista de objetos [T].
     */
    suspend inline fun <reified T : Any> getListOf(
        objPath: String,
        listName: String,
        action: ArrayList<ApiActionParam> = arrayListOf(),
        filter: ArrayList<ApiFilterParam> = arrayListOf(),
        callback: (APIResponse<ListResponse<T>>) -> Unit
    ) {
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
                if (col.isNotEmpty()) this.append("$ACTION_FILTER[${col}]${like}", value)
            }
        }

        val urlComplete = "${url.path}/$VERSION_PATH/$objPath/"
        if (BuildConfig.DEBUG) {
            println("URL: $urlComplete")
            println("PARAMS: $params")
        }

        /** HTTP Get function */
        val httpResponse = httpClient.get {
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

        val result = handleHttpListResponseResponse<T>(
            response = httpResponse,
            listKey = listName,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Crea un objeto del tipo especificado [T] en el recurso especificado por [objPath] utilizando los datos proporcionados en [payload].
     *
     * @param T el tipo de objeto que se espera recibir como respuesta.
     * @param objPath la ruta al recurso donde se creará el objeto.
     * @param payload un objeto que contiene los datos necesarios para crear el objeto en el recurso.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de creación.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación.
     */
    suspend inline fun <reified T : Any> create(
        objPath: String, payload: Any, callback: (APIResponse<T>) -> Unit
    ) {
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

        val result = handleHttpResponse<T>(
            response = response,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Actualiza un objeto del tipo especificado [T] en el recurso especificado por [objPath] utilizando los datos proporcionados en [payload].
     *
     * @param T el tipo de objeto que se espera recibir como respuesta.
     * @param objPath la ruta al recurso donde se actualizará el objeto.
     * @param payload un objeto que contiene los datos necesarios para actualizar el objeto en el recurso.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de actualización.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación.
     */
    suspend inline fun <reified T : Any> update(
        objPath: String, payload: Any, callback: (APIResponse<T>) -> Unit
    ) {
        val url = URL(apiUrl)

        /** HTTP Post function */
        val response = httpClient.put {
            /** Set a Basic auth */
            basicAuth(
                username = Statics.currentUserName, password = Statics.currentPass
            )
            /** Set the API URL and parameters */
            url {
                protocol = if (url.protocol.equals("HTTP", true)) URLProtocol.HTTP
                else URLProtocol.HTTPS
                host = url.host
                path("${url.path}/$VERSION_PATH/$objPath/$UPDATE_PATH")
            }
            /** Payload content */
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        val result = handleHttpResponse<T>(
            response = response,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Mueve una orden utilizando los datos proporcionados en [payload].
     *
     * @param payload un objeto [OrderMovePayload] que contiene la información necesaria para mover la orden.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de mover la orden.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación.
     */
    suspend fun moveOrder(
        payload: OrderMovePayload, callback: (APIResponse<OrderResponse>) -> Unit
    ) {
        suspend fun httpResponse(): HttpResponse {
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

            return response
        }

        val result = handleHttpResponse<OrderResponse>(
            response = httpResponse(),
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Envía un código de artículo utilizando el objeto [payload] y procesa la respuesta.
     *
     * @param payload un objeto que contiene el código de artículo a enviar.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de envío de código de artículo.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es un objeto [ItemCodeResponse].
     */
    suspend fun sendItemCode(
        payload: ItemCodePayload, callback: (APIResponse<ItemCodeResponse>) -> Unit
    ) {
        suspend fun httpResponse(): HttpResponse {
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
            return response
        }

        val result = handleHttpResponse<ItemCodeResponse>(
            response = httpResponse(),
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Obtiene y muestra información detallada de un objeto específico del tipo [T] en [objPath] con el ID proporcionado.
     *
     * @param T el tipo de objeto que se espera recibir y mostrar.
     * @param objPath la ruta al objeto que se desea ver en detalle.
     * @param id el ID del objeto que se desea ver en detalle.
     * @param action una lista de parámetros de acción que pueden aplicarse a la solicitud (opcional).
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de vista del objeto.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es un objeto [T].
     */
    suspend inline fun <reified T : Any> view(
        objPath: String, id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<T>) -> Unit
    ) {
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

        val result = handleHttpResponse<T>(
            response = response,
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Obtiene información de la base de datos correspondiente a la versión especificada.
     *
     * @param version la versión de la base de datos para la que se desea obtener información.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de obtención de información de la base de datos.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es un objeto [DatabaseData].
     */
    suspend fun getDatabase(version: String, callback: (APIResponse<DatabaseData>) -> Unit) {
        suspend fun httpResponse(): HttpResponse {
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
            return response
        }

        val result = handleHttpResponse<DatabaseData>(
            response = httpResponse(),
            success = { it },
            error = { it }
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
    }

    /**
     * Obtiene una lista de ubicaciones de pedidos utilizando los filtros proporcionados en [filter].
     *
     * @param filter una lista de parámetros de filtro que controlan la consulta de ubicaciones de pedidos.
     * @param callback una función de devolución de llamada que se ejecutará cuando se complete la operación de obtención de ubicaciones de pedidos.
     *   El [APIResponse] pasado a esta función de devolución de llamada contendrá los resultados de la operación, que es una lista de objetos [OrderLocation].
     */
    suspend inline fun <reified T : Any> getListOf(
        listKey: String, filter: ArrayList<ApiFilterParam>, callback: (APIResponse<List<T>>) -> Unit
    ) {
        val url = URL(apiUrl)

        /** We build the parameters (query actions) */
        val params = Parameters.build {
            filter.forEach {
                val col = it.columnName
                val like = if (it.like) "[$ACTION_FILTER_LIKE]" else ""
                val value = it.value
                if (col.isNotEmpty()) this.append("$ACTION_FILTER[${col}]${like}", value)
            }
        }

        val urlComplete = "${url.path}/$VERSION_PATH/$ORDER_LOCATION_PATH"
        if (BuildConfig.DEBUG) {
            println("URL: $urlComplete")
            println("PARAMS: $params")
        }

        val httpResponse = httpClient.get {
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

        val result = handleHttpListResponse<T>(
            response = httpResponse,
            success = { it },
            error = { it },
            listKey = listKey
        )

        result.fold(
            ifLeft = {
                callback(APIResponse(it))
            },
            ifRight = {
                val r = "${it.name}${Statics.lineSeparator}${it.message}".trim()
                println(r)
                callback(APIResponse(onEvent = SnackBarEventData(r, SnackBarType.ERROR)))
            })
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
        const val UPDATE_PATH = "update"

        fun validUrl(): Boolean {
            val url = URL(apiUrl)
            return url.protocol.isNotEmpty() && url.host.isNotEmpty()
        }
    }
}
