package com.dacosys.warehouseCounter.data.ktor.v2.impl

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeCodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderLocation
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderMovePayload
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderPackage
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderUpdatePayload
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.BARCODE_LABEL_TEMPLATE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ITEM_CODE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ITEM_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ORDER_PACKAGE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.ORDER_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.RACK_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_AREA_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_PATH
import com.dacosys.warehouseCounter.data.ktor.v2.service.APIResponse
import com.dacosys.warehouseCounter.data.ktor.v2.service.APIService

class APIServiceImpl : APIService {
    /**
     * Get the database info for the current user
     *
     * @param version Version of the Database
     * @param callback [APIResponse] of [DatabaseData] with the location of the database on the server.
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/database/)
     * [index GET](http://localhost:8002/v2/database/location-v2)
     */
    override suspend fun getDatabase(version: String, callback: (APIResponse<DatabaseData>) -> Unit) {
        apiRequest.getDatabase(version = version, callback = callback)
    }

    /**
     * View a [Rack] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [Rack].
     */
    override suspend fun viewRack(id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<Rack>) -> Unit) {
        apiRequest.view<Rack>(
            objPath = RACK_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[Rack]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/rack/)
     * [index GET](http://localhost:8002/v2/rack/)
     */
    override suspend fun getRack(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Rack>>) -> Unit
    ) {
        apiRequest.getListOf<Rack>(
            objPath = RACK_PATH,
            listName = Rack.RACK_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [Rack] and Template.
     *
     * @param params Barcode request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/rack)
     * [POST](http://localhost:8002/v2/rack/barcode)
     */
    override suspend fun getRackBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit) {
        apiRequest.getBarcodeOf(
            objPath = RACK_PATH, params = params, callback = callback
        )
    }

    /**
     * View a [WarehouseArea] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [WarehouseArea].
     */
    override suspend fun viewWarehouseArea(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<WarehouseArea>) -> Unit
    ) {
        apiRequest.view<WarehouseArea>(
            objPath = WAREHOUSE_AREA_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[WarehouseArea]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [index GET](http://localhost:8002/v2/warehouse_area/)
     */
    override suspend fun getWarehouseArea(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<WarehouseArea>>) -> Unit,
    ) {
        apiRequest.getListOf<WarehouseArea>(
            objPath = WAREHOUSE_AREA_PATH,
            listName = WarehouseArea.WAREHOUSE_AREA_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [WarehouseArea] and Template.
     *
     * @param params Barcode request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [POST](http://localhost:8002/v2/warehouse-area/barcode)
     */
    override suspend fun getWarehouseAreaBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit) {
        apiRequest.getBarcodeOf(
            objPath = WAREHOUSE_AREA_PATH, params = params, callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired code of [WarehouseArea] and Template.
     *
     * @param params Barcode code request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [POST](http://localhost:8002/v2/order-package/barcode-code)
     */
    override suspend fun getWarehouseAreaBarcodeByCode(
        params: BarcodeCodeParam, callback: (APIResponse<List<Barcode>>) -> Unit
    ) {
        apiRequest.getBarcodeByCodeOf(
            objPath = WAREHOUSE_AREA_PATH, params = params, callback = callback
        )
    }

    /**
     * View a [Warehouse] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [Warehouse].
     */
    override suspend fun viewWarehouse(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<Warehouse>) -> Unit
    ) {
        apiRequest.view<Warehouse>(
            objPath = WAREHOUSE_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[Warehouse]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse/)
     * [index GET](http://localhost:8002/v2/warehouse/)
     */
    override suspend fun getWarehouse(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Warehouse>>) -> Unit
    ) {
        apiRequest.getListOf<Warehouse>(
            objPath = WAREHOUSE_PATH,
            listName = Warehouse.WAREHOUSE_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * View a [BarcodeLabelTemplate] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [BarcodeLabelTemplate].
     */
    override suspend fun viewBarcodeLabelTemplate(
        id: Long, action: ArrayList<ApiActionParam>,
        callback: (APIResponse<BarcodeLabelTemplate>) -> Unit,
    ) {
        apiRequest.view<BarcodeLabelTemplate>(
            objPath = BARCODE_LABEL_TEMPLATE_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[BarcodeLabelTemplate]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/barcode-label-template)
     * [POST](http://localhost:8002/v2/rack/barcode-label-template)
     */
    override suspend fun getBarcodeLabelTemplate(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<BarcodeLabelTemplate>>) -> Unit
    ) {
        apiRequest.getListOf<BarcodeLabelTemplate>(
            objPath = BARCODE_LABEL_TEMPLATE_PATH,
            listName = BarcodeLabelTemplate.BARCODE_LABEL_TEMPLATE_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * View a [OrderResponse] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [OrderResponse].
     */
    override suspend fun viewOrder(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<OrderResponse>) -> Unit
    ) {
        apiRequest.view<OrderResponse>(
            objPath = ORDER_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Create a new [OrderResponse]
     *
     * @param payload Load object: [OrderRequest] containing the order, contents, scan records, etc.
     * @param callback [APIResponse] of [OrderResponse] with the created order
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/create)
     */
    override suspend fun createOrder(payload: OrderRequest, callback: (APIResponse<OrderResponse>) -> Unit) {
        apiRequest.create<OrderResponse>(
            objPath = ORDER_PATH, payload = payload, callback = callback
        )
    }

    /**
     * Move an order to a desired destination both defined in [OrderMovePayload]
     *
     * @param payload Load object: [OrderMovePayload] containing the order and the destination IDs.
     * @param callback [APIResponse] of [OrderResponse] with the moved order.
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/move)
     */
    override suspend fun moveOrder(payload: OrderMovePayload, callback: (APIResponse<OrderResponse>) -> Unit) {
        apiRequest.moveOrder(
            payload = payload, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[OrderResponse]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired [ListResponse] of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [index GET](http://localhost:8002/v2/order/)
     */
    override suspend fun getOrder(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<OrderResponse>>) -> Unit
    ) {
        apiRequest.getListOf<OrderResponse>(
            objPath = ORDER_PATH,
            listName = OrderResponse.ORDER_RESPONSE_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Update a new [OrderResponse]
     *
     * @param id Object ID.
     * @param payload Load object: [OrderUpdatePayload] containing the necessary order information.
     * @param callback [APIResponse] of [OrderResponse] with the updated order
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/update)
     */
    override suspend fun updateOrder(
        id: Long,
        payload: OrderUpdatePayload,
        callback: (APIResponse<OrderResponse>) -> Unit
    ) {
        apiRequest.update<OrderResponse>(
            objPath = ORDER_PATH, id = id, payload = payload, callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [OrderResponse] and Template.
     *
     * @param params Barcode request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/barcode)
     */
    override suspend fun getOrderBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit) {
        apiRequest.getBarcodeOf(
            objPath = ORDER_PATH, params = params, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[OrderPackage]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order_package/)
     * [index GET](http://localhost:8002/v2/order-package/)
     */
    override suspend fun getOrderPackage(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<OrderPackage>>) -> Unit,
    ) {
        apiRequest.getListOf<OrderPackage>(
            objPath = ORDER_PACKAGE_PATH,
            listName = OrderPackage.ORDER_PACKAGE_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Send an item code relation
     *
     * @param [payload] [ItemCodePayload] object with the item ID, the code and the represented amount.
     * @param [callback] [APIResponse] of [ItemCodeResponse] with diverse data
     */
    override suspend fun sendItemCode(payload: ItemCodePayload, callback: (APIResponse<ItemCodeResponse>) -> Unit) {
        apiRequest.sendItemCode(payload = payload, callback = { callback(it) })
    }

    /**
     * View a [Item] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [Item].
     */
    override suspend fun viewItem(
        id: String,
        action: ArrayList<ApiActionParam>,
        callback: (APIResponse<Item>) -> Unit
    ) {
        apiRequest.view<Item>(
            objPath = ITEM_PATH, id = id, action = action, callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[Item]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [index GET](http://localhost:8002/v2/item/)
     */
    override suspend fun getItem(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Item>>) -> Unit,
    ) {
        apiRequest.getListOf<Item>(
            objPath = ITEM_PATH,
            listName = Item.ITEM_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [Item] and Template.
     *
     * @param params Barcode request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [POST](http://localhost:8002/v2/warehouse-area/barcode)
     */
    override suspend fun getItemBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit) {
        apiRequest.getBarcodeOf(
            objPath = ITEM_PATH, params = params, callback = callback
        )
    }

    /**
     * Returns a list of [Barcode] of the desired code of [Item] and Template.
     *
     * @param params Barcode code request parameters
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [POST](http://localhost:8002/v2/order-package/barcode-code)
     */
    override suspend fun getItemBarcodeByCode(
        params: BarcodeCodeParam, callback: (APIResponse<List<Barcode>>) -> Unit
    ) {
        apiRequest.getBarcodeByCodeOf(
            objPath = ITEM_PATH, params = params, callback = callback
        )
    }

    /**
     * Get a [List]<[OrderLocation]> through a callback
     *
     * @param filter List of parameters
     * @param pagination Pagination
     * @param callback With the [APIResponse] of a desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order_location/)
     * [index GET](http://localhost:8002/v2/order_location)
     */
    override suspend fun getOrderLocation(
        filter: ArrayList<ApiFilterParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<List<OrderLocation>>) -> Unit
    ) {
        apiRequest.getListOf(
            listKey = OrderLocation.ORDER_LOCATION_LIST_KEY,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * Get a [ListResponse]<[ItemCode]> through a callback
     *
     * @param filter List of filters
     * @param action List of parameters
     * @param pagination Pagination
     * @param callback [APIResponse] of desired list of objects
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item-code/)
     * [index GET](http://localhost:8002/v2/item-code/)
     */
    override suspend fun getItemCode(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<ItemCode>>) -> Unit,
    ) {
        apiRequest.getListOf<ItemCode>(
            objPath = ITEM_CODE_PATH,
            listName = ItemCode.ITEM_CODES_LIST_KEY,
            action = action,
            filter = filter,
            pagination = pagination,
            callback = callback
        )
    }

    /**
     * View a [ItemCode] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback [APIResponse] of [ItemCode].
     */
    override suspend fun viewItemCode(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (APIResponse<ItemCode>) -> Unit
    ) {
        apiRequest.view<ItemCode>(
            objPath = ITEM_PATH, id = id, action = action, callback = callback
        )
    }
}
