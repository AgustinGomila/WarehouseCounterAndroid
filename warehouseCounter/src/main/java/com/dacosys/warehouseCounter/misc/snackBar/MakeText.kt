package com.dacosys.warehouseCounter.misc.snackBar

import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.dacosys.warehouseCounter.Statics
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

class MakeText : AppCompatActivity() {
    companion object {
        fun makeText(activity: AppCompatActivity, text: String, snackBarType: SnackBarType) {
            makeText(
                WeakReference(activity.window.decorView.findViewById(android.R.id.content)),
                text,
                snackBarType
            )
        }

        fun makeText(v: View, text: String, snackBarType: SnackBarType) {
            makeText(WeakReference(v), text, snackBarType)
        }

        private fun makeText(v: WeakReference<View>, text: String, snackBarType: SnackBarType) {
            if (snackBarType == SnackBarType.ERROR) {
                Log.e(Statics.WarehouseCounter.getContext().toString(), text)
            }

            val snackbar = Snackbar.make(v.get() ?: return, text, snackBarType.duration)
            val snackbarView = snackbar.view

            val params = snackbar.view.layoutParams
            if (params is CoordinatorLayout.LayoutParams) {
                params.gravity = Gravity.CENTER
            } else {
                (params as FrameLayout.LayoutParams).gravity = Gravity.CENTER
            }
            snackbar.view.layoutParams = params

            snackbarView.background = snackBarType.backColor
            snackbarView.elevation = 6f

            snackbar.animationMode = ANIMATION_MODE_FADE
            snackbar.setTextColor(snackBarType.foreColor)

            val textView =
                snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
            textView.maxLines = 4 // show multiple line

            snackbar.show()
        }
    }
}

