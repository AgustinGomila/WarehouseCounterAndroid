package com.dacosys.warehouseCounter.data.ktor.v1.service

import com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.AuthDataCont
import com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.PackageResponse

interface DacoService {
    suspend fun getClientPackage(
        body: AuthDataCont,
        callback: (PackageResponse) -> Unit
    )
}
