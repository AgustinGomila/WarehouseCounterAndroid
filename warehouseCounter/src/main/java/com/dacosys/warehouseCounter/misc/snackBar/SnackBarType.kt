package com.dacosys.warehouseCounter.misc.snackBar

import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import java.util.*

class SnackBarType(
    snackBarTypeId: Int,
    var description: String,
    var duration: Int,
    var backColor: Drawable?,
    var foreColor: Int,
) {
    var id: Int = snackBarTypeId

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is SnackBarType) {
            false
        } else this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id
    }

    companion object CREATOR {
        var ERROR = SnackBarType(
            snackBarTypeId = 0,
            description = Statics.WarehouseCounter.getContext().getString(R.string.error),
            duration = 3500,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_error, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.firebrick)
                )
            )
        )
        var INFO = SnackBarType(
            snackBarTypeId = 1,
            description = Statics.WarehouseCounter.getContext().getString(R.string.information),
            duration = 1500,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_info, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.goldenrod)
                )
            )
        )
        var RUNNING = SnackBarType(
            snackBarTypeId = 2,
            description = Statics.WarehouseCounter.getContext().getString(R.string.running),
            duration = 750,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_running, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.lightskyblue)
                )
            )
        )
        var SUCCESS = SnackBarType(
            3,
            Statics.WarehouseCounter.getContext().getString(R.string.success),
            duration = 1500,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_success, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.seagreen)
                )
            )
        )
        var ADD = SnackBarType(
            snackBarTypeId = 4,
            description = Statics.WarehouseCounter.getContext().getString(R.string.add),
            duration = 1000,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_add, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.cadetblue)
                )
            )
        )
        var UPDATE = SnackBarType(
            snackBarTypeId = 5,
            description = Statics.WarehouseCounter.getContext().getString(R.string.update),
            duration = 1000,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_update, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.steelblue)
                )
            )
        )
        var REMOVE = SnackBarType(
            snackBarTypeId = 6,
            description = Statics.WarehouseCounter.getContext().getString(R.string.remove),
            duration = 1000,
            backColor = ResourcesCompat.getDrawable(
                Statics.WarehouseCounter.getContext().resources, R.drawable.snackbar_remove, null
            ),
            foreColor = Statics.getBestContrastColor(
                "#" + Integer.toHexString(
                    Statics.WarehouseCounter.getContext().getColor(R.color.orangered)
                )
            )
        )

        fun getAll(): ArrayList<SnackBarType> {
            val allSections = ArrayList<SnackBarType>()
            Collections.addAll(
                allSections,
                ERROR,
                INFO,
                RUNNING,
                SUCCESS,
                ADD,
                UPDATE,
                REMOVE
            )

            return ArrayList(allSections.sortedWith(compareBy { it.id }))
        }

        fun getById(snackBarTypeId: Int): SnackBarType {
            return getAll().firstOrNull { it.id == snackBarTypeId } ?: INFO
        }
    }
}