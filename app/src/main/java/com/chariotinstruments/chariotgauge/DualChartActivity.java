package com.chariotinstruments.chariotgauge;

import java.text.DecimalFormat;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.TimeSeries;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DualChartActivity extends Activity implements Runnable {

    private GraphicalView mChartView;
    private static Thread thread;
    private LineGraphBuilder line = new LineGraphBuilder();
    private XYSeriesRenderer chartOne = new XYSeriesRenderer(); //chart one.
    private XYSeriesRenderer chartTwo = new XYSeriesRenderer();
    private XYSeriesRenderer chartVolts = new XYSeriesRenderer();
    private TimeSeries dataSetOne = new TimeSeries("temp");
    private TimeSeries dataSetTwo = new TimeSeries("temp");
    private TimeSeries dataSetVolts = new TimeSeries("volts");
    
    //Prefs vars
    String gaugeOnePref;
    String gaugeTwoPref;
    
    ImageButton  btnOne;
    ImageButton  btnTwo;
    ImageButton  btnHome;
    ImageButton  btnDisplay;
    String       currentMsg;
    MultiGauges  multiGauge1;
    MultiGauges  multiGauge2;
    MultiGauges  multiGaugeVolts;
    Typeface     typeFaceDigital;
    float        currentSValue;
    boolean      paused;
    String       chartTypeOne;
    String       chartTypeTwo;
    int          i = 0;
    int          currentTokenOne = 1;
    int          currentTokenTwo = 2;
    static DecimalFormat twoDForm;
    
    //Sensor values from the controller
    float   boostSValue;
    float   wbSValue;
    float   tempSValue;
    float   oilSValue;
    float   voltSValue;
    boolean isAbsolute;
    
    //Subtitle labels and data holders
    TextView subTitleLabel1;
    TextView subTitleLabel2;
    TextView subTitleLabel3;
    TextView subTitleLabel4;
    TextView subTitleLabel5;
    TextView subTitleData1;
    TextView subTitleData2;
    TextView subTitleData3;
    TextView subTitleData4;
    TextView subTitleData5;
    
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST        = "toast";
    private static final int VOLT_TOKEN     = 0;
    private static final int BOOST_TOKEN    = 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN     = 3;
    private static final int OIL_TOKEN      = 4;
    
    BluetoothSerialService mSerialService; 
    private static Handler workerHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        //Setup the environment.
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chart_layout);
        
        //Initialize which gauges should be displayed
        prefsInit();
        
        //get which gauge started this chart.
        //Intent chartIntent = getIntent();
        //CURRENT_TOKEN = chartIntent.getIntExtra("chartTypeOne", CURRENT_TOKEN);
        
        //assign the top label buttons
        btnOne     = (ImageButton) findViewById(R.id.btnOne);
        btnTwo     = (ImageButton) findViewById(R.id.btnTwo);
        btnDisplay = (ImageButton) findViewById(R.id.btnDisplay);
        
        //Assign the SubTitle labels and data holders
        subTitleLabel1 = (TextView) findViewById(R.id.subTitleLabel1);
        subTitleLabel2 = (TextView) findViewById(R.id.subTitleLabel2);
        subTitleLabel3 = (TextView) findViewById(R.id.subTitleLabel3);
        subTitleLabel4 = (TextView) findViewById(R.id.subTitleLabel4);
        subTitleLabel5 = (TextView) findViewById(R.id.subTitleLabel5);
        subTitleData1 = (TextView) findViewById(R.id.subTitleData1);
        subTitleData2 = (TextView) findViewById(R.id.subTitleData2);
        subTitleData3 = (TextView) findViewById(R.id.subTitleData3);
        subTitleData4 = (TextView) findViewById(R.id.subTitleData4);
        subTitleData5 = (TextView) findViewById(R.id.subTitleData5);
        
        //Remove unnecessary text views for single chart activity
        ((ViewManager)subTitleLabel4.getParent()).removeView(subTitleLabel4);
        ((ViewManager)subTitleLabel5.getParent()).removeView(subTitleLabel5);
        ((ViewManager)subTitleData4.getParent()).removeView(subTitleData4);
        ((ViewManager)subTitleData5.getParent()).removeView(subTitleData5);
        
        //setup the gauge-calc instances
        multiGauge1      = new MultiGauges(this);
        multiGauge2      = new MultiGauges(this);
        multiGaugeVolts = new MultiGauges(this);
        multiGauge1.buildChart(currentTokenOne);
        multiGauge2.buildChart(currentTokenTwo);
        multiGaugeVolts.buildChart(VOLT_TOKEN);
        
        //Setup font
        typeFaceDigital = Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        subTitleData1.setTypeface(typeFaceDigital);
        subTitleData2.setTypeface(typeFaceDigital);
        subTitleData3.setTypeface(typeFaceDigital);
        subTitleData1.setText("0.00");
        subTitleData2.setText("0.00");
        subTitleData3.setText("0.00");
        
        //Use two decimals when rounding.
        twoDForm = new DecimalFormat("#.##");
        
        //Get the mSerialService object from the UI activity.
        Object obj = PassObject.getObject();
        //Assign it to global mSerialService variable in this activity.
        mSerialService = (BluetoothSerialService) obj;
        
        //Check if the serial service object is null - assign the handler.
        if(mSerialService != null){
            //Update the BluetoothSerialService instance's handler to this activities.
            mSerialService.setHandler(mHandler);
        }
            
        
        thread = new Thread(DualChartActivity.this);
        thread.start();
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
                setDigitalValues();
            }
        }
    };

    @Override
    public void run(){
        Looper.prepare();
        workerHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                
                //local variables
                double pointX = 0.0d;
                double pointYOne = 0.0d;
                double pointYTwo = 0.0d;
                double pointYVolts = 0.0d;
                
                //Parse latest data.
                parseInput((String)msg.obj);
                
                //Calculate display data
                handleSensorData();
                
                pointX = (double)i;
                pointYOne = (double)multiGauge1.getCurrentGaugeValue();
                pointYTwo = (double)multiGauge2.getCurrentGaugeValue();
                pointYVolts = (double)multiGaugeVolts.getCurrentGaugeValue();

                //Put latest data on chart.
                Point p1 = new Point(pointX, pointYOne);
                Point p2 = new Point(pointX, pointYTwo);
                Point pVolts = new Point(pointX, pointYVolts);
                
                //Set the bounds for "real-time"
                if(!paused){
                    line.setXAxisMin(i-30);
                    line.setXAxisMax(i+30);
                }
                
                //Add the points to the graph.
                line.addNewPoints(dataSetOne, p1); 
                line.addNewPoints(dataSetTwo, p2);
                line.addNewPoints(dataSetVolts, pVolts);
                
                if(!paused){
                    try {
                        mChartView.repaint();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                //Increment time value.
                i++;
            }
        };
        Looper.loop();
    }
    
    private void handleSensorData(){
        //Handle gauge one sensor data.
        switch(currentTokenOne){
        case 1:
            multiGauge1.handleSensor(boostSValue);
            break;
        case 2:
            multiGauge1.handleSensor(wbSValue);
            break;
        case 3:
            multiGauge1.handleSensor(tempSValue);
            break;
        case 4:
            multiGauge1.handleSensor(oilSValue);
            break;
        default:
            break;  
        }

        //Handle gauge two sensor data
        switch(currentTokenTwo){
        case 1:
            multiGauge2.handleSensor(boostSValue);
            break;
        case 2:
            multiGauge2.handleSensor(wbSValue);
            break;
        case 3:
            multiGauge2.handleSensor(tempSValue);
            break;
        case 4:
            multiGauge2.handleSensor(oilSValue);
            break;
        default:
            break;  
        }
        
        //Handle voltage sensor data
        multiGaugeVolts.handleSensor(voltSValue);
    }
    
    public void setDigitalValues(){
        subTitleData1.setText(Float.toString(Math.abs(multiGauge1.getCurrentGaugeValue())));
        subTitleData2.setText(Float.toString(Math.abs(multiGauge2.getCurrentGaugeValue())));
        subTitleData3.setText(Float.toString(Math.abs(multiGaugeVolts.getCurrentGaugeValue())));
    }
    
    
    private void parseInput(String sValue){
        String[] tokens=sValue.split(","); //split the input into an array.

        try {
            //Get current tokens for this gauge activity, cast as float.
            voltSValue  = Float.valueOf(tokens[VOLT_TOKEN].toString());
            boostSValue = Float.valueOf(tokens[BOOST_TOKEN].toString());
            wbSValue    = Float.valueOf(tokens[WIDEBAND_TOKEN].toString());
            tempSValue  = Float.valueOf(tokens[TEMP_TOKEN].toString());
            oilSValue   = Float.valueOf(tokens[OIL_TOKEN].toString());
        } catch (NumberFormatException e) {
            boostSValue = 0;
            wbSValue    = 0;
            tempSValue  = 0;
            oilSValue   = 0;
        } catch (ArrayIndexOutOfBoundsException e){
            boostSValue = 0;
            wbSValue    = 0;
            tempSValue  = 0;
            oilSValue   = 0;
        }
    }
    
    protected XYSeriesRenderer buildNewChart(XYSeriesRenderer chartIn, int chartColor){
        chartIn.setColor(chartColor);
        chartIn.setPointStyle(PointStyle.CIRCLE);
        chartIn.setFillPoints(true);
        
        return chartIn;
    }
    
    protected TimeSeries buildNewTimeSeries(TimeSeries dataSetIn, String name){
        dataSetIn.setTitle(name);
        return dataSetIn;
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        //Setup series renderers.
        chartOne = buildNewChart(chartOne, Color.GREEN);
        chartTwo = buildNewChart(chartTwo, Color.WHITE);
        chartVolts = buildNewChart(chartVolts, Color.RED);
        
        //Setup sub title colors
        subTitleLabel1.setTextColor(Color.GREEN);
        subTitleLabel2.setTextColor(Color.WHITE);
        subTitleLabel3.setTextColor(Color.RED);
        subTitleLabel3.setText("Volts:");
        
        
        //Setup datasets.
        //TODO: get prefs for labels
        switch(currentTokenOne){
        case 1:
            dataSetOne = buildNewTimeSeries(dataSetOne, "Boost");
            //line.setYLabel("Pressure (inHG/PSI)");
            subTitleLabel1.setText("Boost:");
            chartTypeOne = "Boost: ";
            break;
        case 2:
            dataSetOne = buildNewTimeSeries(dataSetOne, "WideBand");
            //line.setYLabel("Wideband ");
            subTitleLabel1.setText("Wideband:");
            chartTypeOne = "Wideband: ";
            break;
        case 3:
            dataSetOne = buildNewTimeSeries(dataSetOne, "Temperature");
            //line.setYLabel("Temperature ");
            subTitleLabel1.setText("Temperature:");
            chartTypeOne = "Temp: ";
            break;
        case 4:
            dataSetOne = buildNewTimeSeries(dataSetOne, "Oil");
            //line.setYLabel("Oil Pressure");
            subTitleLabel1.setText("Oil Pressure:");
            chartTypeOne = "Oil: ";
            break;
        default:
            dataSetOne = buildNewTimeSeries(dataSetOne, "Boost");
            //line.setYLabel("Pressure (inHG/PSI)");
            subTitleLabel1.setText("Boost:");
            chartTypeOne = "Boost: ";
            break;
        }
        
        switch(currentTokenTwo){
        case 1:
            dataSetTwo = buildNewTimeSeries(dataSetTwo, "Boost");
            //line.setYLabel("Pressure (inHG/PSI)");
            subTitleLabel2.setText("Boost:");
            chartTypeTwo = "Boost: ";
            break;
        case 2:
            dataSetTwo = buildNewTimeSeries(dataSetTwo, "WideBand");
            //line.setYLabel("Wideband ");
            subTitleLabel2.setText("Wideband:");
            chartTypeTwo = "Wideband: ";
            break;
        case 3:
            dataSetTwo = buildNewTimeSeries(dataSetTwo, "Temperature");
            //line.setYLabel("Temperature ");
            subTitleLabel2.setText("Temperature:");
            chartTypeTwo = "Temp: ";
            break;
        case 4:
            dataSetTwo = buildNewTimeSeries(dataSetTwo, "Oil");
            //line.setYLabel("Oil Pressure");
            subTitleLabel2.setText("Oil Pressure:");
            chartTypeTwo = "Oil: ";
            break;
        default:
            dataSetTwo = buildNewTimeSeries(dataSetTwo, "Boost");
            //line.setYLabel("Pressure (inHG/PSI)");
            subTitleLabel2.setText("Boost:");
            chartTypeTwo = "Boost: ";
            break;
        }
        
        dataSetVolts = buildNewTimeSeries(dataSetVolts, "Volts");
        
        //Setup line-graph view
        //line.setYAxisMin(-35);
        //line.setYAxisMax(0);
        line.addDataSet(dataSetOne);
        line.addDataSet(dataSetTwo);
        line.addDataSet(dataSetVolts);
        line.addSeries(chartOne);
        line.addSeries(chartTwo);
        line.addSeries(chartVolts);
        mChartView = line.getView(this);
        
        //add it to the chart_layout layout
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        layout.addView(mChartView); 
        
        //Setup chart listeners for clicks
        setupChartListeners();
    }
    
    //Chart listeners for when paused and a point is clicked - update the sub title data fields
    private void setupChartListeners(){
        line.getMultiRenderer().setClickEnabled(true);
        line.getMultiRenderer().setSelectableBuffer(15);
        mChartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                if (seriesSelection == null) { 
                    
                }else{
                    String val = Double.toString(round(seriesSelection.getValue()));
                    if(seriesSelection.getSeriesIndex()==0){
                        Toast.makeText(DualChartActivity.this, chartTypeOne+val, Toast.LENGTH_SHORT).show();
                    }
                    if(seriesSelection.getSeriesIndex()==1){
                        Toast.makeText(DualChartActivity.this, chartTypeTwo+val, Toast.LENGTH_SHORT).show();
                    }
                    if(seriesSelection.getSeriesIndex()==2){
                        Toast.makeText(DualChartActivity.this, "Volts: "+val, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
    
    //Kills the looper before going back home
    @Override
    public void onBackPressed(){
        paused = true;
        workerHandler.getLooper().quit();
        thread.interrupt();
        super.onBackPressed();
    }
    
    //chart/gauge display click handling
    public void buttonDisplayClick(View v){
        paused = true;
        workerHandler.getLooper().quit();
        PassObject.setObject(mSerialService);
        
        //Go back to two gauge activity
        Intent gaugeIntent;
        gaugeIntent = new Intent(this, TwoGaugeActivity.class);  
        startActivity(gaugeIntent);
    }
    
    //Button one handling.
    public void buttonOneClick(View v){   
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
        //Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;
            btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
        }else{
            paused = false;
            btnTwo.setBackgroundResource(Color.TRANSPARENT);
        }
    }
    
    //Activity transfer handling
    public void goHome(View v){
        PassObject.setObject(mSerialService);
        onBackPressed();
        finish();
    }
    
    public void onResume(){
        super.onResume();
    }
    
    @Override
    protected void onPause(){
        super.onPause();
        if(this.isFinishing()){
            PassObject.setObject(mSerialService);
        }
    }
    
    public static double round(double unrounded){
        double ret = 0.0;
        try { 
            ret = Double.valueOf(twoDForm.format(unrounded));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    public void prefsInit(){
        SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
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
