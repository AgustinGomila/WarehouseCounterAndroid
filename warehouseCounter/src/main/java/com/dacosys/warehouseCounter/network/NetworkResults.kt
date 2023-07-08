package com.dacosys.warehouseCounter.network

import com.dacosys.warehouseCounter.dto.clientPackage.Package
import com.dacosys.warehouseCounter.dto.database.DatabaseData
import com.dacosys.warehouseCounter.sync.ProgressStatus

enum class ResultStatus {
    SUCCESS, ERROR
}

data class RequestResult(
    var status: ResultStatus = ResultStatus.SUCCESS,
    var msg: String = "",
)

data class PackagesResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: ArrayList<Package> = ArrayList(),
    var clientEmail: String = "",
    var clientPassword: String = "",
    var msg: String = "",
)

data class DbLocationResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: DatabaseData = DatabaseData(),
    var msg: String = "",
)