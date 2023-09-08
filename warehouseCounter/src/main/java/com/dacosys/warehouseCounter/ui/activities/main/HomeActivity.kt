package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.text.format.DateFormat
import android.view.*
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.dacosys.imageControl.network.upload.UploadImagesProgress
import com.dacosys.warehouseCounter.BuildConfig
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.json
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sync
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.syncViewModel
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.writeJsonToFile
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.writeToFile
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.CreateOrder
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.ViewOrder
import com.dacosys.warehouseCounter.data.ktor.v2.sync.SyncViewModel
import com.dacosys.warehouseCounter.data.room.dao.orderRequest.OrderRequestCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.data.sync.*
import com.dacosys.warehouseCounter.databinding.ActivityHomeBinding
import com.dacosys.warehouseCounter.misc.ImageControl.Companion.setupImageControl
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.DATE_FORMAT
import com.dacosys.warehouseCounter.misc.Statics.Companion.isDebuggable
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.mainButton.MainButton
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.ui.activities.codeCheck.CodeCheckActivity
import com.dacosys.warehouseCounter.ui.activities.linkCode.LinkCodeActivity
import com.dacosys.warehouseCounter.ui.activities.order.OrderMoveActivity
import com.dacosys.warehouseCounter.ui.activities.order.OrderPackUnpackActivity
import com.dacosys.warehouseCounter.ui.activities.orderLocation.OrderLocationSelectActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.NewCountActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestContentActivity
import com.dacosys.warehouseCounter.ui.activities.print.PrintLabelActivity
import com.dacosys.warehouseCounter.ui.activities.ptlOrder.NewPtlOrdersActivity
import com.dacosys.warehouseCounter.ui.activities.ptlOrder.PtlOrderActivity
import com.dacosys.warehouseCounter.ui.activities.sync.InboxActivity
import com.dacosys.warehouseCounter.ui.activities.sync.OutboxActivity
import com.dacosys.warehouseCounter.ui.fragments.main.ButtonPageFragment
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import org.parceler.Parcels
import java.io.File
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.io.path.Path
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.OrderRequest as OrderRequestRoom

class HomeActivity : AppCompatActivity(), Scanner.ScannerListener, ButtonPageFragment.ButtonClickedListener {

    private fun onCompletedOrder(orders: ArrayList<OrderRequest>) {
        android.util.Log.d(
            this::class.java.simpleName,
            getString(R.string.completed_orders_) + orders.count()
        )

        if (orders.isNotEmpty() && settingViewModel.autoSend) {
            try {
                thread {
                    CreateOrder(
                        payload = orders,
                        onEvent = { showSnackBar(it.text, it.snackBarType) },
                        onFinish = { successFiles ->
                            if (successFiles.isEmpty()) return@CreateOrder
                            /** We delete the files of the orders sent */
                            OrderRequest.removeCountFiles(
                                successFiles = successFiles,
                                sendEvent = { eventData -> showSnackBar(eventData.text, eventData.snackBarType) }
                            )
                        }

                    ).execute()
                }
            } catch (ex: Exception) {
                ErrorLog.writeLog(
                    this, this::class.java.simpleName, ex.message.toString()
                )
            }
        }

        setTextButton(MainButton.CompletedCounts, orders.count())
    }

    private fun onNewOrder(itemArray: ArrayList<OrderRequest>) {
        if (itemArray.isNotEmpty()) {
            android.util.Log.d(
                this::class.java.simpleName,
                "${getString(R.string.new_orders_received_)}${itemArray.count()}"
            )

            if (!Statics.isExternalStorageWritable) {
                android.util.Log.e(
                    this::class.java.simpleName,
                    getString(R.string.error_external_storage_not_available_for_reading_or_writing)
                )
                return
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                newOrArray = itemArray
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE
                )
                return
            }

            writeNewOrderRequest(itemArray)
        }

