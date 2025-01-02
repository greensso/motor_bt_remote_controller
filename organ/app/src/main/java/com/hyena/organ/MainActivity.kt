package com.hyena.organ

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.hyena.organ.R.*
import com.hyena.organ.R.drawable.*
import com.hyena.organ.ui.theme.OrganTheme
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID
import kotlin.concurrent.thread

/**
 * 我的设备
 */
data class MyDevice(val device: BluetoothDevice, var rssi: Int)


class MainActivity : ComponentActivity() {
    private var myBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var myPairedDevices: Set<BluetoothDevice>
    private val requestEnableBlue = 1
    private var isSupportBlue = true
    private var bluetoothName : String = ""
    private var bluetoothAddress : String = ""
    var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var mBluetoothSocket: BluetoothSocket? = null
    private var isBlueConnected = false

  //  lateinit var binding : ActivityMainBinding

    private fun isOpenBluetooth(): Boolean {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter ?: return false
        return adapter.isEnabled
    }

    private fun hasPermission(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun isAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    //请求定位权限意图
    private val requestLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
            } else {
                showMsg("Android12以下，6及以上需要定位权限才能扫描设备")
            }
        }

    //请求BLUETOOTH_CONNECT权限意图
    private val requestBluetoothConnect =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //打开蓝牙
                enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                showMsg("Android12中未获取此权限，则无法打开蓝牙。")
            }
        }

    //请求BLUETOOTH_SCAN权限意图
    private val requestBluetoothScan =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
            } else {
                showMsg("Android12中未获取此权限，则无法扫描蓝牙。")
            }
        }

    //打开蓝牙意图
    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            if (isOpenBluetooth()) {
                myBluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            } else {
                showMsg("蓝牙未打开")
            }
        }
    }

//
    private var activityResultLauncher : ActivityResultLauncher<Intent>  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val strName= it.data?.getStringExtra("name")
            val strAddress = it.data?.getStringExtra("address")
            bluetoothName = strName.toString()
            bluetoothAddress = strAddress.toString()
            ConnectBluetooth()
            findViewById<Button>(id.btn_power).setBackgroundResource(R.drawable.power_button_shape_enable)

         //   Toast.makeText(this,"返回的数据： $bluetoothName",Toast.LENGTH_LONG).show()
        }
    }

    fun SetConnectedDeviceName(dev: String)
    {
        findViewById<TextView>(id.text_selected_bt_device).setText(dev)
    }

    @SuppressLint("ResourceAsColor")
    fun SetPowerButtonColor(background: Drawable)
    {
        findViewById<Button>(id.btn_power).setBackground(background)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_controler)

        if (isOpenBluetooth()) {
            myBluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        }

        SetConnectedDeviceName("")
        findViewById<Button>(id.btn_power).setBackgroundResource(R.drawable.power_button_shape_disable)

        findViewById<Button>(id.btn_settings).setOnClickListener({
            val intent = Intent(this, BlueToothDevices::class.java)
            activityResultLauncher.launch(intent)
        })

        findViewById<Button>(id.btn_power).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("ON_OFF ".toByteArray(Charsets.UTF_8))
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })

        findViewById<Button>(id.btn_ok).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODEXX ".toByteArray(Charsets.UTF_8))
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })

        findViewById<Button>(id.btn_mode_up).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODEUP".toByteArray(Charsets.UTF_8))
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_mode_down).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODEDW".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_speed_up).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("SPEED+".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_speed_down).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("SPEED-".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_1).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE01".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_2).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE02".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_3).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE03".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_4).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE04".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_5).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE05".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_6).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE06".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_7).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE07".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_8).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE08".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_9).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE09".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_10).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE10".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_11).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE11".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_12).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE12".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_13).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE13".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_14).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE14".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_15).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE15".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_16).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE16".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_17).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE17".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_18).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE18".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_19).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE19".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
        findViewById<Button>(id.btn_num_20).setOnClickListener({
            if (mBluetoothSocket != null && isBlueConnected)
            {
                try {
                    mBluetoothSocket!!.outputStream.write("MODE20".toByteArray())
                } catch (e : IOException){
                    showMsg(e.toString())
                }
            }
        })
    }

    fun ConnectBluetooth()
    {
        if (bluetoothName.isEmpty() || bluetoothAddress.isEmpty()) {
            return
        }

        thread {
            try {
                //这一段代码必须在子线程处理，直接使用协程会阻塞主线程，所以用Thread,其实也可以直接用Thread，不用协程
                if (mBluetoothSocket == null || !isBlueConnected) {
                    val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                    myBluetoothAdapter = manager!!.adapter ?: return@thread

                    val device: BluetoothDevice = myBluetoothAdapter!!.getRemoteDevice(bluetoothAddress)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@thread
                    }
                    mBluetoothSocket =
                        device!!.createInsecureRfcommSocketToServiceRecord(myUUID)
                   // mBluetoothAdapter.cancelDiscovery()
                    mBluetoothSocket!!.connect()
                    isBlueConnected = true
                    SetConnectedDeviceName(bluetoothName)
                }
            } catch (e: IOException) {
                //连接失败销毁Activity
            //    finish()
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OrganTheme {
        Greeting("Android")
    }
}
