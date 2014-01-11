package com.example.pidgey;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

public class Reading {
	private static final String TAG = "com.Pidgey.MainActivity";

	 // the Bicycle class has
    // three fields
    private Date mRead_at;
    private int mGlucose_value;
    private int mCode_no;
    private int mType;
    private String mSerialNumber;
        
    // the Bicycle class has
    // one constructor
    public Reading(Date read_at, int glucose_value, int code_no, int type, String serialNumber) {
    	mRead_at = read_at;
    	mGlucose_value = glucose_value;
    	mCode_no = code_no;
    	mType = type;
    	mSerialNumber = serialNumber;
    }
    
    public String toString()
    {
    	return "value " + mGlucose_value + " code " + mCode_no + " type " + mType;
    }
    public boolean sendToServer()
    {
    	// Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://mew-staging.herokuapp.com/glucose_levels");

        try {
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        	String currentDateandTime = sdf.format(new Date());
        	String readDateandTime = sdf.format(mRead_at);
        	
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("serial_number", mSerialNumber));
            nameValuePairs.add(new BasicNameValuePair("glucose_value", ""+mGlucose_value));
            
            
            String _type = "";
            switch (mType) {
	            case 0:  _type = "Normal";
	                     break;
	            case 1:  _type = "Before Meal(AC)";
	                     break;
	            case 2:  _type = "Before Meal(PC)";
	                     break;
	            case 3:  _type = "CTL Mode(QC)";
	                     break;
	            default: _type = "Invalid Type";
	                     break;
            }
            nameValuePairs.add(new BasicNameValuePair("reading_type", ""+_type));
            
            nameValuePairs.add(new BasicNameValuePair("retrieved_at", currentDateandTime));
            nameValuePairs.add(new BasicNameValuePair("code_number", ""+mCode_no));
            nameValuePairs.add(new BasicNameValuePair("measured_at", readDateandTime));
            nameValuePairs.add(new BasicNameValuePair("format", "json"));
            
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            
        } catch (ClientProtocolException e) {
        	Log.d(TAG, "ClientProtocolException...", e);
        } catch (IOException e) {
        	Log.d(TAG, "IOException...", e);
        }
        return true;
    }
    
}
