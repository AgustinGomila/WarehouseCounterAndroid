package com.example.warehouseCounter.scanners.jotter

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.warehouseCounter.scanners.jotter.event.ActivityEvent
import com.example.warehouseCounter.scanners.jotter.event.FragmentEvent
import com.example.warehouseCounter.scanners.jotter.util.LifecycleListener
import com.example.warehouseCounter.scanners.jotter.util.Logger

class Jotter private constructor(private val builder: Builder) {

    private var listener: Listener? = null
    private var isLogEnabled: Boolean = false
    private var tag: String = TAG
    private var activityEvents: List<String> = listOf()
    private var fragmentEvents: List<String> = listOf()
    private var application: Application? = null

    init {
        application = builder.application
        isLogEnabled = builder.isLogEnabled
        listener = builder.listener
        tag = builder.tag
        activityEvents = builder.activityEvents
        fragmentEvents = builder.fragmentEvents

        Logger.let {
            it.logEnabled = isLogEnabled
            it.tag = tag
        }

        INSTANCE = this
    }

    class Builder(internal val application: Application) {

        internal var isLogEnabled: Boolean = false
        internal var listener: Listener? = null
        internal var tag: String = TAG
        internal var activityEvents = listOf(
            ActivityEvent.Companion.CREATE,
            ActivityEvent.Companion.START,
            ActivityEvent.Companion.RESUME,
            ActivityEvent.Companion.PAUSE,
            ActivityEvent.Companion.STOP,
            ActivityEvent.Companion.SAVE_INSTANCE_STATE,
            ActivityEvent.Companion.DESTROY
        )
        internal var fragmentEvents = listOf(
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

        fun setLogEnable(isLogEnabled: Boolean): Builder {
            this.isLogEnabled = isLogEnabled
            return this
        }

        @Suppress("unused")
        fun setCustomTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun setLifecycleListener(listener: Listener): Builder {
            this.listener = listener
            return this
        }

        fun setActivityEventFilter(events: List<String>): Builder {
            this.activityEvents = events
            return this
        }

        @Suppress("unused")
        fun setFragmentEventFilter(events: List<String>): Builder {
            this.fragmentEvents = events
            return this
        }

        fun build() = Jotter(this)
    }

    fun startListening() {
        LifecycleListener.register(
            application = this.builder.application,
            listener = this.listener,
            activityFilter = this.activityEvents,
            fragmentFilter = this.fragmentEvents
        )
        if (this.listener == null) {
            Logger.debug(Constant.LISTENER_MESSAGE)
        }
    }

    interface Listener {
        fun onReceiveActivityEvent(
            activity: Activity,
            @ActivityEvent event: String,
            bundle: Bundle? = null
        )

        fun onReceiveFragmentEvent(
            fragment: Fragment,
            context: Context? = null,
            @FragmentEvent event: String,
            bundle: Bundle? = null
        )
    }

    companion object {
        @JvmStatic
        @Volatile
        private var INSTANCE: Jotter? = null

        @Synchronized
        fun getInstance(): Jotter = INSTANCE
            ?: throw RuntimeException(Constant.INSTANCE_MESSAGE)

        @Synchronized
        @Suppress("unused")
        fun getDefaultInstance(application: Application): Jotter {
            if (INSTANCE == null) {
                INSTANCE = Builder(application).build()
            }
            return INSTANCE!!
        }

        private const val TAG = "Jotter"
    }

    private object Constant {
        const val INSTANCE_MESSAGE =
            "Jotter isn't initialized yet. Please use Jotter.Builder(application)!"
        const val LISTENER_MESSAGE =
            "Listener not found, you can't receive callbacks, please set it first via Builder or setLifecycleListener API!"
    }
}