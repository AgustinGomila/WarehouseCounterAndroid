package com.dacosys.warehouseCounter.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.dacosys.warehouseCounter.ui.utils.Screen

class ContractsAutoCompleteTextView : AppCompatAutoCompleteTextView {
    private var myThreshold = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    override fun onFilterComplete(count: Int) {
        super.onFilterComplete(count)
        if (onContractsAvailability != null) {
            onContractsAvailability!!.contractsRetrieved(count)
        }
    }

    override fun dismissDropDown() {
        if (onContractsAvailability != null) {
            onContractsAvailability!!.contractsRetrieved(0)
        }
        super.dismissDropDown()
    }

    interface OnContractsAvailability {
        fun contractsRetrieved(count: Int)
    }

    private var onContractsAvailability: OnContractsAvailability? = null
    fun setOnContractsAvailability(onContractsAvailability: OnContractsAvailability?) {
        this.onContractsAvailability = onContractsAvailability
    }

    override fun setThreshold(threshold: Int) {
        var t = threshold
        if (t < 0) {
            t = 0
        }
        myThreshold = t
    }

    override fun enoughToFilter(): Boolean {
        return text.length >= myThreshold
    }

    override fun getThreshold(): Int {
        return myThreshold
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing) {
            if (Screen.isKeyboardVisible()) {
                return false
            }
        }
        return super.dispatchKeyEventPreIme(event)
    }

    /*override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing) {
            if (Statics.isKeyboardVisible(context)) {
                return true
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }*/
}