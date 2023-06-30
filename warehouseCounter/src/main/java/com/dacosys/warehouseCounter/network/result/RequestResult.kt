package com.dacosys.warehouseCounter.network.result

class RequestResult(
    var status: ResultStatus = ResultStatus.SUCCESS,
    var msg: String = "",
)