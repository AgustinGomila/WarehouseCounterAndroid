package com.example.warehouseCounter.data.ktor.v1.service

import com.example.warehouseCounter.misc.objects.status.ProgressStatus

enum class ResultStatus {
    SUCCESS, ERROR
}

data class RequestResult(
    var status: ResultStatus = ResultStatus.SUCCESS,
    var msg: String = "",
)

data class PackagesResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: ArrayList<com.example.warehouseCounter.data.ktor.v1.dto.clientPackage.Package> = ArrayList(),
    var clientEmail: String = "",
    var clientPassword: String = "",
    var msg: String = "",
)

data class DbLocationResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData = com.example.warehouseCounter.data.ktor.v1.dto.database.DatabaseData(),
    var msg: String = "",
)
