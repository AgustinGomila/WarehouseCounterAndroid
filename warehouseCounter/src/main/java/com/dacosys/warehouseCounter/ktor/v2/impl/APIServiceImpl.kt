package com.dacosys.warehouseCounter.ktor.v2.impl

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.apiRequest
import com.dacosys.warehouseCounter.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeCodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.ktor.v2.dto.order.*
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.ITEM_PATH
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.ORDER_PACKAGE_PATH
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.ORDER_PATH
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.RACK_PATH
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_AREA_PATH
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest.Companion.WAREHOUSE_PATH
import com.dacosys.warehouseCounter.ktor.v2.service.APIService

class APIServiceImpl : APIService {
    /**
     * Get the database info for the current user
     *
     * @param version Version of the Database
     * @param callback [DatabaseData] with the location of the database on the server.
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/database/)
     * [index GET](http://localhost:8002/v2/database/location-v2)
     */
    override suspend fun getDatabase(version: String, callback: (DatabaseData?) -> Unit) {
        callback(
            apiRequest.getDatabase(version)
        )
    }

    /**
     * View a [Rack] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback Desired [Rack].
     */
    override suspend fun viewRack(id: Long, action: ArrayList<ApiActionParam>, callback: (Rack?) -> Unit) {
        callback(
            apiRequest.view<Rack>(
                objPath = RACK_PATH, id = id, action = action
            )
        )
    }

