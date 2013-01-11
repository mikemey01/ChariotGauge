package com.chariotinstruments.chariotgauge;

import java.math.BigDecimal;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;

import android.view.ViewManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class OilActivity extends Activity {
	
	GaugeBuilder analogGauge;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
	
    float 	     flt;
    TextView 	 txtViewDigital;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    double		 sensorMinValue; //the lowest value that has been sent from the arduino.
    double		 sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean		 paused;
    
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    
    double		 lowPSI;
    double		 lowOhms;
    double		 highPSI;
    double		 highOhms;
    double		 lowVolts;
    double		 highVolts;
    double		 rangeVolts;
    double		 rangePSI;
    double		 biasResistor;
    
    String test;
        
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 4;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    initSensor();
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge = (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);
	    
	    //Set up the gauge values and the values that are handled from the sensor.
	    minValue = 0;
	    maxValue = 100;
	    sensorMinValue = 0;
	    sensorMaxValue = minValue;
	    
	    //Set up the Boost GaugeBuilder
	    analogGauge.setTotalNotches(80);
	    analogGauge.setIncrementPerLargeNotch(10);
	    analogGauge.setIncrementPerSmallNotch(2);
	    analogGauge.setScaleCenterValue(50);
	    analogGauge.setScaleMinValue(minValue);
	    analogGauge.setScaleMaxValue(maxValue);
	    analogGauge.setUnitTitle("Oil Pressure(PSI)");
	    analogGauge.setValue(minValue);
	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
	    //Assign it to global mSerialService variable in this activity.
	    mSerialService = (BluetoothSerialService) obj;
	    //Update the BluetoothSerialService instance's handler to this activities'.
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
	        ((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge due to fading for now.
	    }
	    
	}
    
    private void initSensor(){
    	lowVolts = (lowOhms/(biasResistor+lowOhms))*5;
    	highVolts = (highOhms/(biasResistor+highOhms))*5;
    	rangeVolts = highVolts - lowVolts;
    	rangePSI = highPSI - lowPSI;
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
test = readMessage;
					//Redraw the needle to the correct value.
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
    	double oil = 0;
    	double vOut = 0;
    	double vPercentage;
    	   	
    	if(!paused){
    		vOut = (sValue*5.00)/1024; //get voltage
    		
    		vOut = vOut - lowVolts; //get on the same level as the oil pressure sensor
    		if(vOut == 0){ //Remove divide by 0 errors.
    			vOut = .01;
    		}
    		vPercentage = vOut / rangeVolts; //find the percentage of the range we're at
    		oil = vPercentage * rangePSI; //apply same percentage to range of oil.
    		
    		if(oil < minValue){ //set the lower bounds on the data.
    			txtViewDigital.setText(Float.toString((float)round(minValue)));
    			analogGauge.setValue((float)round(minValue));
    		}else if (oil > maxValue){ //set the upper bounds on the data.
    			txtViewDigital.setText(Float.toString((float)round(maxValue)));
    			analogGauge.setValue((float)round(maxValue));
    		}else{ //if it is in-between the lower and upper bounds as it should be, display it.
    			txtViewDigital.setText(Float.toString((float)round(oil)));
    			analogGauge.setValue((float)round(oil));
    			
    			if(round(oil) > sensorMaxValue && round(oil) <= maxValue){ //Check to see if we've hit a new high, record it.
            		sensorMaxValue = round(oil);
            	}
    		}
    	}
    }
    
    private float parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.
    	float ret = 0f;
    	
    	try {
			ret = new Float(tokens[CURRENT_TOKEN].toString());//Get current token for this gauge activity, cast as float.
		} catch (NumberFormatException e) {
			ret = 0f;
		} catch(ArrayIndexOutOfBoundsException e){ //If the MC sneezes it can potentially screw up the CSV string sent over.
			ret = 0f;
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
    
    protected void onPause(){
    	super.onPause();
    }
    
    protected void onResume(){
    	super.onResume();
    	analogGauge.invalidate();
    }
    
    public static double round(double unrounded){
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return rounded.doubleValue();
    }
    
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	
    	String slowPSI   = sp.getString("oil_psi_low", "0"); //Assign the prefs to strings since they are stored as such.
    	String slowOhms   = sp.getString("oil_ohm_low", "10");
    	String shighPSI   = sp.getString("oil_psi_high", "80");
    	String shighOhms   = sp.getString("oil_ohm_high", "180");
    	String sbiasResistor   = sp.getString("bias_resistor_oil", "100");
    	
    	try {
			lowPSI   = Float.parseFloat(slowPSI); //attempt to parse the strings to floats.
			lowOhms   = Float.parseFloat(slowOhms);
			highPSI   = Float.parseFloat(shighPSI);
			highOhms   = Float.parseFloat(shighOhms);
			biasResistor = Float.parseFloat(sbiasResistor);
		} catch (NumberFormatException e) { //If the parsing fails, assign default values to continue operation.
			System.out.println("Error in OilActivity.prefsInit "+e);
			lowPSI = 0;
			lowOhms = 10;
			highPSI = 80;
			highOhms = 180;
			biasResistor = 100;
		}
    }
    
}
