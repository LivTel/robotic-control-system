package ngat.rcs.scm.collation;

import java.io.Serializable;
import java.util.Hashtable;

import ngat.util.SerializableStatusCategory;

public class SeeingStatus implements Serializable {
	
	 /** Status time stamp.*/
    long timeStamp;

    double rawSeeing;
    
    double correctedSeeing;
    
    double extinctionCat;

    double prediction;
    
    double elevation;
    
    double azimuth;
    
    double wavelength;
    
    boolean standard;
    
    String source;
    
    String targetName;
    
	/**
	 * 
	 */
	public SeeingStatus() {
	
	}

	
	
	/**
	 * @return the rawSeeing
	 */
	public double getRawSeeing() {
		return rawSeeing;
	}



	/**
	 * @param rawSeeing the rawSeeing to set
	 */
	public void setRawSeeing(double rawSeeing) {
		this.rawSeeing = rawSeeing;
		
	}



	/**key
	 * @return the correctedSeeing
	 */
	public double getCorrectedSeeing() {
		return correctedSeeing;
	}



	/**
	 * @param correctedSeeing the correctedSeeing to set
	 */
	public void setCorrectedSeeing(double correctedSeeing) {
		this.correctedSeeing = correctedSeeing;
		
	}



	/**
	 * @return the extinctionCat
	 */
	public double getExtinctionCat() {
		return extinctionCat;
	}



	/**
	 * @param extinctionCat the extinctionCat to set
	 */
	public void setExtinctionCat(double extinctionCat) {
		this.extinctionCat = extinctionCat;
		String ext = "";
		if (extinctionCat < 0.5)
			ext = "PHOTOMETRIC";
		else
			ext = "SPECTROSCOPIC";
	
	}



	/**
	 * @return the prediction
	 */
	public double getPrediction() {
		return prediction;
	}



	/**
	 * @param prediction the prediction to set
	 */
	public void setPrediction(double prediction) {
		this.prediction = prediction;
		
	}


	
	

	/**
	 * @return the elevation
	 */
	public double getElevation() {
		return elevation;
	}



	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
		
	}



	/**
	 * @return the azimuth
	 */
	public double getAzimuth() {
		return azimuth;
	}



	/**
	 * @param azimuth the azimuth to set
	 */
	public void setAzimuth(double azimuth) {
		this.azimuth = azimuth;
		
	}



	/**
	 * @return the wavelength
	 */
	public double getWavelength() {
		return wavelength;
	}



	/**
	 * @param wavelength the wavelength to set
	 */
	public void setWavelength(double wavelength) {
		this.wavelength = wavelength;
		
	}



	/**
	 * @return the standard
	 */
	public boolean isStandard() {
		return standard;
	}



	/**
	 * @param standard the standard to set
	 */
	public void setStandard(boolean standard) {
		this.standard = standard;
		
	}



	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}



	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
		
	}



	/**
	 * @return the targetName
	 */
	public String getTargetName() {
		return targetName;
	}



	/**
	 * @param targetName the targetName to set
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
		
	}



	/**
	 * @param timeStamp the timeStamp to set
	 */
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}



	public long getTimeStamp() {
		return timeStamp;
	}
    
    @Override
	public String toString() {
    	return "SkyModelStatus: r="+rawSeeing+", c="+correctedSeeing+", p="+prediction+", e="+extinctionCat;
    }
    
}
