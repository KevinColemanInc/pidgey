package com.example.pidgey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class ConnectedThread extends Thread {
	private static final String TAG = "bluetoothCT";

	private final int RECIEVE_MESSAGE = 1;        // Status  for Handler
	private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private StringBuilder sb = new StringBuilder();
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    Handler h;
  
    public ConnectedThread(BluetoothSocket socket) {
    	
    	h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                case RECIEVE_MESSAGE:                                                   // if receive massage
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
                    if (endOfLineIndex > 0) {                                            // if end-of-line,
                        String sbprint = sb.substring(0, endOfLineIndex);               // extract string
                        sb.delete(0, sb.length());                                      // and clear
                        //txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
                        //btnOff.setEnabled(true);
                        //btnOn.setEnabled(true); 
                    }
                    Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                    break;
                }
            };
    	};
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
  
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }
  
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }
    	
    
  
    public void run() {
        byte[] buffer = new byte[256];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
            } catch (IOException e) {
                break;
            }
        }
    }
  
    /* Call this from the main activity to send data to the remote device */
    public void write(String message) {
        Log.d(TAG, "...Data to send: " + message + "...");
        byte[] msgBuffer = message.getBytes();
        try {
            mmOutStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
          }
    }
}
