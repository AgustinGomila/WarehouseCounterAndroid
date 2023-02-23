package com.dacosys.warehouseCounter.moshi.price

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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

@JsonClass(generateAdapter = true)
class PriceList {

    @Json(name = pricesTag)
    var prices: List<Price> = ArrayList<Price>().toList()

    companion object {
        /**
         * Nombre de campos para el Json de este objeto.
         */
        const val pricesTag = "prices"
    }
}
