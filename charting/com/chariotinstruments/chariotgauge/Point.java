package com.chariotinstruments.chariotgauge;

public class Point {
    
    private int x;
    private int y;
    
    public Point(int inX, int inY){
        this.x = inX;
        this.y = inY;
    }
    
    public int getX(){
        return this.x;
    }
    
    public int getY(){
        return this.y;
    }

}