        setTextButton(MainButton.PendingCounts, countPending())
    }

    private fun countPending(): Int {
        val currentDir = Statics.getPendingPath()
        val files = currentDir.listFiles() ?: return 0
        return files.count { t -> t.extension == "json" }
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingViewModel.showScannedCode) showSnackBar(scanCode, INFO)

        JotterListener.lockScanner(this, true)
        JotterListener.hideWindow(this)

        try {
            /*
            runOnUiThread {
                val checkCodeTask = CodeRead()
                checkCodeTask.addParams(this, scanCode)
                checkCodeTask.execute()
            }
            */
        } catch (ex: Exception) {
            showSnackBar(ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(binding.root, text, snackBarType)
    }

    private var rejectNewInstances = false

    override fun onResume() {
        super.onResume()
        rejectNewInstances = false

        Screen.closeKeyboard(this)

        // Si ya está autentificado iniciar la sincronización
        if (Statics.currentUserId > 0L) {
            startSync()
        }
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        pauseInboxOutboxListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        if (!settingViewModel.showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingViewModel.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.action_settings -> {
                configApp()
                return true
            }

            R.id.action_rfid_connect -> {
                JotterListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                JotterListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                JotterListener.toggleCameraFloatingWindowVisibility(this)
                return super.onOptionsItemSelected(item)
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun resize(image: Drawable): Drawable {
        val bitmap = (image as BitmapDrawable).bitmap
        val bitmapResized = Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * 0.5).toInt(), (bitmap.height * 0.5).toInt(), false
        )
        return BitmapDrawable(resources, bitmapResized)
    }

    @SuppressLint("SetTextI18n")
    @Throws(PackageManager.NameNotFoundException::class)
    private fun initSetup() {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        var draw = ContextCompat.getDrawable(this, R.drawable.wc)

        /// USUARIO
        Statics.getCurrentUser {
            if (it != null) {
                binding.clientTextView.text =
                    String.format("%s - %s", settingViewModel.installationCode, it.name)
            }
        }

        /// VERSION
        binding.versionTextView.text =
            String.format("%s %s", getString(R.string.app_milestone), pInfo.versionName)

        /// IMAGEN DE CABECERA
        binding.imageViewHeader.setImageResource(0)
        draw = resize(draw!!)
        binding.imageViewHeader.setImageDrawable(draw)

        /// SETUP VIEW PAGER AND BUTTONS
        setupViewPager()
    }

    override fun onPause() {
        super.onPause()
        pauseInboxOutboxListener()
    }

    private fun pauseInboxOutboxListener() {
        sync.stopSync()
    }

    override fun onButtonClicked(button: Button) {
        clickButton(button)
    }

    private fun clickButton(clickedButton: Button) {
        when (MainButton.getById(clickedButton.tag.toString().toLong())) {
            MainButton.NewCount -> {
                launchNewCountActivity()
            }

            MainButton.PendingCounts -> {
                launchInboxActivity()
            }

            MainButton.CompletedCounts -> {
                launchOutboxActivity()
            }

            MainButton.CodeRead -> {
                launchCodeReadActivity()
            }

            MainButton.LinkItemCodes -> {
                launchLinkCodeActivity()
            }

            MainButton.PtlOrder -> {
                launchNewPtlOrderActivity()
            }

            MainButton.PrintLabels -> {
                launchPrintLabelsActivity()
            }

            MainButton.OrderLocationLabel -> {
                launchOrderLocationLabelActivity()
            }

            MainButton.MoveOrder -> {
                launchMoveOrderActivity()
            }

            MainButton.PackUnpackOrder -> {
                launchPackUnpackOrderActivity()
            }

            MainButton.Configuration -> {
                configApp()
            }
        }
    }

    private fun launchPackUnpackOrderActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, OrderPackUnpackActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.pack_unpack))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, false)
        resultForUnpackOrder.launch(intent)
    }

    private fun launchMoveOrderActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, OrderMoveActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.move_order))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, false)
        startActivity(intent)
    }

    private fun launchOrderLocationLabelActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, OrderLocationSelectActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.order_location))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, true)
        intent.putExtra(OrderLocationSelectActivity.ARG_SHOW_SELECT_BUTTON, false)
        startActivity(intent)
    }

    private fun launchPrintLabelsActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(baseContext, PrintLabelActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun launchLinkCodeActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(context, LinkCodeActivity::class.java)
        intent.putExtra(LinkCodeActivity.ARG_TITLE, getString(R.string.link_code))
        startActivity(intent)
    }

    private fun launchCodeReadActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(context, CodeCheckActivity::class.java)
        intent.putExtra(CodeCheckActivity.ARG_TITLE, getString(R.string.code_read))
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun launchNewCountActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(context, NewCountActivity::class.java)
        intent.putExtra(NewCountActivity.ARG_TITLE, getString(R.string.new_count))
        resultForNewCount.launch(intent)
    }

    private fun launchOutboxActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(context, OutboxActivity::class.java)
        intent.putExtra(OutboxActivity.ARG_TITLE, getString(R.string.completed_counts))
        resultForCompletedCount.launch(intent)
    }

    private fun launchInboxActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        JotterListener.lockScanner(this, true)

        val intent = Intent(context, InboxActivity::class.java)
        intent.putExtra(InboxActivity.ARG_TITLE, getString(R.string.pending_counts))
        resultForPendingCount.launch(intent)
    }

    private fun launchNewPtlOrderActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(context, NewPtlOrdersActivity::class.java)
        intent.putExtra(NewPtlOrdersActivity.ARG_TITLE, getString(R.string.setup_new_ptl))
        resultForNewPtl.launch(intent)
    }

    private val resultForCompletedCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val filenames: ArrayList<String> =
                        data.getStringArrayListExtra(OutboxActivity.ARG_ORDER_REQUEST_FILENAMES)
                            ?: return@registerForActivityResult

                    if (filenames.isEmpty()) return@registerForActivityResult

                    addOrderRequestFromJson(filenames[0])
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForUnpackOrder = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val data = it?.data
        try {
            if (it?.resultCode == RESULT_OK && data != null) {
                val id = data.getLongExtra(OrderPackUnpackActivity.ARG_REPACK_ORDER_ID, 0L)
                if (id > 0) {
                    thread {
                        ViewOrder(
                            id = id,
                            action = ViewOrder.defaultAction,
                            onFinish = { order ->
                                if (order == null) {
                                    showSnackBar(getString(R.string.order_not_found), ERROR)
                                } else {
                                    repackOrder(order)
                                }
                            }).execute()
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            ErrorLog.writeLog(this, this::class.java.simpleName, ex)
        }
    }

    private val resultForNewPtl =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val warehouseArea =
                        Parcels.unwrap<WarehouseArea>(data.getParcelableExtra(NewPtlOrdersActivity.ARG_WAREHOUSE_AREA))
                    if (warehouseArea == null) {
                        rejectNewInstances = false
                        return@registerForActivityResult
                    }

                    showSnackBar(
                        String.format(
                            getString(R.string.area),
                            warehouseArea.description,
                            lineSeparator,
                            "(${warehouseArea.locationParentStr})"
                        ), INFO
                    )

                    val intent = Intent(context, PtlOrderActivity::class.java)
                    intent.putExtra(PtlOrderActivity.ARG_WAREHOUSE_AREA, Parcels.wrap(warehouseArea))
                    resultForPtlFinish.launch(intent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForPtlFinish =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                launchNewPtlOrderActivity()
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForNewCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val client = Parcels.unwrap<Client>(data.getParcelableExtra(NewCountActivity.ARG_CLIENT))
                    val description = data.getStringExtra(NewCountActivity.ARG_DESCRIPTION) ?: ""
                    val orderRequestType =
                        Parcels.unwrap<OrderRequestType>(data.getParcelableExtra<OrderRequestType>(NewCountActivity.ARG_ORDER_REQUEST_TYPE))

                    addOrderRequest(client, description, orderRequestType)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private fun addOrderRequest(client: Client?, description: String, orderRequestType: OrderRequestType) {
        showSnackBar(
            String.format(
                getString(R.string.client_description),
                client?.name ?: getString(R.string.no_client),
                lineSeparator,
                description
            ), INFO
        )

        val orderRequest = OrderRequestRoom(
            clientId = client?.clientId ?: 0,
            creationDate = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString(),
            description = description,
            orderTypeDescription = orderRequestType.description,
            orderTypeId = orderRequestType.id.toInt(),
            resultAllowDiff = 1,
            resultAllowMod = 1,
            resultDiffProduct = 1,
            resultDiffQty = 1,
            startDate = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString(),
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    val intent = Intent(context, OrderRequestContentActivity::class.java)
                    intent.putExtra(OrderRequestContentActivity.ARG_ID, newId)
                    intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, true)
                    startActivity(intent)
                }
            })
    }

    private fun repackOrder(order: OrderResponse) {
        showSnackBar(
            String.format(
                getString(R.string.client_description),
                order.clientId ?: getString(R.string.no_client),
                lineSeparator,
                order.description
            ), INFO
        )

        val orderRequest = OrderRequestRoom(
            clientId = order.clientId ?: 0,
            creationDate = order.rowCreationDate,
            description = order.description,
            orderTypeDescription = OrderRequestType.packaging.description,
            orderTypeId = OrderRequestType.packaging.id.toInt(),
            resultAllowDiff = if (order.resultAllowDiff == true) 1 else 0,
            resultAllowMod = if (order.resultAllowMod == true) 1 else 0,
            resultDiffProduct = if (order.resultDiffProduct == true) 1 else 0,
            resultDiffQty = if (order.resultDiffQty == true) 1 else 0,
            startDate = order.startDate,
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    orderRequest.orderRequestId = newId
                    OrderRequestCoroutines.update(
                        orderRequest = orderRequest.toKtor,
                        contents = order.contentToKtor(),
                        onResult = {
                            if (it) {
                                val intent = Intent(context, OrderRequestContentActivity::class.java)
                                intent.putExtra(OrderRequestContentActivity.ARG_ID, newId)
                                intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, false)
                                startActivity(intent)
                            } else {
                                showSnackBar(getString(R.string.error_when_updating_the_order), ERROR)
                            }
                        })
                } else {
                    showSnackBar(getString(R.string.error_when_creating_the_order), ERROR)
                }
            })
    }

    private val resultForPendingCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val filenames: ArrayList<String> =
                        data.getStringArrayListExtra(InboxActivity.ARG_ORDER_REQUEST_FILENAMES)
                            ?: return@registerForActivityResult

                    if (filenames.isEmpty()) return@registerForActivityResult

                    addOrderRequestFromJson(filenames[0])
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private fun addOrderRequestFromJson(filename: String) {
        val order = OrderRequest(Path(Statics.completePendingPath, filename).toString())
        val completeList = ArrayList(order.contents)

        val orderRequest = OrderRequestRoom(
            clientId = order.clientId ?: 0,
            creationDate = order.creationDate.toString(),
            description = order.description,
            orderTypeDescription = OrderRequestType.packaging.description,
            orderTypeId = OrderRequestType.packaging.id.toInt(),
            resultAllowDiff = if (order.resultAllowDiff == true) 1 else 0,
            resultAllowMod = if (order.resultAllowMod == true) 1 else 0,
            resultDiffProduct = if (order.resultDiffProduct == true) 1 else 0,
            resultDiffQty = if (order.resultDiffQty == true) 1 else 0,
            startDate = order.startDate.toString(),
            userId = Statics.currentUserId,
        )

        OrderRequestCoroutines.add(
            orderRequest = orderRequest,
            onResult = { newId ->
                if (newId != null) {
                    orderRequest.orderRequestId = newId
                    OrderRequestCoroutines.update(
                        orderRequest = orderRequest.toKtor,
                        contents = completeList,
                        onResult = {
                            try {
                                val intent = Intent(context, OrderRequestContentActivity::class.java)
                                intent.putExtra(OrderRequestContentActivity.ARG_ID, newId)
                                intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, false)
                                startActivity(intent)
                            } catch (ex: Exception) {
                                val res =
                                    getString(R.string.an_error_occurred_while_trying_to_load_the_order)
                                showSnackBar(res, ERROR)
                                android.util.Log.e(this::class.java.simpleName, res)
                            }
                        })
                } else {
                    showSnackBar(getString(R.string.error_when_creating_the_order), ERROR)
                }
            })
    }

    private fun configApp() {
        val realPass = settingViewModel.confPassword
        if (realPass.isEmpty()) {
            attemptEnterConfig(realPass)
            return
        }

        runOnUiThread {
            var alertDialog: AlertDialog? = null
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.enter_password))

            val inputLayout = TextInputLayout(this)
            inputLayout.endIconMode = END_ICON_PASSWORD_TOGGLE

            val input = TextInputEditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.isFocusable = true
            input.isFocusableInTouchMode = true
            input.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            if (alertDialog != null) {
                                alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
                                    .performClick()
                            }
                        }
                    }
                }
                false
            }

            inputLayout.addView(input)
            builder.setView(inputLayout)
            builder.setPositiveButton(R.string.accept) { _, _ ->
                attemptEnterConfig(input.text.toString())
            }
            builder.setNegativeButton(R.string.cancel, null)
            alertDialog = builder.create()

            alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            alertDialog.show()
            input.requestFocus()
        }
    }

    private fun attemptEnterConfig(password: String) {
        val realPass = settingViewModel.confPassword
        if (password == realPass) {
            if (!rejectNewInstances) {
                rejectNewInstances = true

                val intent = Intent(context, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                resultForSettings.launch(intent)
            }
        } else {
            showSnackBar(getString(R.string.invalid_password), ERROR)
        }
    }

    private val resultForSettings =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Reconfiguración de parámetros

            // Vamos a reconstruir el scanner por si cambió la configuración
            JotterListener.autodetectDeviceModel(this)

            // Reconfigurar ImageControl
            setupImageControl()

            // Permitir o no la rotación de pantalla
            Screen.setScreenRotation(this)

            // Todavía no está autentificado
            if (Statics.currentUserId < 0L) {
                login()
            }
        }

    private lateinit var splashScreen: SplashScreen
    private lateinit var binding: ActivityHomeBinding
    private val syncVm: SyncViewModel by lazy { syncViewModel }

    private fun createSplashScreen() {
        // Set up 'core-splashscreen' to handle the splash screen in a backward compatible manner.
        splashScreen = installSplashScreen()
        return
    }

    private fun onTimerTick(secs: Int) {
        if (isDestroyed || isFinishing) return

        runOnUiThread {
            val restSec = settingViewModel.wcSyncInterval - secs
            val restMin = restSec / 60
            val rstSecsInMin = restSec % 60
            val msg = "$restMin:${String.format("%02d", rstSecsInMin)}"
            binding.timeTextView.text = msg
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        createSplashScreen()
        super.onCreate(savedInstanceState)
        Screen.setScreenRotation(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        syncVm.syncCompletedOrders.observe(this) { if (it != null) onCompletedOrder(it) }
        syncVm.syncNewOrders.observe(this) { if (it != null) onNewOrder(it) }
        syncVm.syncTimer.observe(this) { if (it != null) onTimerTick(it) }
        syncVm.uploadImagesProgress.observe(this) { if (it != null) onUploadImagesProgress(it) }

        var freshSessionReq = false

        if (!isDebuggable() && !BuildConfig.DEBUG) {
            // Mostramos el Timer solo en DEBUG
            binding.timeTextView.visibility = View.GONE
        }

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados.
            if (Statics.currentUserId < 0L) {
                freshSessionReq = true
            } else {
                initSetup()
            }
        } else {
            // Primera inicialización
            freshSessionReq = true
        }

        if (freshSessionReq) {
            // Inicializar la actividad
            if (settingViewModel.urlPanel.isEmpty()) {
                showSnackBar(getString(R.string.server_is_not_configured), ERROR)
                setupInitConfig()
            } else {
                login()
            }
            return
        }
    }

    override fun onStart() {
        super.onStart()

        sync.onCompletedOrders { syncVm.setSyncCompleted(it) }
        sync.onNewOrders { syncVm.setSyncNew(it) }
        sync.onTimerTick { syncVm.setSyncTimer(it) }
    }

    private fun onUploadImagesProgress(it: UploadImagesProgress) {
        if (isDestroyed || isFinishing) return

        val result: com.dacosys.imageControl.network.common.ProgressStatus = it.result
        val msg: String = it.msg

        when (result.id) {
            ProgressStatus.starting.id, ProgressStatus.running.id -> {
                setProgressBarText(msg)
                showImageProgressBar(true)
            }

            ProgressStatus.crashed.id, ProgressStatus.canceled.id -> {
                showImageProgressBar(false)
                showSnackBar(msg, ERROR)
            }

            ProgressStatus.success.id -> {
                showImageProgressBar(false)
                showSnackBar(getString(R.string.upload_images_success), SnackBarType.SUCCESS)
            }
        }
    }

    private fun setProgressBarText(text: String) {
        runOnUiThread {
            binding.syncStatusTextView.text = text
        }
    }

    private fun showImageProgressBar(show: Boolean) {
        runOnUiThread {
            if (show && binding.progressBarLayout.visibility != View.VISIBLE) {
                binding.progressBarLayout.bringToFront()
                binding.progressBarLayout.visibility = View.VISIBLE

                ViewCompat.setZ(binding.progressBarLayout, 0F)
            } else if (!show && binding.progressBarLayout.visibility != View.GONE) {
                binding.progressBarLayout.visibility = View.GONE
            }
        }
    }

    private fun setupInitConfig() {
        val intent = Intent(this, InitConfigActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        resultForInitConfig.launch(intent)
    }

    private val resultForInitConfig =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                if (settingViewModel.urlPanel.isEmpty()) {
                    showSnackBar(getString(R.string.server_is_not_configured), ERROR)
                    setupInitConfig()
                } else {
                    login()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
                JotterListener.lockScanner(this, false)
            }
        }

    private fun login() {
        if (rejectNewInstances) return
        rejectNewInstances = true

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        resultForLogin.launch(intent)
    }

    private val resultForLogin =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                initSetup()
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
                JotterListener.lockScanner(this, false)
            }
        }

