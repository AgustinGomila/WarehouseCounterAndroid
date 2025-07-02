package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import com.dacosys.imageControl.room.database.IcDatabase
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v1.functions.GetClientPackages
import com.dacosys.warehouseCounter.data.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.data.ktor.v2.dto.database.DatabaseData
import com.dacosys.warehouseCounter.data.ktor.v2.functions.database.GetDatabase
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.data.room.dao.client.ClientCoroutines
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCategory.ItemCategoryCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCode.ItemCodeCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemRegex.ItemRegexCoroutines
import com.dacosys.warehouseCounter.data.room.dao.lot.LotCoroutines
import com.dacosys.warehouseCounter.data.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.data.room.database.WcDatabase
import com.dacosys.warehouseCounter.data.room.database.WcTempDatabase
import com.dacosys.warehouseCounter.data.room.database.helper.DownloadDb
import com.dacosys.warehouseCounter.data.room.database.helper.DownloadStatus
import com.dacosys.warehouseCounter.data.room.database.helper.FileType
import com.dacosys.warehouseCounter.data.room.database.helper.statusEnd
import com.dacosys.warehouseCounter.data.room.entity.user.User
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.data.sync.ClientPackage
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.getConfigFromScannedCode
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.selectClientPackage
import com.dacosys.warehouseCounter.databinding.LoginActivityBinding
import com.dacosys.warehouseCounter.misc.CurrentUser
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.appName
import com.dacosys.warehouseCounter.misc.imageControl.ImageControl.Companion.closeImageControl
import com.dacosys.warehouseCounter.misc.imageControl.ImageControl.Companion.setupImageControl
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.jotter.ScannerManager
import com.dacosys.warehouseCounter.ui.fragments.user.UserSpinnerFragment
import com.dacosys.warehouseCounter.ui.fragments.user.UserSpinnerFragment.Companion.SyncStatus.*
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import io.github.cdimascio.dotenv.DotenvBuilder
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import java.lang.ref.WeakReference
import kotlin.concurrent.thread


