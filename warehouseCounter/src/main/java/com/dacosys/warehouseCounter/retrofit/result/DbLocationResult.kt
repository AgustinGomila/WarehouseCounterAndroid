package com.dacosys.warehouseCounter.retrofit.result

import com.dacosys.warehouseCounter.moshi.database.DatabaseData
import com.dacosys.warehouseCounter.sync.ProgressStatus

class DbLocationResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: DatabaseData = DatabaseData(),
    var msg: String = "",
)