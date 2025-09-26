package com.example.warehouseCounter.scanners.deviceLifecycle.event

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    FragmentEvent.Companion.PRE_ATTACH,
    FragmentEvent.Companion.ATTACH,
    FragmentEvent.Companion.CREATE,
    FragmentEvent.Companion.ACTIVITY_CREATE,
    FragmentEvent.Companion.PRE_CREATE,
    FragmentEvent.Companion.VIEW_CREATE,
    FragmentEvent.Companion.START,
    FragmentEvent.Companion.RESUME,
    FragmentEvent.Companion.PAUSE,
    FragmentEvent.Companion.STOP,
    FragmentEvent.Companion.SAVE_INSTANCE_STATE,
    FragmentEvent.Companion.DESTROY,
    FragmentEvent.Companion.VIEW_DESTROY,
    FragmentEvent.Companion.DETACH
)
annotation class FragmentEvent {
    companion object {
        const val PRE_ATTACH = "PRE_ATTACH"
        const val ATTACH = "ATTACH"
        const val ACTIVITY_CREATE = "ACTIVITY_CREATE"
        const val CREATE = "CREATE"
        const val PRE_CREATE = "PRE_CREATE"
        const val VIEW_CREATE = "VIEW_CREATE"
        const val START = "START"
        const val RESUME = "RESUME"
        const val PAUSE = "PAUSE"
        const val STOP = "STOP"
        const val SAVE_INSTANCE_STATE = "SAVE_INSTANCE_STATE"
        const val DESTROY = "DESTROY"
        const val VIEW_DESTROY = "VIEW_DESTROY"
        const val DETACH = "DETACH"
    }
}