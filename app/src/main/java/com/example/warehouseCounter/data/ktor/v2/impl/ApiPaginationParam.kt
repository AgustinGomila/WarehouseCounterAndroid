package com.example.warehouseCounter.data.ktor.v2.impl

import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import io.ktor.http.*

data class ApiPaginationParam(var page: Int, var perPage: Int) {
    companion object {

        private const val ACTION_PAGE = "page"
        private const val ACTION_PER_PAGE = "per-page"
        fun asParameter(pagination: ApiPaginationParam): Parameters {
            return Parameters.build {
                this.append(ACTION_PAGE, pagination.page.toString())
                this.append(ACTION_PER_PAGE, pagination.perPage.toString())
            }
        }

        val defaultPagination: ApiPaginationParam
            get() {
                return ApiPaginationParam(1, settingsVm.defaultPageSize)
            }
    }
}

