package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.cleanPrefs
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.InitConfigActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.closeKeyboard
import com.dacosys.warehouseCounter.misc.Statics.Companion.getConfig
import com.dacosys.warehouseCounter.misc.Statics.Companion.setupProxy
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.retrofit.functionOld.GetClientPackages
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject
import java.lang.ref.WeakReference

class InitConfigActivity : AppCompatActivity(), Scanner.ScannerListener,
    GetClientPackages.TaskGetPackagesEnded, Statics.Companion.TaskSetupProxyEnded,
    Statics.Companion.TaskConfigPanelEnded {
    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            isConfiguring = false
            makeText(binding.root, getString(R.string.configuration_applied), SnackBarType.SUCCESS)
            Statics.removeDataBases()
            finish()
        }
    }

    override fun onTaskGetPackagesEnded(
        status: ProgressStatus,
        result: ArrayList<JSONObject>,
        clientEmail: String,
        clientPassword: String,
        msg: String,
    ) {
        if (status == ProgressStatus.finished) {
            if (result.size > 0) {
                runOnUiThread {
                    Statics.selectClientPackage(callback = this,
                        weakAct = WeakReference(this),
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword,
                        onEventData = { showSnackBar(it) })
                }
            } else {
                isConfiguring = false
                makeText(binding.root, msg, INFO)
            }
        } else if (status == ProgressStatus.success) {
            isConfiguring = false
            makeText(binding.root, msg, SnackBarType.SUCCESS)
            finish()
        } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
            isConfiguring = false
            makeText(binding.root, msg, ERROR)
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    override fun onTaskSetupProxyEnded(
        status: ProgressStatus,
        email: String,
        password: String,
        installationCode: String,
    ) {
        if (status == ProgressStatus.finished) {
            getConfig(
                callback = this,
                email = email,
                password = password,
                installationCode = installationCode
            )
        }
    }

    private var rejectNewInstances = false
    private var isReturnedFromSettings = false

    private var email: String = ""
    private var password: String = ""

    override fun onResume() {
        super.onResume()

        JotterListener.lockScanner(this, false)
        rejectNewInstances = false

        // Parece que las actividades de tipo Setting no devuelven resultados
        // así que de esta manera puedo volver a llenar el fragmento de usuarios
        if (isReturnedFromSettings) {
            isReturnedFromSettings = false

            // Vamos a reconstruir el scanner por si cambió la configuración
            JotterListener.autodetectDeviceModel(this)

            if (settingViewModel().urlPanel.isEmpty()) {
                makeText(binding.root, getString(R.string.server_is_not_configured), ERROR)
                return
            }

            closeKeyboard(this)

            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onBackPressed() {
        // Esto sirve para salir del programa desde la pantalla de Login
        // moveTaskToBack(true)

        val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("email", binding.emailEditText.text.toString())
        savedInstanceState.putString("password", binding.passwordEditText.text.toString())
    }

    private lateinit var binding: InitConfigActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = InitConfigActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            email = savedInstanceState.getString("email") ?: ""
            password = savedInstanceState.getString("password") ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                email = extras.getString("email") ?: ""
                password = extras.getString("password") ?: ""
            }

            clearOldPrefs()
            JotterListener.autodetectDeviceModel(this)
        }

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val str = "${getString(R.string.app_milestone)} ${pInfo.versionName}"
        binding.versionTextView.text = str

        binding.imageView.setImageResource(0)

        var draw = ContextCompat.getDrawable(this, R.drawable.wc)

        draw = resize(draw ?: return)
        binding.imageView.setImageDrawable(draw)

        binding.passwordEditText.setText(password, TextView.BufferType.EDITABLE)
        binding.passwordEditText.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.ACTION_DOWN || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                closeKeyboard(this)
                attemptToConfigure()
                true
            } else {
                false
            }
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Statics.showKeyboard(this)
            }
        }
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    closeKeyboard(this)
                    attemptToConfigure()
                    true
                }
                else -> false
            }
        }

        binding.emailEditText.setText(email, TextView.BufferType.EDITABLE)
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Statics.showKeyboard(this)
            }
        }
        binding.emailEditText.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.ACTION_DOWN || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                binding.passwordEditText.requestFocus()
                true
            } else {
                false
            }
        }
        binding.emailEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    binding.passwordEditText.requestFocus()
                    true
                }
                else -> false
            }
        }

        binding.emailEditText.requestFocus()
    }

    private fun clearOldPrefs() {
        cleanPrefs()
    }

    private fun configApp() {
        val realPass = settingViewModel().confPassword
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

        var alertDialog: AlertDialog? = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enter_password))

        val inputLayout = TextInputLayout(this)
        inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        val input = TextInputEditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (alertDialog != null) {
                            alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        }
                    }
                }
            }
            false
        }

        inputLayout.addView(input)
        builder.setView(inputLayout)
        builder.setPositiveButton(R.string.accept) { _, _ ->
            attemptEnterConfig(input.text.toString())
        }
        builder.setNegativeButton(R.string.cancel, null)
        alertDialog = builder.create()

        alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        alertDialog.show()
        input.requestFocus()
    }

    private fun attemptEnterConfig(password: String) {
        val realPass = settingViewModel().confPassword
        if (password == realPass) {
            // TODO: Ver para qué?
            // Statics.setDebugConfigValues()

            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(baseContext, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
            isReturnedFromSettings = true
        } else {
            makeText(binding.root, getString(R.string.invalid_password), ERROR)
        }
    }

    private var isConfiguring = false
    private fun attemptToConfigure() {
        if (isConfiguring) {
            return
        }
        isConfiguring = true

        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
            if (!binding.proxyCheckBox.isChecked) {
                getConfig(
                    callback = this,
                    email = email,
                    password = password,
                    installationCode = ""
                )
            } else {
                setupProxy(
                    callback = this,
                    weakAct = WeakReference(this),
                    email = email,
                    password = password
                )
            }
        } else {
            isConfiguring = false
        }
    }

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.5).toInt(),
            (bitmap.height * 0.5).toInt(),
            false
        )
        return BitmapDrawable(resources, bitmapResized)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) JotterListener.onRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun scannerCompleted(scanCode: String) {
        JotterListener.lockScanner(this, true)
        try {
            Statics.getConfigFromScannedCode(
                callback = this,
                scanCode = scanCode,
                mode = QRConfigClientAccount
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(binding.root, ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_login, menu)

        if (!settingViewModel().showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingViewModel().useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.home, android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_settings -> {
                configApp()
                true
            }
            R.id.action_rfid_connect -> {
                JotterListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }
            R.id.action_trigger_scan -> {
                ///* For Debug */
                //scannerCompleted(
                //    """{"config":{"client_email":"miguel@dacosys.com","client_password":"sarasa123!!"}}""".trimIndent()
                //)
                //return super.onOptionsItemSelected(item)

                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }
            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}