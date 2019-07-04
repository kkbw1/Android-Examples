package com.kkb.udptest;

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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Debugging
    private static final String TAG = "UDPTEST";
    private static final boolean D = true;

    // Key names received from the UDPService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    String IP = "NONE";
    int Port = 0;
    //	InetAddress IPaddr;
//	DatagramSocket socket;
//	DatagramPacket packet;
//	byte[] buffer_rx = new byte[1024];

    private UDPService mUdpService;

    private ArrayList<String> list_array;
    private ArrayAdapter<String> list_adapter;
    private ListView lv_chat;
    private EditText et_message;
    private Button btn_send;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitializeComponent();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mUdpService == null) {
            mUdpService = new UDPService(mHandler);
            mUdpService.start();
        }
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
            case R.id.menu_myIP:
                DialogViewMyIP();
                break;
            case R.id.menu_viewIP:
                DialogViewIP();
                break;
            case R.id.menu_setIP:
                DialogSetIP();
                break;
        }
        return true;
    }

    private android.view.View.OnClickListener sendOnClick = new android.view.View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mUdpService != null && IP != "NONE" && Port != 0)
            {
                String message = et_message.getText().toString();
//                mUdpService.send(message);
                message = message + "\r\n";
                if( !(TextUtils.isEmpty(message)) )
                {
                    new SendMessageTask().execute(message);
                }
            }
        }
    };

    // The Handler that gets information back from the TCPService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UDPService.MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {

                    }
                    break;
				case UDPService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    list_array.add("In: " + readMessage);
                    list_adapter.notifyDataSetChanged();
                    break;
                case UDPService.MESSAGE_WRITE:
                    // construct a string from the buffer
                    String writeMessage = (String) msg.obj;
                    list_array.add("Out: " + writeMessage);
                    list_adapter.notifyDataSetChanged();
                    et_message.setText("");
                    break;
                case UDPService.MESSAGE_SERVER_IP:

                    break;
                case UDPService.MESSAGE_CLIENT_IP:

                    break;
                case UDPService.MESSAGE_ADMIN:
                    // construct a string from the buffer
                    String addr = (String) msg.obj;
                    list_array.add("Receive From " + addr);
                    list_adapter.notifyDataSetChanged();
                    break;
                case UDPService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void InitializeComponent() {
        list_array = new ArrayList<String>();
        list_adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list_array);
        lv_chat = findViewById(R.id.list_chat);
        lv_chat.setAdapter(list_adapter);
        lv_chat.setChoiceMode(ListView.CHOICE_MODE_NONE);
        lv_chat.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        et_message = findViewById(R.id.et_message);
        btn_send = findViewById(R.id.btn_send);
        btn_send.setOnClickListener(sendOnClick);
    }

    private void DialogViewMyIP() {
        WifiManager wifi;
        WifiInfo info;

        wifi = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        info = wifi.getConnectionInfo();

        int ipAddr = info.getIpAddress();
        String MyIP = String.format("%d.%d.%d.%d",
                (ipAddr & 0xff), (ipAddr >> 8 & 0xff),
                (ipAddr >> 16 & 0xff), (ipAddr >> 24 & 0xff));

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("My IP: " + MyIP);
                alertDialogBuilder.setPositiveButton("Close",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void DialogViewIP() {
        Context context = MainActivity.this;
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_viewip, (ViewGroup)findViewById(R.id.layout_viewip));
        final TextView tvIP = layout.findViewById(R.id.tv_ip);
        final TextView tvPORT = layout.findViewById(R.id.tv_port);

        tvIP.setText(IP);
        tvPORT.setText(String.valueOf(Port));

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("View IP");
        builder.setView(layout);
        builder.setPositiveButton("OK", null);
        builder.create().show();
    }

    private void DialogSetIP() {
        Context context = MainActivity.this;
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.dialog_setip, (ViewGroup)findViewById(R.id.layout_setip));
        final EditText etIP = layout.findViewById(R.id.et_ip);
        final EditText etPORT = layout.findViewById(R.id.et_port);

        if(IP != "" && Port != 0)
        {
            etIP.setText(IP);
            etPORT.setText(String.valueOf(Port));
        }

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle("Set IP");
        builder.setView(layout);
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String IP_temp = etIP.getText().toString();
                String PORT_str_temp = etPORT.getText().toString();
                if(IP_temp.length() != 0 && PORT_str_temp.length() != 0)
                {
                    IP = IP_temp;
                    Port = Integer.valueOf(PORT_str_temp);
                    mUdpService.set(IP, Port);
                    Toast.makeText(MainActivity.this, "Set", Toast.LENGTH_SHORT).show();
                    DialogViewIP();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Wrong IP or Port.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Back", null);
        builder.create().show();
    }

    private class  SendMessageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {

            try{
                mUdpService.send(strings[0]);
            } catch (Exception e) {
                Log.e(TAG, "Exception", e);
            }

            return null;
        }
    }
}
