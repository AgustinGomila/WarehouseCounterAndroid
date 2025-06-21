package com.dacosys.warehouseCounter.ui.panel

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.dacosys.warehouseCounter.ui.panel.PanelController.PanelConfiguration

abstract class BasePanelFragment : Fragment(), PanelConfiguration {
    private lateinit var panelController: PanelController

    abstract fun provideRootLayout(): ConstraintLayout
    abstract fun provideTopButton(): Button?
    abstract fun provideBottomButton(): Button?

    protected fun initPanelController(savedInstanceState: Bundle?, fitsSystemWindows: Boolean = false) {
        panelController = PanelController(
            context = requireContext(),
            config = this,
            layout = provideRootLayout(),
            topButton = provideTopButton(),
            bottomButton = provideBottomButton()
        )
        panelController.onCreate(savedInstanceState)
        panelController.setupWindowInsetsAnimation(requireActivity().window, fitsSystemWindows)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        panelController.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        panelController.onConfigurationChanged(newConfig)
    }
}