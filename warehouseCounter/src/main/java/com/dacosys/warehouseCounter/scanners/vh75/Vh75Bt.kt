package com.dacosys.warehouseCounter.scanners.vh75

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.core.app.ActivityCompat
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.settingsVm
import com.dacosys.warehouseCounter.scanners.rfid.Rfid
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.inv


class Vh75Bt(private var listener: RfidDeviceListener?) : Rfid() {

    private val tag = this::class.java.simpleName

    /**
     * Constructor. Prepares a new Vh75Bt session.     *
     * BuilderVH75 Contains both the Listener and the Context
     */
    // Member fields
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mConnectedThread: ConnectedThread? = null

    /**
     * Return the current connection mState.
     */
    @get:Synchronized
    var mState: Int = 0

    private var mNewState: Int = 0
    private val mDestroy = AtomicBoolean(false)

    @Volatile
    private var reconnectAttempts = 0

    init {
        mState = STATE_NONE
        mDestroy.set(false)

        mNewState = mState

        pairDevice()
    }

    fun setListener(listener: RfidDeviceListener?) {
        this.listener = listener
    }

    private fun pairDevice() {
        val sv = settingsVm
        val btAddress = sv.rfidBtAddress
        if (btAddress.isEmpty()) {
            return
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pairedDevices = mAdapter.bondedDevices
        Log.v(tag, "pairDevice: Total devices: ${pairedDevices.size}")
        if (pairedDevices.size > 0) {
            var device: BluetoothDevice? = null
            for (d in pairedDevices.toTypedArray()) {
                if (d.address == btAddress) {
                    device = d
                    break
                }
            }

            if (device == null) {
                return
            }

            Log.v(tag, "pairDevice: Connecting to ${device.name}")

            // Start the thread to connect with the given device
            connect(device)
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mDevice: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocketWrapper? = null

        init {
            var tmp: BluetoothSocket? = null

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    tmp = mDevice.createRfcommSocketToServiceRecord(uuid)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket create() failed", e)
            }

            if (tmp != null) {
                mmSocket = NativeBluetoothSocket(tmp)
                mState = STATE_CONNECTING
            }
        }

        override fun run() {
            Log.v(TAG, "BEGIN mConnectThread")

            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            var failed = false
            try {
                // Always cancel discovery because it will slow down a connection
                mAdapter.cancelDiscovery()

                // Make a connection to the BluetoothSocket
                mmSocket?.connect()
            } catch (e: InterruptedException) {
                try {
                    mmSocket?.close()
                } catch (e1: IOException) {
                    Log.e(TAG, "Unable to close() socket", e)
                }
                mmSocket = null

                currentThread().interrupt()
                Log.v(TAG, "END mConnectThread (InterruptedException)")
                return
            } catch (e: IOException) {
                try {
                    Log.v(TAG, "Error al conectar Socket, reintentando...")

                    mmSocket = FallbackBluetoothSocket(mmSocket?.underlyingSocket!!)
                    sleep(500)
                    mmSocket?.connect()

                    /*
                        mmSocket = mDevice.javaClass.getMethod
                            ("createRfcommSocket",
                             Int::class.javaPrimitiveType!!)
                            .invoke(mDevice, 1) as BluetoothSocket
                        mmSocket?.connect()
                        */
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to connect() to socket", e)

                    // Close the socket
                    try {
                        mmSocket?.close()
                    } catch (e2: IOException) {
                        Log.e(TAG, "Unable to close() socket during connection failure", e2)
                    } finally {
                        mmSocket = null

                        connectionFailed()
                        failed = true
                    }
                }
            }

            if (failed) return

            if (listener != null) {
                listener!!.onDeviceName(mDevice.name)
            }

            // Start the connected thread
            connected(mmSocket!!.underlyingSocket!!)
        }
    }

    /**
     * Report state according to the current mState of the connection
     */
    @Synchronized
    private fun reportState() {
        mState = getState()

        /*
        val STATE_NONE = 0
        val STATE_LISTEN = 1
        val STATE_CONNECTING = 2
        val STATE_CONNECTED = 3
        */

        var mNewStateStr = ""
        when (mNewState) {
            STATE_CONNECTED -> mNewStateStr = "STATE_CONNECTED"
            STATE_NONE -> mNewStateStr = "STATE_NONE"
            STATE_CONNECTING -> mNewStateStr = "STATE_CONNECTING"
        }

        var mStateStr = ""
        when (mState) {
            STATE_CONNECTED -> mStateStr = "STATE_CONNECTED"
            STATE_NONE -> mStateStr = "STATE_NONE"
            STATE_CONNECTING -> mStateStr = "STATE_CONNECTING"
        }

        Log.v(TAG, "reportState() $mNewStateStr ($mNewState) -> $mStateStr ($mState)")
        mNewState = mState

        if (mNewState == STATE_CONNECTED) {
            val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen1.startTone(ToneGenerator.TONE_DTMF_S, 150)
            // RFID Device connected SUCCESS!
        }
        if (listener != null) {
            listener!!.onStateChanged(mNewState)
        }
    }

    /**
     * Return the current connection state.
     */
    @Synchronized
    fun getState(): Int {
        return mState
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    @Synchronized
    fun connect(device: BluetoothDevice) {
        Log.v(TAG, "Connect to: $device")

        destroy()

        // Start the thread to connect with the given device
        val conn = ConnectThread(device)
        conn.start()

        reportState()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The Socket that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket) {
        Log.v(TAG, "Connected to Socket")

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.interrupt()
            mConnectedThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, MODE_CONTINUOS_READ)
        mConnectedThread!!.start()

        reportState()

        // Leer la configuración del dispositivo.
        // El resultado de la lectura dispara el evento onRead y este
        // se procesará en Rfid.processMessage, si el resultado es correcto
        // se reconfigurará el dispositivo con los parámetros de configuración del usuario

        TimeUnit.SECONDS.sleep(1)
        readConfigParam()
    }

    /**
     * Destroy all threads
     */
    @Synchronized
    fun destroy() {
        if (mConnectedThread != null) {
            mConnectedThread!!.finish()
            mConnectedThread = null
        }

        reportState()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    private fun write(out: ByteArray) {
        // Create a temporary object
        val r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the writing in an unsynchronized way
        r?.write(out)
    }

    fun pause() {
        // Create a temporary object
        val r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }

        if (r != null) {
            if (r.mThreadMode != MODE_PAUSE) {
                r.mOldThreadMode = r.mThreadMode

                r.mThreadMode = MODE_PAUSE
                reportMode(MODE_PAUSE)
            }
        }
    }

    fun resume() {
        // Create a temporary object
        val r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }

        if (r != null) {
            if (r.mThreadMode == MODE_PAUSE) {
                r.mThreadMode = r.mOldThreadMode
                reportMode(r.mThreadMode)
            }
        }
    }

