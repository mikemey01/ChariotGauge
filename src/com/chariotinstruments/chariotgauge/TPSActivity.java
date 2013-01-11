package com.chariotinstruments.chariotgauge;

import java.math.BigDecimal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class TPSActivity extends Activity{
	
    GaugeBuilder analogGauge;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
    
    float 	     flt;
    TextView 	 txtViewDigital;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    int			 sensorMinValue; //the lowest value that has been sent from the arduino.
    int			 sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean		 paused;
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 0;
    
    BluetoothSerialService mSerialService;
     
    @Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.gauge_layout);
    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
    prefsInit(); //Load up the preferences.
    
    //Instantiate the gaugeBuilder.
    analogGauge 	= (GaugeBuilder) findViewById(R.id.analogGauge);
    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
    btnOne			= (ImageButton) findViewById(R.id.btnOne);
    btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
    typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
    
    //Set the font of the title text
    txtViewDigital.setTypeface(typeFaceDigital);
    
    //Set up the gauge values and the values that are handled from the sensor.
    minValue = 0;
    maxValue = 100;
    sensorMinValue = 1000;
    sensorMaxValue = minValue;
    
    //Set up the TPS GaugeBuilder
    analogGauge.setTotalNotches(80);
    analogGauge.setIncrementPerLargeNotch(10);
    analogGauge.setIncrementPerSmallNotch(2);
    analogGauge.setScaleCenterValue(50);
    analogGauge.setScaleMinValue(minValue);
    analogGauge.setScaleMaxValue(maxValue);
    analogGauge.setUnitTitle("Throttle");
    analogGauge.setValue(minValue);
  
    //Get the mSerialService object from the UI activity.
    Object obj     = PassObject.getObject();
    //Assign it to global mSerialService variable in this activity.
    mSerialService = (BluetoothSerialService) obj;
    //Update the BluetoothSerialService instance's handler to this activities.
    mSerialService.setHandler(mHandler);
    
    if(!showAnalog){
    	((ViewManager)analogGauge.getParent()).removeView(analogGauge); //Remove analog gauge
    }
    if(!showDigital){
    	((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge
    }
    if(showNightMode){
    	root = btnOne.getRootView(); //Get root layer view.
        root.setBackgroundColor(android.R.color.black); //Set background color to black.
    }
}
    
    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case MESSAGE_READ:
            		
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

					//Parse the CSV then Redraw the needle to the correct value.
					handleSensor(parseInput(readMessage));
                    
            		break;
            	case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                   Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    
    
    private void handleSensor(float sValue){
    	float lowHighSens;
    	float currentSens;
    	float ret;
    	
    	//Dynamically update max/min vals.
    	if(sValue > sensorMaxValue){
    		sensorMaxValue = (int)sValue;
    	}else if(sValue < sensorMinValue){
    		sensorMinValue = (int)sValue;
    	}
    	
    	//Get averages of max/min sensor readings.
    	lowHighSens = sensorMaxValue - sensorMinValue;
    	currentSens = sValue - sensorMinValue;
    	
    	//no errors no errors.
    	if(currentSens<1){
    		currentSens = 1;
    	}
    	
    	//Keep the values within the bounds of the gauge.
    	ret = (currentSens / lowHighSens)*100;
    	if(ret<minValue){
    		ret = minValue;
    	}else if(ret>maxValue){
    		ret = maxValue;
    	}
    	
    	//Set the values to digital/analog gauges.
    	analogGauge.setValue(ret);
    	txtViewDigital.setText(Integer.toString(Math.round(sValue)));
    }
    
    private float parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.
    	float ret = 0f;
    	
    	try {
			ret = new Float(tokens[CURRENT_TOKEN].toString());//Get current token for this gauge activity, cast as float.
		} catch (NumberFormatException e) {
			ret = 0f;
			System.out.println("Error in TPSActivity.parseInput: "+e);
		}
    	
    	return ret;
    }

       
    //Activity transfer handling
    public void goHome(View v){
    	PassObject.setObject(mSerialService);
    	onBackPressed();
		//final Intent intent = new Intent(this, PSensor.class);
		//this.startActivity (intent);
	}
    
  //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	sensorMaxValue = minValue;
    	Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		//set the gauge/digital to the max value captured so far for two seconds.
        	txtViewDigital.setText(Float.toString((float)round(sensorMaxValue)));
        	analogGauge.setValue((float)round(sensorMaxValue));
        	btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
    	}else{
    		paused = false;
    		btnTwo.setBackgroundResource(Color.TRANSPARENT);
    	}
	}
    
    public static double round(double unrounded){
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }
    
    public void prefsInit(){/**TODO:**/
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    }

}
