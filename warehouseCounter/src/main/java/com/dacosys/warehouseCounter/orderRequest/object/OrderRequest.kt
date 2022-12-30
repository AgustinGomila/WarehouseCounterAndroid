@file:Suppress("UNCHECKED_CAST")

package com.dacosys.warehouseCounter.orderRequest.`object`

import android.os.Parcel
import android.os.Parcelable
import com.dacosys.imageControl.`object`.Document
import com.dacosys.imageControl.`object`.DocumentContent
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.log.`object`.Log
import com.dacosys.warehouseCounter.log.`object`.LogContent
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Type


class OrderRequest : Parcelable, JsonSerializer<OrderRequest>, JsonDeserializer<OrderRequest> {
    override fun serialize(
        src: OrderRequest?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("orderRequestId", src.orderRequestId)
            jsonObject.addProperty("clientId", src.clientId)
            jsonObject.addProperty("userId", src.userId)
            jsonObject.addProperty("externalId", src.externalId)
            jsonObject.addProperty("creationDate", src.creationDate)
            jsonObject.addProperty("description", src.description)
            jsonObject.addProperty("zone", src.zone)
            jsonObject.add(
                "orderRequestedType", OrderRequestType().serialize(
                    src.orderRequestedType,
                    OrderRequestType::class.java,
                    context
                )
            )
            jsonObject.addProperty("resultDiffQty", src.resultDiffQty)
            jsonObject.addProperty("resultDiffProduct", src.resultDiffProduct)
            jsonObject.addProperty("resultAllowDiff", src.resultAllowDiff)
            jsonObject.addProperty("resultAllowMod", src.resultAllowMod)
            jsonObject.addProperty("completed", src.completed)
            jsonObject.addProperty("startDate", src.startDate)
            jsonObject.addProperty("finishDate", src.finishDate)

            // ORDER REQUEST CONTENT
            val jsonContentArray = JsonArray()
            for ((index, cont) in src.content.withIndex()) {
                val jsonCont = OrderRequestContent().serialize(
                    cont,
                    OrderRequestContent::class.java,
                    context
                )

                val orcJson = JsonObject()
                orcJson.add(
                    "content$index",
                    jsonCont
                )

                jsonContentArray.add(orcJson)
            }
            jsonObject.add(
                "content",
                jsonContentArray
            )

            // IMAGE CONTROL DOCUMENTS
            val jsonDocumentArray = JsonArray()
            for ((index, doc) in src.docArray.withIndex()) {
                val jsonCont = Document().serialize(
                    doc,
                    Document::class.java,
                    context
                )

                val orcJson = JsonObject()
                orcJson.add(
                    "document$index",
                    jsonCont
                )

                jsonDocumentArray.add(orcJson)
            }
            jsonObject.add(
                "document",
                jsonDocumentArray
            )

            // LOG
            jsonObject.add(
                "log", Log().serialize(
                    src.log,
                    Log::class.java,
                    context
                )
            )
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): OrderRequest {
        val jsonObject = json.asJsonObject

        val orderRequestId = jsonObject.get("orderRequestId")?.asLong
        val clientId = jsonObject.get("clientId")?.asLong
        val userId = jsonObject.get("userId")?.asLong
        val externalId = jsonObject.get("externalId")?.asString
        val creationDate = jsonObject.get("creationDate")?.asString
        val description = jsonObject.get("description")?.asString
        val zone = jsonObject.get("zone")?.asString
        val orderRequestedType = jsonObject.get("orderRequestedType")?.asJsonObject
        val resultDiffQty = jsonObject.get("resultDiffQty")?.asBoolean
        val resultDiffProduct = jsonObject.get("resultDiffProduct")?.asBoolean
        val resultAllowDiff = jsonObject.get("resultAllowDiff")?.asBoolean
        val resultAllowMod = jsonObject.get("resultAllowMod")?.asBoolean
        val completed = jsonObject.get("completed")?.asBoolean
        val startDate = jsonObject.get("startDate")?.asString
        val finishDate = jsonObject.get("finishDate")?.asString

        val content: ArrayList<OrderRequestContent> = ArrayList()
        for ((index, cont) in jsonObject.getAsJsonArray("content").withIndex()) {
            if (cont is JsonObject) {
                val c = cont.get("content$index")
                if (c != null) {
                    content.add(
                        context.deserialize(
                            c,
                            OrderRequestContent::class.java
                        )
                    )
                }
            } else if (cont is JsonPrimitive) {
                var d = cont.toString().replace("\\", "")
                if (d.startsWith("\"")) {
                    d = d.replaceFirst("\"", "")
                }
                if (d.endsWith("\"")) {
                    d = d.replaceAfterLast("\"", "")
                }
                val c = JSONObject(d)["content$index"]
                content.add(
                    context.deserialize(
                        Gson().fromJson(c.toString(), JsonElement::class.java),
                        OrderRequestContent::class.java
                    )
                )
            }
        }

        val document: ArrayList<Document> = ArrayList()
        for ((index, cont) in jsonObject.getAsJsonArray("document").withIndex()) {
            if (cont is JsonObject) {
                val c = cont.get("document$index")
                if (c != null) {
                    document.add(
                        context.deserialize(
                            c,
                            Document::class.java
                        )
                    )
                }
            } else if (cont is JsonPrimitive) {
                var d = cont.toString().replace("\\", "")
                if (d.startsWith("\"")) {
                    d = d.replaceFirst("\"", "")
                }
                if (d.endsWith("\"")) {
                    d = d.replaceAfterLast("\"", "")
                }
                val c = JSONObject(d)["document$index"]
                document.add(
                    context.deserialize(
                        Gson().fromJson(c.toString(), JsonElement::class.java),
                        Document::class.java
                    )
                )
            }
        }

        val log = context.deserialize<Log>(
            jsonObject.get("log"),
            Log::class.java
        )

        val or = OrderRequest()

        or.orderRequestId = orderRequestId ?: 0L
        or.clientId = clientId
        or.userId = userId
        or.externalId = externalId ?: ""
        or.creationDate = creationDate
        or.description = description ?: ""
        or.zone = zone ?: ""
        or.orderRequestedType = context.deserialize(
            orderRequestedType,
            OrderRequestType::class.java
        )
        or.resultDiffQty = resultDiffQty
        or.resultDiffProduct = resultDiffProduct
        or.resultAllowDiff = resultAllowDiff
        or.resultAllowMod = resultAllowMod
        or.completed = completed
        or.startDate = startDate
        or.finishDate = finishDate

        or.content = content
        or.docArray = document
        or.log = log ?: Log()

        return or
    }

