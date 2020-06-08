package com.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import cn.com.heaton.blelibrary.ble.Ble
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import cn.com.heaton.blelibrary.ble.utils.ByteUtils

object AppProtocol {

    private val bleDeviceBleWriteCallback = object : BleWriteCallback<BleDevice>() {
        override fun onWriteSuccess(
            device: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {

            Log.e(
                "AppProtocol",
                "onWriteSuccess: ${ByteUtils.toString(characteristic?.value, "GBK")}"
            )
        }
    }

    private fun write(device: BleDevice, data: ByteArray) {
        val list = Ble.getInstance<BleDevice>().connetedDevices
        if (null != list && list.size > 0) {
            Ble.getInstance<BleDevice>().write(device, data, bleDeviceBleWriteCallback)
        }
    }

    /**
     * 获取电池电量
     */
    fun getBatteryCapacity(device: BleDevice) {
        val data = ByteArray(16)
        data[0] = 0x55.toByte()
        data[1] = 0x12.toByte()
        data[2] = 0x00.toByte()
        data[3] = 0xAA.toByte()
        write(device, data)
    }
}