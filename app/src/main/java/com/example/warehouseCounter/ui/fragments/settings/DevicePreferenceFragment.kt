package com.example.warehouseCounter.ui.fragments.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.settings.SettingsRepository
import com.example.warehouseCounter.data.settings.SettingsViewModel
import com.example.warehouseCounter.data.settings.custom.CollectorTypePreference
import com.example.warehouseCounter.data.settings.custom.DevicePreference
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.scanners.Collector.Companion.collectorTypeChanged
import com.example.warehouseCounter.scanners.collector.CollectorType
import com.example.warehouseCounter.scanners.collector.RfidType
import com.example.warehouseCounter.scanners.devices.rfid.Rfid
import com.example.warehouseCounter.scanners.devices.rfid.Rfid.Companion.appHasBluetoothPermission
import com.example.warehouseCounter.scanners.devices.vh75.Vh75Bt
import com.example.warehouseCounter.ui.activities.main.SettingsActivity
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * This fragment shows notification preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
class DevicePreferenceFragment : PreferenceFragmentCompat(), Rfid.RfidDeviceListener {

    private val vm: SettingsViewModel by lazy { settingsVm }
    private val sp: SettingsRepository by lazy { settingsRepository }
    private lateinit var printerPref: PreferenceScreen
    private lateinit var rfidPref: PreferenceScreen

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        var key = rootKey
        if (arguments != null) {
            key = requireArguments().getString("rootKey")
        }
        setPreferencesFromResource(R.xml.pref_devices, key)

        // Llenar sólo el fragmento que se ve para evitar NullExceptions
        when (key) {
            "printer" -> setPrinterPref()
            "rfid" -> setRfidPref()
            "symbology" -> {}
            else -> setDevicePref()
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        val prefFragment = DevicePreferenceFragment()
        val args = Bundle()
        args.putString("rootKey", preferenceScreen.key)
        prefFragment.arguments = args
        parentFragmentManager.beginTransaction().replace(id, prefFragment).addToBackStack(null)
            .commit()
    }

