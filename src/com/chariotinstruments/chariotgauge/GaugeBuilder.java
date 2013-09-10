package com.chariotinstruments.chariotgauge;

	// Copyright (c) 2010, Freddy Martens (http://atstechlab.wordpress.com), 
	// MindTheRobot (http://mindtherobot.com/blog/)  and contributors
	// All rights reserved.
	//
	// Redistribution and use in source and binary forms, with or without modification, 
	// are permitted provided that the following conditions are met:
	//
//	      * Redistributions of source code must retain the above copyright notice, 
//	        this list of conditions and the following disclaimer.
//	      * Redistributions in binary form must reproduce the above copyright notice, 
//	        this list of conditions and the following disclaimer in the documentation 
//	        and/or other materials provided with the distribution.
//	      * Neither the name of Ondrej Zara nor the names of its contributors may be used 
//	        to endorse or promote products derived from this software without specific 
//	        prior written permission.
	//
	// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
	// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
	// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
	// IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
	// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
	// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
	// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY 
	// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
	// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
	// EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

	import android.content.Context;
	import android.graphics.Bitmap;
	import android.graphics.BitmapFactory;
	import android.graphics.BitmapShader;
	import android.graphics.Canvas;
	import android.graphics.Color;
	import android.graphics.LinearGradient;
	import android.graphics.Matrix;
	import android.graphics.Paint;
	import android.graphics.Path;
	import android.graphics.RadialGradient;
	import android.graphics.RectF;
	import android.graphics.Shader;
	import android.graphics.Typeface;
	import android.os.Bundle;
	import android.os.Parcelable;
	import android.util.AttributeSet;
	import android.util.Log;
	import android.view.View;
