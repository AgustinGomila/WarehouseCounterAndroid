package com.dacosys.warehouseCounter.scanners.nfc

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dacosys.warehouseCounter.R
import com.dacosys.warehouseCounter.WarehouseCounterApp.Companion.context
import com.dacosys.warehouseCounter.ui.activities.codeCheck.CodeCheckActivity
import com.dacosys.warehouseCounter.ui.activities.common.ObservationActivity
import com.dacosys.warehouseCounter.ui.activities.common.QtySelectorActivity
import com.dacosys.warehouseCounter.ui.activities.linkCode.LinkCodeActivity
import com.dacosys.warehouseCounter.ui.activities.orderRequest.OrderRequestContentActivity
import com.dacosys.warehouseCounter.ui.utils.ParcelUtils.parcelable
import kotlin.experimental.or

/**
 * Created by Agustin on 28/11/2017.
 */

object Nfc {

    private val tag = this::class.java.simpleName

    private var mNfcAdapter: NfcAdapter? = null

    fun setupNFCReader(targetActivity: AppCompatActivity?) {
        if (targetActivity == null) return

        mNfcAdapter = NfcAdapter.getDefaultAdapter(targetActivity)

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Log.d(
                tag,
                context.getString(R.string.this_device_doesnt_support_NFC)
            )
            return
        }

        if (!mNfcAdapter!!.isEnabled) {
            Log.d(tag, context.getString(R.string.NFC_is_disabled))
        } else {
            Log.d(tag, context.getString(R.string.NFC_is_enabled))
        }

