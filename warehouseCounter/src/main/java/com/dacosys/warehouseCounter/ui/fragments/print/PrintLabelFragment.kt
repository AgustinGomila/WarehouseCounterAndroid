package com.dacosys.warehouseCounter.ui.fragments.print

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v1.dto.ptlOrder.Label
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.Barcode
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.ktor.v2.functions.template.ViewBarcodeLabelTemplate
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.dao.itemCategory.ItemCategoryCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.databinding.PrintLabelFragmentBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.misc.UTCDataTime
import com.dacosys.warehouseCounter.printer.Printer
import com.dacosys.warehouseCounter.ui.activities.barcodeLabel.TemplateSelectActivity
import com.dacosys.warehouseCounter.ui.activities.main.SettingsActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.serializable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.CounterHandler
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PrintLabelFragment.FragmentListener] interface
 * to handle interaction events.
 */
class PrintLabelFragment private constructor(builder: Builder) : Fragment(), CounterHandler.CounterListener {

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    var template: BarcodeLabelTemplate? = null

    private var rejectNewInstances = false

    // Configuración guardada de los controles que se ven o no se ven
    private var printer: String = ""
    private var templateId: Long? = null
    private var templateTypeIdList: ArrayList<Long> = arrayListOf()
    private var qty: Int = 1

    private var fragmentListener: FragmentListener? = null

    private var ch: CounterHandler? = null

    // Container Activity must implement this interface
    interface FragmentListener {
        fun onFilterChanged(
            printer: String,
            template: BarcodeLabelTemplate?,
            qty: Int?,
        )

        fun onPrintRequested(
            printer: String,
            qty: Int,
        )

        fun onQtyTextViewFocusChanged(
            hasFocus: Boolean,
        )
    }

    override fun onStart() {
        super.onStart()
        if (requireActivity() is FragmentListener) {
            fragmentListener = activity as FragmentListener
        }
    }

    private fun sendMessage() {
        fragmentListener?.onFilterChanged(printer = printer, template = template, qty = qty)
    }

    private fun saveSharedPreferences() {
        settingsVm.printerQty = qty
    }

    override fun onDetach() {
        super.onDetach()
        fragmentListener = null
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        saveSharedPreferences()
        this.fragmentListener = null
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        saveBundleValues(savedInstanceState)
    }

