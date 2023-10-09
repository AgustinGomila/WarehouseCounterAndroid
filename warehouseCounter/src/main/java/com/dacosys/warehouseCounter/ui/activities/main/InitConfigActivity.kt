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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sharedPreferences
import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetClientPackages
import com.dacosys.warehouseCounter.data.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.data.room.database.helper.FileHelper.Companion.removeDataBases
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.data.sync.ClientPackage
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.getConfigFromScannedCode
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.selectClientPackage
import com.dacosys.warehouseCounter.databinding.InitConfigActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.ui.activities.main.ProxySetup.Companion.setupProxy
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.cdimascio.dotenv.DotenvBuilder
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class InitConfigActivity : AppCompatActivity(), Scanner.ScannerListener,
    ProxySetup.Companion.TaskSetupProxyEnded, ClientPackage.Companion.TaskConfigPanelEnded {
    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            isConfiguring = false
            showSnackBar(
                getString(R.string.configuration_applied), SnackBarType.SUCCESS
            )
            removeDataBases()
            finish()
        }
    }

    private fun onGetPackagesEnded(packagesResult: PackagesResult) {
        val status: ProgressStatus = packagesResult.status
        val result: ArrayList<com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.Package> =
            packagesResult.result
        val clientEmail: String = packagesResult.clientEmail
        val clientPassword: String = packagesResult.clientPassword
        val msg: String = packagesResult.msg

        if (status == ProgressStatus.finished) {
            if (result.size > 0) {
                runOnUiThread {
                    selectClientPackage(callback = this,
                        weakAct = WeakReference(this),
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword,
                        onEventData = { showSnackBar(it.text, it.snackBarType) })
                }
            } else {
                isConfiguring = false
                showSnackBar(msg, INFO)
            }
        } else if (status == ProgressStatus.success) {
            isConfiguring = false
            showSnackBar(msg, SnackBarType.SUCCESS)
            finish()
        } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
            isConfiguring = false
            showSnackBar(msg, ERROR)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    override fun onTaskSetupProxyEnded(
        status: ProgressStatus,
        email: String,
        password: String,
        installationCode: String,
    ) {
        if (status == ProgressStatus.finished) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    GetClientPackages.Builder()
                        .onEvent { onGetPackagesEnded(it) }
                        .addParams(
                            email = email,
                            password = password,
                            installationCode = installationCode
                        ).build()
                }
            } else {
                isConfiguring = false
            }
        }
    }

    private var rejectNewInstances = false

    private var email: String = ""
    private var password: String = ""

    override fun onResume() {
        super.onResume()

        LifecycleListener.lockScanner(this, false)
        rejectNewInstances = false
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
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
        Screen.setScreenRotation(this)
        binding = InitConfigActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
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
            LifecycleListener.autodetectDeviceModel(this)
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
                Screen.closeKeyboard(this)
                attemptToConfigure()
                true
            } else {
                false
            }
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Screen.showKeyboard(this)
            }
        }
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    Screen.closeKeyboard(this)
                    attemptToConfigure()
                    true
                }

                else -> false
            }
        }

        binding.emailEditText.setText(email, TextView.BufferType.EDITABLE)
        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Screen.showKeyboard(this)
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
        return try {
            sharedPreferences.edit().clear().apply()
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    private fun configApp() {
        val realPass = settingsVm.confPassword
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

        runOnUiThread {
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
                                alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
                                    .performClick()
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
    }

    private fun attemptEnterConfig(password: String) {
        val realPass = settingsVm.confPassword
        if (password == realPass) {
            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(baseContext, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForSettings.launch(intent)
            }
        } else {
            showSnackBar(getString(R.string.invalid_password), ERROR)
        }
    }

    private val resultForSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Vamos a reconstruir el scanner por si cambió la configuración
            LifecycleListener.autodetectDeviceModel(this)

            if (settingsVm.urlPanel.isEmpty()) {
                showSnackBar(getString(R.string.server_is_not_configured), ERROR)
                return@registerForActivityResult
            }

            Screen.closeKeyboard(this)

            setResult(RESULT_OK)
            finish()
        }

    private var isConfiguring = false
    private fun attemptToConfigure() {
        if (isConfiguring) return
        isConfiguring = true

        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (!binding.proxyCheckBox.isChecked) {
            if (email.trim().isNotEmpty() && password.trim().isNotEmpty()) {
                thread {
                    GetClientPackages.Builder()
                        .onEvent { onGetPackagesEnded(it) }
                        .addParams(
                            email = email,
                            password = password,
                            installationCode = ""
                        ).build()
                }
            } else {
                isConfiguring = false
            }
        } else {
            setupProxy(
                callback = this,
                weakAct = WeakReference(this),
                email = email,
                password = password
            )
        }
    }

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * 0.5).toInt(), (bitmap.height * 0.5).toInt(), false
        )
        return BitmapDrawable(resources, bitmapResized)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            LifecycleListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        LifecycleListener.lockScanner(this, true)
        LifecycleListener.hideWindow(this)

        try {
            getConfigFromScannedCode(
                onEvent = { onGetPackagesEnded(it) },
                scanCode = scanCode,
                mode = QRConfigClientAccount
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            showSnackBar(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            LifecycleListener.lockScanner(this, false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)

        if (!settingsVm.showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home, android.R.id.home -> {
                @Suppress("DEPRECATION") onBackPressed()
                return true
            }

            R.id.action_settings -> {
                configApp()
                true
            }

            R.id.action_rfid_connect -> {
                LifecycleListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                if (Statics.TEST_MODE && BuildConfig.DEBUG) {
                    val env = DotenvBuilder()
                        .directory("/assets")
                        .filename("env")
                        .load()

                    val username = env["CLIENT_EMAIL"]
                    val password = env["CLIENT_PASSWORD"]

                    scannerCompleted("""{"config":{"client_email":"$username","client_password":"$password"}}""".trimIndent())
                    return super.onOptionsItemSelected(item)
                }

                LifecycleListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                LifecycleListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}
