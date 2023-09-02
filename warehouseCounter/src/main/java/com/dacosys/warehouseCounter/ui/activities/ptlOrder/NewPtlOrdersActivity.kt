package com.dacosys.warehouseCounter.ui.activities.ptlOrder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.functions.location.GetWarehouse
import com.dacosys.warehouseCounter.data.ktor.v2.impl.ApiRequest
import com.dacosys.warehouseCounter.databinding.NewPtlOrdersBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.activities.location.LocationSelectActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import org.parceler.Parcels
import kotlin.concurrent.thread

class NewPtlOrdersActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) JotterListener.onRequestPermissionsResult(
            this, requestCode, permissions, grantResults
        )
    }

    override fun scannerCompleted(scanCode: String) {
        if (isFinishing) return

        if (settingViewModel.showScannedCode) showSnackBar(scanCode, SnackBarType.INFO)
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

        warehouseArea = b.getParcelable(ARG_WAREHOUSE_AREA)
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
                warehouseArea =
                    Parcels.unwrap<WarehouseArea>(data.getParcelableExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA))
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
            showSnackBar(context.getString(R.string.invalid_url), SnackBarType.ERROR)
            return
        }

        thread {
            GetWarehouse(
                action = GetWarehouse.defaultAction,
                onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
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

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
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
        if (warehouseArea != null) data.putExtra(
            ARG_WAREHOUSE_AREA, Parcels.wrap<WarehouseArea>(warehouseArea)
        )

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

    override fun onReadCompleted(scanCode: String) {
        scannerCompleted(scanCode)
    }

    //endregion READERS Reception    

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingViewModel.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_rfid_connect -> {
                JotterListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
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

