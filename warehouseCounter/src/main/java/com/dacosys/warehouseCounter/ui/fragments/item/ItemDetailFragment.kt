package com.dacosys.warehouseCounter.ui.fragments.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.databinding.ItemDetailBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.model.item.Item
import com.dacosys.warehouseCounter.model.itemCode.ItemCode
import com.dacosys.warehouseCounter.model.price.Price
import com.dacosys.warehouseCounter.retrofit.functions.GetPrice
import com.dacosys.warehouseCounter.retrofit.search.SearchPrice
import com.dacosys.warehouseCounter.ui.snackBar.MakeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarEventData
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [ItemDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ItemDetailFragment : Fragment() {
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
                binding.eanAutoResizeTextView.visibility = GONE
            } else {
                binding.eanAutoResizeTextView.text = item!!.ean
                binding.eanAutoResizeTextView.visibility = VISIBLE
            }

            if (item!!.description.isEmpty()) {
                binding.descriptionAutoResizeTextView.text = ""
                binding.descriptionAutoResizeTextView.visibility = GONE
            } else {
                binding.descriptionAutoResizeTextView.text = item!!.description
                binding.descriptionAutoResizeTextView.visibility = VISIBLE
            }

            binding.idAutoResizeTextView.text = item!!.itemId.toString()
            binding.idAutoResizeTextView.visibility = VISIBLE

            if (item!!.externalId.isNullOrEmpty()) {
                binding.extIdAutoResizeTextView.text = ""
                binding.extIdAutoResizeTextView.visibility = GONE
                binding.extIdTextView.visibility = GONE
            } else {
                binding.extIdAutoResizeTextView.text = item!!.externalId
                binding.extIdAutoResizeTextView.visibility = VISIBLE
                binding.extIdTextView.visibility = VISIBLE
            }

            if (item!!.itemCategoryStr.isEmpty()) {
                binding.categoryAutoResizeTextView.text = ""
                binding.categoryAutoResizeTextView.visibility = GONE
                binding.categoryTextView.visibility = GONE
            } else {
                binding.categoryAutoResizeTextView.text = item!!.itemCategoryStr
                binding.categoryAutoResizeTextView.visibility = VISIBLE
                binding.categoryTextView.visibility = VISIBLE
            }

            if (item!!.price == null) {
                binding.priceAutoResizeTextView.text = ""
                binding.priceAutoResizeTextView.visibility = GONE
                binding.priceTextView.visibility = GONE
            } else {
                binding.priceAutoResizeTextView.text =
                    String.format("$ %s", Statics.roundToString(item!!.price!!, 2))
                binding.priceAutoResizeTextView.visibility = VISIBLE
                binding.priceTextView.visibility = VISIBLE
            }

            if (itemCode != null) {
                binding.itemCodeConstraintLayout.visibility = VISIBLE

                binding.codeAutoResizeTextView.text = itemCode!!.code
                binding.codeAutoResizeTextView.visibility = VISIBLE
                binding.codeTextView.visibility = VISIBLE

                binding.qtyAutoResizeTextView.text =
                    Statics.roundToString(itemCode!!.qty, Statics.decimalPlaces)
                binding.qtyAutoResizeTextView.visibility = VISIBLE
                binding.qtyTextView.visibility = VISIBLE
            } else {
                binding.itemCodeConstraintLayout.visibility = GONE
            }

            fun fillPriceLayout(it: ArrayList<Price>) {
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

            // Obtenemos los precios en un thread aparte
            thread {
                GetPrice(searchPrice = SearchPrice(item!!.itemId),
                    onEvent = { showSnackBar(it) },
                    onFinish = { fillPriceLayout(it) }).execute()
            }
        }
    }

    private fun showSnackBar(it: SnackBarEventData) {
        MakeText.makeText(binding.root, it.text, it.snackBarType)
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