    @Synchronized
    private fun reportMode(currentMode: Int) {
        Log.v(TAG, "reportMode() ${getThreadModeDescription(currentMode)} ($currentMode)")
    }

    private fun getThreadModeDescription(currentMode: Int): String {/*
        val MODE_PAUSE = 0
        val MODE_CONTINUOS_READ = 1
        val MODE_FIRST_READ_UNTIL_TIMEOUT = 2
        val MODE_ALL_READ_UNTIL_TIMEOUT = 3
        */

        var currentModeStr = ""
        when (currentMode) {
            MODE_PAUSE -> currentModeStr = "MODE_PAUSE"
            MODE_CONTINUOS_READ -> currentModeStr = "MODE_CONTINUOS_READ"
            MODE_FIRST_READ_UNTIL_TIMEOUT -> currentModeStr = "MODE_FIRST_READ_UNTIL_TIMEOUT"
            MODE_ALL_READ_UNTIL_TIMEOUT -> currentModeStr = "MODE_ALL_READ_UNTIL_TIMEOUT"
        }

        return currentModeStr
    }

    private var timeOut = 3000
    private var dataToWrite = ""
    fun writeTag(data: String): Boolean {
        dataToWrite = data

        // Create a temporary object
        val r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return false
            r = mConnectedThread
        }

