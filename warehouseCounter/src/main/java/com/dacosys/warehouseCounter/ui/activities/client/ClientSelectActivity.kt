package com.dacosys.warehouseCounter.ui.activities.client

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.room.dao.client.ClientCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.databinding.CodeSelectActivityBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.adapter.client.ClientAdapter
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ContractsAutoCompleteTextView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import kotlin.concurrent.thread

class ClientSelectActivity : AppCompatActivity(),
    ContractsAutoCompleteTextView.OnContractsAvailability, KeyboardVisibilityEventListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        Screen.closeKeyboard(this)
        binding.autoCompleteTextView.setOnContractsAvailability(null)
        binding.autoCompleteTextView.setAdapter(null)
    }

    private var client: Client? = null

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelable(ARG_CLIENT, client)
    }

    private lateinit var binding: CodeSelectActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = CodeSelectActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permite finalizar la actividad si se toca la pantalla
        // fuera de la ventana. Esta actividad se ve como un diálogo.
        setFinishOnTouchOutside(true)

        var tempTitle = getString(R.string.search_client)
        if (savedInstanceState != null) {
            // Dejo de escuchar estos eventos hasta pasar los valores guardados
            // más adelante se reconectan
            binding.autoCompleteTextView.setOnEditorActionListener(null)
            binding.autoCompleteTextView.onItemClickListener = null
            binding.autoCompleteTextView.setOnTouchListener(null)
            binding.autoCompleteTextView.onFocusChangeListener = null
            binding.autoCompleteTextView.setOnDismissListener(null)

            client = savedInstanceState.parcelable(ARG_CLIENT)
        } else {
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1

                client = extras.parcelable(ARG_CLIENT)
            }
        }

        title = tempTitle

        binding.codeSelect.setOnClickListener {
            @Suppress("DEPRECATION") onBackPressed()
        }

        binding.codeClearImageView.setOnClickListener {
            client = null
            refreshClientText(cleanText = true, focus = true)
        }

        // region Setup CATEGORY_CATEGORY ID AUTOCOMPLETE
        // Set a client click checkedChangedListener for auto complete text view
        binding.autoCompleteTextView.threshold = 1
        binding.autoCompleteTextView.hint = tempTitle
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is ClientAdapter) {
                    val it = adapter.getItem(position)
                    if (it != null) {
                        client = it
                    }
                }
                clientSelected()
            }
        binding.autoCompleteTextView.setOnContractsAvailability(this)
        binding.autoCompleteTextView.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus && binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold &&
                    (binding.autoCompleteTextView.adapter?.count ?: 0) > 0 &&
                    !binding.autoCompleteTextView.isPopupShowing
                ) {
                    // Display the suggestion dropdown on focus
                    Handler(Looper.getMainLooper()).post {
                        adjustAndShowDropDown()
                    }
                }
            }
        binding.autoCompleteTextView.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                Screen.showKeyboard(this)
                adjustDropDownHeight()
                return@setOnTouchListener false
            } else if (motionEvent.action == MotionEvent.BUTTON_BACK) {
                Screen.closeKeyboard(this)

                setResult(RESULT_CANCELED, null)
                finish()
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }
        binding.autoCompleteTextView.setOnEditorActionListener { _, actionId, event ->
            val ev = event ?: return@setOnEditorActionListener false
            if (actionId == EditorInfo.IME_ACTION_DONE || ev.keyCode == KeyEvent.KEYCODE_ENTER && ev.action == KeyEvent.ACTION_DOWN) {
                val adapter = binding.autoCompleteTextView.adapter
                if (adapter is ClientAdapter) {
                    if (binding.autoCompleteTextView.text.trim().length >= binding.autoCompleteTextView.threshold) {
                        val all = adapter.getAll()
                        if (all.any()) {
                            var founded = false
                            for (a in all) {
                                if (a.name.startsWith(
                                        binding.autoCompleteTextView.text.toString().trim(), true
                                    )
                                ) {
                                    client = a
                                    founded = true
                                    break
                                }
                            }
                            if (!founded) {
                                for (a in all) {
                                    if (a.name.contains(
                                            binding.autoCompleteTextView.text.toString().trim(),
                                            true
                                        )
                                    ) {
                                        client = a
                                        break
                                    }
                                }
                            }
                        }
                    }
                    clientSelected()
                }
                true
            } else {
                false
            }
        }
        // endregion

        KeyboardVisibilityEvent.registerEventListener(this, this)

        refreshClientText(cleanText = false, focus = true)

        thread { fillAdapter() }
    }

    @Suppress("SameParameterValue")
    private fun showProgressBar(visibility: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = visibility
        }, 20)
    }

    private fun refreshClientText(cleanText: Boolean, focus: Boolean) {
        runOnUiThread {
            binding.autoCompleteTextView.setText(
                if (client == null) {
                    if (cleanText) {
                        ""
                    } else {
                        binding.autoCompleteTextView.text.toString()
                    }
                } else {
                    client?.name ?: ""
                }
            )

            binding.autoCompleteTextView.post {
                binding.autoCompleteTextView.setSelection(
                    binding.autoCompleteTextView.length()
                )
            }

            if (focus) {
                binding.autoCompleteTextView.requestFocus()
            }
        }
    }

    private fun clientSelected() {
        Screen.closeKeyboard(this)

        val data = Intent()
        data.putExtra(ARG_CLIENT, client)
        setResult(RESULT_OK, data)
        finish()
    }

    var isFilling = false
    private fun fillAdapter() {
        if (isFilling) return
        isFilling = true

        try {
            Log.d(tag, "Selecting clients...")
            ClientCoroutines.get {
                val adapter = ClientAdapter(
                    activity = this,
                    resource = R.layout.client_row,
                    clientArray = it,
                    suggestedList = ArrayList()
                )

                runOnUiThread {
                    binding.autoCompleteTextView.setAdapter(adapter)
                    (binding.autoCompleteTextView.adapter as ClientAdapter).notifyDataSetChanged()

                    while (binding.autoCompleteTextView.adapter == null) {
                        // Wait for complete loaded
                    }
                    refreshClientText(cleanText = false, focus = false)
                    showProgressBar(GONE)
                }
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, tag, ex)
        } finally {
            isFilling = false
        }
    }

    @SuppressLint("MissingSuperCall")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        Screen.closeKeyboard(this)

        setResult(RESULT_CANCELED)
        finish()
    }

    //region SOFT KEYBOARD AND DROPDOWN ISSUES
    override fun onVisibilityChanged(isOpen: Boolean) {
        adjustDropDownHeight()
    }

    private fun calculateDropDownHeight(): Int {
        val r = Rect()
        val mRootWindow = window
        val view: View = mRootWindow.decorView
        view.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    private fun adjustAndShowDropDown() {
        topLayout()

        val viewHeight = settingsVm.clientViewHeight
        val maxNeeded = (binding.autoCompleteTextView.adapter?.count ?: 0) * viewHeight
        val availableHeight =
            calculateDropDownHeight() - (binding.autoCompleteTextView.y + binding.autoCompleteTextView.height).toInt()
        var newHeight = availableHeight / viewHeight * viewHeight
        if (maxNeeded < newHeight) {
            newHeight = maxNeeded
        }

        binding.autoCompleteTextView.dropDownHeight = newHeight
        binding.autoCompleteTextView.showDropDown()
    }

    private var isTopping = false
    private fun topLayout() {
        if (isTopping) return
        isTopping = true
        val set = ConstraintSet()

        set.clone(binding.codeSelect)
        set.clear(binding.gralLayout.id, ConstraintSet.BOTTOM)
        set.applyTo(binding.codeSelect)

        isTopping = false
    }

    private var isCentring = false
    private fun centerLayout() {
        if (isCentring) return

        isCentring = true
        val set = ConstraintSet()

        set.clone(binding.codeSelect)
        set.connect(
            binding.gralLayout.id,
            ConstraintSet.BOTTOM,
            binding.codeSelect.id,
            ConstraintSet.BOTTOM,
            0
        )
        set.applyTo(binding.codeSelect)

        isCentring = false
    }

    private fun adjustDropDownHeight() {
        runOnUiThread {
            when {
                binding.autoCompleteTextView.isPopupShowing -> {
                    adjustAndShowDropDown()
                }

                else -> {
                    centerLayout()
                }
            }
        }
    }

    override fun contractsRetrieved(count: Int) {
        if (count > 0) {
            adjustDropDownHeight()
        } else {
            centerLayout()
        }
    }
    // endregion SOFT KEYBOARD AND DROPDOWN ISSUES


    companion object {
        const val ARG_TITLE = "title"
        const val ARG_CLIENT = "client"
    }
}