    var filename: String = ""

    @Expose
    @SerializedName("orderRequestId")
    var orderRequestId: Long? = null

    @Expose
    @SerializedName("externalId")
    var externalId: String = ""

    @Expose
    @SerializedName("creationDate")
    var creationDate: String? = null

    @Expose
    @SerializedName("description")
    var description: String = ""

    @Expose
    @SerializedName("zone")
    var zone: String = ""

    @Expose
    @SerializedName("orderRequestedType")
    var orderRequestedType: OrderRequestType? = null

    @Expose
    @SerializedName("resultDiffQty")
    var resultDiffQty: Boolean? = null

    @Expose
    @SerializedName("resultDiffProduct")
    var resultDiffProduct: Boolean? = null

    @Expose
    @SerializedName("resultAllowDiff")
    var resultAllowDiff: Boolean? = null

    @Expose
    @SerializedName("resultAllowMod")
    var resultAllowMod: Boolean? = null

    @Expose
    @SerializedName("completed")
    var completed: Boolean? = null

    @Expose
    @SerializedName("startDate")
    var startDate: String? = null

    @Expose
    @SerializedName("finishDate")
    var finishDate: String? = null

    @Expose
    @SerializedName("clientId")
    var clientId: Long? = null

    @Expose
    @SerializedName("userId")
    var userId: Long? = null

