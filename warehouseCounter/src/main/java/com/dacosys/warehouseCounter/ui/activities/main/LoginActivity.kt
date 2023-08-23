package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.dacosys.imageControl.room.database.IcDatabase
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.LoginActivityBinding
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.Package
import com.dacosys.warehouseCounter.ktor.v1.functions.GetClientPackages.Companion.getConfig
import com.dacosys.warehouseCounter.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.ktor.v2.functions.GetDatabase
import com.dacosys.warehouseCounter.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.misc.ImageControl.Companion.closeImageControl
import com.dacosys.warehouseCounter.misc.ImageControl.Companion.setupImageControl
import com.dacosys.warehouseCounter.misc.Md5
import com.dacosys.warehouseCounter.misc.Proxy
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.appName
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.room.database.WcDatabase
import com.dacosys.warehouseCounter.room.database.WcTempDatabase
import com.dacosys.warehouseCounter.room.entity.user.User
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.settings.utils.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.settings.utils.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.sync.ClientPackage
import com.dacosys.warehouseCounter.sync.ClientPackage.Companion.getConfigFromScannedCode
import com.dacosys.warehouseCounter.sync.ClientPackage.Companion.selectClientPackage
import com.dacosys.warehouseCounter.sync.DownloadDb
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.fragments.user.UserSpinnerFragment
import com.dacosys.warehouseCounter.ui.fragments.user.UserSpinnerFragment.Companion.SyncStatus.*
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import org.parceler.Parcels
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity(), UserSpinnerFragment.OnItemSelectedListener,
    UserSpinnerFragment.OnSpinnerFillListener, Scanner.ScannerListener,
    Proxy.Companion.TaskSetupProxyEnded, ClientPackage.Companion.TaskConfigPanelEnded,
    DownloadDb.DownloadDbTask {
    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            showSnackBar(
                SnackBarEventData(
                    getString(R.string.configuration_applied), SnackBarType.SUCCESS
                )
            )
            initialSetup()
        } else if (status == ProgressStatus.crashed) {
            showSnackBar(SnackBarEventData(getString(R.string.error_setting_user_panel), ERROR))
        }
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        userSpinnerFragment?.onDestroy()
    }

    private fun onGetPackagesEnded(packagesResult: PackagesResult) {
        val status: ProgressStatus = packagesResult.status
        val result: ArrayList<Package> = packagesResult.result
        val clientEmail: String = packagesResult.clientEmail
        val clientPassword: String = packagesResult.clientPassword
        val msg: String = packagesResult.msg

        if (status == ProgressStatus.finished) {
            if (result.isNotEmpty()) {
                runOnUiThread {
                    selectClientPackage(weakAct = WeakReference(this),
                        callback = this,
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword,
                        onEventData = { showSnackBar(it) })
                }
            } else {
                showSnackBar(SnackBarEventData(msg, SnackBarType.INFO))
            }
        } else if (status == ProgressStatus.success) {
            showSnackBar(SnackBarEventData(msg, SnackBarType.SUCCESS))
        } else if (status == ProgressStatus.crashed) {
            showSnackBar(SnackBarEventData(msg, ERROR))
        }
    }

    private fun onGetDatabaseData(data: DatabaseData?) {
        if (data != null) {
            thread {
                val sync = DownloadDb()
                sync.addParams(callBack = this,
                    timeFileUrl = data.dbDate,
                    dbFileUrl = data.dbFile,
                    onEventData = { showSnackBar(it) })
                sync.execute()
            }
        } else {
            showProgressBar()
            setButton(ButtonStyle.REFRESH)
            attemptSync = false
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    override fun onDownloadDbTask(downloadStatus: DownloadDb.DownloadStatus) {
        if (downloadStatus in DownloadDb.statusEnd()) {
            attemptSync = false
        }

        when (downloadStatus) {
            DownloadDb.DownloadStatus.STARTING -> {
                setButton(ButtonStyle.BUSY)
                showProgressBar(getString(R.string.downloading_database))
            }

            DownloadDb.DownloadStatus.FINISHED,
            DownloadDb.DownloadStatus.CANCELED,
            -> {
                // FINISHED = Ok
                // CANCELED = Sin conexión
                showProgressBar()
                enableLogin()
            }

            DownloadDb.DownloadStatus.CRASHED -> {
                setButton(ButtonStyle.REFRESH)
                showProgressBar()
            }

            DownloadDb.DownloadStatus.DOWNLOADING,
            DownloadDb.DownloadStatus.COPYING,
            -> {
                setButton(ButtonStyle.BUSY)
            }

            DownloadDb.DownloadStatus.INFO -> {
            }
        }
    }

    private fun enableLogin() {
        showProgressBar(getString(R.string.updating_item_codes))

        Statics.insertItemCodes()
        refreshUsers()

        setButton(ButtonStyle.READY)
        showProgressBar()
    }

    override fun onDownloadFileTask(
        msg: String,
        fileType: DownloadDb.FileType,
        downloadStatus: DownloadDb.DownloadStatus,
        progress: Int?,
    ) {
        if (downloadStatus == DownloadDb.DownloadStatus.DOWNLOADING || downloadStatus == DownloadDb.DownloadStatus.COPYING) {
            showProgressBar(msg, progress)
        }
    }

    override fun onTaskSetupProxyEnded(
        status: ProgressStatus,
        email: String,
        password: String,
        installationCode: String,
    ) {
        if (status == ProgressStatus.finished) {
            getConfig(
                onEvent = { onGetPackagesEnded(it) },
                email = email,
                password = password,
                installationCode = installationCode
            )
        }
    }

    private fun setEditTextFocus() {
        runOnUiThread {
            binding.passwordEditText.isCursorVisible = true
            binding.passwordEditText.isFocusable = true
            binding.passwordEditText.isFocusableInTouchMode = true
            binding.passwordEditText.requestFocus()
        }
    }

    enum class ButtonStyle {
        READY, REFRESH, BUSY
    }

    private fun setButton(style: ButtonStyle) {
        runOnUiThread {
            connectionSuccess = style == ButtonStyle.READY
            when (style) {
                ButtonStyle.READY -> setLoginButton()
                ButtonStyle.REFRESH -> setRefreshButton()
                ButtonStyle.BUSY -> setWaitButton()
            }
        }
    }

    private fun setRefreshButton() {
        runOnUiThread {
            binding.loginImageView.setImageResource(R.drawable.ic_refresh)
            binding.loginImageView.background =
                ResourcesCompat.getDrawable(resources, R.drawable.rounded_corner_button_gold, null)
            binding.loginImageView.contentDescription = getString(R.string.retry_connection)
            binding.loginImageView.foregroundTintList =
                ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.black, null))
        }
    }

    private fun setWaitButton() {
        runOnUiThread {
            binding.loginImageView.setImageResource(R.drawable.ic_hourglass)
            binding.loginImageView.background = ResourcesCompat.getDrawable(
                resources, R.drawable.rounded_corner_button_steelblue, null
            )
            binding.loginImageView.contentDescription = getString(R.string.connecting)
            binding.loginImageView.foregroundTintList =
                ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, null))
        }
    }

    private fun setLoginButton() {
        runOnUiThread {
            binding.loginImageView.setImageResource(R.drawable.ic_check)
            binding.loginImageView.background = ResourcesCompat.getDrawable(
                resources, R.drawable.rounded_corner_button_seagreen, null
            )
            binding.loginImageView.contentDescription = getString(R.string.sign_in)
            binding.loginImageView.foregroundTintList =
                ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.cornsilk, null))
        }
    }

    override fun onResume() {
        super.onResume()

        JotterListener.lockScanner(this, false)
        rejectNewInstances = false

        Statics.isLogged = false
    }

    private fun refreshUsers() {
        if (Statics.downloadDbRequired) return

        try {
            runOnUiThread { userSpinnerFragment!!.reFill() }
        } catch (ex: Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        }

        attemptSync = false
    }

    override fun onItemSelected(user: User?) {
        setEditTextFocus()
    }

    override fun onBackPressed() {
        // Esto sirve para salir del programa desde la pantalla de Login
        moveTaskToBack(true)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putParcelable(ARG_USER, userSpinnerFragment!!.selectedUser)
        savedInstanceState.putString(ARG_PASSWORD, binding.passwordEditText.text.toString())
    }

    private var userSpinnerFragment: UserSpinnerFragment? = null
    private var rejectNewInstances = false

    private var attemptSync = false
    private var attemptRunning = false
    private var connectionSuccess = false

    private var user: User? = null
    private var password: String = ""

    private lateinit var binding: LoginActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //// INICIALIZAR CONTROLES
        userSpinnerFragment =
            supportFragmentManager.findFragmentById(R.id.userSpinnerFragment) as UserSpinnerFragment?

        if (savedInstanceState != null) {
            user = savedInstanceState.getParcelable(ARG_USER)
            password = savedInstanceState.getString(ARG_PASSWORD) ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                user = Parcels.unwrap<User>(extras.getParcelable<User>(ARG_USER))
                password = extras.getString(ARG_PASSWORD) ?: ""
            }
        }

        // region Mostrar número de versión, etc.
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val str = "${getString(R.string.app_milestone)} ${pInfo.versionName}"
        binding.versionTextView.text = str

        // Cliente
        binding.clientTextView.text = settingViewModel.installationCode
        // endregion

        // region Mostar la imagen de cabecera
        binding.imageView.setImageResource(0)

        var draw = ContextCompat.getDrawable(this, R.drawable.wc)
        draw = resize(draw!!)

        binding.imageView.setImageDrawable(draw)
        // endregion

        userSpinnerFragment!!.selectedUser = user

        binding.passwordEditText.setText(password, TextView.BufferType.EDITABLE)
        binding.passwordEditText.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                binding.loginImageView.performClick()
                true
            } else {
                false
            }
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                Screen.showKeyboard(this)
            } else {
                Screen.closeKeyboard(this)
            }
        }
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    binding.loginImageView.performClick()
                    true
                }

                else -> false
            }
        }
        binding.passwordEditText.requestFocus()

        binding.loginImageView.setOnClickListener {
            if (connectionSuccess) {
                user = userSpinnerFragment!!.selectedUser
                password = binding.passwordEditText.text.toString()
                attemptLogin()
            } else {
                initialSetup()
            }
        }

        initialSetup()

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        Screen.setupUI(binding.root, this)
    }

    private fun showProgressBar(msg: String = "", progress: Int? = null) {
        runOnUiThread {
            if (msg.isNotEmpty()) {
                val percent = if (progress == null) "" else "$progress%"

                binding.syncStatusTextView.text = msg
                binding.syncPercentTextView.text = percent
                binding.progressBarLayout.visibility = VISIBLE
                binding.progressBarLayout.bringToFront()

                ViewCompat.setZ(binding.progressBarLayout, 0F)
            } else {
                binding.progressBarLayout.visibility = GONE
                binding.syncPercentTextView.text = ""
                binding.syncStatusTextView.text = ""
            }
        }
    }

    private fun initSync() {
        if (!ApiRequest.validUrl()) {
            showSnackBar(SnackBarEventData(context.getString(R.string.invalid_url), ERROR))
            return
        }

        thread {
            GetDatabase(
                onEvent = { showSnackBar(it) },
                onFinish = { onGetDatabaseData(it) }).execute()
        }
    }

    private fun initialSetup() {
        if (attemptSync) return
        attemptSync = true

        setButton(ButtonStyle.BUSY)

        if (settingViewModel.urlPanel.isEmpty()) {
            showSnackBar(
                SnackBarEventData(
                    text = getString(R.string.server_is_not_configured), ERROR
                )
            )

            setButton(ButtonStyle.REFRESH)
            attemptSync = false
        } else {
            try {
                /* Des-inicializamos IC para evitar que se
                   suban imágenes pendientes antes de autentificarse.
                   Escenario en el que el usuario ha vuelto a esta
                   actividad después haber estado autentificado.
                 */
                closeImageControl()

                WcDatabase.cleanInstance()
                WcTempDatabase.cleanInstance()
                IcDatabase.cleanInstance()

                initSync()
            } catch (ex: Exception) {
                showProgressBar()
                showSnackBar(SnackBarEventData(text = ex.message.toString(), ERROR))
                ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())

                setButton(ButtonStyle.REFRESH)
                attemptSync = false
            }
        }
    }

    private fun configApp() {
        val realPass = settingViewModel.confPassword
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

        runOnUiThread {
            var alertDialog: AlertDialog? = null
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enter_password))

            val inputLayout = TextInputLayout(this)
            inputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE

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
        val realPass = settingViewModel.confPassword
        if (password == realPass) {
            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(context, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForSettings.launch(intent)
            }
        } else {
            makeText(binding.root, getString(R.string.invalid_password), ERROR)
        }
    }

    private val resultForSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Vamos a reconstruir el scanner por si cambió la configuración
            JotterListener.autodetectDeviceModel(this)

            initialSetup()
        }

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * 0.5).toInt(), (bitmap.height * 0.5).toInt(), false
        )
        return BitmapDrawable(resources, bitmapResized)
    }

    private fun attemptLogin() {
        if (attemptRunning) return
        attemptRunning = true

        // Store values at the time of the login attempt.
        val userId = userSpinnerFragment!!.selectedUserId ?: 0L
        val userPass = userSpinnerFragment!!.selectedUserPass
        val password = binding.passwordEditText.text.toString()

        attemptLogin(userId.toLong(), userPass, password)
    }

    private fun attemptLogin(userId: Long?, userPass: String, password: String) {
        runOnUiThread {
            binding.passwordEditText.error = null
        }

        var cancel = false

        // Check for a valid email address.
        if (userId == null || userId <= 0) {
            showSnackBar(SnackBarEventData(getString(R.string.you_must_select_a_user), ERROR))
            cancel = true
        } else if (!isUserValid(userId)) {
            showSnackBar(SnackBarEventData(getString(R.string.you_must_select_a_user), ERROR))
            cancel = true
        }

        attemptRunning = false

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            runOnUiThread {
                userSpinnerFragment!!.view?.requestFocus()
            }
        } else {
            // Show a progress spinner and kick off a background task to
            // perform the user login attempt.
            if (userPass == Md5.getMd5(password)) {

                Statics.cleanCurrentUser()
                Statics.currentUserId = user!!.userId
                Statics.currentUserName = user!!.name
                Statics.currentPass = user!!.password ?: ""
                Statics.isLogged = true

                setupImageControl()

                finish()
            } else {
                showSnackBar(
                    SnackBarEventData(
                        getString(R.string.wrong_user_password_combination), ERROR
                    )
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) JotterListener.onRequestPermissionsResult(
            this, requestCode, permissions, grantResults
        )
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingViewModel.showScannedCode && BuildConfig.DEBUG) makeText(
            binding.root, scanCode, SnackBarType.INFO
        )

        JotterListener.lockScanner(this, true)
        JotterListener.hideWindow(this)

        try {
            val mainJson = JSONObject(scanCode)

            // LOGIN
            if (scanCode.contains("log_user_name") && scanCode.contains("log_password")) {
                val confJson = mainJson.getJSONObject(appName)

                val userName = confJson.getString("log_user_name")
                val password = confJson.getString("log_password")

                if (userName.isNotEmpty() && password.isNotEmpty()) {
                    UserCoroutines.getByName(userName) {
                        if (it != null) {
                            attemptLogin(it.userId, it.password ?: "", password)
                        } else {
                            showSnackBar(SnackBarEventData(getString(R.string.invalid_user), ERROR))
                        }
                    }
                } else {
                    showSnackBar(SnackBarEventData(getString(R.string.invalid_code), ERROR))
                }
                return
            }

            when {
                mainJson.has("config") -> {
                    // CLIENT CONFIGURATION
                    runOnUiThread {
                        try {
                            val adb = AlertDialog.Builder(this)
                            adb.setTitle(getString(R.string.download_database_required))
                            adb.setMessage(getString(R.string.download_database_required_question))
                            adb.setNegativeButton(R.string.cancel, null)
                            adb.setPositiveButton(R.string.accept) { _, _ ->
                                Statics.downloadDbRequired = true
                                getConfigFromScannedCode(
                                    onEvent = { onGetPackagesEnded(it) },
                                    scanCode = scanCode,
                                    mode = QRConfigClientAccount
                                )
                            }
                            adb.show()
                        } catch (ex: java.lang.Exception) {
                            ex.printStackTrace()
                            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
                        }
                    }
                }

                mainJson.has(appName) -> {
                    // APP CONFIGURATION
                    getConfigFromScannedCode(
                        onEvent = { onGetPackagesEnded(it) },
                        scanCode = scanCode,
                        mode = QRConfigApp
                    )
                }

                else -> {
                    showSnackBar(SnackBarEventData(getString(R.string.invalid_code), ERROR))
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            showSnackBar(SnackBarEventData(ex.message.toString(), ERROR))
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    private fun isUserValid(userId: Long): Boolean {
        return userId > 0
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_login, menu)

        if (!settingViewModel.showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingViewModel.useBtRfid) {
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

    override fun onSpinnerFill(status: UserSpinnerFragment.Companion.SyncStatus) {
        when (status) {
            STARTING -> {
                setButton(ButtonStyle.BUSY)
                showProgressBar(getString(R.string.loading_users))
            }

            CANCELED, CRASHED -> {
                setButton(ButtonStyle.REFRESH)
                showProgressBar()
            }

            FINISHED -> {
                showProgressBar()
                if (userSpinnerFragment!!.count < 1) {
                    setButton(ButtonStyle.REFRESH)
                    Log.d(
                        this::class.java.simpleName,
                        getString(R.string.there_are_no_users_in_the_database)
                    )
                } else {
                    setButton(ButtonStyle.READY)
                }
            }

            RUNNING -> {}
        }
    }

    companion object {
        const val ARG_USER = "user"
        const val ARG_PASSWORD = "password"
    }
}
