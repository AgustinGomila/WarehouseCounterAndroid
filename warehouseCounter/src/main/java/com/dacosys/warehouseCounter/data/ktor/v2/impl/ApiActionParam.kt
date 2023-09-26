@file:Suppress("unused", "SpellCheckingInspection")

package com.dacosys.warehouseCounter.data.ktor.v2.impl

import io.ktor.http.*

data class ApiActionParam(var action: String = "", var extension: Set<String> = setOf()) {
    companion object {

        private const val EXT_SEPARATOR = ","
        fun asParameter(actions: List<ApiActionParam>): Parameters {
            return Parameters.build {
                actions.forEach {
                    if (it.action.isNotEmpty())
                        append(it.action, it.extension.joinToString(EXT_SEPARATOR))
                }
            }
        }
    }
}