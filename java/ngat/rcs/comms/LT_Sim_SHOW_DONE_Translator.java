/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package ngat.rcs.comms;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

public class LT_Sim_SHOW_DONE_Translator implements Translator {
      
    protected Properties prop;

    /** Stores the key from the actual command sent.*/
    protected int key;

    public LT_Sim_SHOW_DONE_Translator(int key) { 
	this.key = key;
    }
    
    public Object translate(Object data) throws TranslationException {
	
	TCS_Status.Segment xstatus = null;

	if ( ! (data instanceof Properties ) )
	    throw new TranslationException("SHOW_DONE Data was not a java.util.Properties: "+
					   data.getClass().getName());
	
	prop = (Properties)data;

	try {
	    switch (key) {
	    // Astrometry.
	    case SHOW.ASTROMETRY:{
		TCS_Status.Astrometry status = new TCS_Status.Astrometry();
		
		status.refractionPressure    = parseDouble("refractionPressure");		
		status.refractionTemperature = parseDouble("refractionTemperature");		
		status.refractionHumidity    = parseDouble("refractionHumidity");		
		status.refractionWavelength  = parseDouble("refractionWavelength");			
		status.ut1_utc               = parseDouble("ut1_utc");		
		status.tdt_utc               = parseDouble("tdt_utc");		
		status.polarMotion_X         = parseDouble("polarMotion_X");		
		status.polarMotion_Y         = parseDouble("polarMotion_Y");		
		status.airmass               = parseDouble("airmass");
		status.agwavelength          = parseDouble("agwavelength");
		xstatus = status;
	    }
	    break;
		
	    // Autoguider.
	    case SHOW.AUTOGUIDER:{
		TCS_Status.Autoguider status = new TCS_Status.Autoguider();
		
		status.agSelected            = parseString("agSelected");	
		status.agStatus              = parseInt   ("agStatus");		
		status.agMode                = parseInt   ("agMode");		
		status.guideStarMagnitude    = parseDouble("guideStarMagnitude");		
		status.fwhm                  = parseDouble("fwhm");		

		status.agMirrorDemand        = parseDouble("agMirrorDemand");		
		status.agMirrorPos           = parseDouble("agMirrorPos");		
		status.agMirrorStatus        = parseInt   ("agMirrorStatus");		

		status.agFocusDemand         = parseDouble("agFocusDemand");		
		status.agFocusPos            = parseDouble("agFocusPos");		
		status.agFocusStatus         = parseInt   ("agFocusStatus");	
	
		status.agFilterDemand        = parseInt   ("agFilterDemand");	
		status.agFilterPos           = parseInt   ("agFilterPos");
		status.agFilterStatus        = parseInt   ("agFilterStatus");
		xstatus = status;
	    }
	    break;
		
	    // Calibrate.	    
	    case SHOW.CALIBRATE:{
		TCS_Status.Calibrate status = new TCS_Status.Calibrate();
		
		status.defAzError    = parseDouble("defAzError");	
		status.defAltError   = parseDouble("defAltError");		
		status.defCollError  = parseDouble("defCollError");

		status.currAzError   = parseDouble("currAzError");
		status.currAltError  = parseDouble("currAltError");
		status.currCollError = parseDouble("currCollError");
		
		status.lastAzError   = parseDouble("lastAzError");		
		status.lastAzRms     = parseDouble("lastAzRms");		
		
		status.lastAltError  = parseDouble("lastAltError");	
		status.lastAltRms    = parseDouble("lastAltRms");
		
		status.lastCollError = parseDouble("lastCollError");	
		status.lastCollRms   = parseDouble("lastCollRms");
		
		status.lastSkyRms    = parseDouble("lastSkyRms");	
		xstatus = status;
	    }
	    break;
	    
	    // Focus.
	    case SHOW.FOCUS:{
		TCS_Status.FocalStation status = new TCS_Status.FocalStation();
		
		status.station = ((String)prop.get("station"));		
		status.instr   = ((String)prop.get("instr"));		
		status.ag      = ((String)prop.get("ag"));
		xstatus = status;
	    }
	    break;
	    
	    // Limits.
	    case SHOW.LIMITS:{
		TCS_Status.Limits status = new TCS_Status.Limits();
		
		status.azPosLimit     = parseDouble("azPosLimit");		
		status.azNegLimit     = parseDouble("azNegLimit"); 
		
		status.altPosLimit    = parseDouble("altPosLimit");		
		status.altNegLimit    = parseDouble("altNegLimit");
		
		status.rotPosLimit    = parseDouble("rotPosLimit");		
		status.rotNegLimit    = parseDouble("rotNegLimit");
		
		status.timeToAzLimit  = parseDouble("timeToAzLimit");
		status.azLimitSense   = parseInt   ("azLimitSense");

		status.timeToAltLimit = parseDouble("timeToAltLimit");
		status.altLimitSense  = parseInt   ("altLimitSense");

		status.timeToRotLimit = parseDouble("timeToRotLimit");
		status.rotLimitSense  = parseInt   ("rotLimitSense");
		xstatus = status;
	    }
	    break;
	    
	    // Mechanisms.
	    case SHOW.MECHANISMS:{
		TCS_Status.Mechanisms status = new TCS_Status.Mechanisms();
				
		status.azName                = ((String)prop.get("azName"));		
		status.azDemand              = parseDouble("azDemand");				
		status.azPos                 = parseDouble("azPos");		
		status.azStatus              = parseInt("azStatus");
		
		status.altName               = ((String)prop.get("altName"));		
		status.altDemand             = parseDouble("altDemand");		
		status.altPos                = parseDouble("altPos");		
		status.altStatus             = parseInt("altStatus");
		
		status.rotName               = ((String)prop.get("rotName"));		
		status.rotDemand             = parseDouble("rotDemand");		
		status.rotPos                = parseDouble("rotPos");		
		status.rotMode               = parseInt("rotMode");		
		status.rotSkyAngle           = parseDouble("rotSkyAngle");		
		status.rotStatus             = parseInt("rotStatus");
		
		status.encShutter1Name       = ((String)prop.get("encShutter1Name"));		
		status.encShutter1Demand     = parseInt("encShutter1Demand");
		status.encShutter1Pos        = parseInt("encShutter1Pos");
		status.encShutter1Status     = parseInt("encShutter1Status");

		status.encShutter2Name       = ((String)prop.get("encShutter2Name"));		
		status.encShutter2Demand     = parseInt("encShutter2Demand");
		status.encShutter2Pos        = parseInt("encShutter2Pos");
		status.encShutter2Status     = parseInt("encShutter2Status");
		
		status.foldMirrorName        = ((String)prop.get("foldMirrorName"));		
		status.foldMirrorDemand      = parseInt("foldMirrorDemand");	
		status.foldMirrorPos         = parseInt("foldMirrorPos");
		status.foldMirrorStatus      = parseInt("foldMirrorStatus");
		
		status.primMirrorName        = ((String)prop.get("primMirrorName"));		
		status.primMirrorCoverDemand = parseInt("primMirrorCoverDemand");	
		status.primMirrorCoverPos    = parseInt("primMirrorCoverPos");	
		status.primMirrorCoverStatus = parseInt("primMirrorCoverStatus");
		
		status.secMirrorName         = ((String)prop.get("secMirrorName"));		
		status.secMirrorDemand       = parseDouble("secMirrorDemand");		
		status.secMirrorPos          = parseDouble("secMirrorPos");		
		status.focusOffset           = parseDouble("focusOffset");		
		status.secMirrorStatus       = parseInt("secMirrorStatus");
		
		status.primMirrorSysName     = ((String)prop.get("primMirrorSysName"));		
		status.primMirrorSysStatus   = parseInt("primMirrorSysStatus");
		xstatus = status;
	    }
	    break;
	    
	    // Meteorology.
	    case SHOW.METEOROLOGY:{
		TCS_Status.Meteorology status = new TCS_Status.Meteorology();
		
		status.wmsStatus = parseInt("wmsStatus");
		status.rainState = parseInt("rainState");
				
		status.extTemperature            = parseDouble("extTemperature");		
		status.serrurierTrussTemperature = parseDouble("serrurierTrussTemperature");		
		status.oilTemperature            = parseDouble("oilTemperature");		
		status.primMirrorTemperature     = parseDouble("primMirrorTemperature");
		status.secMirrorTemperature      = parseDouble("secMirrorTemperature");

		status.windSpeed                 = parseDouble("windSpeed");		
		status.windDirn                  = parseDouble("windDirn");		
		status.pressure                  = parseDouble("pressure");		
		status.humidity                  = parseDouble("humidity");
		status.lightLevel                = parseDouble("lightLevel");
		xstatus = status;
	    }
	    break;
	    
	    // Source.
	    case SHOW.SOURCE:{
		TCS_Status.SourceBlock status = new TCS_Status.SourceBlock();
		
		status.srcName           = ((String)prop.get("srcName"));		
		status.srcRa             = parseDouble("srcRa");		
		status.srcDec            = parseDouble("srcDec");		
		status.srcEquinox        = parseDouble("srcEquinox");		
		status.srcEpoch          = parseDouble("srcEpoch");
		status.srcNsTrackRA      = parseDouble("srcNsTrackRA");
		status.srcNsTrackDec     = parseDouble("srcNsTrackDec");
		status.srcPmRA           = parseDouble("srcPmRA");	
		status.srcPmDec          = parseDouble("srcPmDec");		
		status.srcParallax       = parseDouble("srcParallax");
		status.srcRadialVelocity = parseDouble("srcRadialVelocity");
		xstatus = status;
	    }
	    break;
	    
	    // State.
	    case SHOW.STATE:{
		TCS_Status.State status = new TCS_Status.State();
		
		status.networkControlState      = parseInt("networkControlState");	
		status.engineeringOverrideState = parseInt("engineeringOverrideState");
		status.telescopeState           = parseInt("telescopeState");		
		status.tcsState                 = parseInt("tcsState");
		status.systemRestartFlag        = parseBoolean("systemRestartFlag");
		status.systemShutdownFlag       = parseBoolean("systemShutdownFlag");
		xstatus = status;
	    }
	    break;
	    
	    // TCS Version.
	    case SHOW.VERSION:{
		TCS_Status.Version status = new TCS_Status.Version();
		
		status.tcsVersion = ((String)prop.get("tcsVersion"));
		xstatus = status;
	    }
	    break;
	    
	    // Time.
	    case SHOW.TIME:{
		TCS_Status.Time status = new TCS_Status.Time();
		
		status.mjd = parseDouble("mjd");		
		status.ut1 = parseDouble("ut1");		
		status.lst = parseDouble("lst");
	    
		status.timeStamp = parseLong("timeStamp");
		xstatus = status;
	    }
	    break;
	    default:
		break;
	    }
	} catch (NumberFormatException pe) {
	    System.out.println("SIM_SHOW::parse error returning NULL");
	    return null;
	    //throw new TranslationException("Error translating data: "+pe);
	}
	
	//System.out.println("SIM_SHOW::Returning an inst of: "+xstatus.getClass().getName());
	
	return xstatus;	
	
    }

