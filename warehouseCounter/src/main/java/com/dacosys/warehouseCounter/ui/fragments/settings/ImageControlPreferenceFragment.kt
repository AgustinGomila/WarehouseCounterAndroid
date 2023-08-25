package com.dacosys.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.dacosys.imageControl.Statics.Companion.albumFolder
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
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
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen

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
        parentFragmentManager.beginTransaction()
            .replace(id, prefFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // region: Tamaño de la imagen
        bindPreferenceSummaryToValue(this, settingRepository.icPhotoMaxHeightOrWidth)
        // endregion

        // region: QR de configuración
        val qrCodeButton = findPreference<Preference>("ic_qr_code")
        qrCodeButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val icUrl = settingViewModel.icWsServer
            val icNamespace = settingViewModel.icWsNamespace
            val icUserWs = settingViewModel.icWsUser
            val icPasswordWs = settingViewModel.icWsPass

            if (icUrl.isEmpty() || icNamespace.isEmpty() || icUserWs.isEmpty() || icPasswordWs.isEmpty()) {
                if (view != null) showSnackBar(
                    WarehouseCounterApp.context.getString(R.string.invalid_webservice_data), SnackBarType.ERROR
                )
                return@OnPreferenceClickListener false
            }

            if (requireActivity().isFinishing) return@OnPreferenceClickListener false

            ClientPackage.generateQrCode(
                screenSize = Screen.getScreenSize(requireActivity()),
                data = ClientPackage.getBarcodeForConfig(SettingsRepository.getImageControl(), Statics.appName),
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

        val scanConfigCode = findPreference<Preference>("scan_config_code")
        scanConfigCode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                okDoShit(QRConfigType.QRConfigImageControl)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                if (view != null) showSnackBar("${getString(R.string.error)}: ${ex.message}", SnackBarType.ERROR)
                ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                false
            }
        }

        val inputConfCodePref = findPreference<Preference>("input_config_code")
        inputConfCodePref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showSnackBar(getString(R.string.no_available_option), SnackBarType.INFO)
            true
        }
        // endregion

        // region: Datos de conexión
        bindPreferenceSummaryToValue(this, settingRepository.icWsServer)
        val urlPref = findPreference<Preference>(settingRepository.icWsServer.key)

        bindPreferenceSummaryToValue(this, settingRepository.icWsNamespace)
        val namespacePref = findPreference<Preference>(settingRepository.icWsNamespace.key)

        val userPref = findPreference<Preference>(settingRepository.icUser.key)
        val passPref = findPreference<Preference>(settingRepository.icPass.key)

        /*
        val userWsPref = findPreference<Preference>(P.icWsUser.key)
        val passWsPref = findPreference<Preference>(P.icWsPass.key)
        */

        val testButton = findPreference<Preference>("ic_test")
        testButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (urlPref != null && namespacePref != null && userPref != null && passPref != null) {
                val url = settingViewModel.icWsServer
                val namespace = settingViewModel.icWsNamespace

                testImageControlConnection(url = url, namespace = namespace)
            }
            true
        }

        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, settingRepository.icWsUser)
            bindPreferenceSummaryToValue(this, settingRepository.icWsPass)
            bindPreferenceSummaryToValue(this, settingRepository.icUser)
            bindPreferenceSummaryToValue(this, settingRepository.icPass)
        }
        // endregion

        // region: Proxy preferences
        val useProxyPref = findPreference<Preference>(settingRepository.icWsUseProxy.key) as SwitchPreference
        useProxyPref.setOnPreferenceChangeListener { _, newValue ->
            settingViewModel.icWsUseProxy = newValue == true
            true
        }
        bindPreferenceSummaryToValue(this, settingRepository.icWsProxy)
        bindPreferenceSummaryToValue(this, settingRepository.icWsProxyPort)

        /*
        val proxyUrlPref = findPreference<Preference>(P.icWsProxy.key)
        val proxyPortPref = findPreference<Preference>(P.icWsProxyPort.key)
        val useProxyPref = findPreference<Preference>(P.icWsUseProxy.key)
        val proxyUserPref = findPreference<Preference>(P.icWsProxyUser.key)
        val proxyPassPref = findPreference<Preference>(P.icWsProxyPass.key)
        */
        // endregion

        // region: Remove images cache
        val removeImagesCache = findPreference<Preference>("remove_images_cache")
        removeImagesCache?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            //code for what you want it to do
            val diaBox = askForDelete()
            diaBox.show()
            true
        }
        // endregion
    }

    private fun askForDelete(): AlertDialog {
        return AlertDialog.Builder(requireActivity())
            //set message, title, and icon
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.do_you_want_to_delete_the_image_cache_question))
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                val album = albumFolder
                if (album.isDirectory) {
                    val files = album.listFiles()
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
            if (view != null) showSnackBar(
                WarehouseCounterApp.context.getString(R.string.invalid_webservice_data), SnackBarType.INFO
            )
            return
        }
        ImageControlCheckUser { showSnackBar(it.text, it.snackBarType) }.execute()
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            if (view != null) showSnackBar(
                getString(R.string.configuration_applied), SnackBarType.INFO
            )
            FileHelper.removeDataBases()
            requireActivity().onBackPressed()
        } else if (status == ProgressStatus.crashed) {
            if (view != null) showSnackBar(getString(R.string.error_setting_user_panel), SnackBarType.ERROR)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        if (requireActivity().isDestroyed || requireActivity().isFinishing) return

        MakeText.makeText(requireView(), text, snackBarType)
    }
}