    private fun loadPrinterPreferences() {
        qty = settingsVm.printerQty

        // Impresora guardada en preferencias
        val useBtPrinter = settingsVm.useBtPrinter
        val useNetPrinter = settingsVm.useNetPrinter

        val pBt = settingsVm.printerBtAddress
        val pIp = settingsVm.ipNetPrinter
        val port = settingsVm.portNetPrinter

        printer = when {
            useBtPrinter -> pBt
            useNetPrinter -> "$pIp (${port})"
            else -> ""
        }

        // Si tenemos una plantilla guardada la seleccionamos
        if (template != null) {
            setTemplateText()
        } else {
            val bltId = templateId ?: 0L
            // No tenemos la plantilla cargada, pero tenemos el ID de la plantilla
            if (bltId > 0) {
                ViewBarcodeLabelTemplate(
                    id = bltId,
                    action = arrayListOf(),
                    onEvent = { if (it.snackBarType != SnackBarType.SUCCESS) showSnackBar(it.text, it.snackBarType) },
                    onFinish = {
                        template = it
                        setTemplateText()
                    }
                ).execute()
            }
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private fun loadBundleValues(b: Bundle) {
        printer = b.getString(ARG_PRINTER) ?: ""
        template = b.parcelable(ARG_TEMPLATE)
        val temp =
            if (b.containsKey(ARG_TEMPLATE_TYPE_ID_LIST)) b.serializable<ArrayList<Long>>(ARG_TEMPLATE_TYPE_ID_LIST) as ArrayList<*>
            else ArrayList<Long>()
        if (temp.firstOrNull() is Long) {
            @Suppress("UNCHECKED_CAST")
            templateTypeIdList = temp as ArrayList<Long>
        }
    }

    private fun saveBundleValues(b: Bundle) {
        b.putString(ARG_PRINTER, printer)
        b.putParcelable(ARG_TEMPLATE, template)
        b.putSerializable(ARG_TEMPLATE_TYPE_ID_LIST, templateTypeIdList)
    }

    private var _binding: PrintLabelFragmentBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = PrintLabelFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        }

        binding.printerTextView.setOnClickListener { configApp() }

        binding.templateTextView.setOnClickListener {
            if (rejectNewInstances) return@setOnClickListener
            rejectNewInstances = true

            val intent = Intent(requireContext(), TemplateSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(TemplateSelectActivity.ARG_TEMPLATE, template)
            intent.putExtra(TemplateSelectActivity.ARG_TEMPLATE_TYPE_ID_LIST, templateTypeIdList)
            intent.putExtra(TemplateSelectActivity.ARG_TITLE, getString(R.string.select_label_template))
            resultForTemplateSelect.launch(intent)
        }

        // Esta clase controla el comportamiento de los botones (+) y (-)
        ch = CounterHandler.Builder()
            .incrementalView(binding.moreButton)
            .decrementalView(binding.lessButton).minRange(1.0) // cant go any less than -50
            .maxRange(100.0) // cant go any further than 50
            .isCycle(true) // 49,50,-50,-49 and so on
            .counterDelay(50) // speed of counter
            .startNumber(qty.toDouble()).counterStep(1)  // steps e.g. 0,2,4,6...
            .listener(this) // to listen to counter-results and show them in app
            .build()

        binding.qtyEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                // Filtro que devuelve un texto válido
                val validStr = getValidValue(source = s.toString())

                // Si es NULL no hay que hacer cambios en el texto
                // porque está dentro de las reglas del filtro
                if (validStr != null && validStr != s.toString()) {
                    s.clear()
                    s.insert(0, validStr)

                    qty = validStr.toString().toInt()
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
        binding.qtyEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        binding.printButton.performClick()
                    }
                }
            }
            false
        }
        // Cambia el modo del teclado en pantalla a tipo numérico
        // cuando este control lo necesita.
        binding.qtyEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        binding.qtyEditText.setOnFocusChangeListener { _, hasFocus ->
            fragmentListener?.onQtyTextViewFocusChanged(hasFocus)
        }

        binding.printerSearchImageView.setOnClickListener { binding.printerTextView.performClick() }
        binding.printerClearImageView.setOnClickListener {
            printer = ""
            setPrinterText()
            sendMessage()
        }

        binding.templateSearchImageView.setOnClickListener { binding.templateTextView.performClick() }
        binding.templateClearImageView.setOnClickListener {
            template = null
            setTemplateText()
            sendMessage()
        }

        binding.printButton.setOnClickListener {
            if (printer.isEmpty()) {
                showSnackBar(getString(R.string.you_must_select_a_printer), SnackBarType.ERROR)
                return@setOnClickListener
            }
            if (qty <= 0) {
                showSnackBar(getString(R.string.you_must_select_the_amount_of_labels_to_print), SnackBarType.ERROR)
                return@setOnClickListener
            }

            requestPrint()
        }

        return view
    }

    private val resultForTemplateSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    template = data.parcelable(TemplateSelectActivity.ARG_TEMPLATE)
                    setTemplateText()
                    sendMessage()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                rejectNewInstances = false
            }
        }

    private fun setTemplateText() {
        if (_binding == null) return

        activity?.runOnUiThread {
            if (template == null) {
                binding.templateTextView.typeface = Typeface.DEFAULT
                binding.templateTextView.text = getString(R.string.search_label_template_)
            } else {
                binding.templateTextView.typeface = Typeface.DEFAULT_BOLD
                binding.templateTextView.text = template!!.description
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadPrinterPreferences()

        refreshViews()
    }

    private fun configApp() {
        val realPass = settingsVm.confPassword
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

        var alertDialog: AlertDialog? = null
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.enter_password))

        val inputLayout = TextInputLayout(requireContext())
        inputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE

        val input = TextInputEditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (alertDialog != null) {
                            alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                        }
                    }
                }
            }
            false
        }

        inputLayout.addView(input)
        builder.setView(inputLayout)
        builder.setPositiveButton(R.string.accept) { _, _ ->
            attemptEnterConfig(input.text.toString())
        }
        builder.setNegativeButton(R.string.cancel, null)
        alertDialog = builder.create()

        alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        alertDialog.show()
        input.requestFocus()
    }

    private fun attemptEnterConfig(password: String) {
        Screen.closeKeyboard(requireActivity())

        val realPass = settingsVm.confPassword
        if (password == realPass) {
            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(requireContext(), SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForPrinterConfig.launch(intent)
            }
        } else {
            showSnackBar(getString(R.string.invalid_password), SnackBarType.ERROR)
        }
    }

    private val resultForPrinterConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            loadPrinterPreferences()
            refreshViews()
            rejectNewInstances = false
        }

    private fun requestPrint() {
        fragmentListener?.onPrintRequested(printer = printer, qty = qty)
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun refreshViews() {
        activity?.runOnUiThread {
            binding.qtyEditText.setText(qty.toString(), TextView.BufferType.EDITABLE)
            setPrinterText()
        }
    }

    fun printBarcodes(labelArray: ArrayList<Barcode>, onFinish: (Boolean) -> Unit) {
        if (labelArray.isEmpty()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_label), SnackBarType.ERROR)
            onFinish(false)
            return
        }

        val sendThis = labelArray.joinToString(lineSeparator) { it.body }

        sendToPrinter(sendThis, onFinish)
    }

    fun printPtlLabels(labelArray: ArrayList<Label>, onFinish: (Boolean) -> Unit) {
        if (labelArray.isEmpty()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_label), SnackBarType.ERROR)
            onFinish(false)
            return
        }

        val sendThis = labelArray.joinToString(lineSeparator) { it.body }

        sendToPrinter(sendThis, onFinish)
    }

    fun printItemById(itemIdArray: ArrayList<Long>, onFinish: (Boolean) -> Unit) {
        if (itemIdArray.isEmpty()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_item), SnackBarType.ERROR)
            onFinish(false)
            return
        }

        val items: ArrayList<Item> = ArrayList()
        var isDone = false
        for ((index, id) in itemIdArray.withIndex()) {
            ItemCoroutines.getById(id) {
                if (it != null) items.add(it)
                isDone = index == itemIdArray.lastIndex
            }
        }

        var startTime = System.currentTimeMillis()
        while (!isDone) {
            if (System.currentTimeMillis() - startTime == settingsVm.connectionTimeout.toLong())
                isDone = true
        }

        if (items.isEmpty()) {
            showSnackBar(getString(R.string.you_must_select_at_least_one_item), SnackBarType.ERROR)
            onFinish(false)
            return
        }

        var sendThis = ""
        isDone = false
        for ((index, item) in items.withIndex()) {
            getLabel(item) {
                sendThis += it
                isDone = index == items.lastIndex
            }
        }

        startTime = System.currentTimeMillis()
        while (!isDone) {
            if (System.currentTimeMillis() - startTime == settingsVm.connectionTimeout.toLong())
                isDone = true
        }

        sendToPrinter(sendThis, onFinish)
    }

    private fun sendToPrinter(sendThis: String, onFinish: (Boolean) -> Unit) {
        Printer(
            activity = requireActivity(),
            onEvent = { showSnackBar(it.text, it.snackBarType) }
        ).printThis(
            printThis = sendThis,
            qty = qty,
            onFinish = onFinish
        )
    }

    private fun getLabel(item: Item, onFinished: (String) -> Unit = {}) {
        val ean: String = item.ean
        var itemCategoryStr: String
        var description: String
        var price: String

        ItemCategoryCoroutines.getById(item.itemCategoryId) {
            itemCategoryStr = ""
            if (it != null) {
                if (it.parentStr.isNotEmpty()) {
                    "${it.parentStr} - ${it.description}"
                } else {
                    it.description
                }
            }

            description = item.description.uppercase(Locale.getDefault())
            price = Statics.roundToString(item.price ?: 0f, 2)

            // Trim contents
            if (description.length > 18) {
                description = description.substring(0, 21)
            }

            if (itemCategoryStr.length > 35) {
                itemCategoryStr = itemCategoryStr.substring(0, 60)
            }

            if (price.length > 35) {
                price = price.substring(0, 60)
            }

            val template = getString(R.string.wc_barcode_label_default)
            onFinished(
                String.format(
                    template,
                    ean,
                    normalizeStrings(description),
                    normalizeStrings(itemCategoryStr),
                    normalizeStrings(price),
                    UTCDataTime.getUTCDateTimeAsString()
                )
            )
        }
    }

    private fun normalizeStrings(name: String): String {
        return name.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o')
            .replace('ú', 'u').replace('ñ', 'n').replace('Á', 'A').replace('É', 'E')
            .replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U').replace('Ñ', 'N')
    }

    private fun setPrinterText() {
        if (_binding == null) return

        activity?.runOnUiThread {
            if (printer.trim().isEmpty()) {
                binding.printerTextView.typeface = Typeface.DEFAULT
                binding.printerTextView.text = getString(R.string.select_printer_)
            } else {
                binding.printerTextView.typeface = Typeface.DEFAULT_BOLD
                binding.printerTextView.text = printer.trim()
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
        maxIntegerPlaces: Int = 3,
        maxDecimalPlaces: Int = 0,
        maxValue: Double = 100.0,
        decimalSeparator: Char = '.',
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
                decimalPart =
                    validText.substring(validText.indexOf(decimalSeparator) + 1, validText.length)
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

    override fun onIncrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
        qty = number.toInt()
        sendMessage()
    }

    override fun onDecrement(view: View?, number: Double) {
        binding.qtyEditText.setText(number.toString())
        qty = number.toInt()
        sendMessage()
    }

    init {
        templateId = builder.templateId
        templateTypeIdList = builder.templateTypeIdList
        qty = builder.qty
    }

    class Builder {
        fun build(): PrintLabelFragment {
            return PrintLabelFragment(this)
        }

        internal var templateId: Long? = null
        internal var templateTypeIdList: ArrayList<Long> = arrayListOf()
        internal var qty: Int = 1

        @Suppress("unused")
        fun setQty(qty: Int): Builder {
            this.qty = qty
            return this
        }

        @Suppress("unused")
        fun setTemplateTypeIdList(list: ArrayList<Long>): Builder {
            this.templateTypeIdList = list
            return this
        }

        @Suppress("unused")
        fun setTemplateId(templateId: Long): Builder {
            this.templateId = templateId
            return this
        }
    }

    companion object {
        private const val ARG_PRINTER = "printer"
        private const val ARG_TEMPLATE_TYPE_ID_LIST = "templateTypeIdList"
        private const val ARG_TEMPLATE = "template"
    }
}
