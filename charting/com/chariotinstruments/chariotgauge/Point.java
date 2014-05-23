package com.chariotinstruments.chariotgauge;

public class Point {
    
    private double x;
    private double y;
    
    public Point(double inX, double inY){
        this.x = inX;
        this.y = inY;
    }
    
    public double getX(){
        return this.x;
    }
    
    public double getY(){
        return this.y;
    }

}
