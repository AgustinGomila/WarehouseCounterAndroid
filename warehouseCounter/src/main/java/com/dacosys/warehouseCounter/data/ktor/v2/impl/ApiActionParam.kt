@file:Suppress("unused", "SpellCheckingInspection")

package com.dacosys.warehouseCounter.data.ktor.v2.impl

data class ApiActionParam(var action: String = "", var extension: Set<String> = setOf())
