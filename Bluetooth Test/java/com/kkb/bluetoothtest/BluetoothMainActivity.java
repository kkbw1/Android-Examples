package com.kkb.bluetoothtest;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

public class BluetoothMainActivity extends AppCompatActivity implements View.OnClickListener {

    //********************************************************************************************//
    //                                                                                            //
    //                                   Declaration Views                                        //
    //                                                                                            //
    //********************************************************************************************//
    private TextView TV0;
    private TextView TV1;
    private TextView TV2;

    private EditText OutEditText;

    private Button SendBtn;


    //********************************************************************************************//
    //                                                                                            //
    //                         Declaration Menu Item Variables and Objects                        //
    //                                                                                            //
    //********************************************************************************************//
    // Paired Devices List
    private ArrayList<String> mPairedDevicesArrayList;
    private String[] PairedDevicesArray;
    private AlertDialog.Builder PairedDevicesListDialog;

    // Searched Devices List
    private ArrayList<String> mNewDevicesArrayList;
    private String[] NewDevicesArray;
    private AlertDialog.Builder NewDevicesListDialog;

    // Bluetooth Discoverable Timer
    private Handler TimerHandler;
    private ProgressDialog pd;

    private String dialogTemp;

    /////////////// Chatting List ////////////////
    //private ArrayList<String> MyArrayList;
    private ArrayAdapter<String> MyArrayAdapter;
    private ListView MyListView;

    //private ArrayList<String> OpponentArrayList;
    private ArrayAdapter<String> OpponentArrayAdapter;
    private ListView OpponentListView;
    //////////////////////////////////////////////

    // Debugging
    private static final String TAG = "BluetoothMainActivity";
    private static final boolean D = true;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBtAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    private int flag_timer = 1;
    private int Scan_mode_sec = DISCOVER_DURATION;
    private static final int DISCOVER_DURATION = 20;

    private int number_scanDevices = 0;

    public String address;

    //********************************************************************************************//
    //                                                                                            //
    //                                   Overriden Methods                                        //
    //                                                                                            //
    //********************************************************************************************//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        getActionBar();

        InitializeComponent();

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        this.registerReceiver(mReceiver, filter);

        TimerHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                setTitle("DISCOVERABLE(" + Integer.toString(Scan_mode_sec) + "sec)");
                Scan_mode_sec--;
                if(flag_timer == 0)
                {
                    TimerHandler.sendEmptyMessageDelayed(0, 1000);
                }
                else
                {
                    setTitle("BluetoothTest: Not Connected");
                    Scan_mode_sec = DISCOVER_DURATION;
                }
                if(Scan_mode_sec == 0)
                {
                    flag_timer = 1;
                }
            }
        };

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // CellPhone Not Support Bluetooth
        if(mBtAdapter == null)
        {
            Toast.makeText(this, "Your Android is not supported BT.", Toast.LENGTH_LONG);
            setTitle("BluetoothTest: NOT SUPPORTED");
        }

        // CellPhone Support Bluetooth
        if(mBtAdapter != null)
        {
            if(!mBtAdapter.isEnabled())
            {
                mBtAdapter.enable();
                while(!mBtAdapter.isEnabled());
                Toast.makeText(this, "BT is On.", Toast.LENGTH_LONG).show();
            }

            switch(mBtAdapter.getScanMode())
            {
                case BluetoothAdapter.SCAN_MODE_NONE:
                    TV2.setText("Status: None");
                    break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    TV2.setText("Status: Connectable");
                    break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    TV2.setText("Status: Both");
                    break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        if (mChatService == null)
        {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(this, mHandler);

            // Initialize the buffer for outgoing messages
            mOutStringBuffer = new StringBuffer("");
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.d(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.d(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null)
        {
            mBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.d(TAG, "--- ON DESTROY ---");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
            case BluetoothChatService.REQUEST_BT_DISCOVERABLE:
                if(resultCode != RESULT_CANCELED)
                {
                    TimerHandler.sendEmptyMessage(0);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Paired Device");
        menu.add(0, 1, 0, "Search Device");
        menu.add(0, 2, 0, "Discoverable");
        menu.add(0, 3, 0, "Disconnect");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == 0)	// Paired Device View
        {
            if(mBtAdapter.isEnabled())
            {
                // Get a set of currently paired devices
                Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

                mPairedDevicesArrayList.clear();
                // If there are paired devices, add each one to the ArrayAdapter
                if (pairedDevices.size() > 0)
                {
                    for (BluetoothDevice device : pairedDevices)
                    {
                        mPairedDevicesArrayList.add(device.getName() + "\n" + device.getAddress());
                    }
                }
                else
                {
                    String noDevices = "No Found";
                    mPairedDevicesArrayList.add(noDevices);
                }

                PairedDevicesArray = mPairedDevicesArrayList.toArray(new String[]{});

                // Setting a list dialog to show a paired device list
                PairedDevicesListDialog = new AlertDialog.Builder(BluetoothMainActivity.this);
                PairedDevicesListDialog.setTitle("Paired Device");
                PairedDevicesListDialog.setSingleChoiceItems(PairedDevicesArray, -1,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialogTemp = PairedDevicesArray[which];
                    }
                });
                PairedDevicesListDialog.setPositiveButton("Connect",
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel discovery because it's costly and we're about to connect
                        mBtAdapter.cancelDiscovery();

                        // Get the device MAC address, which is the last 17 chars in the View
                        if(dialogTemp == "No Found")
                        {
                            return;
                        }
                        address = dialogTemp.substring(dialogTemp.length() - 17);

                        // Get the BluetoothDevice object
                        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                        // Attempt to connect to the device
                        mChatService.connect(device);
                    }
                });
                PairedDevicesListDialog.setNegativeButton("Cancel", null);
                AlertDialog ld = PairedDevicesListDialog.create();
                ld.show();
            }
            else
            {
                Toast.makeText(this, "Bluetooth is not Available.", Toast.LENGTH_LONG).show();
            }
        }
        else if(item.getItemId() == 1)	// Search Devices
        {
            if(mBtAdapter.isEnabled())
            {
                mNewDevicesArrayList.clear();
                doDiscovery();
                //item.setEnabled(false);
            }
            else
            {
                Toast.makeText(this, "Bluetooth is not Available.", Toast.LENGTH_LONG).show();
            }
        }
        else if(item.getItemId() == 2)	// My CellPhone Discoverable
        {
            if(mBtAdapter.isEnabled())
            {
                ensureDiscoverable();
                flag_timer = 0;
            }
            else
            {
                Toast.makeText(this, "Bluetooth is not Available.", Toast.LENGTH_LONG).show();
            }
        }
        else if(item.getItemId() == 3)	// Disconnection
        {
            if (mChatService != null)
            {
                mChatService.stop();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == SendBtn.getId())
        {
            // Send a message using content of the edit text widget
//            TextView view = (TextView) findViewById(R.id.edit_text_out);
//            String message = view.getText().toString();
            String message = OutEditText.getText().toString();
            OutEditText.setText("");

            sendMessage(message);
        }
    }

    //********************************************************************************************//
    //                                                                                            //
    //                                   Handlers and Callbacks                                   //
    //                                                                                            //
    //********************************************************************************************//

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {   // When discovery finds a device
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                mNewDevicesArrayList.add(device.getName() + "\n" + device.getAddress());

                number_scanDevices++;
                pd.setMessage("Searching...Please wait (" + number_scanDevices + ")");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {   // When discovery is finished, change the Activity title
                //setProgressBarIndeterminateVisibility(false);
                setTitle("BluetoothTest: Select Device");
                if (mNewDevicesArrayList.size() == 0)
                {
                    String noDevices = "No Found";
                    mNewDevicesArrayList.add(noDevices);
                }
                pd.dismiss();
                NewDevicesArray = mNewDevicesArrayList.toArray(new String[]{});

                // creating list dialog to show a new device list
                NewDevicesListDialog = new AlertDialog.Builder(BluetoothMainActivity.this);
                NewDevicesListDialog.setTitle("New Devices Found.");
                NewDevicesListDialog.setSingleChoiceItems(NewDevicesArray, -1,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialogTemp = NewDevicesArray[which];
                    }
                });

                if(number_scanDevices != 0) {
                    NewDevicesListDialog.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Cancel discovery because it's costly and we're about to connect
                            mBtAdapter.cancelDiscovery();
                            // Get the device MAC address, which is the last 17 chars in the View
                            if (dialogTemp == "No Found") {
                                return;
                            }
                            address = dialogTemp.substring(dialogTemp.length() - 17);

                            // Get the BLuetoothDevice object
                            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
                            // Attempt to connect to the device
                            mChatService.connect(device);
                        }
                    });
                }
                NewDevicesListDialog.setNegativeButton("Cancel", null);
                AlertDialog ld = NewDevicesListDialog.create();
                ld.show();
            }
            else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action))
            {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);

                switch(mode)
                {
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        TV2.setText("Status: None");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        TV2.setText("Status: Connectable");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        TV2.setText("Status: Both");
                        break;
                }
            }
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setTitle("Connected " + mConnectedDeviceName);
                            TV1.setText(mConnectedDeviceName);
                            MyArrayAdapter.clear();
                            OpponentArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setTitle("Connecting...." + address);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setTitle("BluetoothTest: Not Connected");
                            break;
                    }
                    break;
                case BluetoothChatService.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    MyArrayAdapter.add("Me: " + writeMessage);
                    break;
                case BluetoothChatService.MESSAGE_READ:
                    byte[] rawBuff = (byte[]) msg.obj;
                    byte[] readBuff = new byte[msg.arg1];

