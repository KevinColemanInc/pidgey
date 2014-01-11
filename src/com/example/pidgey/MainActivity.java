package com.example.pidgey;

//00:12:3E:FF:1C:E8 //device

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	
	private int mTotalNumberOfReads = 0;
	private int mCurrentIndex = 0;
	private String mSerialNumber;
	private String mNumberOfReadings;
	private Date mCurrentDate;
	private List<Reading> mReadings = new ArrayList<Reading>();
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 3];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 3] = hexArray[v >>> 4];
	        hexChars[j * 3 + 1] = hexArray[v & 0x0F];
	        hexChars[j * 3 + 2] = ' ';
	    }
	    return new String(hexChars);
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate2() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mFetchSerialNumber = (Button) findViewById(R.id.fetch_sn_button);
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
            	Log.d(TAG, "...Handling a message...");
                switch (msg.what) {
                case RECIEVE_MESSAGE:                                                // if receive massage
                	Log.d(TAG, "...Got something!...");
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
                    sb.append(strIncom);                                                // append string
                    
                    Log.d(TAG, "Received txCmd [" + bytesToHex(readBuf) + "]");
                    
                    if (readBuf[1] == 0x28) //Serial Number Requested. Get next half.
                    {
                    	byte[] data = new byte[] {readBuf[5],readBuf[4],readBuf[3],readBuf[2]};
                    	mSerialNumber = bytesToHex(data);
                    	
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
                    }
                    else if (readBuf[1] == 0x27) //Received second half of SN. Get reading count.
                    {
                    	byte[] data = new byte[] {readBuf[5],readBuf[4],readBuf[3],readBuf[2]};
                    	mSerialNumber += bytesToHex(data);
                    	mSerialNumber = mSerialNumber.replace(" ", "");
                    	Log.d(TAG, "...Serial Number:"+ mSerialNumber + "...");
                    	 
                    	byte[] serialCmd = new byte[8];
                    	serialCmd[0] = 0x51;
                    	serialCmd[1] = 0x2B;
                    	serialCmd[2] = 0x00;
                    	serialCmd[3] = 0x00;
                    	serialCmd[4] = 0x00;
                    	serialCmd[5] = 0x00;
                    	serialCmd[6] = (byte)0xa3;
                    	serialCmd[7] = 0x1F;        	
                    	mConnectedThread.write(serialCmd);
                    }
                    else if (readBuf[1] == 0x2B) // Receive reading count and get date of next one
                    {
                    	byte[] data = new byte[] {readBuf[5],readBuf[4],readBuf[3],readBuf[2]};
                    	mNumberOfReadings = bytesToHex(data);
                    	
                    	mTotalNumberOfReads = (readBuf[3] << 8) | readBuf[2];
                    	mCurrentIndex = mTotalNumberOfReads-1;//index start at 0
                    	Log.d(TAG, "...Num of Readings:"+ mTotalNumberOfReads + "...");
                    	
                    	byte[] serialCmd = new byte[8];
                    	serialCmd[0] = 0x51;
                    	serialCmd[1] = 0x25;
                    	serialCmd[2] = (byte)mCurrentIndex;
                    	serialCmd[3] = 0x00;
                    	serialCmd[4] = 0x00;
                    	serialCmd[5] = 0x00;
                    	serialCmd[6] = (byte)0xa3;
                    	serialCmd[7] = 0x19;        	
                    	mConnectedThread.write(serialCmd);
                    	
                    }
                    else if (readBuf[1] == 0x25) // Receive date of next one and get the readings
                    {
                    	byte[] data = new byte[] {readBuf[5],readBuf[4],readBuf[3],readBuf[2]};
                    	Log.d(TAG, "Date Reading: " + bytesToHex(data));
                    	int sum = (readBuf[3] << 8)  | readBuf[2];
                    	int timeSum = (readBuf[5] << 8)  | readBuf[4];
                    	int day = (sum) & 0x1F;
                    	int year = sum >> 9;
                    	int month = (sum & (0x0F << 5)) >> 5;
                    	int min = timeSum & 0x3F;
                    	
                    	Log.d(TAG, "Day " +day + " /Month " + month + " /Year " + year + " Hour " + readBuf[4] + " min " + min);
                    	mCurrentDate = new Date();
                    	mCurrentDate.setDate(day);
                    	mCurrentDate.setHours(readBuf[4]);
                    	mCurrentDate.setMinutes(min);
                    	mCurrentDate.setMonth(month-1);
                    	mCurrentDate.setYear(year + 100);
                    	
                    	byte[] serialCmd = new byte[8];
                    	serialCmd[0] = 0x51;
                    	serialCmd[1] = 0x26;
                    	serialCmd[2] = 0x00;
                    	serialCmd[3] = (byte) mCurrentIndex;
                    	serialCmd[4] = 0x00;
                    	serialCmd[5] = 0x00;
                    	serialCmd[6] = (byte)0xa3;
                    	serialCmd[7] = 0x1a;        	
                    	mConnectedThread.write(serialCmd);
                    	                   	
                    }
                    else if (readBuf[1] == 0x26) // Receive the readings, decrement index, continue if index > 0 else upload to server
                    {
                    	byte[] data = new byte[] {readBuf[5],readBuf[4],readBuf[3],readBuf[2]};
                    	Log.d(TAG, "Data Reading: " + bytesToHex(data));
                    	
                    	int glucose_value = ((readBuf[3] << 8) | (readBuf[2] & 0xFF));
                    	int typeAndCode = ((readBuf[5] << 8) | readBuf[4]);
                    	int type = typeAndCode >> 12;
                    	int code_no = typeAndCode & 0xFFF;
                    	Reading read = new Reading(mCurrentDate, glucose_value, code_no, type, mSerialNumber);
                    	Log.d(TAG, "Reading.ToString(): " + read.toString());
                    	mReadings.add(read);
                    	
                    	mCurrentIndex -= 1;
                    	if (mCurrentIndex > 0) {
                    		Log.d(TAG, "Fetching next index...");
	                    	byte[] serialCmd = new byte[8];
	                    	serialCmd[0] = 0x51;
	                    	serialCmd[1] = 0x25;
	                    	serialCmd[2] = 0x00;
	                    	serialCmd[3] = (byte) mCurrentIndex;
	                    	serialCmd[4] = 0x00;
	                    	serialCmd[5] = 0x00;
	                    	serialCmd[6] = (byte) ((byte)0xa3 + (byte) mCurrentIndex);
	                    	serialCmd[7] = 0x19;        	
	                    	mConnectedThread.write(serialCmd);
                    	}
                    	else
                    	{
                    		//write up to server
                    		Log.d(TAG, "Writing to server...");
                    		
                    		for (Reading r: mReadings) {
                    			Log.d(TAG, "Sending reading " + r.toString());
                    		    r.sendToServer();
                    		}
                    	}
                    	                   	
                    }
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
        	serialCmd[1] = 0x28;
        	serialCmd[2] = 0x00;
        	serialCmd[3] = 0x00;
        	serialCmd[4] = 0x00;
        	serialCmd[5] = 0x00;
        	serialCmd[6] = (byte)0xa3;
        	serialCmd[7] = 0x1C;        	
        	mConnectedThread.write(serialCmd);
        	
            Toast.makeText(getBaseContext(), "Data Sent", Toast.LENGTH_SHORT).show();
          }
        });
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
                byte[] buffer = new byte[8];  // buffer store for the stream
                int bytes = 0; // bytes returned from read()
     
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                    	byte[] tempBuffer = new byte[8];
                    	
                    	int tempBytes = mmInStream.read(tempBuffer);        // Get number of bytes and message in "buffer"
                    	Log.d(TAG, "...Received Data: " + tempBytes + "...");
                    	
                    	for(int i = bytes; i < bytes + tempBytes; i++)
                    	{
                    		buffer[i] = tempBuffer[i-bytes];
                    	}
                    	bytes += tempBytes;
                    	
                        Log.d(TAG, "...Total received Data: " + bytes + "...");
                        
                        if (bytes == 8) // wait until entire packet is received
                        {
                        	Log.d(TAG, "...Complete message: " + bytes + "...");
                        	h.obtainMessage(RECIEVE_MESSAGE, 8, -1, buffer).sendToTarget();     // Send to message queue Handler
                        	buffer = new byte[8];  // flush buffer
                        	bytes = 0;
                        }
                        
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
