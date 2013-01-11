package com.chariotinstruments.chariotgauge;

import java.math.BigDecimal;
import java.lang.Math;
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

public class TemperatureActivity extends Activity {
	
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
    String 		 tempUnits;
    
    double		 tempOne;
    double		 tempTwo;
    double		 tempThree;
    double		 ohmsOne;
    double		 ohmsTwo;
    double		 ohmsThree;
    double		 biasRes;
    
    //SHH Coefficients
    double a;
    double b;
    double c;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 3;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    getSHHCoefficients(); //Initialize the Steinhart-Hart coefficients to be used in the final equation.
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge = (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);

        if(tempUnits.toLowerCase().equals("celsius")){
		    //Set up the gauge values and the values that are handled from the sensor for Celsius.
		    minValue = -35;
		    maxValue = 105;
		    sensorMinValue = minValue;
		    sensorMaxValue = minValue;
		    
		    //Set up the Boost GaugeBuilder
		    analogGauge.setTotalNotches(60);
		    analogGauge.setIncrementPerLargeNotch(20);
		    analogGauge.setIncrementPerSmallNotch(4);
		    analogGauge.setScaleCenterValue(65);
		    analogGauge.setScaleMinValue(minValue);
		    analogGauge.setScaleMaxValue(maxValue);
		    analogGauge.setUnitTitle("Temperature (C)");
		    analogGauge.setValue(minValue);
		    txtViewDigital.setText(Float.toString((float)round(minValue)));
        }else{
        	//Set up the gauge values and the values that are handled from the sensor for Fahrenheit.
		    minValue = -20;
		    maxValue = 220;
		    sensorMinValue = 0;
		    sensorMaxValue = minValue;
		    
		    //Set up the Boost GaugeBuilder
		    analogGauge.setTotalNotches(45);
		    analogGauge.setIncrementPerLargeNotch(40);
		    analogGauge.setIncrementPerSmallNotch(8);
		    analogGauge.setScaleCenterValue(140);
		    analogGauge.setScaleMinValue(minValue);
		    analogGauge.setScaleMaxValue(maxValue);
		    analogGauge.setUnitTitle("Temperature (F)");
		    analogGauge.setValue(minValue);
		    txtViewDigital.setText(Float.toString((float)round(minValue)));
        }
	  
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
    
    private void getSHHCoefficients(){
    	double numer;
    	double denom;
    	
    	//Start with C
    	numer = ((1/(tempOne+273.15)-1/(tempTwo+273.15))-(Math.log(ohmsOne)-Math.log(ohmsTwo))*(1/(tempOne+273.15)-1/(tempThree+273.15))/(Math.log(ohmsOne)-Math.log(ohmsThree)));
    	denom = ((Math.pow((Math.log(ohmsOne)), 3)-Math.pow((Math.log(ohmsTwo)), 3) - (Math.log(ohmsOne)-Math.log(ohmsTwo))*(Math.pow((Math.log(ohmsOne)),3)-Math.pow((Math.log(ohmsThree)), 3))/(Math.log(ohmsOne)-Math.log(ohmsThree))));
    	c = numer / denom;
    	
    	//Then B
    	b = ((1/(tempOne+273.15)-1/(tempTwo+273.15))-c*(Math.pow((Math.log(ohmsOne)), 3)-Math.pow((Math.log(ohmsTwo)), 3)))/(Math.log(ohmsOne)-Math.log(ohmsTwo));
    	
    	//Finally A
    	a = 1/(tempOne+273.15)-c*Math.pow((Math.log(ohmsOne)), 3)-b*Math.log(ohmsOne);
    }
    
    private double getTemperature(double resistance){
    	double ret = 0;
    	
    	//This is the final equation.
    	try {
			ret = ((1/(a+(b*Math.log(resistance))+(c*(Math.pow((Math.log(resistance)), 3)))))-273.15);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("Error in TemperatureActivity.getTemperature"+e);
			Toast.makeText(getApplicationContext(), "Something happened when calculating the temperature. I'm so sorry.", Toast.LENGTH_SHORT).show();
		}
    	
    	return ret;
    }
    
    private double getResistance(float ADC){
    	double numer;
    	double denom;
    	double ret;
    	
    	numer = biasRes*(ADC/1024);
    	denom = Math.abs((ADC/1024)-1);
    	
    	ret = numer / denom;
    	
    	return ret;
    }
    
    private double getF(double c){
    	return (c*1.8)+32;
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
    	double res;
    	double temp;
    	
    	if(!paused){
    		res = getResistance(sValue);
        	temp = getTemperature(res);
    		if(tempUnits.toLowerCase().equals("celsius")){ //Celsius---
    			if(temp < minValue){ //set the lower bounds on the data.
    				txtViewDigital.setText(Float.toString((float)round(minValue)));
	    			analogGauge.setValue((float)round(minValue));
    			}else if (temp > maxValue){ //set the upper bounds on the data.
    				txtViewDigital.setText(Float.toString((float)round(maxValue)));
	    			analogGauge.setValue((float)round(maxValue));
    			}else{ //if it is in-between the lower and upper bounds as it should be, display it.
	    			txtViewDigital.setText(Float.toString((float)round(temp)));
	    			analogGauge.setValue((float)round(temp));
	    		
	    			if(round(temp) > sensorMaxValue && round(temp) <= maxValue){
	    				sensorMaxValue = round(temp);
	    			}
    			}
    		}else{ //Fahrenheit---
    			if(getF(temp) < minValue){
    				txtViewDigital.setText(Float.toString((float)round(minValue)));
	    			analogGauge.setValue((float)round(minValue));
    			}else if(getF(temp) > maxValue){
    				txtViewDigital.setText(Float.toString((float)round(maxValue)));
	    			analogGauge.setValue((float)round(maxValue));
    			}else{
	    			txtViewDigital.setText(Float.toString((float)round(getF(temp))));
	    			analogGauge.setValue((float)round(getF(temp)));
	    		
	    			if(round(getF(temp)) > sensorMaxValue && round(getF(temp)) <= maxValue){
	    				sensorMaxValue = round(getF(temp));
	    			}
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
    	Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
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
    	
    	tempUnits = sp.getString("tempUnits", "fahrenheit");
    	
    	String stempOne   = sp.getString("temp_one", "-18.00");
    	String stempTwo   = sp.getString("temp_two", "4.00");
    	String stempThree = sp.getString("temp_three", "99.00");
    	String sohmsOne	  = sp.getString("ohms_one", "25000");
    	String sohmsTwo	  = sp.getString("ohms_two", "7500");
    	String sohmsThree = sp.getString("ohms_three", "185");
    	String sbiasRes   = sp.getString("bias_resistor", "2000");
    	
    	try {
			tempOne   = Float.parseFloat(stempOne);
			tempTwo   = Float.parseFloat(stempTwo);
			tempThree = Float.parseFloat(stempThree);
			ohmsOne   = Float.parseFloat(sohmsOne);
			ohmsTwo   = Float.parseFloat(sohmsTwo);
			ohmsThree = Float.parseFloat(sohmsThree);
			biasRes   = Float.parseFloat(sbiasRes);
		} catch (NumberFormatException e) {
			System.out.println("Error in WidebandActivity.prefsInit()"+e);
			Toast.makeText(getApplicationContext(), "Error in Temp Calibration table, restoring defaults", Toast.LENGTH_SHORT).show();
			tempOne = -18.00d;
			tempTwo = 4.00d;
			tempThree = 99.00d;
			ohmsOne = 25000d;
			ohmsTwo = 7500d;
			ohmsThree = 185d;
			biasRes = 2000d;
		}
    }
    
}