//                    // show raw byte number
//                    for(int i = 0; i < msg.arg1; i++)
//                    {
//                        readBuff[i] = rawBuff[i];
//                        OpponentArrayAdapter.add(mConnectedDeviceName+": " + rawBuff[i]);
//                    }

                    // show the data converted to string
//                    String str_tmp = new String(rawBuff, 0, msg.arg1);
                    String str_tmp = new String(rawBuff);
                    OpponentArrayAdapter.add(mConnectedDeviceName+": " + str_tmp);

//                    // Decoding for supporting Korean
//                    try {
//                        String text = new String(readBuff, "EUC-KR");
//                        //text = text.substring(0, msg.arg1 - 1);
//                        OpponentArrayAdapter.add(mConnectedDeviceName+": " + text);
//                    } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
//                    }
                    break;
                case BluetoothChatService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothChatService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    //********************************************************************************************//
    //                                                                                            //
    //                                   User-defined subroutines                                 //
    //                                                                                            //
    //********************************************************************************************//
    private void InitializeComponent()
    {
        TV0 = findViewById(R.id.tvMe);
        TV1 = findViewById(R.id.tvDevice);
        TV2 = findViewById(R.id.textView3);

        SendBtn = findViewById(R.id.btnSend);
        SendBtn.setOnClickListener(this);

        OutEditText = findViewById(R.id.editTextOut);

        // Find and set up the ListView for paired devices
        mPairedDevicesArrayList = new ArrayList<String>();

        // Find and set up the ListView for newly discovered devices
        mNewDevicesArrayList = new ArrayList<String>();

        MyArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        MyListView = findViewById(R.id.listView1);
        MyListView.setAdapter(MyArrayAdapter);
        MyListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        OpponentArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        OpponentListView = findViewById(R.id.listView2);
        OpponentListView.setAdapter(OpponentArrayAdapter);
        OpponentListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("BluetoothTest: Not Connected");

        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
        number_scanDevices = 0;
        pd = ProgressDialog.show(BluetoothMainActivity.this, "",
                "Scanning...Please wait (0)", true);
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure Discoverable");

        if(mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Scan_mode_sec);
            startActivityForResult(discoverableIntent, BluetoothChatService.REQUEST_BT_DISCOVERABLE);
        }
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            String strMsg = message + "\r\n";
            byte[] send = strMsg.getBytes();
            mChatService.write(send);
//            mChatService.write(new byte[]{13, 10});

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            OutEditText.setText(mOutStringBuffer);
        }
    }
}
