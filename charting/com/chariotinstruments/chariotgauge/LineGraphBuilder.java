package com.chariotinstruments.chariotgauge;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;

public class LineGraphBuilder {

    private GraphicalView view;
    
    private TimeSeries dataset = new TimeSeries("Rain Fall"); 
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    
    private XYSeriesRenderer renderer = new XYSeriesRenderer(); // This will be used to customize line 1
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph
    
    public LineGraphBuilder()
    {
        // Add single dataset to multiple dataset
        mDataset.addSeries(dataset);
        
        // Customization time for line 1!
        renderer.setColor(Color.WHITE);
        renderer.setPointStyle(PointStyle.SQUARE);
        renderer.setFillPoints(true);
        
        // Enable Zoom
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setXTitle("Day #");
        mRenderer.setYTitle("CM in Rainfall");
        
        // Add single renderer to multiple renderer
        mRenderer.addSeriesRenderer(renderer);  
    }
    
    public GraphicalView getView(Context context) 
    {
        view =  ChartFactory.getLineChartView(context, mDataset, mRenderer);
        return view;
    }
    
    public void addNewPoints(Point p)
    {
        dataset.add(p.getX(), p.getY());
    }
    
}
