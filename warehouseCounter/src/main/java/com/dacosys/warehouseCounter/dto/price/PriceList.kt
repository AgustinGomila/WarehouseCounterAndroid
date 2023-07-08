package com.dacosys.warehouseCounter.dto.price

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esta clase serializa y deserializa un Json con la siguiente estructura:
 *
 * {
 *  "prices": [
 *      {
 *          "id": 28422,
 *          "extId": null,
 *          "itemPriceListId": 2,
 *          "itemPriceListDescription": "Precio de Lista",
 *          "itemPriceListListOrder": 0,
 *          "itemId": 209300,
 *          "itemDescription": "Cinta Pvc 15plus 20mts Negro   ",
 *          "price": "479.2700",
 *          "active": 1,
 *          "creationDate": "2022-12-06 12:25:57",
 *          "modificationDate": "2022-12-06 12:25:57"
 *      },
 *      ]
 * }
 */

@Serializable
class PriceList {

    @SerialName(pricesTag)
    var prices: List<Price> = ArrayList<Price>().toList()

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val pricesTag = "prices"
    }
}