// region ViewPager
    /**
     * Así se construye el nombre de un fragmento en FragmentManager
     */
    private fun makeFragmentName(viewId: Long, id: Long): String {
        return "android:switcher:$viewId:$id"
    }

    private fun setTextButton(button: MainButton, newItems: Int) {
        val name = makeFragmentName(
            binding.buttonViewPager.id.toLong(),
            (binding.buttonViewPager.adapter as ViewPagerAdapter).getItemId(0)
        )

        // Así se obtiene correctamente y de manera segura el fragmento
        // que estamos buscando
        val frag = supportFragmentManager.findFragmentByTag(name) as ButtonPageFragment
        if (!frag.isAdded) return

        runOnUiThread {
            frag.setButtonSubText(button, newItems.toString())
        }

        if (newItems > 0) {
            if (settingViewModel.shakeOnPendingOrders) {
                shakeDevice()
            }
            if (settingViewModel.soundOnPendingOrders) {
                playNotification()
            }

            val v = frag.getButton(button)
            if (v != null) {
                shakeView(v, 20, 5)
            }
        }
    }

    private fun setupViewPager() {
        // Paginado de botones
        val adapter = ViewPagerAdapter(supportFragmentManager)
        binding.buttonViewPager.offscreenPageLimit = 2
        binding.buttonViewPager.adapter = adapter
    }

    internal inner class ViewPagerAdapter(fragmentManager: FragmentManager) :
        PersistentPagerAdapter<ButtonPageFragment>(fragmentManager) {
        override fun getItem(position: Int): Fragment {
            val allButtons = MainButton.getAll()
            val firstPageButtons: ArrayList<MainButton> = ArrayList()
            val secPageButtons: ArrayList<MainButton> = ArrayList()
            for ((i, t) in allButtons.withIndex()) {
                if (i < 6) {
                    firstPageButtons.add(t)
                } else if (i < 12) {
                    secPageButtons.add(t)
                }
            }

            return when (position) {
                0 -> ButtonPageFragment.newInstance(firstPageButtons, position)
                1 -> ButtonPageFragment.newInstance(secPageButtons, position)
                else -> ButtonPageFragment()
            }
        }

        private val totalPages = 2
        override fun getCount(): Int {
            return totalPages
        }
    }
