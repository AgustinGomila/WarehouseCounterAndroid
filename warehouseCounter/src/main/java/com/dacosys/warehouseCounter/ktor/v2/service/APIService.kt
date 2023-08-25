package com.dacosys.warehouseCounter.ktor.v2.service

import com.dacosys.warehouseCounter.ktor.v2.dto.apiParam.ListResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeCodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.ktor.v2.dto.item.Item
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodePayload
import com.dacosys.warehouseCounter.ktor.v2.dto.item.ItemCodeResponse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.ktor.v2.dto.order.*
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiActionParam
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiFilterParam

interface APIService {
    suspend fun getDatabase(version: String, callback: (DatabaseData?) -> Unit)

    suspend fun viewRack(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (Rack?) -> Unit
    )

    suspend fun getRack(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<Rack>) -> Unit
    )

    suspend fun getRackBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit)

    suspend fun viewWarehouseArea(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (WarehouseArea?) -> Unit
    )

    suspend fun getWarehouseArea(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<WarehouseArea>) -> Unit
    )

    suspend fun getWarehouseAreaBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit)
    suspend fun getWarehouseAreaBarcodeByCode(params: BarcodeCodeParam, callback: (List<Barcode>) -> Unit)

    suspend fun viewWarehouse(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (Warehouse?) -> Unit
    )

    suspend fun getWarehouse(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<Warehouse>) -> Unit
    )

    suspend fun viewBarcodeLabelTemplate(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (BarcodeLabelTemplate?) -> Unit
    )

    suspend fun getBarcodeLabelTemplate(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<BarcodeLabelTemplate>) -> Unit
    )

    suspend fun viewOrder(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (OrderResponse?) -> Unit
    )

    suspend fun createOrder(payload: OrderRequest, callback: (OrderResponse) -> Unit)
    suspend fun moveOrder(payload: OrderMovePayload, callback: (OrderResponse) -> Unit)
    suspend fun getOrder(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<OrderResponse>) -> Unit
    )

    suspend fun getOrderBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit)

    suspend fun getOrderPackage(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<OrderPackage>) -> Unit
    )

    suspend fun sendItemCode(payload: ItemCodePayload, callback: (ItemCodeResponse?) -> Unit)

    suspend fun viewItem(
        id: Long,
        action: ArrayList<ApiActionParam>,
        callback: (Item?) -> Unit
    )

    suspend fun getItem(
        filter: ArrayList<ApiFilterParam>,
        action: ArrayList<ApiActionParam>,
        callback: (ListResponse<Item>) -> Unit
    )

    suspend fun getItemBarcode(params: BarcodeParam, callback: (List<Barcode>) -> Unit)
    suspend fun getItemBarcodeByCode(params: BarcodeCodeParam, callback: (List<Barcode>) -> Unit)

    suspend fun getOrderLocation(filter: ArrayList<ApiFilterParam>, callback: (List<OrderLocation>) -> Unit)
}
