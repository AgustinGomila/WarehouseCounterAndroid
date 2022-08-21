package com.dacosys.warehouseCounter.itemRegex.`object`

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.ACTIVE
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.CODE_LENGTH
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.DESCRIPTION
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.ITEM_REGEX_ID
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.JSON_CONFIG
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexContract.ItemRegexEntry.Companion.REGEX
import com.dacosys.warehouseCounter.itemRegex.dbHelper.ItemRegexDbHelper
import org.json.JSONObject
import java.util.regex.PatternSyntaxException


class ItemRegex : Parcelable {
    var itemRegexId: Long = 0
    private var dataRead: Boolean = false

    constructor()

    constructor(
        itemRegexId: Long,
        description: String,
        regex: String,
        jsonConfig: String,
        codeLength: Long,
        active: Boolean
    ) {
        this.itemRegexId = itemRegexId
        this.description = description
        this.regex = regex
        this.jsonConfig = jsonConfig
        this.codeLength = codeLength
        this.active = active

        dataRead = true
    }

    constructor(id: Long, doChecks: Boolean) {
        itemRegexId = id

        if (doChecks) {
            refreshData()
        }
    }

    private fun refreshData(): Boolean {
        val i = ItemRegexDbHelper()
        val temp = i.selectById(this.itemRegexId)

        dataRead = true
        return when {
            temp != null -> {
                itemRegexId = temp.itemRegexId
                description = temp.description
                regex = temp.regex
                jsonConfig = temp.jsonConfig
                codeLength = temp.codeLength
                active = temp.active

                true
            }
            else -> false
        }
    }

    override fun toString(): String {
        return description
    }

