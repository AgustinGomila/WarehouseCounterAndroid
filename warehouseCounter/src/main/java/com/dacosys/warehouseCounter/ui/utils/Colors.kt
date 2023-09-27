package com.dacosys.warehouseCounter.ui.utils

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import java.lang.reflect.Field
import kotlin.math.min
import kotlin.math.roundToInt


class Colors {
    companion object {

        private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

        fun changeDrawableColor(icon: Int, newColor: Int): Drawable {
            val mDrawable = ContextCompat.getDrawable(context, icon)!!.mutate()

            mDrawable.colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_IN)
            return mDrawable
        }

        @SuppressLint("DiscouragedPrivateApi") /// El campo mGradientState no es parte de la SDK
        fun getBackColor(b: View, defaultColor: Int = R.color.text_light): Int {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (b.background) {
                    is StateListDrawable -> ((b.background as StateListDrawable).current as GradientDrawable).color?.defaultColor
                        ?: defaultColor

                    is GradientDrawable -> (b.background as GradientDrawable).color?.defaultColor
                        ?: defaultColor

                    else -> return defaultColor
                }
            } else {
                // Use reflection below API level 23
                try {
                    val drawable = when (b.background) {
                        is StateListDrawable -> (b.background as StateListDrawable).current as GradientDrawable
                        is GradientDrawable -> b.background as GradientDrawable
                        else -> return defaultColor
                    }

                    var field: Field = drawable.javaClass.getDeclaredField("mGradientState")
                    field.isAccessible = true
                    val myObj = field.get(drawable)
                    if (myObj == null) defaultColor
                    else {
                        field = myObj.javaClass.getDeclaredField("mSolidColors")
                        field.isAccessible = true
                        (field.get(myObj) as ColorStateList).defaultColor
                    }
                } catch (e: NoSuchFieldException) {
                    e.printStackTrace()
                    defaultColor
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                    defaultColor
                }
            }
            return Color.rgb(Color.red(r), Color.green(r), Color.blue(r))
        }

        fun getBestContrastColorId(colorId: Int): Int {
            return try {
                val color =
                    ResourcesCompat.getColor(context.resources, colorId, null)
                val backColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                val l =
                    0.2126 * Color.red(backColor) + 0.7152 * Color.green(backColor) + 0.0722 * Color.blue(
                        backColor
                    )
                if (l <= 128) textLightColor()
                else textDarkColor()
            } catch (ex: Exception) {
                Log.e(tag, ex.message.toString())
                textDarkColor()
            }
        }

        fun getBestContrastColor(color: Int): Int {
            return try {
                val backColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
                val l =
                    0.2126 * Color.red(backColor) + 0.7152 * Color.green(backColor) + 0.0722 * Color.blue(
                        backColor
                    )
                if (l <= 128) textLightColor()
                else textDarkColor()
            } catch (ex: Exception) {
                Log.e(tag, ex.message.toString())
                textDarkColor()
            }
        }

        fun getBestContrastColor(color: String): Int {
            return try {
                val backColor = Color.parseColor(color)
                val l =
                    0.2126 * Color.red(backColor) + 0.7152 * Color.green(backColor) + 0.0722 * Color.blue(
                        backColor
                    )
                if (l <= 128) textLightColor()
                else textDarkColor()
            } catch (ex: Exception) {
                Log.e(tag, ex.message.toString())
                textDarkColor()
            }
        }

        @ColorInt
        fun textLightColor(): Int {
            return ResourcesCompat.getColor(
                context.resources, R.color.text_light, null
            )
        }

        @ColorInt
        fun textDarkColor(): Int {
            return ResourcesCompat.getColor(
                context.resources, R.color.text_dark, null
            )
        }

        fun manipulateColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(a, min(r, 255), min(g, 255), min(b, 255))
        }

        fun darkenColor(color: Int, factor: Float): Int {
            val alpha = Color.alpha(color)
            val red = (Color.red(color) * factor).toInt()
            val green = (Color.green(color) * factor).toInt()
            val blue = (Color.blue(color) * factor).toInt()
            return Color.argb(alpha, red, green, blue)
        }

        fun getColorWithAlpha(colorId: Int, alpha: Int): Int {
            val color = ResourcesCompat.getColor(context.resources, colorId, null)

            val red = Color.red(color)
            val blue = Color.blue(color)
            val green = Color.green(color)

            return Color.argb(alpha, red, green, blue)
        }
    }
}
