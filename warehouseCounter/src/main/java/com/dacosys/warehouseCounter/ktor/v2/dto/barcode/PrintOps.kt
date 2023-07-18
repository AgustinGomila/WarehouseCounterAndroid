package com.dacosys.warehouseCounter.ktor.v2.dto.barcode

import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrintOps(
    @SerialName(COLL_OFFSET_KEY) var colOffset: Int = 0,
    @SerialName(LINE_END_CHAR_KEY) var lineEndChar: Char = Char(13),
    @SerialName(NAME_KEY) var printerName: String = "",
    @SerialName(POWER_KEY) var printerPower: Int = 0,
    @SerialName(QTY_KEY) var printerQty: Int = 0,
    @SerialName(SPEED_KEY) var printerSpeed: Int = 0,
    @SerialName(ROW_OFFSET_KEY) var rowOffset: Int = 0,
    @SerialName(BARCODE_LABEL_TEMPLATE_ID_KEY) var barcodeLabelTemplateId: Int? = null,
) {
    override fun hashCode(): Int {
        var result = colOffset
        result = 31 * result + lineEndChar.hashCode()
        result = 31 * result + printerName.hashCode()
        result = 31 * result + printerPower
        result = 31 * result + printerQty
        result = 31 * result + printerSpeed
        result = 31 * result + rowOffset
        result = 31 * result + (barcodeLabelTemplateId ?: -1)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrintOps

        if (colOffset != other.colOffset) return false
        if (lineEndChar != other.lineEndChar) return false
        if (printerName != other.printerName) return false
        if (printerPower != other.printerPower) return false
        if (printerQty != other.printerQty) return false
        if (printerSpeed != other.printerSpeed) return false
        if (rowOffset != other.rowOffset) return false
        return barcodeLabelTemplateId == other.barcodeLabelTemplateId
    }

    companion object {
        const val COLL_OFFSET_KEY = "colOffset"
        const val LINE_END_CHAR_KEY = "lineEndChar"
        const val NAME_KEY = "printerName"
        const val POWER_KEY = "printerPower"
        const val QTY_KEY = "printerQty"
        const val SPEED_KEY = "printerSpeed"
        const val ROW_OFFSET_KEY = "rowOffset"
        const val BARCODE_LABEL_TEMPLATE_ID_KEY = "barcodeLabelTemplateId"

        /**
         * Return the options saved in the app's configuration as a Json to send to the API.
         */
        fun getPrintOps(): PrintOps {
            val printOps = PrintOps()
            val sv = settingViewModel

            printOps.printerSpeed = sv.printerSpeed
            printOps.printerPower = sv.printerPower

            if (sv.useBtPrinter) printOps.printerName = sv.printerBtAddress
            else printOps.printerName = sv.ipNetPrinter

            printOps.colOffset = sv.colOffset
            printOps.rowOffset = sv.rowOffset
            printOps.lineEndChar = sv.lineSeparator.toCharArray().first()
            printOps.printerQty = sv.printerQty

            /** The default template is -1 (null), and we don't pass it. */
            if (sv.barcodeLabelTemplateId >= 0) printOps.barcodeLabelTemplateId = sv.barcodeLabelTemplateId

            return printOps
        }
    }
}