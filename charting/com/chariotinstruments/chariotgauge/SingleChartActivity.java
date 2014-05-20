package com.chariotinstruments.chariotgauge;

import java.util.Random;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

public class SingleChartActivity extends Activity {

    private static GraphicalView mChartView;
    private LineGraphBuilder line = new LineGraphBuilder();
    private static Thread thread;
    private XYSeriesRenderer xYPlot = new XYSeriesRenderer(); //This is the XYPlot itself.

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chart_layout);

        thread = new Thread() {
            public void run()
            {
                for (int i = 0; i < 100; i++) 
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
    
    protected int generateRandomData()
    {
        Random random = new Random();
        return random.nextInt(40);
    }

}