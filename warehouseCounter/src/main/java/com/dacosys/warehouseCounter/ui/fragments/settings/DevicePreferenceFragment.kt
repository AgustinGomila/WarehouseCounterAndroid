package com.dacosys.warehouseCounter.ui.fragments.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.settings.SettingsRepository
import com.dacosys.warehouseCounter.data.settings.SettingsViewModel
import com.dacosys.warehouseCounter.data.settings.custom.CollectorTypePreference
import com.dacosys.warehouseCounter.data.settings.custom.DevicePreference
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.collectorType.CollectorType
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.rfid.RfidType
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.google.android.gms.common.api.CommonStatusCodes
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.thread

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
            "printer" -> {
                setPrinterPref()
            }

            "rfid" -> {
                setRfidPref()
            }

            "symbology" -> {}

            else -> {
                setDevicePref()
            }
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

    override fun onGetBluetoothName(name: String) {
        rfidDeviceNamePreference?.summary = name
    }

    override fun onReadCompleted(scanCode: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    private fun setupRfidReader() {
        try {
            if (vm.useBtRfid) {
                Rfid.setListener(this, RfidType.vh75)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            showSnackBar(getString(R.string.rfid_reader_not_initialized), SnackBarType.INFO)
            ErrorLog.writeLog(activity, this::class.java.simpleName, ex)
        }
    }

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
        val collectorTypeListPreference =
            findPreference<Preference>(sp.collectorType.key) as CollectorTypePreference
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

    fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned?, dStart: Int, dEnd: Int): CharSequence? {
        if (source == "") return null // Para el backspace
        val builder = java.lang.StringBuilder(dest.toString())
        builder.replace(dStart, dEnd, source.subSequence(start, end).toString())
        val matcher: Matcher = ipv4Pattern.matcher(builder)
        return if (!matcher.matches()) "" else null
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
        val portNetPrinterPref =
            findPreference<Preference>(sp.portNetPrinter.key) as EditTextPreference
        portNetPrinterPref.summary = portNetPrinterPref.text

        val ipNetPrinterPref =
            findPreference<Preference>(sp.ipNetPrinter.key) as EditTextPreference
        ipNetPrinterPref.summary = ipNetPrinterPref.text

        // TODO: Crear un filtro para IPs
        // ipNetPrinterPref.setOnBindEditTextListener {
        //     val filters = arrayOfNulls<InputFilter>(1)
        //     filters[0] = InputFilter { source, start, end, dest, dStart, dEnd ->
        //         filter(source, start, end, dest, dStart, dEnd)
        //     }
        //     it.filters = filters
        // }
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
                } catch (ignore: NumberFormatException) {
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
                } catch (ignore: NumberFormatException) {
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

    private val resultForRfidConnect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it?.resultCode == CommonStatusCodes.SUCCESS || it?.resultCode == CommonStatusCodes.SUCCESS_CACHE) {
                setupRfidReader()
            }
        }

    private val resultForRfidPermissionConnect =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // returns boolean representing whether the
            // permission is granted or not
            if (!isGranted) {
                showSnackBar(
                    WarehouseCounterApp.context.getString(R.string.app_dont_have_necessary_permissions),
                    SnackBarType.ERROR
                )
            } else {
                setupRfidReader()
            }
        }

    private fun getRfidSummary(): String {
        var rfidSummary =
            if (vm.useBtRfid) getString(R.string.enabled)
            else getString(R.string.disabled)

        if (vm.rfidBtAddress.isNotEmpty())
            rfidSummary = "$rfidSummary: ${getBluetoothNameFromAddress()}"

        return rfidSummary
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(currentView, text, snackBarType)
    }

    // Esta preferencia se utiliza al recibir el nombre del dispositivo
    // RFID seleccionado para modificar el texto de su sumario.
    private var rfidDeviceNamePreference: EditTextPreference? = null

    private fun setRfidPref() {
        //region //// USE RFID
        val swPrefBtRfid = findPreference<Preference>(sp.useBtRfid.key) as SwitchPreference

        swPrefBtRfid.setOnPreferenceChangeListener { _, newValue ->
            vm.useBtRfid = newValue != null && newValue == true
            thread { connectToRfidDevice() }
            true
        }
        //endregion //// USE RFID

        //region //// BLUETOOTH NAME
        rfidDeviceNamePreference =
            findPreference<Preference>("rfid_bluetooth_name") as EditTextPreference
        if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
            (Rfid.rfidDevice as Vh75Bt).getBluetoothName()
        }
        rfidDeviceNamePreference!!.setOnPreferenceClickListener {
            if (Rfid.rfidDevice == null || (Rfid.rfidDevice as Vh75Bt).getState() != Vh75Bt.STATE_CONNECTED) {
                showSnackBar(
                    getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR
                )
            }
            true
        }
        rfidDeviceNamePreference!!.setOnPreferenceChangeListener { _, newValue ->
            if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
                (Rfid.rfidDevice as Vh75Bt).setBluetoothName(newValue.toString())
            } else {
                showSnackBar(
                    getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR
                )
            }
            true
        }
        //endregion //// BLUETOOTH NAME

        //region //// DEVICE LIST PREFERENCE
        val deviceListPreference =
            findPreference<Preference>(sp.rfidBtAddress.key) as DevicePreference
        if (deviceListPreference.value == null) {
            // to ensure we don't selectByItemId a null value
            // set first value by default
            deviceListPreference.setValueIndex(0)
        }
        deviceListPreference.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                vm.rfidBtAddress = newValue.toString()
                preference.summary = getBluetoothNameFromAddress()

                // De este modo se actualiza el Summary del PreferenceScreen padre
                //(preferenceScreen.rootAdapter as BaseAdapter).notifyDataSetChanged()
                true
            }
        deviceListPreference.summary = getBluetoothNameFromAddress()
        //endregion //// DEVICE LIST PREFERENCE

        //region //// RFID POWER
        val rfidReadPower =
            findPreference<Preference>(sp.rfidReadPower.key) as SeekBarPreference
        rfidReadPower.setOnPreferenceChangeListener { _, newValue ->
            rfidReadPower.summary = "$newValue dB"
            true
        }
        rfidReadPower.summary = "${vm.rfidReadPower} dB"
        //endregion //// RFID POWER

        //region //// RESET TO FACTORY
        val resetButton = findPreference<Preference>("rfid_reset_to_factory") as Preference
        resetButton.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
                val diaBox = askForResetToFactory()
                diaBox.show()
            } else {
                showSnackBar(
                    getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR
                )
            }
            true
        }
        //endregion //// RESET TO FACTORY

        thread { connectToRfidDevice() }
    }

    private fun connectToRfidDevice() {
        if (!vm.useBtRfid) return

        val bluetoothManager =
            WarehouseCounterApp.context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            showSnackBar(getString(R.string.there_are_no_bluetooth_devices), SnackBarType.INFO)
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
            // returns boolean representing whether the
            // permission is granted or not
            if (!isGranted) {
                showSnackBar(
                    WarehouseCounterApp.context.getString(R.string.app_dont_have_necessary_permissions),
                    SnackBarType.ERROR
                )
            }
        }

    private fun getBluetoothNameFromAddress(): String {
        var s = getString(R.string.there_is_no_selected_rfid_scanner)

        if (vm.rfidBtAddress.isNotEmpty()) {
            val bluetoothManager =
                WarehouseCounterApp.context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
            val mBluetoothAdapter = bluetoothManager.adapter

            if (ActivityCompat.checkSelfPermission(
                    WarehouseCounterApp.context, Manifest.permission.BLUETOOTH_CONNECT
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
                    if (mDevice.address == vm.rfidBtAddress) {
                        s = mDevice.name.toString()
                        break
                    }
                }
            }
        }

        return s
    }
}
