package com.chariotinstruments.chariotgauge;

import java.util.Random;
import org.achartengine.GraphicalView;


import android.app.Activity;
import android.os.Bundle;

public class SingleChartActivity extends Activity {

    private static GraphicalView view;
    private LineGraphBuilder line = new LineGraphBuilder();
    private static Thread thread;

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
                        // TODO Auto-generated catch block
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
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        view = line.getView(this);
        line.setYAxisMin(-100);
        line.setYAxisMax(100);
        setContentView(view);
    }
    
    protected int generateRandomData()
    {
        Random random = new Random();
        return random.nextInt(40);
    }

}