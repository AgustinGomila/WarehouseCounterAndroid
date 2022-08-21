package com.dacosys.warehouseCounter.log.`object`

import android.os.Parcelable
import com.google.gson.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.lang.reflect.Type


class Log : Parcelable, JsonSerializer<Log>, JsonDeserializer<Log> {
    override fun serialize(
        src: Log?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?,
    ): JsonElement {
        val jsonObject = JsonObject()
        if (src != null) {
            jsonObject.addProperty("clientId", src.clientId)
            jsonObject.addProperty("userId", src.userId)
            jsonObject.addProperty("description", src.description)
            jsonObject.addProperty("creationDate", src.creationDate)

            val jsonContentArray = JsonArray()
            for ((index, cont) in src.content.withIndex()) {
                val jsonCont = LogContent().serialize(
                    cont,
                    LogContent::class.java,
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
        }

        return jsonObject
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): Log {
        val jsonObject = json.asJsonObject

        val clientId = jsonObject.get("clientId")?.asLong
        val userId = jsonObject.get("userId")?.asLong
        val description = jsonObject.get("description")?.asString
        val creationDate = jsonObject.get("creationDate")?.asString

        val content: ArrayList<LogContent> = ArrayList()
        for ((index, cont) in jsonObject.getAsJsonArray("content").withIndex()) {
            if (cont is JsonObject) {
                val c = cont.get("content$index")
                if (c != null) {
                    content.add(
                        context.deserialize(
                            c,
                            LogContent::class.java
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
                        LogContent::class.java
                    )
                )
            }
        }

        val log = Log()

        log.clientId = clientId
        log.userId = userId
        log.description = description ?: ""
        log.creationDate = creationDate
        log.content = content

        return log
    }

    private var filename: String = ""

    fun getFilename(): String {
        return filename
    }

    @Expose
    @SerializedName("clientId")
    var clientId: Long? = null

    @Expose
    @SerializedName("userId")
    var userId: Long? = null

    @Expose
    @SerializedName("description")
    var description: String = ""

    @Expose
    @SerializedName("content")
    var content: ArrayList<LogContent> = ArrayList()

    @Expose
    @SerializedName("creationDate")
    var creationDate: String? = null

    constructor(parcel: android.os.Parcel) : this() {
        filename = parcel.readString() ?: ""

        clientId = parcel.readLong()
        userId = parcel.readLong()
        description = parcel.readString() ?: ""
        creationDate = parcel.readString()
        content = parcel.createTypedArrayList(LogContent.CREATOR) as ArrayList<LogContent>
    }

    constructor()

    constructor(
        clientId: Long?,
        userId: Long?,
        description: String,
        creationDate: String,
        content: ArrayList<LogContent>,
    ) {
        this.clientId = clientId
        this.userId = userId
        this.description = description
        this.creationDate = creationDate
        this.content = content
    }

    override fun toString(): String {
        return description
    }

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(filename)

        parcel.writeLong(clientId ?: 0)
        parcel.writeLong(userId ?: 0)
        parcel.writeString(description)
        parcel.writeString(creationDate)
        parcel.writeTypedArray(content.toTypedArray(), flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Log> {

        override fun createFromParcel(parcel: android.os.Parcel): Log {
            return Log(parcel)
        }

        override fun newArray(size: Int): Array<Log?> {
            return arrayOfNulls(size)
        }

        fun fromJson(json: String): Log? {
            // Configure GSON
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    Log::class.java,
                    Log()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<LogContent>>() {}.type,
                    LogContent()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            return gSon.fromJson(json, Log::class.java)
        }

        fun toJson(or: Log): String {
            val gSon = GsonBuilder()
                .registerTypeAdapter(
                    Log::class.java,
                    Log()
                )
                .registerTypeAdapter(
                    object : TypeToken<ArrayList<LogContent>>() {}.type,
                    LogContent()
                )
                .excludeFieldsWithoutExposeAnnotation()
                .setLenient()
                .create()

            val x = gSon.toJson(or)
            println(x)

            // Serialization
            return x
        }
    }
}