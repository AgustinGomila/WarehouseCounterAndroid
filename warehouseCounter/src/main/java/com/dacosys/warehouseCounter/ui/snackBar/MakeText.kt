package com.dacosys.warehouseCounter.ui.snackBar

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

class MakeText : AppCompatActivity() {
    companion object {
        fun makeText(activity: AppCompatActivity, text: String, snackBarType: SnackBarType) {
            makeText(
                WeakReference(activity.window.decorView.findViewById(R.id.content)),
                text,
                snackBarType
            )
        }

        fun makeText(v: View, text: String, snackBarType: SnackBarType) {
            makeText(WeakReference(v), text, snackBarType)
        }

        private fun makeText(v: WeakReference<View>, text: String, snackBarType: SnackBarType) {
            if (snackBarType == SnackBarType.ERROR) {
                Log.e(context.toString(), text)
            }

            val snackBar = Snackbar.make(v.get() ?: return, text, snackBarType.duration)
            val sbView = snackBar.view

            val params = snackBar.view.layoutParams
            if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.CENTER
            } else {
                (params as FrameLayout.LayoutParams).gravity = Gravity.CENTER
            }
            snackBar.view.layoutParams = params

            sbView.background = ResourcesCompat.getDrawable(context.resources, snackBarType.backColor, null)
            sbView.elevation = 6f

            snackBar.animationMode = ANIMATION_MODE_FADE

            val fc = ResourcesCompat.getColor(context.resources, snackBarType.foreColor, null)

            snackBar.setTextColor(getBestContrastColor("#" + Integer.toHexString(fc)))

            val textView = sbView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 4 // show multiple line

            snackBar.show()
        }
    }
}

