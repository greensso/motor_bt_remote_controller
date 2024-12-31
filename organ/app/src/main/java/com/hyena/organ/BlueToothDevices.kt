package com.hyena.organ

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID
import android.content.Intent
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hyena.organ.ui.theme.OrganTheme
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

//创建实体类,存放蓝牙名和蓝牙地址
class BlueDevice(val deviceName:String,val device:BluetoothDevice)

class BlueToothDevices : ComponentActivity(){
    private var myBluetoothAdapter: BluetoothAdapter? = null
    private lateinit var myPairedDevices: Set<BluetoothDevice>


    companion object {
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var mBluetoothSocket: BluetoothSocket? = null
        lateinit var mBluetoothAdapter: BluetoothAdapter
        var isBlueConnected: Boolean = false
        const val MESSAGE_RECEIVE_TAG = 111
        lateinit var blueAddress: String
        lateinit var blueName: String
        private val BUNDLE_RECEIVE_DATA = "ReceiveData"
        private val TAG = "BlueToothDevices"
       //设置发送和接收的字符编码格式
        private val ENCODING_FORMAT = "UTF-8"
        const val BLUE_ADDRESS: String = "DeviceAddress"
        const val BLUE_NAME: String = "DeviceName"
        var selectName : String = ""
        var selectAddress : String = ""
    }

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
        this.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                //扫描蓝牙
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
                mBluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                scanner = mBluetoothAdapter.bluetoothLeScanner
            //    showMsg("蓝牙已打开")
            } else {
                showMsg("蓝牙未打开")
            }
        }
    }

    private val TAG = MainActivity::class.java.simpleName

    //获取系统蓝牙适配器
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    //扫描者
    private lateinit var scanner: BluetoothLeScanner

    //是否正在扫描
    var isScanning = false

    //获取已经配对的蓝牙列表
    private fun pairedDeviceList() {
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
            Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show()
            return
        }

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        myBluetoothAdapter = manager.adapter ?: return
        myPairedDevices = myBluetoothAdapter!!.bondedDevices
        //   val list : ArrayList<BluetoothDevice> = ArrayList()
        val list : ArrayList<BlueDevice> = ArrayList()
        if (!myPairedDevices.isEmpty()) {
            for (device: BluetoothDevice in myPairedDevices) {
                list.add(BlueDevice(device.name,device))
            }
        } else {
            Toast.makeText(this, "没有找到蓝牙设备", Toast.LENGTH_SHORT).show()
        }


        val layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.recycler_blue_device_list).layoutManager = layoutManager
        val adapter = BlueDeviceListAdapter(list,this)
        findViewById<RecyclerView>(R.id.recycler_blue_device_list).adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoothdevices)

        findViewById<Button>(R.id.btn_bluetooth_ok).setOnClickListener({
            val intent=Intent()
            intent.putExtra( "name", selectName)
            intent.putExtra( "address", selectAddress)
            setResult(RESULT_OK,intent)
            finish()
        })

        findViewById<Button>(R.id.btn_bluetooth_refresh).setOnClickListener({
            //蓝牙是否已打开
            if (isOpenBluetooth()) {
            //    showMsg("蓝牙已打开")
            }
            //是Android12
            if (isAndroid12()) {
                //检查是否有BLUETOOTH_CONNECT权限
                if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    //打开蓝牙
                    enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                } else {
                    //请求权限
                    requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
            //不是Android12 直接打开蓝牙
          //  enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

            pairedDeviceList()
        })

    }

    //创建RecyclerView适配器
    class BlueDeviceListAdapter(val deviceList: List<BlueDevice>, val context: Context): RecyclerView.Adapter<BlueDeviceListAdapter.ViewHolder>(){
        inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
            val deviceName: TextView = view.findViewById(R.id.item_name)
            val deviceAddress: TextView = view.findViewById(R.id.address_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_blue_item,parent,false)
            val viewHolder = ViewHolder(view)

            //点击事件,启动蓝牙收发Activity
            viewHolder.itemView.setOnClickListener {
                val position = viewHolder.bindingAdapterPosition
                val device = deviceList[position]
                val intent = Intent()
                intent.putExtra(BLUE_ADDRESS, device.device.address)
                intent.putExtra(BLUE_NAME,device.deviceName)
             //   it.context.setResult(RESULT_OK, intent)
                BlueToothDevices.selectName = device.deviceName
                BlueToothDevices.selectAddress = device.device.address
                Toast.makeText(parent.context, device.deviceName, Toast.LENGTH_SHORT).show()
            }
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = deviceList[position]
            holder.deviceName.text = device.deviceName
            holder.deviceAddress.text = device.device.address
        }

        override fun getItemCount(): Int {
            return deviceList.size
        }
    }
}
