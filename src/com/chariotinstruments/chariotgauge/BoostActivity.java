package com.chariotinstruments.chariotgauge;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.ViewManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BoostActivity extends Activity implements Runnable {
	
	GaugeBuilder analogGauge;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    ImageButton  btnHome;
    Typeface	 typeFaceDigital;
    MultiGauges  multiGauge;
    MultiGauges  multiGaugeVolts;
    Context		 context;
    String	 	 currentMsg;
    TextView 	 txtViewDigital;
    TextView	 txtViewVolts;
    TextView	 txtViewVoltsText;
    float		 currentSValue;
    float 		 voltSValue;
    boolean		 paused;
    
    
    //Prefs vars
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    boolean		 showVoltMeter;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    //Test
    
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 1;
    private static final int VOLT_TOKEN    = 0;
    
    BluetoothSerialService mSerialService; 
    private static Handler workerHandler;
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    context = this;
	    prefsInit(); //Load up the preferences.
	    
	    //Instantiate the gaugeBuilder.
	    analogGauge		= (GaugeBuilder) findViewById(R.id.analogGauge);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital); 
	    txtViewVolts    = (TextView) findViewById(R.id.txtViewVolts);
	    txtViewVoltsText= (TextView) findViewById(R.id.txtViewVoltsText);
	    multiGauge	 	= new MultiGauges(context);
	    multiGaugeVolts = new MultiGauges(context);
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        
        //Set the font of the title text
        txtViewDigital.setTypeface(typeFaceDigital);
        txtViewVolts.setTypeface(typeFaceDigital);
        txtViewVoltsText.setTypeface(typeFaceDigital);
        
        //Setup gauge
        multiGauge.setAnalogGauge(analogGauge);
        multiGauge.buildGauge(CURRENT_TOKEN);
        multiGaugeVolts.buildGauge(VOLT_TOKEN);
        
        //Check if the gauge uses negative numbers or not.
        if(analogGauge.getAbsoluteNumbers()){ 
        	txtViewDigital.setText(Float.toString(Math.abs(multiGauge.getMinValue())));
        }else{
        	txtViewDigital.setText(Float.toString(multiGauge.getMinValue()));
        }
        	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
	    //Assign it to global mSerialService variable in this activity.
	    mSerialService = (BluetoothSerialService) obj;
	    //Update the BluetoothSerialService instance's handler to this activities.
	    mSerialService.setHandler(mHandler);
	    
	    Thread thread = new Thread(BoostActivity.this);
		thread.start();
	   
	    if(!showAnalog){
	    	((ViewManager)analogGauge.getParent()).removeView(analogGauge); //Remove analog gauge
	    }
	    if(!showDigital){
	    	((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge
	    }
	    if(showNightMode){
	    	root = btnOne.getRootView(); //Get root layer view.
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	        //((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital); //Remove digital gauge due to fading for now.
	    }
	    if(!showVoltMeter){
	    	root = btnOne.getRootView(); //Get root layer view.
	    	((ViewManager)txtViewVolts.getParent()).removeView(txtViewVolts);
	    	((ViewManager)txtViewVoltsText.getParent()).removeView(txtViewVoltsText);
	    }

	}
    
    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if(!paused){
	            byte[] readBuf = (byte[]) msg.obj;
	            // construct a string from the valid bytes in the buffer
	            String readMessage;
				try {
					readMessage = new String(readBuf, 0, msg.arg1);
				} catch (NullPointerException e) {
					readMessage = "0";
				}
				//Redraw the needle to the correct value.
	            currentMsg = readMessage;
			
				Message workerMsg = workerHandler.obtainMessage(1, currentMsg);
				workerMsg.sendToTarget();
				updateGauges();
			}
        }
    };
    
    public void run(){
    	Looper.prepare();
    	workerHandler = new Handler(){
    		@Override
    		public void handleMessage(Message msg){
    			parseInput((String)msg.obj);
    			multiGauge.handleSensor(currentSValue);
    			multiGaugeVolts.handleSensor(voltSValue);
    		}
    	};
    	Looper.loop();
    }
    
    public void updateGauges(){
    	if(!paused){
    		analogGauge.setValue(multiGauge.getCurrentGaugeValue());
    		txtViewDigital.setText(Float.toString(Math.abs(multiGauge.getCurrentGaugeValue())));
    		txtViewVolts.setText(Float.toString(Math.abs(multiGaugeVolts.getCurrentGaugeValue())));
    	}
    }
    
    
    private void parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.

    	try {
			currentSValue = Float.valueOf(tokens[CURRENT_TOKEN].toString());//Get current token for this gauge activity, cast as float.
			voltSValue = Float.valueOf(tokens[VOLT_TOKEN].toString());//Get volt token value, cast as float.
		} catch (NumberFormatException e) {
			currentSValue = 0f;
			voltSValue = 0f;
		} catch (ArrayIndexOutOfBoundsException e){
			currentSValue = 0f;
			voltSValue = 0f;
		}
    }
  
    
    //Activity transfer handling
    public void goHome(View v){
    	PassObject.setObject(mSerialService);
    	onBackPressed();
		finish();
	}
    
    @Override
    public void onBackPressed(){
    	paused = true;
    	workerHandler.getLooper().quit();
    	super.onBackPressed();
    }
    
    
    //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	multiGauge.setSensorMaxValue(multiGauge.getMinValue());
    	multiGaugeVolts.setSensorMaxValue(multiGaugeVolts.getMinValue());
    	Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		
    		//set the gauge/digital to the max value captured so far for two seconds.
    		txtViewDigital.setText(Double.toString(Math.abs(multiGauge.getSensorMaxValue())));
    		analogGauge.setValue((float)multiGauge.getSensorMaxValue());
    		txtViewVolts.setText(Double.toString(multiGaugeVolts.getSensorMaxValue()));
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
    	Thread thread = new Thread(BoostActivity.this);
		thread.start();
		analogGauge.invalidate();
    }
    
    
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	showVoltMeter = sp.getBoolean("showVoltMeter", true);
    }

    
}
