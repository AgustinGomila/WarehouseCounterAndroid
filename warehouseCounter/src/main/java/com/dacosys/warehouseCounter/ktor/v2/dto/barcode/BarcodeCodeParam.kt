package com.dacosys.warehouseCounter.ktor.v2.dto.barcode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BarcodeCodeParam(
    @SerialName(CODE_TAG) var code: String = "",
    @SerialName(BARCODE_LABEL_TEMPLATE_ID_TAG) var templateId: Long,
    @SerialName(BARCODE_PRINT_OPS_TAG) var printOps: PrintOps,
) {
    companion object {
        const val CODE_TAG = "code"
        const val BARCODE_LABEL_TEMPLATE_ID_TAG = "barcodeLabelTemplateId"
        const val BARCODE_PRINT_OPS_TAG = "barcodePrintOps"
    }
}