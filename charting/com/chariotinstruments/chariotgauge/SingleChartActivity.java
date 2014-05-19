package com.chariotinstruments.chariotgauge;

import java.util.Random;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYSeriesRenderer;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;

public class SingleChartActivity extends Activity {

    private static GraphicalView view;
    private LineGraphBuilder line = new LineGraphBuilder();
    private static Thread thread;
    private XYSeriesRenderer xYPlot = new XYSeriesRenderer(); //This is the XYPlot itself.

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                        view.repaint();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }
    
    protected XYSeriesRenderer setupXYPlots(){
        this.xYPlot.setColor(Color.GREEN);
        this.xYPlot.setPointStyle(PointStyle.CIRCLE);
        this.xYPlot.setFillPoints(true);
        
        return this.xYPlot;
    }

    @Override
    protected void onStart() {
        super.onStart();
        line.setYAxisMin(-100);
        line.setYAxisMax(100);
        line.addSeries(setupXYPlots());
        view = line.getView(this);
        setContentView(view); 
    }
    
    protected int generateRandomData()
    {
        Random random = new Random();
        return random.nextInt(40);
    }

}