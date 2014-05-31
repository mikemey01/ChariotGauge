package com.chariotinstruments.chariotgauge;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.content.Context;

public class LineGraphBuilder {

    private GraphicalView view;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph
    
    public LineGraphBuilder()
    {
        // Add single dataset to multiple dataset
        //mDataset.addSeries(dataset);
        
        // Setup multiRenderer environment.
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setXTitle("Time");
        mRenderer.setYTitle("Sensor Measurement");
        mRenderer.setAxisTitleTextSize(20.0f);
        mRenderer.setChartTitleTextSize(20.0f);
        mRenderer.setDisplayValues(true);
        mRenderer.setPointSize(1.5f);
        mRenderer.setLabelsTextSize(20.0f);
        mRenderer.setLegendTextSize(20.0f); 
        mRenderer.setShowGrid(true);  
    }
    
    public void addSeries(XYSeriesRenderer xYPlot){
        this.mRenderer.addSeriesRenderer(xYPlot);
    }
    
    public void setXAxisMin(int xMin){
        this.mRenderer.setXAxisMin(xMin);
    }
    
    public void setXAxisMax(int xMax){
        this.mRenderer.setXAxisMax(xMax);
    }
    
    public void setYAxisMin(int yMin){
        this.mRenderer.setYAxisMin(yMin);
    }
    
    public void setYAxisMax(int yMax){
        this.mRenderer.setYAxisMax(yMax);
    }
    
    public void setYLabel(String labelIn){
        this.mRenderer.setYTitle(labelIn);
    }
    
    public void addNewPoints(TimeSeries dataSet, Point p)
    {
        dataSet.add(p.getX(), p.getY());
    }
    
    public void addDataSet(TimeSeries dataSet){
        mDataset.addSeries(dataSet);
    }
    
    public GraphicalView getView(Context context){
        view =  ChartFactory.getLineChartView(context, mDataset, mRenderer);
        return view;
    }
    
    public XYMultipleSeriesDataset getMultiDataSet(){
        return this.mDataset;
    }
    
    public XYMultipleSeriesRenderer getMultiRenderer(){
        return this.mRenderer;
    }
    
}
