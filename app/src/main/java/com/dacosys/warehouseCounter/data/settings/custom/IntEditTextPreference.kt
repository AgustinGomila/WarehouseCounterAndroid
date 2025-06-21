package com.dacosys.warehouseCounter.data.settings.custom

import android.content.Context
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.EditText
import androidx.preference.EditTextPreference

@Suppress("unused")
class IntEditTextPreference : EditTextPreference, EditTextPreference.OnBindEditTextListener {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context, attrs, defStyleAttr, defStyleRes
    ) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context) : super(context) {
        setOnBindEditTextListener(this)
    }

    private var mText: String? = null

    /**
     * Saves the text to the current data storage.
     *
     * @param text The text to save
     */
    override fun setText(text: String?) {
        val wasBlocking = shouldDisableDependents()
        mText = text
        val value = text?.toInt() ?: return
        persistInt(value)
        val isBlocking = shouldDisableDependents()
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking)
        }
        notifyChanged()
    }

    /**
     * Gets the text from the current data storage.
     *
     * @return The current preference value
     */
    override fun getText(): String? {
        return mText
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val value: Int = if (defaultValue != null) {
            val strDefaultValue = defaultValue as String
            val defaultIntValue = strDefaultValue.toInt()
            getPersistedInt(defaultIntValue)
        } else {
            getPersistedInt(0)
        }
        text = value.toString()
    }

    override fun shouldDisableDependents(): Boolean {
        return TextUtils.isEmpty(text) || super.shouldDisableDependents()
    }

    override fun onBindEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
    }
}
