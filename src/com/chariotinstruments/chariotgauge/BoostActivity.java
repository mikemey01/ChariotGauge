package com.chariotinstruments.chariotgauge;

import java.math.BigDecimal;

import android.app.Activity;
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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BoostActivity extends Activity {
	
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
    String		 pressureUnits;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    //Test
    
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST       = "toast";
    public static final double KPA_TO_PSI  = 0.14503773773020923;
    public static final double ATMOSPHERIC = 101.325;
    public static final double KPA_TO_INHG = 0.295299830714;
    private static final int CURRENT_TOKEN = 1;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge		= (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);
	    
        if(pressureUnits.equals("KPA")){
        	//Set up the gauge values and the values that are handled from the sensor for KPA
        	minValue = 0;
		    maxValue = 250;
		    sensorMinValue = 0;
		    sensorMaxValue = minValue;
		    
		    //Set up the Boost GaugeBuilder for KPA
		    analogGauge.setTotalNotches(65);
		    analogGauge.setIncrementPerLargeNotch(25);
		    analogGauge.setIncrementPerSmallNotch(5);
		    analogGauge.setScaleCenterValue(150);
		    analogGauge.setScaleMinValue(minValue);
		    analogGauge.setScaleMaxValue(maxValue);
		    analogGauge.setUnitTitle("Boost (KPA)");
		    analogGauge.setValue(minValue);
		    txtViewDigital.setText(Float.toString((float)round(minValue)));
        }else{
        	//Set up the gauge values and the values that are handled from the sensor for PSI
        	minValue = -10;
		    maxValue = 25;
		    sensorMinValue = 0;
		    sensorMaxValue = minValue;
		    
		    //Set up the Boost GaugeBuilder for PSI
		    analogGauge.setTotalNotches(55);
		    analogGauge.setIncrementPerLargeNotch(5);
		    analogGauge.setIncrementPerSmallNotch(1);
		    analogGauge.setScaleCenterValue(10);
		    analogGauge.setScaleMinValue(minValue);
		    analogGauge.setScaleMaxValue(maxValue);
		    analogGauge.setUnitTitle("Boost (PSI)");
		    analogGauge.setValue(minValue);
		    txtViewDigital.setText(Float.toString((float)round(minValue)));
        }
	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
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
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	        ((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge due to fading for now.
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
    	double kpa=0;
    	double psi=0;
    	   	
    	if(!paused){
    		vOut = (sValue*5.00)/1024;
    		kpa = ((vOut/5.00)+.04)/.004;
    		kpa = kpa - ATMOSPHERIC;
    		psi = kpa * KPA_TO_PSI;
    		if(pressureUnits.equals("KPA")){ //Gauge displayed in KPA
    			txtViewDigital.setText(Float.toString((float)round(kpa+ATMOSPHERIC)));
    			analogGauge.setValue((float)round(kpa+ATMOSPHERIC));
    			
    			if(round(kpa) > sensorMaxValue && round(kpa) <= maxValue){
            		sensorMaxValue = round(kpa);
            	}
    		}else{ //Gauge displayed in PSI
    			if(psi < minValue){ //set the lower bounds on the data.
    				txtViewDigital.setText(Float.toString((float)round(minValue)));
					analogGauge.setValue((float)round(minValue));
    			}else if (psi > maxValue){ //Set the upper bounds on the data
    				txtViewDigital.setText(Float.toString((float)round(maxValue)));
					analogGauge.setValue((float)round(maxValue));
    			}else{ //if it is in-between the lower and upper bounds as it should be, display it.
    				txtViewDigital.setText(Float.toString((float)round(psi)));
					analogGauge.setValue((float)round(psi));

					if(round(psi) > sensorMaxValue && round(psi) <= maxValue){
						sensorMaxValue = round(psi);
					}
    			}
    		} //PSI closing paren.
    	} //If paused closing paren.
    } //handleSensor closing paren.
    
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
		finish();
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
    	pressureUnits = sp.getString("pressureUnits", "PSI");
    }

    
}
