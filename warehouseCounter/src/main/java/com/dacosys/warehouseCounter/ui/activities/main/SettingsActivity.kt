package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputFilter
import android.text.Spanned
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.SettingsActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.generateQrCode
import com.dacosys.warehouseCounter.misc.Statics.Companion.getBarcodeForConfig
import com.dacosys.warehouseCounter.misc.Statics.Companion.getConfigFromScannedCode
import com.dacosys.warehouseCounter.misc.objects.collectorType.CollectorType
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.moshi.clientPackage.Package
import com.dacosys.warehouseCounter.retrofit.result.PackagesResult
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.rfid.RfidType
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt.Companion.STATE_CONNECTED
import com.dacosys.warehouseCounter.settings.QRConfigType
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigImageControl
import com.dacosys.warehouseCounter.settings.QRConfigType.CREATOR.QRConfigWebservice
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.custom.CollectorTypePreference
import com.dacosys.warehouseCounter.settings.custom.DevicePreference
import com.dacosys.warehouseCounter.settings.utils.DownloadController
import com.dacosys.warehouseCounter.settings.utils.ImageControlCheckUser
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.AccountPreferenceFragment.Companion.currentQRConfigType
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.AccountPreferenceFragment.Companion.okDoShit
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.google.android.gms.common.api.CommonStatusCodes
import java.io.File
import java.lang.ref.WeakReference
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.thread

/**
 * A [SettingsActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
 * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
 * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */

