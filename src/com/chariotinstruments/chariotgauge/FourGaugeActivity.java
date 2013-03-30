package com.chariotinstruments.chariotgauge;

import android.app.Activity;
import android.content.Context;
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

public class FourGaugeActivity extends Activity{
	GaugeBuilder analogGauge1;
	GaugeBuilder analogGauge2;
	GaugeBuilder analogGauge3;
	GaugeBuilder analogGauge4;
	MultiGauges  multiGauge1;
	MultiGauges  multiGauge2;
	MultiGauges  multiGauge3;
	MultiGauges  multiGauge4;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
    TextView 	 txtViewDigital;
    int		     digitalToken;
	
    float 	     flt;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    double		 sensorMinValue; //the lowest value that has been sent from the arduino.
    double		 sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean		 paused;
    Context		 context;
    float		 boostSValue;
    float		 wbSValue;
    float		 tempSValue;
    float		 oilSValue;
    
    //Prefs vars
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    String		 gaugeOnePref;
    String		 gaugeTwoPref;
    int			 currentTokenOne = 1;
    int			 currentTokenTwo = 2;
    
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
    private static final int BOOST_TOKEN = 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN = 3;
    private static final int OIL_TOKEN = 4;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout_4);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    context = this;
	    
	    //Instantiate the GaugeBuilder.
	    analogGauge1	= (GaugeBuilder) findViewById(R.id.analogGauge);
	    analogGauge2	= (GaugeBuilder) findViewById(R.id.analogGauge2);
	    analogGauge3	= (GaugeBuilder) findViewById(R.id.analogGauge3);
	    analogGauge4	= (GaugeBuilder) findViewById(R.id.analogGauge4);
	    multiGauge1 	= new MultiGauges(context);
	    multiGauge2 	= new MultiGauges(context);
	    multiGauge3 	= new MultiGauges(context);
	    multiGauge4 	= new MultiGauges(context);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital);
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);  
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        digitalToken	= 1;
        
        //Set the font of the digital.
        txtViewDigital.setTypeface(typeFaceDigital);
        txtViewDigital.setText("0.00");
   	    
        //Setup gauge 1
        multiGauge1.setAnalogGauge(analogGauge1);
        multiGauge1.buildGauge(1);
        
        //Setup gauge 2
        multiGauge2.setAnalogGauge(analogGauge2);
        multiGauge2.buildGauge(2);
        
        //Setup gauge 3
        multiGauge3.setAnalogGauge(analogGauge3);
        multiGauge3.buildGauge(3);
        
        //Setup gague 4
        multiGauge4.setAnalogGauge(analogGauge4);
        multiGauge4.buildGauge(4);
	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
	    //Assign it to global mSerialService variable in this activity.
	    mSerialService = (BluetoothSerialService) obj;
	    //Update the BluetoothSerialService instance's handler to this activities.
	    mSerialService.setHandler(mHandler);
	   
	    if(!showAnalog){
	    	((ViewManager)analogGauge1.getParent()).removeView(analogGauge1); //Remove analog gauge
	    }
	    if(showNightMode){
	    	root = btnOne.getRootView(); //Get root layer view.
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	    }
	    
	    Toast.makeText(getApplicationContext(), "Digital set to boost, tap a gauge to change", Toast.LENGTH_LONG).show();

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
					//update/process the inbound data.
					updateGauges(readMessage);
            		break;
            	case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                   Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    
    public void updateGauges(String msg){
    	if(!paused){
	    	parseInput(msg);
    		multiGauge1.handleSensor(boostSValue);
    		multiGauge2.handleSensor(wbSValue);
    		multiGauge3.handleSensor(tempSValue);
    		multiGauge4.handleSensor(oilSValue);

			analogGauge1.setValue(multiGauge1.getCurrentGaugeValue());
			analogGauge2.setValue(multiGauge2.getCurrentGaugeValue());
			analogGauge3.setValue(multiGauge3.getCurrentGaugeValue());
			analogGauge4.setValue(multiGauge4.getCurrentGaugeValue());
			
			switch(digitalToken){
			case 1:
				txtViewDigital.setText(Float.toString(multiGauge1.getCurrentGaugeValue()));
				break;
			case 2:
				txtViewDigital.setText(Float.toString(multiGauge2.getCurrentGaugeValue()));
				break;
			case 3:
				txtViewDigital.setText(Float.toString(multiGauge3.getCurrentGaugeValue()));
				break;
			case 4:
				txtViewDigital.setText(Float.toString(multiGauge4.getCurrentGaugeValue()));
				break;
			default:
				txtViewDigital.setText(Float.toString(multiGauge1.getCurrentGaugeValue()));
				break;
			}
    	}
    }
    
    private void parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.
    	
    	try {
    		//Get current tokens for this gauge activity, cast as float.
    		boostSValue = new Float(tokens[BOOST_TOKEN].toString());
    		wbSValue 	= new Float(tokens[WIDEBAND_TOKEN].toString());
    		tempSValue 	= new Float(tokens[TEMP_TOKEN].toString());
    		oilSValue 	= new Float(tokens[OIL_TOKEN].toString());
		} catch (NumberFormatException e) {
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		} catch (ArrayIndexOutOfBoundsException e){
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		}
    }
    
    //Activity transfer
    public void goHome(View v){
    	PassObject.setObject(mSerialService);
    	onBackPressed();
		finish();
	}
    
    //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	multiGauge1.setSensorMaxValue(multiGauge1.getMinValue());
    	multiGauge2.setSensorMaxValue(multiGauge2.getMinValue());
    	multiGauge3.setSensorMaxValue(multiGauge3.getMinValue());
    	multiGauge4.setSensorMaxValue(multiGauge4.getMinValue());
    	Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		//set the gauge/digital to the max value captured so far.
    		switch(digitalToken){
			case 1:
				txtViewDigital.setText(Double.toString(multiGauge1.getSensorMaxValue()));
				break;
			case 2:
				txtViewDigital.setText(Double.toString(multiGauge2.getSensorMaxValue()));
				break;
			case 3:
				txtViewDigital.setText(Double.toString(multiGauge3.getSensorMaxValue()));
				break;
			case 4:
				txtViewDigital.setText(Double.toString(multiGauge4.getSensorMaxValue()));
				break;
			default:
				txtViewDigital.setText(Double.toString(multiGauge1.getSensorMaxValue()));
				break;
			}
    		analogGauge1.setValue((float)multiGauge1.getSensorMaxValue());
    		analogGauge2.setValue((float)multiGauge2.getSensorMaxValue());
    		analogGauge3.setValue((float)multiGauge3.getSensorMaxValue());
    		analogGauge4.setValue((float)multiGauge4.getSensorMaxValue());
        	btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
    	}else{
    		paused = false;
    		btnTwo.setBackgroundResource(Color.TRANSPARENT);
    	}
	}
    
    //Analog gauge one click
    public void gaugeOneClick(View v){
    	digitalToken = BOOST_TOKEN;
    }
    
    public void gaugeTwoClick(View v){
    	digitalToken = WIDEBAND_TOKEN;
    }
    
    public void gaugeThreeClick(View v){
    	digitalToken = TEMP_TOKEN;
    }
    
    public void gaugeFourClick(View v){
    	digitalToken = OIL_TOKEN;
    }
    
    protected void onPause(){
    	super.onPause();
    }
    
    protected void onResume(){
    	super.onResume();
    	analogGauge1.invalidate();
    	analogGauge2.invalidate();
    }
       
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	gaugeOnePref = sp.getString("multiGaugeOne", "boost");
    	gaugeTwoPref = sp.getString("multiGaugeTwo", "wb");
    	
    	if(gaugeOnePref.equals("Boost")){currentTokenOne = BOOST_TOKEN;}else 
    	if(gaugeOnePref.equals("Wideband O2")){currentTokenOne = WIDEBAND_TOKEN;}else 
    	if(gaugeOnePref.equals("Temperature")){currentTokenOne = TEMP_TOKEN;}else 
    	if(gaugeOnePref.equals("Oil Pressure")){currentTokenOne = OIL_TOKEN;}
    	if(gaugeTwoPref.equals("Boost")){currentTokenTwo = BOOST_TOKEN;}else
    	if(gaugeTwoPref.equals("Wideband O2")){currentTokenTwo = WIDEBAND_TOKEN;}else
    	if(gaugeTwoPref.equals("Temperature")){currentTokenTwo = TEMP_TOKEN;}else
    	if(gaugeTwoPref.equals("Oil Pressure")){currentTokenTwo = OIL_TOKEN;}
    }
}
