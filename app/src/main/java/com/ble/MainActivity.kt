package com.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import cn.com.heaton.blelibrary.ble.Ble
import cn.com.heaton.blelibrary.ble.callback.*
import cn.com.heaton.blelibrary.ble.model.BleDevice
import cn.com.heaton.blelibrary.ble.utils.ByteUtils
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUESTCODE: Int = 0x01

    private lateinit var mDevice: BleDevice
    private lateinit var mBle: Ble<BleDevice>
    private var listDatas = mutableListOf<BleDevice>()
    private val adapter = DeviceAdapter(listDatas)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查权限
        requestBLEPermission()

        // 初始化视图
        initView()

        // 初始化点击事件
        initListener()

        // 初始化BLE
        initBLE()
    }

    // 点击事件监听
    private fun initListener() {
        scan.setOnClickListener {
            checkBluetoothStatus()
        }
        sendData.setOnClickListener {
            val list = mBle.connetedDevices
            for (device in list) {
                Ble.getInstance<BleDevice>().writeStrByUuid(
                    device,
                    "0x55 0x12 0x00 0xAA",
                    UUID.fromString("0000FFE5-0000-1000-8000-00805f9b34fb"),
                    UUID.fromString("0000FFE9-0000-1000-8000-00805f9b34fb"),
                    writeCallback
                )
            }
        }
        readData.setOnClickListener {
            ble_info.visibility = View.VISIBLE
            val list = mBle.connetedDevices
            for (device in list) {
                Ble.getInstance<BleDevice>().readByUuid(
                    device,
                    UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb"),
                    UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb"),
                    bleReadCallback
                )
            }
        }
    }

    // 数据写入回调
    private val writeCallback = object : BleWriteCallback<BleDevice>() {
        override fun onWriteSuccess(
            device: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.e(
                "writeCallback",
                "onWriteSuccess: ${ByteUtils.toString(characteristic?.value, "GBK")}"
            )
            ThreadUtils.ui {
                toast("数据写入成功>>>>>:${ByteUtils.toString(characteristic?.value, "GBK")}")
            }
        }

    }

    // 初始化视图
    private fun initView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener {
            val device = adapter.items[it]
            device.apply {
                if (isConnected) {
                    mBle.disconnect(this)
                } else if (!isConnectting) {
                    mBle.connect(this, connectCallback)
                }
            }
        }
    }

    // 读取数据回调
    private val bleReadCallback = object : BleReadCallback<BleDevice>() {
        override fun onReadSuccess(
            dedvice: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onReadSuccess(dedvice, characteristic)
            ble_info.text =
                "生产厂商： ${ByteUtils.toString(characteristic?.value, "GBK")}"
        }

        override fun onReadFailed(device: BleDevice?, failedCode: Int) {
            super.onReadFailed(device, failedCode)
            ThreadUtils.ui {
                toast("读取蓝牙信息失败>>>>>:$failedCode")
            }
        }
    }

    // 连接回调
    private val connectCallback = object : BleConnectCallback<BleDevice>() {
        override fun onConnectionChanged(device: BleDevice?) {
            adapter.notifyDataSetChanged()
        }

        override fun onConnectException(device: BleDevice?, errorCode: Int) {
            super.onConnectException(device, errorCode)
            toast("连接异常，异常状态码:$errorCode")
        }

        override fun onConnectTimeOut(device: BleDevice?) {
            super.onConnectTimeOut(device)
            toast("连接异常，异常状态码:${device?.bleName}")
        }

        override fun onReady(device: BleDevice?) {
            super.onReady(device)
            if (device != null) mDevice = device

            mBle.enableNotifyByUuid(
                device,
                true,
                UUID.fromString("0000FFE0-0000-1000-8000-00805f9b34fb"),
                UUID.fromString("0000FFE4-0000-1000-8000-00805f9b34fb"),
                bleNotifyCallback
            )
        }
    }

    // 通知回调
    private val bleNotifyCallback = object : BleNotiftCallback<BleDevice>() {
        override fun onChanged(
            device: BleDevice?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            ThreadUtils.ui {
                toast("收到硬件数据>>>>>:${ByteUtils.bytes2HexStr(characteristic?.value)}")
            }
            Log.e("收到硬件数据>>>>>onChanged:", ByteUtils.bytes2HexStr(characteristic?.value))
        }

    }

    //初始化蓝牙
    private fun initBLE() {
        mBle = Ble.options().apply {
            logTAG = "BluetoothLE"
            logBleEnable = true
            throwBleException = true
            autoConnect = true
            connectFailedRetryCount = 3
            connectTimeout = 10000L
            scanPeriod = 12000L
            uuidService = UUID.fromString("0000FFE5-0000-1000-8000-00805f9b34fb")
            uuidWriteCha = UUID.fromString("0000FFE9-0000-1000-8000-00805f9b34fb")
        }.create(applicationContext)
        //3、检查蓝牙是否支持及打开
        checkBluetoothStatus()


    }

    //检查蓝牙是否支持及打开
    private fun checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!mBle.isSupportBle(this)) {
            toast("该设备不支持BLE蓝牙")
            finish()
        }
        if (!mBle.isBleEnable) {
            //4、若未打开，则请求打开蓝牙
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT)
        } else {
            //5、若已打开，则进行扫描
            mBle.startScan(bleScanCallback)
        }
    }

    // 扫描回调
    private val bleScanCallback = object : BleScanCallback<BleDevice>() {
        override fun onLeScan(device: BleDevice?, rssi: Int, scanRecord: ByteArray?) {
            for (d in listDatas) {
                if (d.bleAddress == device?.bleAddress) return
            }
            device?.let {
                if (it.bleName != null)
                    listDatas.add(it)
                adapter.notifyDataSetChanged()
            }
        }
    }

    // 请求蓝牙权限
    private fun requestBLEPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUESTCODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    val p = shouldShowRequestPermissionRationale(permissions[0])
                    if (!p) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                        toast("请手动开启蓝牙权限")
                    } else
                        finish()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val i = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    toast("请手动开启蓝牙权限")
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mBle.disconnectAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBle.disconnectAll()
    }
}
