package com.dacosys.warehouseCounter.ui.activities.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestContent
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.databinding.QtySelectorBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.round
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.scanners.nfc.Nfc
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.CounterHandler
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent

/**
 * Esta actividad simula el aspecto de un Dialog
 * para ingresar cantidades y
 * con la capacidad de capturar los eventos del escáner
 */
class QtySelectorActivity : AppCompatActivity(), CounterHandler.CounterListener,
    Scanner.ScannerListener, Rfid.RfidDeviceListener {

    private var ch: CounterHandler? = null

    private lateinit var orc: OrderRequestContent
    private var partial: Boolean = false

    private var multiplier: Long = 1
    private var currentValue: Double = 0.toDouble()
    private var minValue: Double = 0.toDouble()
    private var maxValue: Double = 999999.toDouble()

    private var allowClicks = true
    private var rejectNewInstances = false

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putParcelable(ARG_ORDER_REQUEST_CONTENT, orc)
        savedInstanceState.putBoolean(ARG_PARTIAL, partial)

        savedInstanceState.putDouble(ARG_CURRENT_VALUE, currentValue)
        savedInstanceState.putLong(ARG_MULTIPLIER, multiplier)
        savedInstanceState.putDouble(ARG_MIN_VALUE, minValue)
        savedInstanceState.putDouble(ARG_MAX_VALUE, maxValue)
    }

    private fun loadBundleValues(b: Bundle) {
        orc = b.parcelable(ARG_ORDER_REQUEST_CONTENT) ?: OrderRequestContent()
        partial = b.getBoolean(ARG_PARTIAL)

        currentValue = b.getDouble(ARG_CURRENT_VALUE)
        multiplier = b.getLong(ARG_MULTIPLIER)
        minValue = b.getDouble(ARG_MIN_VALUE)
        minValue = b.getDouble(ARG_MIN_VALUE)
    }

    private fun loadExtrasBundleValues(b: Bundle) {
        orc = b.parcelable(ARG_ORDER_REQUEST_CONTENT) ?: OrderRequestContent()
        partial = b.getBoolean(ARG_PARTIAL)

        currentValue = b.getDouble(ARG_INITIAL_VALUE)
        multiplier = b.getLong(ARG_MULTIPLIER)
        minValue = b.getDouble(ARG_MIN_VALUE)
        maxValue = b.getDouble(ARG_MAX_VALUE)
    }

    private lateinit var binding: QtySelectorBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = QtySelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        } else {
            val extras = intent.extras
            if (extras != null) loadExtrasBundleValues(extras)
        }

        refreshTextViews()

        binding.editDescriptionButton.setOnClickListener {
            itemDescriptionDialog(orc)
        }

        if (partial) {
            binding.typeTextView.setText(R.string.partial_count)
            minValue = -(orc.qtyCollected ?: 0.0)
        } else {
            binding.typeTextView.setText(R.string.total_count)
            currentValue = orc.qtyCollected ?: 0.0
            multiplier = 1 // Conteo total, multiplicar por 1
        }

        binding.multiplierTextView.text = String.format("%sX", multiplier)

        // Esta clase controla el comportamiento de los botones (+) y (-)
        ch = CounterHandler.Builder()
            .incrementalView(binding.moreButton)
            .decrementalView(binding.lessButton).minRange(minValue) // cant go any less than -50
            .maxRange(maxValue) // cant go any further than 50
            .isCycle(true) // 49,50,-50,-49 and so on
            .counterDelay(50) // speed of counter
            .startNumber(currentValue).counterStep(1)  // steps e.g. 0,2,4,6...
            .listener(this) // to listen to counter-results and show them in app
            .build()

        binding.okButton.setOnClickListener { selectQty() }

        binding.qtyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                // Filtro que devuelve un texto válido
                val validStr = getValidValue(
                    source = s.toString(),
                    maxIntegerPlaces = 7,
                    maxDecimalPlaces = settingsVm.decimalPlaces,
                    maxValue = maxValue,
                    decimalSeparator = settingsVm.decimalSeparator
                )

                // Si es NULL no hay que hacer cambios en el texto
                // porque está dentro de las reglas del filtro
                if (validStr != null && validStr != s.toString() && ch != null && ch!!.setValue(
                        validStr.toString().toDouble()
                    )
                ) {
                    s.clear()
                    s.insert(0, validStr)
                }
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int,
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int,
            ) {
            }
        })
        binding.qtyEditText.clearFocus()
        binding.qtyEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        binding.qtyEditText.setText(
            currentValue.toString(), TextView.BufferType.EDITABLE
        )
        binding.qtyEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        selectQty()
                    }
                }
            }
            false
        }
        // Cambia el modo del teclado en pantalla a tipo numérico
        // cuando este control lo necesita.
        binding.qtyEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !KeyboardVisibilityEvent.isKeyboardVisible(this)) {
                Screen.showKeyboard(this)
            }
        }

        binding.qtyEditText.isCursorVisible = true
        binding.qtyEditText.isFocusable = true
        binding.qtyEditText.isFocusableInTouchMode = true
        binding.qtyEditText.requestFocus()
    }

    private fun selectQty() {
        Screen.closeKeyboard(this)

        var qty = 0.toDouble()
        try {
            qty = binding.qtyEditText.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            e.printStackTrace() //prints error
            finish()
        }

        val resultData = Intent()
        resultData.putExtra(ARG_QTY, round(qty * multiplier, settingsVm.decimalPlaces))
        resultData.putExtra(ARG_ORDER_REQUEST_CONTENT, orc)
        setResult(RESULT_OK, resultData)
        finish()
    }

    private fun itemDescriptionDialog(orc: OrderRequestContent) {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, EnterCodeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.putExtra(EnterCodeActivity.ARG_TITLE, getString(R.string.enter_description))
        intent.putExtra(EnterCodeActivity.ARG_ORC, orc)
        resultForDescriptionSelect.launch(intent)
    }

    private val resultForDescriptionSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val description = data.getStringExtra(EnterCodeActivity.ARG_CODE) ?: ""
                    val orc = data.parcelable<OrderRequestContent>(EnterCodeActivity.ARG_ORC)
                    if (orc != null) {
                        setDescription(orc, description)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
                allowClicks = true
                JotterListener.lockScanner(this, false)
            }
        }

    private fun setDescription(orc: OrderRequestContent, description: String) {
        val itemId = orc.itemId ?: return

        updateItemDescriptionToDb(itemId, description) {
            if (it == null) {
                val res =
                    getString(R.string.an_error_occurred_while_updating_the_description_of_the_item_in_the_database)
                showSnackBar(res, ERROR)
                android.util.Log.e(this::class.java.simpleName, res)
                return@updateItemDescriptionToDb
            }

            orc.apply { itemDescription = it.description }
            this.orc = orc

            refreshTextViews()
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun updateItemDescriptionToDb(
        itemId: Long,
        description: String,
        onFinish: (Item?) -> Unit = {},
    ) {
        ItemCoroutines.updateDescription(itemId, description) {
            ItemCoroutines.getById(itemId) {
                if (it != null) onFinish(it)
                else onFinish(null)
            }
        }
    }

    private fun refreshTextViews() {
        settingsVm.decimalPlaces = 0

        binding.itemDescriptionTextView.text = String.format(
            "%s: %s", getString(R.string.item), orc.itemDescription
        )
        binding.typeTextView.text = getString(R.string.unit)
        binding.codeTextView.text = String.format(
            "%s: %s", getString(R.string.item_code), orc.ean
        )

        binding.totalTextView.text = String.format(
            "%s: %s",
            getString(R.string.total_qty),
            Statics.roundToString(orc.qtyCollected!!, settingsVm.decimalPlaces)
        )
    }

    public override fun onStart() {
        super.onStart()
        rejectNewInstances = false
    }

    override fun onIncrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

    override fun onDecrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
    }

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
        if (settingsVm.showScannedCode) showSnackBar(scanCode, INFO)

        JotterListener.lockScanner(this, true)
        if (equals(scanCode, orc.ean)) {
            try {
                val currentQty: Double
                try {
                    currentQty = java.lang.Double.parseDouble(binding.qtyEditText.text.toString())
                } catch (e: NumberFormatException) {
                    showSnackBar(e.message.toString(), ERROR)
                    return
                }

                runOnUiThread {
                    val r = currentQty + multiplier
                    binding.qtyEditText.setText(r.toString(), TextView.BufferType.EDITABLE)
                }
            } catch (ex: Exception) {
                showSnackBar(ex.message.toString(), ERROR)
            } finally {
                JotterListener.lockScanner(this, false)
            }
        }
    }

    // region Filtro para aceptar solo números entre ciertos parámetros

    /**
     * Devuelve una cadena de texto formateada que se ajusta a los parámetros.
     * Devuelve una cadena vacía en caso de Exception.
     * Devuelve null si no es necesario cambiar la cadena ingresada porque ya se ajusta a los parámetros
     *          o porque es igual que la cadena original.
     */
    private fun getValidValue(
        source: String,
        maxIntegerPlaces: Int,
        maxDecimalPlaces: Int,
        maxValue: Double,
        decimalSeparator: Char,
    ): CharSequence? {
        if (source.isEmpty()) {
            return null
        } else {
            // Regex para eliminar caracteres no permitidos.
            var validText = source.replace("[^0-9?!\\" + decimalSeparator + "]".toRegex(), "")

            // Probamos convertir el valor, si no se puede
            // se devuelve una cadena vacía
            val numericValue: Double
            try {
                numericValue = java.lang.Double.parseDouble(validText)
            } catch (e: NumberFormatException) {
                return ""
            }

            // Si el valor numérico es mayor al valor máximo reemplazar el
            // texto válido por el valor máximo
            validText = if (numericValue > maxValue) {
                maxValue.toString()
            } else {
                validText
            }

            // Obtener la parte entera y decimal del valor en forma de texto
            var decimalPart = ""
            val integerPart: String
            if (validText.contains(decimalSeparator)) {
                decimalPart = validText.substring(
                    validText.indexOf(decimalSeparator) + 1, validText.length
                )
                integerPart = validText.substring(0, validText.indexOf(decimalSeparator))
            } else {
                integerPart = validText
            }

            // Si la parte entera es más larga que el máximo de dígitos permitidos
            // retorna un carácter vacío.
            if (integerPart.length > maxIntegerPlaces) {
                return ""
            }

            // Si la cantidad de espacios decimales permitidos es cero devolver la parte entera
            // si no, concatenar la parte entera con el separador de decimales y
            // la cantidad permitida de decimales.
            val result = if (maxDecimalPlaces == 0) {
                integerPart
            } else integerPart + decimalSeparator + decimalPart.substring(
                0,
                if (decimalPart.length > maxDecimalPlaces) maxDecimalPlaces else decimalPart.length
            )

            // Devolver solo si son valores positivos diferentes a los de originales.
            // NULL si no hay que hacer cambios sobre el texto original.
            return if (result != source) {
                result
            } else null
        }
    }

    // endregion

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

    companion object {
        const val ARG_QTY = "qty"
        const val ARG_ORDER_REQUEST_CONTENT = "orderRequestContent"
        const val ARG_PARTIAL = "partial"
        const val ARG_INITIAL_VALUE = "initialValue"
        const val ARG_MULTIPLIER = "multiplier"
        const val ARG_MIN_VALUE = "minValue"
        const val ARG_MAX_VALUE = "maxValue"
        const val ARG_CURRENT_VALUE = "currentValue"

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
