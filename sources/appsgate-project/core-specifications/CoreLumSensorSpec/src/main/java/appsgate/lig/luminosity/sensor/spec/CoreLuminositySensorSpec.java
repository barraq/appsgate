package appsgate.lig.luminosity.sensor.spec;

import java.util.List;

/**
 * This java interface is an ApAM specification shared by all ApAM
 * Appsgate application to handle illumination variation from sensors.
 * 
 * @author Cédric Gérard
 * @version 1.0.0
 * @since February 5, 2013
 *
 */
public interface CoreLuminositySensorSpec {
	
	/**
	 * Enum type that defines available luminosity units
	 * @author Cédric Gérard
	 *
	 */
	public enum LuminosityUnit {
		Lux;
	}
	
	/**
	 * Get the current luminosity unit
	 * @return a LuminosityUnit object that represent the luminosity unit
	 */
	public LuminosityUnit getLuminosityUnit();
	
	/**
	 * Get the current illumination = the last value sent by the illumination sensor
	 * @return the illumination as an integer
	 */
	public int getIllumination();
        
        /**
         * Get the current illumination label = label of the last value sent by the illumination sensor
         * @return the illumination as a string·
         */
        public String getCurrentIlluminationLabel();
        
        /**
         * Get the illumination label = label of the value in parameter
         * @param value
         * @return the illumination as a string·
         */
        public String getIlluminationLabel(int value);
        
        /**
         * Get the list of the scale labels
         * @return list of scale labels
         */
        public List<String> getListScaleLabel();
        
        /**
         * Get the value of minimum value of the interval of the label
         * @param labelIllumination
         * @return minimum value of the current interval 
         */
        public int getMinValue(String labelIllumination);
        
        /**
         * Get the value of maximum value of the interval of the label 
         * @param labelIllumination
         * @return maximum value of the current interval 
         */
        public int getMaxValue(String labelIllumination);

}
