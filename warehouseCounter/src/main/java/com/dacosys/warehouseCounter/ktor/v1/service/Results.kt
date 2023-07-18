package com.dacosys.warehouseCounter.ktor.v1.service

import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.Package
import com.dacosys.warehouseCounter.ktor.v1.dto.database.DatabaseData
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