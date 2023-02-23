package com.dacosys.warehouseCounter.room.entity.itemRegex

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.room.dao.itemRegex.ItemRegexCoroutines
import org.json.JSONObject
import java.util.regex.PatternSyntaxException

@Entity(
    tableName = ItemRegexEntry.TABLE_NAME,
    indices = [Index(value = [ItemRegexEntry.ITEM_REGEX_ID], unique = true)]
)
data class ItemRegex(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = ItemRegexEntry.ITEM_REGEX_ID) val itemRegexId: Long = 0L,
    @ColumnInfo(name = ItemRegexEntry.DESCRIPTION) val description: String,
    @ColumnInfo(name = ItemRegexEntry.REGEX) val regex: String,
    @ColumnInfo(name = ItemRegexEntry.JSON_CONFIG) val jsonConfig: String?,
    @ColumnInfo(name = ItemRegexEntry.CODE_LENGTH) val codeLength: Int?,
    @ColumnInfo(name = ItemRegexEntry.ACTIVE) val active: Int,
) {
    companion object {
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

        /**
         * Devuelve una lista de resultados completos al intentar hacer una serie de Regex
         *     sobre un código dado.
         *     Si no encuentra alguno de los Keys necesarios ("ean", "lotid" o "qty") el resultado
         *     es incompleto y no se devuelve.
         * @param codeRead
         * @return Lista de resultados completos del Regex
         */
        fun tryToRegex(codeRead: String, onFinished: (ArrayList<RegexResult>) -> Unit = {}) {
            ItemRegexCoroutines().get {
                val result = ArrayList<RegexResult>()

                // Es un regex?
                for (reg in it) {
                    if (codeRead.length != (reg.codeLength
                            ?: 0).toInt() || reg.jsonConfig.isNullOrEmpty()
                    ) {
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
                                if (groupName.isNullOrEmpty() || groupName != key) continue

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
                                            Log.e(
                                                this::class.java.simpleName, ex.message.toString()
                                            )
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

                onFinished(result)
            }
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