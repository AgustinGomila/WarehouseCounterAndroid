package com.dacosys.warehouseCounter.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.moshi
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingViewModel
import com.dacosys.warehouseCounter.databinding.ActivityHomeBinding
import com.dacosys.warehouseCounter.misc.Statics
import com.dacosys.warehouseCounter.misc.Statics.Companion.closeKeyboard
import com.dacosys.warehouseCounter.misc.Statics.Companion.generateTaskCode
import com.dacosys.warehouseCounter.misc.Statics.Companion.writeToFile
import com.dacosys.warehouseCounter.misc.objects.errorLog.ErrorLog
import com.dacosys.warehouseCounter.misc.objects.mainButton.MainButton
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequest.CREATOR.getCompletedOrders
import com.dacosys.warehouseCounter.moshi.orderRequest.OrderRequestType
import com.dacosys.warehouseCounter.retrofit.functions.NewOrderListener
import com.dacosys.warehouseCounter.retrofit.functions.SendOrder
import com.dacosys.warehouseCounter.room.entity.client.Client
import com.dacosys.warehouseCounter.scanners.JotterListener
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.sync.*
import com.dacosys.warehouseCounter.ui.activities.codeCheck.CodeCheckActivity
import com.dacosys.warehouseCounter.ui.activities.item.ItemSelectActivity
import com.dacosys.warehouseCounter.ui.activities.linkCode.LinkCodeActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.NewCountActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestContentActivity
import com.dacosys.warehouseCounter.ui.activities.sync.InboxActivity
import com.dacosys.warehouseCounter.ui.activities.sync.OutboxActivity
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.ERROR
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType.CREATOR.INFO
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import org.parceler.Parcels
import java.io.File
import java.io.UnsupportedEncodingException
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class HomeActivity : AppCompatActivity(), Scanner.ScannerListener, NewOrderListener,
    CompletedOrderListener {

    override fun onCompletedOrderResult(
        status: ProgressStatus,
        itemArray: ArrayList<OrderRequest>,
        taskCode: Int,
        msg: String,
    ) {
        if (taskCode == TASK_CODE_SYNC_ORDERS) {
            if (status == ProgressStatus.finished || status == ProgressStatus.success || status == ProgressStatus.crashed || status == ProgressStatus.canceled) {
                Log.d(
                    this::class.java.simpleName,
                    getString(R.string.completed_orders_) + itemArray.count()
                )

                if (settingViewModel.autoSend) {
                    val orArray = getCompletedOrders()
                    if (orArray.isNotEmpty()) {
                        try {
                            thread {
                                SendOrder(orArray) { }.execute()
                            }
                        } catch (ex: Exception) {
                            ErrorLog.writeLog(
                                this, this::class.java.simpleName, ex.message.toString()
                            )
                        }
                    }
                }

                for (button in buttonCollection) {
                    if (button.tag == MainButton.CompletedCounts.id) {
                        runOnUiThread {
                            button.text = String.format(
                                "%s%s(%s)",
                                MainButton.CompletedCounts.description,
                                System.getProperty("line.separator"),
                                itemArray.count()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun countPending(): Int {
        val currentDir = Statics.getPendingPath()
        val files = currentDir.listFiles() ?: return 0
        return files.count { t -> t.extension == "json" }
    }

    override fun scannerCompleted(scanCode: String) {
        if (settingViewModel.showScannedCode) makeText(binding.root, scanCode, INFO)

        JotterListener.lockScanner(this, true)

        try {
            /*
            runOnUiThread {
                run {
                    val checkCodeTask = CodeRead()
                    checkCodeTask.addParams(this, scanCode)
                    checkCodeTask.execute()
                }
            }
            */
        } catch (ex: Exception) {
            makeText(binding.root, ex.message.toString(), ERROR)
            ErrorLog.writeLog(this, this::class.java.simpleName, ex.message.toString())
        } finally {
            // Unless is blocked, unlock the partial
            JotterListener.lockScanner(this, false)
        }
    }

    private var rejectNewInstances = false

    private val buttonCollection: ArrayList<Button> = ArrayList()

    private var isReturnedFromSettings = false

    override fun onResume() {
        super.onResume()

        closeKeyboard(this)

        rejectNewInstances = false

        // Parece que las actividades de tipo Setting no devuelven resultados
        // así que de esta manera puedo volver a llenar el fragmento de usuarios
        if (isReturnedFromSettings) {
            isReturnedFromSettings = false

            // Vamos a reconstruir el scanner por si cambió la configuración
            JotterListener.autodetectDeviceModel(this)

            // Permitir o no la rotación de pantalla
            Statics.setScreenRotation(this)

            // Todavía no está loggeado
            if (Statics.currentUserId < 0L) {
                login()
            }
        }

        if (Statics.currentUserId > 0L) {
            startInboxOutboxListener()
        }
    }

    override fun onDestroy() {
        destroyLocals()
        super.onDestroy()
    }

    private fun destroyLocals() {
        pauseInboxOutboxListener()
    }

    private fun touchButton(motionEvent: MotionEvent, button: Button) {
        when (motionEvent.action) {
            MotionEvent.ACTION_UP -> {
                button.isPressed = false
                button.performClick()
            }
            MotionEvent.ACTION_DOWN -> {
                button.isPressed = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        if (!(settingViewModel.showConfButton)) {
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
                binding.userTextView.text =
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

        //region Configurar dispositivos de lectura de datos
        //endregion Configurar dispositivos de lectura de datos

        /// SETUP BUTTONS
        setupMainButton()
    }

    override fun onPause() {
        super.onPause()
        pauseInboxOutboxListener()
    }

    private fun pauseInboxOutboxListener() {
        Sync.stopTimer()
    }

    private fun clickButton(clickedButton: Button) {
        when (MainButton.getById(clickedButton.tag.toString().toLong())) {
            MainButton.NewCount -> {
                if (rejectNewInstances) return
                rejectNewInstances = true

                val intent = Intent(context, NewCountActivity::class.java)
                intent.putExtra("title", getString(R.string.new_count))
                resultForNewCount.launch(intent)
            }
            MainButton.PendingCounts -> {
                if (rejectNewInstances) return
                rejectNewInstances = true
                JotterListener.lockScanner(this, true)

                val intent = Intent(context, InboxActivity::class.java)
                intent.putExtra("title", getString(R.string.pending_counts))
                resultForPendingCount.launch(intent)
            }
            MainButton.CompletedCounts -> {
                if (rejectNewInstances) return
                rejectNewInstances = true

                val intent = Intent(context, OutboxActivity::class.java)
                intent.putExtra("title", getString(R.string.completed_counts))
                startActivity(intent)
            }
            MainButton.CodeRead -> {
                if (rejectNewInstances) return
                rejectNewInstances = true

                val intent = Intent(context, CodeCheckActivity::class.java)
                intent.putExtra("title", getString(R.string.code_read))
                intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
            MainButton.LinkItemCodes -> {
                if (rejectNewInstances) return
                rejectNewInstances = true

                try {
                    val intent = Intent(context, LinkCodeActivity::class.java)
                    intent.putExtra("title", getString(R.string.link_code))
                    startActivity(intent)
                } catch (ex: Exception) {
                    makeText(binding.root, "Error:" + ex.message, ERROR)
                }
            }
            MainButton.PrintItemLabel -> {
                if (rejectNewInstances) return
                rejectNewInstances = true
                JotterListener.lockScanner(this, true)

                try {
                    val intent = Intent(context, ItemSelectActivity::class.java)
                    intent.putExtra("title", getString(R.string.print_code))
                    intent.putExtra("multiSelect", true)
                    startActivity(intent)
                } catch (ex: Exception) {
                    makeText(binding.root, "Error:" + ex.message, ERROR)
                }
            }
            MainButton.Configuration -> {
                configApp()
            }
        }
    }

    private val resultForNewCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val client = Parcels.unwrap<Client>(data.getParcelableExtra("client"))
                    val description = data.getStringExtra("description")
                    val orderRequestType =
                        Parcels.unwrap<OrderRequestType>(data.getParcelableExtra<OrderRequestType>("orderRequestType"))
                    val log = com.dacosys.warehouseCounter.moshi.log.Log()

                    makeText(
                        binding.root, String.format(
                            getString(R.string.client_description),
                            client?.name ?: getString(R.string.no_client),
                            System.getProperty("line.separator"),
                            description
                        ), INFO
                    )

                    val r = Random()
                    val fakeOrderRequestId = r.nextInt(-888888 - -999999)

                    val or = OrderRequest(
                        orderRequestId = fakeOrderRequestId.toLong(),
                        clientId = client?.clientId ?: 0,
                        userId = Statics.currentUserId,
                        externalId = "",
                        creationDate = DateFormat.format(
                            "yyyy-MM-dd hh:mm:ss", System.currentTimeMillis()
                        ).toString(),
                        description = description ?: "",
                        zone = "",
                        orderRequestedType = orderRequestType,
                        resultDiffQty = true,
                        resultDiffProduct = true,
                        resultAllowDiff = true,
                        resultAllowMod = true,
                        completed = false,
                        startDate = DateFormat.format(
                            "yyyy-MM-dd hh:mm:ss", System.currentTimeMillis()
                        ).toString(),
                        finishDate = null,
                        content = ArrayList(),
                        documents = ArrayList(),
                        log = log
                    )

                    val intent = Intent(context, OrderRequestContentActivity::class.java)
                    intent.putExtra("orderRequest", Parcels.wrap(or))
                    intent.putExtra("isNew", true)

                    //start the second Activity
                    startActivity(intent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
        }

    private val resultForPendingCount =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it?.data
            try {
                if (it?.resultCode == RESULT_OK && data != null) {
                    val orArray: ArrayList<OrderRequest> =
                        data.getParcelableArrayListExtra("orderRequests")
                            ?: return@registerForActivityResult

                    if (orArray.isEmpty()) return@registerForActivityResult

                    try {
                        val or = orArray[0]
                        makeText(
                            binding.root, String.format(
                                getString(R.string.requested_count_state_),
                                if (equals(
                                        or.description, ""
                                    )
                                ) getString(R.string.no_description) else or.description,
                                if (or.completed != null && or.completed!!) getString(R.string.completed) else getString(
                                    R.string.pending
                                )
                            ), INFO
                        )

                        val intent = Intent(context, OrderRequestContentActivity::class.java)
                        intent.putExtra("orderRequest", Parcels.wrap(or))
                        intent.putExtra("isNew", false)

                        //start the second Activity
                        startActivity(intent)
                    } catch (ex: Exception) {
                        val res =
                            getString(R.string.an_error_occurred_while_trying_to_load_the_order)
                        makeText(binding.root, res, ERROR)
                        Log.e(this::class.java.simpleName, res)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                ErrorLog.writeLog(this, this::class.java.simpleName, ex)
            } finally {
                rejectNewInstances = false
            }
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
                startActivity(intent)
            }
            isReturnedFromSettings = true
        } else {
            makeText(binding.root, getString(R.string.invalid_password), ERROR)
        }
    }

    private lateinit var splashScreen: SplashScreen
    private lateinit var binding: ActivityHomeBinding

    private fun createSplashScreen() {
        // Set up 'core-splashscreen' to handle the splash screen in a backward compatible manner.
        splashScreen = installSplashScreen()
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        createSplashScreen()
        super.onCreate(savedInstanceState)
        Statics.setScreenRotation(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var freshSessionReq = false

        if (savedInstanceState != null) {
            // Recuperar el estado previo de la actividad con los datos guardados.
            if (Statics.currentUserId < 0L) {
                freshSessionReq = true
            } else {
                initSetup()
            }
        } else {
            // Primer inicialización
            freshSessionReq = true
        }

        if (freshSessionReq) {
            // Inicializar la actividad
            if (settingViewModel.urlPanel.isEmpty()) {
                makeText(binding.root, getString(R.string.server_is_not_configured), ERROR)
                setupInitConfig()
            } else {
                login()
            }
            return
        }

        // Set Listener for inbox and outbox orders
        startInboxOutboxListener()
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
                    makeText(binding.root, getString(R.string.server_is_not_configured), ERROR)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupButton(button: Button) {
        button.setOnClickListener {
            try {
                clickButton(button)
            } catch (ex: Exception) {
                makeText(
                    binding.root, "${getString(R.string.exception_error)}: " + ex.message, ERROR
                )
            }
        }
        button.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            touchButton(motionEvent, view as Button)
            return@OnTouchListener true
        })
    }

    @SuppressLint("DiscouragedPrivateApi") /// El campo mGradientState no es parte de la SDK
    private fun setupMainButton() {
        buttonCollection.add(binding.mainButton1)
        buttonCollection.add(binding.mainButton2)
        buttonCollection.add(binding.mainButton3)
        buttonCollection.add(binding.mainButton4)
        buttonCollection.add(binding.mainButton5)
        buttonCollection.add(binding.mainButton6)

        val allButtonMain = MainButton.getAllMain()
        for (i in buttonCollection.indices) {
            val b = buttonCollection[i]
            if (i < allButtonMain.count()) {
                val backColor: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ((b.background as StateListDrawable).current as GradientDrawable).color?.defaultColor
                        ?: R.color.white
                } else {
                    // Use reflection below API level 23
                    try {
                        val drawable =
                            (b.background as StateListDrawable).current as GradientDrawable
                        var field: Field = drawable.javaClass.getDeclaredField("mGradientState")
                        field.isAccessible = true
                        val myObj = field.get(drawable)
                        if (myObj == null) R.color.white
                        else {
                            field = myObj.javaClass.getDeclaredField("mSolidColors")
                            field.isAccessible = true
                            (field.get(myObj) as ColorStateList).defaultColor
                        }
                    } catch (e: NoSuchFieldException) {
                        e.printStackTrace()
                        R.color.white
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                        R.color.white
                    }
                }

                val textColor = Statics.getBestContrastColor("#" + Integer.toHexString(backColor))

                b.setTextColor(textColor)
                b.visibility = View.VISIBLE
                b.tag = allButtonMain[i].id
                b.text = allButtonMain[i].description
                b.textAlignment = View.TEXT_ALIGNMENT_VIEW_START

                if (allButtonMain[i].iconResource != null) {
                    b.setCompoundDrawablesWithIntrinsicBounds(
                        AppCompatResources.getDrawable(
                            this, allButtonMain[i].iconResource!!
                        ), null, null, null
                    )
                    b.compoundDrawables.filterNotNull().forEach {
                        it.colorFilter =
                            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                                ResourcesCompat.getColor(context.resources, R.color.white, null),
                                BlendModeCompat.SRC_IN
                            )
                    }
                }
                b.compoundDrawablePadding = 15
            } else {
                b.visibility = View.GONE
            }
        }

        for (a in buttonCollection) {
            setupButton(a)
        }
    }

    /**
     *
     * @param view      view that will be animated
     * @param duration  for how long in ms will it shake
     * @param offset    start offset of the animation
     * @return          returns the same view with animation properties
     */
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

    private fun startInboxOutboxListener() {
        TASK_CODE_SYNC_ORDERS = generateTaskCode()

        Thread {
            Sync.startTimer(this, this, TASK_CODE_SYNC_ORDERS)
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(Manifest.permission.BLUETOOTH_CONNECT)) JotterListener.onRequestPermissionsResult(
            this, requestCode, permissions, grantResults
        )
        else {
            when (requestCode) {
                REQUEST_EXTERNAL_STORAGE -> {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        makeText(
                            binding.root,
                            getString(R.string.cannot_write_to_external_storage),
                            ERROR
                        )
                    } else {
                        writeNewOrderRequest(newOrArray)
                    }
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
            val orJson = moshi.adapter(OrderRequest::class.java).toJson(newOrder)

            // Acá se comprueba si el ID ya existe y actualizamos la orden.
            // Si no se agrega una orden nueva.
            var alreadyExists = false
            if (pendingOrderArray.any()) {
                for (pendingOr in pendingOrderArray) {
                    if (pendingOr.orderRequestId == newOrder.orderRequestId) {
                        alreadyExists = true

                        if (newOrder.completed == true) {
                            // Está completada, eliminar localmente
                            val currentDir = Statics.getPendingPath()
                            val filePath =
                                currentDir.absolutePath + File.separator + pendingOr.filename
                            val fl = File(filePath)
                            if (!fl.delete()) isOk = false
                        } else {
                            // Actualizar contenido local
                            if (!updateOrder(origOrder = pendingOr, newOrder = newOrder)) isOk =
                                false
                        }

                        break
                    }
                }
            }

            if (!alreadyExists) {
                val orFileName = String.format("%s.json", df.format(Calendar.getInstance().time))

                if (!writeToFile(
                        fileName = orFileName, data = orJson, directory = Statics.getPendingPath()
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
            makeText(binding.root, res, SnackBarType.SUCCESS)
            Log.d(this::class.java.simpleName, res)
        } else {
            val res = getString(R.string.an_error_occurred_while_trying_to_save_the_count)
            makeText(binding.root, res, ERROR)
            Log.e(this::class.java.simpleName, res)
        }
    }

    private fun updateOrder(origOrder: OrderRequest, newOrder: OrderRequest): Boolean {
        var error = false

        try {
            val orJson = moshi.adapter(OrderRequest::class.java).toJson(newOrder)
            Log.i(this::class.java.simpleName, orJson)
            val orFileName = origOrder.filename.substringAfterLast('/')

            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                error = !Statics.writeJsonToFile(
                    v = binding.root,
                    filename = orFileName,
                    value = orJson,
                    completed = newOrder.completed ?: false
                )
                finish()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE
                )
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            Log.e(this::class.java.simpleName, e.message ?: "")
            error = true
        }

        return !error
    }

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 5001

        var TASK_CODE_SYNC_ORDERS = -1

        fun equals(a: Any?, b: Any): Boolean {
            return a != null && a == b
        }

        private var newOrArray: ArrayList<OrderRequest> = ArrayList()
    }

    override fun onNewOrderEvent(itemArray: ArrayList<OrderRequest>) {
        Log.d(
            this::class.java.simpleName,
            getString(R.string.new_orders_received_) + itemArray.count()
        )

        if (!Statics.isExternalStorageWritable) {
            Log.e(
                this::class.java.simpleName,
                getString(R.string.error_external_storage_not_available_for_reading_or_writing)
            )
            return
        }

        if (itemArray.isNotEmpty()) {
            if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                writeNewOrderRequest(itemArray)
            } else {
                newOrArray = itemArray
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL_STORAGE
                )
            }
        }

        for (button in buttonCollection) {
            if (button.tag == MainButton.PendingCounts.id) {
                runOnUiThread {
                    button.text = String.format(
                        "%s%s(%s)",
                        MainButton.PendingCounts.description,
                        System.getProperty("line.separator"),
                        countPending()
                    )

                    if (itemArray.isNotEmpty()) {
                        if (settingViewModel.shakeOnPendingOrders) {
                            shakeDevice()
                        }
                        if (settingViewModel.soundOnPendingOrders) {
                            playNotification()
                        }
                        shakeView(button, 20, 5)
                    }
                }
            }
        }
    }
}