package com.example.warehouseCounter.data.room.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class RegexTest {
    @Test
    fun parse_regex() = runTest {
        val regex = """(?<externalId>\w+)\|(?<ean>\w+)\|(?<lotId>\w+)\|(?<qty>\w+)""".toRegex()
        val input = "8214|7792261029823|58290|64"

        val matchResult = regex.find(input)

        if (matchResult != null) {
            val externalId = matchResult.groups["externalId"]?.value ?: ""
            val ean = matchResult.groups["ean"]?.value ?: ""
            val lotCode = matchResult.groups["lotId"]?.value ?: ""
            val qty = matchResult.groups["qty"]?.value ?: ""

            assertEquals("8214", externalId)
            assertEquals("7792261029823", ean)
            assertEquals("58290", lotCode)
            assertEquals("64", qty)
        } else {
            assertEquals(1, 2)
        }
    }
}