    private lateinit var currentView: View
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        currentView = super.onCreateView(inflater, container, savedInstanceState)
        return currentView
    }

    override fun onGetBluetoothName(name: String) {}

    override fun onReadCompleted(scanCode: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onStateChanged(state: Int) {}

    /**
     * Set device preferences
     * Llena todos los fragmentos de configuración de dispositivos.
     * Se usa cuando estamos en la pantalla principal de configuración de dispositivos.
     */
    private fun setDevicePref() {
        /* Para actualizar el sumario de la preferencia */
        /* PANTALLA DE CONFIGURACIÓN DE LA IMPRESORA */
        printerPref = findPreference<Preference>("printer") as PreferenceScreen
        printerPref.summaryProvider = Preference.SummaryProvider<PreferenceScreen> {
            getPrinterName()
        }

        /* RFID DEVICE */
        rfidPref = findPreference<Preference>("rfid") as PreferenceScreen
        rfidPref.summaryProvider = Preference.SummaryProvider<PreferenceScreen> {
            getRfidSummary()
        }

        setPrinterPref()
        setCollectorPref()
        setRfidPref()
    }

    private fun setCollectorPref() {
        ////////////////// COLECTOR //////////////////
        SettingsActivity.bindPreferenceSummaryToValue(this, sp.collectorType)

        // PERMITE ACTUALIZAR EN PANTALLA EL ITEM SELECCIONADO EN EL SUMMARY DEL CONTROL
        val collectorTypeListPreference = findPreference<Preference>(sp.collectorType.key) as CollectorTypePreference
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
                collectorTypeChanged = true
                true
            }
    }

    private fun getPrinterName(): String {
        val r = if (!vm.useBtPrinter && !vm.useNetPrinter) {
            getString(R.string.disabled)
        } else if (vm.useBtPrinter && (vm.printerBtAddress.isEmpty() || vm.printerBtAddress == "0")) {
            getString(R.string.there_is_no_selected_printer)
        } else if (vm.useNetPrinter && vm.ipNetPrinter.isEmpty()) {
            getString(R.string.there_is_no_selected_printer)
        } else {
            when {
                vm.useBtPrinter -> vm.printerBtAddress
                vm.useNetPrinter -> "${vm.ipNetPrinter} (${vm.portNetPrinter})"
                else -> getString(R.string.there_is_no_selected_printer)
            }
        }
        return r
    }

    private fun setPrinterPref() {
        //region //// DEVICE LIST
        val deviceListPreference = findPreference<Preference>(sp.printerBtAddress.key) as DevicePreference
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
                true
            }
        //endregion //// DEVICE LIST

        //region //// PRINTER IP / PORT
        val portNetPrinterPref = findPreference<Preference>(sp.portNetPrinter.key) as EditTextPreference
        portNetPrinterPref.summary = portNetPrinterPref.text

        val ipNetPrinterPref = findPreference<Preference>(sp.ipNetPrinter.key) as EditTextPreference
        ipNetPrinterPref.summary = ipNetPrinterPref.text
        ipNetPrinterPref.setOnBindEditTextListener { editText ->
            val filters = arrayOf(InputFilter { source, _, _, dest, _, _ ->
                val input = (dest.toString() + source.toString())
                if (input.matches(Regex("^([0-9]{1,3}\\.){0,3}[0-9]{0,3}\$"))) {
                    val segments = input.split('.')
                    if (segments.all { it.isEmpty() || (it.toIntOrNull() in 0..255) }) {
                        return@InputFilter null
                    }
                }
                ""
            })
            editText.filters = filters
        }
        ipNetPrinterPref.setOnPreferenceChangeListener { _, newValue ->
            if (vm.useNetPrinter && newValue != null) {
                ipNetPrinterPref.summary = newValue.toString()
            }
            true
        }

        portNetPrinterPref.setOnPreferenceChangeListener { _, newValue ->
            if (vm.useNetPrinter && newValue != null) {
                portNetPrinterPref.summary = newValue.toString()
            }
            true
        }
        //endregion //// PRINTER IP / PORT

        //region //// USE BLUETOOTH / NET PRINTER
        val swPrefBtPrinter = findPreference<Preference>(sp.useBtPrinter.key) as SwitchPreference

        val swPrefNetPrinter = findPreference<Preference>(sp.useNetPrinter.key) as SwitchPreference

        swPrefBtPrinter.setOnPreferenceChangeListener { _, newValue ->
            vm.useBtPrinter = newValue != null && newValue == true
            if (newValue == true) swPrefNetPrinter.isChecked = false
            true
        }
        swPrefNetPrinter.setOnPreferenceChangeListener { _, newValue ->
            vm.useNetPrinter = newValue != null && newValue == true
            if (newValue == true) swPrefBtPrinter.isChecked = false
            true
        }
        //endregion //// USE BLUETOOTH / NET PRINTER

        //region //// POTENCIA Y VELOCIDAD
        val maxPower = 23
        val printerPowerPref = findPreference<Preference>(sp.printerPower.key) as EditTextPreference
        printerPowerPref.summary = printerPowerPref.text
        printerPowerPref.setOnBindEditTextListener {
            val filters = arrayOf(InputFilter { source, _, _, dest, _, _ ->
                try {
                    val input = (dest.toString() + source.toString()).toInt()
                    if (input in 1 until maxPower) return@InputFilter null
                } catch (_: NumberFormatException) {
                }
                ""
            })
            it.filters = filters
        }
        printerPowerPref.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            sp.printerPower.value = newValue
            true
        }

        val maxSpeed = 10
        val printerSpeedPref = findPreference<Preference>(sp.printerSpeed.key) as EditTextPreference
        printerSpeedPref.summary = printerSpeedPref.text
        printerSpeedPref.setOnBindEditTextListener {
            val filters = arrayOf(InputFilter { source, _, _, dest, _, _ ->
                try {
                    val input = (dest.toString() + source.toString()).toInt()
                    if (input in 1 until maxSpeed) return@InputFilter null
                } catch (_: NumberFormatException) {
                }
                ""
            })
            it.filters = filters
        }
        printerSpeedPref.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            sp.printerSpeed.value = newValue
            true
        }
        //endregion //// POTENCIA Y VELOCIDAD

        //region //// CARÁCTER DE SALTO DE LÍNEA
        val swPrefCharLF = findPreference<Preference>("conf_printer_new_line_char_lf") as SwitchPreference
        val swPrefCharCR = findPreference<Preference>("conf_printer_new_line_char_cr") as SwitchPreference

        val lineSeparator = vm.lineSeparator
        if (lineSeparator == Char(10).toString()) swPrefCharLF.isChecked
        else if (lineSeparator == Char(13).toString()) swPrefCharCR.isChecked

        swPrefCharLF.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                vm.lineSeparator = Char(10).toString()
                swPrefCharCR.isChecked = false
            }
            true
        }

        swPrefCharCR.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                vm.lineSeparator = Char(13).toString()
                swPrefCharLF.isChecked = false
            }
            true
        }
        //endregion //// CARÁCTER DE SALTO DE LÍNEA
    }

    private fun showMessage(text: String, snackBarType: SnackBarType) {
        makeText(currentView, text, snackBarType)
    }

    // region RFID
    private fun getRfidSummary(): String {
        var rfidSummary =
            if (settingsVm.useRfid) getString(R.string.enabled)
            else getString(R.string.disabled)

        val btAddress = settingsVm.rfidBtAddress
        if (btAddress.isNotEmpty() && btAddress != 0.toString()) {
            var description = btAddress
            val btName = settingsVm.rfidBtName
            if (btName.isNotEmpty() && btName != 0.toString()) description = btName
            rfidSummary = "$rfidSummary: $description"
        }

        return rfidSummary
    }

    private fun setRfidPref() {
        resultForRfidBtPermissions()
    }

    private val vh75: Vh75Bt?
        get() = Rfid.vh75.takeIf { it?.state == Vh75Bt.STATE_CONNECTED }

    private fun resultForRfidBtPermissions() {
        if (!appHasBluetoothPermission()) {
            requestPermissionsRfid.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            setRfid()
        }
    }

    private val requestPermissionsRfid = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            setRfid()
        }
    }

    private fun setRfid() {
        //region //// USE RFID
        val swBtRfidSwitchPref: SwitchPreference? = findPreference(settingsRepository.useBtRfid.key)

        swBtRfidSwitchPref?.setOnPreferenceChangeListener { _, _ ->
            lifecycleScope.launch(IO) { connectToRfidDevice() }
            true
        }
        //endregion //// USE RFID

        //region //// BLUETOOTH NAME
        val rfidNamePref: EditTextPreference? = findPreference(settingsRepository.rfidBtName.key)
        rfidNamePref?.setOnPreferenceClickListener {
            if (vh75?.state != Vh75Bt.STATE_CONNECTED) {
                showMessage(getString(R.string.there_is_no_rfid_device_connected), ERROR)
            }
            true
        }
        rfidNamePref?.setOnPreferenceChangeListener { _, newValue ->
            if (vh75?.state == Vh75Bt.STATE_CONNECTED) {
                vh75?.setBluetoothName(newValue.toString())
            } else {
                showMessage(getString(R.string.there_is_no_rfid_device_connected), ERROR)
            }
            true
        }
        rfidNamePref?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
        //endregion //// BLUETOOTH NAME

        //region //// DEVICE LIST PREFERENCE
        val deviceListPref: DevicePreference? = findPreference(settingsRepository.rfidBtAddress.key)
        if (deviceListPref?.value == null) {
            // to ensure we don't selectByItemId a null value
            // set first value by default
            deviceListPref?.setValueIndex(0)
        }
        deviceListPref?.summaryProvider = Preference.SummaryProvider<DevicePreference> { preference ->
            val text = preference.entry
            if (text.isNullOrEmpty()) {
                getString(R.string.there_is_no_selected_rfid_scanner)
            } else {
                text
            }
        }
        deviceListPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                val entry = deviceListPref?.entry
                if (!entry.isNullOrEmpty()) {
                    settingsVm.rfidBtName = entry.toString()
                }
                true
            }
        //endregion //// DEVICE LIST PREFERENCE

        //region //// RFID POWER
        val rfidReadPower: SeekBarPreference? = findPreference(settingsRepository.rfidReadPower.key)
        rfidReadPower?.setOnPreferenceChangeListener { _, newValue ->
            rfidReadPower.summary = "$newValue dB"
            true
        }
        rfidReadPower?.summaryProvider = Preference.SummaryProvider<SeekBarPreference> { preference ->
            "${preference.value} dB"
        }
        //endregion //// RFID POWER

        //region //// RESET TO FACTORY
        val resetPref: Preference? = findPreference("rfid_reset_to_factory")
        resetPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (vh75?.state == Vh75Bt.STATE_CONNECTED) {
                val diaBox = askForResetToFactory()
                diaBox.show()
            } else {
                showMessage(getString(R.string.there_is_no_rfid_device_connected), ERROR)
            }
            true
        }
        //endregion //// RESET TO FACTORY

        lifecycleScope.launch(IO) { connectToRfidDevice() }
    }

    private fun connectToRfidDevice() {
        if (!settingsVm.useRfid) return

        val bluetoothManager =
            requireContext().getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            showMessage(getString(R.string.there_are_no_bluetooth_devices), INFO)
        } else {
            if (mBluetoothAdapter.isEnabled) {
                setupRfidReader()
                return
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            resultForRfidConnect.launch(enableBtIntent)
        }
    }

    private fun setupRfidReader() {
        try {
            if (Rfid.isRfidRequired(this::class)) {
                Rfid.setListener(this, RfidType.vh75)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            showMessage(getString(R.string.rfid_reader_not_initialized), INFO)
            ErrorLog.writeLog(activity, this::class.java.simpleName, ex)
        }
    }

    private val resultForRfidConnect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == CommonStatusCodes.SUCCESS ||
                it.resultCode == CommonStatusCodes.SUCCESS_CACHE
            ) {
                setupRfidReader()
            }
        }

    private fun askForResetToFactory(): AlertDialog {
        return AlertDialog.Builder(requireActivity())
            //set message, title, and icon
            .setTitle(getString(R.string.reset_to_factory))
            .setMessage(getString(R.string.you_want_to_reset_the_rfid_device_to_its_factory_settings))
            .setPositiveButton(
                getString(R.string.reset)
            ) { dialog, _ ->
                vh75?.resetToFactory()
                dialog.dismiss()
            }.setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }.create()
    }

    private fun setSymbology() {
        val resetSymbologyPref: Preference? = findPreference("restore_to_default")
        resetSymbologyPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val diaBox = askForResetSymbology()
            diaBox.show()
            true
        }
    }

    private fun askForResetSymbology(): AlertDialog {
        return AlertDialog.Builder(requireActivity())
            //set message, title, and icon
            .setTitle(getString(R.string.default_values))
            .setMessage(getString(R.string.do_you_want_to_restore_the_default_system_symbology_settings))
            .setPositiveButton(
                getString(R.string.ok)
            ) { dialog, _ ->
                resetSymbology()
                dialog.dismiss()
            }.setNegativeButton(
                R.string.cancel
            ) { dialog, _ -> dialog.dismiss() }.create()
    }

    private fun resetSymbology() {
        setDefaultSymbology()
        updateSymbologySwitchPreferences()
    }

    private fun setDefaultSymbology() {
        val allSymbology = SettingsRepository.getSymbology()
        for (s in allSymbology) {
            s.value = s.default
        }
    }

    private fun updateSymbologySwitchPreferences() {
        val preferenceScreen = preferenceScreen
        for (i in 0 until preferenceScreen.preferenceCount) {
            val preference = preferenceScreen.getPreference(i)
            if (preference is SwitchPreference) {
                val allSymbology = SettingsRepository.getSymbology()
                val pref = allSymbology.firstOrNull { it.key == preference.key } ?: continue
                val defaultValue = pref.default as Boolean
                preference.isChecked = defaultValue
            }
        }
    }

    // endregion RFID
}