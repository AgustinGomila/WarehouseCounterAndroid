package com.dacosys.warehouseCounter.data.ktor.v2.service

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
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiFilterParam
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiPaginationParam

interface APIService {
    suspend fun getDatabase(version: String, callback: (APIResponse<DatabaseData>) -> Unit)

    suspend fun viewRack(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<Rack>) -> Unit
    )

    suspend fun getRack(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Rack>>) -> Unit
    )

    suspend fun getRackBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)

    suspend fun viewWarehouseArea(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<WarehouseArea>) -> Unit
    )

    suspend fun getWarehouseArea(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<WarehouseArea>>) -> Unit
    )

    suspend fun getWarehouseAreaBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)
    suspend fun getWarehouseAreaBarcodeByCode(params: BarcodeCodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)

    suspend fun viewWarehouse(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<Warehouse>) -> Unit
    )

    suspend fun getWarehouse(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Warehouse>>) -> Unit
    )

    suspend fun viewBarcodeLabelTemplate(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<BarcodeLabelTemplate>) -> Unit
    )

    suspend fun getBarcodeLabelTemplate(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<BarcodeLabelTemplate>>) -> Unit
    )

    suspend fun viewOrder(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<OrderResponse>) -> Unit
    )

    suspend fun createOrder(payload: OrderRequest, callback: (APIResponse<OrderResponse>) -> Unit)
    suspend fun moveOrder(payload: OrderMovePayload, callback: (APIResponse<OrderResponse>) -> Unit)
    suspend fun getOrder(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<OrderResponse>>) -> Unit
    )

    suspend fun updateOrder(id: Long, payload: OrderUpdatePayload, callback: (APIResponse<OrderResponse>) -> Unit)

    suspend fun getOrderBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)

    suspend fun getOrderPackage(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<OrderPackage>>) -> Unit
    )

    suspend fun sendItemCode(payload: ItemCodePayload, callback: (APIResponse<ItemCodeResponse>) -> Unit)

    suspend fun viewItem(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<Item>) -> Unit
    )

    suspend fun getItem(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<Item>>) -> Unit
    )

    suspend fun getItemBarcode(params: BarcodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)
    suspend fun getItemBarcodeByCode(params: BarcodeCodeParam, callback: (APIResponse<List<Barcode>>) -> Unit)

    suspend fun getOrderLocation(
        filter: ArrayList<ApiFilterParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<List<OrderLocation>>) -> Unit
    )

    suspend fun getItemCode(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        pagination: ApiPaginationParam,
        callback: (APIResponse<ListResponse<ItemCode>>) -> Unit
    )

    suspend fun viewItemCode(
        id: Long, action: ArrayList<ApiActionParam>, callback: (APIResponse<ItemCode>) -> Unit
    )
}
