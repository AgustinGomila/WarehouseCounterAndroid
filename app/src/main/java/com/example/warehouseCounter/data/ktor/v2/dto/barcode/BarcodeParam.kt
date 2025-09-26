package com.example.warehouseCounter.data.ktor.v2.dto.barcode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BarcodeParam(
    @SerialName(ID_LIST_TAG) var idList: ArrayList<Long> = ArrayList(),
    @SerialName(BARCODE_LABEL_TEMPLATE_ID_TAG) var templateId: Long,
    @SerialName(BARCODE_PRINT_OPS_TAG) var printOps: PrintOps,
) {
    companion object {
        const val ID_LIST_TAG = "ids"
        const val BARCODE_LABEL_TEMPLATE_ID_TAG = "barcodeLabelTemplateId"
        const val BARCODE_PRINT_OPS_TAG = "barcodePrintOps"
    }
}

