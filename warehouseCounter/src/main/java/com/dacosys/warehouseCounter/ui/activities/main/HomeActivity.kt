package com.dacosys.warehouseCounter.ui.activities.main

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.content.Intent
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
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.Interpolator
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
import com.dacosys.warehouseCounter.WarehouseCounterApp
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.sync
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.countPending
import com.dacosys.warehouseCounter.data.io.IOFunc.Companion.getCompletedOrders
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.BarcodeParam
import com.dacosys.warehouseCounter.data.ktor.v2.dto.barcode.PrintOps
import com.dacosys.warehouseCounter.data.ktor.v2.dto.location.WarehouseArea
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequest
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderRequestType
import com.dacosys.warehouseCounter.data.ktor.v2.dto.order.OrderResponse
import com.dacosys.warehouseCounter.data.ktor.v2.functions.barcode.GetOrderBarcode
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.SendOrder
import com.dacosys.warehouseCounter.data.ktor.v2.functions.order.ViewOrder
import com.dacosys.warehouseCounter.data.ktor.v2.sync.SyncViewModel
import com.dacosys.warehouseCounter.data.room.dao.pendingLabel.PendingLabelCoroutines
import com.dacosys.warehouseCounter.data.room.entity.client.Client
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.AddOrder
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.AddOrderFromFile
import com.dacosys.warehouseCounter.data.room.entity.orderRequest.RepackOrder
import com.dacosys.warehouseCounter.databinding.ActivityHomeBinding
import com.dacosys.warehouseCounter.misc.ImageControl.Companion.setupImageControl
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.isDebuggable
import com.dacosys.warehouseCounter.misc.Statics.Companion.lineSeparator
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.mainButton.MainButton
import com.dacosys.warehouseCounter.misc.objects.status.ProgressStatus
import com.dacosys.warehouseCounter.printer.Printer
import com.dacosys.warehouseCounter.scanners.LifecycleListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.ui.activities.codeCheck.CodeCheckActivity
import com.dacosys.warehouseCounter.ui.activities.linkCode.LinkCodeActivity
import com.dacosys.warehouseCounter.ui.activities.order.OrderMoveActivity
import com.dacosys.warehouseCounter.ui.activities.order.OrderPackUnpackActivity
import com.dacosys.warehouseCounter.ui.activities.order.OrderPagingActivity
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
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.SUCCESS
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import kotlin.concurrent.thread


class HomeActivity : AppCompatActivity(), Scanner.ScannerListener, ButtonPageFragment.ButtonClickedListener {

    private val tag = this::class.java.enclosingClass?.simpleName ?: this::class.java.simpleName

    @get:Synchronized
    private var isSending = false
    private fun onCompletedOrder() {
        sendCompletedOrders(
            onFinish = {
                setTextButton(MainButton.CompletedCounts, it)
            }
        )
    }

    private fun sendCompletedOrders(onFinish: (Int) -> Unit) {
        val orders = getCompletedOrders()

        if (!isSending && orders.isNotEmpty() && settingsVm.autoSend) {
            isSending = true

            runOnUiThread {
                SendOrder(
                    orders = orders,
                    onEvent = {
                        isSending = it.snackBarType !in SnackBarType.getFinish()
                        if (it.snackBarType == SUCCESS) {
                            onFinish(0)
                        } else {
                            showSnackBar(it.text, it.snackBarType)
                        }
                    },
                    onFinish = {
                        PendingLabelCoroutines.add(it)
                        if (settingsVm.autoPrint) {
                            printOrderLabels(it)
                        }
                    }
                )
            }
        } else {
            onFinish(orders.count())
        }
    }

