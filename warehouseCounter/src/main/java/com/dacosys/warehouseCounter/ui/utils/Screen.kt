package com.dacosys.warehouseCounter.ui.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent

class Screen {
    companion object {
        @SuppressLint("SourceLockedOrientationActivity")
        fun setScreenRotation(activity: AppCompatActivity) {
            val rotation: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = activity.display
                display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
                display.rotation
            }
            val height: Int
            val width: Int

            val displayMetrics = Resources.getSystem().displayMetrics
            height = displayMetrics.heightPixels
            width = displayMetrics.widthPixels

            if (settingViewModel.allowScreenRotation) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                when (rotation) {
                    Surface.ROTATION_90 -> when {
                        width > height -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    }

                    Surface.ROTATION_180 -> when {
                        height > width -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    }

                    Surface.ROTATION_270 -> when {
                        width > height -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }

                    Surface.ROTATION_0 -> activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                    else -> when {
                        height > width -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                        else -> activity.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
        }

        fun getScreenWidth(activity: Activity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity.resources.configuration.smallestScreenWidthDp < 600) { // landscape and phone
                    val navigationBarSize: Int = insets.right + insets.left
                    bounds.width() - navigationBarSize
                } else { // portrait or tablet
                    bounds.width()
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.widthPixels
            }
        }

        fun getScreenHeight(activity: Activity): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity.resources.configuration.smallestScreenWidthDp < 600) { // landscape and phone
                    bounds.height()
                } else { // portrait or tablet
                    val navigationBarSize: Int = insets.bottom
                    bounds.height() - navigationBarSize
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                outMetrics.heightPixels
            }
        }

        fun getScreenSize(activity: Activity): Size {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = activity.windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                val insets: Insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && activity.resources.configuration.smallestScreenWidthDp < 600) { // landscape and phone
                    val navigationBarSize: Int = insets.right + insets.left
                    Size(bounds.width() - navigationBarSize, bounds.height())
                } else { // portrait or tablet
                    val navigationBarSize: Int = insets.bottom
                    Size(bounds.width(), bounds.height() - navigationBarSize)
                }
            } else {
                val outMetrics = DisplayMetrics()
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.getMetrics(outMetrics)
                Size(outMetrics.widthPixels, outMetrics.heightPixels)
            }
        }

        fun getSystemBarsHeight(activity: AppCompatActivity): Int {
            // Valores de la pantalla actual
            // status bar height
            var statusBarHeight = 0
            val resourceId1: Int =
                activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId1 > 0) {
                statusBarHeight = activity.resources.getDimensionPixelSize(resourceId1)
            }

            // action bar height
            val styledAttributes: TypedArray =
                activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
            val actionBarHeight = styledAttributes.getDimension(0, 0f).toInt()
            styledAttributes.recycle()

            // navigation bar height
            var navigationBarHeight = 0
            val resourceId2: Int =
                activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId2 > 0) {
                navigationBarHeight = activity.resources.getDimensionPixelSize(resourceId2)
            }

            return statusBarHeight + actionBarHeight + navigationBarHeight
        }

        fun isTablet(): Boolean {
            return context.resources.getBoolean(R.bool.isTab)
        }

        fun isKeyboardVisible(): Boolean {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            return imm != null && imm.isActive
        }

        fun showKeyboard(activity: AppCompatActivity) {
            if (!KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                val imm =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(activity.window.decorView.rootView, 0)
            }
        }

        fun closeKeyboard(activity: AppCompatActivity) {
            if (KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                val imm =
                    activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                val cf = activity.currentFocus
                if (cf != null) {
                    imm.hideSoftInputFromWindow(cf.windowToken, 0)
                }
            }
        }

        fun closeKeyboard(activity: FragmentActivity) {
            if (KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                val imm =
                    activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
                val cf = activity.currentFocus
                if (cf != null) {
                    imm.hideSoftInputFromWindow(cf.windowToken, 0)
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        fun setupUI(view: View, activity: AppCompatActivity) {
            // Set up touch checkedChangedListener for non-text box views to hide keyboard.
            if (view !is EditText && view !is AppCompatTextView) {
                view.setOnTouchListener { _, motionEvent ->
                    closeKeyboard(activity)
                    if (view is Button && view !is Switch && view !is CheckBox) {
                        touchButton(motionEvent, view)
                        true
                    } else {
                        false
                    }
                }
            }

            //If a layout container, iterate over children and seed recursion.
            if (view is ViewGroup) {
                (0 until view.childCount).map { view.getChildAt(it) }
                    .forEach { setupUI(it, activity) }
            }
        }

        private fun touchButton(motionEvent: MotionEvent, button: Button) {
            when (motionEvent.action) {
                MotionEvent.ACTION_UP -> {
                    button.isPressed = false
                    button.performClick()
                }

                MotionEvent.ACTION_DOWN -> {
                    button.isPressed = true
                }
            }
        }
    }
}
