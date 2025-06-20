package com.dacosys.warehouseCounter.ui.fragments.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.data.room.dao.user.UserCoroutines
import com.dacosys.warehouseCounter.data.room.entity.user.User
import com.dacosys.warehouseCounter.databinding.FragmentSpinnerBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.ui.adapter.user.UserAdapter
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelableArrayList

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
        get() {
            if (_binding == null) return null

            val temp = binding.fragmentSpinner.selectedItem
            return when {
                temp != null -> {
                    val r = temp as User
                    when (r.userId) {
                        0L -> null
                        else -> r
                    }
                }

                else -> null
            }
        }
        set(user) {
            initialUserId = user?.userId
            if (_binding == null) return

            if (user == null) {
                activity?.runOnUiThread {
                    binding.fragmentSpinner.setSelection(0)
                }
                return
            }

            val adapter = binding.fragmentSpinner.adapter as UserAdapter
            for (i in 0 until adapter.count) {
                if (equals(user, adapter.getItem(i))) {
                    activity?.runOnUiThread {
                        binding.fragmentSpinner.setSelection(i)
                    }
                    break
                }
            }
        }

    val selectedUserPass: String
        get() {
            if (_binding == null) return ""

            val temp = binding.fragmentSpinner.selectedItem
            return when {
                temp != null -> {
                    val r = temp as User
                    when {
                        r.userId <= 0 -> ""
                        else -> r.password ?: ""
                    }
                }

                else -> ""
            }
        }

    var selectedUserId: Int?
        get() {
            if (_binding == null) return null

            val temp = binding.fragmentSpinner.selectedItem
            return when {
                temp != null -> {
                    val r = temp as User
                    when (r.userId) {
                        0L -> null
                        else -> r.userId.toInt()
                    }
                }

                else -> null
            }
        }
        set(id) {
            initialUserId = id?.toLong()
            if (_binding == null) return

            if (id == null || id <= 0) {
                if (binding.fragmentSpinner.adapter != null) {
                    activity?.runOnUiThread {
                        binding.fragmentSpinner.setSelection(0)
                    }
                }
                return
            }

            if (binding.fragmentSpinner.adapter != null) {
                val adapter = binding.fragmentSpinner.adapter as UserAdapter
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i) == null) {
                        continue
                    }

                    if (equals(id, adapter.getItem(i)!!.userId)) {
                        activity?.runOnUiThread {
                            binding.fragmentSpinner.setSelection(i)
                        }
                        break
                    }
                }
            }
        }

    val count: Int
        get() = when {
            _binding == null -> 0
            binding.fragmentSpinner.adapter != null -> binding.fragmentSpinner.adapter.count
            else -> 0
        }

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
            selectedUserId = initialUserId?.toInt()
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
            throw ClassCastException(activity.toString() + " must implement OnItemSelectedListener")
        }

        try {
            mListener = activity as OnSpinnerFillListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement OnSpinnerFillListener")
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
                    password = "81dc9bdb52d04dc20036dbd8313ed055"
                )
            )
        }

        UserCoroutines.get {
            allUser = it
            fillAdapter()
        }
    }

    private fun fillAdapter() {
        allUser!!.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        val spinnerArrayAdapter = UserAdapter(
            resource = R.layout.custom_spinner_dropdown_item, user = allUser!!
        )

        activity?.runOnUiThread {
            binding.fragmentSpinner.adapter = spinnerArrayAdapter
            if (oldPos >= 0) {
                binding.fragmentSpinner.setSelection(oldPos)
            }
            spinnerArrayAdapter.notifyDataSetChanged()
        }

        while (binding.fragmentSpinner.adapter == null) {
            // Horrible wait for a full load
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
