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

import java.util.*;
import java.text.*;

import ngat.util.*;
import ngat.phase2.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** Implementation of a CommandTranslator for the Liverpool Telescope
 * Simulated Telescope Control System. All source command objects must be 
 * instances of ngat.message.RCS_TCS.RCS_TO_TCS or subclasses. 
 * <br><br>
 * $Id: LT_Sim_CommandTranslatorFactory.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public class LT_Sim_CommandTranslatorFactory implements CommandTranslatorFactory {

    public static final int OK = 0;

    public static final int ILLEGAL_CIL_RETURN_TYPE  = 606501;
    
    public static final int CIL_RESPONSE_PARSE_ERROR = 606502;
    
    public static final int LT_SIM_TRANSLATION_ERROR = 606503;

    /** Generic TCS generated error - these need expanding.*/
    public static final int TCS_ERROR = 500000;


    /** The singleton instance.*/
    private static LT_Sim_CommandTranslatorFactory instance = null;

    /** @return A reference to the singleton instance.*/
    public static LT_Sim_CommandTranslatorFactory getInstance() {
	if (instance == null)
	    instance = new LT_Sim_CommandTranslatorFactory();
	return instance;
    }

    /** Creates the singleton instance.*/
    private LT_Sim_CommandTranslatorFactory() {}

    /** Translates a supplied RCS_TO_TCS command into a String
     * suitable for the RGO TCS. If command is null or of the
     * wrong (subclass) type, returns null.
     * @param command A subclass of ngat.message.RCS_TCS.RCS_TO_TCS - 
     * the command to translate.
     * @return The translated command String.
     */
    public Object translateCommand(Object command) {
	if ( (command == null) ||
	     ( ! (command instanceof RCS_TO_TCS)) )
	    return null;
	
	// Calculate the string for Simulation TCS Command.
	
	if (command instanceof AGCENTROID ) {
	    return "AGCENTROID:";	
	}
	
	if ( command instanceof AGFILTER ) {
	    
	    switch (((AGFILTER)command).getState()) {
	    case AGFILTER.IN:
		return "AGFILTER: &state IN";
	    case AGFILTER.OUT:
		return "AGFILTER: &state OUT";
	    }
	}

	if ( command instanceof AGFOCUS ) {    
	    return "AGFOCUS: &focus "+((AGFOCUS)command).getFocus();
	}
	
	if ( command instanceof AGMOVE ) {	    
	    return "AGMOVE: &position "+((AGMOVE)command).getPosition() + 
		" &angle " + ((AGMOVE)command).getAngle()+" &pixelX "+
		((AGMOVE)command).getPixelX()+" &pixelY "+((AGMOVE)command).getPixelY();
	}
	
	if (command instanceof AGRADIAL) {
	    return "AGRADIAL: &position "+((AGRADIAL)command).getPosition();
	}
	 
	if ( command instanceof AGSELECT ) {   
	    switch (((AGSELECT)command).getAutoguider()) {
	    case AGSELECT.CASSEGRAIN:
		return "AGSELECT: &autoguider CASSEGRAIN";
	    }
	}
	
	if ( command instanceof AGVIEW ) {
	    return "AGVIEW:";
	}
	
	if ( command instanceof AGWAVELENGTH ) {	    
	    return "AGWAVELENGTH: &wavelength " + ((AGWAVELENGTH)command).getWavelength(); 
	}
	
	if ( command instanceof ALTITUDE ) {	    
	    return "ALTITUDE: &angle " + ((ALTITUDE)command).getAngle();
	}
	
	if ( command instanceof AUTOGUIDE ) {
	    String args = "AUTOGUIDE: ";
	    
	    switch (((AUTOGUIDE)command).getState()) {
	    case AUTOGUIDE.ON:
		args += "&state ON";
		break;
	    case AUTOGUIDE.OFF:
		args += "&state OFF";
		break;
	    case AUTOGUIDE.SUSPEND:
		args += "&state SUSPEND";
		break;
	    case AUTOGUIDE.RESUME:
		args += "&state RESUME";
		break;
	    }
	    
	    switch (((AUTOGUIDE)command).getMode()) {
	    case AUTOGUIDE.RANK:
		if (((AUTOGUIDE)command).getRank() == 1) return args+" "+"&mode BRIGHTEST";
		return args+" "+"&mode RANK &rank "+((AUTOGUIDE)command).getRank();
	    case AUTOGUIDE.RANGE:
		return args+" "+"&mode RANGE &range1 "+((AUTOGUIDE)command).getRange1()+
		    " &range2 "+((AUTOGUIDE)command).getRange2();
	    case AUTOGUIDE.PIXEL:
		return args+" "+"&mode PIXEL &pixelX "+((AUTOGUIDE)command).getXPix()+
		    " &pixelY "+((AUTOGUIDE)command).getYPix();
	    default:
		return args;
	    }
	}
	
	if ( command instanceof AZIMUTH ) {
	    return "AZIMUTH: &angle " + ((AZIMUTH)command).getAngle(); 
	}
	
	if ( command instanceof BEAMSWITCH ) {
	    return "BEAMSWITCH: &offsetX" + ((BEAMSWITCH)command).getOffsetX() 
		+ " &offsetY " + ((BEAMSWITCH)command).getOffsetY();
	}

	if ( command instanceof CALIBRATE ) {
	    switch (((CALIBRATE)command).getMode()) {		    
	    case CALIBRATE.DEFAULT:
		return "CALIBRATE: &mode DEFAULT";
	    case CALIBRATE.NEW:
		return "CALIBRATE: &mode NEW";
	    case CALIBRATE.LAST:
		return "CALIBRATE: &mode LAST";
	    }
	}
	
	if ( command instanceof DFOCUS ) {
	    return "DFOCUS: &offset " + ((DFOCUS)command).getOffset(); 
	}
	
	if ( command instanceof ENCLOSURE ) {
	    int pos = ((ENCLOSURE)command).getPos();
	   
	    String mech = "";
	    switch (((ENCLOSURE)command).getMechanism()) {
	    case ENCLOSURE.BOTH:
		mech = "BOTH";
		break;
	    case ENCLOSURE.NORTH:
		mech = "NORTH";
		break;
	    case ENCLOSURE.SOUTH:
		mech = "SOUTH";
		
	    }

	    String state = "";
	    switch (((ENCLOSURE)command).getState()) {
	    case ENCLOSURE.OPEN:
		state = "OPEN";
		break;
	    case ENCLOSURE.CLOSE:
		state = "CLOSE";
		break;
	    }
	    return "ENCLOSURE: &mechanism "+mech+" &state "+state;

	}
	
	if ( command instanceof FOCUS ) {
	    return "FOCUS: &focus " + ((FOCUS)command).getFocus();
	}
	
	if ( command instanceof HUMIDITY ) {
	    return "HUMIDITY: &humidity " + ((HUMIDITY)command).getHumidity(); 
	}
	
	if (command instanceof INSTRUMENT ) {
	    return "INSTRUMENT: &instrument " + ((INSTRUMENT)command).getInstrumentAlias();
	}
	
	if ( command instanceof MIRROR_COVER ) {
	    
	    switch (((MIRROR_COVER)command).getState()) {
	    case MIRROR_COVER.OPEN:
		    return "MIRROR_COVER: &state OPEN";
	    case MIRROR_COVER.CLOSE:
		return "MIRROR_COVER: &state CLOSE";
	    }
	}
	
	if ( command instanceof MOVE_FOLD ) {
	    
	    switch (((MOVE_FOLD)command).getState()) {
	    case MOVE_FOLD.STOWED:
		return "MOVE_FOLD: &state STOWED";
	    case MOVE_FOLD.POSITION1:
		return "MOVE_FOLD: &state POSITION1";
	    case MOVE_FOLD.POSITION2:
		return "MOVE_FOLD: &state POSITION2";
	    case MOVE_FOLD.POSITION3:
		return "MOVE_FOLD: &state POSITION3";
	    case MOVE_FOLD.POSITION4:
		return "MOVE_FOLD: &state POSITION4";
	    }
	}
	
	if ( command instanceof OFFBY ) {
	    
	    String args = "";
	    
	    switch (((OFFBY)command).getMode()) {		
	    case OFFBY.ARC:
		args = "OFFBY: &mode ARC ";
	    case OFFBY.TIME:
		args = "OFFBY: &mode TIME ";
	    }
	    // sim args are in degress
	    args = args + "&offsetRA "+ Math.toDegrees(((OFFBY)command).getOffsetRA() ) +
		" &offsetDec " + Math.toDegrees(((OFFBY)command).getOffsetDec());
	    return args;
	}
	
	
	if ( command instanceof OPERATIONAL ) {
	    
	    switch (((OPERATIONAL)command).getState()) {
	    case OPERATIONAL.OFF:
		return "OPERATIONAL: &state OFF";
	    case OPERATIONAL.ON:
		return "OPERATIONAL: &state ON";
	    }
	}
	
	if ( command instanceof PARK ) {
	    return "PARK: &position ZENITH";
	}
	
	if ( command instanceof POLE ) {
	    return "POLE: &xpos " + ((POLE)command).getXPos() + 
		" &ypos " + ((POLE)command).getYPos(); 
	}

	if ( command instanceof PRESSURE ) {
	    return "PRESSURE: &pressure " + ((PRESSURE)command).getPressure();
	}
	
	if ( command instanceof ROTATOR ) {		
	    
	    switch (((ROTATOR)command).getMode()) {
	    case ROTATOR.SKY:
		return "ROTATOR: &mode SKY &position " + 
		    Math.toDegrees(((ROTATOR)command).getPosition());
	    case ROTATOR.MOUNT:
		return "ROTATOR: &mode MOUNT &position " + 
		    Math.toDegrees(((ROTATOR)command).getPosition());
	    case ROTATOR.FLOAT:
		return "ROTATOR: &mode FLOAT";
	    case ROTATOR.VERTICAL:
		return "ROTATOR: &mode VERTICAL";
	    case ROTATOR.VFLOAT:
		return "ROTATOR: &mode VFLOAT";
	    }
	}
	
	
	if ( command instanceof SHOW ) {
	    
	    switch (((SHOW)command).getKey()) {
	    case SHOW.ALL:
		return "SHOW: &key ALL";
	    case SHOW.ASTROMETRY: 
		return "SHOW: &key ASTROMETRY";
	    case SHOW.AUTOGUIDER: 
		return "SHOW: &key AUTOGUIDER";
	    case SHOW.CALIBRATE:
		return "SHOW: &key CALIBRATE";
	    case SHOW.FOCUS: 
		return "SHOW: &key FOCUS";
	    case SHOW.LIMITS: 
		return "SHOW: &key LIMITS";
	    case SHOW.MECHANISMS: 
		return "SHOW: &key MECHANISMS";
	    case SHOW.METEOROLOGY: 
		return "SHOW: &key METEOROLOGY";
	    case SHOW.SOURCE: 
		return "SHOW: &key SOURCE";
	    case SHOW.STATE: 
		return "SHOW: &key STATE";
	    case SHOW.TIME: 
		return "SHOW: &key TIME";
	    case SHOW.VERSION:
		return "SHOW: &key VERSION";
	    }
	}
	
	if ( command instanceof SLEW ) {
	    
	    String   args = "GOTO: ";

	    Source   source   = ((SLEW)command).getSource();
	    Position position = source.getPosition();	    
	    double   ra       = position.getRA();
	    double   dec      = position.getDec();
	    
	    args = args + " &ra "  + Math.toDegrees(ra);
	    args = args + " &dec " + Math.toDegrees(dec);
	 
	    args = args + " &equinox " + source.getEquinoxLetter() + source.getEquinox();
	    args = args + " &epoch " + source.getEpoch();
	 
	    if (source instanceof SolarSystemSource) {
		args = args + " &nsTrackRA " + ((SolarSystemSource)source).getNsTrackRA();
		args = args + " &nsTrackDec " + ((SolarSystemSource)source).getNsTrackDec();
		args = args + " &pmRA 0.0";
		args = args + " &pmDec 0.0";
		args = args + " &parallax 0.0";
		args = args + " &radialVel 0.0";
	    } else {
		args = args + " &nsTrackRA 0.0";
		args = args + " &nsTrackDec 0.0";
		args = args + " &pmRA " + ((ExtraSolarSource)source).getPmRA();
		args = args + " &pmDec " + ((ExtraSolarSource)source).getPmDec();
		args = args + " &parallax " + ((ExtraSolarSource)source).getParallax();
		args = args + " &radialVel " + ((ExtraSolarSource)source).getRadialVelocity();
	    }
	    return args;
	}
	
	if ( command instanceof STOP ) {
	    
	    switch (((STOP)command).getMechanism()) {
	    case STOP.ALL:
		return "STOP: &mechanism ALL";
	    case STOP.AGFOCUS: 
		return "STOP: &mechanism AGFOCUS";
	    case STOP.AGPROBE: 
		return "STOP: &mechanism AGPROBE";
	    case STOP.ALTITUDE: 
		return "STOP: &mechanism ALTITUDE";
	    case STOP.AZIMUTH: 
		return "STOP: &mechanism AZIMUTH";
	    case STOP.ENCLOSURE:
		return "STOP: &mechanism ENCLOSURE";
	    case STOP.FOCUS: 
		return "STOP: &mechanism FOCUS";
	    case STOP.ROTATOR: 
		return "STOP: &mechanism ROTATOR";
	    }
	}
	
	if ( command instanceof TEMPERATURE ) {
	    return "TEMPERATURE: &temperature " + ((TEMPERATURE)command).getTemperature();
	}
	
	if ( command instanceof TRACK ) { 

	    String args = "TRACK: ";
	    
	    switch (((TRACK)command).getMechanism()) {
	    case TRACK.ALL:
		args += "&mechanism ALL ";
	    }
	    switch (((TRACK)command).getState()) {
	    case TRACK.OFF:
		args += "&state OFF";
		break;
	    case TRACK.ON:
		args += "&state ON";
		break;
	    }
	    return args;
	}
	
	if ( command instanceof TWEAK ) {
	    return "TWEAK: &xOffset " + ((TWEAK)command).getXOffset() + 
		" &yOffset " + ((TWEAK)command).getYOffset(); 
	}
	
	if ( command instanceof UNWRAP ) {
	    
	    switch (((UNWRAP)command).getMechanism()) {
	    case UNWRAP.AZIMUTH:
		return "UNWRAP: &mechanism AZIMUTH";
	    case UNWRAP.ROTATOR:
		return "UNWRAP: &mechanism ROTATOR";
	    }
	}
	
	if ( command instanceof UT1UTC ) {		
	    return "UT1_UTC: &value " + ((UT1UTC)command).getValue(); 
	}
	
	if ( command instanceof WAVELENGTH ) {		
	    return "WAVELENGTH: &wavelength " + ((WAVELENGTH)command).getWavelength();
	}
	
	return "NO_COMMAND:";
    }

    /** Translates a supplied response String from the RGO TCS
     * into a ngat.message.COMMAND_DONE. If data is null or not
     * a String, returns an empty String.<br>
     * ## Currently this does nothing ##.
     * @param command The command (class) - subclass of RCS_TO_TCS 
     * which generated this response - needed in order to parse
     * the response (a String) which gives no indication of the
     * command which originated it.
     * @param data The response String formatted in accordance
     * with that specified in the RGO TCS User Manual.
     * @return A COMMAND_DONE encapsulating the response data.
     */
    public Object translateResponse(Object command, Object data) {

	boolean success     = true;
	int     errorCode   = OK;
	String  errorString = "";
	String  response    = "";

	// Test for unexpected response type.
	if ( ! (data instanceof String) ) { 
	    success   = false;
	    errorCode = ILLEGAL_CIL_RETURN_TYPE;
	    errorString = "Unknown response type: "+data.getClass().getName();
	} else {
	    response = ((String)data).trim();
	}

	// Test for an error response. **  TBD TBD **
	// ########################################################################
	// ### We need to parse the response to get the correct error code here ###
	// ### and set the errorstring and code appropriately.                  ###
	// ########################################################################
	if ( success ) {
	    if (response.startsWith("ERROR:")) {
		// Chop off the designator.
		response = response.substring(response.indexOf(":")+1);
		success   = false;
		errorCode = TCS_ERROR;
		errorString = "TCS-Error: "+response;
	    }
	}

	if (command instanceof SHOW) { //use a SHOW_DONE. 
	    
	    SHOW_DONE done = new SHOW_DONE(((COMMAND)command).getId()+"-ProxySHOWResponse");
	   
	    if ( ! success ) {
		done.setSuccessful(false);
		done.setErrorNum(errorCode);
		done.setErrorString("SHOW_DONE: "+errorString);
		return done;
	    }
	    
	    CommandParser parser = new CommandParser("&");
	    try {
		parser.parse(response, "enable", "disable");
	    } catch (ParseException pe) {
		done.setSuccessful(false);
		done.setErrorNum(CIL_RESPONSE_PARSE_ERROR);
		done.setErrorString("Error parsing SHOW_DONE data: "+pe);
		return done;
	    }
	    
	    Properties map = parser.getMap();
	    
	    LT_Sim_SHOW_DONE_Translator translator = 
		new LT_Sim_SHOW_DONE_Translator(((SHOW)command).getKey());
	    TCS_Status.Segment tcs_statusSegment = null;
	    try {
		tcs_statusSegment = (TCS_Status.Segment)translator.translate(map);
	    } catch (TranslationException te) {
		done.setSuccessful(false);
		done.setErrorNum(LT_SIM_TRANSLATION_ERROR);
		done.setErrorString("Error translating SHOW_DONE data: "+te);
		return done;
	    }

	    done.setSuccessful(true);
	    done.setErrorNum(OK);
	    done.setErrorString("");
	    done.setStatus(tcs_statusSegment);
	   
	    return done;
	    
	}

	if (command instanceof DFOCUS) { //use an OFFSET_FOCUS_DONE. 
	    
	    ngat.message.ISS_INST.OFFSET_FOCUS_DONE done = 
		new  ngat.message.ISS_INST.OFFSET_FOCUS_DONE(((COMMAND)command).getId()+"-ProxyDFOCUSResponse");
	    
	    if ( ! success ) {
		done.setSuccessful(false);
		done.setErrorNum(errorCode);
		done.setErrorString("DFOCUS_DONE: "+errorString);
		return done;
	    }

	    done.setSuccessful(true);
	    done.setErrorNum(OK);
	    done.setErrorString(errorString);
	    
	    return done;
	}

	if (command instanceof AGFOCUS) { // use AGFOCUS_DONE.
	    COMMAND_DONE done = new AGFOCUS_DONE(((COMMAND)command).getId()+"-ProxyAGFOCUSResponse");

	    if ( ! success ) {
		done.setSuccessful(false);
		done.setErrorNum(errorCode);
		done.setErrorString("AGFOCUS_DONE: "+errorString);
		return done;
	    }

	    done.setSuccessful(true);
	    done.setErrorNum(OK);
	    done.setErrorString("");
		
	    return done;
	}
	
	if (command instanceof AGCENTROID) { // use AGCENTROID_DONE.
	    AGCENTROID_DONE done = new AGCENTROID_DONE(((COMMAND)command).getId()+"-ProxyAGCENTROIDResponse");

	    if ( ! success)  {
		done.setSuccessful(false);
		done.setErrorNum(errorCode);
		done.setErrorString("AGCENTROID_DONE: "+errorString);
		return done;
	    }
	    
	    CommandParser parser = new CommandParser("&");
	    try {
		parser.parse((String)data, "enable", "disable");
	    } catch (ParseException pe) {
		done.setSuccessful(false);
		done.setErrorNum(CIL_RESPONSE_PARSE_ERROR);
		done.setErrorString("Error parsing AGCENTROID_DONE data: "+pe);
		return done;
	    }
	    
	    Properties map = parser.getMap();
	    double pixelX = 0.0;
	    double pixelY = 0.0;
	    int    peak   = 0;
	    double fwhm   = 0.0;
	    try {
		pixelX = Double.parseDouble(map.getProperty("pixelX"));
		pixelY = Double.parseDouble(map.getProperty("pixelY"));
		peak   = Integer.parseInt(map.getProperty("integratedCounts"));
		fwhm   = Double.parseDouble(map.getProperty("fwhm"));
	    } catch  (NumberFormatException ne) {
		done.setSuccessful(false);
		done.setErrorNum(CIL_RESPONSE_PARSE_ERROR);
		done.setErrorString("Error parsing AGCENTROID_DONE data: "+ne);
		return done;
	    }
	    
	    done.setSuccessful(true);
	    done.setErrorNum(OK);
	    done.setErrorString("");
	    
	    done.setFwhm(fwhm);
	    done.setPeak(peak);
	    done.setXPixel(pixelX);
	    done.setYPixel(pixelY);
	    
	    return done;
	    
	}

	if (command instanceof CALIBRATE) { // use AGFOCUS_DONE.
	    CALIBRATE_DONE done = new CALIBRATE_DONE(((COMMAND)command).getId()+"-ProxyCALIBRATEResponse");
	    
	    if ( ! success ) {
		done.setSuccessful(false);
		done.setErrorNum(errorCode);
		done.setErrorString("CALIBRATE_DONE: "+errorString);
		return done;
	    }
	    
	    CommandParser parser = new CommandParser("&");
	    try {
		parser.parse((String)data, "enable", "disable");
	    } catch (ParseException pe) {
		done.setSuccessful(false);
		done.setErrorNum(CIL_RESPONSE_PARSE_ERROR);
		done.setErrorString("Error parsing CALIBRATE_DONE data: "+pe);
		return done;
	    }
	    
	    Properties map = parser.getMap();
	    double skyRms = 0.0;
	    
	    try {
		skyRms = Double.parseDouble(map.getProperty("skyRms"));
	    } catch  (NumberFormatException ne) {
		done.setSuccessful(false);
		done.setErrorNum(CIL_RESPONSE_PARSE_ERROR);
		done.setErrorString("Error parsing CALIBRATE_DONE data: "+ne);
		return done;
	    }
	    
	    done.setSuccessful(true);
	    done.setErrorNum(OK);
	    done.setErrorString("");
	    
	    done.setRmsError(skyRms);
	   
	    return done;
	    
	}

	// Any other commands.
	COMMAND_DONE done = new COMMAND_DONE(((COMMAND)command).getId()+"-ProxyResponse");
	
	done.setSuccessful(success);
	done.setErrorNum(errorCode);
	done.setErrorString(errorString);
	return done;
    }
    
}

/** $Log: LT_Sim_CommandTranslatorFactory.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.6  2002/09/16 09:38:28  snf
/** *** empty log message ***
/** */
