package com.dacosys.warehouseCounter.ui.fragments.item

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingRepository
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.ItemSelectFilterFragmentBinding
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.entity.itemCategory.ItemCategory
import com.dacosys.warehouseCounter.settings.Preference
import com.dacosys.warehouseCounter.ui.activities.item.CodeSelectActivity
import com.dacosys.warehouseCounter.ui.activities.itemCategory.ItemCategorySelectActivity
import org.parceler.Parcels

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ItemSelectFilterFragment.OnFilterChangedListener] interface
 * to handle interaction events.
 * Use the [ItemSelectFilterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ItemSelectFilterFragment : Fragment() {
    private var mCallback: OnFilterChangedListener? = null
    private var rejectNewInstances = false

    // Configuración guardada de los controles que se ven o no se ven
    var itemCode: String = ""
    var itemCategory: ItemCategory? = null
    private var onlyActive: Boolean = true

    // Container Activity must implement this interface
    interface OnFilterChangedListener {
        fun onFilterChanged(
            code: String,
            itemCategory: ItemCategory?,
            onlyActive: Boolean,
        )
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        this.mCallback = null
    }

    fun setListener(listener: OnFilterChangedListener) {
        this.mCallback = listener
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean(ARG_ONLY_ACTIVE, onlyActive)
        savedInstanceState.putString(ARG_CODE, itemCode)
        savedInstanceState.putParcelable(ARG_ITEM_CATEGORY, itemCategory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            loadSavedValues(args)
        }
    }

    private fun loadSavedValues(b: Bundle) {
        itemCategory = Parcels.unwrap<ItemCategory>(b.getParcelable(ARG_ITEM_CATEGORY))
        itemCode = b.getString(ARG_CODE) ?: ""
        onlyActive = b.getBoolean(ARG_ONLY_ACTIVE)
    }

    private var _binding: ItemSelectFilterFragmentBinding? = null

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
        _binding = ItemSelectFilterFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        if (savedInstanceState != null) {
            loadSavedValues(savedInstanceState)
            refreshTextViews()
        }

        binding.onlyActiveCheckBox.setOnCheckedChangeListener(null)
        binding.onlyActiveCheckBox.setOnCheckedChangeListener { _, isChecked ->
            settingViewModel.selectItemOnlyActive = isChecked

            onlyActive = binding.onlyActiveCheckBox.isChecked

            mCallback?.onFilterChanged(
                code = itemCode, itemCategory = itemCategory, onlyActive = onlyActive
            )
        }

        binding.codeTextView.setOnClickListener {
            if (rejectNewInstances) {
                return@setOnClickListener
            }
            rejectNewInstances = true

            val intent = Intent(requireContext(), CodeSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(CodeSelectActivity.ARG_TITLE, getString(R.string.search_by_ean_description))
            intent.putExtra(CodeSelectActivity.ARG_CODE, itemCode)
            resultForItemSelect.launch(intent)
        }
        binding.codeSearchImageView.setOnClickListener { binding.codeTextView.performClick() }
        binding.codeClearImageView.setOnClickListener {
            itemCode = ""
            setCodeText()

            mCallback?.onFilterChanged(
                code = itemCode, itemCategory = itemCategory, onlyActive = onlyActive
            )
        }

        binding.categoryTextView.setOnClickListener {
            if (rejectNewInstances) {
                return@setOnClickListener
            }
            rejectNewInstances = true

            val intent = Intent(requireContext(), ItemCategorySelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(ItemCategorySelectActivity.ARG_ITEM_CATEGORY, itemCategory)
            intent.putExtra(ItemCategorySelectActivity.ARG_TITLE, getString(R.string.select_category))
            resultForCategorySelect.launch(intent)
        }
        binding.categorySearchImageView.setOnClickListener { binding.categoryTextView.performClick() }
        binding.categoryClearImageView.setOnClickListener {
            itemCategory = null
            setCategoryText()

            mCallback?.onFilterChanged(
                code = itemCode, itemCategory = itemCategory, onlyActive = onlyActive
            )
        }

        setVisibleFilters()
        return view
    }

    private val resultForItemSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    itemCode = data.getStringExtra(CodeSelectActivity.ARG_CODE) ?: return@registerForActivityResult

                    setCodeText()

                    mCallback?.onFilterChanged(
                        code = itemCode, itemCategory = itemCategory, onlyActive = onlyActive
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForCategorySelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == AppCompatActivity.RESULT_OK && data != null) {
                    itemCategory = data.getParcelableExtra(ItemCategorySelectActivity.ARG_ITEM_CATEGORY)
                        ?: return@registerForActivityResult

                    setCategoryText()

                    mCallback?.onFilterChanged(
                        code = itemCode, itemCategory = itemCategory, onlyActive = onlyActive
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(requireActivity(), this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    fun refreshTextViews() {
        activity?.runOnUiThread {
            binding.onlyActiveCheckBox.isChecked = onlyActive
            setCodeText()
            setCategoryText()
        }
    }

    private fun setCodeText() {
        activity?.runOnUiThread {
            if (itemCode.isEmpty()) {
                binding.codeTextView.typeface = Typeface.DEFAULT
                binding.codeTextView.text = getString(R.string.search_by_ean_description)
            } else {
                binding.codeTextView.typeface = Typeface.DEFAULT_BOLD
                binding.codeTextView.text = itemCode
            }
        }
    }

    private fun setCategoryText() {
        activity?.runOnUiThread {
            if (itemCategory == null) {
                binding.categoryTextView.typeface = Typeface.DEFAULT
                binding.categoryTextView.text = getString(R.string.search_by_category)
            } else {
                binding.categoryTextView.typeface = Typeface.DEFAULT_BOLD
                binding.categoryTextView.text = itemCategory!!.description
            }
        }
    }

    // Se llama cuando el fragmento está visible ante el usuario.
    // Obviamente, depende del método onStart() de la actividad para saber si la actividad se está mostrando.
    override fun onStart() {
        super.onStart()
        if (mCallback is OnFilterChangedListener) {
            mCallback = activity as OnFilterChangedListener
        }
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    // region Visibility functions
    fun getVisibleFilters(): ArrayList<Preference> {
        val sv = settingViewModel
        val sp = settingRepository

        val r: ArrayList<Preference> = ArrayList()
        if (sv.selectItemSearchByItemEan) {
            r.add(sp.selectItemSearchByItemEan)
        }
        if (sv.selectItemSearchByItemCategory) {
            r.add(sp.selectItemSearchByItemCategory)
        }

        return r
    }

    private fun setVisibleFilters() {
        //Retrieve the values
        if (settingViewModel.selectItemSearchByItemEan) {
            setEanDescriptionVisibility(View.VISIBLE)
        } else {
            setEanDescriptionVisibility(View.GONE)
        }

        if (settingViewModel.selectItemSearchByItemCategory) {
            setCategoryVisibility(View.VISIBLE)
        } else {
            setCategoryVisibility(View.GONE)
        }
    }

    fun setEanDescriptionVisibility(visibility: Int) {
        binding.codePanel.visibility = visibility
        settingViewModel.selectItemSearchByItemEan = visibility == View.VISIBLE
    }

    fun setCategoryVisibility(visibility: Int) {
        binding.categoryPanel.visibility = visibility
        settingViewModel.selectItemSearchByItemCategory = visibility == View.VISIBLE
    }
    // endregion Visibility functions

    companion object {

        // The fragment initialization parameters
        private const val ARG_CODE = "itemCode"
        private const val ARG_ITEM_CATEGORY = "itemCategory"
        private const val ARG_ONLY_ACTIVE = "onlyActive"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance(
            itemCode: String = "",
            itemCategory: ItemCategory? = null,
            onlyActive: Boolean = true
        ): ItemSelectFilterFragment {
            val fragment = ItemSelectFilterFragment()

            val args = Bundle()
            args.putString(ARG_CODE, itemCode)
            args.putParcelable(ARG_ITEM_CATEGORY, itemCategory)
            args.putBoolean(ARG_ONLY_ACTIVE, onlyActive)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}