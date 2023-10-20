package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.room.dao.client.ClientCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.databinding.NewCountActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.activities.client.ClientSelectActivity
import com.dacosys.warehouseCounter.ui.fragments.orderRequest.OrderRequestTypeSpinnerFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.utils.TextViewUtils.Companion.isActionDone
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import kotlin.concurrent.thread

class NewCountActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener,
    OrderRequestTypeSpinnerFragment.OnItemSelectedListener {
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
            LifecycleListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun scannerCompleted(scanCode: String) {
        if (isFinishing) return

        if (settingsVm.showScannedCode) showSnackBar(scanCode, SnackBarType.INFO)

        runOnUiThread {
            binding.descEditText.setText(scanCode)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private var client: Client? = null
    private var orderRequestType: OrderRequestType = OrderRequestType.stockAuditFromDevice
    private var tempTitle: String = ""
    private var tempDescription: String = ""

    private var orderRequestTypeSpinnerFragment: OrderRequestTypeSpinnerFragment? = null

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_DESCRIPTION, binding.descEditText.text.toString())
        savedInstanceState.putParcelable(ARG_CLIENT, client)
        savedInstanceState.putParcelable(ARG_ORDER_REQUEST_TYPE, orderRequestType)
    }

    private fun loadBundleValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        tempTitle = if (!t1.isNullOrEmpty()) t1
        else context.getString(R.string.setup_new_count)

        tempDescription = b.getString(ARG_DESCRIPTION) ?: ""
        client = b.parcelable(ARG_CLIENT)
        orderRequestType = b.parcelable(ARG_ORDER_REQUEST_TYPE) ?: OrderRequestType.stockAuditFromDevice
    }

    private fun loadExtraBundleValues(b: Bundle) {
        val t1 = b.getString(ARG_TITLE)
        tempTitle = if (!t1.isNullOrEmpty()) t1
        else context.getString(R.string.setup_new_count)
    }

    private lateinit var binding: NewCountActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = NewCountActivityBinding.inflate(layoutInflater)
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

        // Order request type spinner
        orderRequestTypeSpinnerFragment =
            OrderRequestTypeSpinnerFragment.Builder()
                .orderRequestType(OrderRequestType.prepareOrder)
                .allOrderRequestType(OrderRequestType.getAll())
                .callback(this)
                .build()

        supportFragmentManager.beginTransaction()
            .replace(R.id.orderRequestTypeSpinnerFragment, orderRequestTypeSpinnerFragment!!).commit()

        binding.topAppbar.title = tempTitle

        binding.continueButton.setOnClickListener { attemptSetupNewCount() }

        binding.clientTextView.setOnClickListener {
            val intent = Intent(this, ClientSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(ARG_CLIENT, client)
            intent.putExtra(ARG_TITLE, context.getString(R.string.select_client))
            resultForClientSelect.launch(intent)
        }

        binding.clientSearchImageView.setOnClickListener { binding.clientTextView.performClick() }
        binding.clientClearImageView.setOnClickListener {
            client = null
            setClientText()
        }

        binding.descEditText.clearFocus()
        binding.descEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                Screen.showKeyboard(this)
            } else {
                Screen.closeKeyboard(this)
            }
        }
        binding.descEditText.setOnKeyListener { _, _, event ->
            if (isActionDone(event)) {
                attemptSetupNewCount()
                true
            } else {
                false
            }
        }
        binding.descEditText.setOnEditorActionListener { _, actionId, event ->
            return@setOnEditorActionListener if (isActionDone(actionId, event)) {
                binding.continueButton.performClick()
                true
            } else {
                false
            }
        }

        binding.descEditText.isCursorVisible = true
        binding.descEditText.isFocusable = true
        binding.descEditText.isFocusableInTouchMode = true
        binding.descEditText.requestFocus()
    }

    private val resultForClientSelect = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                client = data.parcelable(ClientSelectActivity.ARG_CLIENT) ?: return@registerForActivityResult
                setClientText()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        }
    }

    private fun selectDefaultClient() {
        if (isFinishing) return

        ClientCoroutines.get {
            client = it.firstOrNull()
            setClientText()
        }
    }

    private fun setClientText() {
        runOnUiThread {
            if (client == null) {
                binding.clientTextView.typeface = Typeface.DEFAULT
                binding.clientTextView.text = context.getString(R.string.search_client)
            } else {
                binding.clientTextView.typeface = Typeface.DEFAULT_BOLD
                binding.clientTextView.text = client!!.name
            }
        }
    }

    private fun attemptSetupNewCount() {
        // Reset errors.
        binding.descEditText.error = null

        // Store values at the time of the attempt.
        val description = binding.descEditText.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid data.
        if (!TextUtils.isEmpty(description) && !isDescriptionValid(description)) {
            binding.descEditText.error = context.getString(R.string.invalid_description)
            focusView = binding.descEditText
            cancel = true
        }

        if (cancel) {
            // Hacer foco en el problema
            focusView?.requestFocus()
        } else {
            Screen.closeKeyboard(this)

            val data = Intent()
            data.putExtra(ARG_DESCRIPTION, description.trim())
            data.putExtra(ARG_ORDER_REQUEST_TYPE, orderRequestType)
            if (client != null) data.putExtra(ARG_CLIENT, client)

            setResult(RESULT_OK, data)
            finish()
        }
    }

    private fun isClientValid(client: Client): Boolean {
        return client.clientId > 0
    }

    private fun isDescriptionValid(description: String): Boolean {
        return description.isNotEmpty()
    }

    public override fun onStart() {
        super.onStart()

        thread {
            selectDefaultClient()

            if (tempDescription.isNotEmpty()) {
                binding.descEditText.setText(tempDescription)
            }
        }
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
        menuInflater.inflate(R.menu.menu_read_activity, menu)

        if (!settingsVm.useBtRfid) {
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
                LifecycleListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                LifecycleListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                LifecycleListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_DESCRIPTION = "description"
        const val ARG_CLIENT = "client"
        const val ARG_ORDER_REQUEST_TYPE = "orderRequestType"

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }

    override fun onItemSelected(orderRequestType: OrderRequestType?) {
        if (orderRequestType != null) this.orderRequestType = orderRequestType
    }
}