    /**
     * Get a [ListResponse]<[Rack]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/rack/)
     * [index GET](http://localhost:8002/v2/rack/)
     */
    override suspend fun getRack(action: ArrayList<ApiActionParam>, callback: (ListResponse<Rack>) -> Unit) {
        callback(
            apiRequest.getListOf<Rack>(
                objPath = RACK_PATH, listName = Rack.RACK_LIST_KEY, action = action
            )
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [Rack] and Template.
     *
     * @param params Barcode request parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/rack)
     * [POST](http://localhost:8002/v2/rack/barcode)
     */
    override suspend fun getRackBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit) {
        callback(
            apiRequest.getBarcodeOf(
                objPath = RACK_PATH, params = params
            )
        )
    }

    /**
     * View a [WarehouseArea] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback Desired [WarehouseArea].
     */
    override suspend fun viewWarehouseArea(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (WarehouseArea?) -> Unit,
    ) {
        callback(
            apiRequest.view<WarehouseArea>(
                objPath = WAREHOUSE_AREA_PATH, id = id, action = action
            )
        )
    }

    /**
     * Get a [ListResponse]<[WarehouseArea]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [index GET](http://localhost:8002/v2/warehouse_area/)
     */
    override suspend fun getWarehouseArea(
        action: ArrayList<ApiActionParam>, callback: (ListResponse<WarehouseArea>) -> Unit,
    ) {
        callback(
            apiRequest.getListOf<WarehouseArea>(
                objPath = WAREHOUSE_AREA_PATH, listName = WarehouseArea.WAREHOUSE_AREA_LIST_KEY, action = action
            )
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [WarehouseArea] and Template.
     *
     * @param params Barcode request parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [POST](http://localhost:8002/v2/warehouse-area/barcode)
     */
    override suspend fun getWarehouseAreaBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit) {
        callback(
            apiRequest.getBarcodeOf(
                objPath = WAREHOUSE_AREA_PATH, params = params
            )
        )
    }

    /**
     * Returns a list of [Barcode] of the desired code of [WarehouseArea] and Template.
     *
     * @param params Barcode code request parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse_area/)
     * [POST](http://localhost:8002/v2/order-package/barcode-code)
     */
    override suspend fun getWarehouseAreaBarcodeByCode(params: BarcodeCodeParam, callback: (List<Barcode>) -> Unit) {
        callback(
            apiRequest.getBarcodeByCodeOf(
                objPath = WAREHOUSE_AREA_PATH, params = params
            )
        )
    }

    /**
     * View a [Warehouse] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback Desired [Warehouse].
     */
    override suspend fun viewWarehouse(id: Long, action: ArrayList<ApiActionParam>, callback: (Warehouse?) -> Unit) {
        callback(
            apiRequest.view<Warehouse>(
                objPath = WAREHOUSE_PATH, id = id, action = action
            )
        )
    }

    /**
     * Get a [ListResponse]<[Warehouse]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/warehouse/)
     * [index GET](http://localhost:8002/v2/warehouse/)
     */
    override suspend fun getWarehouse(action: ArrayList<ApiActionParam>, callback: (ListResponse<Warehouse>) -> Unit) {
        callback(
            apiRequest.getListOf<Warehouse>(
                objPath = WAREHOUSE_PATH, listName = Warehouse.WAREHOUSE_LIST_KEY, action = action
            )
        )
    }

    /**
     * View a [OrderResponse] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback Desired [OrderResponse].
     */
    override suspend fun viewOrder(id: Long, action: ArrayList<ApiActionParam>, callback: (OrderResponse?) -> Unit) {
        callback(
            apiRequest.view<OrderResponse>(
                objPath = ORDER_PATH, id = id, action = action
            )
        )
    }

    /**
     * Create a new [OrderResponse]
     *
     * @param payload Load object: [OrderRequest] containing the order, contents, scan records, etc.
     * @param callback Return object: created order
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/create)
     */
    override suspend fun createOrder(payload: OrderRequest, callback: (OrderResponse) -> Unit) {
        callback(
            apiRequest.create<OrderResponse>(objPath = ORDER_PATH, payload = payload)
        )
    }

    /**
     * Move an order to a desired destination both defined in [OrderMovePayload]
     *
     * @param payload Load object: [OrderMovePayload] containing the order and the destination IDs.
     * @param callback Response object: [OrderResponse] with the moved order.
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [POST](http://localhost:8002/v2/order/move)
     */
    override suspend fun moveOrder(payload: OrderMovePayload, callback: (OrderResponse) -> Unit) {
        callback(
            apiRequest.moveOrder(payload = payload)
        )
    }

    /**
     * Get a [ListResponse]<[OrderResponse]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order/)
     * [index GET](http://localhost:8002/v2/order/)
     */
    override suspend fun getOrder(action: ArrayList<ApiActionParam>, callback: (ListResponse<OrderResponse>) -> Unit) {
        callback(
            apiRequest.getListOf<OrderResponse>(
                objPath = ORDER_PATH, listName = OrderResponse.ORDER_RESPONSE_LIST_KEY, action = action
            )
        )
    }

    /**
     * Get a [ListResponse]<[OrderPackage]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order_package/)
     * [index GET](http://localhost:8002/v2/order-package/)
     */
    override suspend fun getOrderPackage(
        action: ArrayList<ApiActionParam>, callback: (ListResponse<OrderPackage>) -> Unit,
    ) {
        callback(
            apiRequest.getListOf<OrderPackage>(
                objPath = ORDER_PACKAGE_PATH, listName = OrderPackage.ORDER_PACKAGE_LIST_KEY, action = action
            )
        )
    }

    /**
     * Send an item code relation
     *
     * @param [payload] [ItemCodePayload] object with the item ID, the code and the represented amount.
     * @param [callback] [ItemCodeResponse] with diverse data
     */
    override suspend fun sendItemCode(payload: ItemCodePayload, callback: (ItemCodeResponse?) -> Unit) {
        callback(
            apiRequest.sendItemCode(payload = payload)
        )
    }

    /**
     * View a [Item] by ID
     *
     * @param id Object ID.
     * @param action List of parameters.
     * @param callback Desired [Item].
     */
    override suspend fun viewItem(id: Long, action: ArrayList<ApiActionParam>, callback: (Item?) -> Unit) {
        callback(
            apiRequest.view<Item>(
                objPath = ITEM_PATH, id = id, action = action
            )
        )
    }

    /**
     * Get a [ListResponse]<[Item]> through a callback
     *
     * @param action List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [index GET](http://localhost:8002/v2/item/)
     */
    override suspend fun getItem(
        action: ArrayList<ApiActionParam>, callback: (ListResponse<Item>) -> Unit,
    ) {
        callback(
            apiRequest.getListOf<Item>(
                objPath = ITEM_PATH, listName = Item.ITEM_LIST_KEY, action = action
            )
        )
    }

    /**
     * Returns a list of [Barcode] of the desired list of [Item] and Template.
     *
     * @param params Barcode request parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [POST](http://localhost:8002/v2/warehouse-area/barcode)
     */
    override suspend fun getItemBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit) {
        callback(
            apiRequest.getBarcodeOf(
                objPath = ITEM_PATH, params = params
            )
        )
    }

    /**
     * Returns a list of [Barcode] of the desired code of [Item] and Template.
     *
     * @param params Barcode code request parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/item/)
     * [POST](http://localhost:8002/v2/order-package/barcode-code)
     */
    override suspend fun getItemBarcodeByCode(params: BarcodeCodeParam, callback: (List<Barcode>) -> Unit) {
        callback(
            apiRequest.getBarcodeByCodeOf(
                objPath = ITEM_PATH, params = params
            )
        )
    }

    /**
     * Get a [List]<[OrderLocation]> through a callback
     *
     * @param filter List of parameters
     * @param callback Request callback
     *
     * [Manual](http://manual.dacosys.com/warehouse_counter/software/API/v2/order_location/)
     * [index GET](http://localhost:8002/v2/order_location)
     */
    override suspend fun getOrderLocation(
        filter: ArrayList<ApiFilterParam>,
        callback: (List<OrderLocation>) -> Unit
    ) {
        callback(
            apiRequest.getListOfOrderLocation(filter = filter)
        )
    }
}