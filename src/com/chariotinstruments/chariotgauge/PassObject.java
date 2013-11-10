package com.chariotinstruments.chariotgauge;

public class PassObject {
	private static Object object;
	
	//Set the object to be picked up by a separate activity.
	public static void setObject(Object obj){
		object = obj;
	}
	
	//Get the object that has been set.
	public static Object getObject(){
		Object obj = object;
		
		// can get only once
		object = null;
		return obj;
	}
}
