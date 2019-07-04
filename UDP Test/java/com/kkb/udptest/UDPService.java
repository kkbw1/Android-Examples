package com.kkb.udptest;

import android.os.Handler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPService extends Thread {

    // Message types sent from the UDPService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_SERVER_IP = 4;
    public static final int MESSAGE_CLIENT_IP = 5;
    public static final int MESSAGE_ADMIN = 6;
    public static final int MESSAGE_TOAST = 7;

    // Debugging
    private static final String TAG = "UDPSERVICE";
    private static final boolean D = true;

    // Member fields
    private final Handler mHandler;

    private DatagramSocket socket;

    private DatagramPacket packet_rx;
    private DatagramPacket packet_tx;

    private String IP;
    private int Port;

    private byte[] buffer_rx;
    private byte[] buffer_tx;

    private boolean conn = false;

    /**
     * Constructor. Prepares a new UDP session.
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public UDPService(Handler handler) {
        mHandler = handler;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            return;
        }
    }

    public void set(String ip, int port) {
        this.IP = ip;
        this.Port = port;
    }

    public void run() {
        conn = true;
        buffer_rx = new byte[1024];
        packet_rx = new DatagramPacket(buffer_rx, buffer_rx.length);
        String addr;
        while(conn) {
            try {
                socket.receive(packet_rx);
                addr = packet_rx.getAddress().getHostAddress()
                        + ":" + String.valueOf(packet_rx.getPort());
                mHandler.obtainMessage(UDPService.MESSAGE_ADMIN, addr).sendToTarget();
                mHandler.obtainMessage(UDPService.MESSAGE_READ, packet_rx.getLength(),
                        -1, buffer_rx).sendToTarget();
            } catch (IOException e) {

            }
        }
    }

    public void send(String message) {
        try {
            buffer_tx = message.getBytes();
            packet_tx = new DatagramPacket(buffer_tx, buffer_tx.length,
                    InetAddress.getByName(IP), Port);
            socket.send(packet_tx);
            mHandler.obtainMessage(UDPService.MESSAGE_WRITE, message).sendToTarget();
        } catch (UnknownHostException e) {

        } catch (IOException e) {

        }
    }
}