    private fun printOrderLabels(ids: ArrayList<Long>) {
        val templateId = settingsVm.defaultOrderTemplateId
        val qty = settingsVm.printerQty
        val printOps = PrintOps.getPrintOps()

        GetOrderBarcode(
            param = BarcodeParam(
                idList = ids,
                templateId = templateId,
                printOps = printOps
            ),
            onEvent = {
                if (it.snackBarType != SUCCESS) showSnackBar(it.text, it.snackBarType)
            },
            onFinish = { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val sendThis = barcodes.joinToString(lineSeparator) { it.body }
                    Printer.PrinterFactory.createPrinter(
                        activity = this,
                        onEvent = { showSnackBar(it.text, it.snackBarType) }
                    )?.printLabel(
                        printThis = sendThis,
                        qty = qty,
                        onFinish = { success ->
                            if (success) PendingLabelCoroutines.remove(ids)
                        }
                    )
                }
            }
        ).execute()
    }

    private fun onNewOrder(itemArray: ArrayList<OrderRequest>) {
        Log.d(tag, "${getString(R.string.new_orders_received_)}${itemArray.count()}")
        Log.d(tag, itemArray.map { it.orderRequestId }.joinToString(","))

        setTextButton(MainButton.PendingCounts, countPending())
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingsVm.showScannedCode) showSnackBar(scanCode, INFO)

        LifecycleListener.lockScanner(this, true)
        LifecycleListener.hideWindow(this)

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
            ErrorLog.writeLog(this, tag, ex.message.toString())
        } finally {
            // Unless is blocked, unlock the partial
            LifecycleListener.lockScanner(this, false)
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

        if (Statics.currentUserId > 0L) {
            sync.startSync()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        Statics.cleanCurrentUser()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        if (!settingsVm.showConfButton) {
            menu.removeItem(menu.findItem(R.id.action_settings).itemId)
        }

        if (!settingsVm.useBtRfid) {
            menu.removeItem(menu.findItem(R.id.action_rfid_connect).itemId)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home, android.R.id.home -> {
                @Suppress("DEPRECATION") onBackPressed()
                return true
            }

            R.id.action_settings -> {
                configApp()
                return true
            }

            R.id.action_rfid_connect -> {
                LifecycleListener.rfidStart(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_trigger_scan -> {
                LifecycleListener.trigger(this)
                return super.onOptionsItemSelected(item)
            }

            R.id.action_read_barcode -> {
                LifecycleListener.toggleCameraFloatingWindowVisibility(this)
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

            MainButton.OrderLocation -> {
                launchOrderLocationLabelActivity()
            }

            MainButton.MoveOrder -> {
                launchMoveOrderActivity()
            }

            MainButton.PackUnpackOrder -> {
                launchPackUnpackOrderActivity()
            }

            MainButton.TestButton -> {
                launchTestActivity()
            }

            MainButton.Configuration -> {
                configApp()
            }
        }
    }

    private fun launchTestActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        LifecycleListener.lockScanner(this, true)

        val intent = Intent(context, OrderPagingActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.test))
        intent.putExtra(OrderLocationSelectActivity.ARG_SHOW_SELECT_BUTTON, false)
        resultForTestActivity.launch(intent)
    }

    private fun launchPackUnpackOrderActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        LifecycleListener.lockScanner(this, true)

        val intent = Intent(context, OrderPackUnpackActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.pack_unpack))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, false)
        resultForUnpackOrder.launch(intent)
    }

    private fun launchMoveOrderActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        LifecycleListener.lockScanner(this, true)

        val intent = Intent(context, OrderMoveActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.move_order))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, false)
        startActivity(intent)
    }

    private fun launchOrderLocationLabelActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        LifecycleListener.lockScanner(this, true)

        val intent = Intent(context, OrderLocationSelectActivity::class.java)
        intent.putExtra(OrderLocationSelectActivity.ARG_TITLE, getString(R.string.order_location))
        intent.putExtra(OrderLocationSelectActivity.ARG_MULTI_SELECT, true)
        intent.putExtra(OrderLocationSelectActivity.ARG_SHOW_SELECT_BUTTON, false)
        startActivity(intent)
    }

    private fun launchPrintLabelsActivity() {
        if (rejectNewInstances) return
        rejectNewInstances = true
        LifecycleListener.lockScanner(this, true)

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
        LifecycleListener.lockScanner(this, true)

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

    private val resultForTestActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            rejectNewInstances = false
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
                ErrorLog.writeLog(this, tag, ex)
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
            ErrorLog.writeLog(this, tag, ex)
        }
    }

    private val resultForNewPtl =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val warehouseArea =
                        data.parcelable<WarehouseArea>(NewPtlOrdersActivity.ARG_WAREHOUSE_AREA)
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
                    intent.putExtra(PtlOrderActivity.ARG_WAREHOUSE_AREA, warehouseArea)
                    resultForPtlFinish.launch(intent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, tag, ex)
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
                ErrorLog.writeLog(this, tag, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForNewCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val client = data.parcelable<Client>(NewCountActivity.ARG_CLIENT)
                    val description = data.getStringExtra(NewCountActivity.ARG_DESCRIPTION) ?: ""
                    val orderRequestType = data.parcelable<OrderRequestType>(NewCountActivity.ARG_ORDER_REQUEST_TYPE)

                    if (orderRequestType != null) {
                        addOrderRequest(client, description, orderRequestType)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, tag, ex)
            } finally {
                rejectNewInstances = false
            }
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
                ErrorLog.writeLog(this, tag, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private fun addOrderRequest(client: Client?, description: String, orderRequestType: OrderRequestType) {
        AddOrder(
            client = client,
            description = description,
            orderRequestType = orderRequestType,
            onEvent = { showSnackBar(it.text, it.snackBarType) },
            onNewId = {
                val intent = Intent(context, OrderRequestContentActivity::class.java)
                intent.putExtra(OrderRequestContentActivity.ARG_ID, it)
                intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, true)
                startActivity(intent)
            }
        )
    }

    private fun repackOrder(order: OrderResponse) {
        RepackOrder(
            order = order,
            onEvent = { showSnackBar(it.text, it.snackBarType) },
            onNewId = {
                val intent = Intent(context, OrderRequestContentActivity::class.java)
                intent.putExtra(OrderRequestContentActivity.ARG_ID, it)
                intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, false)
                startActivity(intent)
            }
        )
    }

    private fun addOrderRequestFromJson(filename: String) {
        AddOrderFromFile(
            filename = filename,
            onEvent = { showSnackBar(it.text, it.snackBarType) },
            onNewId = {
                val intent = Intent(context, OrderRequestContentActivity::class.java)
                intent.putExtra(OrderRequestContentActivity.ARG_ID, it)
                intent.putExtra(OrderRequestContentActivity.ARG_IS_NEW, false)
                startActivity(intent)
            }
        )
    }

    private fun configApp() {
        val realPass = settingsVm.confPassword
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
        val realPass = settingsVm.confPassword
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
            LifecycleListener.autodetectDeviceModel(this)

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
    private val syncVm: SyncViewModel by lazy { WarehouseCounterApp.syncVm }

    private fun createSplashScreen() {
        splashScreen = installSplashScreen()
        return
    }

    private fun onTimerTick(secs: Int) {
        if (isDestroyed || isFinishing) return

        runOnUiThread {
            val restSec = settingsVm.wcSyncInterval - secs
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

        if (!isDebuggable() && !BuildConfig.DEBUG) {
            binding.timeTextView.visibility = View.GONE
            binding.syncImageView.visibility = View.GONE
        }

        binding.syncImageView.setOnClickListener {
            runOnUiThread {
                val rotationAnimator = ObjectAnimator.ofFloat(it, View.ROTATION, 0f, 360f)
                rotationAnimator.duration = 1000
                rotationAnimator.interpolator = Interpolator { it * it * (3f - 2f * it) }
                rotationAnimator.start()
            }
            sync.forceSync()
        }

        sync.resetSync()
    }

    override fun onStop() {
        super.onStop()

        sync.stopSync()

        syncVm.syncCompletedOrders.removeObservers(this)
        syncVm.syncNewOrders.removeObservers(this)
        syncVm.syncTimer.removeObservers(this)
        syncVm.uploadImagesProgress.removeObservers(this)
    }

    override fun onStart() {
        super.onStart()

        if (Statics.currentUserId < 0L) {
            if (settingsVm.urlPanel.isEmpty()) {
                showSnackBar(getString(R.string.server_is_not_configured), ERROR)
                setupInitConfig()
            } else {
                login()
            }
            return
        }

        setHeader()
        setupViewPager()

        sync.onCompletedOrders { syncVm.setSyncCompleted(it) }
        sync.onNewOrders { syncVm.setSyncNew(it) }
        sync.onTimerTick { syncVm.setSyncTimer(it) }

        syncVm.syncCompletedOrders.observe(this) { if (it != null) onCompletedOrder() }
        syncVm.syncNewOrders.observe(this) { if (it != null) onNewOrder(it) }
        syncVm.syncTimer.observe(this) { if (it != null) onTimerTick(it) }
        syncVm.uploadImagesProgress.observe(this) { if (it != null) onUploadImagesProgress(it) }
    }

    private fun setHeader() {
        runOnUiThread {
            setHeaderUserName()
            setHeaderVersion()
            setHeaderImageBanner()
        }
    }

    private fun setHeaderUserName() {
        Statics.getCurrentUser {
            if (it != null) {
                binding.clientTextView.text =
                    String.format("%s - %s", settingsVm.installationCode, it.name)
            }
        }
    }

    private fun setHeaderVersion() {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        binding.versionTextView.text =
            String.format("%s %s", getString(R.string.app_milestone), pInfo.versionName)
    }

    private fun setHeaderImageBanner() {
        binding.imageViewHeader.setImageResource(0)
        var draw = ContextCompat.getDrawable(this, R.drawable.wc)
        draw = resize(draw!!)
        binding.imageViewHeader.setImageDrawable(draw)
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
                showSnackBar(getString(R.string.upload_images_success), SUCCESS)
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
                if (settingsVm.urlPanel.isEmpty()) {
                    showSnackBar(getString(R.string.server_is_not_configured), ERROR)
                    setupInitConfig()
                } else {
                    login()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, tag, ex)
            } finally {
                rejectNewInstances = false
                LifecycleListener.lockScanner(this, false)
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
            rejectNewInstances = false
            LifecycleListener.lockScanner(this, false)
        }

    // region ViewPager
    /**
     * Construye el nombre apropiado para el fragmento en FragmentManager
     */
    private fun makeFragmentName(viewId: Long, id: Long): String {
        return "android:switcher:$viewId:$id"
    }

    private fun setTextButton(button: MainButton, newItems: Int) {
        val adapter = binding.buttonViewPager.adapter ?: return
        val itemId = (adapter as ViewPagerAdapter).getItemId(0)

        val name = makeFragmentName(binding.buttonViewPager.id.toLong(), itemId)

        /** Obtiene correctamente y de manera segura el fragmento ButtonPageFragment **/
        val frag = supportFragmentManager.findFragmentByTag(name) as ButtonPageFragment
        if (!frag.isAdded) return

        runOnUiThread {
            frag.setButtonSubText(button, newItems.toString())
        }

        if (newItems > 0) {
            if (settingsVm.shakeOnPendingOrders) {
                shakeDevice()
            }
            if (settingsVm.soundOnPendingOrders) {
                playNotification()
            }

            val v = frag.getButton(button)
            if (v != null) {
                shakeView(v, 20, 5)
            }
        }
    }

    private fun setupViewPager() {
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
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(150, 10))
        } else {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(150)
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

    companion object {
        fun equals(a: Any?, b: Any): Boolean {
            return a != null && a == b
        }
    }
}
