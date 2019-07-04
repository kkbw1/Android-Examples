package com.kkb.tcptest;

import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Key names received from the TCPService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Debugging
    private static final String TAG = "TCPTEST";
    private static final boolean D = true;

    private TextView tv1;
    private TextView tv2;
    private TextView tv3;

    private EditText et1;

    private Button btn1;

    private String ServerIP;
    private static final String PORT_NONE = "NONE";
    private int ServerPort = 8000;

    // Name of the connected ip
    private String mConnectedIP = null;
    // Array adapter for the conversation thread
    private ArrayList<String> list_array;
    private ArrayAdapter<String> list_adapter;
    private ListView list;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Wifi Classes
    private WifiManager wifi;
    private WifiInfo info;
    // Member object for the tcp services
    private TCPService mTCPService = null;


    //********************************************************************************************//
    //                                                                                            //
    //                                   Overriden Methods                                        //
    //                                                                                            //
    //********************************************************************************************//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifi = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        info = wifi.getConnectionInfo();
        if(!(wifi.isWifiEnabled() && info.getSSID() != null))
        {
            Toast.makeText(this, "WiFi is not Available", Toast.LENGTH_LONG).show();
            finish();
//            return;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        InitializeComponent();
        setTitle(getString(R.string.app_name) + ": " + info.getSSID());

        // Getting my IPv4 Address.
        int ipAddr = info.getIpAddress();
        ServerIP = String.format("%d.%d.%d.%d",
                (ipAddr & 0xff), (ipAddr >> 8 & 0xff),
                (ipAddr >> 16 & 0xff), (ipAddr >> 24 & 0xff));
        tv1.setText(ServerIP);

        try {
            ServerSocket ss = new ServerSocket(ServerPort);
            ss.close();

            tv2.setText(String.valueOf(ServerPort));
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Port is Using.",
                    Toast.LENGTH_SHORT).show();
            tv2.setText(PORT_NONE);
        }

        if(mTCPService == null)
        {
            mTCPService = new TCPService(this, mHandler);
            // Initialize the buffer for outgoing messages
            mOutStringBuffer = new StringBuffer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTCPService != null) mTCPService.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_addr:

                break;
            case R.id.menu_port:
                DialogPort();
                break;
            case R.id.menu_server:
                if (mTCPService != null)
                {
                    if(!tv2.getText().toString().equals(PORT_NONE))
                    {
                        mTCPService.start(ServerPort);
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Port is Wrong",
                                Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case R.id.menu_client:
                DialogClient();
                break;
            case R.id.menu_disconnect:
                if (mTCPService != null) mTCPService.stop();
                break;
        }
        return true;
    }


    //********************************************************************************************//
    //                                                                                            //
    //                             Listeners, Callbacks, Handlers                                 //
    //                                                                                            //
    //********************************************************************************************//
    private android.view.View.OnClickListener SendBtnClick = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String data = et1.getText().toString();
            data = data + "\r\n";
            if( !(TextUtils.isEmpty(data)) )
            {
//                sendMessage(data);
                new SendMessageTask().execute(data);
            }
        }
    };

    // The Handler that gets information back from the TCPService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TCPService.MESSAGE_STATE_CHANGE:
                    if(D) Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case TCPService.STATE_CONNECTED:
                            tv3.setText("Connected.");
                            break;
                        case TCPService.STATE_CONNECTING:
                            tv3.setText("Connecting to Other Device.");
                            break;
                        case TCPService.STATE_LISTEN:
                            tv3.setText("Server Opened.");
                            break;
                        case TCPService.STATE_NONE:
                            tv3.setText("Do Nothing.");
                            break;
                    }
                    break;
                case TCPService.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    list_array.add("Out: " + writeMessage);
                    list_adapter.notifyDataSetChanged();
                    break;
                case TCPService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    list_array.add("In: " + readMessage);
                    list_adapter.notifyDataSetChanged();
                    break;
                case TCPService.MESSAGE_SERVER_IP:
                    // save the connected device's name
                    String server = msg.getData().getString(DEVICE_NAME);
                    list_array.add("Connected Server: " + server);
                    list_adapter.notifyDataSetChanged();
                    break;
                case TCPService.MESSAGE_CLIENT_IP:
                    String client = msg.getData().getString(DEVICE_NAME);
                    list_array.add("Connected Client: " + client);
                    list_adapter.notifyDataSetChanged();
                    break;
                case TCPService.MESSAGE_ADMIN:
                    String read = (String) msg.obj;
                    list_array.add(read);
                    list_adapter.notifyDataSetChanged();
                    break;
                case TCPService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    //********************************************************************************************//
    //                                                                                            //
    //                               User Defined Sub-routines                                    //
    //                                                                                            //
    //********************************************************************************************//
    public void InitializeComponent()
    {
        tv1 = findViewById(R.id.textView1);
        tv2 = findViewById(R.id.textView2);
        tv3 = findViewById(R.id.textView3);

        et1 = findViewById(R.id.editText1);

        btn1 = findViewById(R.id.button1);
        btn1.setOnClickListener(SendBtnClick);

        list_array = new ArrayList<String>();
        list_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                list_array);
        list = findViewById(R.id.listView1);
        list.setAdapter(list_adapter);
        list.setChoiceMode(ListView.CHOICE_MODE_NONE);
        list.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    }

    private void DialogPort() {
        Context context = MainActivity.this;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_port, (ViewGroup)findViewById(R.id.layout_port));
        final EditText etPortSet = layout.findViewById(R.id.editTextPort);

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("Port Set");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String tmp = etPortSet.getText().toString();
                if(TextUtils.isEmpty(tmp) || tmp.length() > 4)
                {
                    Toast.makeText(MainActivity.this, "Empty or Wrong Port.",
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
                else
                {
                    try {
                        ServerSocket ss = new ServerSocket(Integer.valueOf(tmp));
                        ss.close();
                        ServerPort = Integer.valueOf(tmp);
                        tv2.setText(tmp);
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Port is Using or Wrong Port.",
                                Toast.LENGTH_SHORT).show();
                        tv2.setText(PORT_NONE);
                    }
                }
            }
        });
        builder.create().show();
    }

    private void DialogClient() {
        Context context = MainActivity.this;

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_connect, (ViewGroup)findViewById(R.id.layout_connect));
        final EditText etIP = layout.findViewById(R.id.editText1);
        final EditText etPORT = layout.findViewById(R.id.editText2);

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("Connect");
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String IP = etIP.getText().toString();
                String PORT_str = etPORT.getText().toString();
                if(!( TextUtils.isEmpty(PORT_str) || TextUtils.isEmpty(IP) ))
                {
                    try {
                        int PORT = Integer.valueOf(PORT_str);
                        mTCPService.connect(IP, PORT);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Wrong IP or Port.", Toast.LENGTH_SHORT).show();
                    }
                }
//                else
//                {
//
//                }
            }
        });
        builder.create().show();
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mTCPService.getState() != TCPService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mTCPService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            et1.setText(mOutStringBuffer);
        }
    }

    private class  SendMessageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            try{
                sendMessage(strings[0]);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }

            return null;
        }
    }
}
