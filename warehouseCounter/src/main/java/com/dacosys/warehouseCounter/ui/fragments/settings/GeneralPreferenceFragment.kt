package com.dacosys.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.sync.ClientPackage
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.bindPreferenceSummaryToValue
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.okDoShit
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.File
import java.lang.ref.WeakReference

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
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.divisionChar)
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.wcSyncInterval)
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.wcSyncRefreshOrder)

        findPreference<Preference>(WarehouseCounterApp.settingRepository.registryError.key) as Preference
        findPreference<Preference>(WarehouseCounterApp.settingRepository.showConfButton.key) as Preference
        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.confPassword)
        }

        val removeLogFiles = findPreference<Preference>("remove_log_files")
        removeLogFiles?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //code for what you want it to do
            val diaBox = askForDelete()
            diaBox.show()
            true
        }

        val scanConfigCode = findPreference<Preference>("scan_config_code")
        scanConfigCode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                okDoShit(QRConfigType.QRConfigApp)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                if (view != null) MakeText.makeText(
                    requireView(), "${getString(R.string.error)}: ${ex.message}", SnackBarType.ERROR
                )
                ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                false
            }
        }

        val qrCodeButton = findPreference<Preference>("ac_qr_code")
        qrCodeButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            ClientPackage.generateQrCode(
                WeakReference(requireActivity()),
                ClientPackage.getBarcodeForConfig(SettingsRepository.getAppConf(), Statics.appName)
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