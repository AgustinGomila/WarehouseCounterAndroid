package com.example.warehouseCounter.ui.snackBar

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.ui.utils.Colors.Companion.getBestContrastColor
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

class MakeText : AppCompatActivity() {
    companion object {
        fun makeText(v: View, text: String, snackBarType: SnackBarType) {
            makeText(WeakReference(v), text, snackBarType)
        }

        fun makeText(v: View, text: String, snackBarType: Int) {
            makeText(WeakReference(v), text, SnackBarType.getById(snackBarType))
        }

        private fun makeText(v: WeakReference<View>, text: String, snackBarType: SnackBarType) {
            val view = v.get() ?: return

            val snackBar = Snackbar.make(view, text, snackBarType.duration)

            val sbView = snackBar.view
            val params = sbView.layoutParams
            if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.CENTER
            } else {
                (params as FrameLayout.LayoutParams).gravity = Gravity.CENTER
            }
            sbView.layoutParams = params
            sbView.background = ResourcesCompat.getDrawable(context.resources, snackBarType.backColor, null)
            sbView.elevation = 6f

            val textView = sbView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 4 // show multiple line

            snackBar.animationMode = ANIMATION_MODE_FADE

            val fc = ResourcesCompat.getColor(context.resources, snackBarType.foreColor, null)

            snackBar.setTextColor(getBestContrastColor("#" + Integer.toHexString(fc)))

            snackBar.show()
        }
    }
}

