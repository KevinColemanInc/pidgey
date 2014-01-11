package com.example.pidgey;

import java.util.Date;

public class Reading {

	 // the Bicycle class has
    // three fields
    private Date mRead_at;
    private int mGlucose_value;
    private int mCode_no;
    private int mType;
        
    // the Bicycle class has
    // one constructor
    public Reading(Date read_at, int glucose_value, int code_no, int type) {
    	mRead_at = read_at;
    	mGlucose_value = glucose_value;
    	mCode_no = code_no;
    	mType = type;
    }
    
    public String toString()
    {
    	return "value " + mGlucose_value + " code " + mCode_no + " type " + mType;
    }
    public boolean sendToServer()
    {
    	return true;
    }

}
