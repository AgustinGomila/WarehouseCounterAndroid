package com.dacosys.warehouseCounter.data.room.entity.itemRegex

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dacosys.warehouseCounter.data.room.dao.itemRegex.ItemRegexCoroutines
import java.util.regex.PatternSyntaxException
import com.dacosys.warehouseCounter.data.room.entity.itemRegex.ItemRegexEntry as Entry

@Entity(
    tableName = Entry.TABLE_NAME,
    indices = [
        Index(
            value = [Entry.ITEM_REGEX_ID], name = "IDX_${Entry.TABLE_NAME}_${Entry.ITEM_REGEX_ID}"
        ),
    ]
)
data class ItemRegex(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Entry.ITEM_REGEX_ID) val itemRegexId: Long = 0L,
    @ColumnInfo(name = Entry.DESCRIPTION) val description: String,
    @ColumnInfo(name = Entry.REGEX) val regex: String,
    @ColumnInfo(name = Entry.JSON_CONFIG) val jsonConfig: String?,
    @ColumnInfo(name = Entry.CODE_LENGTH) val codeLength: Int?,
    @ColumnInfo(name = Entry.ACTIVE, defaultValue = "1") val active: Int,
) {
    companion object {
        private const val EAN_KEY = "ean"
        private const val LOT_CODE_KEY = "lotCode"
        private const val LOT_ID_KEY = "lotId"
        private const val QTY_KEY = "qty"
        private const val EXT_ID_KEY = "externalId"

        class RegexResult(
            var ean: String,
            var lotCode: String,
            var lotId: String,
            var qty: Float?,
            var extId: String?
        ) : Parcelable {
            constructor(parcel: Parcel) : this(
                ean = parcel.readString() ?: "",
                lotCode = parcel.readString() ?: "",
                lotId = parcel.readString() ?: "",
                qty = parcel.readValue(Float::class.java.classLoader) as? Float,
                extId = parcel.readString() ?: ""
            )

            override fun writeToParcel(parcel: Parcel, flags: Int) {
                parcel.writeString(ean)
                parcel.writeString(lotCode)
                parcel.writeString(lotId)
                parcel.writeValue(qty)
                parcel.writeString(extId)
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
         *     Si no encuentra alguno de los Keys necesarios ("ean", "lotCode", "lotId", "qty" o "externalId") el
         *     resultado es incompleto y no se devuelve.
         * @param codeRead
         * @return Lista de resultados completos del Regex
         */
        fun tryToRegex(codeRead: String, onFinished: (ArrayList<RegexResult>) -> Unit = {}) {
            ItemRegexCoroutines.get {
                val result = mutableListOf<RegexResult>()

                // Procesar cada elemento
                for (reg in it) {
                    val expectedCodeLength = reg.codeLength ?: 0
                    if (expectedCodeLength > 0 && codeRead.length != expectedCodeLength) continue

                    val regex = try {
                        reg.regex.toRegex()
                    } catch (ex: PatternSyntaxException) {
                        Log.e(this::class.java.simpleName, ex.message.orEmpty())
                        continue
                    }

                    val match = regex.find(codeRead) ?: continue

                    var ean = ""
                    try {
                        ean = match.groups[EAN_KEY]?.value.orEmpty()
                    } catch (_: Exception) {
                    }
                    var lotCode = ""
                    try {
                        lotCode = match.groups[LOT_CODE_KEY]?.value.orEmpty()
                    } catch (_: Exception) {
                    }
                    var lotId = ""
                    try {
                        lotId = match.groups[LOT_ID_KEY]?.value.orEmpty()
                    } catch (_: Exception) {
                    }
                    var qty: Float? = 0F
                    try {
                        qty = match.groups[QTY_KEY]?.value?.toFloatOrNull()
                    } catch (_: Exception) {
                    }
                    var extId = ""
                    try {
                        extId = match.groups[EXT_ID_KEY]?.value.orEmpty()
                    } catch (_: Exception) {
                    }

                    result.add(
                        RegexResult(
                            ean = ean,
                            lotCode = lotCode,
                            lotId = lotId,
                            qty = qty,
                            extId = extId
                        )
                    )
                }

                onFinished(ArrayList(result))
            }
        }
    }
}