class LoginActivity : AppCompatActivity(), UserSpinnerFragment.OnItemSelectedListener,
    UserSpinnerFragment.OnSpinnerFillListener, Scanner.ScannerListener,
    ProxySetup.Companion.TaskSetupProxyEnded, ClientPackage.Companion.TaskConfigPanelEnded,
    DownloadDb.DownloadDbTask {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            showMessage(
                getString(R.string.configuration_applied), SUCCESS
            )
            initialSetup()
        } else if (status == ProgressStatus.crashed) {
            showMessage(getString(R.string.error_setting_user_panel), ERROR)
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
        val result: ArrayList<com.dacosys.warehouseCounter.data.ktor.v1.dto.clientPackage.Package> =
            packagesResult.result
        val clientEmail: String = packagesResult.clientEmail
        val clientPassword: String = packagesResult.clientPassword
        val msg: String = packagesResult.msg

        if (status == ProgressStatus.finished) {
            if (result.isNotEmpty()) {
                runOnUiThread {
                    selectClientPackage(
                        weakAct = WeakReference(this),
                        callback = this,
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword,
                        onEventData = { showMessage(it.text, it.snackBarType) })
                }
            } else {
                showMessage(msg, SnackBarType.INFO)
            }
        } else if (status == ProgressStatus.success) {
            showMessage(msg, SUCCESS)
        } else if (status == ProgressStatus.crashed) {
            showMessage(msg, ERROR)
        }
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        if (isFinishing || isDestroyed) return
        if (type == ERROR) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    override fun onDownloadDbTask(downloadStatus: DownloadStatus) {
        if (downloadStatus in statusEnd()) {
            attemptSync = false
        }

        when (downloadStatus) {
            DownloadStatus.STARTING -> {
                setButton(ButtonStyle.BUSY)
                showProgressBar(getString(R.string.downloading_database))
            }

            DownloadStatus.FINISHED, /* FINISHED = Ok */
            DownloadStatus.CANCELED, /* CANCELED = Sin conexión */
                -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    setHeader()
                    closeCurrentInstances()

                    if (BuildConfig.DEBUG) {
                        setHeaderDbInfo()
                    }

                    enableLogin()
                }, 200)
            }

            DownloadStatus.CRASHED -> {
                setButton(ButtonStyle.REFRESH)
                hideProgressBar()
            }

            DownloadStatus.DOWNLOADING,
            DownloadStatus.COPYING,
                -> {
                setButton(ButtonStyle.BUSY)
            }

            DownloadStatus.INFO -> {
            }
        }
    }

    private fun onGetDatabaseData(data: DatabaseData) {
        thread {
            val sync = DownloadDb.Builder()
                .onEventData { showMessage(it.text, it.snackBarType) }
                .callBack(this)
                .timeFileUrl(data.dbDate)
                .dbFileUrl(data.dbFile)
                .build()
            sync.execute()
        }
    }

    private fun enableLogin() {
        refreshUsers()

        setButton(ButtonStyle.READY)
        hideProgressBar()
    }

    override fun onDownloadFileTask(
        msg: String,
        fileType: FileType,
        downloadStatus: DownloadStatus,
        progress: Int?,
    ) {
        if (downloadStatus == DownloadStatus.DOWNLOADING || downloadStatus == DownloadStatus.COPYING) {
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
            }
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

            binding.loginImageView.setOnClickListener {
                initialSetup()
            }
        }
    }

    private fun setWaitButton() {
        runOnUiThread {
            binding.loginImageView.setOnClickListener { }

            binding.loginImageView.setImageResource(R.drawable.ic_refresh_rotate)
            binding.loginImageView.background = ResourcesCompat.getDrawable(
                resources, R.drawable.rounded_corner_button_steelblue, null
            )
            binding.loginImageView.contentDescription = getString(R.string.connecting)
            binding.loginImageView.foregroundTintList =
                ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, null))

            val drawable: Drawable = binding.loginImageView.drawable ?: return@runOnUiThread
            val anim = createRotationAnimator(drawable)
            anim.start()
        }
    }

    private fun createRotationAnimator(drawable: Drawable): Animator {
        val anim = ObjectAnimator.ofInt(drawable, "level", 0, 10000)
        anim.setDuration(2000)
        anim.repeatCount = Animation.INFINITE
        anim.interpolator = AccelerateDecelerateInterpolator()
        return anim
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

            binding.loginImageView.setOnClickListener {
                user = userSpinnerFragment?.selectedUser
                password = binding.passwordEditText.text.toString()
                attemptLogin()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        ScannerManager.lockScanner(this, false)
        rejectNewInstances = false

        CurrentUser.isLogged = false
    }

    private fun refreshUsers() {
        if (Statics.downloadDbRequired) return

        try {
            runOnUiThread { userSpinnerFragment?.reFill() }
        } catch (ex: Exception) {
            ErrorLog.writeLog(this, tag, ex.message.toString())
        }

        attemptSync = false
    }

    override fun onItemSelected(user: User?) {
        setEditTextFocus()
    }

    private fun isBackPressed() {
        moveTaskToBack(true)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putParcelable(ARG_USER, userSpinnerFragment?.selectedUser)
        savedInstanceState.putString(ARG_PASSWORD, binding.passwordEditText.text.toString())
    }

    private var userSpinnerFragment: UserSpinnerFragment? = null
    private var rejectNewInstances = false

    private var attemptSync = false
    private var attemptRunning = false

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

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        userSpinnerFragment = supportFragmentManager.findFragmentById(R.id.userSpinnerFragment) as UserSpinnerFragment?

        if (savedInstanceState != null) {
            user = savedInstanceState.parcelable(ARG_USER)
            password = savedInstanceState.getString(ARG_PASSWORD) ?: ""
        } else {
            val extras = intent.extras
            if (extras != null) {
                user = extras.parcelable<User>(ARG_USER)
                password = extras.getString(ARG_PASSWORD) ?: ""
            }
        }

        binding.passwordEditText.setText(password, TextView.BufferType.EDITABLE)
        binding.passwordEditText.setOnKeyListener { _, _, event ->
            if (isActionDone(event)) {
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
        binding.passwordEditText.setOnEditorActionListener { _, actionId, event ->
            return@setOnEditorActionListener if (isActionDone(actionId, event)) {
                binding.loginImageView.performClick()
                true
            } else {
                false
            }
        }
        binding.passwordEditText.requestFocus()

        Screen.setupUI(binding.root, this)
    }

    override fun onStart() {
        super.onStart()

        userSpinnerFragment?.selectedUser = user

        setHeader()

        initialSetup()
    }

    private fun setHeader() {
        runOnUiThread {
            setHeaderUserName()
            setHeaderVersion()
            setHeaderImageBanner()
        }
    }

    private fun setHeaderDbInfo() {
        runOnUiThread {
            binding.dbInfoLayout.visibility = VISIBLE
        }

        ItemCoroutines.count {
            runOnUiThread {
                binding.dbItemsTextView.text =
                    String.format(getString(R.string._0_items), it.toString())
            }
        }
        ItemCategoryCoroutines.count {
            runOnUiThread {
                binding.dbCategoriesTextView.text =
                    String.format(getString(R.string._0_categories), it.toString())
            }
        }
        ClientCoroutines.count {
            runOnUiThread {
                binding.dbClientsTextView.text =
                    String.format(getString(R.string._0_clients), it.toString())
            }
        }
        ItemCodeCoroutines.count {
            runOnUiThread {
                binding.dbItemCodesTextView.text =
                    String.format(getString(R.string._0_item_codes), it.toString())
            }
        }
        ItemRegexCoroutines.count {
            runOnUiThread {
                binding.dbItemRegexTextView.text =
                    String.format(getString(R.string._0_item_regexs), it.toString())
            }
        }
        LotCoroutines.count {
            runOnUiThread {
                binding.dbLotTextView.text =
                    String.format(getString(R.string._0_lots), it.toString())
            }
        }
        UserCoroutines.count {
            runOnUiThread {
                binding.dbUserTextView.text =
                    String.format(getString(R.string._0_users), it.toString())
            }
        }
    }

    private fun setHeaderUserName() {
        binding.clientTextView.text = settingsVm.installationCode
    }

    private fun setHeaderVersion() {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val str = "${getString(R.string.app_milestone)} ${pInfo.versionName}"
        binding.versionTextView.text = str
    }

    private fun setHeaderImageBanner() {
        binding.imageView.setImageResource(0)
        var draw = ContextCompat.getDrawable(this, R.drawable.wc)
        draw = resize(draw!!)
        binding.imageView.setImageDrawable(draw)
    }

    private fun hideProgressBar() {
        showProgressBar()
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
            showMessage(context.getString(R.string.invalid_url), ERROR)
            return
        }

        thread {
            GetDatabase(
                onEvent = {
                    if (it.snackBarType != SUCCESS) showMessage(it.text, it.snackBarType)
                },
                onFinish = {
                    if (it != null) {
                        onGetDatabaseData(it)
                    } else {
                        hideProgressBar()
                        setButton(ButtonStyle.REFRESH)
                        attemptSync = false
                    }
                }).execute()
        }
    }

    private fun initialSetup() {
        if (attemptSync) return
        attemptSync = true

        setButton(ButtonStyle.BUSY)

        if (settingsVm.urlPanel.isEmpty()) {
            showMessage(getString(R.string.server_is_not_configured), ERROR)

            setButton(ButtonStyle.REFRESH)
            attemptSync = false
        } else {
            try {
                closeCurrentInstances()
                initSync()
            } catch (ex: Exception) {
                hideProgressBar()
                showMessage(ex.message.toString(), ERROR)
                ErrorLog.writeLog(this, tag, ex.message.toString())

                setButton(ButtonStyle.REFRESH)
                attemptSync = false
            }
        }
    }

    private fun closeCurrentInstances() {
        /** Cerramos ImageControl para evitar que se
         *  suban imágenes pendientes antes de autentificarse.
         *  Escenario en el que el usuario ha vuelto a esta
         *  actividad después haber estado autentificado.
         */
        closeImageControl()

        WcDatabase.cleanInstance()
        WcTempDatabase.cleanInstance()
        IcDatabase.cleanInstance()
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
            inputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE

            val input = TextInputEditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.isFocusable = true
            input.isFocusableInTouchMode = true
            input.setOnKeyListener { _, _, event ->
                if (isActionDone(event)) {
                    alertDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.performClick()
                    true
                } else {
                    false
                }
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

                val intent = Intent(context, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForSettings.launch(intent)
            }
        } else {
            showMessage(getString(R.string.invalid_password), ERROR)
        }
    }

    private val resultForSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Vamos a reconstruir el scanner por si cambió la configuración
            ScannerManager.autodetectDeviceModel(this)

            initialSetup()
        }

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = bitmap.scale((bitmap.width * 0.5).toInt(), (bitmap.height * 0.5).toInt(), false)
        return bitmapResized.toDrawable(resources)
    }

    private fun attemptLogin() {
        if (attemptRunning) return
        attemptRunning = true

        // Store values at the time of the login attempt.
        val userId = userSpinnerFragment?.selectedUserId ?: 0L
        val userPass = userSpinnerFragment?.selectedUserPass ?: ""
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
            showMessage(getString(R.string.you_must_select_a_user), ERROR)
            cancel = true
        } else if (!isUserValid(userId)) {
            showMessage(getString(R.string.you_must_select_a_user), ERROR)
            cancel = true
        }

        attemptRunning = false

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            runOnUiThread {
                userSpinnerFragment?.view?.requestFocus()
            }
        } else {
            // Show a progress spinner and kick off a background task to
            // perform the user login attempt.
            val tempUser = user
            if (tempUser != null && userPass == Md5.getMd5(password)) {

                CurrentUser.set(tempUser)

                setupImageControl()

                finish()
            } else {
                showMessage(getString(R.string.wrong_user_password_combination), ERROR)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode && BuildConfig.DEBUG) showMessage(
            scanCode, SnackBarType.INFO
        )

        ScannerManager.lockScanner(this, true)
        ScannerManager.hideWindow(this)

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
                            showMessage(getString(R.string.invalid_user), ERROR)
                        }
                    }
                } else {
                    showMessage(getString(R.string.invalid_code), ERROR)
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
                            ErrorLog.writeLog(this, tag, ex)
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
                    showMessage(getString(R.string.invalid_code), ERROR)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, tag, ex)
        } finally {
            // Unless is blocked, unlock the partial
            ScannerManager.lockScanner(this, false)
        }
    }

    private fun isUserValid(userId: Long): Boolean {
        return userId > 0
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)

        if (!settingsVm.showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingsVm.useRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.home, android.R.id.home -> {
                isBackPressed()
                return true
            }

            R.id.action_settings -> {
                configApp()
                true
            }

            R.id.action_rfid_connect -> {
                ScannerManager.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                if (Statics.TEST_MODE && BuildConfig.DEBUG) {
                    val env = DotenvBuilder()
                        .directory("/assets")
                        .filename("env")
                        .load()

                    var username = env["CLIENT_EMAIL"]
                    var password = env["CLIENT_PASSWORD"]

                    if (settingsVm.clientEmail.contains(username)) {
                        username = env["CLIENT_EMAIL_ALT"]
                        password = env["CLIENT_PASSWORD_ALT"]
                    }

                    scannerCompleted("""{"config":{"client_email":"$username","client_password":"$password"}}""".trimIndent())
                    return super.onOptionsItemSelected(item)
                }

                ScannerManager.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                ScannerManager.toggleCameraFloatingWindowVisibility(this)
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
                hideProgressBar()
            }

            FINISHED -> {
                hideProgressBar()
                if ((userSpinnerFragment?.count ?: 0) < 1) {
                    setButton(ButtonStyle.REFRESH)
                    Log.d(
                        tag,
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