    private boolean parseBoolean(String key) {
	String args = (String)prop.get(key);
	if (args == null) return false;
	if (args.equals("true"))
	    return true;
	return false;
    }

    private String parseString(String key) {
	String data = "NOT_SET";
	String args = (String)prop.get(key);
	if (args == null) args = data; 
	//System.err.println("SIM_SHOW::Find: Key:"+key+" value:"+args);
	return args;
    }

    private long parseLong(String key) {
	long data = 0L;
	String args = (String)prop.get(key);
	if (args == null) return data;
	try {
	    data = Long.parseLong(args);
	    //System.err.println("SIM_SHOW::Parse: Key:"+key+" value:"+args+" -> result: "+data);
	} catch (NumberFormatException e) {
	    //System.err.println("Error parsing Long from key: "+key+" args: "+args);
	}
	return data;
    }
    
    private double parseDouble(String key) {
	double data = 0.0;
	String args = (String)prop.get(key);
	if (args == null) return data;
	try {
	    data = Double.parseDouble(args); 
	    //System.err.println("SIM_SHOW::Parse: Key:"+key+" value:"+args+" -> result: "+data);
	} catch (NumberFormatException e) {
	    System.err.println("Error parsing Double from key: "+key+" args: "+args);
	} 
	return data;
    }
    
    private int parseInt(String key) {
	int data = 0;
	String args = (String)prop.get(key);
	if (args == null) return data;
	try {
	    data = Integer.parseInt(args);
	    //System.err.println("SIM_SHOW::Parse: Key:"+key+" value:"+args+" -> result: "+data);
	} catch (NumberFormatException e) {
	    System.err.println("Error parsing Int from key: "+key+" args: "+args);
	}
	return data;
    }


}
