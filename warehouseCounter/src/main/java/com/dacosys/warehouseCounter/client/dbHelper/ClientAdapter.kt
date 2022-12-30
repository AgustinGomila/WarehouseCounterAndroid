package com.dacosys.warehouseCounter.client.dbHelper

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.client.`object`.Client
import java.util.*


/**
 * Created by Agustin on 18/01/2017.
 */

@Suppress("SpellCheckingInspection")
class ClientAdapter : ArrayAdapter<Client>, Filterable {
    private var resource: Int = 0
    private var activity: AppCompatActivity

    private var dataSetChangedListener: DataSetChangedListener? = null
    private var checkedChangedListener: CheckedChangedListener? = null

    private var multiSelect: Boolean = false

    private var clientArray: ArrayList<Client> = ArrayList()
    private var suggestedList: ArrayList<Client> = ArrayList()
    private var checkedIdArray: ArrayList<Long> = ArrayList()

    constructor(
        activity: AppCompatActivity,
        resource: Int,
        clients: ArrayList<Client>,
        suggestedList: ArrayList<Client>,
    ) : super(context(), resource, suggestedList) {
        this.activity = activity
        this.resource = resource
        this.clientArray = clients
        this.suggestedList = suggestedList
    }

    fun refreshListeners(
        checkedChangedListener: CheckedChangedListener?,
        dataSetChangedListener: DataSetChangedListener?,
    ) {
        this.checkedChangedListener = checkedChangedListener
        this.dataSetChangedListener = dataSetChangedListener
    }

    interface DataSetChangedListener {
        // Define data you like to return from AysncTask
        fun onDataSetChanged()
    }

    interface CheckedChangedListener {
        // Define data you like to return from AysncTask
        fun onCheckedChanged(
            isChecked: Boolean,
            pos: Int,
        )
    }

    override fun clear() {
        activity.runOnUiThread {
            super.clear()
            clearChecked()
        }
    }

    private fun getIndex(client: Client): Int {
        for (i in 0 until count) {
            val t = (getItem(i) as Client)
            if (t == client) {
                return i
            }
        }
        return -1
    }

    fun count(): Int {
        return count
    }

    fun getAllId(): ArrayList<Long> {
        val r: ArrayList<Long> = ArrayList()
        for (i in 0 until count) {
            val it = getItem(i)
            if (it != null) {
                r.add(it.clientId)
            }
        }
        return r
    }

    fun getAll(): ArrayList<Client> {
        val r: ArrayList<Client> = ArrayList()
        for (i in 0 until count) {
            val t = getItem(i) ?: continue
            r.add(t)
        }
        return r
    }

    fun countChecked(): Int {
        return checkedIdArray.count()
    }

    fun getAllChecked(): ArrayList<Long> {
        return checkedIdArray
    }

    private var isFilling = false
    fun setChecked(clients: ArrayList<Client>, isChecked: Boolean) {
        if (isFilling) return
        isFilling = true

        for (i in clients) {
            setChecked(i, isChecked)
        }

        isFilling = false
        refresh()
    }

    fun setChecked(client: Client, isChecked: Boolean, suspendRefresh: Boolean = false) {
        val position = getIndex(client)
        if (isChecked) {
            if (!checkedIdArray.contains(client.clientId)) {
                checkedIdArray.add(client.clientId)
            }
        } else {
            checkedIdArray.remove(client.clientId)
        }

        if (checkedChangedListener != null) {
            checkedChangedListener!!.onCheckedChanged(isChecked, position)
        }

        if (!suspendRefresh) {
            refresh()
        }
    }

    override fun sort(comparator: Comparator<in Client>) {
        super.sort(customComparator)
    }

    private val customComparator = Comparator { o1: Client?, o2: Client? ->
        ClientComparator().compareNullable(o1, o2)
    }

    fun refresh() {
        activity.runOnUiThread { notifyDataSetChanged() }
        dataSetChangedListener?.onDataSetChanged()
    }

    fun setChecked(checkedItems: ArrayList<Client>) {
        checkedItems.clear()
        setChecked(checkedItems, true)
    }

    private fun clearChecked() {
        checkedIdArray.clear()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        var alreadyExists = true

        // Seleccionamos el layout dependiendo si es
        // un row visible u oculto según su AsseStatus.

        val currentLayout: Int = R.layout.client_row

        if (v == null || v.tag == null) {
            // El view todavía no fue creado, crearlo con el layout correspondiente.
            val vi = LayoutInflater.from(context)
            v = vi.inflate(currentLayout, parent, false)

            alreadyExists = false
        } else {
            // El view ya existe, comprobar que no necesite cambiar de layout.
            if (
            // Row null cambiando...
                v.tag is String && currentLayout == R.layout.client_row) {
                // Ya fue creado, si es un row normal que está siendo seleccionada
                // o un row expandido que está siendo deseleccionado
                // debe cambiar de layout, por lo tanto volver a crearse.
                val vi = LayoutInflater.from(context)
                v = vi.inflate(currentLayout, parent, false)

                alreadyExists = false
            }
        }

        fillSimpleView(position, v!!, alreadyExists)
        return v
    }

    private fun createSimpleViewHolder(v: View, holder: SimpleViewHolder) {
        // Holder para los rows de dropdown.
        holder.checkBox = v.findViewById(R.id.checkBox)
        holder.nameCheckedTextView = v.findViewById(R.id.name)
        holder.taxNumberCheckedTextView = v.findViewById(R.id.tax_number)

        if (multiSelect) {
            holder.checkBox?.visibility = VISIBLE
        } else {
            holder.checkBox?.visibility = GONE
        }

        v.tag = holder
    }

