package com.dacosys.warehouseCounter.ui.fragments.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.databinding.HomeActivityButtonPage1Binding
import com.dacosys.warehouseCounter.databinding.HomeActivityButtonPage2Binding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.mainButton.MainButton
import com.dacosys.warehouseCounter.ui.activities.main.HomeActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Colors

/**
 * A simple [Fragment] subclass.
 * Use the [ButtonPageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ButtonPageFragment : Fragment() {
    interface ButtonClickedListener {
        fun onButtonClicked(button: Button)
    }

    private var allButton: ArrayList<MainButton>? = ArrayList()
    private var pageIndex = 0

    fun getButton(mainButton: MainButton): View? {
        val buttonCollection = getButtonCollection()
        for (b in buttonCollection) {
            if (b.tag == mainButton.id) {
                return b
            }
        }
        return null
    }

    fun setButtonSubText(mainButton: MainButton, subText: String) {
        val buttonCollection = getButtonCollection()
        for (b in buttonCollection) {
            if (b.tag == mainButton.id) {
                b.text =
                    String.format(
                        "%s%s(%s)",
                        mainButton.description,
                        Statics.lineSeparator,
                        subText
                    )
                break
            }
        }
    }

    private fun getButtonCollection(): ArrayList<Button> {
        val buttonCollection: ArrayList<Button> = ArrayList()
        buttonCollection.clear()

        if (pageIndex == 0) {
            buttonCollection.add(binding1.mainButton1)
            buttonCollection.add(binding1.mainButton2)
            buttonCollection.add(binding1.mainButton3)
            buttonCollection.add(binding1.mainButton4)
            buttonCollection.add(binding1.mainButton5)
            buttonCollection.add(binding1.mainButton6)
        } else {
            buttonCollection.add(binding2.mainButton1)
            buttonCollection.add(binding2.mainButton2)
            buttonCollection.add(binding2.mainButton3)
            buttonCollection.add(binding2.mainButton4)
            buttonCollection.add(binding2.mainButton5)
            buttonCollection.add(binding2.mainButton6)
        }

        return buttonCollection
    }

    private fun setupMainButton() {
        if (allButton == null) return

        val allButtonMain: ArrayList<MainButton> = ArrayList()
        var index = 0
        val buttonCollection = getButtonCollection()
        for (b in allButton!!) {
            if (b.visibility) {
                allButtonMain.add(b)

                // Si alcanza el máximo salir
                index++
                if (index == buttonCollection.size) {
                    break
                }
            }
        }

        for (i in buttonCollection.indices) {
            val b = buttonCollection[i]
            if (i < allButtonMain.size) {
                setupButton(b, allButtonMain[i])
            } else {
                b.visibility = View.GONE
            }
        }

        for (a in buttonCollection) {
            setupButton(a)
        }
    }

    private fun setupButton(b: Button, m: MainButton) {
        val backColor: Int = Colors.getBackColor(b)
        val textColor = Colors.getBestContrastColor("#" + Integer.toHexString(backColor))

        b.setTextColor(textColor)
        b.visibility = View.VISIBLE
        b.tag = m.id
        b.text = m.description
        b.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

        try {
            b.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(
                    requireContext(),
                    m.iconResource
                ),
                null,
                null,
                null
            )
        } catch (ex: Exception) {
            Log.e(this::class.java.simpleName, ex.message.toString())
        }

        b.compoundDrawables
            .filterNotNull()
            .forEach {
                it.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        ResourcesCompat.getColor(
                            WarehouseCounterApp.context.resources,
                            R.color.white,
                            null
                        ),
                        BlendModeCompat.SRC_IN
                    )
            }
        b.compoundDrawablePadding = 25
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pageIndex", pageIndex)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButton(button: Button) {
        button.setOnClickListener {
            try {
                if (activity is HomeActivity) {
                    (activity as HomeActivity).onButtonClicked(button)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                showSnackBar("${getString(R.string.exception_error)}: " + ex.message, SnackBarType.ERROR)
            }
        }
        button.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            touchButton(motionEvent, view as Button)
            return@OnTouchListener true
        })
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding1.root, text, snackBarType)
    }

    private fun touchButton(motionEvent: MotionEvent, button: Button) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                button.isPressed = false
                button.performClick()
            }

            MotionEvent.ACTION_DOWN -> {
                button.isPressed = true
            }

            MotionEvent.ACTION_CANCEL -> {
                button.isPressed = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            allButton = requireArguments().getParcelableArrayList(ARG_ALL_BUTTONS)
            pageIndex = requireArguments().getInt(ARG_PAGE_INDEX)
        }
    }

    private var _binding1: HomeActivityButtonPage1Binding? = null
    private var _binding2: HomeActivityButtonPage2Binding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding1 get() = _binding1!!
    private val binding2 get() = _binding2!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding1 = null
        _binding2 = null
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding1 = HomeActivityButtonPage1Binding.inflate(inflater, container, false)
        _binding2 = HomeActivityButtonPage2Binding.inflate(inflater, container, false)

        if (savedInstanceState != null) {
            pageIndex = savedInstanceState.getInt("pageIndex")
        }

        setupMainButton()

        return if (pageIndex == 0) binding1.root else binding2.root
    }

    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private const val ARG_ALL_BUTTONS = "allButtons"
        private const val ARG_PAGE_INDEX = "pageIndex"

        fun newInstance(
            mainButtonArray: ArrayList<MainButton>,
            pageIndex: Int,
        ): ButtonPageFragment {
            val fragment = ButtonPageFragment()

            val args = Bundle()
            args.putParcelableArrayList(ARG_ALL_BUTTONS, mainButtonArray)
            args.putInt(ARG_PAGE_INDEX, pageIndex)

            fragment.arguments = args
            return fragment
        }

        fun equals(a: Any?, b: Any?): Boolean {
            return a != null && a == b
        }
    }
}
