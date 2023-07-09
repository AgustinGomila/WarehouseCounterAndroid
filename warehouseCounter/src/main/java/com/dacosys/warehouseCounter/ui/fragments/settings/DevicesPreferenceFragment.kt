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
import androidx.preference.*
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.collectorType.CollectorType
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.scanners.rfid.RfidType
import com.dacosys.warehouseCounter.scanners.vh75.Vh75Bt
import com.dacosys.warehouseCounter.settings.custom.CollectorTypePreference
import com.dacosys.warehouseCounter.settings.custom.DevicePreference
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.google.android.gms.common.api.CommonStatusCodes
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.thread

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
            if (settingViewModel.useBtRfid) {
                Rfid.setListener(this, RfidType.vh75)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            MakeText.makeText(v, getString(R.string.rfid_reader_not_initialized), SnackBarType.INFO)
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
        SettingsActivity.bindPreferenceSummaryToValue(this, settingRepository.collectorType)

        // PERMITE ACTUALIZAR EN PANTALLA EL ITEM SELECCIONADO EN EL SUMMARY DEL CONTROL
        val collectorTypeListPreference =
            findPreference<Preference>(settingRepository.collectorType.key) as CollectorTypePreference
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
            findPreference<Preference>(settingRepository.printerBtAddress.key) as DevicePreference
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
            findPreference<Preference>(settingRepository.portNetPrinter.key) as EditTextPreference
        portNetPrinterPref.summary = portNetPrinterPref.text

        val ipNetPrinterPref =
            findPreference<Preference>(settingRepository.ipNetPrinter.key) as EditTextPreference
        ipNetPrinterPref.summary = ipNetPrinterPref.text

        // ipNetPrinterPref.setOnBindEditTextListener {
        //     val filters = arrayOfNulls<InputFilter>(1)
        //     filters[0] = InputFilter { source, start, end, dest, dStart, dEnd ->
        //         filter(source, start, end, dest, dStart, dEnd)
        //     }
        //     it.filters = filters
        // }
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
            findPreference<Preference>(settingRepository.useBtPrinter.key) as SwitchPreference
        useBtPrinter = swPrefBtPrinter.isChecked

        val swPrefNetPrinter =
            findPreference<Preference>(settingRepository.useNetPrinter.key) as SwitchPreference
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
            findPreference<Preference>(settingRepository.printerPower.key) as EditTextPreference
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
            settingRepository.printerPower.value = newValue
            true
        }

        val maxSpeed = 10
        val printerSpeedPref =
            findPreference<Preference>(settingRepository.printerSpeed.key) as EditTextPreference
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
            settingRepository.printerSpeed.value = newValue
            true
        }
        //endregion //// POTENCIA Y VELOCIDAD

        //region //// CARACTER DE SALTO DE LÍNEA
        val swPrefCharLF =
            findPreference<Preference>("conf_printer_new_line_char_lf") as SwitchPreference
        val swPrefCharCR =
            findPreference<Preference>("conf_printer_new_line_char_cr") as SwitchPreference

        val lineSeparator = settingViewModel.lineSeparator
        if (lineSeparator == Char(10).toString()) swPrefCharLF.isChecked
        else if (lineSeparator == Char(13).toString()) swPrefCharCR.isChecked

        swPrefCharLF.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                settingViewModel.lineSeparator = Char(10).toString()
                swPrefCharCR.isChecked = false
            }
            true
        }

        swPrefCharCR.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                settingViewModel.lineSeparator = Char(13).toString()
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
            // returns boolean representing whether the
            // permission is granted or not
            if (!isGranted) {
                MakeText.makeText(
                    v,
                    WarehouseCounterApp.context.getString(R.string.app_dont_have_necessary_permissions),
                    SnackBarType.ERROR
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
            findPreference<Preference>(settingRepository.useBtRfid.key) as SwitchPreference
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
        if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
            (Rfid.rfidDevice as Vh75Bt).getBluetoothName()
        }
        rfidDeviceNamePreference!!.setOnPreferenceClickListener {
            if (Rfid.rfidDevice == null || (Rfid.rfidDevice as Vh75Bt).getState() != Vh75Bt.STATE_CONNECTED) {
                MakeText.makeText(v, getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR)
            }
            true
        }
        rfidDeviceNamePreference!!.setOnPreferenceChangeListener { _, newValue ->
            if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
                (Rfid.rfidDevice as Vh75Bt).setBluetoothName(newValue.toString())
            } else {
                MakeText.makeText(v, getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR)
            }
            true
        }
        //endregion //// BLUETOOTH NAME

        //region //// DEVICE LIST PREFERENCE
        val deviceListPreference =
            findPreference<Preference>(settingRepository.rfidBtAddress.key) as DevicePreference
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
            findPreference<Preference>(settingRepository.rfidReadPower.key) as SeekBarPreference
        rfidReadPower.setOnPreferenceChangeListener { _, newValue ->
            rfidReadPower.summary = "$newValue dB"
            true
        }
        rfidReadPower.summary = "${settingViewModel.rfidReadPower} dB"
        //endregion //// RFID POWER

        //region //// RESET TO FACTORY
        val resetButton = findPreference<Preference>("rfid_reset_to_factory") as Preference
        resetButton.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (Rfid.rfidDevice != null && (Rfid.rfidDevice as Vh75Bt).getState() == Vh75Bt.STATE_CONNECTED) {
                val diaBox = askForResetToFactory()
                diaBox.show()
            } else {
                MakeText.makeText(v, getString(R.string.there_is_no_rfid_device_connected), SnackBarType.ERROR)
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

        val bluetoothManager =
            WarehouseCounterApp.context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter = bluetoothManager.adapter
        if (mBluetoothAdapter == null) {
            MakeText.makeText(v, getString(R.string.there_are_no_bluetooth_devices), SnackBarType.INFO)
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
                MakeText.makeText(
                    v,
                    WarehouseCounterApp.context.getString(R.string.app_dont_have_necessary_permissions),
                    SnackBarType.ERROR
                )
            }
        }

    private fun getBluetoothNameFromAddress(address: Any?, summary: String): String {
        var s = summary

        if (address != null) {
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