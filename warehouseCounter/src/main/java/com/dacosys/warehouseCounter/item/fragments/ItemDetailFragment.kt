package com.dacosys.warehouseCounter.item.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.databinding.ItemDetailBinding
import com.dacosys.warehouseCounter.item.`object`.Item
import com.dacosys.warehouseCounter.itemCode.`object`.ItemCode

/**
 * A simple [Fragment] subclass.
 * Use the [ItemDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ItemDetailFragment : androidx.fragment.app.Fragment() {
    private var item: Item? = null
    private var itemCode: ItemCode? = null

    // Este método es llamado cuando el fragmento se está creando.
    // En el puedes inicializar todos los componentes que deseas guardar si el fragmento fue pausado o detenido.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            item = requireArguments().getParcelable("item")
            itemCode = requireArguments().getParcelable("itemCode")
        }
    }

    private var _binding: ItemDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable("item", item)
        outState.putParcelable("itemCode", itemCode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ItemDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            item = savedInstanceState.getParcelable("item")
            itemCode = savedInstanceState.getParcelable("itemCode")
        }

        fillControls()

        return view
    }

    private fun fillControls() {
        if (item != null) {
            if (item!!.ean.isEmpty()) {
                binding.eanAutoResizeTextView.text = ""
                binding.eanAutoResizeTextView.visibility = View.GONE
            } else {
                binding.eanAutoResizeTextView.text = item!!.ean
                binding.eanAutoResizeTextView.visibility = View.VISIBLE
            }

            if (item!!.description.isEmpty()) {
                binding.descriptionAutoResizeTextView.text = ""
                binding.descriptionAutoResizeTextView.visibility = View.GONE
            } else {
                binding.descriptionAutoResizeTextView.text = item!!.description
                binding.descriptionAutoResizeTextView.visibility = View.VISIBLE
            }

            binding.idAutoResizeTextView.text = item!!.itemId.toString()
            binding.idAutoResizeTextView.visibility = View.VISIBLE

            if (item!!.externalId.isNullOrEmpty()) {
                binding.extIdAutoResizeTextView.text = ""
                binding.extIdAutoResizeTextView.visibility = View.GONE
                binding.extIdTextView.visibility = View.GONE
            } else {
                binding.extIdAutoResizeTextView.text = item!!.externalId
                binding.extIdAutoResizeTextView.visibility = View.VISIBLE
                binding.extIdTextView.visibility = View.VISIBLE
            }

            if (item!!.itemCategoryStr.isEmpty()) {
                binding.categoryAutoResizeTextView.text = ""
                binding.categoryAutoResizeTextView.visibility = View.GONE
                binding.categoryTextView.visibility = View.GONE
            } else {
                binding.categoryAutoResizeTextView.text = item!!.itemCategoryStr
                binding.categoryAutoResizeTextView.visibility = View.VISIBLE
                binding.categoryTextView.visibility = View.VISIBLE
            }

            if (item!!.price == null) {
                binding.priceAutoResizeTextView.text = ""
                binding.priceAutoResizeTextView.visibility = View.GONE
                binding.priceTextView.visibility = View.GONE
            } else {
                binding.priceAutoResizeTextView.text =
                    String.format("$ %s", Statics.roundToString(item!!.price!!, 2))
                binding.priceAutoResizeTextView.visibility = View.VISIBLE
                binding.priceTextView.visibility = View.VISIBLE
            }

            if (itemCode != null) {
                binding.itemCodeConstraintLayout.visibility = View.VISIBLE

                binding.codeAutoResizeTextView.text = itemCode!!.code
                binding.codeAutoResizeTextView.visibility = View.VISIBLE
                binding.codeTextView.visibility = View.VISIBLE

                binding.qtyAutoResizeTextView.text =
                    Statics.roundToString(itemCode!!.qty, Statics.decimalPlaces)
                binding.qtyAutoResizeTextView.visibility = View.VISIBLE
                binding.qtyTextView.visibility = View.VISIBLE
            } else {
                binding.itemCodeConstraintLayout.visibility = View.GONE
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param item Parameter 1.
         * @return A new instance of fragment strOption_spinner.
         */
        fun newInstance(item: Item): ItemDetailFragment {
            val fragment = ItemDetailFragment()

            val args = Bundle()
            args.putParcelable("item", item)

            fragment.arguments = args
            return fragment
        }

        fun newInstance(itemCode: ItemCode): ItemDetailFragment {
            val fragment = ItemDetailFragment()

            val args = Bundle()
            args.putParcelable("item", itemCode.item)
            args.putParcelable("itemCode", itemCode)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}