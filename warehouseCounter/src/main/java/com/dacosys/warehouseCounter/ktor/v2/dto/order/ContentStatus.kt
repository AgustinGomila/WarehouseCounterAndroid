package com.dacosys.warehouseCounter.ktor.v2.dto.order

enum class ContentStatus(val id: Long) {
    QTY_DEFAULT(-1), QTY_EQUAL(0), QTY_MORE(1), QTY_LESS(2)
}