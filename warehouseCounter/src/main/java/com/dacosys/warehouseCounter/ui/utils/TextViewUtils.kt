package com.dacosys.warehouseCounter.ui.utils

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo

class TextViewUtils {
    companion object {
        fun isActionDone(actionId: Int, event: KeyEvent?): Boolean {
            return actionId == EditorInfo.IME_ACTION_DONE &&
                    (event == null ||
                            event.action == KeyEvent.ACTION_DOWN) &&
                    (event == null ||
                            event.keyCode == KeyEvent.KEYCODE_ENTER ||
                            event.keyCode == KeyEvent.KEYCODE_UNKNOWN ||
                            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        }

        fun isActionDone(event: KeyEvent?): Boolean {
            return isActionDone(actionId = EditorInfo.IME_ACTION_DONE, event = event)
        }
    }
}