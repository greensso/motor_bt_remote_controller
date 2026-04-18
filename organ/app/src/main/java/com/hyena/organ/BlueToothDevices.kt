package com.hyena.organ

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    
    // 设备列表，用于存储已配对和未配对的设备
    private val deviceList = ArrayList<BlueDevice>()
    // 适配器
    private lateinit var adapter: BlueDeviceListAdapter


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
        const val PREF_NAME = "BluetoothPrefs"
        const val KEY_SELECT_NAME = "selectName"
        const val KEY_SELECT_ADDRESS = "selectAddress"
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
    
    // 保存选中的设备信息
    private fun saveSelectedDevice() {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_SELECT_NAME, selectName)
        editor.putString(KEY_SELECT_ADDRESS, selectAddress)
        editor.apply()
    }
    
    // 加载选中的设备信息
    private fun loadSelectedDevice() {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        selectName = sharedPreferences.getString(KEY_SELECT_NAME, "") ?: ""
        selectAddress = sharedPreferences.getString(KEY_SELECT_ADDRESS, "") ?: ""
    }

    //请求定位权限意图
    private val requestLocation =
        this.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                // 权限授予后，重新检查权限并启动扫描
                checkPermissionsAndStartScan()
            } else {
                showMsg("Android12以下，6及以上需要定位权限才能扫描设备")
            }
        }

    //请求BLUETOOTH_CONNECT权限意图
    private val requestBluetoothConnect =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                // 权限授予后，重新检查权限并启动扫描
                checkPermissionsAndStartScan()
            } else {
                showMsg("Android12中未获取此权限，则无法打开蓝牙。")
            }
        }

    //请求BLUETOOTH_SCAN权限意图
    private val requestBluetoothScan =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                // 权限授予后，重新检查权限并启动扫描
                checkPermissionsAndStartScan()
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
                // 蓝牙打开后，重新检查权限并启动扫描
                checkPermissionsAndStartScan()
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
    
    // 广播接收器，用于接收蓝牙设备发现和配对状态变化
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // 发现新设备
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        // 检查设备是否已在列表中
                        val exists = deviceList.any { it.device.address == device.address }
                        if (!exists) {
                            deviceList.add(BlueDevice(it.name ?: "未知设备", it))
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    // 配对状态变化
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            // 配对成功
                            showMsg("配对成功: ${device?.name}")
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            // 正在配对
                            showMsg("正在配对: ${device?.name}")
                        }
                        BluetoothDevice.BOND_NONE -> {
                            // 配对取消
                            showMsg("配对取消: ${device?.name}")
                        }
                    }
                }
            }
        }
    }
    
    // 注册广播接收器
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        registerReceiver(bluetoothReceiver, filter)
    }
    
    // 注销广播接收器
    private fun unregisterBluetoothReceiver() {
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            // 接收器未注册，忽略异常
        }
    }
    
    // BLE扫描回调
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            // 检查设备是否已在列表中
            val exists = deviceList.any { it.device.address == device.address }
            if (!exists) {
                deviceList.add(BlueDevice(device.name ?: "未知BLE设备", device))
                adapter.notifyDataSetChanged()
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                val device = result.device
                val exists = deviceList.any { it.device.address == device.address }
                if (!exists) {
                    deviceList.add(BlueDevice(device.name ?: "未知BLE设备", device))
                }
            }
            adapter.notifyDataSetChanged()
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            showMsg("BLE扫描失败，错误码: $errorCode")
        }
    }
    
    // 启动扫描
    private fun startScan() {
        // 获取蓝牙适配器
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = manager.adapter ?: run {
            showMsg("无法获取蓝牙适配器")
            return
        }
        
        // 检查蓝牙是否启用
        if (!bluetoothAdapter.isEnabled) {
            showMsg("蓝牙未启用")
            return
        }
        
        // 清空设备列表（先清空，后添加已配对设备）
        deviceList.clear()
        
        // 加载已配对设备（优先显示）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
            == PackageManager.PERMISSION_GRANTED) {
            val pairedDevices = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                for (device in pairedDevices) {
                    deviceList.add(BlueDevice(device.name ?: "未知设备", device))
                }
            }
        }
        
        // 通知适配器数据已更改
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
        
        // 注册广播接收器（用于接收新发现的设备）
        registerBluetoothReceiver()
        
        // 启动经典蓝牙扫描
        try {
            isScanning = true
            val started = bluetoothAdapter.startDiscovery()
            if (!started) {
                showMsg("启动经典蓝牙扫描失败")
            }
        } catch (e: SecurityException) {
            showMsg("启动经典蓝牙扫描权限异常")
            e.printStackTrace()
        }
        
        // 启动BLE扫描
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                val scanSettings = android.bluetooth.le.ScanSettings.Builder()
                    .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, scanSettings, leScanCallback)
            } else {
                showMsg("BLE扫描器不可用")
            }
        } catch (e: SecurityException) {
            showMsg("启动BLE扫描权限异常")
            e.printStackTrace()
        }
        
        // 显示扫描中提示
        showMsg("正在扫描蓝牙设备...")
        
        // 自动选中上次选中的已配对设备
        autoSelectLastDevice()
        
        // 30秒后停止扫描
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
            showMsg("扫描完成")
        }, 30000)
    }
    
    // 自动选中上次选中的已配对设备
    private fun autoSelectLastDevice() {
        // 如果之前没有选中过设备，则尝试选中上次保存的设备
        if (selectAddress.isNotEmpty()) {
            val lastDevice = deviceList.find { it.device.address == selectAddress }
            if (lastDevice != null && lastDevice.device.bondState == BluetoothDevice.BOND_BONDED) {
                // 已找到上次选中的设备，已自动选中
                return
            }
        }
        
        // 如果没有选中设备，自动选中第一个已配对的设备
        val firstPairedDevice = deviceList.find { it.device.bondState == BluetoothDevice.BOND_BONDED }
        if (firstPairedDevice != null) {
            selectName = firstPairedDevice.deviceName
            selectAddress = firstPairedDevice.device.address
            adapter.notifyDataSetChanged()
        }
    }
    
    // 停止扫描
    private fun stopScan() {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = manager.adapter ?: return
        
        // 停止经典蓝牙扫描
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        
        // 停止BLE扫描
        val scanner = bluetoothAdapter.bluetoothLeScanner
        scanner?.stopScan(leScanCallback)
        
        isScanning = false
    }

    //获取已经配对的蓝牙列表
    private fun pairedDeviceList() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "没有蓝牙权限", Toast.LENGTH_SHORT).show()
            return
        }

        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        myBluetoothAdapter = manager.adapter ?: return
        myPairedDevices = myBluetoothAdapter!!.bondedDevices
        
        // 清空设备列表
        deviceList.clear()
        
        // 添加已配对的设备
        if (!myPairedDevices.isEmpty()) {
            for (device: BluetoothDevice in myPairedDevices) {
                deviceList.add(BlueDevice(device.name,device))
            }
        } else {
            Toast.makeText(this, "没有找到蓝牙设备", Toast.LENGTH_SHORT).show()
        }

        // 初始化适配器
        val layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.recycler_blue_device_list).layoutManager = layoutManager
        adapter = BlueDeviceListAdapter(deviceList,this)
        findViewById<RecyclerView>(R.id.recycler_blue_device_list).adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetoothdevices)

        try {
            // 加载上次选中的设备
            loadSelectedDevice()
            
            // 初始化适配器
            val layoutManager = LinearLayoutManager(this)
            findViewById<RecyclerView>(R.id.recycler_blue_device_list).layoutManager = layoutManager
            adapter = BlueDeviceListAdapter(deviceList,this)
            findViewById<RecyclerView>(R.id.recycler_blue_device_list).adapter = adapter
            
            // 检查权限并启动扫描
            checkPermissionsAndStartScan()
        } catch (e : Exception) {

        }


        findViewById<Button>(R.id.btn_bluetooth_ok).setOnClickListener({
            val intent=Intent()
            intent.putExtra( "name", selectName)
            intent.putExtra( "address", selectAddress)
            setResult(RESULT_OK,intent)
            finish()
        })

        findViewById<Button>(R.id.btn_bluetooth_refresh).setOnClickListener({
            // 刷新时直接重新扫描
            startScan()
        })
    }
    
    // 检查权限并启动扫描
    private fun checkPermissionsAndStartScan() {
        // 检查蓝牙是否已打开
        if (!isOpenBluetooth()) {
            // 蓝牙未打开，请求打开
            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        
        // 是Android12
        if (isAndroid12()) {
            // 检查是否有BLUETOOTH_CONNECT权限
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                requestBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
            // 检查是否有BLUETOOTH_SCAN权限
            if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                requestBluetoothScan.launch(Manifest.permission.BLUETOOTH_SCAN)
                return
            }
            // 检查位置权限
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        } else {
            // Android 12以下
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        }
        
        // 权限检查通过，启动扫描
        startScan()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止扫描
        stopScan()
        // 注销广播接收器
        unregisterBluetoothReceiver()
    }

    //创建RecyclerView适配器
    class BlueDeviceListAdapter(val deviceList: List<BlueDevice>, val context: Context): RecyclerView.Adapter<BlueDeviceListAdapter.ViewHolder>(){
        inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
            val deviceName: TextView = view.findViewById(R.id.item_name)
            val deviceAddress: TextView = view.findViewById(R.id.address_item)
            val deviceCard: androidx.cardview.widget.CardView = view.findViewById(R.id.device_item_card)
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
                
                // 保存选中的设备
                (context as BlueToothDevices).saveSelectedDevice()
                
             //   Toast.makeText(parent.context, device.deviceName, Toast.LENGTH_SHORT).show()
                
                // 刷新适配器以显示选中状态
                notifyDataSetChanged()
                
                // 检查设备是否已配对
                if (device.device.bondState != BluetoothDevice.BOND_BONDED) {
                    // 未配对，尝试配对
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12及以上需要BLUETOOTH_CONNECT权限
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            device.device.createBond()
                        } else {
                            Toast.makeText(context, "需要蓝牙连接权限", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Android 12以下直接配对
                        device.device.createBond()
                    }
                }
            }
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = deviceList[position]
            holder.deviceName.text = device.deviceName
            holder.deviceAddress.text = device.device.address
            
            // 检查是否为选中项
            val isSelected = BlueToothDevices.selectAddress == device.device.address
            
            // 检查是否已配对
            val isPaired = device.device.bondState == BluetoothDevice.BOND_BONDED
            
            // 设置选中状态的视觉效果
            if (isSelected) {
                holder.deviceCard.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
                holder.deviceCard.alpha = 1.0f
            } else {
                holder.deviceCard.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                holder.deviceCard.alpha = 0.5f
            }
            
            // 在设备名称后添加配对状态标识
            if (isPaired) {
                holder.deviceName.text = "${device.deviceName} (已配对)"
            } else {
                holder.deviceName.text = device.deviceName
            }
        }

        override fun getItemCount(): Int {
            return deviceList.size
        }
    }
}
