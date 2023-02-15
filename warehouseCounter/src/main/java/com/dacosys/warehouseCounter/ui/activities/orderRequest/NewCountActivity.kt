package com.dacosys.warehouseCounter.ui.activities.orderRequest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.dataBase.client.ClientDbHelper
import com.dacosys.warehouseCounter.databinding.NewCountActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.model.client.Client
import com.dacosys.warehouseCounter.model.errorLog.ErrorLog
import com.dacosys.warehouseCounter.model.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.activities.client.ClientSelectActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.parceler.Parcels
import kotlin.concurrent.thread

class NewCountActivity : AppCompatActivity(), Scanner.ScannerListener, Rfid.RfidDeviceListener {
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

        if (settingViewModel().showScannedCode) makeText(binding.root, scanCode, SnackBarType.INFO)

        runOnUiThread {
            binding.countCodeEditText.setText(scanCode)
        }
    }

    private var client: Client? = null

    private var tempTitle: String = ""
    private var tempDescription: String = ""

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("description", binding.countCodeEditText.text.toString())
        savedInstanceState.putParcelable("client", client)
    }

    private fun loadBundleValues(b: Bundle) {
        val t1 = b.getString("title")
        tempTitle = if (t1 != null && t1.isNotEmpty()) t1
        else context().getString(R.string.setup_new_count)

        tempDescription = b.getString("description") ?: ""
        client = b.getParcelable("client")
    }

    private fun loadExtraBundleValues(b: Bundle) {
        val t1 = b.getString("title")
        tempTitle = if (t1 != null && t1.isNotEmpty()) t1
        else context().getString(R.string.setup_new_count)
    }

    private lateinit var binding: NewCountActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = NewCountActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados
            loadBundleValues(savedInstanceState)
        } else {
            // Inicializar la actividad. EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) loadExtraBundleValues(extras)
        }

        title = tempTitle

        binding.continueButton.setOnClickListener { attemptSetupNewCount() }

        binding.clientTextView.setOnClickListener {
            val intent = Intent(this, ClientSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("client", client)
            intent.putExtra("title", context().getString(R.string.select_client))
            resultForClientSelect.launch(intent)
        }

        binding.clientSearchImageView.setOnClickListener { binding.clientTextView.performClick() }
        binding.clientClearImageView.setOnClickListener {
            client = null
            setClientText()
        }

        binding.countCodeEditText.clearFocus()
        binding.countCodeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                Statics.showKeyboard(this)
            } else {
                Statics.closeKeyboard(this)
            }
        }
        binding.countCodeEditText.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                attemptSetupNewCount()
                true
            } else {
                false
            }
        }
        binding.countCodeEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    binding.continueButton.performClick()
                    true
                }
                else -> false
            }
        }

        binding.countCodeEditText.isCursorVisible = true
        binding.countCodeEditText.isFocusable = true
        binding.countCodeEditText.isFocusableInTouchMode = true
        binding.countCodeEditText.requestFocus()
    }

    private val resultForClientSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    client = data.getParcelableExtra("client") ?: return@registerForActivityResult
                    setClientText()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            }
        }

    private fun selectDefaultClient() {
        if (isFinishing) return

        client = ClientDbHelper().select().firstOrNull()
        setClientText()
    }

    private fun setClientText() {
        runOnUiThread {
            if (client == null) {
                binding.clientTextView.typeface = Typeface.DEFAULT
                binding.clientTextView.text = context().getString(R.string.search_client)
            } else {
                binding.clientTextView.typeface = Typeface.DEFAULT_BOLD
                binding.clientTextView.text = client!!.name
            }
        }
    }

    private fun attemptSetupNewCount() {
        // Reset errors.
        //clientSpinnerFragment.setError(null);
        binding.countCodeEditText.error = null

        // Store values at the time of the setupnewcount attempt.
        val description = binding.countCodeEditText.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid data.
        if (!TextUtils.isEmpty(description) && !isDescriptionValid(description)) {
            binding.countCodeEditText.error = context().getString(R.string.invalid_description)
            focusView = binding.countCodeEditText
            cancel = true
        }

        /*
        if (client == null || !isClientValid(client!!)) {
            //clientSpinnerFragment.setError(getString(R.string.error_field_required));
            focusView = clientAutoCompleteTextView!!
            Toast.makeText(binding.root,context(), R.string.you_must_select_a_client, Toast.LENGTH_SHORT).show()
            cancel = true
        }
        */

        if (cancel) {
            // Hacer foco en el problema
            focusView?.requestFocus()
        } else {
            Statics.closeKeyboard(this)

            val data = Intent()
            data.putExtra("description", description.trim())
            if (client != null) data.putExtra("client", Parcels.wrap<Client>(client))
            data.putExtra("orderRequestType", Parcels.wrap(OrderRequestType.stockAuditFromDevice))

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
                binding.countCodeEditText.setText(tempDescription)
            }
        }
    }

    // region READERS Reception

    override fun onNewIntent(intent: Intent) {
        /*
          This method gets called, when a new Intent gets associated with the current activity instance.
          Instead of creating a new activity, onNewIntent will be called. For more information have a look
          at the documentation.

          In our case this method gets called, when the user attaches a className to the device.
         */
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

        if (!settingViewModel().useBtRfid) {
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
        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}

