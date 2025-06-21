package com.dacosys.warehouseCounter.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class ContractsAutoCompleteTextView : AppCompatAutoCompleteTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var onContractsAvailability: OnContractsAvailability? = null

    fun setOnContractsAvailability(listener: OnContractsAvailability?) {
        onContractsAvailability = listener
    }

    fun isThresholdReached() =
        text.toString().trim().length >= threshold

    override fun onFilterComplete(count: Int) {
        super.onFilterComplete(count)
        onContractsAvailability?.contractsRetrieved(tag, count)
    }

    override fun dismissDropDown() {
        onContractsAvailability?.contractsRetrieved(tag, 0)
        super.dismissDropDown()
    }

    override fun setThreshold(threshold: Int) {
        super.setThreshold(threshold.coerceAtLeast(0))
    }

    override fun enoughToFilter(): Boolean {
        return text.length >= threshold
    }

    // override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
    //     if (event.keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing && !Screen.isKeyboardVisible()) {
    //         return false
    //     }
    //     return super.dispatchKeyEventPreIme(event)
    // }

    interface OnContractsAvailability {
        fun contractsRetrieved(tag: Any?, count: Int)
    }
}