    var description: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var jsonConfig: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var regex: String = ""
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return ""
                }
            }
            return field
        }

    var codeLength: Long? = null
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return null
                }
            }
            return field
        }

    var active: Boolean = false
        get() {
            if (!dataRead) {
                if (!refreshData()) {
                    return false
                }
            }
            return field
        }

    constructor(parcel: Parcel) : this() {
        itemRegexId = parcel.readLong()
        description = parcel.readString() ?: ""
        regex = parcel.readString() ?: ""
        jsonConfig = parcel.readString() ?: ""
        codeLength = parcel.readLong()
        active = parcel.readByte() != 0.toByte()

        dataRead = parcel.readByte() != 0.toByte()
    }

    fun toContentValues(): ContentValues {
        val values = ContentValues()
        values.put(ITEM_REGEX_ID, itemRegexId)
        values.put(DESCRIPTION, description)
        values.put(REGEX, regex)
        values.put(JSON_CONFIG, jsonConfig)
        values.put(CODE_LENGTH, codeLength)
        values.put(ACTIVE, active)
        return values
    }

    fun saveChanges(): Boolean {
        if (!dataRead) {
            if (!refreshData()) {
                return false
            }
        }

        val i = ItemRegexDbHelper()
        return i.update(this)
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is ItemRegex) {
            false
        } else equals(this.itemRegexId, other.itemRegexId)
    }

    override fun hashCode(): Int {
        return this.itemRegexId.toInt()
    }

    class CustomComparator : Comparator<ItemRegex> {
        override fun compare(o1: ItemRegex, o2: ItemRegex): Int {
            if (o1.itemRegexId < o2.itemRegexId) {
                return -1
            } else if (o1.itemRegexId > o2.itemRegexId) {
                return 1
            }
            return 0
        }
    }

    fun equals(a: Any?, b: Any): Boolean {
        return a != null && a == b
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemRegexId)
        parcel.writeString(description)
        parcel.writeString(regex)
        parcel.writeString(jsonConfig)
        parcel.writeLong(codeLength ?: 0)
        parcel.writeByte(if (active) 1 else 0)

        parcel.writeByte(if (dataRead) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ItemRegex> {

        override fun createFromParcel(parcel: Parcel): ItemRegex {
            return ItemRegex(parcel)
        }

        override fun newArray(size: Int): Array<ItemRegex?> {
            return arrayOfNulls(size)
        }

        class RegexResult(var ean: String, var lot: String, var qty: Float?) : Parcelable {
            constructor(parcel: Parcel) : this(
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readValue(Float::class.java.classLoader) as? Float
            )

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeString(ean)
                parcel.writeString(lot)
                parcel.writeValue(qty)
            }

            override fun describeContents(): Int {
                return 0
            }

            companion object CREATOR : Parcelable.Creator<RegexResult> {
                override fun createFromParcel(parcel: Parcel): RegexResult {
                    return RegexResult(parcel)
                }

                override fun newArray(size: Int): Array<RegexResult?> {
                    return arrayOfNulls(size)
                }
            }
        }

        private var allRegexes = ArrayList<ItemRegex>()

        /**
         * Devuelve una lista de resultados completos al intentar hacer una serie de Regex
         *     sobre un código dado.
         *     Si no encuentra alguno de los Keys necesarios ("ean", "lotid" o "qty") el resultado
         *     es incompleto y no se devuelve.
         * @param codeRead
         * @return Lista de resultados completos del Regex
         */
        fun tryToRegex(codeRead: String): ArrayList<RegexResult> {
            val result = ArrayList<RegexResult>()

            // Veamos si es un Regex
            if (!allRegexes.any()) allRegexes = ItemRegexDbHelper().select(true)

            // Es un regex?
            for (reg in allRegexes) {
                if (codeRead.length != (reg.codeLength ?: 0).toInt()) {
                    continue
                }

                val json = JSONObject(reg.jsonConfig)
                val matches: Sequence<MatchResult>?
                val rx: Regex?

                try {
                    rx = reg.regex.toRegex()
                    matches = rx.findAll(codeRead)
                } catch (ex: PatternSyntaxException) {
                    // Cuando se usan caracteres prohibidos como "_" hay posibles:
                    // U_REGEX_INVALID_CAPTURE_GROUP_NAME
                    Log.e(this::class.java.simpleName, ex.message.toString())
                    continue
                }

                for (match in matches) {
                    // Tienen que estar estas tres condiciones para
                    // formar un resultado completo.
                    var isEanFounded = false
                    var isLotIdFounded = false
                    var isQtyFounded = false

                    var ean: String? = null
                    var lot: String? = null
                    var qty: Float? = null

                    // Pasamos por todos los GroupNames (Keys) definidos en el Json
                    for (key in getKeysFromJson(json)) {
                        // El índice que corresponde a este GroupName en el Regex
                        val jsonIndex = json[key] as Int

                        var groupIndex = 0
                        for (gr in match.groups) {
                            // El primer grupo es la lectura completa y no nos interesa, por eso
                            // no aumentamos el contador (groupIndex) todavía.
                            if (gr == null || gr.value == codeRead) continue

                            // Vamos directo al índice que nos interesa.
                            if (groupIndex < jsonIndex) {
                                groupIndex++
                                continue
                            }

                            // Traemos cada GroupName (Key) del Json cuyo valor (Value) coincida con el índice de grupo.
                            val groupName: String? = getKeyFromJsonByIndex(json, jsonIndex)

                            // Si no devuelve un nombre de grupo no nos interesa.
                            // Si el nombre devuelto no es el mismo nombre que buscamos no nos interesa.
                            if (groupName.isNullOrEmpty() || groupName != key)
                                continue

                            var isFounded = false
                            when (groupName) {
                                "ean" -> {
                                    isFounded = true
                                    ean = gr.value
                                    isEanFounded = true
                                }
                                "lotid" -> {
                                    isFounded = true
                                    lot = gr.value
                                    isLotIdFounded = true
                                }
                                "qty" -> {
                                    isFounded = true
                                    try {
                                        qty = gr.value.toFloat()
                                    } catch (ex: Exception) {
                                        // qty quedará en Null. Más adelante el usuario recibirá un notificación,
                                        // pero podrá proseguir como si fuera un código corriente.
                                        Log.e(this::class.java.simpleName, ex.message.toString())
                                    }
                                    isQtyFounded = true
                                }
                            }

                            if (isFounded) break
                        }

                        // ¿Están todos los datos necesarios para agregar un resultado?
                        if (isEanFounded && isLotIdFounded && isQtyFounded) {
                            result.add(RegexResult(ean ?: "", lot ?: "", qty))
                            break
                        }
                    }
                }
            }

            return result
        }

        /**
         * Devuelve todos los Keys del Json
         * @param json Json de origen
         * @return Lista de Keys (GroupsNames)
         */
        private fun getKeysFromJson(json: JSONObject): ArrayList<String> {
            val keys: ArrayList<String> = ArrayList()
            json.keys().forEach { j ->
                keys.add(j.toString())
            }
            return keys
        }

        /**
         * Devuelve el Key (GroupName), si el Valor es igual al índice dado.
         * @param json Json de origen
         * @param index Indice que tiene que coincidir con el valor del Key
         * @return Nombre del grupo o NULL
         */
        private fun getKeyFromJsonByIndex(json: JSONObject, index: Int): String? {
            var label: String? = null
            json.keys().forEach { jKey ->
                var jV = 0
                try {
                    jV = json[jKey] as Int
                } catch (ex: Exception) {
                    Log.e(this::class.java.simpleName, ex.message.toString())
                }

                if (jV == index) {
                    label = jKey
                }
            }
            return label
        }
    }
}