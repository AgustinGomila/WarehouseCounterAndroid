package com.dacosys.warehouseCounter.ui.fragments.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.data.ktor.v2.dto.item.Price
import com.dacosys.warehouseCounter.data.ktor.v2.functions.item.ViewItem
import com.dacosys.warehouseCounter.data.room.dao.item.ItemCoroutines
import com.dacosys.warehouseCounter.data.room.entity.item.Item
import com.dacosys.warehouseCounter.data.room.entity.itemCode.ItemCode
import com.dacosys.warehouseCounter.databinding.ItemDetailBinding
import com.dacosys.warehouseCounter.misc.utils.NumberUtils.Companion.roundToString
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [ItemDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ItemDetailFragment : Fragment() {
    private var item: Item? = null
    private var itemCode: ItemCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            item = requireArguments().parcelable(ARG_ITEM)
            itemCode = requireArguments().parcelable(ARG_ITEM_CODE)

            val ic = itemCode ?: return
            ItemCoroutines.getById(ic.itemId ?: 0L) {
                item = it
            }
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

        outState.putParcelable(ARG_ITEM, item)
        outState.putParcelable(ARG_ITEM_CODE, itemCode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ItemDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            item = savedInstanceState.parcelable(ARG_ITEM)
            itemCode = savedInstanceState.parcelable(ARG_ITEM_CODE)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fillControls()
    }

    private fun fillControls() {
        val it = this.item ?: return

        if (it.ean.isEmpty()) {
            binding.eanAutoResizeTextView.text = ""
            binding.eanAutoResizeTextView.visibility = GONE
        } else {
            binding.eanAutoResizeTextView.text = it.ean
            binding.eanAutoResizeTextView.visibility = VISIBLE
        }

        if (it.description.isEmpty()) {
            binding.descriptionAutoResizeTextView.text = ""
            binding.descriptionAutoResizeTextView.visibility = GONE
        } else {
            binding.descriptionAutoResizeTextView.text = it.description
            binding.descriptionAutoResizeTextView.visibility = VISIBLE
        }

        binding.idAutoResizeTextView.text = it.itemId.toString()
        binding.idAutoResizeTextView.visibility = VISIBLE

        if (it.externalId.isNullOrEmpty()) {
            binding.extIdAutoResizeTextView.text = ""
            binding.extIdAutoResizeTextView.visibility = GONE
            binding.extIdTextView.visibility = GONE
        } else {
            binding.extIdAutoResizeTextView.text = it.externalId
            binding.extIdAutoResizeTextView.visibility = VISIBLE
            binding.extIdTextView.visibility = VISIBLE
        }

        if (it.itemCategoryStr.isEmpty()) {
            binding.categoryAutoResizeTextView.text = ""
            binding.categoryAutoResizeTextView.visibility = GONE
            binding.categoryTextView.visibility = GONE
        } else {
            binding.categoryAutoResizeTextView.text = it.itemCategoryStr
            binding.categoryAutoResizeTextView.visibility = VISIBLE
            binding.categoryTextView.visibility = VISIBLE
        }

        if (it.price == null) {
            binding.priceAutoResizeTextView.text = ""
            binding.priceAutoResizeTextView.visibility = GONE
            binding.priceTextView.visibility = GONE
        } else {
            binding.priceAutoResizeTextView.text =
                String.format("$ %s", roundToString(it.price ?: 0f, 2))
            binding.priceAutoResizeTextView.visibility = VISIBLE
            binding.priceTextView.visibility = VISIBLE
        }

        if (itemCode != null) {
            binding.itemCodeConstraintLayout.visibility = VISIBLE

            binding.codeAutoResizeTextView.text = itemCode!!.code
            binding.codeAutoResizeTextView.visibility = VISIBLE
            binding.codeTextView.visibility = VISIBLE

            binding.qtyAutoResizeTextView.text =
                roundToString(itemCode?.qty ?: 0.0, settingsVm.decimalPlaces)
            binding.qtyAutoResizeTextView.visibility = VISIBLE
            binding.qtyTextView.visibility = VISIBLE
        } else {
            binding.itemCodeConstraintLayout.visibility = GONE
        }

        // Get the prices of this particular item in a separate thread
        thread {
            ViewItem(
                id = it.itemId.toString(),
                action = ViewItem.defaultAction,
                onEvent = {
                    if (it.snackBarType != SnackBarType.SUCCESS)
                        showMessage(it.text, it.snackBarType)
                },
                onFinish = {
                    val prices = it?.prices ?: listOf()
                    fillPriceLayout(prices)
                }
            ).execute()
        }
    }

    private fun fillPriceLayout(it: List<Price>) {
        if (_binding == null) return

        if (!it.any()) {
            binding.priceLayout.visibility = GONE
            return
        }

        binding.priceLayout.visibility = VISIBLE

        binding.priceDescTextView1.visibility = GONE
        binding.priceListTextView1.visibility = GONE

        binding.priceDescTextView2.visibility = GONE
        binding.priceListTextView2.visibility = GONE

        binding.priceDescTextView3.visibility = GONE
        binding.priceListTextView3.visibility = GONE

        binding.priceDescTextView4.visibility = GONE
        binding.priceListTextView4.visibility = GONE

        binding.priceDescTextView5.visibility = GONE
        binding.priceListTextView5.visibility = GONE

        binding.priceDescTextView6.visibility = GONE
        binding.priceListTextView6.visibility = GONE

        binding.priceDescTextView7.visibility = GONE
        binding.priceListTextView7.visibility = GONE

        binding.priceDescTextView8.visibility = GONE
        binding.priceListTextView8.visibility = GONE

        binding.priceDescTextView9.visibility = GONE
        binding.priceListTextView9.visibility = GONE

        binding.priceDescTextView10.visibility = GONE
        binding.priceListTextView10.visibility = GONE

        val d: ArrayList<TextView> = arrayListOf(
            binding.priceDescTextView1,
            binding.priceDescTextView2,
            binding.priceDescTextView3,
            binding.priceDescTextView4,
            binding.priceDescTextView5,
            binding.priceDescTextView6,
            binding.priceDescTextView7,
            binding.priceDescTextView8,
            binding.priceDescTextView9,
            binding.priceDescTextView10
        )
        val l: ArrayList<TextView> = arrayListOf(
            binding.priceListTextView1,
            binding.priceListTextView2,
            binding.priceListTextView3,
            binding.priceListTextView4,
            binding.priceListTextView5,
            binding.priceListTextView6,
            binding.priceListTextView7,
            binding.priceListTextView8,
            binding.priceListTextView9,
            binding.priceListTextView10
        )

        for ((index, p) in it.withIndex()) {
            if (index == 10) break
            if (p.active != 1) continue

            l[index].text = buildString {
                append("$ ")
                append(p.price)
            }
            l[index].visibility = VISIBLE

            d[index].text = p.itemPriceListDescription
            d[index].visibility = VISIBLE
        }
    }

    private fun showMessage(text: String, snackBarType: SnackBarType) {
        if (_binding == null) return
        makeText(binding.root, text, snackBarType)
    }

    companion object {
        const val ARG_ITEM = "item"
        const val ARG_ITEM_CODE = "itemCode"

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
            args.putParcelable(ARG_ITEM, item)

            fragment.arguments = args
            return fragment
        }

        fun newInstance(itemCode: ItemCode): ItemDetailFragment {
            val fragment = ItemDetailFragment()

            val args = Bundle()
            args.putParcelable(ARG_ITEM_CODE, itemCode)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
