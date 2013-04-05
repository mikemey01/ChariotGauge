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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class WidebandActivity extends Activity implements Runnable {
	
	GaugeBuilder analogGauge;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
	
    TextView 	 txtViewDigital;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    boolean		 paused;
    MultiGauges  multiGauge;
    Context		 context;
    String	 	 currentMsg;
    
    //Prefs vars
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
    private static final int CURRENT_TOKEN = 2;
        
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    context = this;
	    prefsInit(); //Load up the preferences.
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge 	= (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
	    multiGauge		= new MultiGauges(context);
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);
        
        //Setup gauge
        multiGauge.setAnalogGauge(analogGauge);
        multiGauge.buildGauge(CURRENT_TOKEN);

	  
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
                    currentMsg = readMessage;
					if(!paused){
						Thread thread = new Thread(WidebandActivity.this);
						thread.start();
						updateGauges();
					}
                    
            		break;
            	case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    
    public void run(){
    	multiGauge.handleSensor(parseInput(currentMsg));
    }
    
    public void updateGauges(){
    	if(!paused){
    		analogGauge.setValue(multiGauge.getCurrentGaugeValue());
    		txtViewDigital.setText(Float.toString(multiGauge.getCurrentGaugeValue()));
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
	}
    
    //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	multiGauge.setSensorMaxValue(multiGauge.getMinValue());
    	Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		//set the gauge/digital to the max value captured so far for two seconds.
    		txtViewDigital.setText(Double.toString(multiGauge.getSensorMaxValue()));
    		analogGauge.setValue((float)multiGauge.getSensorMaxValue());
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
       
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    }
}