//endregion

    /**
     *
     * @param view      view that will be animated
     * @param duration  for how long in ms will it shake?
     * @param offset    start offset of the animation
     * @return          returns the same view with animation properties
     */
    @Suppress("SameParameterValue")
    private fun shakeView(view: View, duration: Int, offset: Int): View {
        val anim = TranslateAnimation(-offset.toFloat(), offset.toFloat(), 0.toFloat(), 0.toFloat())

        anim.duration = duration.toLong()
        anim.repeatMode = Animation.REVERSE
        anim.repeatCount = 5
        view.startAnimation(anim)

        return view
    }

    // Vibrate for 150 milliseconds
    private fun shakeDevice() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(
                    150, 10
                )
            )
        } else {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        150, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                vibrator.vibrate(150)
            }
        }
    }

    private fun playNotification() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r: Ringtone = RingtoneManager.getRingtone(this, notification)
            r.play()
        } catch (ignore: Exception) {
        }
    }

    private fun startSync() {
        Thread { sync.startSync() }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) {
            JotterListener.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
            return
        }

        when (requestCode) {
            REQUEST_EXTERNAL_STORAGE -> {
                // If the request is canceled, the result arrays are empty.
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showSnackBar(getString(R.string.cannot_write_to_external_storage), ERROR)
                } else {
                    writeNewOrderRequest(newOrArray)
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun writeNewOrderRequest(newOrArray: ArrayList<OrderRequest>) {
        val pendingOrderArray = OrderRequest.getPendingOrders()
        val df = SimpleDateFormat("yyMMddHHmmssZ")

        var isOk = true
        for (newOrder in newOrArray) {
            val orJson = json.encodeToString(OrderRequest.serializer(), newOrder)

            // Acá se comprueba si el ID ya existe y actualizamos la orden.
            // Si no se agrega una orden nueva.
            var alreadyExists = false
            if (pendingOrderArray.any()) {
                for (pendingOr in pendingOrderArray) {
                    if (pendingOr.orderRequestId == newOrder.orderRequestId) {
                        alreadyExists = true

                        isOk = if (newOrder.completed == true) {
                            // Está completada, eliminar localmente
                            val currentDir = Statics.getPendingPath()
                            val filePath = "${currentDir.absolutePath}${File.separator}${pendingOr.filename}"
                            val fl = File(filePath)
                            fl.delete()
                        } else {
                            // Actualizar contenido local
                            updateOrder(origOrder = pendingOr, newOrder = newOrder)
                        }

                        break
                    }
                }
            }

            if (!alreadyExists) {
                val orFileName = String.format("%s.json", df.format(Calendar.getInstance().time))

                if (!writeToFile(
                        fileName = orFileName,
                        data = orJson,
                        directory = Statics.getPendingPath()
                    )
                ) {
                    isOk = false
                    break
                }
            }
        }

        newOrArray.clear()

        if (isOk) {
            val res = getString(R.string.new_counts_saved)
            showSnackBar(res, SnackBarType.SUCCESS)
            android.util.Log.d(this::class.java.simpleName, res)
        } else {
            val res = getString(R.string.an_error_occurred_while_trying_to_save_the_count)
            showSnackBar(res, ERROR)
            android.util.Log.e(this::class.java.simpleName, res)
        }
    }

    private fun updateOrder(origOrder: OrderRequest, newOrder: OrderRequest): Boolean {
        var error: Boolean
        try {
            val orJson = json.encodeToString(OrderRequest.serializer(), newOrder)
            android.util.Log.i(this::class.java.simpleName, orJson)
            val orFileName = origOrder.filename.substringAfterLast('/')

            error = !writeJsonToFile(
                v = binding.root,
                filename = orFileName,
                value = orJson,
                completed = newOrder.completed ?: false
            )
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            android.util.Log.e(this::class.java.simpleName, e.message ?: "")
            error = true
        }
        return !error
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 5001

        fun equals(a: Any?, b: Any): Boolean {
            return a != null && a == b
        }

        private var newOrArray: ArrayList<OrderRequest> = ArrayList()
    }
}
