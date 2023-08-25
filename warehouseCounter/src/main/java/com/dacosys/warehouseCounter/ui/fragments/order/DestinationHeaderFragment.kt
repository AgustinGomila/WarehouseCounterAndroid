package com.dacosys.warehouseCounter.ui.fragments.order

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.DestinationHeaderBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Rack
import com.dacosys.warehouseCounter.ktor.v2.dto.location.Warehouse
import com.dacosys.warehouseCounter.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.ui.activities.location.LocationSelectActivity
import org.parceler.Parcels

/**
 * Destination header fragment
 */
class DestinationHeaderFragment private constructor(builder: Builder) : Fragment() {

    /**
     * Required constructor for Fragments
     */
    constructor() : this(Builder())

    private var locationChangedListener: LocationChangedListener? = null

    private var title: String = ""
    private var warehouse: Warehouse? = null
    var warehouseArea: WarehouseArea? = null
    var rack: Rack? = null

    private var showChangePosButton: Boolean = true

    interface LocationChangedListener {
        fun onLocationChanged(
            warehouse: Warehouse?,
            warehouseArea: WarehouseArea?,
            rack: Rack?,
        )
    }

    private val resultForLocationSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    val wa =
                        Parcels.unwrap<WarehouseArea>(data.getParcelableExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA))
                    val r = Parcels.unwrap<Rack>(data.getParcelableExtra(LocationSelectActivity.ARG_RACK))
                    setDestination(wa, r)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
            }
        }

    private fun setDestination(wa: WarehouseArea? = null, r: Rack? = null) {
        warehouse = null
        warehouseArea = wa
        rack = r

        if (rack != null) {
            warehouseArea = rack?.warehouseArea
        }
        if (warehouseArea != null) {
            warehouse = warehouseArea?.warehouse
        }

        if (_binding != null) {
            refreshViews()
        }

        locationChangedListener?.onLocationChanged(
            warehouse = warehouse,
            warehouseArea = warehouseArea,
            rack = rack
        )
    }

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun refreshViews() {
        activity?.runOnUiThread {
            binding.titleTextView.text = title

            binding.warehouseTextView.text = warehouse?.description ?: ""
            TooltipCompat.setTooltipText(binding.warehouseTextView, warehouse?.description ?: "")

            binding.warehouseAreaTextView.text = warehouseArea?.description ?: ""
            TooltipCompat.setTooltipText(
                binding.warehouseAreaTextView,
                warehouseArea?.description ?: ""
            )

            binding.rackCodeTextView.text = rack?.code ?: ""
            TooltipCompat.setTooltipText(binding.rackCodeTextView, rack?.code ?: "")
        }
    }

    fun setDestination(rack: Rack?) {
        setDestination(r = rack)
    }

    fun setDestination(warehouseArea: WarehouseArea?) {
        setDestination(wa = warehouseArea)
    }

    private var _binding: DestinationHeaderBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        locationChangedListener = null
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean(ARG_SHOW_CHANGE_POSITION_BUTTON, showChangePosButton)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
        savedInstanceState.putParcelable(ARG_RACK, rack)
        savedInstanceState.putString(ARG_TITLE, title)
    }

    private fun loadBundleValues(b: Bundle) {
        showChangePosButton = b.getBoolean(ARG_SHOW_CHANGE_POSITION_BUTTON)
        warehouseArea = b.getParcelable(ARG_WAREHOUSE_AREA)
        rack = b.getParcelable(ARG_RACK)
        title = b.getString(ARG_TITLE) ?: ""
    }

    override fun onStart() {
        super.onStart()
        if (requireActivity() is LocationChangedListener) {
            locationChangedListener = activity as LocationChangedListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DestinationHeaderBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadBundleValues(savedInstanceState)
        }

        setButtonPanelVisibility()

        binding.changePositionButton.setOnClickListener { changePosition() }
        TooltipCompat.setTooltipText(
            binding.changePositionButton,
            getString(R.string.change_position)
        )

        setDestination(warehouseArea, rack)

        return view
    }

    private fun setButtonPanelVisibility() {
        if (showChangePosButton) {
            binding.changePosPanel.visibility = View.VISIBLE
        } else {
            binding.changePosPanel.visibility = View.GONE
        }
    }

    private fun changePosition() {
        val intent = Intent(requireContext(), LocationSelectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val title = getString(R.string.select_destination)
        intent.putExtra(LocationSelectActivity.ARG_TITLE, title)
        intent.putExtra(LocationSelectActivity.ARG_WAREHOUSE_AREA, warehouseArea)
        intent.putExtra(LocationSelectActivity.ARG_RACK, rack)
        resultForLocationSelect.launch(intent)
    }

    init {
        title = builder.title
        warehouse = builder.warehouse
        warehouseArea = builder.warehouseArea
        rack = builder.rack
        showChangePosButton = builder.showChangePosButton
    }

    class Builder {
        internal var title: String = ""
        internal var warehouse: Warehouse? = null
        internal var warehouseArea: WarehouseArea? = null
        internal var rack: Rack? = null
        internal var showChangePosButton: Boolean = true

        fun build(): DestinationHeaderFragment {
            return DestinationHeaderFragment(this)
        }

        @Suppress("unused")
        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        @Suppress("unused")
        fun setWarehouse(warehouse: Warehouse?): Builder {
            this.warehouse = warehouse
            return this
        }

        @Suppress("unused")
        fun setWarehouseArea(warehouseArea: WarehouseArea?): Builder {
            this.warehouseArea = warehouseArea
            return this
        }

        @Suppress("unused")
        fun setRack(rack: Rack?): Builder {
            this.rack = rack
            return this
        }

        @Suppress("unused")
        fun setShowChangePosButton(showChangePosButton: Boolean): Builder {
            this.showChangePosButton = showChangePosButton
            return this
        }
    }


    companion object {
        const val ARG_SHOW_CHANGE_POSITION_BUTTON = "showChangePosButton"
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_RACK = "rack"
    }
}
