package com.dacosys.warehouseCounter.ui.activities.print

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.databinding.PrintLabelActivityBinding
import com.dacosys.warehouseCounter.ui.activities.item.ItemSelectActivity
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.setScreenRotation
import com.dacosys.warehouseCounter.ui.utils.Screen.Companion.setupUI

class PrintLabelActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()

        rejectNewInstances = false
    }

    private var rejectNewInstances = false

    private lateinit var binding: PrintLabelActivityBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setScreenRotation(this)
        binding = PrintLabelActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.items_and_location_catalog)

        binding.itemButton.setOnClickListener {
            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(context, ItemSelectActivity::class.java)
                intent.putExtra(ItemSelectActivity.ARG_TITLE, getString(R.string.print_code))
                intent.putExtra(ItemSelectActivity.ARG_MULTI_SELECT, true)
                startActivity(intent)
            }
        }

        binding.locationButton.setOnClickListener {
            if (!rejectNewInstances) {
                // rejectNewInstances = true

                // val intent = Intent(baseContext, WarehouseAreaPrintLabelActivity::class.java)
                // intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                // intent.putExtra("multiSelect", true)
                // startActivity(intent)
            }
        }

        setupUI(binding.root, this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.home, android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }
}
