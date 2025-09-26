package com.example.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import com.example.warehouseCounter.R
import com.example.warehouseCounter.WarehouseCounterApp.Companion.context
import com.example.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.example.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.example.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.example.warehouseCounter.data.ktor.v2.functions.location.GetWarehouse
import com.example.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.example.warehouseCounter.databinding.NewPtlOrdersBinding
import com.example.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.example.warehouseCounter.scanners.Scanner
import com.example.warehouseCounter.scanners.devices.nfc.Nfc
import com.example.warehouseCounter.scanners.devices.rfid.Rfid
import com.example.warehouseCounter.scanners.devices.vh75.Vh75Bt
import com.example.warehouseCounter.scanners.jotter.ScannerManager
import com.example.warehouseCounter.ui.activities.location.LocationSelectActivity
import com.example.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.example.warehouseCounter.ui.snackBar.SnackBarType
import com.example.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.example.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.example.warehouseCounter.ui.utils.Screen
import kotlin.concurrent.thread

class NewPtlOrdersActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            ScannerManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (isFinishing) return

        if (settingsVm.showScannedCode) showMessage(scanCode, SnackBarType.INFO)
    }

    private var warehouseArea: WarehouseArea? = null
    private var tempTitle: String = ""

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
    }

    private fun loadBundleValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        tempTitle = if (!t1.isNullOrEmpty()) t1
        else context.getString(R.string.setup_new_ptl)

        warehouseArea = b.parcelable(ARG_WAREHOUSE_AREA)
    }

    private fun loadExtraBundleValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        tempTitle = if (!t1.isNullOrEmpty()) t1
        else context.getString(R.string.setup_new_ptl)
    }

    private lateinit var binding: NewPtlOrdersBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = NewPtlOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad. EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) loadExtraBundleValues(extras)
        }

        binding.topAppbar.title = tempTitle

        binding.continueButton.setOnClickListener { attemptSetupNewCount() }

        binding.areaTextView.setOnClickListener {
            val intent = Intent(this, LocationSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA, warehouseArea)
            intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_RACK_VISIBLE, false)
            intent.putExtra(LocationSelectActivity.ARG_TITLE, context.getString(R.string.select_area))
            resultForAreaSelect.launch(intent)
        }

        binding.areaSearchImageView.setOnClickListener { binding.areaTextView.performClick() }
        binding.areaClearImageView.setOnClickListener {
            warehouseArea = null
            setAreaText()
        }
    }

    private val resultForAreaSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                warehouseArea = data.parcelable<WarehouseArea>(LocationSelectActivity.ARG_WAREHOUSE_AREA)
                    ?: return@registerForActivityResult
                setAreaText()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        }
    }

    private fun selectDefaultArea() {
        if (isFinishing) return

        if (!ApiRequest.validUrl()) {
            showMessage(context.getString(R.string.invalid_url), ERROR)
            return
        }

        thread {
            GetWarehouse(
                action = GetWarehouse.defaultAction,
                onEvent = { if (!SnackBarType.SUCCESS.equals(it.snackBarType)) showMessage(it.text, it.snackBarType) },
                onFinish = { if (it.any()) onGetWarehouses(it) }
            ).execute()
        }
    }

    private fun onGetWarehouses(it: ArrayList<Warehouse>) {
        val w = it.first()
        val wa = w.areas?.first()

        warehouseArea = wa
        setAreaText()
    }

    private fun showMessage(msg: String, type: Int) {
        if (isFinishing || isDestroyed) return
        if (ERROR.equals(type)) Log.e(javaClass.simpleName, msg)
        makeText(binding.root, msg, type)
    }

    private fun showMessage(msg: String, type: SnackBarType) {
        showMessage(msg, type.snackBarTypeId)
    }

    private fun setAreaText() {
        runOnUiThread {
            if (warehouseArea == null) {
                binding.areaTextView.typeface = Typeface.DEFAULT
                binding.areaTextView.text = context.getString(R.string.search_area_)
            } else {
                binding.areaTextView.typeface = Typeface.DEFAULT_BOLD
                binding.areaTextView.text = warehouseArea!!.description
            }
        }
    }

    private fun attemptSetupNewCount() {
        Screen.closeKeyboard(this)

        val data = Intent()
        if (warehouseArea != null) data.putExtra(ARG_WAREHOUSE_AREA, warehouseArea)

        setResult(RESULT_OK, data)
        finish()
    }

    private fun isAreaValid(area: WarehouseArea): Boolean {
        return area.id > 0
    }

    public override fun onStart() {
        super.onStart()

        thread { selectDefaultArea() }
    }

    // region READERS Reception

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Nfc.nfcHandleIntent(intent, this)
    }

    override fun onGetBluetoothName(name: String) {}

    override fun onWriteCompleted(isOk: Boolean) {}

    override fun onStateChanged(state: Int) {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        if (settingsVm.rfidShowConnectedMessage) {
            when (Rfid.vh75State) {
                Vh75Bt.STATE_CONNECTED -> showMessage(getString(R.string.rfid_connected), SnackBarType.SUCCESS)
                Vh75Bt.STATE_CONNECTING -> showMessage(
                    getString(R.string.searching_rfid_reader),
                    SnackBarType.RUNNING
                )

                else -> showMessage(getString(R.string.there_is_no_rfid_device_connected), SnackBarType.INFO)
            }
        }
    }

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    //endregion READERS Reception    

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingsVm.useRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                finish()
                return true
            }

            R.id.action_rfid_connect -> {
                ScannerManager.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                ScannerManager.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                ScannerManager.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}