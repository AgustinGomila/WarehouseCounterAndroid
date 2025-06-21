package com.dacosys.warehouseCounter.scanners.jotter.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.dacosys.warehouseCounter.scanners.jotter.Jotter
import com.dacosys.warehouseCounter.scanners.jotter.event.ActivityEvent
import com.dacosys.warehouseCounter.scanners.jotter.event.FragmentEvent
import java.lang.ref.WeakReference

internal object LifecycleListener {

    internal fun register(
        application: Application,
        listener: Jotter.Listener?,
        activityFilter: List<String>,
        fragmentFilter: List<String>
    ) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var weakReference: WeakReference<FragmentManager.FragmentLifecycleCallbacks?>? =
                null

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                weakReference = WeakReference(
                    registerToFragmentLifecycle(
                        activity = activity,
                        listener = listener,
                        fragmentFilter = fragmentFilter
                    )
                )
                Logger.debug("onActivityCreated >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.CREATE))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.CREATE,
                        bundle = savedInstanceState
                    )
            }

            override fun onActivityStarted(activity: Activity) {
                Logger.debug("onActivityStarted >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.START))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.START
                    )
            }

            override fun onActivityResumed(activity: Activity) {
                Logger.debug("onActivityResumed >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.RESUME))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.RESUME
                    )
            }

            override fun onActivityPaused(activity: Activity) {
                Logger.debug("onActivityPaused >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.PAUSE))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.PAUSE
                    )
            }

            override fun onActivityStopped(activity: Activity) {
                Logger.debug("onActivityStopped >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.STOP))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.STOP
                    )
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Logger.debug("onActivitySaveInstanceState >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.SAVE_INSTANCE_STATE))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.SAVE_INSTANCE_STATE,
                        bundle = outState
                    )
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (weakReference != null) {
                    unregisterToFragmentLifecycle(activity, weakReference?.get())
                    weakReference?.clear()
                }
                Logger.debug("onActivityDestroyed >>> $activity")
                if (activityFilter.contains(ActivityEvent.Companion.DESTROY))
                    listener?.onReceiveActivityEvent(
                        activity = activity,
                        event = ActivityEvent.Companion.DESTROY
                    )
            }
        })
    }

    private fun unregisterToFragmentLifecycle(
        activity: Activity,
        callbacks: FragmentManager.FragmentLifecycleCallbacks?
    ) {
        if (callbacks != null && activity is FragmentActivity) {
            val supportFragmentManager = activity.supportFragmentManager
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(callbacks)
        }
    }

    private fun registerToFragmentLifecycle(
        activity: Activity,
        listener: Jotter.Listener?,
        fragmentFilter: List<String>
    ): FragmentManager.FragmentLifecycleCallbacks? {
        if (activity is FragmentActivity) {
            val supportFragmentManager = activity.supportFragmentManager
            val fragmentLifecycleCallbacks: FragmentManager.FragmentLifecycleCallbacks =
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentPreAttached(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        context: Context
                    ) {
                        super.onFragmentPreAttached(fragmentManager, fragment, context)
                        Logger.debug("onFragmentPreAttached >>> $fragment >>> $context")
                        if (fragmentFilter.contains(FragmentEvent.Companion.PRE_ATTACH))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                context = context,
                                event = FragmentEvent.Companion.PRE_ATTACH
                            )
                    }

                    override fun onFragmentAttached(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        context: Context
                    ) {
                        super.onFragmentAttached(fragmentManager, fragment, context)
                        Logger.debug("onFragmentAttached >>> $fragment >>> $context")
                        if (fragmentFilter.contains(FragmentEvent.Companion.ATTACH))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                context = context,
                                event = FragmentEvent.Companion.ATTACH
                            )
                    }

                    override fun onFragmentCreated(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentCreated(fragmentManager, fragment, savedInstanceState)
                        Logger.debug("onFragmentCreated >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.CREATE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.CREATE,
                                bundle = savedInstanceState
                            )
                    }

                    override fun onFragmentActivityCreated(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentActivityCreated(
                            fragmentManager,
                            fragment,
                            savedInstanceState
                        )
                        Logger.debug("onFragmentActivityCreated >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.ACTIVITY_CREATE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.ACTIVITY_CREATE,
                                bundle = savedInstanceState
                            )
                    }

                    override fun onFragmentPreCreated(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentPreCreated(fragmentManager, fragment, savedInstanceState)
                        Logger.debug("onFragmentActivityCreated >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.PRE_CREATE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.PRE_CREATE,
                                bundle = savedInstanceState
                            )
                    }

                    override fun onFragmentViewCreated(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        view: View,
                        savedInstanceState: Bundle?
                    ) {
                        super.onFragmentViewCreated(
                            fragmentManager,
                            fragment,
                            view,
                            savedInstanceState
                        )
                        Logger.debug("onFragmentViewCreated >>> $fragment >>> $view")
                        if (fragmentFilter.contains(FragmentEvent.Companion.VIEW_CREATE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.VIEW_CREATE,
                                bundle = savedInstanceState
                            )
                    }

                    override fun onFragmentStarted(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentStarted(fragmentManager, fragment)
                        Logger.debug("onFragmentStarted >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.START))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.START
                            )
                    }

                    override fun onFragmentResumed(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentResumed(fragmentManager, fragment)
                        Logger.debug("onFragmentResumed >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.RESUME))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.RESUME
                            )
                    }

                    override fun onFragmentPaused(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentPaused(fragmentManager, fragment)
                        Logger.debug("onFragmentPaused >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.PAUSE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.PAUSE
                            )
                    }

                    override fun onFragmentStopped(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentStopped(fragmentManager, fragment)
                        Logger.debug("onFragmentStopped >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.STOP))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.STOP
                            )
                    }

                    override fun onFragmentSaveInstanceState(
                        fragmentManager: FragmentManager,
                        fragment: Fragment,
                        outState: Bundle
                    ) {
                        super.onFragmentSaveInstanceState(fragmentManager, fragment, outState)
                        Logger.debug("onFragmentSaveInstanceState >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.SAVE_INSTANCE_STATE))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.SAVE_INSTANCE_STATE,
                                bundle = outState
                            )
                    }

                    override fun onFragmentViewDestroyed(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentViewDestroyed(fragmentManager, fragment)
                        Logger.debug("onFragmentViewDestroyed >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.VIEW_DESTROY))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.VIEW_DESTROY
                            )
                    }

                    override fun onFragmentDestroyed(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentDestroyed(fragmentManager, fragment)
                        Logger.debug("onFragmentDestroyed >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.DESTROY))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.DESTROY
                            )
                    }

                    override fun onFragmentDetached(
                        fragmentManager: FragmentManager,
                        fragment: Fragment
                    ) {
                        super.onFragmentDetached(fragmentManager, fragment)
                        Logger.debug("onFragmentDetached >>> $fragment")
                        if (fragmentFilter.contains(FragmentEvent.Companion.DETACH))
                            listener?.onReceiveFragmentEvent(
                                fragment = fragment,
                                event = FragmentEvent.Companion.DETACH
                            )
                    }
                }
            supportFragmentManager.registerFragmentLifecycleCallbacks(
                fragmentLifecycleCallbacks,
                true
            )
            return fragmentLifecycleCallbacks
        }
        return null
    }
}