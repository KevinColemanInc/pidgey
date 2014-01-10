package com.example.pidgey;

//00:12:3E:FF:1C:5C
//00:12:3E:FF:1C:E8 //device
//4C:80:93:91:C7:C3 //sparklii

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static Handler h;
	    
	final int RECIEVE_MESSAGE = 1;        // Status  for Handler
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private StringBuilder sb = new StringBuilder();
	  
	private ConnectedThread mConnectedThread;
	    
	// SPP UUID service
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	  
	// MAC-address of Bluetooth module (you must edit this line)
	private static String address = "00:12:3E:FF:1C:E8";

	private static final String TAG = "com.Pidgey.MainActivity";
	
	private Button mFetchSerialNumber;
	private Button mFetchFirstLevel;
	private TextView mDebugTextView;
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 3];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 3] = hexArray[v >>> 4];
	        hexChars[j * 3 + 1] = hexArray[v & 0x0F];
	        hexChars[j * 3 + 2] = ',';
	    }
	    return new String(hexChars);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate2() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mFetchSerialNumber = (Button) findViewById(R.id.fetch_sn_button);
        mFetchFirstLevel = (Button) findViewById(R.id.fetch_first_lvl_button);
        mDebugTextView = (TextView) findViewById(R.id.debug_text_view);
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
            	Log.d(TAG, "...Handling a message...");
                switch (msg.what) {
                case RECIEVE_MESSAGE:                                                // if receive massage
                	Log.d(TAG, "...Got something!...");
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    
                    Log.d(TAG, "Receved txCmd [" + bytesToHex(readBuf) + "]");
                	
                    Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                    break;
                }
            };
        };
          
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();
      
        mFetchSerialNumber.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            //mFetchSerialNumber.setEnabled(false);
        	Log.d(TAG, "Button Pressed !");
        	byte[] serialCmd = new byte[8];
        	       	
        	serialCmd[0] = 0x51;
        	serialCmd[1] = 0x27;
        	serialCmd[2] = 0x00;
        	serialCmd[3] = 0x00;
        	serialCmd[4] = 0x00;
        	serialCmd[5] = 0x00;
        	serialCmd[6] = (byte)0xa3;
        	serialCmd[7] = 0x1B;        	
        	mConnectedThread.write(serialCmd);
        	//51,27,04,05,00,70,A5,96
            Toast.makeText(getBaseContext(), "Data Sent", Toast.LENGTH_SHORT).show();
          }
        });
      
        /*btnOff.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            //btnOff.setEnabled(false);  
            mConnectedThread.write("0");    // Send "0" via Bluetooth
            //Toast.makeText(getBaseContext(), "Turn off LED", Toast.LENGTH_SHORT).show();
          }
        }); */
      }
       
      private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
          if(Build.VERSION.SDK_INT >= 10){
              try {
                  final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                  return (BluetoothSocket) m.invoke(device, MY_UUID);
              } catch (Exception e) {
                  Log.e(TAG, "Could not create Insecure RFComm Connection",e);
              }
          }
          return  device.createRfcommSocketToServiceRecord(MY_UUID);
      }
        
      @Override
      public void onResume() {
        super.onResume();
      
        Log.d(TAG, "...onResume - try connect...");
        
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        
        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
         
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }
        
        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();
        
        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
          btSocket.connect();
          Toast.makeText(getBaseContext(), "Connection Ok", Toast.LENGTH_SHORT).show();
          Log.d(TAG, "....Connection ok...");
        } catch (IOException e) {
          try {
        	Log.d(TAG, "....Connection failed...", e);
            btSocket.close();
          } catch (IOException e2) {
            errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
          }
        }
          
        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");
        
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    
      }
      
      @Override
      public void onPause() {
        super.onPause();
      
        Log.d(TAG, "...In onPause()...");
       
        try     {
          btSocket.close();
        } catch (IOException e2) {
          errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
      }
        
      private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) { 
          errorExit("Fatal Error", "Bluetooth not support");
        } else {
          if (btAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth ON...");
          } else {
            //Prompt user to turn on Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
          }
        }
      }
      
      private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
      }
      
      private class ConnectedThread extends Thread {
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;
          
            public ConnectedThread(BluetoothSocket socket) {
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
            	Log.d(TAG, "...Started Listener Thread...");
                byte[] buffer = new byte[32];  // buffer store for the stream
                int bytes; // bytes returned from read()
     
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                    	//Log.d(TAG, "...listening...");
                        // Read from the InputStream
                    	buffer = new byte[32];  // flush buffer
                    	
                        bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                        Log.d(TAG, "...Received Data: " + bytes + "...");
                        h.obtainMessage(RECIEVE_MESSAGE, 8, -1, buffer).sendToTarget();     // Send to message queue Handler
                    } catch (IOException e) {
                    	Log.d(TAG, "...IOException...", e);
                        break;
                    }
                }
            }
          
            /* Call this from the main activity to send data to the remote device */
            public void write(byte[] msgBuffer) {
            	Log.d(TAG, String.format("Send txCmd [%02X %02X %02X %02X %02X %02X %02X %02X]", msgBuffer[0],msgBuffer[1],msgBuffer[2],msgBuffer[3],msgBuffer[4],msgBuffer[5],msgBuffer[6],msgBuffer[7]));
            	   
                try {
                	mmOutStream.write(msgBuffer);
                } catch (IOException e) {
                    Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
                  }
            }
        }
    }
