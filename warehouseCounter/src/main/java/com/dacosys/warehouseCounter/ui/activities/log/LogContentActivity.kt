package com.dacosys.warehouseCounter.ui.activities.log

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.databinding.LogContentActivityBinding
import com.dacosys.warehouseCounter.ktor.v2.dto.order.Log
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.room.dao.orderRequest.LogCoroutines
import com.dacosys.warehouseCounter.ui.adapter.log.LogAdapter
import com.dacosys.warehouseCounter.ui.utils.Screen

class LogContentActivity :
    AppCompatActivity(),
    SwipeRefreshLayout.OnRefreshListener {
    override fun onRefresh() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefresh.isRefreshing = false
        }, 100)
    }

    private var adapter: LogAdapter? = null
    private var lastSelected: Log? = null
    private var firstVisiblePos: Int? = null
    private var currentScrollPosition: Int = 0

    private var orderRequestId: Long = 0L
    private var completeList: ArrayList<Log> = ArrayList()

    // Se usa para saber si estamos en onStart luego de onCreate
    private var fillRequired = false

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString(ARG_TITLE, title.toString())
        savedInstanceState.putLong(ARG_ID, orderRequestId)

        if (adapter != null) {
            savedInstanceState.putParcelable("lastSelected", adapter?.currentItem())
            savedInstanceState.putInt("firstVisiblePos", adapter?.firstVisiblePos() ?: RecyclerView.NO_POSITION)
            savedInstanceState.putInt("currentScrollPosition", currentScrollPosition)
        }
    }

    private lateinit var binding: LogContentActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = LogContentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                currentScrollPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        })

        // Para el llenado en el onStart siguiente de onCreate
        fillRequired = true

        var tempTitle = getString(R.string.count_log)

        if (savedInstanceState != null) {
            // region Recuperar el título de la ventana
            val t1 = savedInstanceState.getString(ARG_TITLE)
            if (!t1.isNullOrEmpty()) tempTitle = t1
            // endregion

            orderRequestId = savedInstanceState.getLong(ARG_ID)
            lastSelected = savedInstanceState.getParcelable("lastSelected")
            firstVisiblePos =
                if (savedInstanceState.containsKey("firstVisiblePos")) savedInstanceState.getInt("firstVisiblePos")
                else -1
            currentScrollPosition = savedInstanceState.getInt("currentScrollPosition")
        } else {
            // Inicializar la actividad

            // region EXTRAS: Parámetros que recibe la actividad
            val extras = intent.extras
            if (extras != null) {
                val t1 = extras.getString(ARG_TITLE)
                if (!t1.isNullOrEmpty()) tempTitle = t1
                orderRequestId = extras.getLong(ARG_ID)
            }
        }

        title = tempTitle

        binding.swipeRefresh.setOnRefreshListener(this)
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    override fun onStart() {
        super.onStart()

        if (fillRequired) {
            fillRequired = false

            loadLogs()
        }
    }

    private fun loadLogs() {
        LogCoroutines.getByOrderId(
            orderId = orderRequestId,
            onResult = {
                completeList = it
                fillAdapter(completeList)
            }
        )
    }

    private fun showProgressBar(show: Boolean) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.swipeRefresh.isRefreshing = show
        }, 20)
    }

    private fun fillAdapter(t: ArrayList<Log>) {
        completeList = t

        showProgressBar(true)

        runOnUiThread {
            try {
                if (adapter != null) {
                    // Si el adapter es NULL es porque aún no fue creado.
                    // Por lo tanto, puede ser que los valores de [lastSelected]
                    // sean valores guardados de la instancia anterior y queremos preservarlos.
                    lastSelected = adapter?.currentItem()
                }

                adapter = LogAdapter(
                    recyclerView = binding.recyclerView,
                    fullList = completeList
                )

                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = adapter

                while (binding.recyclerView.adapter == null) {
                    // Horrible wait for a full load
                }

                // Estas variables locales evitar posteriores cambios de estado.
                val ls = lastSelected
                val cs = currentScrollPosition
                Handler(Looper.getMainLooper()).postDelayed({
                    adapter?.selectItem(ls, false)
                    adapter?.scrollToPos(cs, true)
                }, 200)
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

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_ID = "id"
    }
}