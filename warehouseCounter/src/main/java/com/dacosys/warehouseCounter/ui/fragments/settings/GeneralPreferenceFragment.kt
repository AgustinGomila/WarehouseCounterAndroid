package com.dacosys.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.data.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.data.sync.ClientPackage
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.bindPreferenceSummaryToValue
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.okDoShit
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.io.File

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
        bindPreferenceSummaryToValue(this, settingRepository.divisionChar)
        bindPreferenceSummaryToValue(this, settingRepository.wcSyncInterval)
        bindPreferenceSummaryToValue(this, settingRepository.wcSyncRefreshOrder)

        findPreference<Preference>(settingRepository.registryError.key) as Preference
        findPreference<Preference>(settingRepository.showConfButton.key) as Preference
        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, settingRepository.confPassword)
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
                if (view != null) showSnackBar("${getString(R.string.error)}: ${ex.message}", SnackBarType.ERROR)
                ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                false
            }
        }

        val qrCodeButton = findPreference<Preference>("ac_qr_code")
        qrCodeButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {

            if (requireActivity().isFinishing) return@OnPreferenceClickListener false

            ClientPackage.generateQrCode(
                screenSize = Screen.getScreenSize(requireActivity()),
                data = ClientPackage.getBarcodeForConfig(SettingsRepository.getAppConf(), Statics.appName),
                onFinish = {

                    if (requireActivity().isFinishing) return@generateQrCode

                    val imageView = ImageView(activity)
                    imageView.setImageBitmap(it)
                    val builder = AlertDialog.Builder(requireActivity()).setTitle(R.string.configuration_qr_code)
                        .setMessage(R.string.scan_the_code_below_with_another_device_to_copy_the_configuration)
                        .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .setView(imageView)

                    builder.create().show()
                }
            )
            true
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(requireView(), text, snackBarType)
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
