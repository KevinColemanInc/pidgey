package com.example.pidgey;

//00:12:3E:FF:1C:5C
//00:12:3E:FF:1C:E8

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.*;
import android.bluetooth.*;
import android.content.Intent;

public class MainActivity extends Activity {
	private final static int REQUEST_ENABLE_BT = 1;

	private static final String TAG = "com.Pidgey.MainActivity";
	
	private Button mFetchSerialNumber;
	private Button mFetchFirstLevel;
	private TextView mDebugTextView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate2() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

        mFetchSerialNumber = (Button) findViewById(R.id.fetch_sn_button);
        mFetchFirstLevel = (Button) findViewById(R.id.fetch_first_lvl_button);
        mDebugTextView = (TextView) findViewById(R.id.debug_text_view);
        
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        
        List<String> mArrayAdapter = new ArrayList<String>();
        
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        String test = "";
        
        Log.d(TAG, "Paired devices Count " + pairedDevices.size());
	     // If there are paired devices
	     if (pairedDevices.size() > 0) {
	         // Loop through paired devices
	         for (BluetoothDevice device : pairedDevices) {
	             // Add the name and address to an array adapter to show in a ListView
	             mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	             test += " " +  device.getName() + " \t " + device.getAddress() + " ";
	             Log.d(TAG, device.getName() + " \t " + device.getAddress());
	         }
	     }
        
	     mDebugTextView.setText(test);
        mFetchSerialNumber.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        
        
        return true;
    }
    
    public void fetchSerialNumber(View view) {
    	Toast.makeText(this, "Fetching data...", Toast.LENGTH_SHORT).show();
    	
    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    	if (mBluetoothAdapter == null) {
    		Toast.makeText(this, "Cannot Find Adapter", Toast.LENGTH_SHORT).show();
    	}
    	
    	if (!mBluetoothAdapter.isEnabled()) {
    	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    	    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    	}
    	
    }
    
}
