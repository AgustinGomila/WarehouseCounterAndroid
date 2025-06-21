package com.dacosys.warehouseCounter.ui.panel

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.*
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.dacosys.imageControl.ui.utils.ParcelUtils.serializable
import com.dacosys.imageControl.ui.utils.ParcelUtils.serializableMap
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R

/**
 * Maneja paneles expandibles/colapsables con:
 * - Soporte para teclado y cambios de orientación
 * - Animaciones fluidas
 * - Gestión de estados persistente
 *
 * Requiere que las subclases definan:
 * - Los layouts principales
 * - Textos para botones (no null si se usan)
 */
class PanelController(
    private val context: Context,
    private val config: PanelConfiguration,
    private val layout: ConstraintLayout,
    private val topButton: Button?,
    private val bottomButton: Button?
) {
    interface PanelConfiguration {
        val stateConfig: PanelStateConfiguration
        val layoutConfig: PanelLayoutConfiguration
        val textConfig: PanelTextConfiguration
        val animationConfig: PanelAnimationConfiguration
    }

    data class PanelStateConfiguration(
        val initialPanelTopState: PanelState? = null,
        val initialPanelBottomState: PanelState? = null,
    )

    data class PanelLayoutConfiguration(
        val topPanelExpandedLayout: Int? = null,
        val bottomPanelExpandedLayout: Int? = null,
        val allPanelsExpandedLayout: Int? = null,
        val allPanelsCollapsedLayout: Int? = null,
    )

    data class PanelTextConfiguration(
        val topButtonText: Int? = null,
        val bottomButtonText: Int? = null,
    )

    data class PanelAnimationConfiguration(
        val preImeHideAnimation: (() -> Unit)? = null,
        val postImeHideAnimation: (() -> Unit)? = null,
        val preImeShowAnimation: (() -> Unit)? = null,
        val postImeShowAnimation: (() -> Unit)? = null,
        val postTopPanelAnimation: (() -> Unit)? = null,
        val postBottomPanelAnimation: (() -> Unit)? = null,
    )

    var lastOrientation = Configuration.ORIENTATION_UNDEFINED
    var panelTopState: PanelState = PanelState.COLLAPSED
    var panelBottomState: PanelState = PanelState.COLLAPSED

    var isKeyboardVisible: Boolean = false
    private var pendingPanelStates = mutableMapOf<PanelType, PanelState>()

    /**
     * TOP/BOTTOM: Coll/Coll - Coll/Exp - Exp/Coll - Exp/Exp
     * IDX         0           1          2          3
     * BIN         00          01         10         11
     */
    private fun allLayouts(): List<Int?> = listOf(
        config.layoutConfig.allPanelsCollapsedLayout,
        config.layoutConfig.bottomPanelExpandedLayout,
        config.layoutConfig.topPanelExpandedLayout,
        config.layoutConfig.allPanelsExpandedLayout,
    )

    companion object {
        private const val KEY_PANEL_TOP_STATE = "TOP_STATE"
        private const val KEY_PANEL_BOTTOM_STATE = "BOTTOM_STATE"
        private const val KEY_PENDING_STATES = "PENDING_STATES"
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(KEY_PANEL_TOP_STATE, panelTopState)
        outState.putSerializable(KEY_PANEL_BOTTOM_STATE, panelBottomState)
        outState.putSerializable(KEY_PENDING_STATES, HashMap(pendingPanelStates))
    }

    fun onCreate(savedInstanceState: Bundle?) {
        lastOrientation = context.resources.configuration.orientation
        savedInstanceState?.let { loadBundle(it) } ?: {
            panelTopState = config.stateConfig.initialPanelTopState ?: PanelState.COLLAPSED
            panelBottomState = config.stateConfig.initialPanelBottomState ?: PanelState.COLLAPSED
        }
        setupPanelAnimations()
        handlePanelState(panelTopState, panelBottomState)
    }

    private fun loadBundle(bundle: Bundle) {
        panelTopState = bundle.serializable<PanelState>(KEY_PANEL_TOP_STATE) ?: PanelState.COLLAPSED
        panelBottomState = bundle.serializable<PanelState>(KEY_PANEL_BOTTOM_STATE) ?: PanelState.COLLAPSED
        pendingPanelStates = bundle.serializableMap<PanelType, PanelState>(KEY_PENDING_STATES)
            ?: mutableMapOf()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation != lastOrientation) {
            lastOrientation = newConfig.orientation
            handlePanelState(panelTopState, panelBottomState)
        }
    }

    fun setupWindowInsetsAnimation(window: Window, fitsSystem: Boolean = false) {
        WindowCompat.setDecorFitsSystemWindows(window, fitsSystem)
        if (fitsSystem) return

        setupInsetsListener(layout)
        setupInsetsAnimation(layout)
    }

    private fun setupInsetsListener(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val imeVisible = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0
            if (imeVisible == isKeyboardVisible) {
                updateBottomPadding(view, insets)
            }
            insets
        }
    }

    private fun setupInsetsAnimation(rootView: View) {
        ViewCompat.setWindowInsetsAnimationCallback(
            rootView,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    if (animation.isImeAnimation()) {
                        handlePreImeAnimationActions()
                    }
                    return super.onStart(animation, bounds)
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    if (animation.isImeAnimation()) {
                        handlePostImeAnimationActions()
                    }
                    super.onEnd(animation)
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    isKeyboardVisible = ime.bottom > 0

                    updateBottomPadding(rootView, insets)
                    return insets
                }
            })
    }

    private fun updateBottomPadding(view: View, insets: WindowInsetsCompat) {
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val bottomInset = if (ime.bottom == 0) systemBars.bottom else ime.bottom

        view.updatePadding(
            left = systemBars.left,
            top = systemBars.top,
            right = systemBars.right,
            bottom = bottomInset
        )

        logDebug("Ime size ${ime.bottom}")
    }

    private fun WindowInsetsAnimationCompat.isImeAnimation() =
        typeMask and WindowInsetsCompat.Type.ime() != 0

    private fun handlePreImeAnimationActions() {
        if (!isKeyboardVisible) handlePreShowImeAnimationActions()
        else handlePreHideImeAnimationActions()
    }

    private fun handlePreHideImeAnimationActions() {
        config.animationConfig.preImeHideAnimation?.invoke()
    }

    private fun handlePreShowImeAnimationActions() {
        config.animationConfig.preImeShowAnimation?.invoke()
    }

    private fun handlePostImeAnimationActions() {
        if (isKeyboardVisible) handlePostShowImeAnimationActions()
        else handlePostHideImeAnimationActions()
    }

    private fun handlePostHideImeAnimationActions() {
        pendingPanelStates.keys.forEach { panelType ->
            pendingPanelStates[panelType]?.let { state ->
                handlePanelState(panelType, state)
                pendingPanelStates.remove(panelType)
            }
        }
        config.animationConfig.postImeHideAnimation?.invoke()
    }

    private fun handlePostShowImeAnimationActions() {
        pendingPanelStates.clear()
        config.animationConfig.postImeShowAnimation?.invoke()
    }

    /**
     * Si el teclado es visible y estamos por expandir alguno de los paneles
     * primero permitimos que el teclado se oculte por completo para realizar la
     * acción de expansión del panel.
     */
    private fun shouldDeferPanelAction(panelType: PanelType): Boolean {
        return isKeyboardVisible && when (panelType) {
            PanelType.TOP -> isPanelCollapsed(PanelType.TOP)
            PanelType.BOTTOM -> isPanelCollapsed(PanelType.BOTTOM)
        }
    }

    private fun setupPanelAnimations() {
        topButton?.let {
            setupPanelButton(it, PanelType.TOP)
        }
        bottomButton?.let {
            setupPanelButton(it, PanelType.BOTTOM)
        }
    }

    private fun setupPanelButton(button: Button, panelType: PanelType) {
        button.setOnClickListener {
            if (shouldDeferPanelAction(panelType)) {
                pendingPanelStates[panelType] = getToggleState(panelType)
                return@setOnClickListener
            }
            togglePanel(panelType)
        }
    }

    private fun togglePanel(panelType: PanelType) {
        applyPanelState(panelType, getToggleState(panelType))
    }

    fun applyPanelState(panelType: PanelType, state: PanelState) {
        val layoutResource = determineLayout(panelType, state)
        val constraintSet = loadConstraintSet(layoutResource)

        applyConstraintsWithAnimation(constraintSet) {
            logDebug("Apply state $state to panel $panelType")
            postPanelAnimation(panelType)
        }

        updatePanelStateAndText(panelType, state)
    }

    private fun determineLayout(panelType: PanelType, state: PanelState): Int {
        val otherPanelState = when (panelType) {
            PanelType.TOP -> panelBottomState
            PanelType.BOTTOM -> panelTopState
        }
        val otherPanelVisible = when (panelType) {
            PanelType.TOP -> bottomButton != null
            PanelType.BOTTOM -> topButton != null
        }
        return getLayout(
            panelType = panelType,
            state = state,
            otherPanelVisible = otherPanelVisible,
            otherPanelState = otherPanelState
        )
    }

    private val constraintSetCache = mutableMapOf<Int, ConstraintSet>()

    private fun loadConstraintSet(layoutResource: Int): ConstraintSet {
        return constraintSetCache.getOrPut(layoutResource) {
            ConstraintSet().apply { load(context, layoutResource) }
        }
    }

    private fun postPanelAnimation(panelType: PanelType) {
        when (panelType) {
            PanelType.TOP -> config.animationConfig.postTopPanelAnimation?.invoke()
            PanelType.BOTTOM -> config.animationConfig.postBottomPanelAnimation?.invoke()
        }
    }

    private fun applyConstraintsWithAnimation(constraintSet: ConstraintSet, onEnd: () -> Unit) {
        val transition = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(Fade())
            duration = 300
            interpolator = OvershootInterpolator(0.7f)
            addListener(createTransitionListener(onEnd))
        }

        TransitionManager.beginDelayedTransition(layout, transition)
        constraintSet.applyTo(layout)
    }

    private fun createTransitionListener(onEnd: () -> Unit) =
        object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) = onEnd()
            override fun onTransitionResume(transition: Transition) {}
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionCancel(transition: Transition) {}
            override fun onTransitionStart(transition: Transition) {}
        }

    private fun updateButtonText(panelType: PanelType, state: PanelState) {
        val resId = when {
            state == PanelState.EXPANDED -> R.string.collapse_panel
            else -> when (panelType) {
                PanelType.TOP -> requireNotNull(config.textConfig.topButtonText) { "Top button text must be set" }
                PanelType.BOTTOM -> requireNotNull(config.textConfig.bottomButtonText) { "Bottom button text must be set" }
            }
        }

        val button = when (panelType) {
            PanelType.TOP -> topButton
            PanelType.BOTTOM -> bottomButton
        }
        button?.text = context.getString(resId)
    }

    fun handlePanelState(topState: PanelState, bottomState: PanelState) {
        // Force top panel state change
        if (topButton != null) {
            updatePanelStateAndText(PanelType.TOP, topState)
        }

        // Apply state to bottom panel
        applyPanelState(PanelType.BOTTOM, bottomState)
    }

    fun handlePanelState(panelType: PanelType, state: PanelState) {
        when (state) {
            PanelState.EXPANDED -> expandPanel(panelType)
            else -> collapsePanel(panelType)
        }
    }

    fun collapsePanel(panelType: PanelType) {
        if (isPanelExpanded(panelType)) {
            applyPanelState(panelType, PanelState.COLLAPSED)
        }
    }

    fun expandPanel(panelType: PanelType) {
        if (isPanelCollapsed(panelType)) {
            applyPanelState(panelType, PanelState.EXPANDED)
        }
    }

    private fun isPanelExpanded(panelType: PanelType) = when (panelType) {
        PanelType.TOP -> panelTopState == PanelState.EXPANDED
        PanelType.BOTTOM -> panelBottomState == PanelState.EXPANDED
    }

    private fun isPanelCollapsed(panelType: PanelType) = !isPanelExpanded(panelType)

    private fun updatePanelStateAndText(panelType: PanelType, state: PanelState) {
        updatePanelState(panelType, state)
        updateButtonText(panelType, state)
    }

    private fun updatePanelState(panelType: PanelType, state: PanelState) {
        when (panelType) {
            PanelType.TOP -> panelTopState = state
            PanelType.BOTTOM -> panelBottomState = state
        }
    }

    private fun getToggleState(panelType: PanelType): PanelState {
        return if (isPanelExpanded(panelType)) PanelState.COLLAPSED
        else PanelState.EXPANDED
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, message)
    }

    private fun getLayout(
        panelType: PanelType,
        state: PanelState,
        otherPanelVisible: Boolean,
        otherPanelState: PanelState
    ): Int {
        val effectiveOtherState =
            if (otherPanelVisible) otherPanelState
            else state

        val (topState, bottomState) =
            when (panelType) {
                PanelType.TOP -> state to effectiveOtherState
                PanelType.BOTTOM -> effectiveOtherState to state
            }

        return allLayouts()[topState.ordinal * 2 + bottomState.ordinal]!!
    }
}