package com.smile.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private String deviceName;
    public static BluetoothChatService mBluetoothService = null;
    private TextView tvBTName;
    private SwitchCompat switchBT;
    private Button btnVisible;
    private Button btnSearch;
    private LinearLayout llHaveNotDevice;
    private LinearLayout llHaveNotDevice1;
    private RecyclerView rvDevices;
    private RecyclerView rvDevices1;
    private List<BluetoothDevice> deviceLists = new ArrayList<BluetoothDevice>();
    private List<BluetoothDevice> bondedDeviceLists;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    /**
     * Return Intent extra
     */

    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_DISABLE_BT = 4;
    private MyAdaper myAdapter;
    private MyAdaper myAdapter1;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.ACCESS_COARSE_LOCATION"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //检测是否有位置定位的权限
        int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                "android.permission.ACCESS_COARSE_LOCATION");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            try {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }

    private void initView() {
        tvBTName = (TextView) findViewById(R.id.tvBTName);
        switchBT = (SwitchCompat) findViewById(R.id.switchBT);
        btnVisible = (Button) findViewById(R.id.btnVisible);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        llHaveNotDevice = (LinearLayout) findViewById(R.id.llHaveNotDevice);
        rvDevices = (RecyclerView) findViewById(R.id.rvDevices);
        llHaveNotDevice1 = (LinearLayout) findViewById(R.id.llHaveNotDevice1);
        rvDevices1 = (RecyclerView) findViewById(R.id.rvDevices1);
    }

    private void initEvent() {
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        bondedDeviceLists = new ArrayList<BluetoothDevice>(pairedDevices);
        if (mBluetoothAdapter.isEnabled()) {
            switchBT.setChecked(true);
            mBluetoothService = new BluetoothChatService(this, mHandler);
            mBluetoothService.start();
        }
        tvBTName.setText(mBluetoothAdapter.getName());
        switchBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    if (mBluetoothAdapter != null) {
                        mBluetoothAdapter.disable();
                    }
                }
            }
        });
        btnVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ensureDiscoverable();
            }
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                    Log.e("TAG", "onClick: ----------取消搜索");
                } else {
                    // TODO 这里可以先获取已经配对的设备
                    // Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if (deviceLists != null) {
                        deviceLists.clear();
                    }
                    // 开始扫描设备
                    mBluetoothAdapter.startDiscovery();
                    Log.e("TAG", "onClick: ----------进行搜索");
                }
            }
        });
        myAdapter = new MyAdaper(this, bondedDeviceLists);
        rvDevices.setItemAnimator(new DefaultItemAnimator());
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.addItemDecoration(new DividerItemDecoration(this, 1));
        rvDevices.setAdapter(myAdapter);
        myAdapter1 = new MyAdaper(this, deviceLists);
        rvDevices1.setItemAnimator(new DefaultItemAnimator());
        rvDevices1.setLayoutManager(new LinearLayoutManager(this));
        rvDevices1.addItemDecoration(new DividerItemDecoration(this, 1));
        rvDevices1.setAdapter(myAdapter1);

        myAdapter.notifyDataSetChanged();
        if (bondedDeviceLists.size() == 0) {
            llHaveNotDevice.setVisibility(View.VISIBLE);
            rvDevices.setVisibility(View.GONE);
        } else {
            llHaveNotDevice.setVisibility(View.GONE);
            rvDevices.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    switchBT.setChecked(true);
                    mBluetoothService = new BluetoothChatService(this, mHandler);
                    mBluetoothService.start();
                    Log.e("MainActivity", "onActivityResult: 返回的是开启成功了呀-------");
                } else {
                    switchBT.setChecked(false);
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        bondedDeviceLists.clear();
        bondedDeviceLists.addAll(new ArrayList<BluetoothDevice>(pairedDevices));
        myAdapter.notifyDataSetChanged();
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("TAG", "onReceive: -----" + action);

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                if (!deviceLists.contains(device)) {
                    deviceLists.add(device);
                }
                myAdapter1.notifyDataSetChanged();
                if (deviceLists.size() == 0) {
                    llHaveNotDevice1.setVisibility(View.VISIBLE);
                    rvDevices1.setVisibility(View.GONE);
                } else {
                    llHaveNotDevice1.setVisibility(View.GONE);
                    rvDevices1.setVisibility(View.VISIBLE);
                }
//                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            }
        }
    };

    class MyAdaper extends RecyclerView.Adapter<MyAdaper.ViewHolder> {

        private Context context;
        private List<BluetoothDevice> devices;

        public MyAdaper(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            this.devices = devices;
        }

        @NonNull
        @Override
        public MyAdaper.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_device, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyAdaper.ViewHolder holder, final int position) {
            final BluetoothDevice device = devices.get(position);
            holder.tvDeviceName.setText(device.getName());
            holder.tvDeviceAddress.setText(device.getAddress());
            holder.tvDeviceState.setText("");
            holder.rlDevice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Cancel discovery because it's costly and we're about to connect
                    deviceName = device.getName();
                    mBluetoothService.connect(device, true);
                }
            });
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private RelativeLayout rlDevice;
            private TextView tvDeviceName;
            private TextView tvDeviceAddress;
            private TextView tvDeviceState;

            public ViewHolder(View itemView) {
                super(itemView);
                rlDevice = (RelativeLayout) itemView.findViewById(R.id.rlDevice);
                tvDeviceName = (TextView) itemView.findViewById(R.id.tvDeviceName);
                tvDeviceAddress = (TextView) itemView.findViewById(R.id.tvDeviceAddress);
                tvDeviceState = (TextView) itemView.findViewById(R.id.tvDeviceState);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    Log.e("DeviceConnectActivity", "handleMessage: --------MESSAGE_STATE_CHANGE-------" + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Intent intent = new Intent(MainActivity.this, DeviceConnectActivity.class);
                            intent.putExtra("name", deviceName);
                            startActivity(intent);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    Message msg1 = new Message();
                    msg1.obj = msg.obj;
                    msg1.what = Constants.MESSAGE_WRITE;
                    DeviceConnectActivity.mHandler.sendMessage(msg1);
                    break;
                case Constants.MESSAGE_READ:
                    Message msg2 = new Message();
                    msg2.obj = msg.obj;
                    msg2.what = Constants.MESSAGE_READ;
                    DeviceConnectActivity.mHandler.sendMessage(msg2);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    deviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    // save the connected device's name
                    break;
                case Constants.MESSAGE_TOAST:
                    Log.e("DeviceConnectActivity", "handleMessage: --------MESSAGE_TOAST");
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
