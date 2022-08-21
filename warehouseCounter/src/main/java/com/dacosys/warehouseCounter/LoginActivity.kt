package com.dacosys.warehouseCounter

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
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import com.dacosys.warehouseCounter.Statics.Companion.appName
import com.dacosys.warehouseCounter.Statics.Companion.closeKeyboard
import com.dacosys.warehouseCounter.Statics.Companion.prefsGetString
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.configuration.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.configuration.SettingsActivity
import com.dacosys.warehouseCounter.databinding.LoginActivityBinding
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.Md5
import com.dacosys.warehouseCounter.misc.Preference
import com.dacosys.warehouseCounter.misc.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType
import com.dacosys.warehouseCounter.misc.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.sync.DownloadDb
import com.dacosys.warehouseCounter.sync.GetClientPackages
import com.dacosys.warehouseCounter.sync.GetDatabaseLocation
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.user.`object`.User
import com.dacosys.warehouseCounter.user.dbHelper.UserDbHelper
import com.dacosys.warehouseCounter.user.fragment.UserSpinnerFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import org.parceler.Parcels
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class LoginActivity :
    AppCompatActivity(),
    UserSpinnerFragment.OnItemSelectedListener,
    UserSpinnerFragment.OnSpinnerFillListener,
    Scanner.ScannerListener,
    Statics.Companion.TaskSetupProxyEnded,
    GetClientPackages.TaskGetPackagesEnded,
    Statics.Companion.TaskConfigPanelEnded,
    GetDatabaseLocation.TaskGetDatabaseLocationEnded,
    DownloadDb.DownloadDbTask {
    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            makeText(binding.root, getString(R.string.configuration_applied), SnackBarType.SUCCESS)
            initialSetup()
        } else if (status == ProgressStatus.crashed) {
            makeText(binding.root, getString(R.string.error_setting_user_panel), ERROR)
        }
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        userSpinnerFragment?.onDestroy()
    }

    override fun onTaskGetPackagesEnded(
        status: ProgressStatus,
        result: ArrayList<JSONObject>,
        clientEmail: String,
        clientPassword: String,
        msg: String,
    ) {
        if (status == ProgressStatus.finished) {
            if (result.isNotEmpty()) {
                runOnUiThread {
                    Statics.selectClientPackage(
                        parentView = binding.root,
                        weakAct = WeakReference(this),
                        callback = this,
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword
                    )
                }
            } else {
                makeText(binding.root, msg, SnackBarType.INFO)
            }
        } else if (status == ProgressStatus.success) {
            makeText(binding.root, msg, SnackBarType.SUCCESS)
        } else if (status == ProgressStatus.crashed) {
            makeText(binding.root, msg, ERROR)
        }
    }

    override fun onTaskGetDatabaseLocationEnded(
        status: ProgressStatus,
        timeFileUrl: String,
        dbFileUrl: String,
        msg: String,
    ) {
        when (status) {
            ProgressStatus.crashed -> {
                makeText(binding.root, msg, ERROR)
                showProgressBar(false, "")

                setButton(ButtonStyle.REFRESH)
                attemptSync = false
            }
            ProgressStatus.running -> {
                setButton(ButtonStyle.BUSY)
            }
            ProgressStatus.canceled -> {
                makeText(binding.root, msg, ERROR)
                showProgressBar(false, "")
                attemptSync = false

                // Sin conexión
                enableLogin()
            }
            ProgressStatus.finished -> {
                thread {
                    val sync = DownloadDb()
                    sync.addParams(
                        parentView = binding.root,
                        callBack = this,
                        timeFileUrl = timeFileUrl,
                        dbFileUrl = dbFileUrl
                    )
                    sync.execute()
                }
            }
        }
    }

    override fun onDownloadDbTask(downloadStatus: DownloadDb.DownloadStatus) {
        if (downloadStatus in DownloadDb.statusEnd()) {
            attemptSync = false
        }

        when (downloadStatus) {
            DownloadDb.DownloadStatus.STARTING -> {
                setButton(ButtonStyle.BUSY)
                showProgressBar(true, getString(R.string.downloading_database))
            }
            DownloadDb.DownloadStatus.FINISHED,
            DownloadDb.DownloadStatus.CANCELED,
            -> {
                // FINISHED = Ok
                // CANCELED = Sin conexión
                enableLogin()
            }
            DownloadDb.DownloadStatus.CRASHED -> {
                setButton(ButtonStyle.REFRESH)
                showProgressBar(false, "")
            }
            DownloadDb.DownloadStatus.DOWNLOADING -> {
                setButton(ButtonStyle.BUSY)
            }
            DownloadDb.DownloadStatus.INFO -> {
            }
        }
    }

    private fun enableLogin() {
        showProgressBar(true, getString(R.string.updating_item_codes))

        Statics.insertItemCodes()
        refreshUsers()

        setButton(ButtonStyle.READY)
        showProgressBar(false, "")
    }

    override fun onDownloadFileTask(
        msg: String,
        fileType: DownloadDb.FileType,
        downloadStatus: DownloadDb.DownloadStatus,
        progress: Int?,
    ) {
    }

    override fun onTaskSetupProxyEnded(
        status: ProgressStatus,
        email: String,
        password: String,
        installationCode: String,
    ) {
        if (status == ProgressStatus.finished) {
            Statics.getConfig(
                callback = this,
                email = email,
                password = password,
                installationCode = installationCode
            )
        }
    }

    private var userSpinnerFragment: UserSpinnerFragment? = null

    private var firstTime = true
    private var rejectNewInstances = false

    private var isReturnedFromSettings = false

    private var user: User? = null
    private var password: String = ""

    private fun setEditTextFocus(isFocused: Boolean) {
        binding.passwordEditText.isCursorVisible = isFocused
        binding.passwordEditText.isFocusable = isFocused
        binding.passwordEditText.isFocusableInTouchMode = isFocused

        if (isFocused) {
            binding.passwordEditText.requestFocus()
        }
    }

    private var connectionSuccess = false

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
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.rounded_corner_button_gold,
                    null
                )
            binding.loginImageView.contentDescription = getString(R.string.retry_connection)
            binding.loginImageView.foregroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
        }
    }

    private fun setWaitButton() {
        runOnUiThread {
            binding.loginImageView.setImageResource(R.drawable.ic_hourglass)
            binding.loginImageView.background =
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.rounded_corner_button_steelblue,
                    null
                )
            binding.loginImageView.contentDescription = getString(R.string.connecting)
            binding.loginImageView.foregroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    resources,
                    R.color.white,
                    null
                )
            )
        }
    }

    private fun setLoginButton() {
        runOnUiThread {
            binding.loginImageView.setImageResource(R.drawable.ic_check)
            binding.loginImageView.background =
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.rounded_corner_button_seagreen,
                    null
                )
            binding.loginImageView.contentDescription = getString(R.string.sign_in)
            binding.loginImageView.foregroundTintList = ColorStateList.valueOf(
                ResourcesCompat.getColor(
                    resources,
                    R.color.cornsilk,
                    null
                )
            )
        }
    }

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

            initialSetup()
        }
    }

    private fun refreshUsers() {
        if (Statics.downloadDbRequired) {
            return
        }

        try {
            runOnUiThread { userSpinnerFragment!!.reFill() }
        } catch (ex: Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        }

        attemptSync = false
    }

    override fun onItemSelected(user: User?) {
        // The user selected the headline of an article from the HeadlinesFragment
        // Do something here to display that article

        // El ADAPTER dispara siempre este evento la primera vez
        if (firstTime) {
            firstTime = false
            return
        }

        setEditTextFocus(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch setOnTouchListener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, motionEvent ->
                closeKeyboard(this)
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
            (0 until view.childCount)
                .map { view.getChildAt(it) }
                .forEach { setupUI(it) }
        }
    }

    override fun onBackPressed() {
        // Esto sirve para salir del programa desde la pantalla de Login
        moveTaskToBack(true)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putParcelable("user", userSpinnerFragment!!.selectedUser)
        savedInstanceState.putString("password", binding.passwordEditText.text.toString())
    }

    private lateinit var binding: LoginActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //// INICIALIZAR CONTROLES
        userSpinnerFragment =
            supportFragmentManager.findFragmentById(R.id.userSpinnerFragment) as UserSpinnerFragment?

        if (savedInstanceState != null) {
            user = savedInstanceState.getParcelable("user")
            password = savedInstanceState.getString("pass") ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                user = Parcels.unwrap<User>(extras.getParcelable<User>("user"))
                password = extras.getString("password") ?: ""
            }
        }

        // region Mostrar número de versión, etc...
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val str = "${getString(R.string.app_milestone)} ${pInfo.versionName}"
        binding.versionTextView.text = str
        // endregion

        // region Mostar imágen de cabecera
        binding.imageView.setImageResource(0)

        var draw = ContextCompat.getDrawable(this, R.drawable.wc)
        draw = resize(draw!!)

        binding.imageView.setImageDrawable(draw)
        // endregion

        userSpinnerFragment!!.selectedUser = user

        binding.passwordEditText.setText(password, TextView.BufferType.EDITABLE)
        binding.passwordEditText.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP &&
                (keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                binding.loginImageView.performClick()
                true
            } else {
                false
            }
        }
        binding.passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                Statics.showKeyboard(this)
            } else {
                closeKeyboard(this)
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

                runOnUiThread { attemptLogin() }
            } else {
                initialSetup()
            }
        }

        initialSetup()

        // ESTO SIRVE PARA OCULTAR EL TECLADO EN PANTALLA CUANDO PIERDEN EL FOCO LOS CONTROLES QUE LO NECESITAN
        setupUI(binding.login)
    }

    private fun showProgressBar(show: Boolean, msg: String) {
        runOnUiThread {
            if (show) {
                binding.syncStatusTextView.text = msg
                binding.progressBarLayout.visibility = VISIBLE
                binding.progressBarLayout.bringToFront()

                ViewCompat.setZ(binding.progressBarLayout, 0F)
            } else {
                binding.progressBarLayout.visibility = GONE
            }
        }
    }

    private fun initSync() {
        try {
            thread {
                val dbLocation = GetDatabaseLocation()
                dbLocation.addParams(this)
                dbLocation.execute()
            }
        } catch (ex: Exception) {
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())

            setButton(ButtonStyle.REFRESH)
            attemptSync = false
        }
    }

    private var attemptSync: Boolean = false
    private fun initialSetup() {
        if (attemptSync) return
        attemptSync = true

        setButton(ButtonStyle.BUSY)

        if (Statics.urlPanel.isEmpty()) {
            makeText(binding.root, text = getString(R.string.server_is_not_configured), ERROR)

            setButton(ButtonStyle.REFRESH)
            attemptSync = false
        } else {
            try {
                Statics.closeImageControl()
                initSync()
            } catch (ex: Exception) {
                showProgressBar(false, "")
                makeText(binding.root, text = ex.message.toString(), ERROR)
                ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())

                setButton(ButtonStyle.REFRESH)
                attemptSync = false
            }
        }
    }

    private fun configApp() {
        val realPass = prefsGetString(Preference.confPassword)
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

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
        val realPass = prefsGetString(Preference.confPassword)
        if (password == realPass) {
            Statics.setDebugConfigValues()

            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent =
                    Intent(Statics.WarehouseCounter.getContext(), SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
            isReturnedFromSettings = true
        } else {
            makeText(binding.root, getString(R.string.invalid_password), ERROR)
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

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.5).toInt(), (bitmap.height * 0.5).toInt(), false
        )
        return BitmapDrawable(resources, bitmapResized)
    }

    private var attemptRunning = false
    private fun attemptLogin() {
        if (attemptRunning) {
            return
        }
        attemptRunning = true

        // Store values at the time of the login attempt.
        val userId = userSpinnerFragment!!.selectedUserId ?: 0L
        val userPass = userSpinnerFragment!!.selectedUserPass
        val password = binding.passwordEditText.text.toString()

        runOnUiThread {
            attemptLogin(userId.toLong(), userPass, password)
        }
    }

    private fun attemptLogin(userId: Long?, userPass: String, password: String) {
        binding.passwordEditText.error = null

        var cancel = false
        var focusView: View? = null

        // Check for a valid email address.
        if (userId == null || userId <= 0) {
            //userSpinnerFragment.setError(getString(R.string.error_field_required));
            focusView = userSpinnerFragment!!.view
            makeText(binding.root, getString(R.string.you_must_select_a_user), ERROR)
            cancel = true
        } else if (!isUserValid(userId)) {
            //userSpinnerFragment.setError(getString(R.string.error_invalid_email));
            focusView = userSpinnerFragment!!.view
            makeText(binding.root, getString(R.string.you_must_select_a_user), ERROR)
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            if (userPass == Md5.getMd5(password)) {
                Statics.currentUserId = user!!.userId
                Statics.currentUserName = user!!.name
                Statics.setupImageControl()

                finish()
            } else {
                makeText(binding.root, getString(R.string.wrong_user_password_combination), ERROR)
            }
        }

        attemptRunning = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            JotterListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (Statics.prefsGetBoolean(Preference.showScannedCode) && BuildConfig.DEBUG)
            makeText(binding.root, scanCode, SnackBarType.INFO)

        JotterListener.lockScanner(this, true)

        try {
            val mainJson = JSONObject(scanCode)

            // LOGIN
            if (scanCode.contains("log_user_name") && scanCode.contains("log_password")) {
                val confJson = mainJson.getJSONObject(appName)

                val userName = confJson.getString("log_user_name")
                val password = confJson.getString("log_password")

                if (userName.isNotEmpty() && password.isNotEmpty()) {
                    val uDbH = UserDbHelper()
                    val user = uDbH.selectByName(userName).firstOrNull()
                    if (user != null) {
                        runOnUiThread {
                            attemptLogin(user.userId, user.password ?: "", password)
                        }
                    } else {
                        makeText(binding.root, getString(R.string.invalid_user), ERROR)
                    }
                } else {
                    makeText(
                        binding.root,
                        getString(R.string.invalid_code),
                        ERROR
                    )
                }
                return
            }

            when {
                mainJson.has("config") -> {
                    // CLIENT CONFIGURATION
                    try {
                        val adb = AlertDialog.Builder(this)
                        adb.setTitle(getString(R.string.download_database_required))
                        adb.setMessage(getString(R.string.download_database_required_question))
                        adb.setNegativeButton(R.string.cancel, null)
                        adb.setPositiveButton(R.string.accept) { _, _ ->
                            Statics.downloadDbRequired = true
                            Statics.getConfigFromScannedCode(
                                callback = this,
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
                mainJson.has(appName) -> {
                    // APP CONFIGURATION
                    Statics.getConfigFromScannedCode(
                        callback = this,
                        scanCode = scanCode,
                        mode = QRConfigApp
                    )
                }
                else -> {
                    makeText(
                        binding.root,
                        getString(R.string.invalid_code),
                        ERROR
                    )
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(
                binding.root,
                ex.message.toString(), ERROR
            )
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

        if (!Statics.prefsGetBoolean(Preference.showConfButton)) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!Statics.isRfidRequired()) {
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

    override fun onSpinnerFill(status: Int) {
        when (status) {
            Statics.STARTING -> {
                setButton(ButtonStyle.BUSY)
                showProgressBar(true, getString(R.string.loading_users))
            }
            Statics.CANCELED, Statics.CRASHED -> {
                setButton(ButtonStyle.REFRESH)
                showProgressBar(false, "")
            }
            Statics.FINISHED -> {
                showProgressBar(false, "")
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
        }
    }
}