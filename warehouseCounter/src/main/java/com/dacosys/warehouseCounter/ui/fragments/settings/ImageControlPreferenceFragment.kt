package com.dacosys.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.database.FileHelper
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.utils.ImageControlCheckUser
import com.dacosys.warehouseCounter.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.sync.ClientPackage
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.bindPreferenceSummaryToValue
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.okDoShit
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import java.io.File
import java.lang.ref.WeakReference

/**
 * This fragment shows notification preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
class ImageControlPreferenceFragment : PreferenceFragmentCompat(),
    ClientPackage.Companion.TaskConfigPanelEnded {
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

        bindPreferenceSummaryToValue(
            this,
            WarehouseCounterApp.settingRepository.icPhotoMaxHeightOrWidth
        )
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsServer)
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsNamespace)

        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsUser)
            bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsPass)
            bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icUser)
            bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icPass)
        }

        val urlEditText = findPreference<Preference>(WarehouseCounterApp.settingRepository.icWsServer.key)
        val namespaceEditText = findPreference<Preference>(WarehouseCounterApp.settingRepository.icWsNamespace.key)
        /*
        val userWsEditText = findPreference<Preference>(P.icWsUser.key)
        val passWsEditText = findPreference<Preference>(P.icWsPass.key)
        */
        val userEditText = findPreference<Preference>(WarehouseCounterApp.settingRepository.icUser.key)
        val passEditText = findPreference<Preference>(WarehouseCounterApp.settingRepository.icPass.key)

        findPreference<Preference>(WarehouseCounterApp.settingRepository.icWsUseProxy.key)
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsProxy)
        bindPreferenceSummaryToValue(this, WarehouseCounterApp.settingRepository.icWsProxyPort)

        /*
        val proxyUrlEditText = findPreference<Preference>(P.icWsProxy.key)
        val proxyPortEditText = findPreference<Preference>(P.icWsProxyPort.key)
        val useProxyCheckBox = findPreference<Preference>(P.icWsUseProxy.key)
        val proxyUserEditText = findPreference<Preference>(P.icWsProxyUser.key)
        val proxyPassEditText = findPreference<Preference>(P.icWsProxyPass.key)
        */

        val button = findPreference<Preference>("ic_test")
        button?.onPreferenceClickListener = Preference.OnPreferenceClickListener {

            if (urlEditText != null && namespaceEditText != null && userEditText != null && passEditText != null) {
                val url = WarehouseCounterApp.settingViewModel.icWsServer
                val namespace = WarehouseCounterApp.settingViewModel.icWsNamespace

                testImageControlConnection(url = url, namespace = namespace)
            }
            true
        }

        val removeImagesCache = findPreference<Preference>("remove_images_cache")
        removeImagesCache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //code for what you want it to do
            val diaBox = askForDelete()
            diaBox.show()
            true
        }

        val qrCodeButton = findPreference<Preference>("ic_qr_code")
        qrCodeButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val icUrl = WarehouseCounterApp.settingViewModel.icWsServer
            val icNamespace = WarehouseCounterApp.settingViewModel.icWsNamespace
            val icUserWs = WarehouseCounterApp.settingViewModel.icWsUser
            val icPasswordWs = WarehouseCounterApp.settingViewModel.icWsPass

            if (icUrl.isEmpty() || icNamespace.isEmpty() || icUserWs.isEmpty() || icPasswordWs.isEmpty()) {
                if (view != null) MakeText.makeText(
                    requireView(),
                    WarehouseCounterApp.context.getString(R.string.invalid_webservice_data),
                    SnackBarType.ERROR
                )
                return@OnPreferenceClickListener false
            }

            ClientPackage.generateQrCode(
                WeakReference(requireActivity()),
                ClientPackage.getBarcodeForConfig(SettingsRepository.getImageControl(), Statics.appName)
            )
            true
        }

        val scanConfigCode = findPreference<Preference>("scan_config_code")
        scanConfigCode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                okDoShit(QRConfigType.QRConfigImageControl)
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
    }

    private fun askForDelete(): AlertDialog {
        return AlertDialog.Builder(requireActivity())
            //set message, title, and icon
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.do_you_want_to_delete_the_image_cache_question))
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                //your deleting code
                val albumFolder = File(
                    WarehouseCounterApp.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
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
            if (view != null) MakeText.makeText(
                requireView(),
                WarehouseCounterApp.context.getString(R.string.invalid_webservice_data),
                SnackBarType.INFO
            )
            return
        }
        ImageControlCheckUser { showSnackBar(it) }.execute()
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            if (view != null) MakeText.makeText(
                requireView(), getString(R.string.configuration_applied), SnackBarType.INFO
            )
            FileHelper.removeDataBases()
            requireActivity().onBackPressed()
        } else if (status == ProgressStatus.crashed) {
            if (view != null) MakeText.makeText(
                requireView(), getString(R.string.error_setting_user_panel), SnackBarType.ERROR
            )
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        if (requireActivity().isDestroyed || requireActivity().isFinishing) return

        MakeText.makeText(requireView(), it.text, it.snackBarType)
    }
}