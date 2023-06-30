package com.dacosys.warehouseCounter.network.result

import com.dacosys.warehouseCounter.dto.database.DatabaseData
import com.dacosys.warehouseCounter.sync.ProgressStatus

class DbLocationResult(
    var status: ProgressStatus = ProgressStatus.unknown,
    var result: DatabaseData = DatabaseData(),
    var msg: String = "",
)