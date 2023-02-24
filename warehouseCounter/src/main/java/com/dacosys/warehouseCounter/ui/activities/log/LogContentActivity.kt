package com.dacosys.warehouseCounter.ui.activities.log

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.adapter.log.LogContentAdapter
import com.dacosys.warehouseCounter.databinding.LogContentActivityBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.moshi.log.Log
import com.dacosys.warehouseCounter.moshi.log.LogContent

class LogContentActivity :
    AppCompatActivity(),
    SwipeRefreshLayout.OnRefreshListener {
    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshLogContent.isRefreshing = false
            }
        }, 100)
    }

    private var logContAdapter: LogContentAdapter? = null
    private var lastSelected: LogContent? = null
    private var firstVisiblePos: Int? = null

    private var log: Log? = null
    private var logContent: ArrayList<LogContent> = ArrayList()

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("title", title.toString())
        savedInstanceState.putParcelable("log", log)

        if (logContAdapter != null) {
            savedInstanceState.putParcelable(
                "lastSelected",
                (logContAdapter ?: return).currentLogCont()
            )
            savedInstanceState.putInt(
                "firstVisiblePos",
                (logContAdapter ?: return).firstVisiblePos()
            )
            savedInstanceState.putParcelableArrayList("logContent", logContAdapter?.getAll())
        }
    }

    private lateinit var binding: LogContentActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = LogContentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tempTitle = getString(R.string.count_log)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t1 = savedInstanceState.getString("title")
            if (t1 != null && t1.isNotEmpty()) tempTitle = t1
            // endregion

            logContent =
                savedInstanceState.getParcelableArrayList<LogContent>("logContent") as ArrayList<LogContent>
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos") else -1
        } else {
            // Inicializar la actividad

            // region EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString("title")
                if (t1 != null && t1.isNotEmpty()) tempTitle = t1

                log = extras.getParcelable("log")
                logContent =
                    extras.getParcelableArrayList<LogContent>("logContent") as ArrayList<LogContent>
            }
        }

        title = tempTitle

        binding.swipeRefreshLogContent.setOnRefreshListener(this)
        binding.swipeRefreshLogContent.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        fillAdapter(logContent)
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            run {
                binding.swipeRefreshLogContent.isRefreshing = show
            }
        }, 20)
    }

    private fun fillAdapter(t: ArrayList<LogContent>) {
        logContent = t

        showProgressBar(true)

        runOnUiThread {
            try {
                if (logContAdapter != null) {
                    lastSelected = (logContAdapter ?: return@runOnUiThread).currentLogCont()
                    firstVisiblePos = (logContAdapter ?: return@runOnUiThread).firstVisiblePos()
                }

                logContAdapter = LogContentAdapter(
                    activity = this,
                    resource = R.layout.log_content_row,
                    logContArray = logContent,
                    listView = binding.logContListView
                )

                while (binding.logContListView.adapter == null) {
                    // Horrible wait for full load
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    run {
                        logContAdapter?.setSelectItemAndScrollPos(lastSelected, firstVisiblePos)
                    }
                }, 20)
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                showProgressBar(false)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.home || id == android.R.id.home) {
            onBackPressed()
            true
        } else super.onOptionsItemSelected(item)
    }
}