class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    Statics.Companion.TaskConfigPanelEnded, Scanner.ScannerListener {

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_headers, rootKey)
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val args: Bundle = pref.extras
        val fragment: Fragment =
            supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment ?: "")
        fragment.arguments = args

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction().replace(R.id.settings, fragment)
            .addToBackStack(null).commit()
        supportFragmentManager.setFragmentResultListener("requestKey", this) { key, _ ->
            if (key == "requestKey") title = pref.title
        }
        return true
    }

    private lateinit var binding: SettingsActivityBinding
    private lateinit var titleTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createOptionsMenu()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleTag = getString(R.string.settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            titleTag = savedInstanceState.getCharSequence("title").toString()
            title = titleTag
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                title = titleTag
            }
        }
    }

    private fun createOptionsMenu() {
        // Add menu items without overriding methods in the Activity
        (this as ComponentActivity).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                // Inflate the menu; this adds items to the action bar if it is present.
                menuInflater.inflate(R.menu.menu_read_activity, menu)
                menu.removeItem(menu.findItem(R.id.action_trigger_scan).itemId)
                menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
                menu.removeItem(menu.findItem(R.id.action_read_barcode).itemId)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                return when (menuItem.itemId) {
                    R.id.home, android.R.id.home -> {
                        onBackPressed()
                        true
                    }
                    R.id.action_read_barcode -> {
                        okDoShit(QRConfigApp)
                        true
                    }
                    else -> {
                        true
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        Statics.closeKeyboard(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current requireActivity() title so we can set it again after a configuration change
        outState.putCharSequence("title", title)
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            makeText(binding.settings, getString(R.string.configuration_applied), INFO)
            Statics.removeDataBases()
            onBackPressed()
        } else if (status == ProgressStatus.crashed) {
            makeText(binding.settings, getString(R.string.error_setting_user_panel), ERROR)
        }
    }

    private fun onGetPackagesEnded(packagesResult: PackagesResult) {
        val status: ProgressStatus = packagesResult.status
        val result: ArrayList<Package> = packagesResult.result
        val clientEmail: String = packagesResult.clientEmail
        val clientPassword: String = packagesResult.clientPassword
        val msg: String = packagesResult.msg

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
                makeText(binding.settings, msg, INFO)
            }
        } else if (status == ProgressStatus.success) {
            makeText(binding.settings, msg, SUCCESS)
        } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
            makeText(binding.settings, msg, ERROR)
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        makeText(binding.root, it.text, it.snackBarType)
    }

    class AccountPreferenceFragment : PreferenceFragmentCompat(),
        Statics.Companion.TaskConfigPanelEnded {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            var key = rootKey
            if (arguments != null) {
                key = requireArguments().getString("rootKey")
            }
            setPreferencesFromResource(R.xml.pref_account, key)
        }

        override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
            val prefFragment = AccountPreferenceFragment()
            val args = Bundle()
            args.putString("rootKey", preferenceScreen.key)
            prefFragment.arguments = args
            parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null)
                .commit()
        }

        private var alreadyAnsweredYes = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.

            if (BuildConfig.DEBUG) {
                bindPreferenceSummaryToValue(this, settingRepository().clientEmail)
                bindPreferenceSummaryToValue(this, settingRepository().clientPassword)
            }

            val emailEditText = findPreference<Preference>(settingRepository().clientEmail.key)
            emailEditText?.setOnPreferenceChangeListener { preference, newValue ->
                if (!alreadyAnsweredYes) {
                    val diaBox =
                        askForDownloadDbRequired(preference = preference, newValue = newValue)
                    diaBox.show()
                    false
                } else {
                    preference.summary = newValue.toString()
                    true
                }
            }

            val passwordEditText =
                findPreference<Preference>(settingRepository().clientPassword.key)
            passwordEditText?.setOnPreferenceChangeListener { preference, newValue ->
                if (!alreadyAnsweredYes) {
                    val diaBox =
                        askForDownloadDbRequired(preference = preference, newValue = newValue)
                    diaBox.show()
                    false
                } else {
                    preference.summary = newValue.toString()
                    true
                }
            }

            val selectPackageButton = findPreference<Preference>("select_package")
            selectPackageButton?.onPreferenceClickListener = OnPreferenceClickListener {
                if (emailEditText != null && passwordEditText != null) {
                    val email = settingViewModel().clientEmail
                    val password = settingViewModel().clientPassword

                    if (!alreadyAnsweredYes) {
                        val diaBox = askForDownloadDbRequired2(email = email, password = password)
                        diaBox.show()
                    } else {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            Statics.getConfig(
                                onEvent = { onGetPackagesEnded(it) },
                                email = email,
                                password = password,
                                installationCode = ""
                            )
                        }
                    }
                }
                true
            }

            val scanConfigCode = findPreference<Preference>("scan_config_code")
            scanConfigCode?.onPreferenceClickListener = OnPreferenceClickListener {
                try {
                    okDoShit(QRConfigClientAccount)
                    true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    if (view != null) makeText(
                        requireView(), "${getString(R.string.error)}: ${ex.message}", ERROR
                    )
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                    false
                }
            }

            val qrCodeButton = findPreference<Preference>("ac_qr_code")
            qrCodeButton?.onPreferenceClickListener = OnPreferenceClickListener {
                val urlPanel = settingViewModel().urlPanel
                val installationCode = settingViewModel().installationCode
                val clientEmail = settingViewModel().clientEmail
                val clientPassword = settingViewModel().clientPassword
                val clientPackage = settingViewModel().clientPackage

                if (urlPanel.isEmpty() || installationCode.isEmpty() || clientPackage.isEmpty() || clientEmail.isEmpty() || clientPassword.isEmpty()) {
                    if (view != null) makeText(
                        requireView(), context().getString(R.string.invalid_client_data), ERROR
                    )
                    return@OnPreferenceClickListener false
                }

                generateQrCode(
                    WeakReference(requireActivity()),
                    getBarcodeForConfig(SettingsRepository.getClient(), "config")
                )
                true
            }

            // Actualizar el programa
            val updateAppButton = findPreference<Preference>("update_app") as Preference
            updateAppButton.onPreferenceClickListener = OnPreferenceClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !requireContext().packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                        Uri.parse(
                            String.format(
                                "package:%s", requireContext().packageName
                            )
                        )
                    )
                    resultForRequestPackageInstall.launch(intent)
                } else {
                    // check storage permission granted if yes then start downloading file
                    checkStoragePermission()
                }
                true
            }

            // Si ya está loggeado, deshabilitar estas opciones
            if (Statics.currentUserId > 0) {
                passwordEditText?.isEnabled = false
                emailEditText?.isEnabled = false
                selectPackageButton?.isEnabled = false
                scanConfigCode?.isEnabled = false
            }
        }

        private val resultForRequestPackageInstall =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it?.resultCode == CommonStatusCodes.SUCCESS || it?.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                    // check storage permission granted if yes then start downloading file
                    checkStoragePermission()
                }
            }

        private fun checkStoragePermission() {
            // Check if the storage permission has been granted
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is missing and must be requested.
                resultForStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return
            }

            // start downloading
            val downloadController = DownloadController(requireView())
            downloadController.enqueueDownload()
        }

        private val resultForStoragePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // returns boolean representing whether the
                // permission is granted or not
                if (!isGranted) {
                    makeText(
                        requireView(),
                        requireContext().getString(R.string.app_dont_have_necessary_permissions),
                        ERROR
                    )
                } else {
                    // start downloading
                    val downloadController = DownloadController(requireView())
                    downloadController.enqueueDownload()
                }
            }

        private fun askForDownloadDbRequired2(
            email: String,
            password: String,
        ): AlertDialog {
            return AlertDialog.Builder(requireActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.download_database_required))
                .setMessage(getString(R.string.download_database_required_question))
                .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                    //your deleting code
                    Statics.downloadDbRequired = true
                    alreadyAnsweredYes = true

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        Statics.getConfig(
                            onEvent = { onGetPackagesEnded(it) },
                            email = email,
                            password = password,
                            installationCode = ""
                        )
                    }
                    dialog.dismiss()
                }.setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }.create()
        }

        private fun askForDownloadDbRequired(
            preference: Preference,
            newValue: Any,
        ): AlertDialog {
            return AlertDialog.Builder(requireActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.download_database_required))
                .setMessage(getString(R.string.download_database_required_question))
                .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                    //your deleting code
                    Statics.downloadDbRequired = true
                    preference.summary = newValue.toString()
                    alreadyAnsweredYes = true
                    if (newValue is String) {
                        SettingsRepository.getByKey(preference.key)?.value = newValue
                    }
                    dialog.dismiss()
                }.setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }.create()
        }

        companion object {
            //region CAMERA SCAN
            var currentQRConfigType = QRConfigApp

            fun okDoShit(qrConfigType: QRConfigType) {
                currentQRConfigType = qrConfigType
                // TODO: JotterListener.toggleCameraFloatingWindowVisibility(this)
            }
            //endregion CAMERA READER

            fun equals(a: Any?, b: Any?): Boolean {
                return a != null && a == b
            }

            /**
             * Binds a preference's summary to its value. More specifically, when the
             * preference's value is changed, its summary (line of text below the
             * preference title) is updated to reflect the value. The summary is also
             * immediately updated upon calling this method. The exact display format is
             * dependent on the type of preference.
             *
             * @see .sBindPreferenceSummaryToValueListener
             */
            private fun bindPreferenceSummaryToValue(
                frag: PreferenceFragmentCompat,
                pref: com.dacosys.warehouseCounter.settings.Preference,
            ) {
                val preference = frag.findPreference<Preference>(pref.key)
                val all: Map<String, *> =
                    PreferenceManager.getDefaultSharedPreferences(context()).all

                // Set the listener to watch for value changes.
                preference?.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

                val defaultValue: Any = pref.value

                when {
                    all[pref.key] is String && preference != null -> {
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                            preference,
                            PreferenceManager.getDefaultSharedPreferences(preference.context)
                                .getString(preference.key, defaultValue.toString())
                        )
                    }
                    all[pref.key] is Boolean && preference != null -> {
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                            preference,
                            PreferenceManager.getDefaultSharedPreferences(preference.context)
                                .getBoolean(preference.key, defaultValue.toString().toBoolean())
                        )
                    }
                    all[pref.key] is Float && preference != null -> {
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                            preference,
                            PreferenceManager.getDefaultSharedPreferences(preference.context)
                                .getFloat(preference.key, defaultValue.toString().toFloat())
                        )
                    }
                    all[pref.key] is Int && preference != null -> {
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                            preference,
                            PreferenceManager.getDefaultSharedPreferences(preference.context)
                                .getInt(preference.key, defaultValue.toString().toInt())
                        )
                    }
                    all[pref.key] is Long && preference != null -> {
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                            preference,
                            PreferenceManager.getDefaultSharedPreferences(preference.context)
                                .getLong(preference.key, defaultValue.toString().toLong())
                        )
                    }
                    else -> {
                        try {
                            if (preference != null) when (defaultValue) {
                                is String -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                    preference,
                                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                                        .getString(preference.key, defaultValue)
                                )
                                is Float -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                    preference,
                                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                                        .getFloat(preference.key, defaultValue)
                                )
                                is Int -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                    preference,
                                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                                        .getInt(preference.key, defaultValue)
                                )
                                is Long -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                    preference,
                                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                                        .getLong(preference.key, defaultValue)
                                )
                                is Boolean -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                    preference,
                                    PreferenceManager.getDefaultSharedPreferences(preference.context)
                                        .getBoolean(preference.key, defaultValue)
                                )
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                        }
                    }
                }
            }
        }

        private fun onGetPackagesEnded(packagesResult: PackagesResult) {
            val status: ProgressStatus = packagesResult.status
            val result: ArrayList<Package> = packagesResult.result
            val clientEmail: String = packagesResult.clientEmail
            val clientPassword: String = packagesResult.clientPassword
            val msg: String = packagesResult.msg

            if (status == ProgressStatus.finished) {
                if (result.size > 0) {
                    requireActivity().runOnUiThread {
                        Statics.selectClientPackage(callback = this,
                            weakAct = WeakReference(requireActivity()),
                            allPackage = result,
                            email = clientEmail,
                            password = clientPassword,
                            onEventData = { showSnackBar(it) })
                    }
                } else {
                    if (view != null) makeText(requireView(), msg, INFO)
                }
            } else if (status == ProgressStatus.success) {
                if (view != null) makeText(requireView(), msg, SUCCESS)
            } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                if (view != null) makeText(requireView(), msg, ERROR)
            }
        }

        override fun onTaskConfigPanelEnded(status: ProgressStatus) {
            if (status == ProgressStatus.finished) {
                if (view != null) makeText(
                    requireView(), getString(R.string.configuration_applied), INFO
                )
                Statics.removeDataBases()
                requireActivity().onBackPressed()
            } else if (status == ProgressStatus.crashed) {
                if (view != null) makeText(
                    requireView(), getString(R.string.error_setting_user_panel), ERROR
                )
            }
        }

        private fun showSnackBar(it: SnackBarEventData) {
            makeText(requireView(), it.text, it.snackBarType)
        }
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            if (arguments != null) {
                val key = requireArguments().getString("rootKey")
                setPreferencesFromResource(R.xml.pref_general, key)
            } else {
                setPreferencesFromResource(R.xml.pref_general, rootKey)
            }
        }

        override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
            val prefFragment = GeneralPreferenceFragment()
            val args = Bundle()
            args.putString("rootKey", preferenceScreen.key)
            prefFragment.arguments = args
            parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null)
                .commit()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(this, settingRepository().divisionChar)

            findPreference<Preference>(settingRepository().registryError.key) as Preference
            findPreference<Preference>(settingRepository().showConfButton.key) as Preference
            if (BuildConfig.DEBUG) {
                bindPreferenceSummaryToValue(this, settingRepository().confPassword)
            }

            val removeLogFiles = findPreference<Preference>("remove_log_files")
            removeLogFiles?.onPreferenceClickListener = OnPreferenceClickListener {
                //code for what you want it to do
                val diaBox = askForDelete()
                diaBox.show()
                true
            }

            val scanConfigCode = findPreference<Preference>("scan_config_code")
            scanConfigCode?.onPreferenceClickListener = OnPreferenceClickListener {
                try {
                    okDoShit(QRConfigApp)
                    true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    if (view != null) makeText(
                        requireView(), "${getString(R.string.error)}: ${ex.message}", ERROR
                    )
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                    false
                }
            }

            val qrCodeButton = findPreference<Preference>("ac_qr_code")
            qrCodeButton?.onPreferenceClickListener = OnPreferenceClickListener {
                generateQrCode(
                    WeakReference(requireActivity()),
                    getBarcodeForConfig(SettingsRepository.getAppConf(), Statics.appName)
                )
                true
            }
        }

        private fun askForDelete(): AlertDialog {
            return AlertDialog.Builder(requireActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.do_you_want_to_delete_the_old_error_logs_question))
                .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                    deleteRecursive(ErrorLog.errorLogPath)
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.create()
        }

        private fun deleteRecursive(fileOrDirectory: File) {
            if (fileOrDirectory.isDirectory) {
                val files = fileOrDirectory.listFiles()
                if (files != null && files.any()) {
                    for (file in files) {
                        deleteRecursive(file)
                    }
                }
            }

            fileOrDirectory.delete()
        }
    }


    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    class DevicesPreferenceFragment : PreferenceFragmentCompat(), Rfid.RfidDeviceListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            var key = rootKey
            if (arguments != null) {
                key = requireArguments().getString("rootKey")
            }
            setPreferencesFromResource(R.xml.pref_devices, key)

            // Llenar sólo el fragmento que se ve para evitar NullExceptions
            when (key) {
                "printer" -> {
                    setPrinterPref()
                }
                "rfid" -> {
                    setRfidPref()
                }
                "symbology" -> {}
                else -> {
                    setDevicesPref()
                }
            }
        }

        override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
            val prefFragment = DevicesPreferenceFragment()
            val args = Bundle()
            args.putString("rootKey", preferenceScreen.key)
            prefFragment.arguments = args
            parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null)
                .commit()
        }

        private lateinit var v: View
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            v = super.onCreateView(inflater, container, savedInstanceState)
            return v
        }

        override fun onGetBluetoothName(name: String) {
            rfidDeviceNamePreference?.summary = name
        }

        override fun onReadCompleted(scanCode: String) {}

        override fun onWriteCompleted(isOk: Boolean) {}

        private fun setupRfidReader() {
            try {
                if (settingViewModel().useBtRfid) {
                    Rfid.setListener(this, RfidType.vh75)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                makeText(v, getString(R.string.rfid_reader_not_initialized), INFO)
                ErrorLog.writeLog(activity, this::class.java.simpleName, ex)
            }
        }

        private fun setDevicesPref() {
            setPrinterPref()
            setCollectorPref()
            setRfidPref()
        }

        private fun setCollectorPref() {
            ////////////////// COLECTOR //////////////////
            bindPreferenceSummaryToValue(this, settingRepository().collectorType)

            // PERMITE ACTUALIZAR EN PANTALLA EL ITEM SELECCIONADO EN EL SUMMARY DEL CONTROL
            val collectorTypeListPreference =
                findPreference<Preference>(settingRepository().collectorType.key) as CollectorTypePreference
            if (collectorTypeListPreference.value == null) {
                // to ensure we don't selectByItemId a null value
                // set first value by default
                collectorTypeListPreference.setValueIndex(0)
            }

            collectorTypeListPreference.summary =
                CollectorType.getById(collectorTypeListPreference.value?.toInt() ?: 0).description
            collectorTypeListPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    preference.summary =
                        CollectorType.getById(newValue.toString().toInt()).description
                    Statics.collectorTypeChanged = true
                    true
                }
        }

        private val ipv4Regex =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"

        private val ipv4Pattern: Pattern = Pattern.compile(ipv4Regex)

        fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned?,
            dStart: Int,
            dEnd: Int,
        ): CharSequence? {
            if (source == "") return null // Para el backspace
            val builder = java.lang.StringBuilder(dest.toString())
            builder.replace(dStart, dEnd, source.subSequence(start, end).toString())
            val matcher: Matcher = ipv4Pattern.matcher(builder)
            return if (!matcher.matches()) "" else null
        }

        private var useBtPrinter = false
        private var useNetPrinter = false

        private fun getPrinterName(
            btPrinterName: CharSequence,
            netPrinterIp: CharSequence,
            netPrinterPort: CharSequence,
        ): String {
            val r = if (!useBtPrinter && !useNetPrinter) {
                getString(R.string.there_is_no_selected_printer)
            } else if (useBtPrinter && btPrinterName.isEmpty()) {
                getString(R.string.there_is_no_selected_printer)
            } else if (useNetPrinter && (netPrinterIp.isEmpty() || netPrinterPort.isEmpty())) {
                getString(R.string.there_is_no_selected_printer)
            } else {
                when {
                    useBtPrinter -> btPrinterName.toString()
                    useNetPrinter -> "$netPrinterIp ($netPrinterPort)"
                    else -> getString(R.string.there_is_no_selected_printer)
                }
            }
            return r
        }

        private fun setPrinterPref() {
            /////// PANTALLA DE CONFIGURACIÓN DE LA IMPRESORA ///////
            val printerPref = findPreference<Preference>("printer") as PreferenceScreen

            //region //// DEVICE LIST
            val deviceListPreference =
                findPreference<Preference>(settingRepository().printerBtAddress.key) as DevicePreference
            if (deviceListPreference.value == null) {
                // to ensure we don't selectByItemId a null value
                // set first value by default
                deviceListPreference.setValueIndex(0)
            }
            deviceListPreference.summary =
                if (deviceListPreference.entry.isNullOrEmpty()) getString(R.string.there_is_no_selected_printer)
                else deviceListPreference.entry
            deviceListPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, _ ->
                    val pn =
                        if (deviceListPreference.entry.isNullOrEmpty()) getString(R.string.there_is_no_selected_printer)
                        else deviceListPreference.entry.toString()
                    preference.summary = pn
                    if (useBtPrinter) printerPref.summary = pn
                    true
                }
            //endregion //// DEVICE LIST

            //region //// PRINTER IP / PORT
            val portNetPrinterPref =
                findPreference<Preference>(settingRepository().portNetPrinter.key) as EditTextPreference
            portNetPrinterPref.summary = portNetPrinterPref.text

            val ipNetPrinterPref =
                findPreference<Preference>(settingRepository().ipNetPrinter.key) as EditTextPreference
            ipNetPrinterPref.summary = ipNetPrinterPref.text

            ipNetPrinterPref.setOnBindEditTextListener {
                val filters = arrayOfNulls<InputFilter>(1)
                filters[0] = InputFilter { source, start, end, dest, dStart, dEnd ->
                    filter(source, start, end, dest, dStart, dEnd)
                }
                it.filters = filters
            }
            ipNetPrinterPref.setOnPreferenceChangeListener { _, newValue ->
                if (useNetPrinter && newValue != null) {
                    ipNetPrinterPref.summary = newValue.toString()
                    val pn = "$newValue (${portNetPrinterPref.text})"
                    printerPref.summary = pn
                }
                true
            }

            portNetPrinterPref.setOnPreferenceChangeListener { _, newValue ->
                if (useNetPrinter && newValue != null) {
                    portNetPrinterPref.summary = newValue.toString()
                    val pn = "${ipNetPrinterPref.text} ($newValue)"
                    printerPref.summary = pn
                }
                true
            }
            //endregion //// PRINTER IP / PORT

            //region //// USE BLUETOOTH / NET PRINTER
            val swPrefBtPrinter =
                findPreference<Preference>(settingRepository().useBtPrinter.key) as SwitchPreference
            useBtPrinter = swPrefBtPrinter.isChecked

            val swPrefNetPrinter =
                findPreference<Preference>(settingRepository().useNetPrinter.key) as SwitchPreference
            useNetPrinter = swPrefNetPrinter.isChecked

            swPrefBtPrinter.setOnPreferenceChangeListener { _, newValue ->
                useBtPrinter = newValue != null && newValue == true
                if (newValue == true) swPrefNetPrinter.isChecked = false
                val pn =
                    if (deviceListPreference.entry.isNullOrEmpty()) getString(R.string.there_is_no_selected_printer)
                    else deviceListPreference.entry.toString()
                if (useBtPrinter) printerPref.summary = pn
                true
            }
            swPrefNetPrinter.setOnPreferenceChangeListener { _, newValue ->
                useNetPrinter = newValue != null && newValue == true
                if (newValue == true) swPrefBtPrinter.isChecked = false
                val pn = "${ipNetPrinterPref.text} (${portNetPrinterPref.text})"
                if (useNetPrinter) printerPref.summary = pn
                true
            }
            //endregion //// USE BLUETOOTH / NET PRINTER

            //region //// POTENCIA Y VELOCIDAD
            val maxPower = 23
            val printerPowerPref =
                findPreference<Preference>(settingRepository().printerPower.key) as EditTextPreference
            printerPowerPref.summary = printerPowerPref.text
            printerPowerPref.setOnBindEditTextListener {
                val filters = arrayOf(InputFilter { source, _, _, dest, _, _ ->
                    try {
                        val input = (dest.toString() + source.toString()).toInt()
                        if (input in 1 until maxPower) return@InputFilter null
                    } catch (ignore: NumberFormatException) {
                    }
                    ""
                })
                it.filters = filters
            }

            val maxSpeed = 10
            val printerSpeedPref =
                findPreference<Preference>(settingRepository().printerSpeed.key) as EditTextPreference
            printerSpeedPref.summary = printerSpeedPref.text
            printerSpeedPref.setOnBindEditTextListener {
                val filters = arrayOf(InputFilter { source, _, _, dest, _, _ ->
                    try {
                        val input = (dest.toString() + source.toString()).toInt()
                        if (input in 1 until maxSpeed) return@InputFilter null
                    } catch (ignore: NumberFormatException) {
                    }
                    ""
                })
                it.filters = filters
            }
            //endregion //// POTENCIA Y VELOCIDAD

            //region //// CARACTER DE SALTO DE LÍNEA
            val swPrefCharLF =
                findPreference<Preference>("conf_printer_new_line_char_lf") as SwitchPreference
            val swPrefCharCR =
                findPreference<Preference>("conf_printer_new_line_char_cr") as SwitchPreference

            val lineSeparator = settingViewModel().lineSeparator
            if (lineSeparator == Char(10).toString()) swPrefCharLF.isChecked
            else if (lineSeparator == Char(13).toString()) swPrefCharCR.isChecked

            swPrefCharLF.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    settingViewModel().lineSeparator = Char(10).toString()
                    swPrefCharCR.isChecked = false
                }
                true
            }

            swPrefCharCR.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    settingViewModel().lineSeparator = Char(13).toString()
                    swPrefCharLF.isChecked = false
                }
                true
            }
            //endregion //// CARACTER DE SALTO DE LÍNEA

            printerPref.summary = if (!useBtPrinter && !useNetPrinter) getString(R.string.disabled)
            else getPrinterName(
                btPrinterName = deviceListPreference.entry ?: "",
                netPrinterIp = ipNetPrinterPref.text.toString(),
                netPrinterPort = portNetPrinterPref.toString()
            )
        }

        private var useRfid = false
        private var rfidSummary = ""
        private var rfidName = ""

        private val resultForRfidConnect =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it?.resultCode == CommonStatusCodes.SUCCESS || it?.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                    setupRfidReader()
                }
            }

        private val resultForRfidPermissionConnect =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // returns boolean representind whether the
                // permission is granted or not
                if (!isGranted) {
                    makeText(
                        v, context().getString(R.string.app_dont_have_necessary_permissions), ERROR
                    )
                } else {
                    setupRfidReader()
                }
            }

        // Esta preferencia se utiliza al recibir el nombre del dispositivo
        // RFID seleccionado para modificar el texto de su sumario.
        private var rfidDeviceNamePreference: EditTextPreference? = null

        private fun setRfidPref() {
            ////////////////// RFID DEVICE //////////////////
            val rfidPref = findPreference<Preference>("rfid") as PreferenceScreen

            //region //// USE RFID
            val swPrefBtRfid =
                findPreference<Preference>(settingRepository().useBtRfid.key) as SwitchPreference
            useRfid = swPrefBtRfid.isChecked

            swPrefBtRfid.setOnPreferenceChangeListener { _, newValue ->
                useRfid = newValue != null && newValue == true
                rfidSummary =
                    (if (useRfid) getString(R.string.enabled) else getString(R.string.disabled)) + ": " + rfidName
                rfidPref.summary = rfidSummary

                thread { connectToRfidDevice() }
                true
            }
            //endregion //// USE RFID

            //region //// BLUETOOTH NAME
            rfidDeviceNamePreference =
                findPreference<Preference>("rfid_bluetooth_name") as EditTextPreference
            if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == STATE_CONNECTED) {
                (Rfid.rfidDevice as Vh75Bt).getBluetoothName()
            }
            rfidDeviceNamePreference!!.setOnPreferenceClickListener {
                if (Rfid.rfidDevice == null || (Rfid.rfidDevice as Vh75Bt).getState() != STATE_CONNECTED) {
                    makeText(v, getString(R.string.there_is_no_rfid_device_connected), ERROR)
                }
                true
            }
            rfidDeviceNamePreference!!.setOnPreferenceChangeListener { _, newValue ->
                if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == STATE_CONNECTED) {
                    (Rfid.rfidDevice as Vh75Bt).setBluetoothName(newValue.toString())
                } else {
                    makeText(v, getString(R.string.there_is_no_rfid_device_connected), ERROR)
                }
                true
            }
            //endregion //// BLUETOOTH NAME

            //region //// DEVICE LIST PREFERENCE
            val deviceListPreference =
                findPreference<Preference>(settingRepository().rfidBtAddress.key) as DevicePreference
            if (deviceListPreference.value == null) {
                // to ensure we don't selectByItemId a null value
                // set first value by default
                deviceListPreference.setValueIndex(0)
            }
            deviceListPreference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    rfidName = getBluetoothNameFromAddress(
                        newValue, getString(R.string.there_is_no_selected_rfid_scanner)
                    )

                    preference.summary = rfidName

                    rfidSummary =
                        (if (useRfid) getString(R.string.enabled) else getString(R.string.disabled)) + ": " + rfidName
                    rfidPref.summary = rfidSummary

                    // De este modo se actualiza el Summary del PreferenceScreen padre
                    //(preferenceScreen.rootAdapter as BaseAdapter).notifyDataSetChanged()
                    true
                }
            deviceListPreference.summary = rfidName
            //endregion //// DEVICE LIST PREFERENCE

            //region //// RFID POWER
            val rfidReadPower =
                findPreference<Preference>(settingRepository().rfidReadPower.key) as SeekBarPreference
            rfidReadPower.setOnPreferenceChangeListener { _, newValue ->
                rfidReadPower.summary = "$newValue dB"
                true
            }
            rfidReadPower.summary = "${settingViewModel().rfidReadPower} dB"
            //endregion //// RFID POWER

            //region //// RESET TO FACTORY
            val resetButton = findPreference<Preference>("rfid_reset_to_factory") as Preference
            resetButton.onPreferenceClickListener = OnPreferenceClickListener {
                if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == STATE_CONNECTED) {
                    val diaBox = askForResetToFactory()
                    diaBox.show()
                } else {
                    makeText(v, getString(R.string.there_is_no_rfid_device_connected), ERROR)
                }
                true
            }
            //endregion //// RESET TO FACTORY

            rfidName = if (deviceListPreference.entry == null || !swPrefBtRfid.isChecked) {
                getString(R.string.there_is_no_selected_rfid_scanner)
            } else {
                deviceListPreference.entry!!.toString()
            }

            rfidPref.summary =
                "${if (useRfid) getString(R.string.enabled) else getString(R.string.disabled)}: $rfidName"

            thread { connectToRfidDevice() }
        }

        private fun connectToRfidDevice() {
            if (!useRfid) return

            val bluetoothManager = context().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = bluetoothManager.adapter
            if (mBluetoothAdapter == null) {
                makeText(v, getString(R.string.there_are_no_bluetooth_devices), INFO)
            } else {
                if (!mBluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBtIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            resultForRfidPermissionConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        return
                    }
                    resultForRfidConnect.launch(enableBtIntent)
                } else {
                    setupRfidReader()
                }
            }
        }

        private fun askForResetToFactory(): AlertDialog {
            return AlertDialog.Builder(requireActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.reset_to_factory))
                .setMessage(getString(R.string.you_want_to_reset_the_rfid_device_to_its_factory_settings))
                .setPositiveButton(getString(R.string.reset)) { dialog, _ ->
                    //your deleting code
                    (Rfid.rfidDevice as Vh75Bt).resetToFactory()
                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.create()
        }

        private val resultForBtPermissionConnect =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                // returns boolean representind whether the
                // permission is granted or not
                if (!isGranted) {
                    makeText(
                        v, context().getString(R.string.app_dont_have_necessary_permissions), ERROR
                    )
                }
            }

        private fun getBluetoothNameFromAddress(address: Any?, summary: String): String {
            var s = summary

            if (address != null) {
                val bluetoothManager =
                    context().getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                val mBluetoothAdapter = bluetoothManager.adapter

                if (ActivityCompat.checkSelfPermission(
                        context(), Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        resultForBtPermissionConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    return s
                }

                val mPairedDevices = mBluetoothAdapter!!.bondedDevices
                if (mPairedDevices.size > 0) {
                    for (mDevice in mPairedDevices) {
                        if (mDevice.address == address.toString()) {
                            s = mDevice.name.toString()
                            break
                        }
                    }
                }
            }

            return s
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    class ImageControlPreferenceFragment : PreferenceFragmentCompat(),
        Statics.Companion.TaskConfigPanelEnded {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            var key = rootKey
            if (arguments != null) {
                key = requireArguments().getString("rootKey")
            }
            setPreferencesFromResource(R.xml.pref_image_control, key)
        }

        override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
            val prefFragment = ImageControlPreferenceFragment()
            val args = Bundle()
            args.putString("rootKey", preferenceScreen.key)
            prefFragment.arguments = args
            parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null)
                .commit()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            bindPreferenceSummaryToValue(this, settingRepository().icPhotoMaxHeightOrWidth)
            bindPreferenceSummaryToValue(this, settingRepository().icWsServer)
            bindPreferenceSummaryToValue(this, settingRepository().icWsNamespace)

            if (BuildConfig.DEBUG) {
                bindPreferenceSummaryToValue(this, settingRepository().icWsUser)
                bindPreferenceSummaryToValue(this, settingRepository().icWsPass)
                bindPreferenceSummaryToValue(this, settingRepository().icUser)
                bindPreferenceSummaryToValue(this, settingRepository().icPass)
            }

            val urlEditText = findPreference<Preference>(settingRepository().icWsServer.key)
            val namespaceEditText =
                findPreference<Preference>(settingRepository().icWsNamespace.key)
            /*
            val userWsEditText = findPreference<Preference>(P.icWsUser.key)
            val passWsEditText = findPreference<Preference>(P.icWsPass.key)
            */
            val userEditText = findPreference<Preference>(settingRepository().icUser.key)
            val passEditText = findPreference<Preference>(settingRepository().icPass.key)

            findPreference<Preference>(settingRepository().icWsUseProxy.key)
            bindPreferenceSummaryToValue(this, settingRepository().icWsProxy)
            bindPreferenceSummaryToValue(this, settingRepository().icWsProxyPort)

            /*
            val proxyUrlEditText = findPreference<Preference>(P.icWsProxy.key)
            val proxyPortEditText = findPreference<Preference>(P.icWsProxyPort.key)
            val useProxyCheckBox = findPreference<Preference>(P.icWsUseProxy.key)
            val proxyUserEditText = findPreference<Preference>(P.icWsProxyUser.key)
            val proxyPassEditText = findPreference<Preference>(P.icWsProxyPass.key)
            */

            val button = findPreference<Preference>("ic_test")
            button?.onPreferenceClickListener = OnPreferenceClickListener {

                if (urlEditText != null && namespaceEditText != null && userEditText != null && passEditText != null) {
                    val url = settingViewModel().icWsServer
                    val namespace = settingViewModel().icWsNamespace

                    testImageControlConnection(url = url, namespace = namespace)
                }
                true
            }

            val removeImagesCache = findPreference<Preference>("remove_images_cache")
            removeImagesCache?.onPreferenceClickListener = OnPreferenceClickListener {
                //code for what you want it to do
                val diaBox = askForDelete()
                diaBox.show()
                true
            }

            val qrCodeButton = findPreference<Preference>("ic_qr_code")
            qrCodeButton?.onPreferenceClickListener = OnPreferenceClickListener {
                val icUrl = settingViewModel().icWsServer
                val icNamespace = settingViewModel().icWsNamespace
                val icUserWs = settingViewModel().icWsUser
                val icPasswordWs = settingViewModel().icWsPass

                if (icUrl.isEmpty() || icNamespace.isEmpty() || icUserWs.isEmpty() || icPasswordWs.isEmpty()) {
                    if (view != null) makeText(
                        requireView(), context().getString(R.string.invalid_webservice_data), ERROR
                    )
                    return@OnPreferenceClickListener false
                }

                generateQrCode(
                    WeakReference(requireActivity()),
                    getBarcodeForConfig(SettingsRepository.getImageControl(), Statics.appName)
                )
                true
            }

            val scanConfigCode = findPreference<Preference>("scan_config_code")
            scanConfigCode?.onPreferenceClickListener = OnPreferenceClickListener {
                try {
                    okDoShit(QRConfigImageControl)
                    true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    if (view != null) makeText(
                        requireView(), "${getString(R.string.error)}: ${ex.message}", ERROR
                    )
                    ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                    false
                }
            }
        }

        private fun askForDelete(): AlertDialog {
            return AlertDialog.Builder(requireActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.do_you_want_to_delete_the_image_cache_question))
                .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                    //your deleting code
                    val albumFolder = File(
                        context().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "ImageControl"
                    )

                    if (albumFolder.isDirectory) {
                        val files = albumFolder.listFiles()
                        if (files != null && files.any()) {
                            for (file in files) {
                                if (file.isFile) {
                                    file.delete()
                                }
                            }
                        }
                    }

                    dialog.dismiss()
                }.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }.create()
        }

        private fun testImageControlConnection(
            url: String,
            namespace: String,
        ) {
            if (url.isEmpty() || namespace.isEmpty()) {
                if (view != null) makeText(
                    requireView(), context().getString(R.string.invalid_webservice_data), INFO
                )
                return
            }
            ImageControlCheckUser { showSnackBar(it) }.execute()
        }

        override fun onTaskConfigPanelEnded(status: ProgressStatus) {
            if (status == ProgressStatus.finished) {
                if (view != null) makeText(
                    requireView(), getString(R.string.configuration_applied), INFO
                )
                Statics.removeDataBases()
                requireActivity().onBackPressed()
            } else if (status == ProgressStatus.crashed) {
                if (view != null) makeText(
                    requireView(), getString(R.string.error_setting_user_panel), ERROR
                )
            }
        }

        private fun showSnackBar(it: SnackBarEventData) {
            if (requireActivity().isDestroyed || requireActivity().isFinishing) return

            makeText(requireView(), it.text, it.snackBarType)
        }
    }

    companion object {
        /**
         * A preference value change checkedChangedListener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener =
            Preference.OnPreferenceChangeListener { preference, value ->
                val stringValue = value.toString()

                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    val index = preference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                        if (index >= 0) preference.entries[index]
                        else null
                    )
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.summary = stringValue
                }
                true
            }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(
            frag: PreferenceFragmentCompat,
            pref: com.dacosys.warehouseCounter.settings.Preference,
        ) {
            val preference = frag.findPreference<Preference>(pref.key) ?: return
            val defaultValue: Any = pref.value
            bindPreferenceSummaryToValue(preference, defaultValue)
        }

        private fun bindPreferenceSummaryToValue(
            preference: Preference,
            defaultValue: Any?,
        ) {
            val all: Map<String, *> = PreferenceManager.getDefaultSharedPreferences(context()).all

            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            when {
                all[preference.key] is String -> {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, defaultValue.toString())
                    )
                }
                all[preference.key] is Boolean -> {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getBoolean(preference.key, defaultValue.toString().toBoolean())
                    )
                }
                all[preference.key] is Float -> {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getFloat(preference.key, defaultValue.toString().toFloat())
                    )
                }
                all[preference.key] is Int -> {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getInt(preference.key, defaultValue.toString().toInt())
                    )
                }
                all[preference.key] is Long -> {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(
                        preference,
                        PreferenceManager.getDefaultSharedPreferences(preference.context)
                            .getLong(preference.key, defaultValue.toString().toLong())
                    )
                }
                else -> {
                    try {
                        when (defaultValue) {
                            is String -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                preference,
                                PreferenceManager.getDefaultSharedPreferences(preference.context)
                                    .getString(preference.key, defaultValue)
                            )
                            is Float -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                preference,
                                PreferenceManager.getDefaultSharedPreferences(preference.context)
                                    .getFloat(preference.key, defaultValue)
                            )
                            is Int -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                preference,
                                PreferenceManager.getDefaultSharedPreferences(preference.context)
                                    .getInt(preference.key, defaultValue)
                            )
                            is Long -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                preference,
                                PreferenceManager.getDefaultSharedPreferences(preference.context)
                                    .getLong(preference.key, defaultValue)
                            )
                            is Boolean -> sBindPreferenceSummaryToValueListener.onPreferenceChange(
                                preference,
                                PreferenceManager.getDefaultSharedPreferences(preference.context)
                                    .getBoolean(preference.key, defaultValue)
                            )
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                    }
                }
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
        JotterListener.lockScanner(this, true)

        try {
            // No capturar códigos que cambian el servidor cuando está logeado.
            if (currentQRConfigType == QRConfigClientAccount || currentQRConfigType == QRConfigWebservice) {
                return
            }

            getConfigFromScannedCode(
                onEvent = { onGetPackagesEnded(it) },
                scanCode = scanCode,
                mode = currentQRConfigType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            makeText(binding.settings, ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }
}

