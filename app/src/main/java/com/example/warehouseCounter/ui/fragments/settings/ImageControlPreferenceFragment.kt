package com.example.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.example.warehouseCounter.BuildConfig
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.room.database.helper.FileHelper
import com.example.warehouseCounter.data.settings.SettingsRepository
import com.example.warehouseCounter.data.settings.utils.ImageControlCheckUser
import com.example.warehouseCounter.data.settings.utils.QRConfigType
import com.example.warehouseCounter.data.sync.ClientPackage
import com.example.warehouseCounter.misc.Statics
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.misc.objects.status.ProgressStatus
import com.example.warehouseCounter.ui.activities.main.SettingsActivity.Companion.bindPreferenceSummaryToValue
import com.example.warehouseCounter.ui.activities.main.SettingsActivity.Companion.okDoShit
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.utils.Screen
import java.io.File

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
        bindPreferenceSummaryToValue(this, settingsRepository.icPhotoMaxHeightOrWidth)
        // endregion

        // region: QR de configuración
        val qrCodeButton = findPreference<Preference>("ic_qr_code")
        qrCodeButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val icUrl = settingsVm.icWsServer
            val icNamespace = settingsVm.icWsNamespace
            val icUserWs = settingsVm.icWsUser
            val icPasswordWs = settingsVm.icWsPass

            if (icUrl.isEmpty() || icNamespace.isEmpty() || icUserWs.isEmpty() || icPasswordWs.isEmpty()) {
                if (view != null) showMessage(
                    WarehouseCounterApp.context.getString(R.string.invalid_webservice_data), ERROR
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
                if (view != null) showMessage("${getString(R.string.error)}: ${ex.message}", ERROR)
                ErrorLog.writeLog(null, this::class.java.simpleName, ex)
                false
            }
        }

        val inputConfCodePref = findPreference<Preference>("input_config_code")
        inputConfCodePref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showMessage(getString(R.string.no_available_option), SnackBarType.INFO)
            true
        }
        // endregion

        // region: Datos de conexión
        bindPreferenceSummaryToValue(this, settingsRepository.icWsServer)
        val urlPref = findPreference<Preference>(settingsRepository.icWsServer.key)

        bindPreferenceSummaryToValue(this, settingsRepository.icWsNamespace)
        val namespacePref = findPreference<Preference>(settingsRepository.icWsNamespace.key)

        val userPref = findPreference<Preference>(settingsRepository.icUser.key)
        val passPref = findPreference<Preference>(settingsRepository.icPass.key)

        /*
        val userWsPref = findPreference<Preference>(P.icWsUser.key)
        val passWsPref = findPreference<Preference>(P.icWsPass.key)
        */

        val testButton = findPreference<Preference>("ic_test")
        testButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (urlPref != null && namespacePref != null && userPref != null && passPref != null) {
                val url = settingsVm.icWsServer
                val namespace = settingsVm.icWsNamespace

                testImageControlConnection(url = url, namespace = namespace)
            }
            true
        }

        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, settingsRepository.icWsUser)
            bindPreferenceSummaryToValue(this, settingsRepository.icWsPass)
            bindPreferenceSummaryToValue(this, settingsRepository.icUser)
            bindPreferenceSummaryToValue(this, settingsRepository.icPass)
        }
        // endregion

        // region: Proxy preferences
        val useProxyPref = findPreference<Preference>(settingsRepository.icWsUseProxy.key) as SwitchPreference
        useProxyPref.setOnPreferenceChangeListener { _, newValue ->
            settingsVm.icWsUseProxy = newValue == true
            true
        }
        bindPreferenceSummaryToValue(this, settingsRepository.icWsProxy)
        bindPreferenceSummaryToValue(this, settingsRepository.icWsProxyPort)

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
            .setMessage(getString(R.string.do_you_want_to_delete_the_image_cache_question)).setPositiveButton(
                getString(R.string.delete)
            ) { dialog, _ ->
                //your deleting code
                val albumFolder = File(
                    WarehouseCounterApp.context.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES
                    ), "ImageControl"
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
            }.setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }.create()
    }

    private fun testImageControlConnection(
        url: String,
        namespace: String,
    ) {
        if (url.isEmpty() || namespace.isEmpty()) {
            if (view != null) showMessage(
                WarehouseCounterApp.context.getString(R.string.invalid_webservice_data), SnackBarType.INFO
            )
            return
        }
        ImageControlCheckUser { showMessage(it.text, it.snackBarType) }.execute()
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            if (view != null) showMessage(
                getString(R.string.configuration_applied), SnackBarType.INFO
            )
            FileHelper.removeDataBases()
            @Suppress("DEPRECATION") requireActivity().onBackPressed()
        } else if (status == ProgressStatus.crashed) {
            if (view != null) showMessage(getString(R.string.error_setting_user_panel), ERROR)
        }
    }

    private fun showMessage(msg: String, type: Int) {
        if (requireActivity().isFinishing || requireActivity().isDestroyed) return
        if (ERROR.equals(type)) Log.e(javaClass.simpleName, msg)
        makeText(requireView(), msg, type)
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        showMessage(msg, type.snackBarTypeId)
    }
}
