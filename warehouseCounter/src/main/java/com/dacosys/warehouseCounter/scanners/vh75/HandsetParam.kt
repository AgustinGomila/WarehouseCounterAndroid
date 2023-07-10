package com.dacosys.warehouseCounter.scanners.vh75

/**
 * Device parameters
 */
class HandsetParam {
    var TagType: Byte = 0          //Label type: 01H-ISO18000-6B, 02H-EPCC1, 04H-EPCC1G2, 08H-EM4442.
    var Alarm: Byte = 0            //Bit0-bit7

    //   bit0:0-no alarm, 1-alarm
    //   bit1:0-do not use whitelist 1-Use whitelist.
    //   0/1
    var OutputMode: Byte = 0       //Data output mode:

    //   0-Save and direct output,
    //   1-Save but not directly output,
    //   2-Do not save but directly output
    var USBBaudRate: Byte = 0      //USB interface baud rate 04H--08H
    var Reserve5: Byte = 0         //reserved
    var Min_Frequence: Byte = 0    //The starting point of the frequency of the transmitted microwave signal,

    //   value: 1~63。
    var Max_Frequence: Byte = 0    //The starting point of the frequency of the transmitted microwave signal,

    //   value: 1~63。
    var Power: Byte = 0            //Transmission power value, value: 18-26。
    var RFhrdVer1: Byte = 0        //RF module hardware main version
    var RFhrdVer2: Byte = 0        //RF module hardware minor version
    var RFSoftVer1: Byte = 0       //RF module software main version
    var RFSoftVer2: Byte = 0       //RF module software minor version
    var ISTID: Byte = 0            //Whether to read the TID area
    var TIDAddr: Byte = 0          //TID read start position
    var TIDLen: Byte = 0           //TID read length
    var ISUSER: Byte = 0           //Do you read USER?
    var USERAddr: Byte = 0         //USER reads the starting position
    var USERLen: Byte = 0          //USER read length
    var Reserve19: Byte = 0        //vibration,0-NO，1-Vibrate
    var Reserve20: Byte = 0        //UHF module type,0---R2000, 1---VM5E
    var Reserve21: Byte = 0        //reserved
    var Reserve22: Byte = 0        //reserved
    var Reserve23: Byte = 0        //reserved
    var Reserve24: Byte = 0        //reserved
    var Reserve25: Byte = 0        //reserved
    var Reserve26: Byte = 0        //reserved
    var Reserve27: Byte = 0        //reserved
    var Reserve28: Byte = 0        //reserved
    var Reserve29: Byte = 0        //reserved
    var Reserve30: Byte = 0        //reserved
    var Reserve31: Byte = 0        //reserved
    var Reserve32: Byte = 0        //reserved

    val rfVersion: String
        get() = "Hardware version $RFhrdVer1.$RFhrdVer2, Software version $RFSoftVer1.$RFSoftVer2"

    val isAlarm: Boolean
        get() = Alarm.toInt() and 0x01 == 0x01 //00000001

    val isLabel: Boolean
        get() = Alarm.toInt() and 0x02 == 0x02 //00000010

    var isTID: Boolean
        get() = ISTID.toInt() != 0
        set(isTID) = if (isTID) {
            ISTID = 1
        } else {
            ISTID = 0
        }

    var isUSER: Boolean
        get() = ISUSER.toInt() != 0
        set(isUser) = if (isUser) {
            ISUSER = 1
        } else {
            ISUSER = 0
        }

    fun toBytes(): ByteArray {
        val data = ByteArray(32)
        var index = 0

        data[index++] = TagType
        data[index++] = Alarm
        data[index++] = OutputMode
        data[index++] = USBBaudRate
        data[index++] = Reserve5
        data[index++] = Min_Frequence
        data[index++] = Max_Frequence
        data[index++] = Power
        data[index++] = RFhrdVer1
        data[index++] = RFhrdVer2
        data[index++] = RFSoftVer1
        data[index++] = RFSoftVer2
        data[index++] = ISTID
        data[index++] = TIDAddr
        data[index++] = TIDLen
        data[index++] = ISUSER
        data[index++] = USERAddr
        data[index++] = USERLen
        data[index++] = Reserve19
        data[index++] = Reserve20
        data[index++] = Reserve21
        data[index++] = Reserve22
        data[index++] = Reserve23
        data[index++] = Reserve24
        data[index++] = Reserve25
        data[index++] = Reserve26
        data[index++] = Reserve27
        data[index++] = Reserve28
        data[index++] = Reserve29
        data[index++] = Reserve30
        data[index++] = Reserve31
        data[index++] = Reserve32

        return data
    }

    fun setAlarm(isAlarm: Boolean, isLabel: Boolean) {
        var alarm = 1
        if (!isAlarm) {
            alarm = 0
        }

        if (isLabel) {
            alarm += 2
        }

        Alarm = alarm.toByte()
    }

    override fun toString(): String {
        return "HandsetParam{TagType=$TagType, Alarm=$Alarm, OutputMode=$OutputMode, USBBaudRate=$USBBaudRate, Reserve5=$Reserve5, Min_Frequence=$Min_Frequence, Max_Frequence=$Max_Frequence, Power=$Power, RFhrdVer1=$RFhrdVer1, RFhrdVer2=$RFhrdVer2, RFSoftVer1=$RFSoftVer1, RFSoftVer2=$RFSoftVer2, ISTID=$ISTID, TIDAddr=$TIDAddr, TIDLen=$TIDLen, ISUSER=$ISUSER, USERAddr=$USERAddr, USERLen=$USERLen, Reserve19=$Reserve19, Reserve20=$Reserve20, Reserve21=$Reserve21, Reserve22=$Reserve22, Reserve23=$Reserve23, Reserve24=$Reserve24, Reserve25=$Reserve25, Reserve26=$Reserve26, Reserve27=$Reserve27, Reserve28=$Reserve28, Reserve29=$Reserve29, Reserve30=$Reserve30, Reserve31=$Reserve31, Reserve32=$Reserve32}"
    }
}