        try {
            // 1. Al enviar este comando, obligo al thread de lectura continua a recibir una respuesta
            // desde el dispositivo, terminar el ciclo del bucle en que se encuentra la ejecución del thread
            // e iniciar un nuevo ciclo.
            getVersion()

            // Semáforo que desvía el thread de lectura continua hacia el bucle de lectura única con TimeOut
            r!!.mThreadMode = MODE_FIRST_READ_UNTIL_TIMEOUT
            reportMode(r.mThreadMode)

            // 2. Comando de solicitud de la lista de TAG detectados
            listTagID(1, 0, 0)
        } catch (e: java.lang.Exception) {
            Log.e(tag, "writeTag: Error reading InputStream. ${e.message}")
        }

        return true
    }

    private fun processReadingAndWriteTag(res: ByteArray) {
        // Este proceso es llamado únicamente desde el thread de lectura única con TimeOut
        // Recibe la información del tag leído y llama a la función de escritura

        TimeUnit.SECONDS.sleep(1)

        if (checkSuccess(res)) {
            val count = res[3]
            if (count > 0) {
                val len = res[4]
                val epcId = res.copyOfRange(5, (5 + len * 2))
                writeData(epcId, dataToWrite)
            }
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        Log.e(tag, "connectionFailed -> Unable to connect device")

        mState = STATE_NONE
        reportState()

        reconnect()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        Log.e(tag, "connectionLost -> Device connection was lost")

        mState = STATE_NONE
        reportState()

        reconnect()
    }

    private fun reconnect() {
        if (reconnectAttempts < 3) {
            reconnectAttempts++
            Log.v(
                TAG, "${context.getString(R.string.searching_rfid_reader)} ($reconnectAttempts)..."
            )

            destroy()
            pairDevice()
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, threadMode: Int) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        @Volatile
        var mThreadMode = MODE_PAUSE

        @Volatile
        var mOldThreadMode = MODE_PAUSE

        init {
            Log.v(TAG, "Create ConnectedThread")
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "Temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED

            reconnectAttempts = 0
            mThreadMode = threadMode
        }

        override fun run() {
            Log.v(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED && !mDestroy.get()) {
                while (mThreadMode == MODE_PAUSE && !mDestroy.get()) {
                }

                if (mDestroy.get()) {
                    return
                }

                if (mThreadMode == MODE_FIRST_READ_UNTIL_TIMEOUT) {
                    // Bucle de lectura única con TimeOut
                    try {
                        // Keep listening to the InputStream until reach timeout
                        val startTime = System.currentTimeMillis()

                        while ((System.currentTimeMillis() - startTime) < timeOut) {
                            if (mThreadMode == MODE_PAUSE || mDestroy.get()) {
                                // Termina el bucle actual.
                                // Al iniciar el siguiente, se detendrá en pausa o
                                // terminará el proceso, según corresponda
                                break
                            }

                            // Read from the InputStream
                            try {
                                if (mmInStream != null) {
                                    val available = mmInStream.available()
                                    if (available > 0) {
                                        // Read from the InputStream
                                        bytes = mmInStream.read(buffer)

                                        if (mThreadMode == MODE_PAUSE || mDestroy.get()) {
                                            // Termina el bucle actual.
                                            // Al iniciar el siguiente, se detendrá en pausa o
                                            // terminará el proceso, según corresponda
                                            break
                                        }

                                        if (bytes >= 3) {
                                            Log.v(
                                                TAG,
                                                "${getThreadModeDescription(mThreadMode)} Command Code: " + CommandCode.getByCode(
                                                    buffer[2]
                                                )
                                            )
                                            processReadingAndWriteTag(buffer.copyOfRange(0, bytes))
                                            break
                                        }
                                    }
                                }
                            } catch (e: InterruptedException) {
                                currentThread().interrupt()
                            } catch (e: IOException) {
                                Log.e(
                                    tag,
                                    "${getThreadModeDescription(mThreadMode)}: Error reading InputStream. ${e.message}"
                                )
                                break
                            }
                        }
                    } catch (e: InterruptedException) {
                        currentThread().interrupt()
                    } finally {
                        mThreadMode = MODE_CONTINUOS_READ
                        reportMode(mThreadMode)
                        continue
                    }
                } else if (mThreadMode == MODE_CONTINUOS_READ) {
                    // Bucle de lectura continua
                    try {
                        // Read from the InputStream
                        bytes = mmInStream?.read(buffer) ?: 0

                        while (mThreadMode == MODE_PAUSE || mDestroy.get()) {
                            // Termina el bucle actual.
                            // Al iniciar el siguiente, se detendrá en pausa o
                            // terminará el proceso, según corresponda
                            continue
                        }

                        if (bytes > 0) {
                            if (bytes >= 3) {
                                val commandCode = buffer[2]
                                val commandStr = CommandCode.getByCode(commandCode)
                                Log.v(
                                    TAG, "${getThreadModeDescription(mThreadMode)} Command Code: $commandStr"
                                )
                            }
                            if (listener != null) {
                                listener!!.onRead(buffer, bytes)
                            }
                        }
                    } catch (e: InterruptedException) {
                        currentThread().interrupt()
                        return
                    } catch (e: IOException) {
                        if (!mDestroy.get()) {
                            Log.e(TAG, "Disconnected by exception: ", e)
                            connectionLost()
                        }
                        break
                    } finally {
                    }
                } else if (mThreadMode == MODE_CONTINUOS_READ) {
                    // Bucle de multiples lecturas hasta el TimeOut
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray) {
            if (buffer.count() >= 3) {
                val commandCode = buffer[2]
                val commandStr = CommandCode.getByCode(commandCode)
                Log.v(TAG, "Writing Command Code: $commandStr(${commandCode.toInt()})")
            }

            try {
                mmOutStream?.write(buffer)
                if (listener != null) {
                    listener!!.onWrite(buffer)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)

                mmSocket.close()
                mmOutStream?.close()
                mmInStream?.close()

                mState = STATE_NONE
                reportState()

                reconnect()
            }
        }

        fun finish() {
            try {
                // 1. Dejar de escuchar los eventos
                Log.v(TAG, "ConnectedThread -> Destroying listener and context...")
                listener = null

                // 2. Detener el Thread
                Log.v(TAG, "ConnectedThread -> Interrupting connected thread...")

                mState = STATE_NONE
                mThreadMode = MODE_PAUSE
                mDestroy.set(true)

                interrupt()

                // 3. Cerrar el socket
                Log.v(TAG, "ConnectedThread -> Closing socket...")
                mmInStream?.close()
                mmOutStream?.close()
                mmSocket.close()

                Log.v(TAG, "ConnectedThread FINISHED")
            } catch (e: IOException) {
                Log.e(TAG, "finish() failed", e)
            }
        }
    }

    companion object {
        // Debugging
        private const val TAG = "Vh75Bt"

        // Unique UUID for this application
        private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Constants that indicate the current connection mState
        const val STATE_NONE = 0       // we're doing nothing
        const val STATE_CONNECTING = 1 // now initiating an outgoing connection
        const val STATE_CONNECTED = 2  // now connected to a remote device

        // Contiene los estados de ejecución del Thread de lectura
        const val MODE_PAUSE = 0       // Pausar lecturas
        const val MODE_CONTINUOS_READ = 1 // Lectura continua
        const val MODE_FIRST_READ_UNTIL_TIMEOUT = 2  // Primera lectura antes del TimeOut
        const val MODE_ALL_READ_UNTIL_TIMEOUT = 3  // Todas las lecturas antes del TimeOut

        fun checkSuccess(data: ByteArray): Boolean {
            var commandCode = "Unknown command"
            if (data.count() > 2) {
                val v = CommandCode.getByCode(data[2])
                if (v != null) {
                    commandCode = v.description
                } else {
                    return false
                }
            }

            when {
                data[0] == Head.RECEIVE_OK.code -> Log.v(
                    TAG, "checkSuccess: $commandCode ${Head.RECEIVE_OK.name} ${
                        Utility.bytes2HexStringWithSeparator(data)
                    }"
                )

                data[0] == Head.RECEIVE_FAIL.code -> Log.e(
                    TAG, "checkSuccess: $commandCode ${Head.RECEIVE_FAIL.name} ${
                        Utility.bytes2HexStringWithSeparator(data)
                    }"
                )

                data[0] == Head.SEND.code -> Log.v(
                    TAG, "checkSuccess: $commandCode ${Head.SEND.name} ${
                        Utility.bytes2HexStringWithSeparator(data)
                    }"
                )

                data[0] == 0x00.toByte() -> Log.v(
                    TAG, "checkSuccess: $commandCode OK ${Utility.bytes2HexStringWithSeparator(data)}"
                )

                else -> Log.v(
                    TAG, "checkSuccess: $commandCode Unknown Result ${
                        Utility.bytes2HexStringWithSeparator(data)
                    }"
                )
            }

            return data[0] != Head.RECEIVE_FAIL.code
        }

        fun processMessage(listener: RfidDeviceListener, data: ByteArray, bytes: Int) {
            val ret2 = data.copyOfRange(0, bytes)
            if (ret2.count() < 4) {
                // Si tiene menos de 4 bytes no es correcta la información
                return
            }

            val epcLen = 12
            Log.v(TAG, "processMessage: ${Utility.bytes2HexStringWithSeparator(ret2)}")

            // Cuando se escanea un tag con el botón del dispositivo devuelve estos primeros 12 bytes:
            // 000055AA-13031B04-03030406
            // 000055AA-13031B04-03020406
            // 000055AA-13031B04-070D0406
            // 000055AA-13031B04-090B0406-00000000-00000000-00000000
            // <HEADER>-<     FECHA   ><L><          DATOS         >
            // 000055AA-13031B16-14210406
            // 000055AA-13031B16-15340406
            // 000055AA-13031B16-1E220406

            // Cabecera que se obtiene cuando se hace una lectura presionando el botón del dispositivo
            val expectedHeader = Utility.convert2HexArray("000055AA")

            val header = ret2.copyOfRange(0, 4)
            if (header.contentEquals(expectedHeader)) {
                // Convertir a String y enviar los códigos capturados
                val epcId = getEpcID(ret2, 12, (12 + epcLen))
                if (epcId.isNotEmpty()) {
                    if (epcId.contains('�')) {
                        Log.e(TAG, "Scanned code contains unrecognized token")
                    }

                    // Lectura completa!
                    listener.onReadCompleted(epcId)
                }
            } else {
                var bootCode = ret2[0]
                val len = ret2[1] // Length of last 3 parts
                val commandCode = ret2[2]
                val dataSegment = getDataSegment(ret2)
                var checkSum = ret2.last()

                val cSuc = checkSuccess(ret2)
                if (cSuc) {
                    Log.v(TAG, "CommandCode: ${CommandCode.getByCode(commandCode)} OK")
                } else {
                    Log.e(TAG, "CommandCode: ${CommandCode.getByCode(commandCode)} FAIL")
                }

                // Clasificar las respuestas según el tipo de comando
                when (commandCode) {
                    CommandCode.WriteWordBlock.code -> {
                        // Escritura completa!
                        listener.onWriteCompleted(cSuc)
                    }

                    CommandCode.ReadHandsetParam.code -> {
                        // Lectura de parámetros de configuración!
                        if (cSuc) {
                            (rfidDevice as Vh75Bt).setConfigParameters(ret2)
                        }
                    }
                }
            }
        }

        private fun getDataSegment(data: ByteArray): ByteArray? {
            val len = data[1].toInt()
            if (len == 2) { // Command + Checksum
                Log.v(TAG, "Data segment is empty")
                return null
            }

            var dataSegment: ByteArray? = null
            if (len > 2) {
                dataSegment = ByteArray(len - 2)

                System.arraycopy(data, 3, dataSegment, 0, len - 2)
                Log.v(TAG, "Data segment is " + Utility.bytes2HexString(dataSegment))
            }
            return dataSegment
        }

        private fun getEpcID(ret: ByteArray, from: Int, to: Int): String {
            return try {
                return Utility.bytes2String(ret.copyOfRange(from, to))
            } catch (e: Exception) {
                Log.e(TAG, "getEpcID: Error decoding message. ${e.message}")
                ""
            }
        }
    }

    // region SOME Specific VH75 Functions
    private fun genCommand(cmd: CommandCode, param: ByteArray?): ByteArray {
        var len = 1 + 1 // cmd + checksum
        if (param != null) {
            len += param.size // param
        }

        val buffer = ByteBuffer.allocate(len + 2)
        buffer.put(Head.SEND.code).put(len.toByte()).put(cmd.code)
        if (param != null) {
            buffer.put(param)
        }

        val checksum = crc(buffer.array(), len + 1)
        buffer.put(checksum)

        return buffer.array()
    }

    private fun crc(data: ByteArray, len: Int): Byte {
        var checksum: Byte = 0
        for (i in 0 until len) {
            checksum = (checksum + data[i]).toByte()
        }
        checksum = Utility.toByte(checksum.inv().toInt())
        checksum = Utility.toByte(checksum + 1)

        return Utility.toByte(checksum.toInt())
    }

    fun resetToFactory() {
        write(genCommand(cmd = CommandCode.ReadFactoryParameter, param = null))
    }

    fun setBluetoothName(name: String) {
        write(genCommand(cmd = CommandCode.SetBluetoothName, param = name.toByteArray()))
    }

    fun getBluetoothName() {
        write(genCommand(cmd = CommandCode.GetBluetoothName, param = null))
    }

    private fun readConfigParam() {
        write(genCommand(cmd = CommandCode.ReadHandsetParam, param = null))
    }

    fun setReaderMode(nMode: Byte) {
        write(genCommand(cmd = CommandCode.SetReaderMode, param = byteArrayOf(nMode)))
    }

    private fun getVersion() {
        write(genCommand(cmd = CommandCode.GetVersion, param = null))
    }

    private fun listTagID(mem: Int, address: Int, len: Int) {
        write(genCommandListTagID(mem = mem, address = address, len = len, mask = byteArrayOf()))
    }

    fun setConfigParameters(ret2: ByteArray) {
        val sv = settingsVm
        val param = parseReadParamResult(ret2)
        Log.v(
            TAG,
            "Read RFID Config Parameters: TagType: ${param.TagType} Alarm: ${param.Alarm} Vibration: ${param.Reserve19} Power: ${param.Power} MinFrequence: ${param.Min_Frequence} MaxFrequence: ${param.Max_Frequence}"
        )

        val power = sv.rfidReadPower
        val tagType = 4 //04H－ISO-18000-6C
        val alarm = sv.rfidPlaySoundOnRead
        val shock = sv.rfidShockOnRead

        param.Power = power.toByte()
        param.Alarm = if (alarm) 1.toByte() else 0.toByte()
        param.Reserve19 = if (shock) 1.toByte() else 0.toByte()
        param.TagType = tagType.toByte()
        param.Min_Frequence = 1.toByte()
        param.Max_Frequence = 76.toByte()

        Log.v(
            TAG,
            "Write RFID Config Parameters: TagType: ${param.TagType} Alarm: ${param.Alarm} Vibration: ${param.Reserve19} Power: ${param.Power} MinFrequence: ${param.Min_Frequence} MaxFrequence: ${param.Max_Frequence}"
        )

        (rfidDevice as Vh75Bt).write(genCommand(CommandCode.WriteHandlerParam, param.toBytes()))
    }

    private fun genCommandListTagID(mem: Int, address: Int, len: Int, mask: ByteArray): ByteArray {
        val param: ByteArray
        if (len == 0) {
            param = ByteArray(4)
            param[0] = mem.toByte()
        } else {
            var i = 0
            val m: Int = if (len % 8 == 0) {
                len / 8
            } else {
                len / 8 + 1
            }

            param = ByteArray(m + 4)
            param[0] = mem.toByte()
            param[1] = (address shr 8).toByte()
            param[2] = (address and 0xFF).toByte()
            param[3] = len.toByte()

            while (i < m) {
                param[4 + i] = mask[i]
                i++
            }
        }

        return genCommand(CommandCode.ListTag, param)
    }

    private var defaultPassword = "00000000"
    private fun writeData(epcId: ByteArray, data: String): Boolean {
        val dataHex = genHexString(data)
        val epcIdHex = genHexString(epcId)

        Log.v(TAG, "writeData: OLD EpcId: $epcIdHex - NEW EpcId: $data ($dataHex)")

        val mem = 1 // EPC
        val tagDataAddress = 0

        try {
            writeWordBlock(
                epc = epcIdHex, mem = mem, address = tagDataAddress, data = dataHex, password = defaultPassword
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun genHexString(bytes: ByteArray): String {
        var dataHex = ""
        for (b in bytes) {
            dataHex += String.format("%02X", b)
        }

        return dataHex
    }

    private fun genHexString(data: String): String {
        val maxLength = 12

        var d = data
        if (data.length > maxLength) {
            d = data.substring(0, maxLength)
        }

        var dataHex = ""
        val charArray = d.toCharArray()
        for (i in charArray) {
            dataHex += Integer.toHexString(i.code)
        }

        while (dataHex.length < (maxLength * 2)) {
            dataHex += "20"
        }

        return dataHex
    }

    private fun writeWordBlock(
        epc: String,
        mem: Int,
        address: Int,
        data: String,
        password: String,
    ) {
        // L EPC mem address len data AccessPassword
        // 1 12 1 1 1 len 4
        val epcLen = epc.length / 2
        val dataLen = data.length / 2
        val paramSize = 1 + epcLen + 1 + 1 + 1 + dataLen + 4
        val param = ByteArray(paramSize)

        val bEpc = Utility.convert2HexArray(epc)
        param[0] = (epcLen / 2).toByte()
        System.arraycopy(bEpc, 0, param, 1, epcLen) // epc

        param[1 + epcLen] = mem.toByte()
        param[1 + epcLen + 1] = address.toByte()

        val bData = Utility.convert2HexArray(data)
        param[1 + epcLen + 2] = (dataLen / 2).toByte()
        System.arraycopy(bData, 0, param, 1 + epcLen + 2 + 1, dataLen)

        val bPassword = Utility.convert2HexArray(password)
        param[4 + epcLen + dataLen] = bPassword[0]
        param[4 + epcLen + dataLen + 1] = bPassword[1]
        param[4 + epcLen + dataLen + 2] = bPassword[2]
        param[4 + epcLen + dataLen + 3] = bPassword[3]

        (rfidDevice as Vh75Bt).write(genCommand(CommandCode.WriteWordBlock, param))
    }

    private fun parseReadParamResult(data: ByteArray): HandsetParam {
        val param = HandsetParam()
        var index = 3

        param.TagType = data[index++]
        param.Alarm = data[index++]
        param.OutputMode = data[index++]
        param.USBBaudRate = data[index++]
        param.Reserve5 = data[index++]
        param.Min_Frequence = data[index++]
        param.Max_Frequence = data[index++]
        param.Power = data[index++]
        param.RFhrdVer1 = data[index++]
        param.RFhrdVer2 = data[index++]
        param.RFSoftVer1 = data[index++]
        param.RFSoftVer2 = data[index++]
        param.ISTID = data[index++]
        param.TIDAddr = data[index++]
        param.TIDLen = data[index++]
        param.ISUSER = data[index++]
        param.USERAddr = data[index++]
        param.USERLen = data[index++]
        param.Reserve19 = data[index++]
        param.Reserve20 = data[index++]
        param.Reserve21 = data[index++]
        param.Reserve22 = data[index++]
        param.Reserve23 = data[index++]
        param.Reserve24 = data[index++]
        param.Reserve25 = data[index++]
        param.Reserve26 = data[index++]
        param.Reserve27 = data[index++]
        param.Reserve28 = data[index++]
        param.Reserve29 = data[index++]
        param.Reserve30 = data[index++]
        param.Reserve31 = data[index++]
        param.Reserve32 = data[index]

        return param
    }
    // endregion SOME Specific VH75 Functions

    interface BluetoothSocketWrapper {
        @get:Throws(IOException::class)
        val inputStream: InputStream?

        @get:Throws(IOException::class)
        val outputStream: OutputStream?

        val remoteDeviceName: String?

        @Throws(IOException::class)
        fun connect()

        val remoteDeviceAddress: String?

        @Throws(IOException::class)
        fun close()

        val underlyingSocket: BluetoothSocket?
    }

    open class NativeBluetoothSocket(override val underlyingSocket: BluetoothSocket) : BluetoothSocketWrapper {
        @get:Throws(IOException::class)
        override val inputStream: InputStream
            get() = underlyingSocket.inputStream

        @get:Throws(IOException::class)
        override val outputStream: OutputStream
            get() = underlyingSocket.outputStream

        override val remoteDeviceName: String
            get() = if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ""
            } else {
                underlyingSocket.remoteDevice.name.toString()
            }

        @Throws(IOException::class)
        override fun connect() {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            underlyingSocket.connect()
        }

        override val remoteDeviceAddress: String
            get() = underlyingSocket.remoteDevice.address

        @Throws(IOException::class)
        override fun close() {
            underlyingSocket.close()
        }
    }

    class FallbackBluetoothSocket(tmp: BluetoothSocket) : NativeBluetoothSocket(tmp) {
        private var fallbackSocket: BluetoothSocket? = null

        @get:Throws(IOException::class)
        override val inputStream: InputStream
            get() = fallbackSocket!!.inputStream

        @get:Throws(IOException::class)
        override val outputStream: OutputStream
            get() = fallbackSocket!!.outputStream

        @Throws(IOException::class)
        override fun connect() {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fallbackSocket!!.connect()
        }

        @Throws(IOException::class)
        override fun close() {
            fallbackSocket!!.close()
        }

        init {
            fallbackSocket = try {
                val clazz: Class<*> = tmp.remoteDevice.javaClass
                val paramTypes = arrayOf<Class<*>>(Integer.TYPE)
                val m = clazz.getMethod("createRfcommSocket", *paramTypes)
                val params = arrayOf<Any>(Integer.valueOf(1))
                m.invoke(tmp.remoteDevice, *params) as BluetoothSocket
            } catch (e: java.lang.Exception) {
                throw FallbackException(e)
            }
        }
    }

    class FallbackException(e: java.lang.Exception?) : java.lang.Exception(e) {
        companion object {
            private const val SERIAL_VERSION_UID = 1L
        }
    }
}