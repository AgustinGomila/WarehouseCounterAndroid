package com.dacosys.warehouseCounter.retrofit.result

import com.dacosys.warehouseCounter.dto.clientPackage.Package
import com.dacosys.warehouseCounter.sync.ProgressStatus

class PackagesResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: ArrayList<Package> = ArrayList(),
    var clientEmail: String = "",
    var clientPassword: String = "",
    var msg: String = "",
)