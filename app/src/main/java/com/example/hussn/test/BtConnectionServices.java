package com.example.hussn.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


/**
 * Created by Hussni on 9/22/2018.
 */

public class BtConnectionServices {


    private static final String TEG = "BtConnectionServices";

    private static final String appname = "MYAPP";
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private BluetoothDevice mmDevice;
    private BluetoothDevice Tuppy;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler mHandler; // handler that gets info from Bluetooth service

    private ParcelUuid[] TpUuid;
    private UUID TupUuid;
    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }


    public BtConnectionServices(Context context, BluetoothDevice tupperware){
        Log.d(TAG, "BtConnectionServices: started with " + tupperware.getName());
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Tuppy = tupperware;
        TpUuid = tupperware.getUuids();
        TupUuid = TpUuid[0].getUuid();
        start();
    }

    public synchronized void start(){
        if(mConnectThread !=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if(mInsecureAcceptThread ==null){
            Log.d(TAG, "start: creating AcceptThread for device");
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    
    public void startClient(BluetoothDevice device){
        Log.d(TAG, "startClient: new ConnectThread made");
        mConnectThread = new ConnectThread(device);
    }
    private void manageMyConnectedSocket(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void CancelConnections(){
        mConnectedThread.cancel();
    }
    public boolean isConnected(){
        if(mConnectedThread.mmSocket==null){
            return false;
        }
        else {
            return true;
        }
    }

    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            // Use temporary bluetooth socket that is later assigned to mmServerSocket
            // because mmServerSocket is final
            BluetoothServerSocket tmp=null;

            try {
                Log.d(TAG, "AcceptThread: listening using RF comm with service road");
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(appname,TupUuid);
            }catch (IOException e){
                Log.e(TAG,"Socket's listen() method failed",e);
            }
            mmServerSocket=tmp;
        }
        public void run() {
            BluetoothSocket socket = null;
            Log.d(TAG, "run: listening as server");
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    Log.d(TAG, "run: A connection was accepted");
                    manageMyConnectedSocket(socket,mmDevice);

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
        
    }
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;
            ParcelUuid[] uuid = mmDevice.getUuids();

            Log.d(TAG, "ConnectThread: creating RfcommSocket To Service Road");
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                tmp = device.createRfcommSocketToServiceRecord(uuid[0].getUuid());
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            Log.d(TAG, "ConnectThread: Bluetooth Socket create for " + mmDevice.getName());
            mmSocket = tmp;
            run();
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                Log.d(TAG, "run: blocking call to connect to the remote device");
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Log.d(TAG, "run: Managing connected sockets");
            manageMyConnectedSocket(mmSocket,mmDevice);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream
        

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: new connected thread created");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            Log.d(TAG, "ConnectedThread: Input and output streams made");
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            String test = "0000";
            try {
                mmOutStream.write(test.getBytes());
                Log.d(TAG, "ConnectedThread: test data sent");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                // Share the sent message with the UI activity.
                /*Message writtenMsg = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
                */
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }





    public void write(byte[] out){
        mConnectedThread.write(out);
    }





}
