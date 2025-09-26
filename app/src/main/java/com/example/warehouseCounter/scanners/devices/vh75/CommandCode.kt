package com.example.warehouseCounter.scanners.devices.vh75

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class CommandCode : Parcelable {
    var code: Byte = 0x00
    var description: String = ""

    constructor(commandCodeId: Byte, description: String) {
        this.description = description
        this.code = commandCodeId
    }

    override fun toString(): String {
        return description
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is CommandCode) {
            false
        } else this.code == other.code
    }

    override fun hashCode(): Int {
        return this.code.toInt()
    }

    constructor(parcel: Parcel) {
        code = parcel.readByte()
        description = parcel.readString().orEmpty()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(code)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CommandCode> {
        override fun createFromParcel(parcel: Parcel): CommandCode {
            return CommandCode(parcel)
        }

        override fun newArray(size: Int): Array<CommandCode?> {
            return arrayOfNulls(size)
        }

        private var SaveLabelToSDCard = CommandCode(0x01.toByte(), "SaveLabelToSDCard")
        var GetVersion: CommandCode = CommandCode(0x02.toByte(), "GetVersion")
        private var AddLabelID = CommandCode(0x03.toByte(), "AddLabelID")
        private var DelLabelID = CommandCode(0x04.toByte(), "DelLabelID")
        private var GetLabelID = CommandCode(0x05.toByte(), "GetLabelID")
        var ReadHandsetParam: CommandCode = CommandCode(0x06.toByte(), "ReadHandsetParam")
        private var SetReportFilter = CommandCode(0x07.toByte(), "SetReportFilter")
        private var GetReportFilter = CommandCode(0x08.toByte(), "GetReportFilter")
        var WriteHandlerParam: CommandCode = CommandCode(0x09.toByte(), "WriteHandlerParam")

        var SetReaderMode: CommandCode = CommandCode(0x0b.toByte(), "SetReaderMode")
        private var WriteFactoryParam = CommandCode(0x0c.toByte(), "WriteFactoryParam")
        var ReadFactoryParameter: CommandCode = CommandCode(0x0d.toByte(), "ReadFactoryParameter")
        private var SetReaderTime = CommandCode(0x11.toByte(), "SetReaderTime")
        private var GetReaderTime = CommandCode(0x12.toByte(), "GetReaderTime")
        private var GetRecord = CommandCode(0x16.toByte(), "GetRecord")
        private var DeleteAllRecord = CommandCode(0x17.toByte(), "DeleteAllRecord")
        private var SetHandsetID = CommandCode(0x8b.toByte(), "SetHandsetID")
        private var GetHandsetID = CommandCode(0x8c.toByte(), "GetHandsetID")
        var SetBluetoothName: CommandCode = CommandCode(0x8d.toByte(), "SetBluetoothName")
        var GetBluetoothName: CommandCode = CommandCode(0x8e.toByte(), "GetBluetoothName")
        private var SetBtBaudRate = CommandCode(0x8f.toByte(), "SetBtBaudRate")
        private var GetBtBaudRate = CommandCode(0x90.toByte(), "GetBtBaudRate")
        private var SetLock = CommandCode(0xea.toByte(), "SetLock")
        var WriteWordBlock: CommandCode = CommandCode(0xeb.toByte(), "WriteWordBlock")
        private var ReadWordBlock = CommandCode(0xec.toByte(), "ReadWordBlock")
        private var GetIdList = CommandCode(0xed.toByte(), "GetIdList")
        var ListTag: CommandCode = CommandCode(0xee.toByte(), "ListTag")
        private var BlockLock = CommandCode(0xe6.toByte(), "BlockLock")
        private var WriteEpc = CommandCode(0xe7.toByte(), "WriteEpc")
        private var KillTag = CommandCode(0xe8.toByte(), "KillTag")
        private var EraseBlock = CommandCode(0xe9.toByte(), "EraseBlock")
        private var InvalidCode = CommandCode((-1).toByte(), "InvalidCode")
        private var PressedButton = CommandCode(0x55.toByte(), "PressedButton")

        fun getAll(): ArrayList<CommandCode> {
            val allSections = ArrayList<CommandCode>()
            Collections.addAll(
                allSections,
                PressedButton,

                SaveLabelToSDCard,
                GetVersion,
                AddLabelID,
                DelLabelID,
                GetLabelID,
                ReadHandsetParam,
                SetReportFilter,
                GetReportFilter,
                WriteHandlerParam,
                SetReaderMode,
                WriteFactoryParam,
                ReadFactoryParameter,
                SetReaderTime,
                GetReaderTime,
                GetRecord,
                DeleteAllRecord,
                SetBtBaudRate,
                GetBtBaudRate,
                GetHandsetID,
                SetHandsetID,
                GetBluetoothName,
                SetBluetoothName,
                ReadWordBlock,
                WriteWordBlock,
                SetLock,
                EraseBlock,
                KillTag,
                WriteEpc,
                BlockLock,
                ListTag,
                GetIdList,
                InvalidCode
            )

            return ArrayList(allSections.sortedWith(compareBy { it.code }))
        }

        fun getByCode(commandCodeId: Byte): CommandCode? {
            return getAll().firstOrNull { it.code == commandCodeId }
        }
    }
}