    @Expose
    @SerializedName("content")
    var content: ArrayList<OrderRequestContent> = ArrayList()

    @Expose
    @SerializedName("log")
    var log: Log = Log()

    @Expose
    @SerializedName("document")
    var docArray: ArrayList<Document> = ArrayList()

    constructor(parcel: Parcel) : this() {
        filename = parcel.readString() ?: ""

        orderRequestId = parcel.readLong()
        clientId = parcel.readLong()
        userId = parcel.readLong()
        externalId = parcel.readString() ?: ""
        creationDate = parcel.readString() ?: ""
        description = parcel.readString() ?: ""
        zone = parcel.readString() ?: ""
        orderRequestedType = parcel.readParcelable(OrderRequestType::class.java.classLoader)

        resultDiffQty = parcel.readByte() != 0.toByte()
        resultDiffProduct = parcel.readByte() != 0.toByte()
        resultAllowDiff = parcel.readByte() != 0.toByte()
        resultAllowMod = parcel.readByte() != 0.toByte()

        completed = parcel.readByte() != 0.toByte()
        startDate = parcel.readString() ?: ""
        finishDate = parcel.readString() ?: ""

        docArray = parcel.createTypedArrayList(Document.CREATOR) as ArrayList<Document>
        content =
            parcel.createTypedArrayList(OrderRequestContent.CREATOR) as ArrayList<OrderRequestContent>
        log = parcel.readParcelable(Log::class.java.classLoader)!!
    }

    constructor()

    constructor(
        orderRequestId: Long?,
        clientId: Long?,
        userId: Long?,
        externalId: String,
        creationDate: String,
        description: String,
        zone: String,
        orderRequestedType: OrderRequestType,
        resultDiffQty: Boolean?,
        resultDiffProduct: Boolean?,
        resultAllowDiff: Boolean?,
        resultAllowMod: Boolean?,
        completed: Boolean?,
        startDate: String?,
        finishDate: String?,
        content: ArrayList<OrderRequestContent>,
        documents: ArrayList<Document>,
        log: Log,
    ) {
        this.orderRequestId = orderRequestId!!
        this.clientId = clientId
        this.userId = userId
        this.externalId = externalId
        this.creationDate = creationDate
        this.description = description
        this.zone = zone
        this.orderRequestedType = orderRequestedType
        this.resultDiffQty = resultDiffQty
        this.resultDiffProduct = resultDiffProduct
        this.resultAllowDiff = resultAllowDiff
        this.resultAllowMod = resultAllowMod
        this.completed = completed
        this.startDate = startDate
        this.finishDate = finishDate

        this.content = content
        this.docArray = documents
        this.log = log
    }

    constructor(filename: String) {
        val completeJson = Statics.getJsonFromFile(filename)

        try {
            val or = fromJson(completeJson) ?: return
            if (or.orderRequestId == null || or.orderRequestId!! <= 0L)
                return

            this.orderRequestId = or.orderRequestId
            this.externalId = or.externalId
            this.creationDate = or.creationDate
            this.description = or.description
            this.zone = or.zone
            this.orderRequestedType = or.orderRequestedType
            this.resultDiffQty = or.resultDiffQty
            this.resultDiffProduct = or.resultDiffProduct
            this.resultAllowDiff = or.resultAllowDiff
            this.resultAllowMod = or.resultAllowMod
            this.completed = or.completed
            this.startDate = or.startDate
            this.finishDate = or.finishDate
            this.clientId = or.clientId
            this.userId = or.userId

            this.content = or.content
            this.log = or.log
            this.docArray = or.docArray

            this.filename = filename.substringAfterLast('/')
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is OrderRequest) {
            false
        } else {
            equals(this.orderRequestId, other.orderRequestId) &&
                    equals(
                        this.filename.substringAfterLast('/'),
                        other.filename.substringAfterLast('/')
                    )
        }
    }

