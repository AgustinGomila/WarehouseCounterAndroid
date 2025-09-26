package com.example.warehouseCounter.scanners.jotter.event

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    ActivityEvent.Companion.CREATE,
    ActivityEvent.Companion.START,
    ActivityEvent.Companion.RESUME,
    ActivityEvent.Companion.PAUSE,
    ActivityEvent.Companion.STOP,
    ActivityEvent.Companion.SAVE_INSTANCE_STATE,
    ActivityEvent.Companion.DESTROY
)
annotation class ActivityEvent {
    companion object {
        const val CREATE = "CREATE"
        const val START = "START"
        const val RESUME = "RESUME"
        const val PAUSE = "PAUSE"
        const val STOP = "STOP"
        const val SAVE_INSTANCE_STATE = "SAVE_INSTANCE_STATE"
        const val DESTROY = "DESTROY"

        val scannerListenerEvents = listOf(
            CREATE,
            RESUME,
            PAUSE,
            DESTROY
        )
    }
}