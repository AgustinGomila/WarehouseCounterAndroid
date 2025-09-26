package com.example.warehouseCounter.ui.fragments.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.R
import com.example.warehouseCounter.data.room.dao.user.UserCoroutines
import com.example.warehouseCounter.data.room.entity.user.User
import com.example.warehouseCounter.databinding.FragmentSpinnerBinding
import com.example.warehouseCounter.misc.Statics
import com.example.warehouseCounter.ui.adapter.generic.GenericAdapter
import com.example.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [UserSpinnerFragment.OnItemSelectedListener] interface
 * to handle interaction events.
 * Use the [UserSpinnerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UserSpinnerFragment : Fragment() {
    interface OnSpinnerFillListener {
        fun onSpinnerFill(status: SyncStatus)
    }

    interface OnItemSelectedListener {
        fun onItemSelected(user: User?)
    }

    private var allUser: ArrayList<User>? = ArrayList()
    private var showGeneralLevel = false
    private var oldPos = -1
    private var mCallback: OnItemSelectedListener? = null
    private var mListener: OnSpinnerFillListener? = null
    private var initialUserId: Long? = null

    var selectedUser: User?
        get() = (binding.fragmentSpinner.selectedItem as? User)?.takeIf { it.userId != 0L }
        set(user) {
            _binding?.let { binding ->
                val adapter = binding.fragmentSpinner.adapter as? GenericAdapter<*> ?: return
                val index = (0 until adapter.count).firstOrNull { user == adapter.getItem(it) }
                this.requireActivity().runOnUiThread {
                    binding.fragmentSpinner.setSelection(index ?: 0)
                }
            }
        }

    val selectedUserPass
        get() = (binding.fragmentSpinner.selectedItem as? User)?.password.orEmpty()

    var selectedUserId: Long?
        get() = (binding.fragmentSpinner.selectedItem as? User)?.userId
        set(user) {
            _binding?.let { binding ->
                val adapter = binding.fragmentSpinner.adapter as? GenericAdapter<*> ?: return
                val index = (0 until adapter.count).firstOrNull { user == adapter.getItem(it) }
                this.requireActivity().runOnUiThread {
                    binding.fragmentSpinner.setSelection(index ?: 0)
                }
            }
        }

    val count: Int
        get() = binding.fragmentSpinner.adapter?.count ?: 0

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putInt("oldPos", oldPos)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            oldPos = savedInstanceState.getInt("oldPos")
        }

        if (arguments != null) {
            allUser = requireArguments().parcelableArrayList(ARG_ALL_USER)
            showGeneralLevel = requireArguments().getBoolean(ARG_SHOW_GENERAL_LEVEL)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (initialUserId != null) {
            selectedUserId = initialUserId
            initialUserId = null
        }
    }

    private var _binding: FragmentSpinnerBinding? = null

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
        _binding = FragmentSpinnerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.autoResizeTextView.visibility = View.GONE
        binding.fragmentSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (oldPos != position) {
                        oldPos = position
                        mCallback?.onItemSelected(parent.getItemAtPosition(position) as User)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    oldPos = -1
                    mCallback?.onItemSelected(null)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        try {
            mCallback = activity as OnItemSelectedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnItemSelectedListener")
        }

        try {
            mListener = activity as OnSpinnerFillListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnSpinnerFillListener")
        }
    }

    // Se llama cuando el fragmento ya no está asociado a la actividad anfitriona.
    override fun onDetach() {
        super.onDetach()
        mListener = null
        mCallback = null
    }

    fun reFill() {
        getUsers()
    }

    private fun getUsers() {
        UserCoroutines.get {
            if (!it.any() && Statics.SUPER_DEMO_MODE) {
                addFantasyUsers()
            } else {
                allUser = it
                fillAdapter()
            }
        }
    }

    private fun addFantasyUsers() {
        // En modo desarrollo o para mostrar sin datos reales
        // y si no hay usuarios agregados, agrego
        // DATOS FALSOS de 5 items de fantasía
        val fantasyNames = arrayListOf<String>()
        fantasyNames.add("miguel")
        fantasyNames.add("adriana")
        fantasyNames.add("milagros")
        fantasyNames.add("arturo")
        fantasyNames.add("agustin")
        for (i in 1..5) {
            UserCoroutines.add(
                User(
                    name = fantasyNames[i - 1],
                    password = "xxxx"
                )
            )
        }

        UserCoroutines.get {
            allUser = it
            fillAdapter()
        }
    }

    private fun fillAdapter() {
        val spinnerAdapter = GenericAdapter(
            resource = R.layout.custom_spinner_dropdown_item,
            items = allUser.orEmpty(),
            getText = { it.name },
            getTextColor = { user ->
                val gray = ResourcesCompat.getColor(requireContext().resources, R.color.dimgray, null)
                val black = ResourcesCompat.getColor(requireContext().resources, R.color.black, null)
                if (user.userId == 0L) gray else black
            }
        )

        // Actualización de la UI en el hilo principal
        requireActivity().runOnUiThread {
            binding.fragmentSpinner.adapter = spinnerAdapter
            if (oldPos >= 0) binding.fragmentSpinner.setSelection(oldPos)
            spinnerAdapter.notifyDataSetChanged()
        }

        // Esperar hasta que el adapter esté cargado (si es realmente necesario)
        while (binding.fragmentSpinner.adapter == null) {
            // Horrible wait for full load
        }

        if (mListener != null) {
            mListener!!.onSpinnerFill(SyncStatus.FINISHED)
        }
    }

    companion object {
        // The fragment initialization parameters
        private const val ARG_ALL_USER = "allUser"
        private const val ARG_SHOW_GENERAL_LEVEL = "showGeneralLevel"

        /**
         * Estados de las tareas asincrónicas
         */
        enum class SyncStatus(val id: Int) {
            STARTING(1), CANCELED(2), FINISHED(3), RUNNING(4), CRASHED(5)
        }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param allUser Parameter 1.
         * @return A new instance of fragment user_binding.fragmentSpinner.
         */
        fun newInstance(allUser: ArrayList<User>, showGeneralLevel: Boolean): UserSpinnerFragment {
            val fragment = UserSpinnerFragment()

            val args = Bundle()
            args.putParcelableArrayList(ARG_ALL_USER, allUser)
            args.putBoolean(ARG_SHOW_GENERAL_LEVEL, showGeneralLevel)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}