    override fun hashCode(): Int {
        return this.orderRequestId!!.toInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(filename)

        parcel.writeLong(orderRequestId ?: 0)
        parcel.writeLong(clientId ?: 0)
        parcel.writeLong(userId ?: 0)
        parcel.writeString(externalId)
        parcel.writeString(creationDate)
        parcel.writeString(description)
        parcel.writeString(zone)

        parcel.writeParcelable(orderRequestedType, flags)

        parcel.writeInt(if (resultDiffQty == true) 1 else 0)
        parcel.writeInt(if (resultDiffProduct == true) 1 else 0)
        parcel.writeInt(if (resultAllowDiff == true) 1 else 0)
        parcel.writeInt(if (resultAllowMod == true) 1 else 0)
        parcel.writeInt(if (completed == true) 1 else 0)
        parcel.writeString(startDate)
        parcel.writeString(finishDate)

        parcel.writeTypedArray(docArray.toTypedArray(), flags)
        parcel.writeTypedArray(content.toTypedArray(), flags)
        parcel.writeParcelable(log, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderRequest> {
        override fun createFromParcel(parcel: Parcel): OrderRequest {
            return OrderRequest(parcel)
        }

        override fun newArray(size: Int): Array<OrderRequest?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: String): OrderRequest? {
            // Configure GSON
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    OrderRequest::class.java,
                    OrderRequest()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<OrderRequestContent>>() {}.type,
                    OrderRequestContent()
                )
                .registerTypeAdapter(
                    Log::class.java,
                    Log()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<LogContent>>() {}.type,
                    LogContent()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<Document>>() {}.type,
                    Document()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<DocumentContent>>() {}.type,
                    DocumentContent()
                )
                .registerTypeAdapter(
                    Item::class.java,
                    Item()
                )
                .registerTypeAdapter(
                    Qty::class.java,
                    Qty()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            return gSon.fromJson(json, OrderRequest::class.java)
        }

        fun toJson(or: OrderRequest): String {
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    OrderRequest::class.java,
                    OrderRequest()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<OrderRequestContent>>() {}.type,
                    OrderRequestContent()
                )
                .registerTypeAdapter(
                    Log::class.java,
                    Log()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<LogContent>>() {}.type,
                    LogContent()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<Document>>() {}.type,
                    Document()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<DocumentContent>>() {}.type,
                    DocumentContent()
                )
                .registerTypeAdapter(
                    Item::class.java,
                    Item()
                )
                .registerTypeAdapter(
                    Qty::class.java,
                    Qty()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            val x = gSon.toJson(or)
            println(x)

            // Serialization
            return x
        }

        fun getPendingOrderRequests(): ArrayList<OrderRequest> {
            return getOrderRequest(Statics.getPendingPath())
        }

        fun getCompletedOrderRequests(): ArrayList<OrderRequest> {
            return getOrderRequest(Statics.getCompletedPath())
        }

        private fun getOrderRequest(path: File): ArrayList<OrderRequest> {
            val orArray: ArrayList<OrderRequest> = ArrayList()
            if (Statics.isExternalStorageReadable) {
                // Get the directory for the user's public pictures directory.
                val filesInFolder = Statics.getFiles(path.absolutePath)
                if (filesInFolder != null && filesInFolder.isNotEmpty()) {
                    for (filename in filesInFolder) {
                        val filePath = path.absolutePath + File.separator + filename
                        val tempOr = OrderRequest(filePath)
                        if (tempOr.orderRequestId != null && !orArray.contains(tempOr)) {
                            tempOr.filename = filename
                            orArray.add(tempOr)
                        }
                    }
                }
            } else {
                /*MakeText.makeText(
                    binding.root,
                    context().getString(R.string.error_external_storage_not_available_for_reading),
                    com.dacosys.warehouseCounter.misc.snackbar.SnackBarType.ERROR
                )*/
            }

            return orArray
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}