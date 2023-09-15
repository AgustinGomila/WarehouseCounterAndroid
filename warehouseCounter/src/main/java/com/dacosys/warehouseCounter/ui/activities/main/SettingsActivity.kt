package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sharedPreferences
import com.dacosys.warehouseCounter.data.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.data.room.database.helper.FileHelper.Companion.removeDataBases
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigApp
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigClientAccount
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType.CREATOR.QRConfigWebservice
import com.dacosys.warehouseCounter.data.sync.ClientPackage
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.getConfigFromScannedCode
import com.dacosys.warehouseCounter.data.sync.ClientPackage.Companion.selectClientPackage
import com.dacosys.warehouseCounter.databinding.SettingsActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.ui.fragments.settings.HeaderFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.lang.ref.WeakReference

/**
 * A [SettingsActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * category splits settings, with category headers shown to the left of
 * the settings list.
 *
 * See:
 * [Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the
 * [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    ClientPackage.Companion.TaskConfigPanelEnded, Scanner.ScannerListener {

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val args: Bundle = pref.extras
        val fragment: Fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment ?: "")
        fragment.arguments = args

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
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

        createOptionMenu()

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        titleTag = getString(R.string.settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            titleTag = savedInstanceState.getCharSequence(ARG_TITLE).toString()
            title = titleTag
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                title = titleTag
            }
        }
    }

    private fun createOptionMenu() {
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
                        @Suppress("DEPRECATION") onBackPressed()
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
        Screen.closeKeyboard(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current requireActivity() title, so we can set it again after a configuration change
        outState.putCharSequence(ARG_TITLE, title)
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            showSnackBar(getString(R.string.configuration_applied), INFO)
            removeDataBases()
            @Suppress("DEPRECATION") onBackPressed()
        } else if (status == ProgressStatus.crashed) {
            showSnackBar(getString(R.string.error_setting_user_panel), ERROR)
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
                showSnackBar(msg, INFO)
            }
        } else if (status == ProgressStatus.success) {
            showSnackBar(msg, SUCCESS)
        } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
            showSnackBar(msg, ERROR)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    companion object {
        const val ARG_TITLE = "title"

        //region CAMERA SCAN
        var currentQRConfigType = QRConfigApp

        fun okDoShit(qrConfigType: QRConfigType) {
            currentQRConfigType = qrConfigType
            // TODO: JotterListener.toggleCameraFloatingWindowVisibility(this)
        }
        //endregion CAMERA READER

        /**
         * A preference value change checkedChangedListener that updates the preference's summary
         * to reflect its new value.
         */
        private val commonPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, value ->
            // Fill summary
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
        fun bindPreferenceSummaryToValue(
            frag: PreferenceFragmentCompat,
            pref: com.dacosys.warehouseCounter.data.settings.Preference,
        ) {
            val preference = frag.findPreference<Preference>(pref.key) ?: return
            val defaultValue: Any = pref.value
            bindPreferenceSummaryToValue(preference, defaultValue)
        }

        private fun bindPreferenceSummaryToValue(
            preference: Preference,
            defaultValue: Any?,
        ) {
            val all: Map<String, *> = sharedPreferences.all

            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = commonPreferenceChangeListener

            when {
                all[preference.key] is String -> {
                    commonPreferenceChangeListener.onPreferenceChange(
                        preference,
                        sharedPreferences.getString(preference.key, defaultValue.toString())
                    )
                }

                all[preference.key] is Boolean -> {
                    commonPreferenceChangeListener.onPreferenceChange(
                        preference,
                        sharedPreferences.getBoolean(preference.key, defaultValue.toString().toBoolean())
                    )
                }

                all[preference.key] is Float -> {
                    commonPreferenceChangeListener.onPreferenceChange(
                        preference,
                        sharedPreferences.getFloat(preference.key, defaultValue.toString().toFloat())
                    )
                }

                all[preference.key] is Int -> {
                    commonPreferenceChangeListener.onPreferenceChange(
                        preference,
                        sharedPreferences.getInt(preference.key, defaultValue.toString().toInt())
                    )
                }

                all[preference.key] is Long -> {
                    commonPreferenceChangeListener.onPreferenceChange(
                        preference,
                        sharedPreferences.getLong(preference.key, defaultValue.toString().toLong())
                    )
                }

                else -> {
                    try {
                        when (defaultValue) {
                            is String -> commonPreferenceChangeListener.onPreferenceChange(
                                preference,
                                sharedPreferences.getString(preference.key, defaultValue)
                            )

                            is Float -> commonPreferenceChangeListener.onPreferenceChange(
                                preference,
                                sharedPreferences.getFloat(preference.key, defaultValue)
                            )

                            is Int -> commonPreferenceChangeListener.onPreferenceChange(
                                preference,
                                sharedPreferences.getInt(preference.key, defaultValue)
                            )

                            is Long -> commonPreferenceChangeListener.onPreferenceChange(
                                preference,
                                sharedPreferences.getLong(preference.key, defaultValue)
                            )

                            is Boolean -> commonPreferenceChangeListener.onPreferenceChange(
                                preference,
                                sharedPreferences.getBoolean(preference.key, defaultValue)
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
            // No capturar códigos que cambian el servidor cuando está autentificado.
            if (currentQRConfigType == QRConfigClientAccount || currentQRConfigType == QRConfigWebservice) return

            getConfigFromScannedCode(
                onEvent = { onGetPackagesEnded(it) }, scanCode = scanCode, mode = currentQRConfigType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            showSnackBar(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }
}
