package com.dacosys.warehouseCounter.ui.fragments.settings

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.ktor.v1.dto.clientPackage.Package
import com.dacosys.warehouseCounter.ktor.v1.functions.GetClientPackages
import com.dacosys.warehouseCounter.ktor.v1.service.PackagesResult
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.database.FileHelper
import com.dacosys.warehouseCounter.settings.SettingsRepository
import com.dacosys.warehouseCounter.settings.utils.QRConfigType
import com.dacosys.warehouseCounter.sync.ClientPackage
import com.dacosys.warehouseCounter.sync.ProgressStatus
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.bindPreferenceSummaryToValue
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity.Companion.okDoShit
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import java.lang.ref.WeakReference

class AccountPreferenceFragment : PreferenceFragmentCompat(), ClientPackage.Companion.TaskConfigPanelEnded {
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
        parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null).commit()
    }

    private var alreadyAnsweredYes = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.

        if (BuildConfig.DEBUG) {
            bindPreferenceSummaryToValue(this, settingRepository.clientEmail)
            bindPreferenceSummaryToValue(this, settingRepository.clientPassword)
        }

        val emailEditText = findPreference<Preference>(settingRepository.clientEmail.key)
        emailEditText?.setOnPreferenceChangeListener { preference, newValue ->
            if (alreadyAnsweredYes) {
                preference.summary = newValue.toString()
                Statics.downloadDbRequired = true
                if (newValue is String) {
                    SettingsRepository.getByKey(preference.key)?.value = newValue
                }
                true
            } else {
                val diaBox = askForDownloadDbRequired(preference = preference, newValue = newValue)
                diaBox.show()
                false
            }
        }

        val passwordEditText = findPreference<Preference>(settingRepository.clientPassword.key)
        passwordEditText?.setOnPreferenceChangeListener { preference, newValue ->
            if (alreadyAnsweredYes) {
                preference.summary = newValue.toString()
                Statics.downloadDbRequired = true
                if (newValue is String) {
                    SettingsRepository.getByKey(preference.key)?.value = newValue
                }
                true
            } else {
                val diaBox = askForDownloadDbRequired(preference = preference, newValue = newValue)
                diaBox.show()
                false
            }
        }

        val selectPackageButton = findPreference<Preference>("select_package")
        selectPackageButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (emailEditText != null && passwordEditText != null) {
                val email = settingViewModel.clientEmail
                val password = settingViewModel.clientPassword

                if (alreadyAnsweredYes) {
                    Statics.downloadDbRequired = true
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        GetClientPackages.getConfig(
                            onEvent = { onGetPackagesEnded(it) },
                            email = email,
                            password = password,
                            installationCode = ""
                        )
                    }
                } else {
                    val diaBox = askForDownloadDbRequired2(email = email, password = password)
                    diaBox.show()
                }
            }
            true
        }

        val scanConfigCode = findPreference<Preference>("scan_config_code")
        scanConfigCode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                okDoShit(QRConfigType.QRConfigClientAccount)
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
            val urlPanel = settingViewModel.urlPanel
            val installationCode = settingViewModel.installationCode
            val clientEmail = settingViewModel.clientEmail
            val clientPassword = settingViewModel.clientPassword
            val clientPackage = settingViewModel.clientPackage

            if (urlPanel.isEmpty() || installationCode.isEmpty() || clientPackage.isEmpty() || clientEmail.isEmpty() || clientPassword.isEmpty()) {
                if (view != null) showSnackBar(
                    WarehouseCounterApp.context.getString(R.string.invalid_client_data),
                    SnackBarType.ERROR
                )
                return@OnPreferenceClickListener false
            }

            if (requireActivity().isFinishing) return@OnPreferenceClickListener false

            ClientPackage.generateQrCode(
                screenSize = Screen.getScreenSize(requireActivity()),
                data = ClientPackage.getBarcodeForConfig(SettingsRepository.getClient(), "config"),
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

        // Actualizar el programa
        val updateAppButton = findPreference<Preference>("update_app") as Preference
        updateAppButton.isEnabled = false
        updateAppButton.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showSnackBar(getString(R.string.no_available_option), SnackBarType.INFO)
            true
        }

        // Si ya está loggeado, deshabilitar estas opciones
        if (Statics.isLogged) {
            passwordEditText?.isEnabled = false
            emailEditText?.isEnabled = false
            selectPackageButton?.isEnabled = false
            scanConfigCode?.isEnabled = false
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
                    GetClientPackages.getConfig(
                        onEvent = { onGetPackagesEnded(it) }, email = email, password = password, installationCode = ""
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
        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
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
                    ClientPackage.selectClientPackage(callback = this,
                        weakAct = WeakReference(requireActivity()),
                        allPackage = result,
                        email = clientEmail,
                        password = clientPassword,
                        onEventData = { showSnackBar(it.text, it.snackBarType) })
                }
            } else {
                if (view != null) showSnackBar(msg, SnackBarType.INFO)
            }
        } else if (status == ProgressStatus.success) {
            if (view != null) showSnackBar(msg, SnackBarType.SUCCESS)
        } else if (status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
            if (view != null) showSnackBar(msg, SnackBarType.ERROR)
        }
    }

    override fun onTaskConfigPanelEnded(status: ProgressStatus) {
        if (status == ProgressStatus.finished) {
            if (view != null) showSnackBar(
                getString(R.string.configuration_applied), SnackBarType.INFO
            )
            FileHelper.removeDataBases()
            requireActivity().finish()
        } else if (status == ProgressStatus.crashed) {
            if (view != null) showSnackBar(
                getString(R.string.error_setting_user_panel), SnackBarType.ERROR
            )
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        MakeText.makeText(requireView(), text, snackBarType)
    }
}
