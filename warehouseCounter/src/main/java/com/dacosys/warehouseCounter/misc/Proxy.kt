package com.dacosys.warehouseCounter.misc

import android.graphics.Typeface
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.lang.ref.WeakReference

class Proxy {
    companion object {
        // region PROXY THINGS
        private var avoidSetupProxyDialog = false

        interface TaskSetupProxyEnded {
            fun onTaskSetupProxyEnded(
                status: ProgressStatus,
                email: String,
                password: String,
                installationCode: String,
            )
        }

        fun setupProxy(
            callback: TaskSetupProxyEnded,
            weakAct: WeakReference<FragmentActivity>,
            email: String,
            password: String,
            installationCode: String = "",
        ) {
            val activity = weakAct.get() ?: return
            if (activity.isFinishing) return

            if (avoidSetupProxyDialog) {
                return
            }

            val sv = WarehouseCounterApp.settingViewModel
            avoidSetupProxyDialog = true

            val alert: AlertDialog.Builder = AlertDialog.Builder(activity)
            alert.setTitle(WarehouseCounterApp.context.getString(R.string.configure_proxy_question))

            val proxyEditText = EditText(activity)
            proxyEditText.hint = WarehouseCounterApp.context.getString(R.string.proxy)
            proxyEditText.isFocusable = true
            proxyEditText.isFocusableInTouchMode = true

            val proxyPortEditText = EditText(activity)
            proxyPortEditText.inputType = InputType.TYPE_CLASS_NUMBER
            proxyPortEditText.hint = WarehouseCounterApp.context.getString(R.string.port)
            proxyPortEditText.isFocusable = true
            proxyPortEditText.isFocusableInTouchMode = true

            val proxyUserEditText = EditText(activity)
            proxyUserEditText.inputType = InputType.TYPE_CLASS_TEXT
            proxyUserEditText.hint = WarehouseCounterApp.context.getString(R.string.user)
            proxyUserEditText.isFocusable = true
            proxyUserEditText.isFocusableInTouchMode = true

            val proxyPassEditText = TextInputEditText(activity)
            proxyPassEditText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            proxyPassEditText.hint = WarehouseCounterApp.context.getString(R.string.password)
            proxyPassEditText.isFocusable = true
            proxyPassEditText.isFocusableInTouchMode = true
            proxyPassEditText.typeface = Typeface.DEFAULT
            proxyPassEditText.transformationMethod = PasswordTransformationMethod()

            val inputLayout = TextInputLayout(WarehouseCounterApp.context)
            inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            inputLayout.addView(proxyPassEditText)

            val layout = LinearLayout(WarehouseCounterApp.context)
            layout.orientation = LinearLayout.VERTICAL

            layout.addView(proxyEditText)
            layout.addView(proxyPortEditText)
            layout.addView(proxyUserEditText)
            layout.addView(inputLayout)

            alert.setView(layout)
            alert.setNegativeButton(R.string.no) { _, _ ->
                sv.useProxy = false
            }
            alert.setPositiveButton(R.string.yes) { _, _ ->

                val proxy = proxyEditText.text
                val port = proxyPortEditText.text
                val user = proxyUserEditText.text
                val pass = proxyPassEditText.text

                sv.useProxy = true
                sv.proxy = proxy.toString()

                if (port != null) {
                    sv.proxyPort = Integer.parseInt(port.toString())
                }

                if (user.isNotEmpty()) {
                    sv.proxyUser = user.toString()
                }

                if (pass != null && pass.isNotEmpty()) {
                    sv.proxyPass = pass.toString()
                }
            }
            alert.setOnDismissListener {
                callback.onTaskSetupProxyEnded(
                    status = ProgressStatus.finished,
                    email = email,
                    password = password,
                    installationCode = installationCode
                )
                avoidSetupProxyDialog = false
            }

            val dialog = alert.create()
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            dialog.show()
            proxyEditText.requestFocus()
        }
        // endregion PROXY THINGS
    }
}