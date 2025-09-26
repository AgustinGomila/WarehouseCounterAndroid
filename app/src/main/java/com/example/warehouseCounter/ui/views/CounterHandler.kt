package com.example.warehouseCounter.ui.views

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

/**
 * Created by Noman on 11/8/2016.
 */
class CounterHandler private constructor(builder: Builder) {
    internal val handler = Handler(Looper.getMainLooper())

    private var startNumber = 0.toDouble()
    private val incrementalView: View?
    private val decrementalView: View?
    private var minRange = (-1).toDouble()
    private var maxRange = (-1).toDouble()
    private var counterStep: Long = 1
    private var counterDelay = 50 //millis

    private var isCycle = false
    private var autoIncrement = false
    private var autoDecrement = false

    private val listener: CounterListener?

    private val counterRunnable = object : Runnable {
        override fun run() {
            if (autoIncrement) {
                increment()
                handler.postDelayed(this, counterDelay.toLong())
            } else if (autoDecrement) {
                decrement()
                handler.postDelayed(this, counterDelay.toLong())
            }
        }
    }

    init {
        incrementalView = builder.incrementalView
        decrementalView = builder.decrementalView
        minRange = builder.minRange
        maxRange = builder.maxRange
        startNumber = builder.startNumber
        counterStep = builder.counterStep
        counterDelay = builder.counterDelay
        isCycle = builder.isCycle
        listener = builder.listener

        initDecrementalView()
        initIncrementalView()

        if (listener != null) {
            listener.onIncrement(incrementalView, startNumber)
            listener.onDecrement(decrementalView, startNumber)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initIncrementalView() {
        val view = incrementalView ?: return
        view.setOnClickListener { increment() }

        view.setOnLongClickListener {
            autoIncrement = true
            handler.postDelayed(counterRunnable, counterDelay.toLong())
            false
        }
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && autoIncrement) {
                autoIncrement = false
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initDecrementalView() {
        val view = decrementalView ?: return
        view.setOnClickListener { decrement() }

        view.setOnLongClickListener {
            autoDecrement = true
            handler.postDelayed(counterRunnable, counterDelay.toLong())
            false
        }
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && autoDecrement) {
                autoDecrement = false
            }
            false
        }
    }

    private fun increment() {
        var number = startNumber

        if (maxRange != (-1).toDouble()) {
            if (number + counterStep <= maxRange) {
                number += counterStep
            } else if (isCycle) {
                number = if (minRange == (-1).toDouble()) 0.toDouble() else minRange
            }
        } else {
            number += counterStep
        }

        if (number != startNumber && listener != null) {
            startNumber = number
            listener.onIncrement(incrementalView, startNumber)
        }
    }

    fun setValue(value: Double): Boolean {
        return if (minRange !in minRange..maxRange) {
            false
        } else {
            startNumber = value
            true
        }
    }

    private fun decrement() {
        var number = startNumber

        if (minRange != (-1).toDouble()) {
            if (number - counterStep >= minRange) {
                number -= counterStep
            } else if (isCycle) {
                number = if (maxRange == (-1).toDouble()) 0.toDouble() else maxRange
            }
        } else {
            number -= counterStep
        }

        if (number != startNumber && listener != null) {
            startNumber = number
            listener.onDecrement(decrementalView, startNumber)
        }
    }

    interface CounterListener {
        fun onIncrement(view: View?, number: Double)
        fun onDecrement(view: View?, number: Double)
    }

    class Builder {
        internal var incrementalView: View? = null
        internal var decrementalView: View? = null
        internal var minRange: Double = (-1).toDouble()
        internal var maxRange: Double = (-1).toDouble()
        internal var startNumber: Double = 0.toDouble()
        internal var counterStep: Long = 1
        internal var counterDelay = 50
        internal var isCycle: Boolean = false
        internal var listener: CounterListener? = null

        fun incrementalView(`val`: View): Builder {
            incrementalView = `val`
            return this
        }

        fun decrementalView(`val`: View): Builder {
            decrementalView = `val`
            return this
        }

        fun minRange(`val`: Double): Builder {
            minRange = `val`
            return this
        }

        fun maxRange(`val`: Double): Builder {
            maxRange = `val`
            return this
        }

        fun startNumber(`val`: Double): Builder {
            startNumber = `val`
            return this
        }

        fun counterStep(`val`: Long): Builder {
            counterStep = `val`
            return this
        }

        fun counterDelay(`val`: Int): Builder {
            counterDelay = `val`
            return this
        }

        fun isCycle(`val`: Boolean): Builder {
            isCycle = `val`
            return this
        }

        fun listener(`val`: CounterListener): Builder {
            listener = `val`
            return this
        }

        fun build(): CounterHandler {
            return CounterHandler(this)
        }
    }
}
