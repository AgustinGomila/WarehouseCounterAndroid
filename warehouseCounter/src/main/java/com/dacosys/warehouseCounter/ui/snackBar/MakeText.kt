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
import com.dacosys.warehouseCounter.ui.utils.Colors
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
            val snackBarView = snackBar.view

            val params = snackBar.view.layoutParams
            if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.CENTER
            } else {
                (params as FrameLayout.LayoutParams).gravity = Gravity.CENTER
            }
            snackBar.view.layoutParams = params

            snackBarView.background = ResourcesCompat.getDrawable(
                context.resources, snackBarType.backColor, null
            )
            snackBarView.elevation = 6f

            snackBar.animationMode = ANIMATION_MODE_FADE
            snackBar.setTextColor(Colors.getBestContrastColorId(snackBarType.backColor))

            val textView =
                snackBarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 4 // show multiple line

            snackBar.show()
        }
    }
}

