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

public class WidebandActivity extends Activity {
	
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
    
    //Prefs vars
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    String		 unitType; //AFR or Lambda.
    String		 fuelType; //Fuel type in use.
    double		 stoich;
    float		 lowVolts; //Low volts from the target table.
    float		 highVolts; //High volts from the target table.
    float		 lowAFR; //Low AFR from the target table.
    float		 highAFR; //High AFR from the target table.
    double		 afrRange;
    double		 voltRange;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 2;
        
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge = (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);
	    
	    //Set up the gauge based on prefs
        initGauge(fuelType, unitType);
	    
	    //High and low range for AFR/Volts
	    afrRange = (double)(highAFR - lowAFR);
	    voltRange = (double)(highVolts - lowVolts);
	  
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
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	        ((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge due to fading for now.
	    }
	    
	}
    
    private void initGauge(String fuelType, String afrLambda){
    	if(afrLambda.equals("Lambda")){
    		//Set up the gauge values and the values that are handled from the sensor.
		    minValue = 0;
		    maxValue = 2;
		    sensorMinValue = minValue;
		    sensorMaxValue = minValue;
		    //Set up the Boost GaugeBuilder
		    analogGauge.setTotalNotches(7);
		    analogGauge.setIncrementPerLargeNotch(1);
		    analogGauge.setIncrementPerSmallNotch(1);
		    analogGauge.setScaleCenterValue(1);
		    analogGauge.setScaleMinValue(minValue);
		    analogGauge.setScaleMaxValue(maxValue);
		    analogGauge.setUnitTitle(fuelType + " Wideband Lambda");
		    analogGauge.setValue(minValue);
		    txtViewDigital.setText(Float.toString((float)round(minValue)));
    	}else{
    		if(fuelType.equals("Gasoline") || fuelType.equals("Propane") || fuelType.equals("Diesel")){
    			//Set up the gauge values and the values that are handled from the sensor.
    		    minValue = 5;
    		    maxValue = 25;
    		    sensorMinValue = minValue;
    		    sensorMaxValue = minValue;
    		    //Set up the Boost GaugeBuilder
    		    analogGauge.setTotalNotches(40);
    		    analogGauge.setIncrementPerLargeNotch(5);
    		    analogGauge.setIncrementPerSmallNotch(1);
    		    analogGauge.setScaleCenterValue(15);
    		    analogGauge.setScaleMinValue(minValue);
    		    analogGauge.setScaleMaxValue(maxValue);
    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
    		    analogGauge.setValue(minValue);
    		    txtViewDigital.setText(Float.toString((float)round(minValue)));
    		}else if(fuelType.equals("Methanol")){
    			//Set up the gauge values and the values that are handled from the sensor.
    		    minValue = 3;
    		    maxValue = 8;
    		    sensorMinValue = minValue;
    		    sensorMaxValue = minValue;
    		    //Set up the Boost GaugeBuilder
    		    analogGauge.setTotalNotches(10);
    		    analogGauge.setIncrementPerLargeNotch(1);
    		    analogGauge.setIncrementPerSmallNotch(1);
    		    analogGauge.setScaleCenterValue(5);
    		    analogGauge.setScaleMinValue(minValue);
    		    analogGauge.setScaleMaxValue(maxValue);
    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
    		    analogGauge.setValue(minValue);
    		    txtViewDigital.setText(Float.toString((float)round(minValue)));
    		}else if(fuelType.equals("Ethanol") || fuelType.equals("E85")){
    			//Set up the gauge values and the values that are handled from the sensor.
    		    minValue = 5;
    		    maxValue = 12;
    		    sensorMinValue = minValue;
    		    sensorMaxValue = minValue;
    		    //Set up the Boost GaugeBuilder
    		    analogGauge.setTotalNotches(10);
    		    analogGauge.setIncrementPerLargeNotch(1);
    		    analogGauge.setIncrementPerSmallNotch(1);
    		    analogGauge.setScaleCenterValue(8);
    		    analogGauge.setScaleMinValue(minValue);
    		    analogGauge.setScaleMaxValue(maxValue);
    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
    		    analogGauge.setValue(minValue);
    		    txtViewDigital.setText(Float.toString((float)round(minValue)));
    		}else{
    			//Set up the gauge values and the values that are handled from the sensor.
    		    minValue = 5;
    		    maxValue = 25;
    		    sensorMinValue = minValue;
    		    sensorMaxValue = minValue;
    		    //Set up the Boost GaugeBuilder
    		    analogGauge.setTotalNotches(40);
    		    analogGauge.setIncrementPerLargeNotch(5);
    		    analogGauge.setIncrementPerSmallNotch(1);
    		    analogGauge.setScaleCenterValue(15);
    		    analogGauge.setScaleMinValue(minValue);
    		    analogGauge.setScaleMaxValue(maxValue);
    		    analogGauge.setUnitTitle("Wideband AFR");
    		    analogGauge.setValue(minValue);
    		    txtViewDigital.setText(Float.toString((float)round(minValue)));
    		}
    	}
    	initStoich(fuelType); //Set up the stoich variable.
    }
    
    private void initStoich(String fuelType){ //Sets up stoich variable for lambda calc.
    	if(fuelType.equals("Gasoline")){
    		stoich = 14.7d;
    	}else if(fuelType.equals("Propane")){
    		stoich = 15.67d;
    	}else if(fuelType.equals("Methanol")){
    		stoich = 6.47d;
    	}else if(fuelType.equals("Diesel")){
    		stoich = 14.6d;
    	}else if(fuelType.equals("Ethanol")){
    		stoich = 9d;
    	}else if(fuelType.equals("E85")){
    		stoich = 9.76d;
    	}else{
    		stoich = 14.7d;
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
    	double vOut;
    	double vPercentage;
    	double o2=0;

    	if(!paused){
    		
    		vOut = (sValue*voltRange)/1024;
    		vPercentage = vOut / voltRange;
    		o2 = lowAFR + (afrRange * vPercentage);
    		
    		if(unitType.equals("Lambda")){ //If unit type is set to lambda, convert afr to lambda.
    			o2 = o2/stoich;
    		}
    		
    		txtViewDigital.setText(Float.toString((float)round(o2)));
    		analogGauge.setValue((float)round(o2));
    		
    		if(round(o2) > sensorMaxValue && round(o2) <= maxValue){
        		sensorMaxValue = round(o2);
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
		} catch (ArrayIndexOutOfBoundsException e){
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
    
    public void prefsInit(){/**TODO:**/
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	unitType = sp.getString("widebandUnits", "AFR");
    	fuelType = sp.getString("widebandFuelType", "Gasoline");
    	
    	String sLowVolts = sp.getString("afrvoltage_low_voltage", "0.00");
    	String sHighVolts = sp.getString("afrvoltage_high_voltage", "5.00");
    	String sLowAFR = sp.getString("afrvoltage_low_afr", "7.35");
    	String sHighAFR = sp.getString("afrvoltage_high_afr", "22.39");
    	try {
			lowVolts = Float.parseFloat(sLowVolts);
			highVolts = Float.parseFloat(sHighVolts);
			lowAFR = Float.parseFloat(sLowAFR);
			highAFR = Float.parseFloat(sHighAFR);
		} catch (NumberFormatException e) {
			System.out.println("Error in WidebandActivity.prefsInit()"+e);
			Toast.makeText(getApplicationContext(), "Error in AFR Calibration table, restoring defaults.", Toast.LENGTH_SHORT).show();
			lowVolts = 0.00f;
			highVolts = 5.00f;
			lowAFR = 7.35f;
			highAFR = 22.39f;
		}
    }
    
}
