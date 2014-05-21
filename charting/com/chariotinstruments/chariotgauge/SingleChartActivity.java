package com.chariotinstruments.chariotgauge;

import java.util.Random;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SingleChartActivity extends Activity {

    private static GraphicalView mChartView;
    private LineGraphBuilder line = new LineGraphBuilder();
    private static Thread thread;
    private XYSeriesRenderer xYPlot = new XYSeriesRenderer(); //This is the XYPlot itself.
    
    ImageButton  btnOne;
    ImageButton  btnTwo;
    ImageButton  btnHome;
    float        currentSValue;
    float        voltSValue;
    boolean      paused;
    
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST       = "toast";
    private static final int CURRENT_TOKEN = 1;
    private static final int VOLT_TOKEN    = 0;
    
    BluetoothSerialService mSerialService; 
    private static Handler workerHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chart_layout);
        
        btnOne = (ImageButton) findViewById(R.id.btnOne);
        btnTwo = (ImageButton) findViewById(R.id.btnTwo);
        
        //Get the mSerialService object from the UI activity.
        Object obj = PassObject.getObject();
        //Assign it to global mSerialService variable in this activity.
        mSerialService = (BluetoothSerialService) obj;
        //Update the BluetoothSerialService instance's handler to this activities.
        mSerialService.setHandler(mHandler);

        thread = new Thread() {
            public void run()
            {
                for (int i = 0; i < 1000; i++) 
                {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Point p = new Point(i, generateRandomData()); //MockData.getDataFromReceiver(i); // We got new data!
                    if(i > 30){
                        line.setXAxisMin(i-30);
                    }
                    line.setXAxisMax(i+30);
                    line.addNewPoints(p); // Add it to our graph
                    try {
                        mChartView.repaint();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }
    
    protected XYSeriesRenderer setupXYPlot(){
        this.xYPlot.setColor(Color.GREEN);
        this.xYPlot.setPointStyle(PointStyle.CIRCLE);
        this.xYPlot.setFillPoints(true);
        
        return this.xYPlot;
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        //Setup line-graph view
        line.setYAxisMin(-100);
        line.setYAxisMax(100);
        line.addSeries(setupXYPlot());
        mChartView = line.getView(this);
        
        //add it to the chart_layout layout
        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        layout.addView(mChartView); 
    }
    
    //Kills the looper before going back home
    @Override
    public void onBackPressed(){
        paused = true;
        workerHandler.getLooper().quit();
        super.onBackPressed();
    }
    
    //Button one handling.
    public void buttonOneClick(View v){   
        //TODO: reset max value.
        paused = false;
        btnTwo.setBackgroundResource(Color.TRANSPARENT);
        Toast.makeText(getApplicationContext(), "Max value reset.", Toast.LENGTH_SHORT).show();
    }

    //Button two handling.
    public void buttonTwoClick(View v){
        if(!paused){
            paused = true;

            //TODO: set graph to max value
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
    
    protected int generateRandomData()
    {
        Random random = new Random();
        return random.nextInt(40);
    }

}