package com.chariotinstruments.chariotgauge;

import java.text.DecimalFormat;
import java.util.Locale;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MultiGauges extends View{
	
	//Generic Gauge parameters
	private GaugeBuilder analogGauge;
	private int minValue;
	private int maxValue;
	private double sensorMinValue;
	private double sensorMaxValue;
	private Context context;
	private int currentToken;
	TextView txtViewDigital;
	float currentGaugeValue;
	
	//Preference vars
	String		 pressureUnits; //Boost units.
	String		 unitType; //AFR or Lambda.
    String		 fuelType; //Fuel type in use.
    float		 wbLowVolts; //Low volts from the target table.
    float		 wbHighVolts; //High volts from the target table.
    float		 wbLowAFR; //Low AFR from the target table.
    float		 wbHighAFR; //High AFR from the target table.
    String 		 tempUnits;
    double		 tempOne;
    double		 tempTwo;
    double		 tempThree;
    double		 tempOhmsOne;
    double		 tempOhmsTwo;
    double		 tempOhmsThree;
    double		 tempBiasResistor;
    double 		 oilLowPSI;
    double		 oilLowOhms;
    double		 oilHighPSI;
    double		 oilHighOhms;
    double		 oilBiasResistor;
    
	
	//Boost gauge parameters
	public static final double KPA_TO_PSI  = 0.14503773773020923;
    public static final double ATMOSPHERIC = 101.325;
    public static final double KPA_TO_INHG = 0.295299830714;
    
    //Wideband gauge parameters
    double		 wbAFRRange;
    double		 wbVoltRange;
    double		 wbStoich;
    
    //Oil gauge parameters
    double		 oilLowVolts;
    double		 oilHighVolts;
    double		 oilRangeVolts;
    double		 oilRangePSI;
    
    //Temp gauge parameters
    double a;
    double b;
    double c;
	
	public MultiGauges(Context context){
		super(context);
		this.context = context;
	}
	
	public void setAnalogGauge(GaugeBuilder analogGaugeIn){
		analogGauge = analogGaugeIn;
	}
	
	public GaugeBuilder getAnalogGauge(){
		return analogGauge;
	}
	
	public double getSensorMinValue(){
		return sensorMinValue;
	}
	
	public int getMinValue(){
		return minValue;
	}
	
	public double getSensorMaxValue(){
		return sensorMaxValue;
	}
	
	public void setSensorMaxValue(int minValueIn){
		sensorMaxValue = minValueIn;
	}
	
	public float getCurrentGaugeValue(){
		return currentGaugeValue;
	}
	
	//pass the raw sensor data to the appropriate handler
	public void handleSensor(float sValue){ 
		switch(currentToken){
		case 1:
			handleBoostSensor(sValue);
			break;
		case 2:
			handleWidebandSensor(sValue);
			break;
		case 3:
			handleTempSensor(sValue);
			break;
		case 4: 
			handleOilSensor(sValue);
			break;
		default:
			currentGaugeValue = 1;
			break;
		}
	}
	
	public void handleBoostSensor(float sValue){
    	double vOut;
    	double kpa=0;
    	double psi=0;
    	   	
		vOut = (sValue*5.00)/1024;
		kpa = ((vOut/5.00)+.04)/.004;
		kpa = kpa - ATMOSPHERIC;
		psi = kpa * KPA_TO_PSI;
		if(pressureUnits.equals("KPA")){ //Gauge displayed in KPA
			currentGaugeValue = (float)round(kpa+ATMOSPHERIC);
			
			if(round(kpa) > sensorMaxValue && round(kpa) <= maxValue){
        		sensorMaxValue = round(kpa);
        	}
		}else{ //Gauge displayed in PSI
			if(psi < minValue){ //set the lower bounds on the data.
				currentGaugeValue = (float)round(minValue);
			}else if (psi > maxValue){ //Set the upper bounds on the data
				currentGaugeValue = (float)round(maxValue);
			}else{ //if it is in-between the lower and upper bounds as it should be, display it.
				currentGaugeValue = (float)round(psi);

				if(round(psi) > sensorMaxValue && round(psi) <= maxValue){
					sensorMaxValue = round(psi);
				}
			}
		} //PSI closing paren.
    } //handleSensor closing paren.
	
	public void handleWidebandSensor(float sValue){
		double vOut;
    	double vPercentage;
    	double o2=0;

		vOut = (sValue*wbVoltRange)/1024;
		vPercentage = vOut / wbVoltRange;
		o2 = wbLowAFR + (wbAFRRange * vPercentage);
		
		if(unitType.equals("Lambda")){ //If unit type is set to lambda, convert afr to lambda.
			o2 = o2/wbStoich;
		}
		
		currentGaugeValue = (float)round(o2);
		
		if(round(o2) > sensorMaxValue && round(o2) <= maxValue){
    		sensorMaxValue = round(o2);
		}
	}
	
	public void handleTempSensor(float sValue){
		double res;
    	double temp;
    	
		res = getResistance(sValue);
    	temp = getTemperature(res);
		if(tempUnits.toLowerCase().equals("celsius")){ //Celsius
			if(temp < minValue){ //set the lower bounds on the data.
				currentGaugeValue = (float)round(minValue);
			}else if (temp > maxValue){ //set the upper bounds on the data.
				currentGaugeValue = (float)round(maxValue);
			}else{ //if it is in-between the lower and upper bounds as it should be, display it.
    			analogGauge.setValue((float)round(temp));
    		
    			if(round(temp) > sensorMaxValue && round(temp) <= maxValue){
    				sensorMaxValue = round(temp);
    			}
			}
		}else{ //Fahrenheit
			if(getF(temp) < minValue){
				currentGaugeValue = (float)round(minValue);
			}else if(getF(temp) > maxValue){
				currentGaugeValue = (float)round(maxValue);
			}else{
				currentGaugeValue = (float)round(getF(temp));
    		
    			if(round(getF(temp)) > sensorMaxValue && round(getF(temp)) <= maxValue){
    				sensorMaxValue = round(getF(temp));
    			}
			}
		}
	}
	
	public void handleOilSensor(float sValue){
		double oil = 0;
    	double vOut = 0;
    	double vPercentage;
    	   	
		vOut = (sValue*5.00)/1024; //get voltage
		
		vOut = vOut - oilLowVolts; //get on the same level as the oil pressure sensor
		if(vOut == 0){ //Remove divide by 0 errors.
			vOut = .01;
		}
		vPercentage = vOut / oilRangeVolts; //find the percentage of the range we're at
		oil = vPercentage * oilRangePSI; //apply same percentage to range of oil.
		
		if(oil < minValue){ //set the lower bounds on the data.
			currentGaugeValue = (float)round(minValue);
		}else if (oil > maxValue){ //set the upper bounds on the data.
			currentGaugeValue = (float)round(maxValue);
		}else{ //if it is in-between the lower and upper bounds as it should be, display it.
			currentGaugeValue = (float)round(oil);
			
			if(round(oil) > sensorMaxValue && round(oil) <= maxValue){ //Check to see if we've hit a new high, record it.
        		sensorMaxValue = round(oil);
        	}
		}
	}
	
	public void buildGauge(int gaugeType){
		switch(gaugeType){
			case 1: //Boost
				currentToken=1; //set the value to the boost token.
				prefsBoostInit(); //get stored prefs for boost.
				if(pressureUnits.equals("KPA")){
		        	//Set up the gauge values and the values that are handled from the sensor for KPA
		        	minValue = 0;
				    maxValue = 250;
				    sensorMinValue = minValue;
				    sensorMaxValue = minValue;
				    
				    //Set up the Boost GaugeBuilder for KPA
				    analogGauge.setTotalNotches(65);
				    analogGauge.setIncrementPerLargeNotch(25);
				    analogGauge.setIncrementPerSmallNotch(5);
				    analogGauge.setScaleCenterValue(150);
				    analogGauge.setScaleMinValue(minValue);
				    analogGauge.setScaleMaxValue(maxValue);
				    analogGauge.setUnitTitle("Boost (KPA)");
				    analogGauge.setValue((float)minValue);
		        }else{
		        	//Set up the gauge values and the values that are handled from the sensor for PSI
		        	minValue = -10;
				    maxValue = 25;
				    sensorMinValue = minValue;
				    sensorMaxValue = minValue;
				    
				    //Set up the Boost GaugeBuilder for PSI
				    analogGauge.setTotalNotches(55);
				    analogGauge.setIncrementPerLargeNotch(5);
				    analogGauge.setIncrementPerSmallNotch(1);
				    analogGauge.setScaleCenterValue(10);
				    analogGauge.setScaleMinValue(minValue);
				    analogGauge.setScaleMaxValue(maxValue);
				    analogGauge.setUnitTitle("Boost (PSI)");
				    analogGauge.setValue((float)minValue);
		        }
				break;
			case 2: //Wideband
				currentToken=2;
				prefsWidebandInit();
				initStoich(fuelType);
				//High and low range for AFR/Volts
			    wbAFRRange = (double)(wbHighAFR - wbLowAFR);
			    wbVoltRange = (double)(wbHighVolts - wbLowVolts);
				if(unitType.equals("Lambda")){
		    		//Set up the gauge values and the values that are handled from the sensor.
				    minValue = 0;
				    maxValue = 2;
				    sensorMinValue = minValue;
				    sensorMaxValue = minValue;
				    //Set up the Boost GaugeBuilder
				    analogGauge.setTotalNotches(7);
				    analogGauge.setIncrementPerLargeNotch(1);
				    analogGauge.setIncrementPerSmallNotch(1);
				    analogGauge.setScaleCenterValue(1);
				    analogGauge.setScaleMinValue(minValue);
				    analogGauge.setScaleMaxValue(maxValue);
				    analogGauge.setUnitTitle(fuelType + " Wideband Lambda");
				    analogGauge.setValue(minValue);
		    	}else{
		    		if(fuelType.equals("Gasoline") || fuelType.equals("Propane") || fuelType.equals("Diesel")){
		    			//Set up the gauge values and the values that are handled from the sensor.
		    		    minValue = 5;
		    		    maxValue = 25;
		    		    sensorMinValue = minValue;
		    		    sensorMaxValue = minValue;
		    		    //Set up the Boost GaugeBuilder
		    		    analogGauge.setTotalNotches(40);
		    		    analogGauge.setIncrementPerLargeNotch(5);
		    		    analogGauge.setIncrementPerSmallNotch(1);
		    		    analogGauge.setScaleCenterValue(15);
		    		    analogGauge.setScaleMinValue(minValue);
		    		    analogGauge.setScaleMaxValue(maxValue);
		    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
		    		    analogGauge.setValue(minValue);
		    		}else if(fuelType.equals("Methanol")){
		    			//Set up the gauge values and the values that are handled from the sensor.
		    		    minValue = 3;
		    		    maxValue = 8;
		    		    sensorMinValue = minValue;
		    		    sensorMaxValue = minValue;
		    		    //Set up the Boost GaugeBuilder
		    		    analogGauge.setTotalNotches(10);
		    		    analogGauge.setIncrementPerLargeNotch(1);
		    		    analogGauge.setIncrementPerSmallNotch(1);
		    		    analogGauge.setScaleCenterValue(5);
		    		    analogGauge.setScaleMinValue(minValue);
		    		    analogGauge.setScaleMaxValue(maxValue);
		    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
		    		    analogGauge.setValue(minValue);
		    		}else if(fuelType.equals("Ethanol") || fuelType.equals("E85")){
		    			//Set up the gauge values and the values that are handled from the sensor.
		    		    minValue = 5;
		    		    maxValue = 12;
		    		    sensorMinValue = minValue;
		    		    sensorMaxValue = minValue;
		    		    //Set up the Boost GaugeBuilder
		    		    analogGauge.setTotalNotches(10);
		    		    analogGauge.setIncrementPerLargeNotch(1);
		    		    analogGauge.setIncrementPerSmallNotch(1);
		    		    analogGauge.setScaleCenterValue(8);
		    		    analogGauge.setScaleMinValue(minValue);
		    		    analogGauge.setScaleMaxValue(maxValue);
		    		    analogGauge.setUnitTitle(fuelType + " Wideband AFR");
		    		    analogGauge.setValue(minValue);
		    		}else{
		    			//Set up the gauge values and the values that are handled from the sensor.
		    		    minValue = 5;
		    		    maxValue = 25;
		    		    sensorMinValue = minValue;
		    		    sensorMaxValue = minValue;
		    		    //Set up the Boost GaugeBuilder
		    		    analogGauge.setTotalNotches(40);
		    		    analogGauge.setIncrementPerLargeNotch(5);
		    		    analogGauge.setIncrementPerSmallNotch(1);
		    		    analogGauge.setScaleCenterValue(15);
		    		    analogGauge.setScaleMinValue(minValue);
		    		    analogGauge.setScaleMaxValue(maxValue);
		    		    analogGauge.setUnitTitle("Wideband AFR");
		    		    analogGauge.setValue(minValue);
		    		}
		    	}
				break;
			case 3: //Temperature
				currentToken = 3;
				prefsTempInit();
				getSHHCoefficients();
				if(tempUnits.toLowerCase(Locale.US).equals("celsius")){
				    //Set up the gauge values and the values that are handled from the sensor for Celsius.
				    minValue = -35;
				    maxValue = 105;
				    sensorMinValue = minValue;
				    sensorMaxValue = minValue;
				    
				    //Set up the Boost GaugeBuilder
				    analogGauge.setTotalNotches(60);
				    analogGauge.setIncrementPerLargeNotch(20);
				    analogGauge.setIncrementPerSmallNotch(4);
				    analogGauge.setScaleCenterValue(65);
				    analogGauge.setScaleMinValue(minValue);
				    analogGauge.setScaleMaxValue(maxValue);
				    analogGauge.setUnitTitle("Temperature (C)");
				    analogGauge.setValue(minValue);
		        }else{
		        	//Set up the gauge values and the values that are handled from the sensor for Fahrenheit.
				    minValue = -20;
				    maxValue = 220;
				    sensorMinValue = 0;
				    sensorMaxValue = minValue;
				    
				    //Set up the Boost GaugeBuilder
				    analogGauge.setTotalNotches(45);
				    analogGauge.setIncrementPerLargeNotch(40);
				    analogGauge.setIncrementPerSmallNotch(8);
				    analogGauge.setScaleCenterValue(140);
				    analogGauge.setScaleMinValue(minValue);
				    analogGauge.setScaleMaxValue(maxValue);
				    analogGauge.setUnitTitle("Temperature (F)");
				    analogGauge.setValue(minValue);
		        }
				break;
			case 4: //Oil
				currentToken = 4;
				prefsOilInit();
				oilSensorInit();
				
				//Set up the gauge values and the values that are handled from the sensor.
				minValue = 0;
			    maxValue = 100;
			    sensorMinValue = 0;
			    sensorMaxValue = minValue;
			    
				analogGauge.setTotalNotches(80);
			    analogGauge.setIncrementPerLargeNotch(10);
			    analogGauge.setIncrementPerSmallNotch(2);
			    analogGauge.setScaleCenterValue(50);
			    analogGauge.setScaleMinValue(minValue);
			    analogGauge.setScaleMaxValue(maxValue);
			    analogGauge.setUnitTitle("Oil Pressure(PSI)");
			    analogGauge.setValue(minValue);
				break;
			default:
				break;
		}
	}
	
	public static double round(double unrounded){
		DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(unrounded));
    }
	
		/* Initialize Preferences */
	
	public void prefsBoostInit(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    	pressureUnits = sp.getString("pressureUnits", "PSI");
    	
    }
	
	public void prefsWidebandInit(){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		unitType = sp.getString("widebandUnits", "AFR");
    	fuelType = sp.getString("widebandFuelType", "Gasoline");
    	
    	String sLowVolts = sp.getString("afrvoltage_low_voltage", "0.00");
    	String sHighVolts = sp.getString("afrvoltage_high_voltage", "5.00");
    	String sLowAFR = sp.getString("afrvoltage_low_afr", "7.35");
    	String sHighAFR = sp.getString("afrvoltage_high_afr", "22.39");
    	try {
			wbLowVolts = Float.parseFloat(sLowVolts);
			wbHighVolts = Float.parseFloat(sHighVolts);
			wbLowAFR = Float.parseFloat(sLowAFR);
			wbHighAFR = Float.parseFloat(sHighAFR);
		} catch (NumberFormatException e) {
			System.out.println("Error in WidebandActivity.prefsInit()"+e);
			Toast.makeText(context, "Error in AFR Calibration table, restoring defaults.", Toast.LENGTH_SHORT).show();
			wbLowVolts = 0.00f;
			wbHighVolts = 5.00f;
			wbLowAFR = 7.35f;
			wbHighAFR = 22.39f;
		}
	}
	
	public void prefsOilInit(){
		SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
    	
    	String slowPSI   = sp.getString("oil_psi_low", "0"); //Assign the prefs to strings since they are stored as such.
    	String slowOhms   = sp.getString("oil_ohm_low", "10");
    	String shighPSI   = sp.getString("oil_psi_high", "80");
    	String shighOhms   = sp.getString("oil_ohm_high", "180");
    	String sbiasResistor   = sp.getString("bias_resistor_oil", "100");
    	
    	try {
			oilLowPSI   = Float.parseFloat(slowPSI); //attempt to parse the strings to floats.
			oilLowOhms   = Float.parseFloat(slowOhms);
			oilHighPSI   = Float.parseFloat(shighPSI);
			oilHighOhms   = Float.parseFloat(shighOhms);
			oilBiasResistor = Float.parseFloat(sbiasResistor);
		} catch (NumberFormatException e) { //If the parsing fails, assign default values to continue operation.
			System.out.println("Error in OilActivity.prefsInit "+e);
			oilLowPSI = 0;
			oilLowOhms = 10;
			oilHighPSI = 80;
			oilHighOhms = 180;
			oilBiasResistor = 100;
		}
	}
	
	public void prefsTempInit(){
		SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
		tempUnits = sp.getString("tempUnits", "fahrenheit");
    	
    	String stempOne   = sp.getString("temp_one", "-18.00");
    	String stempTwo   = sp.getString("temp_two", "4.00");
    	String stempThree = sp.getString("temp_three", "99.00");
    	String sohmsOne	  = sp.getString("ohms_one", "25000");
    	String sohmsTwo	  = sp.getString("ohms_two", "7500");
    	String sohmsThree = sp.getString("ohms_three", "185");
    	String sbiasRes   = sp.getString("bias_resistor", "2000");
    	
    	try {
			tempOne   = Float.parseFloat(stempOne);
			tempTwo   = Float.parseFloat(stempTwo);
			tempThree = Float.parseFloat(stempThree);
			tempOhmsOne   = Float.parseFloat(sohmsOne);
			tempOhmsTwo   = Float.parseFloat(sohmsTwo);
			tempOhmsThree = Float.parseFloat(sohmsThree);
			tempBiasResistor   = Float.parseFloat(sbiasRes);
		} catch (NumberFormatException e) {
			System.out.println("Error in WidebandActivity.prefsInit()"+e);
			Toast.makeText(context, "Error in Temp Calibration table, restoring defaults", Toast.LENGTH_SHORT).show();
			tempOne = -18.00d;
			tempTwo = 4.00d;
			tempThree = 99.00d;
			tempOhmsOne = 25000d;
			tempOhmsTwo = 7500d;
			tempOhmsThree = 185d;
			tempBiasResistor = 2000d;
		}
	}
		/*	Wideband Helper Functions */
	private void initStoich(String fuelType){ //Sets up stoich variable for lambda calc.
    	if(fuelType.equals("Gasoline")){
    		wbStoich = 14.7d;
    	}else if(fuelType.equals("Propane")){
    		wbStoich = 15.67d;
    	}else if(fuelType.equals("Methanol")){
    		wbStoich = 6.47d;
    	}else if(fuelType.equals("Diesel")){
    		wbStoich = 14.6d;
    	}else if(fuelType.equals("Ethanol")){
    		wbStoich = 9d;
    	}else if(fuelType.equals("E85")){
    		wbStoich = 9.76d;
    	}else{
    		wbStoich = 14.7d;
    	}
    }
	
		/*	Temperature Helper Functions */
	private void getSHHCoefficients(){ //Sets up Steinhart-Hart coefficients.
    	double numer;
    	double denom;
    	
    	//Start with C
    	numer = ((1/(tempOne+273.15)-1/(tempTwo+273.15))-(Math.log(tempOhmsOne)-Math.log(tempOhmsTwo))*(1/(tempOne+273.15)-1/(tempThree+273.15))/(Math.log(tempOhmsOne)-Math.log(tempOhmsThree)));
    	denom = ((Math.pow((Math.log(tempOhmsOne)), 3)-Math.pow((Math.log(tempOhmsTwo)), 3) - (Math.log(tempOhmsOne)-Math.log(tempOhmsTwo))*(Math.pow((Math.log(tempOhmsOne)),3)-Math.pow((Math.log(tempOhmsThree)), 3))/(Math.log(tempOhmsOne)-Math.log(tempOhmsThree))));
    	c = numer / denom;
    	
    	//Then B
    	b = ((1/(tempOne+273.15)-1/(tempTwo+273.15))-c*(Math.pow((Math.log(tempOhmsOne)), 3)-Math.pow((Math.log(tempOhmsTwo)), 3)))/(Math.log(tempOhmsOne)-Math.log(tempOhmsTwo));
    	
    	//Finally A
    	a = 1/(tempOne+273.15)-c*Math.pow((Math.log(tempOhmsOne)), 3)-b*Math.log(tempOhmsOne);
    }
	
	private double getTemperature(double resistance){ //Get temp.
    	double ret = 0;
    	
    	//This is the final equation.
    	try {
			ret = ((1/(a+(b*Math.log(resistance))+(c*(Math.pow((Math.log(resistance)), 3)))))-273.15);
		} catch (Exception e) {
			System.out.println("Error in TemperatureActivity.getTemperature"+e);
			Toast.makeText(context, "Something happened when calculating the temperature. I'm so sorry.", Toast.LENGTH_SHORT).show();
		}
    	
    	return ret;
    }
	
	private double getResistance(float ADC){
    	double numer;
    	double denom;
    	double ret;
    	
    	numer = tempBiasResistor*(ADC/1024);
    	denom = Math.abs((ADC/1024)-1);
    	
    	ret = numer / denom;
    	return ret;
    }
	
	private double getF(double c){
    	return (c*1.8)+32;
    }
	
		/*	Oil Helper Functions */
	private void oilSensorInit(){
    	oilLowVolts = (oilLowOhms/(oilBiasResistor+oilLowOhms))*5;
    	oilHighVolts = (oilHighOhms/(oilBiasResistor+oilHighOhms))*5;
    	oilRangeVolts = oilHighVolts - oilLowVolts;
    	oilRangePSI = oilHighPSI - oilLowPSI;
    }
}