import android.content.res.TypedArray;

	public final class GaugeBuilder extends View {

	        private static final String TAG = GaugeBuilder.class.getSimpleName();
	       	        
	        // drawing tools
	        private RectF rimRect;
	        private Paint rimPaint;
	        private Paint rimCirclePaint;
	        
	        private RectF faceRect;
	        private Bitmap faceTexture;
	        private Paint facePaint;
	        private Paint rimShadowPaint;
	        
	        private Paint scalePaint;
	        private RectF scaleRect;
	        
	        private Paint redPaint;

	        private RectF valueRect;
	        private RectF rangeRect;

	        private Paint rangeAllPaint;
	        private Paint valueAllPaint;
	                
	        private Paint unitPaint;        
	        private Path  unitPath;
	        private RectF unitRect;
	        private boolean absoluteNumbers;

	        private Paint lowerTitlePaint;  
	        private Paint upperTitlePaint;  
	        private Path  lowerTitlePath;
	        private Path  upperTitlePath;
	        private RectF titleRect;

	        private Paint handPaint;
	        private Path handPath;

	        private Paint handScrewPaint;
	        
	        private Paint backgroundPaint; 
	        // end drawing tools
	        
	        private Bitmap background; // holds the cached static part
	        
	        // scale configuration
	        // Values passed as property. Defaults are set here.
	        private boolean showGauge                = false;
	        private boolean showRange                = true;
	        
	        private int totalNotches                 = 0; // Total number of notches on the scale. 
	        private int incrementPerLargeNotch       = 10;
	        private int incrementPerSmallNotch       = 2;

	        private int scaleColor                   = Color .rgb(105, 105, 105);
	        private int redColor                     = Color .rgb(225, 18, 18);
	        private int scaleCenterValue             = 15; // the one in the top center (12 o'clock), this corresponds with -90 degrees
	        private int scaleMinValue                = 0;
	        private int scaleMaxValue                = 100;
	        private float degreeMinValue             = 0;
	        private float degreeMaxValue             = 100;

        
	        @SuppressWarnings("unused")
			private String lowerTitle                = "";
	        @SuppressWarnings("unused")
			private String upperTitle                = "";
	        private String unitTitle                 = "";

	        // Fixed values. Use these to position the "layers" of the gauge.
	        private static final float scalePosition = 0.135f;  // The distance from the rim to the scale
	        private static final float valuePosition = 0.285f; // The distance from the rim to the ranges
	        private static final float rangePosition = 0.140f; // The distance from the rim to the ranges
	        private static final float titlePosition = 0.145f; // The Distance from the rim to the titles
	        private static final float unitPosition  = 0.200f; // The distance from the rim to the unit
	        private static final float rimSize       = 0.030f;

	        private  float degreesPerNotch      = 360.0f/totalNotches; 
	        private static final int centerDegrees   = -90; // the one in the top center (12 o'clock), this corresponds with -90 degrees

	        // hand dynamics 
	        private boolean dialInitialized         = false;
	        private float currentValue              = scaleCenterValue;
	        private float targetValue               = scaleCenterValue;
	        private float dialVelocity              = 300f;
	        private float dialAcceleration          = 50f;
	        private long lastDialMoveTime           = -1L;
	        
	        
	        public GaugeBuilder(Context context) {
	                super(context);
	                init(context, null);
	        }

	        public GaugeBuilder(Context context, AttributeSet attrs) {
	                super(context, attrs);
	                init(context, attrs);
	        }

	        public GaugeBuilder(Context context, AttributeSet attrs, int defStyle) {
	                super(context, attrs, defStyle);
	                init(context, attrs);
	        }
	        
	        public void setTotalNotches(int in){
	        	this.totalNotches = in;
	        	setDegreesPerNotch();
	        }
	        
	        public void setIncrementPerLargeNotch(int in){
	        	this.incrementPerLargeNotch = in;
	        }
	        
	        public void setIncrementPerSmallNotch(int in){
	        	this.incrementPerSmallNotch = in;
	        }
	        
	        public void setScaleCenterValue(int in){
	        	this.scaleCenterValue = in;
	        }
	        
	        public void setScaleMinValue(int in){
	        	this.scaleMinValue = in;
	        }
	        
	        public void setScaleMaxValue(int in){
	        	this.scaleMaxValue = in;
	        }
	        
	        public void setUnitTitle(String in){
	        	this.unitTitle = in;
	        }
	        
	        public void setDegreesPerNotch(){
	        	degreesPerNotch = 360.0f/totalNotches;  
	        }
	        
	        public void setAbsoluteNumbers(boolean trueFalse){
	        	absoluteNumbers = trueFalse;
	        }
	        
	        public boolean getAbsoluteNumbers(){
	        	return absoluteNumbers;
	        }
	        
	        
	        @Override
	        protected void onAttachedToWindow() {
	                super.onAttachedToWindow();
	        }

	        @Override
	        protected void onDetachedFromWindow() {
	                super.onDetachedFromWindow();
	        }
	        
	        @Override
	        protected void onRestoreInstanceState(Parcelable state) {
	                Bundle bundle = (Bundle) state;
	                Parcelable superState = bundle.getParcelable("superState");
	                super.onRestoreInstanceState(superState);
	                
	                dialInitialized  = bundle.getBoolean("dialInitialized");
	                currentValue     = bundle.getFloat("currentValue");
	                targetValue      = bundle.getFloat("targetValue");
	                dialVelocity     = bundle.getFloat("dialVelocity");
	                dialAcceleration = bundle.getFloat("dialAcceleration");
	                lastDialMoveTime = bundle.getLong("lastDialMoveTime");
	        }

	        @Override
	        protected Parcelable onSaveInstanceState() {
	                Parcelable superState = super.onSaveInstanceState();
	                
	                Bundle state = new Bundle();
	                state.putParcelable("superState", superState);
	                state.putBoolean("dialInitialized", dialInitialized);
	                state.putFloat("currentValue", currentValue);
	                state.putFloat("targetValue", targetValue);
	                state.putFloat("dialVelocity", dialVelocity);
	                state.putFloat("dialAcceleration", dialAcceleration);
	                state.putLong("lastDialMoveTime", lastDialMoveTime);
	                return state;
	        }

	        void init(Context context, AttributeSet attrs) {
	                // Get the properties from the resource file.
	                if (context != null && attrs != null){
	                        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dial);
	                        showRange              = a.getBoolean(R.styleable.Dial_showRange,          showRange);
	                        showGauge              = a.getBoolean(R.styleable.Dial_showGauge,          showGauge);

	                        totalNotches           = a.getInt(R.styleable.Dial_totalNotches,           totalNotches);
	                        incrementPerLargeNotch = a.getInt(R.styleable.Dial_incrementPerLargeNotch, incrementPerLargeNotch);
	                        incrementPerSmallNotch = a.getInt(R.styleable.Dial_incrementPerSmallNotch, incrementPerSmallNotch);
	                        scaleCenterValue       = a.getInt(R.styleable.Dial_scaleCenterValue,       scaleCenterValue);
	                        scaleColor             = a.getInt(R.styleable.Dial_scaleColor,             scaleColor);
	                        scaleMinValue          = a.getInt(R.styleable.Dial_scaleMinValue,          scaleMinValue);
	                        scaleMaxValue          = a.getInt(R.styleable.Dial_scaleMaxValue,          scaleMaxValue);

	                        String unitTitle       = a.getString(R.styleable.Dial_unitTitle);
	                        String lowerTitle      = a.getString(R.styleable.Dial_lowerTitle);
	                        String upperTitle      = a.getString(R.styleable.Dial_upperTitle);
	                        if (unitTitle != null) this.unitTitle = unitTitle;
	                        if (lowerTitle != null) this.lowerTitle = lowerTitle;
	                        if (upperTitle != null) this.upperTitle = upperTitle;
	                        
	                        a.recycle();
	                }
	                degreeMinValue        = valueToAngle(scaleMinValue)        + centerDegrees;
	                degreeMaxValue        = valueToAngle(scaleMaxValue)        + centerDegrees;
	                
	                initDrawingTools(context);
	        }

	        private void initDrawingTools(Context context) {
	                rimRect = new RectF(0.0f, 0.0f, 1f, 1f);

	                faceRect = new RectF();
	                faceRect.set(rimRect.left  + rimSize, rimRect.top    + rimSize, 
	                                 rimRect.right - rimSize, rimRect.bottom - rimSize);            

	                scaleRect = new RectF();
	                scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
	                                          faceRect.right - scalePosition, faceRect.bottom - scalePosition);

	                rangeRect = new RectF();
	                rangeRect.set(faceRect.left  + rangePosition, faceRect.top    + rangePosition,
	                                      faceRect.right - rangePosition, faceRect.bottom - rangePosition);
	                
	                valueRect = new RectF();
	                valueRect.set(faceRect.left  + valuePosition, faceRect.top    + valuePosition,
	                                      faceRect.right - valuePosition, faceRect.bottom - valuePosition);

	                titleRect = new RectF();
	                titleRect.set(faceRect.left  + titlePosition, faceRect.top    + titlePosition,
	                                          faceRect.right - titlePosition, faceRect.bottom - titlePosition);

	                unitRect = new RectF();
	                unitRect.set(faceRect.left  + unitPosition, faceRect.top    + unitPosition,
	                                         faceRect.right - unitPosition, faceRect.bottom - unitPosition);
	                
	                faceTexture = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.background_soft_white);
	                BitmapShader paperShader = new BitmapShader(faceTexture, 
	                                                                                                    Shader.TileMode.MIRROR, 
	                                                                                                    Shader.TileMode.MIRROR);
	                Matrix paperMatrix = new Matrix();
	                paperMatrix.setScale(1.0f / faceTexture.getWidth(), 
	                                             1.0f / faceTexture.getHeight());

	                paperShader.setLocalMatrix(paperMatrix);

	                rimShadowPaint = new Paint();
	                rimShadowPaint.setShader(new RadialGradient(0.5f, 0.5f, faceRect.width() / 2.0f, 
	                                                 new int[] { 0x00000000, 0x00000500, 0x50000500 },
	                                                 new float[] { 0.96f, 0.96f, 0.99f },
	                                                 Shader.TileMode.MIRROR));
	                rimShadowPaint.setStyle(Paint.Style.FILL);

	                // the linear gradient is a bit skewed for realism
	                rimPaint = new Paint();
	                rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
	                rimPaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f, 
	                                                                                   Color.rgb(200, 200, 200),
	                                                                                   Color.rgb(90, 90, 90),
	                                                                                   Shader.TileMode.CLAMP));             

	                rimCirclePaint = new Paint();
	                rimCirclePaint.setAntiAlias(true);
	                rimCirclePaint.setStyle(Paint.Style.STROKE);
	                rimCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));
	                rimCirclePaint.setStrokeWidth(0.005f);
	                
	                facePaint = new Paint();
	                facePaint.setFilterBitmap(true);
	                facePaint.setStyle(Paint.Style.FILL);
	                facePaint.setShader(paperShader);

	                Typeface scaleTF = Typeface.createFromAsset(context.getAssets(), "fonts/Mechanical.ttf");
	                scalePaint = new Paint();
	                scalePaint.setStyle(Paint.Style.STROKE);
	                scalePaint.setColor(scaleColor);
	                //scalePaint.setStrokeWidth(0.006f);
	                scalePaint.setAntiAlias(true);
	                scalePaint.setTextSize(0.078f);
	                scalePaint.setTypeface(Typeface.create(scaleTF, Typeface.NORMAL));
	                scalePaint.setTextScaleX(0.6f);
	                scalePaint.setTextAlign(Paint.Align.CENTER); 
	                scalePaint.setLinearText(true);
	                
	                //For red values.
	                redPaint = new Paint();
	                redPaint.setStyle(Paint.Style.STROKE);
	                redPaint.setColor(redColor);
	                redPaint.setAntiAlias(true);
	                redPaint.setTextSize(0.078f);
	                redPaint.setTypeface(Typeface.create(scaleTF, Typeface.NORMAL));
	                redPaint.setTextScaleX(0.6f);
	                redPaint.setTextAlign(Paint.Align.CENTER); 
	                redPaint.setLinearText(true);

	                
	                rangeAllPaint = new Paint();
	                rangeAllPaint.setStyle(Paint.Style.STROKE);
	                rangeAllPaint.setColor(0xcff8f8f8);
	                rangeAllPaint.setStrokeWidth(0.012f);
	                rangeAllPaint.setAntiAlias(true);
	                rangeAllPaint.setShadowLayer(0.005f, -0.002f, -0.002f, 0x7f000000);
	                


	                //This is the inner "warning" gauge.
	                valueAllPaint = new Paint();
	                valueAllPaint.setStyle(Paint.Style.STROKE);
	                valueAllPaint.setColor(0xcff8f8f8);
	                valueAllPaint.setStrokeWidth(0.20f);
	                valueAllPaint.setAntiAlias(true);
	                valueAllPaint.setShadowLayer(0.005f, -0.002f, -0.002f, 0x7f000000);

	                //This is the gauge type (IE TPS, Boost, etc..)
	                Typeface unitTF = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Bold.ttf");
	                unitPaint = new Paint();
	                unitPaint.setColor(scaleColor);
	                unitPaint.setAntiAlias(true);
	                unitPaint.setTypeface(Typeface.create(unitTF, Typeface.NORMAL));
	                unitPaint.setTextAlign(Paint.Align.CENTER);
	                unitPaint.setTextSize(0.07f);
	                unitPaint.setTextScaleX(0.9f);

	                upperTitlePaint = new Paint();
	                upperTitlePaint.setColor(0xaf0c0c0c);
	                upperTitlePaint.setAntiAlias(true);
	                upperTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
	                upperTitlePaint.setTextAlign(Paint.Align.CENTER);
	                upperTitlePaint.setTextSize(0.04f);
	                upperTitlePaint.setTextScaleX(0.8f);

	                lowerTitlePaint = new Paint();
	                lowerTitlePaint.setColor(0xaf0c0c0c);
	                lowerTitlePaint.setAntiAlias(true);
	                lowerTitlePaint.setTypeface(Typeface.DEFAULT_BOLD);
	                lowerTitlePaint.setTextAlign(Paint.Align.CENTER);
	                lowerTitlePaint.setTextSize(0.04f);
	                lowerTitlePaint.setTextScaleX(0.8f);

	                //The needle
	                handPaint = new Paint();
	                handPaint.setAntiAlias(true);
	                handPaint.setColor(Color .rgb(254, 90, 30));         
	                handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
	                handPaint.setStyle(Paint.Style.FILL);   

	                //Where the needle would connect to the gauge.
	                handScrewPaint = new Paint();
	                handScrewPaint.setAntiAlias(true);
	                //handScrewPaint.setColor(0xff493f3c);
	                handScrewPaint.setColor(Color .rgb(0, 0, 0));
	                handScrewPaint.setStyle(Paint.Style.FILL);
	                
	                backgroundPaint = new Paint();
	                backgroundPaint.setFilterBitmap(true);

	                unitPath = new Path();
	                unitPath.addArc(unitRect, -180.0f, -180.0f);

	                upperTitlePath = new Path();
	                upperTitlePath.addArc(titleRect, 180.0f, 180.0f);

	                lowerTitlePath = new Path();
	                lowerTitlePath.addArc(titleRect, -180.0f, -180.0f);

	                // The hand is drawn with the tip facing up. That means when the image is not rotated, the tip 
	                // faces north. When the the image is rotated -90 degrees, the tip is facing west and so on.
	                handPath = new Path();                                      //   X      Y
	                handPath.moveTo(0.5f, 0.50f);                               // 0.500, 0.700 x:  , y: adjusts tail
	                handPath.lineTo(0.5f - 0.040f, 0.5f - .090f);               // 0.490, 0.630 x: adjusts base width, y: adjusts tail length
	                handPath.lineTo(0.5f - 0.005f, 0.5f - 0.37f);               // 0.498, 0.100 x: adjusts tip width, y: adjusts needle length
	                handPath.lineTo(0.5f + 0.005f, 0.5f - 0.37f);               // 0.502, 0.100 x: adjusts tip width, y: adjusts needle length
	                handPath.lineTo(0.5f + 0.040f, 0.5f - .090f);               // 0.510, 0.630 x: adjusts base width, y: adjusts tail length
	                handPath.lineTo(0.5f, 0.50f);                               // 0.500, 0.700 x:  , y: adjusts tail
	                handPath.addCircle(0.5f, 0.5f, 0.060f, Path.Direction.CW);
	                
	        }
	        
	        @Override
	        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	                Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
	                Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));
	                
	                int widthMode = MeasureSpec.getMode(widthMeasureSpec);
	                int widthSize = MeasureSpec.getSize(widthMeasureSpec);
	                
	                int heightMode = MeasureSpec.getMode(heightMeasureSpec);
	                int heightSize = MeasureSpec.getSize(heightMeasureSpec);
	                
	                int chosenWidth = chooseDimension(widthMode, widthSize);
	                int chosenHeight = chooseDimension(heightMode, heightSize);
	                
	                int chosenDimension = Math.min(chosenWidth, chosenHeight);
	                
	                setMeasuredDimension(chosenDimension, chosenDimension);
	        }
	        
	        private int chooseDimension(int mode, int size) {
	                if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
	                        return size;
	                } else { // (mode == MeasureSpec.UNSPECIFIED)
	                        return getPreferredSize();
	                } 
	        }
	        
	        // in case there is no size specified
	        private int getPreferredSize() {
	                return 250;
	        }

	        private void drawRim(Canvas canvas) {
	                // first, draw the metallic body
	                canvas.drawOval(rimRect, rimPaint);
	                // now the outer rim circle
	                canvas.drawOval(rimRect, rimCirclePaint);
	        }
	        
	        private void drawFace(Canvas canvas) {          
	                canvas.drawOval(faceRect, facePaint);
	                // draw the inner rim circle
	                canvas.drawOval(faceRect, rimCirclePaint);
	                // draw the rim shadow inside the face
	                canvas.drawOval(faceRect, rimShadowPaint);
	        }


	        private void drawBackground(Canvas canvas) {
	                if (background == null) {
	                        Log.w(TAG, "Background not created");
	                } else {
	                        canvas.drawBitmap(background, 0, 0, backgroundPaint);
	                }
	        }

	        private void drawScale(Canvas canvas) {
	                // Draw the circle
	                canvas.drawOval(scaleRect, scalePaint);

	                canvas.save(Canvas.MATRIX_SAVE_FLAG);
	                for (int i = 0; i < totalNotches; ++i) {
	                        float y1 = scaleRect.top;
	                        float y2 = y1 - 0.015f;
	                        float y3 = y1 - 0.025f;
	                        
	                        int value = notchToValue(i);

	                        if (i % (incrementPerLargeNotch/incrementPerSmallNotch) == 0) {
	                                if (value >= scaleMinValue && value <= scaleMaxValue) {
	                                        // draw a nick
	                                        canvas.drawLine(0.5f, y1, 0.5f, y3, scalePaint);
	                                        String valueString;
	                                        if(absoluteNumbers){
	                                        	valueString = Integer.toString(Math.abs(value));
	                                        }else{
	                                        	valueString = Integer.toString(value);
	                                        }
	                                        // Draw the text 0.15 away from y3 which is the long nick.
	                                        if(value == scaleCenterValue){
	                                        	canvas.drawText(valueString, 0.5f, y3 - 0.015f, redPaint);//scalePaint);
	                                        }else{
	                                        	canvas.drawText(valueString, 0.5f, y3 - 0.015f, scalePaint);
	                                        }
	                                }
	                        }
	                        else{
	                                if (value >= scaleMinValue && value <= scaleMaxValue) {
	                                        // draw a nick
	                                        canvas.drawLine(0.5f, y1, 0.5f, y2, scalePaint);
	                                }
	                        }
	                        canvas.rotate(degreesPerNotch, 0.5f, 0.5f);
	                }
	                canvas.restore();
	        }
	        
	        private void drawTitle(Canvas canvas) {
	                // Use a vertical offset when printing the upper title. The upper and lower title
	                // use the same rectangular but the spacing  between the title and the ranges
	                // is not equal for the upper and lower title and therefore, the upper title is 
	                // moved downwards.
	                //canvas.drawTextOnPath(upperTitle, upperTitlePath, 0.0f, 0.02f, upperTitlePaint);                                
	                //canvas.drawTextOnPath(lowerTitle, lowerTitlePath, 0.0f, 0.0f,  lowerTitlePaint);                                
	                canvas.drawTextOnPath(unitTitle,  unitPath,       0.0f, 0.0f,  unitPaint);

	        }
	        
	        private void drawHand(Canvas canvas) {
	                if (dialInitialized) {
	                        float angle = valueToAngle(currentValue);
	                        canvas.save(Canvas.MATRIX_SAVE_FLAG);
	                        canvas.rotate(angle, 0.5f, 0.5f);
	                        canvas.drawPath(handPath, handPaint);
	                        canvas.restore();
	                        
	                        canvas.drawCircle(0.5f, 0.5f, 0.02f, handScrewPaint);
	                }
	        }
	        

	        private void drawBezel(Canvas canvas) {
	                // Draw the bevel in which the value is draw.
	                canvas.save(Canvas.MATRIX_SAVE_FLAG);
	                canvas.drawArc(valueRect, degreeMinValue, degreeMaxValue - degreeMinValue, false, valueAllPaint);
	                canvas.restore();               
	        }

	        /* Translate a notch to a value for the scale.
	         * The notches are evenly spread across the scale, half of the notches on the left hand side
	         * and the other half on the right hand side.
	         * The raw value calculation uses a constant so that each notch represents a value n + 2.
	         */
	        private int notchToValue(int notch) {
	                int rawValue = ((notch < totalNotches / 2) ? notch : (notch - totalNotches)) * incrementPerSmallNotch;
	                int shiftedValue = rawValue + scaleCenterValue;
	                return shiftedValue;
	        }
	        
	        private float valueToAngle(float value) {
	                // scaleCenterValue represents an angle of -90 degrees.
	        		// Divide by the small increment instead of hard coded 2.
	                return (value - scaleCenterValue) / incrementPerSmallNotch * degreesPerNotch;
	        }
	        
	        @Override
	        protected void onDraw(Canvas canvas) {
	                drawBackground(canvas);

	                float scale = (float) getWidth();               
	                canvas.save(Canvas.MATRIX_SAVE_FLAG);
	                canvas.scale(scale, scale);

	                // Draw the needle using the updated value
	                drawHand(canvas);
	                canvas.restore();
	        
	                // Calculate a new current value.
	                calculateCurrentValue();
	        }

	        @Override
	        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	                Log.d(TAG, "Size changed to " + w + "x" + h);
	                regenerateBackground();
	        }
	        
	        public void regenerateBackground() {
	                // free the old bitmap
	                if (background != null) {
	                        background.recycle();
	                }
	                
	                background = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
	                Canvas backgroundCanvas = new Canvas(background);
	                float scale = (float) getWidth();               
	                backgroundCanvas.scale(scale, scale);
	                	//Log.d(TAG, "TESTING: " + MeasureSpec.toString((float)backgroundCanvas.getWidth()));
	                drawRim(backgroundCanvas);
	                drawFace(backgroundCanvas);
	                drawScale(backgroundCanvas);

	                if (showGauge){
	                        drawBezel(backgroundCanvas);
	                }
	                drawTitle(backgroundCanvas);            
	        }

	        // Move the hand slowly to the new position.
	        private void calculateCurrentValue() {
	                if (!(Math.abs(currentValue - targetValue) > 0.01f)) {
	                        return;
	                }
	                
	                if (lastDialMoveTime != -1L) {
	                        long currentTime = System.currentTimeMillis();
	                        float delta = (currentTime - lastDialMoveTime) / 1000.0f;

	                        float direction = Math.signum(dialVelocity);
//	                        if (Math.abs(dialVelocity) < 90.0f) {
//	                                dialAcceleration = 1.0f * (targetValue - currentValue);
//	                        } else {
//	                                dialAcceleration = 0.0f;
//	                        }
	                        dialVelocity = 700.0f;
	                        dialAcceleration = 500.0f;
	                        currentValue += dialVelocity * delta;
	                        dialVelocity += dialAcceleration * delta;
	                        if ((targetValue - currentValue) * direction < 0.01f * direction) {
	                                currentValue = targetValue;
	                                //dialVelocity = 0.0f;
	                                //dialAcceleration = 0.0f;
	                                lastDialMoveTime = -1L;
	                        } else {
	                                lastDialMoveTime = System.currentTimeMillis();                          
	                        }
	                        invalidate();
	                } else {
	                        lastDialMoveTime = System.currentTimeMillis();
	                        calculateCurrentValue();
	                }
	        }
	        
	        public void setValue(float value) {
	                if      (value < scaleMinValue) value = scaleMinValue;
	                else if (value > scaleMaxValue) value = scaleMaxValue;

	                targetValue = value;
	                dialInitialized = true;
	                
	                invalidate(); // forces onDraw() to be called.
	        }

	        public float getValue() {
	                return targetValue;
	        }

	}

