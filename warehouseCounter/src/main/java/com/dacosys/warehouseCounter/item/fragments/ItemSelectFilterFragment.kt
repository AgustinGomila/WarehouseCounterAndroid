package com.dacosys.warehouseCounter.item.fragments

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
import com.dacosys.warehouseCounter.Statics
import com.dacosys.warehouseCounter.databinding.ItemSelectFilterFragmentBinding
import com.dacosys.warehouseCounter.errorLog.ErrorLog
import com.dacosys.warehouseCounter.item.activities.CodeSelectActivity
import com.dacosys.warehouseCounter.itemCategory.`object`.ItemCategory
import com.dacosys.warehouseCounter.itemCategory.activities.ItemCategorySelectActivity
import com.dacosys.warehouseCounter.misc.Preference
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

        savedInstanceState.putBoolean("onlyActive", onlyActive)
        savedInstanceState.putString("itemCode", itemCode)
        savedInstanceState.putParcelable("itemCategory", itemCategory)
    }

    // Este método es llamado cuando el fragmento se está creando.
    // En el puedes inicializar todos los componentes que deseas guardar si el fragmento fue pausado o detenido.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            itemCategory =
                Parcels.unwrap<ItemCategory>(requireArguments().getParcelable("itemCategory"))
            itemCode = requireArguments().getString("itemCode") ?: ""

            onlyActive = Statics.prefsGetBoolean(Preference.selectItemOnlyActive)
        }
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
            itemCode = savedInstanceState.getString("itemCode") ?: ""
            itemCategory = savedInstanceState.getParcelable("itemCategory")
            onlyActive = savedInstanceState.getBoolean("onlyActive")

            refreshTextViews()
        }

        binding.onlyActiveCheckBox.setOnCheckedChangeListener(null)
        binding.onlyActiveCheckBox.setOnCheckedChangeListener { _, isChecked ->
            Statics.prefsPutBoolean(Preference.selectItemOnlyActive.key, isChecked)

            onlyActive = binding.onlyActiveCheckBox.isChecked

            mCallback?.onFilterChanged(
                code = itemCode,
                itemCategory = itemCategory,
                onlyActive = onlyActive
            )
        }

        binding.codeTextView.setOnClickListener {
            if (rejectNewInstances) {
                return@setOnClickListener
            }
            rejectNewInstances = true

            val intent = Intent(requireContext(), CodeSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("title", getString(R.string.search_by_ean_description))
            intent.putExtra("itemCode", itemCode)
            resultForItemSelect.launch(intent)
        }

        binding.itemCategoryTextView.setOnClickListener {
            if (rejectNewInstances) {
                return@setOnClickListener
            }
            rejectNewInstances = true

            val intent = Intent(requireContext(), ItemCategorySelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra("itemCategory", itemCategory)
            intent.putExtra("title", getString(R.string.select_category))
            resultForCategorySelect.launch(intent)
        }

        binding.codeSearchImageView.setOnClickListener { binding.codeTextView.performClick() }
        binding.codeClearImageView.setOnClickListener {
            itemCode = ""
            setCodeText()

            mCallback?.onFilterChanged(
                code = itemCode,
                itemCategory = itemCategory,
                onlyActive = onlyActive
            )
        }
        binding.categorySearchImageView.setOnClickListener { binding.itemCategoryTextView.performClick() }
        binding.categoryClearImageView.setOnClickListener {
            itemCategory = null
            setCategoryText()

            mCallback?.onFilterChanged(
                code = itemCode,
                itemCategory = itemCategory,
                onlyActive = onlyActive
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
                    itemCode = data.getStringExtra("itemCode") ?: return@registerForActivityResult

                    setCodeText()

                    mCallback?.onFilterChanged(
                        code = itemCode,
                        itemCategory = itemCategory,
                        onlyActive = onlyActive
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
                    itemCategory =
                        data.getParcelableExtra("itemCategory") ?: return@registerForActivityResult

                    setCategoryText()

                    mCallback?.onFilterChanged(
                        code = itemCode,
                        itemCategory = itemCategory,
                        onlyActive = onlyActive
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
                binding.itemCategoryTextView.typeface = Typeface.DEFAULT
                binding.itemCategoryTextView.text = getString(R.string.search_by_category)
            } else {
                binding.itemCategoryTextView.typeface = Typeface.DEFAULT_BOLD
                binding.itemCategoryTextView.text = itemCategory!!.description
            }
        }
    }

    // Se llama cuando el fragmento esta visible ante el usuario.
    // Obviamente depende del método onStart() de la actividad para saber si la actividad se está mostrando.
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
        val r: ArrayList<Preference> = ArrayList()
        if (Statics.prefsGetBoolean(Preference.selectItemSearchByItemEan)) {
            r.add(Preference.selectItemSearchByItemEan)
        }
        if (Statics.prefsGetBoolean(Preference.selectItemSearchByItemCategory)) {
            r.add(Preference.selectItemSearchByItemCategory)
        }

        return r
    }

    private fun setVisibleFilters() {
        //Retrieve the values
        if (Statics.prefsGetBoolean(Preference.selectItemSearchByItemEan)) {
            setEanDescriptionVisibility(View.VISIBLE)
        } else {
            setEanDescriptionVisibility(View.GONE)
        }

        if (Statics.prefsGetBoolean(Preference.selectItemSearchByItemCategory)) {
            setCategoryVisibility(View.VISIBLE)
        } else {
            setCategoryVisibility(View.GONE)
        }
    }

    fun setEanDescriptionVisibility(visibility: Int) {
        binding.codePanel.visibility = visibility
        Statics.prefsPutBoolean(
            Preference.selectItemSearchByItemEan.key,
            visibility == View.VISIBLE
        )
    }

    fun setCategoryVisibility(visibility: Int) {
        binding.categoryPanel.visibility = visibility
        Statics.prefsPutBoolean(
            Preference.selectItemSearchByItemCategory.key,
            visibility == View.VISIBLE
        )
    }
    // endregion Visibility functions

    companion object {

        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_CODE = "itemCode"
        private const val ARG_ITEM_CATEGORY = "itemCategory"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance(itemCode: String, itemCategory: ItemCategory?): ItemSelectFilterFragment {
            val fragment = ItemSelectFilterFragment()

            val args = Bundle()
            args.putString(ARG_CODE, itemCode)
            args.putParcelable(ARG_ITEM_CATEGORY, itemCategory)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}