        nfcHandleIntent(targetActivity.intent, targetActivity)
    }

    fun nfcHandleIntent(intent: Intent, targetActivity: AppCompatActivity?) {
        if (targetActivity == null) return

        val action = intent.action
        if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {
            var tag: Tag? = intent.parcelable(NfcAdapter.EXTRA_TAG)
            tag = patchTag(tag)

            val id = byte2HexString(tag!!.id)

            // Chequear que sea el tipo de actividad deseada
            when (targetActivity) {
                is CodeCheckActivity -> (targetActivity as? CodeCheckActivity)?.scannerCompleted(id)
                    ?: (targetActivity as? CodeCheckActivity)?.scannerCompleted(id)

                is LinkCodeActivity -> (targetActivity as? LinkCodeActivity)?.scannerCompleted(id)
                    ?: (targetActivity as? LinkCodeActivity)?.scannerCompleted(id)

                is OrderRequestContentActivity -> (targetActivity as? OrderRequestContentActivity)?.scannerCompleted(
                    id
                )
                    ?: (targetActivity as? OrderRequestContentActivity)?.scannerCompleted(id)

                is ObservationActivity -> (targetActivity as? ObservationActivity)?.scannerCompleted(
                    id
                )
                    ?: (targetActivity as? ObservationActivity)?.scannerCompleted(id)

                is QtySelectorActivity -> (targetActivity as? QtySelectorActivity)?.scannerCompleted(
                    id
                )
                    ?: (targetActivity as? QtySelectorActivity)?.scannerCompleted(id)
            }
        }
    }

    /**
     * Convert an array of bytes into a string of hex values.
     *
     * @param bytes Bytes to convert.
     * @return The bytes in hex string format.
     */
    private fun byte2HexString(bytes: ByteArray?): String {
        val ret = StringBuilder()
        if (bytes != null) {
            for (b in bytes) {
                ret.append(String.format("%02X", b.toInt() and 0xFF))
            }
        }
        return ret.toString()
    }

    /**
     * Patch a possibly broken Tag object of HTC One (m7/m8) or Sony
     * Xperia Z3 devices (with Android 5.x.)
     *
     *
     * HTC One: "It seems, the reason of this bug is TechExtras of NfcA is null.
     * However, TechList contains MifareClassic." -- Bildin.
     * This method will fix this. For more information, please refer to
     * https://github.com/ikarus23/MifareClassicTool/issues/52
     * This patch was provided by bildin (https://github.com/bildin).
     *
     *
     * Sony Xperia Z3 (+ emulated MIFARE Classic tag): The buggy tag has
     * two NfcA in the TechList with different SAK values and a MifareClassic
     * (with the Extra of the second NfcA). Both the second NfcA and the
     * MifareClassic technique have a SAK of 0x20. According to NXP's
     * guidelines on identifying MIFARE tags (Page 11), this is a MIFARE Plus or
     * MIFARE DESFire tag. This method creates a new Extra with the SAK
     * values of both NfcA occurrences ORed (as mentioned in NXP's
     * MIFARE type identification procedure guide) and replaces the Extra of
     * the first NfcA with the new one. For more information, please refer to
     * https://github.com/ikarus23/MifareClassicTool/issues/64
     * This patch was provided by bildin (https://github.com/bildin).
     *
     * @param tag The possibly broken tag.
     * @return The fixed tag.
     */
    private fun patchTag(tag: Tag?): Tag? {
        if (tag == null) {
            return null
        }

        val techList = tag.techList

        val oldParcel = Parcel.obtain()
        tag.writeToParcel(oldParcel, 0)
        oldParcel.setDataPosition(0)

        val len = oldParcel.readInt()
        var id = ByteArray(0)
        if (len >= 0) {
            id = ByteArray(len)
            oldParcel.readByteArray(id)
        }
        val oldTechList = IntArray(oldParcel.readInt())
        oldParcel.readIntArray(oldTechList)
        val oldTechExtras = oldParcel.createTypedArray(Bundle.CREATOR)
        val serviceHandle = oldParcel.readInt()
        val isMock = oldParcel.readInt()
        val tagService: IBinder? = if (isMock == 0) {
            oldParcel.readStrongBinder()
        } else {
            null
        }
        oldParcel.recycle()

        var nfcaIdx = -1
        var mcIdx = -1
        var sak: Short = 0
        var isFirstSak = true

        for (i in techList.indices) {
            if (techList[i] == NfcA::class.java.name) {
                if (nfcaIdx == -1) {
                    nfcaIdx = i
                }

                if (oldTechExtras != null) {
                    if (oldTechExtras[i] != null && oldTechExtras[i].containsKey("sak")) {
                        sak = (sak or oldTechExtras[i].getShort("sak"))
                        isFirstSak = nfcaIdx == i
                    }
                }
            } else if (techList[i] == MifareClassic::class.java.name) {
                mcIdx = i
            }
        }

        var modified = false

        // Patch the double NfcA issue (with different SAK) for
        // Sony Z3 devices.
        if (!isFirstSak) {
            if (oldTechExtras != null) {
                oldTechExtras[nfcaIdx].putShort("sak", sak)
                modified = true
            }
        }

        // Patch the wrong index issue for HTC One devices.
        if (oldTechExtras != null) {
            if (nfcaIdx != -1 && mcIdx != -1 && oldTechExtras[mcIdx] == null) {
                oldTechExtras[mcIdx] = oldTechExtras[nfcaIdx]
                modified = true
            }
        }

        if (!modified) {
            // The Old tag was not modified. Return the old one.
            return tag
        }

        // The Old tag was modified. Create a new tag with the new data.
        val newParcel = Parcel.obtain()
        newParcel.writeInt(id.size)
        newParcel.writeByteArray(id)
        newParcel.writeInt(oldTechList.size)
        newParcel.writeIntArray(oldTechList)
        newParcel.writeTypedArray(oldTechExtras, 0)
        newParcel.writeInt(serviceHandle)
        newParcel.writeInt(isMock)
        if (isMock == 0) {
            newParcel.writeStrongBinder(tagService)
        }
        newParcel.setDataPosition(0)
        val newTag = Tag.CREATOR.createFromParcel(newParcel)
        newParcel.recycle()

        return newTag
    }

    /**
     * Enables the NFC foreground dispatch system for the given Activity.
     */
    fun enableNfcForegroundDispatch(targetActivity: AppCompatActivity?) {
        if (targetActivity == null) return

        if (mNfcAdapter != null && mNfcAdapter!!.isEnabled) {
            val intent = Intent(
                targetActivity,
                targetActivity.javaClass
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val pendingIntent = PendingIntent.getActivity(
                targetActivity,
                0,
                intent,
                FLAG_IMMUTABLE
            )

            mNfcAdapter!!.enableForegroundDispatch(
                targetActivity,
                pendingIntent, null,
                arrayOf(arrayOf(NfcA::class.java.name))
            )
        }
    }

    /**
     * Disable the NFC foreground dispatch system for the given Activity.
     */
    fun disableNfcForegroundDispatch(targetActivity: AppCompatActivity?) {
        if (targetActivity == null) return

        if (mNfcAdapter != null && mNfcAdapter!!.isEnabled) {
            mNfcAdapter!!.disableForegroundDispatch(targetActivity)
        }
    }
}
