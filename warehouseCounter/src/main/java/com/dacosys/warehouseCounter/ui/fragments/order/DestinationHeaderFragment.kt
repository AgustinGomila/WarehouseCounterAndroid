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
 * A simple [Fragment] subclass.
 * Use the [DestinationHeaderFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DestinationHeaderFragment :
    Fragment() {
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

    private var locationChangedListener: LocationChangedListener? = null

    private var title: String = ""
    private var warehouse: Warehouse? = null
    var warehouseArea: WarehouseArea? = null
    var rack: Rack? = null

    private var showChangePosButton: Boolean = true

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean(ARG_SHOW_CHANGE_POSITION_BUTTON, showChangePosButton)
        savedInstanceState.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
        savedInstanceState.putParcelable(ARG_RACK, rack)
        savedInstanceState.putString(ARG_TITLE, title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            warehouseArea = requireArguments().getParcelable(ARG_WAREHOUSE_AREA)
            rack = requireArguments().getParcelable(ARG_RACK)
        }
    }

    fun setChangeLocationListener(listener: LocationChangedListener) {
        this.locationChangedListener = listener
    }

    fun showChangePostButton(show: Boolean) {
        showChangePosButton = show
        setButtonPanelVisibility()
    }

    fun setTitle(title: String) {
        this.title = title
        if (_binding == null) return
        binding.titleTextView.text = title
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DestinationHeaderBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            showChangePosButton = savedInstanceState.getBoolean(ARG_SHOW_CHANGE_POSITION_BUTTON)
            warehouseArea = savedInstanceState.getParcelable(ARG_WAREHOUSE_AREA)
            rack = savedInstanceState.getParcelable(ARG_RACK)
            val t1 = savedInstanceState.getString(ARG_TITLE)
            title =
                if (!t1.isNullOrEmpty()) t1
                else getString(R.string.destination)
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
        if (_binding == null) return

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

    companion object {
        const val ARG_SHOW_CHANGE_POSITION_BUTTON = "showChangePosButton"
        const val ARG_TITLE = "title"
        const val ARG_WAREHOUSE_AREA = "warehouseArea"
        const val ARG_RACK = "rack"

        fun newInstance(
            locationChangedListener: LocationChangedListener,
            warehouseArea: WarehouseArea,
            rack: Rack?,
        ): DestinationHeaderFragment {
            val fragment = DestinationHeaderFragment()

            val args = Bundle()
            args.putParcelable(ARG_WAREHOUSE_AREA, warehouseArea)
            args.putParcelable(ARG_RACK, rack)

            fragment.arguments = args
            fragment.locationChangedListener = locationChangedListener

            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
