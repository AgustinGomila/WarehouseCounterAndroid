package com.dacosys.warehouseCounter.ktor

import com.dacosys.warehouseCounter.dto.apiParam.ApiParam
import com.dacosys.warehouseCounter.dto.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.dto.clientPackage.PackageResponse
import com.dacosys.warehouseCounter.dto.database.DatabaseData
import com.dacosys.warehouseCounter.dto.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.dto.price.Price
import com.dacosys.warehouseCounter.dto.ptlOrder.*
import com.dacosys.warehouseCounter.dto.search.SearchObject
import com.dacosys.warehouseCounter.dto.token.TokenObject
import com.dacosys.warehouseCounter.dto.token.UserToken
import com.dacosys.warehouseCounter.dto.user.UserAuthData
import com.dacosys.warehouseCounter.dto.warehouse.Warehouse
import org.json.JSONObject

interface DacoService {
    suspend fun getClientPackage(body: AuthDataCont, callback: (PackageResponse) -> Unit)
}

interface APIService {
    suspend fun getToken(body: UserAuthData, callback: (TokenObject?) -> Unit)
    suspend fun getPtlOrder(body: UserToken, callback: (PtlOrderResponse) -> Unit)
    suspend fun getPrices(body: SearchObject, callback: (ArrayList<Price>) -> Unit)
    suspend fun getNewOrder(callback: (ArrayList<OrderRequest>) -> Unit)
    suspend fun getWarehouse(body: UserToken, callback: (ArrayList<Warehouse>) -> Unit)
    suspend fun getPtlOrderByCode(body: PtlOrderBody, callback: (PtlOrderResponse) -> Unit)
    suspend fun sendItemCode(body: JSONObject, callback: () -> Unit)
    suspend fun sendOrders(body: JSONObject, callback: () -> Unit)
    suspend fun getDbLocation(version: String, callback: (ArrayList<DatabaseData>) -> Unit)
    suspend fun attachPtlOrderToLocation(body: ApiParam, callback: (ApiResponse) -> Unit)
    suspend fun detachPtlOrderToLocation(body: ApiParam, callback: (ApiResponse) -> Unit)
    suspend fun addBoxToOrder(body: ApiParam, callback: (ApiResponse) -> Unit)
    suspend fun printBox(body: ApiParam, callback: (LabelResponse) -> Unit)
    suspend fun pickManual(body: ApiParam, callback: (PickManualResponse) -> Unit)
    suspend fun blinkOneItem(body: ApiParam, callback: (ApiResponse) -> Unit)
    suspend fun blinkAllOrder(body: ApiParam, callback: (ApiResponse) -> Unit)
    suspend fun getPtlOrderContent(body: ApiParam, callback: (PtlContentResponse) -> Unit)
}