    @SuppressLint("ClickableViewAccessibility", "ObsoleteSdkInt")
    private fun fillSimpleView(position: Int, v: View, alreadyExists: Boolean): View {
        var holder = SimpleViewHolder()
        if (alreadyExists) {
            if (v.tag is SimpleViewHolder || v.tag is String) {
                createSimpleViewHolder(v, holder)
            } else {
                holder = v.tag as SimpleViewHolder
            }
        } else {
            createSimpleViewHolder(v, holder)
        }

        if (position >= 0) {
            val client = getItem(position)

            if (client != null) {
                holder.nameCheckedTextView?.text = client.name
                holder.taxNumberCheckedTextView?.text = client.taxNumber
                if (holder.checkBox != null) {
                    var isSpeakButtonLongPressed = false

                    val checkChangeListener =
                        CompoundButton.OnCheckedChangeListener { _, isChecked ->
                            this.setChecked(client, isChecked, true)
                        }

                    val pressHoldListener =
                        OnLongClickListener { // Do something when your hold starts here.
                            isSpeakButtonLongPressed = true
                            true
                        }

                    val pressTouchListener = OnTouchListener { pView, pEvent ->
                        pView.onTouchEvent(pEvent)
                        // We're only interested in when the button is released.
                        if (pEvent.action == MotionEvent.ACTION_UP) {
                            // We're only interested in anything if our speak button is currently pressed.
                            if (isSpeakButtonLongPressed) {
                                // Do something when the button is released.
                                if (!isFilling) {
                                    holder.checkBox?.setOnCheckedChangeListener(null)
                                    val newState = !(holder.checkBox?.isChecked ?: false)
                                    this.setChecked(getAll(), newState)
                                }
                                isSpeakButtonLongPressed = false
                            }
                        }
                        return@OnTouchListener true
                    }

                    //Important to remove previous checkedChangedListener before calling setChecked
                    holder.checkBox?.setOnCheckedChangeListener(null)
                    holder.checkBox?.isChecked = checkedIdArray.contains(client.clientId)

                    holder.checkBox?.tag = position
                    holder.checkBox?.setOnLongClickListener(pressHoldListener)
                    holder.checkBox?.setOnTouchListener(pressTouchListener)
                    holder.checkBox?.setOnCheckedChangeListener(checkChangeListener)
                }

                // Background colors
                val lightgray =
                    ResourcesCompat.getColor(context().resources, R.color.lightgray, null)
                val white = ResourcesCompat.getColor(context().resources, R.color.text_light, null)

                // Font colors
                val black = ResourcesCompat.getColor(context().resources, R.color.text_dark, null)
                val dimgray = ResourcesCompat.getColor(context().resources, R.color.dimgray, null)

                val colorText = when {
                    !client.active -> dimgray
                    else -> black
                }

                val backColor = when {
                    !client.active -> lightgray
                    else -> white
                }

                v.setBackgroundColor(backColor)
                holder.nameCheckedTextView?.setTextColor(colorText)
                holder.taxNumberCheckedTextView?.setTextColor(colorText)
            }
        }

        if (v.height > 0) {
            viewHeight = v.height
            Log.d(this::class.java.simpleName, "-------{RES: $resource Height:${v.height}}-------")
        }
        return v
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val r: ArrayList<Client> = ArrayList()

                if (constraint != null) {
                    val filterString = constraint.toString().lowercase(Locale.getDefault())
                    var filterableItem: Client

                    for (i in 0 until clientArray.size) {
                        filterableItem = clientArray[i]
                        if (filterableItem.name.lowercase(Locale.getDefault())
                                .contains(filterString) || (filterableItem.taxNumber != null && filterableItem.taxNumber!!.lowercase(
                                Locale.getDefault())
                                .contains(filterString)) || (filterableItem.contactName != null && filterableItem.contactName.toString()
                                .contains(filterString))
                        ) {
                            r.add(filterableItem)
                        }
                    }
                }

                results.values = r
                results.count = r.count()
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                constraint: CharSequence?, results: FilterResults?,
            ) {
                suggestedList.clear()
                suggestedList.addAll(results?.values as ArrayList<Client>)
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }

    companion object {

        var viewHeight = if (Statics.isTablet()) 202 else 115

        class ClientComparator : Comparator<Client> {
            fun compareNullable(o1: Client?, o2: Client?): Int {
                return if (o1 == null || o2 == null) {
                    -1
                } else {
                    compare(o1, o2)
                }
            }

            override fun compare(o1: Client, o2: Client): Int {
                try {
                    val nameComp = o1.name.compareTo(o2.name, true)
                    val taxNumberComp = (o1.taxNumber ?: "").compareTo(o2.taxNumber ?: "", true)
                    val contactNameComp =
                        (o1.contactName ?: "").compareTo(o2.contactName ?: "", true)

                    // Orden natural: name, taxNumber, contactName
                    return when (nameComp) {
                        0 -> when (taxNumberComp) {
                            0 -> contactNameComp
                            else -> taxNumberComp
                        }
                        else -> nameComp
                    }
                } catch (ex: Exception) {
                    return 0
                }
            }
        }
    }

    internal inner class SimpleViewHolder {
        var nameCheckedTextView: CheckedTextView? = null
        var taxNumberCheckedTextView: CheckedTextView? = null
        var checkBox: CheckBox? = null
    }
}