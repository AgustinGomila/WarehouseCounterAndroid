package com.dacosys.warehouseCounter.ui.panel

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.dacosys.warehouseCounter.ui.panel.PanelController.PanelConfiguration
import kotlinx.coroutines.launch

abstract class BasePanelActivity : AppCompatActivity(), PanelConfiguration {
    protected lateinit var panelController: PanelController

    abstract fun provideRootLayout(): ConstraintLayout
    abstract fun provideTopButton(): Button?
    abstract fun provideBottomButton(): Button?

    protected fun initPanelController(savedInstanceState: Bundle?) {
        panelController = PanelController(
            context = this,
            config = this,
            layout = provideRootLayout(),
            topButton = provideTopButton(),
            bottomButton = provideBottomButton()
        )
        panelController.onCreate(savedInstanceState)
        panelController.setupWindowInsetsAnimation(window)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        panelController.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        panelController.onConfigurationChanged(newConfig)
    }

    val lastOrientation: Int get() = panelController.lastOrientation
    val isKeyboardVisible: Boolean get() = panelController.isKeyboardVisible
    val panelBottomState: PanelState get() = panelController.panelBottomState

    fun handlePanelState(topState: PanelState, bottomState: PanelState) {
        lifecycleScope.launch {
            panelController.handlePanelState(topState, bottomState)
        }
    }

    fun handlePanelState(panelType: PanelType, state: PanelState) {
        lifecycleScope.launch {
            panelController.handlePanelState(panelType, state)
        }
    }
}