package com.dacosys.warehouseCounter.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

/**
 * @author: liuzhenfeng
 * @function: Al consumir eventos táctiles, controle el cambio de la distancia de
 *            deslizamiento del dedo y establezca el tamaño de la ventana flotante
 * @date: 2019-08-05  09:55
 */
class ScaleImageView(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {

    private var touchDownX = 0f
    private var touchDownY = 0f

    var onScaledListener: OnScaledListener? = null

    interface OnScaledListener {
        fun onScaled(x: Float, y: Float, event: MotionEvent)
        fun onScaledEnded()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(null)

        // Bloquea la intercepción de eventos de la ventana flotante, solo consumida por sí misma
        parent?.requestDisallowInterceptTouchEvent(true)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
            }

            MotionEvent.ACTION_MOVE ->
                onScaledListener?.onScaled(
                    event.x - touchDownX,
                    event.y - touchDownY, event
                )

            MotionEvent.ACTION_UP ->
                onScaledListener?.onScaledEnded()
        }
        return true
    }
}
