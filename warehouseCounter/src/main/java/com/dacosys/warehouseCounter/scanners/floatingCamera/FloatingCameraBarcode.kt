package com.dacosys.warehouseCounter.scanners.floatingCamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.databinding.FloatingCameraActivityBinding
import com.dacosys.warehouseCounter.scanners.Scanner
import com.dacosys.warehouseCounter.ui.snackBar.MakeText.Companion.makeText
import com.dacosys.warehouseCounter.ui.snackBar.SnackBarType
import com.dacosys.warehouseCounter.ui.utils.Screen
import com.dacosys.warehouseCounter.ui.views.ScaleImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.BeepManager
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView.DecodeMode.CONTINUOUS
import com.journeyapps.barcodescanner.BarcodeView.DecodeMode.NONE
import com.journeyapps.barcodescanner.BarcodeView.DecodeMode.SINGLE
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.permission.PermissionUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class FloatingCameraBarcode(private var activity: AppCompatActivity) : BarcodeCallback,
    DecoratedBarcodeView.TorchListener {

    // Se utiliza exteriormente para conocer la actividad anfitriona de la ventana flotante
    var activityName: String = ""

    // Tag para EasyFloat
    private fun getEasyFloatTag() = activityName

    // Controlador de sonido
    private var beepManager: BeepManager? = null

    // region Variables de tamaño, posición y estado //
    private var floatWindowCreated = false
    private var floatWindowDisplayed = false

    private var flCameraMinWidth: Int = 0
    private var flCameraMinHeight: Int = 0

    private var allBarHeight: Int = 0
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0

    private lateinit var flCameraPortraitLoc: IntArray
    private var flCameraPortraitWidth: Int = 0
    private var flCameraPortraitHeight: Int = 0
    private lateinit var flCameraLandscapeLoc: IntArray
    private var flCameraLandscapeWidth: Int = 0
    private var flCameraLandscapeHeight: Int = 0

    private var continuousOn = false
    private var filterRepeatedReads = true

    private var isTorchOn = false
    private var lastScannedCode = ""
    // endregion ////////////////////////////////////

    // ViewBinding
    private var _binding: FloatingCameraActivityBinding? = null
    private val binding get() = _binding!!

    // Vista que usa EasyFloat como Layout
    private var v: View? = null

    private var getOrientation: Int = context.resources.configuration.orientation

    init {
        activityName = activity::class.java.simpleName
    }

    /**
     * Los nombres de las funciones van de acuerdo al momento del ciclo de vida
     * de la actividad anfitriona desde donde debe llamarse para su correcto funcionamiento.
     * onCreate, onResume, onPause, onDestroy
     *
     * onCreate no se llama externamente, se hace cuando se solicitan los permisos
     * que la clase necesita.
     */

    private fun onCreate() {
        loadValues()
        if (v == null) v = getView()

        EasyFloat.with(activity).setTag(getEasyFloatTag()).setShowPattern(ShowPattern.FOREGROUND)
            .setLocation(
                if (getOrientation == Configuration.ORIENTATION_PORTRAIT) flCameraPortraitLoc[0] else flCameraLandscapeLoc[0],
                if (getOrientation == Configuration.ORIENTATION_PORTRAIT) flCameraPortraitLoc[1] else flCameraLandscapeLoc[1]
            ).setAnimator(DefaultAnimator()).setLayout(v!!) { }.registerCallback {
                createResult { _, _, _ ->
                    createResult()
                }
                show {
                    binding.barcodeView.resume()
                }
                hide {
                    binding.barcodeView.pause()
                }
                dismiss {
                    floatWindowCreated = false
                    binding.barcodeView.pause()
                }
                dragEnd {
                    // Guardar la posición actual
                    if (getOrientation == Configuration.ORIENTATION_PORTRAIT) {
                        it.getLocationOnScreen(flCameraPortraitLoc)
                        //flCameraPortraitLoc[1] = flCameraPortraitLoc[1] - allBarHeight
                    } else {
                        it.getLocationOnScreen(flCameraLandscapeLoc)
                        //flCameraLandscapeLoc[1] = flCameraLandscapeLoc[1] - allBarHeight
                    }
                }
            }.show()

        floatWindowDisplayed = EasyFloat.isShow(getEasyFloatTag())
    }

    fun onResume() {
        if (_binding == null) return
        binding.barcodeView.resume()
        if (floatWindowDisplayed) showWindow()
    }

    fun onPause() {
        if (_binding == null) return
        binding.barcodeView.pause()
        hideWindow()
    }

    fun onDestroy() {
        filterTimer?.cancel()
        saveValues()
        EasyFloat.dismiss(getEasyFloatTag(), true)

        _binding = null
        v = null
    }

    private fun getView(): View {
        _binding = FloatingCameraActivityBinding.inflate(
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater),
            null,
            false
        )
        setLayout()
        return binding.root
    }

    private fun setLayout() {
        // Main Layout
        val content = binding.rlContent
        val params = FrameLayout.LayoutParams(
            // Restaurar el último tamaño
            if (getOrientation == Configuration.ORIENTATION_PORTRAIT) flCameraPortraitWidth
            else flCameraLandscapeWidth,
            if (getOrientation == Configuration.ORIENTATION_PORTRAIT) flCameraPortraitHeight
            else flCameraLandscapeHeight
        )
        content.layoutParams = params

        val ivFilterRepeat: ImageView = binding.ivFilterRepeat
        val ivRestoreWindow: ImageView = binding.ivRestoreWindow
        val ivScanMode = binding.ivScanMode
        val ivTorch = binding.ivTorch
        val ivClose = binding.ivClose
        val ivScale = binding.ivScale

        val barcodeScanner: DecoratedBarcodeView = binding.barcodeView
        val messageTv: TextView = binding.messageTextView

        //////////////////////////////////////////////////////////////
        // Iconos -hay que refrescar las imágenes por alguna razón- //
        ivFilterRepeat.setColorFilter(ContextCompat.getColor(activity, R.color.deepskyblue))
        ivScanMode.setColorFilter(ContextCompat.getColor(activity, R.color.deepskyblue))
        ivTorch.setColorFilter(ContextCompat.getColor(activity, R.color.deepskyblue))
        ivClose.setColorFilter(ContextCompat.getColor(activity, R.color.goldenrod))
        ivRestoreWindow.setColorFilter(ContextCompat.getColor(activity, R.color.goldenrod))
        ivScale.setColorFilter(ContextCompat.getColor(activity, R.color.goldenrod))

        ivFilterRepeat.setImageResource(R.drawable.ic_filter_on)
        ivScanMode.setImageResource(R.drawable.ic_barcode_scan)
        ivTorch.setImageResource(R.drawable.ic_flash_off)
        ivClose.setImageResource(R.drawable.ic_close_black)
        ivRestoreWindow.setImageResource(R.drawable.ic_restore_window)
        ivScale.setImageResource(R.drawable.ic_resize)
        //////////////////////////////////////////////////////////////

        // Scale Button
        ivScale.onScaledListener = object : ScaleImageView.OnScaledListener {
            override fun onScaled(x: Float, y: Float, event: MotionEvent) {
                params.width = min(max(params.width + x.toInt(), flCameraMinWidth), screenWidth)
                params.height = min(max(params.height + y.toInt(), flCameraMinHeight), screenHeight)

                // Guardar el tamaño actual
                if (getOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    flCameraPortraitWidth = params.width
                    flCameraPortraitHeight = params.height
                } else {
                    flCameraLandscapeWidth = params.width
                    flCameraLandscapeHeight = params.height
                }

                // Actualice el tamaño del diseño raíz xml
                content.layoutParams = params

                // Actualice el tamaño de la ventana flotante para evitar la limitación
                // de ancho cuando otras aplicaciones se proyectan horizontalmente
                EasyFloat.updateFloat(
                    getEasyFloatTag(), width = params.width, height = params.height
                )
            }
        }

        // Scan mode Button
        ivScanMode.setOnClickListener { v ->
            messageTv.visibility = GONE
            barcodeScanner.barcodeView.stopDecoding()

            continuousOn = !continuousOn
            if (continuousOn) {
                barcodeScanner.decodeContinuous(this)
                (v as ImageView).setImageResource(R.drawable.ic_multi_barcode_scan)
                ivFilterRepeat.setColorFilter(ContextCompat.getColor(activity, R.color.deepskyblue))
                v.contentDescription = context.getString(R.string.continuous_mode)
            } else {
                barcodeScanner.decodeSingle(this)
                (v as ImageView).setImageResource(R.drawable.ic_barcode_scan)
                ivFilterRepeat.setColorFilter(ContextCompat.getColor(activity, R.color.gray))
                v.contentDescription = context.getString(R.string.single_mode)
            }
        }

        // Torch Button
        ivTorch.setOnClickListener { v ->
            if (isTorchOn) {
                barcodeScanner.setTorchOff()
                (v as ImageView).setImageResource(R.drawable.ic_flash_off)
                v.contentDescription = context.getString(R.string.flash_off)
            } else {
                barcodeScanner.setTorchOn()
                (v as ImageView).setImageResource(R.drawable.ic_flash_on)
                v.contentDescription = context.getString(R.string.flash_on)
            }
        }

        // Filter Button
        ivFilterRepeat.setOnClickListener { v ->
            filterRepeatedReads = !filterRepeatedReads
            if (filterRepeatedReads) {
                (v as ImageView).setImageResource(R.drawable.ic_filter_on)
                v.contentDescription = context.getString(R.string.filter_repeated_reads)
            } else {
                (v as ImageView).setImageResource(R.drawable.ic_filter_off)
                v.contentDescription = context.getString(R.string.allow_repeated_reads)
            }
        }

        // Restore Button
        ivRestoreWindow.setOnClickListener {
            params.width = getDefaultWidth()
            params.height = getDefaultHeight()

            // Guardar el tamaño actual
            if (getOrientation == Configuration.ORIENTATION_PORTRAIT) {
                flCameraPortraitWidth = params.width
                flCameraPortraitHeight = params.height
            } else {
                flCameraLandscapeWidth = params.width
                flCameraLandscapeHeight = params.height
            }

            // Actualice el tamaño del diseño raíz xml
            content.layoutParams = params

            // Actualice el tamaño de la ventana flotante para evitar la limitación
            // de ancho cuando otras aplicaciones se proyectan horizontalmente
            EasyFloat.updateFloat(getEasyFloatTag(), width = params.width, height = params.height)
        }

        // Close Button
        ivClose.setOnClickListener {
            toggleWindowVisibility()
        }

        if (continuousOn) {
            ivScanMode.setImageResource(R.drawable.ic_multi_barcode_scan)
            ivScanMode.contentDescription = context.getString(R.string.continuous_mode)
            ivFilterRepeat.setColorFilter(ContextCompat.getColor(activity, R.color.deepskyblue))
        } else {
            ivScanMode.setImageResource(R.drawable.ic_barcode_scan)
            ivScanMode.contentDescription = context.getString(R.string.single_mode)
            ivFilterRepeat.setColorFilter(ContextCompat.getColor(activity, R.color.gray))
        }

        if (isTorchOn) {
            ivTorch.setImageResource(R.drawable.ic_flash_on)
            ivTorch.contentDescription = context.getString(R.string.flash_on)
        } else {
            ivTorch.setImageResource(R.drawable.ic_flash_off)
            ivTorch.contentDescription = context.getString(R.string.flash_off)
        }

        // Beep manager
        beepManager = BeepManager(activity)
    }

    private fun getDefaultHeight(): Int {
        return if (getOrientation == Configuration.ORIENTATION_PORTRAIT) {
            settingsVm.flCameraPortraitHeight
        } else {
            settingsVm.flCameraLandscapeHeight
        }
    }

    private fun getDefaultWidth(): Int {
        return if (getOrientation == Configuration.ORIENTATION_PORTRAIT) {
            settingsVm.flCameraPortraitWidth
        } else {
            settingsVm.flCameraLandscapeHeight
        }
    }

    private fun loadValues() {
        val sv = settingsVm
        allBarHeight = Screen.getSystemBarsHeight(activity)
        screenHeight = Screen.getScreenHeight(activity)
        screenWidth = Screen.getScreenWidth(activity)

        // Cargar la información de posición y tamaño de la ventana flotante
        flCameraMinWidth = sv.flCameraPortraitWidth
        flCameraMinHeight = sv.flCameraPortraitHeight

        flCameraPortraitLoc = intArrayOf(sv.flCameraPortraitLocX, sv.flCameraPortraitLocY)
        flCameraPortraitWidth = min(sv.flCameraPortraitWidth, screenWidth)
        flCameraPortraitHeight = min(sv.flCameraPortraitHeight, screenHeight)
        flCameraLandscapeLoc = intArrayOf(sv.flCameraLandscapeLocX, sv.flCameraLandscapeLocY)
        flCameraLandscapeWidth = min(sv.flCameraLandscapeWidth, screenWidth)
        flCameraLandscapeHeight = min(sv.flCameraLandscapeHeight, screenHeight)

        continuousOn = sv.flCameraContinuousMode
        filterRepeatedReads = sv.flCameraFilterRepeatedReads
    }

    private fun saveValues() {
        if (!floatWindowCreated) return

        // Guardar datos de la ventana flotante
        settingsVm.flCameraPortraitLocX = flCameraPortraitLoc[0]
        settingsVm.flCameraPortraitLocY = flCameraPortraitLoc[1]
        settingsVm.flCameraPortraitWidth = flCameraPortraitWidth
        settingsVm.flCameraPortraitHeight = flCameraPortraitHeight
        settingsVm.flCameraLandscapeLocX = flCameraLandscapeLoc[0]
        settingsVm.flCameraLandscapeLocY = flCameraLandscapeLoc[1]
        settingsVm.flCameraLandscapeWidth = flCameraLandscapeWidth
        settingsVm.flCameraLandscapeHeight = flCameraLandscapeHeight
        settingsVm.flCameraContinuousMode = continuousOn
        settingsVm.flCameraFilterRepeatedReads = filterRepeatedReads
    }

    private fun checkCameraFloatingPermission() {
        // Check if the storage permission has been granted
        if (ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is missing and must be requested.
            resultForCameraPermission.launch(Manifest.permission.CAMERA)
            return
        }

        checkFloatingPermission()
    }

    private fun checkFloatingPermission() {
        if (PermissionUtils.checkPermission(activity)) {
            onCreate()
        } else {
            android.app.AlertDialog.Builder(activity)
                .setMessage(context.getString(R.string.to_use_the_floating_window_function_you_must_authorize_the_floating_window_permission))
                .setPositiveButton(R.string.ok) { _, _ ->
                    onCreate()
                }.setNegativeButton(R.string.cancel) { _, _ -> }.show()
        }
    }

    private val resultForCameraPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // returns boolean representing whether the
            // permission is granted or not
            if (!isGranted) {
                showSnackBar(
                    context.getString(R.string.app_dont_have_necessary_permissions), SnackBarType.ERROR
                )
            } else {
                checkFloatingPermission()
            }
        }

    private fun showSnackBar(text: String, snackBarType: SnackBarType) {
        makeText(activity.window.decorView, text, snackBarType)
    }

    fun hideWindow() {
        if (floatWindowCreated && EasyFloat.isShow(getEasyFloatTag())) EasyFloat.hide(
            getEasyFloatTag()
        )
    }

    private fun showWindow() {
        if (floatWindowCreated && !EasyFloat.isShow(getEasyFloatTag())) EasyFloat.show(
            getEasyFloatTag()
        )
    }

    fun toggleWindowVisibility() {
        if (floatWindowCreated) {
            if (EasyFloat.isShow(getEasyFloatTag())) EasyFloat.hide(getEasyFloatTag())
            else EasyFloat.show(getEasyFloatTag())

            floatWindowDisplayed = EasyFloat.isShow(getEasyFloatTag())
            return
        }

        try {
            checkCameraFloatingPermission()
        } catch (ex: Exception) {
            showSnackBar("Error: ${ex.message}", SnackBarType.ERROR)
            ex.message
        }
    }

    private fun createResult() {
        val barcodeScanner: DecoratedBarcodeView = binding.barcodeView
        val messageTv: TextView = binding.messageTextView
        val barcodePreview: ImageView = binding.barcodePreview
        val lastCodeScannedTv: TextView = binding.lastCodeScanned

        // Barcode camera scanner view
        val sv = settingsVm
        val formats: ArrayList<BarcodeFormat> = ArrayList()
        if (sv.symbologyPDF417) formats.add(BarcodeFormat.PDF_417)
        if (sv.symbologyAztec) formats.add(BarcodeFormat.AZTEC)
        if (sv.symbologyQRCode) formats.add(BarcodeFormat.QR_CODE)
        if (sv.symbologyCODABAR) formats.add(BarcodeFormat.CODABAR)
        if (sv.symbologyCode128) formats.add(BarcodeFormat.CODE_128)
        if (sv.symbologyCode39) formats.add(BarcodeFormat.CODE_39)
        if (sv.symbologyCode93) formats.add(BarcodeFormat.CODE_93)
        if (sv.symbologyDataMatrix) formats.add(BarcodeFormat.DATA_MATRIX)
        if (sv.symbologyEAN13) formats.add(BarcodeFormat.EAN_13)
        if (sv.symbologyEAN8) formats.add(BarcodeFormat.EAN_8)
        if (sv.symbologyMaxiCode) formats.add(BarcodeFormat.MAXICODE)
        if (sv.symbologyRSS14) formats.add(BarcodeFormat.RSS_14)
        if (sv.symbologyRSSExpanded) formats.add(BarcodeFormat.RSS_EXPANDED)
        if (sv.symbologyUPCA) formats.add(BarcodeFormat.UPC_A)
        if (sv.symbologyUPCE) formats.add(BarcodeFormat.UPC_E)

        // Last scanned code
        lastCodeScannedTv.text = lastScannedCode
        messageTv.visibility = GONE

        // Clean the last image captured at click on the view
        barcodePreview.setOnClickListener { barcodePreview.setImageDrawable(null) }
        barcodeScanner.setOnClickListener {
            if (!continuousOn) {
                // Si está en modo simple de disparo, al tocar en la imagen
                // se inicia una nueva lectura.
                if (barcodeScanner.barcodeView.decodeMode == NONE) {
                    messageTv.visibility = GONE
                    barcodeScanner.barcodeView.stopDecoding()
                    barcodeScanner.decodeSingle(this@FloatingCameraBarcode)
                }
            } else {
                // Si está en modo contínuo, al tocar en la imagen
                // se detiene o inicia una nueva lectura.
                if (barcodeScanner.barcodeView.decodeMode == CONTINUOUS) {
                    barcodeScanner.barcodeView.stopDecoding()
                    messageTv.text = context.getString(R.string.touch_the_image_to_start_reading)
                    messageTv.visibility = VISIBLE
                } else if (barcodeScanner.barcodeView.decodeMode == NONE) {
                    messageTv.visibility = GONE
                    barcodeScanner.decodeContinuous(this@FloatingCameraBarcode)
                }
            }
        }
        barcodeScanner.setTorchListener(this@FloatingCameraBarcode)

        barcodeScanner.decoderFactory = DefaultDecoderFactory(formats)
        barcodeScanner.initializeFromIntent(activity.intent)

        if (isTorchOn) barcodeScanner.setTorchOn()
        else barcodeScanner.setTorchOff()

        if (continuousOn) barcodeScanner.decodeContinuous(this@FloatingCameraBarcode)
        else barcodeScanner.decodeSingle(this@FloatingCameraBarcode)

        barcodeScanner.resume()

        floatWindowCreated = true
    }

    // region Filter scanner results
    private var isFilterRunning: AtomicBoolean = AtomicBoolean(false)
    private var filterTimer: CountDownTimer? = null
    private fun startFilterTimer(ms: Long = 1200) {
        if (filterTimer != null) filterTimer?.cancel()
        isFilterRunning.set(true)
        filterTimer = object : CountDownTimer(ms, ms) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                isFilterRunning.set(false)
            }
        }
        filterTimer?.start()
    }
    // endregion Filter scanner results

    override fun barcodeResult(result: BarcodeResult) {
        if (result.text == null) return

        // Filtramos las lecturas repetidas en modo continuo si está puesto el filtro.
        if (result.text == lastScannedCode && filterRepeatedReads && continuousOn) return

        // Si NO están filtradas las repeticiones en modo contínuo igual eliminamos las lecturas
        // repetidas dentro de un tiempo determinado.
        if (!filterRepeatedReads && continuousOn && isFilterRunning.get()) return

        // Iniciar el Timer de filtrado en Modo continuo.
        if (continuousOn) startFilterTimer()

        // Guardar último código escaneado
        lastScannedCode = result.text

        // Emitir sonido y vibrar
        beepManager?.playBeepSoundAndVibrate()

        // Enviar el código recibido a la actividad anfitriona
        (activity as Scanner.ScannerListener?)?.scannerCompleted(lastScannedCode)

        // Added preview of scanned barcode if not in continuous mode
        if (!continuousOn) binding.barcodePreview.setImageBitmap(result.getBitmapWithResultPoints(R.color.goldenrod))

        // Mostrar último código escaneado en la vista
        binding.lastCodeScanned.text = lastScannedCode

        // Bloquear nuevamente si está en Modo de disparo simple
        if (binding.barcodeView.barcodeView.decodeMode == SINGLE) {
            binding.messageTextView.text =
                context.getString(R.string.touch_the_image_to_start_reading)
            binding.messageTextView.visibility = VISIBLE
        }
    }

    override fun onTorchOn() {
        isTorchOn = true
    }

    override fun onTorchOff() {
        isTorchOn = false
    }
}
