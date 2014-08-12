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
package ngat.rcs.calib;

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
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.phase2.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import java.io.*;
import java.util.*;
import java.text.*;
/** Holds information relating to Standards. There are currently two models
 * in use depending on an Instrument specific configuration option.
 *
 * The <b>photometric</b> model used works as follows:
 * Initially, 2 sources designated leading and trailing are selected from those
 * available (configuration option) such that the leading source is close to
 * the zenith and the trailing source is at low elevation and East (rising).
 * These 2 stars are followed during the night such that they are both observed
 * during any standards operations. Should the leading star set (below the dome
 * imposed horizon) then the trailing star is made into the new lead star and
 * another trailing star is selected near the Eastern horizon.
 *
 * The <b>spectrometric</b> model used works as follows:
 * The highest available field is used.
 *
 * Should the model change then the static accessor methods provided may have 
 * to change or optional settings might be used to indicate different models.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Standards.java,v 1.1 2006/12/12 08:25:56 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/calib/RCS/Standards.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Standards implements Logging {
    
  

    /** Stores the set of Standard sources (regions).*/
    protected FieldSet standardFields;

    /** Stores the set of Blank sources (regions).*/
    protected FieldSet blankFields;

    /** Stores the sets of instrument based Standards instances (one per inst).*/
    protected static Map standardSets;

    /** Default Logger.*/
    protected static Logger logger;
    
    protected Standards() {
	blankFields    = new FieldSet();
	standardFields = new FieldSet();
	if (logger == null)
	    logger = LogManager.getLogger("STANDARDS");
    }


    /** Initialize the Standards Registry.*/
    public static void initialize()  { 	
	standardSets = Collections.synchronizedMap(new TreeMap());		
    }
 
    /** Add Standards for an Instrument.
     * @param instId The ID of the Instrument.
     * @return The newly created (empty) Standards for the Instrument.
     */
    public static Standards addStandards(String instId) {
	Standards instStandards = new Standards();
	standardSets.put(instId, instStandards);
	return instStandards;
    }

    /** Return the Standards for the named Instrument.*/
    public static Standards getStandards(String instId) {
	return (Standards)standardSets.get(instId);
    }

    /** Set the default Logger.*/
    public static void setLogger(String logName) {
	logger = LogManager.getLogger(logName);
    }

    /** Returns the set of Blank fields.*/
    public FieldSet getBlankFields() { return blankFields; }

    /** Returns the set of Standard fields.*/
    public FieldSet getStandardFields() { return standardFields; }
       
    /** Holds information about a collection of Standard fields.*/
    public static class FieldSet {

	/** The list of Fields.*/
	protected Vector fields;
	
	/** Counts the number of Standards taken so far (this session).?????*/
	protected int standardCount;
	
	/** Create a new FieldSet.*/
	FieldSet() {
	    fields = new Vector();	
	    standardCount = 0;
	}
	
	/** The current Source to use for the initially High elevation standard.*/
	protected Standard currentLeadingField;
	
	/** The current Source to use for the initially Low elevation, East azimuth standard.*/
	protected Standard currentTrailingField;
	
	/** The current Source which is highest - this is ONLY set by pickHighestSource.*/
	protected Standard currentHighestField;
	
	/** Returns the current Standard to use for the leading standard field.*/
	protected Standard getCurrentLeadingField() { 	
	    return currentLeadingField; 
	}
	
	/** Returns the current Standard to use for the trailing standard field.*/
	protected Standard getCurrentTrailingField() { 	
	    return currentTrailingField; 
	}
	
	/** Returns the current Standard to use for the highest standard field.*/
	protected Standard getCurrentHighestField() { 	
	    return currentHighestField; 
	}
		
	/** Increments the count of the number of standards taken so far - this session.*/
	public void incrementStandardCount() { standardCount++; }
	
	/** Returns the count of the number of standards taken so far - this session..*/
	public int getStandardCount() { return standardCount; }
    

	/** Configure the FieldSet from a File.
	 * @param file The File containing the standard sources in java.util.Properties format
	 * as follows:-<br>
	 * <dl>
	 * <dt>field.count 
	 * <dd>The number of standard fields to use.
	 * <dt>standards.field.<i>count</i>.ra
	 * <dd>The RA of the <i>count</i><super>th</super> field in hh:mm:ss.SSS format.
	 * <dt>standards.field.<i>count</i>.dec
	 * <dd>The Declination of the <i>count</i><super>th</super> field in ddd:mm:ss.SS format.
	 * <dt>standards.field.<i>count</i>.name
	 * <dd>The name/id of this source object.
	 * </dl>
	 * @exception IOException If any error occurs while loading the config or the file
	 * cannot be found.
	 * @exception IllegalArgumentException If any config parameters are out of range
	 * or cannot be read correctly.
	 */
	public void configure(File file, String type) throws IOException, IllegalArgumentException {
	    ConfigurationProperties config = new ConfigurationProperties();
	    config.load(new FileInputStream(file));
	    int count = config.getIntValue("field.count", -1);
	    if (count == -1)
		throw new IllegalArgumentException("Standards: Illegal or unspecified field count:");
	
	    ExtraSolarSource field = null;
	    
	    // New format uses : 
	    //                  stdID.ID = <id> 
	    //                  <id>.key = <val>  (..for various keys)

	    int is = 0;
	    Enumeration e = config.propertyNames();
	    while (e.hasMoreElements()) { 
		double raField        = 0.0;
		double decField       = 0.0;
		double raFocus        = 0.0;
		double decFocus       = 0.0;
		double pmRA           = 0.0;
		double pmDec          = 0.0;
		double parallax       = 0.0;
		double radialVelocity = 0.0;
		
		float epoch    = 2000.0f;
		float equinox  = 2000.0f;
		char  eqLetter = 'J';
		int   frame    = Source.FK5;

		String key = (String)e.nextElement();
		if (key.endsWith(".ID")) {
		    String id = config.getProperty(key);
				
		    try {
			raField = Position.parseHMS(config.getProperty(id+".field.ra"));
		    } catch (ParseException px) {
			throw new IllegalArgumentException("Parsing standard ("+type+") field: "+id+" ra: "+px);
		    }
		    try {
			decField = Position.parseDMS(config.getProperty(id+".field.dec"));
		    } catch (ParseException px) {
		    throw new IllegalArgumentException("Parsing standard ("+type+") field: "+id+" dec: "+px);
		    }
		    
		    
		    double magField = config.getDoubleValue(id+".field.mag", -1.0);
		    
		    if (magField < 0.0)
			throw new IllegalArgumentException("Parsing standard ("+type+") field: "+id+" illegal magnitude.");
		    // Default to Field ID.
		    String srcName = config.getProperty(id+".field.name", id);
		    		  
		    field = new ExtraSolarSource("Std-("+type+")-["+id+"]");
		    
		    // Names: E.g. Std-(PHOTOM)-Field [BD+2314]
		    
		    field.setRA(raField);
		    field.setDec(decField);
		  
		    // Default settings?
		    field.setPmRA(pmRA);
		    field.setPmDec(pmDec);
		    field.setParallax(parallax);
		    field.setRadialVelocity(radialVelocity);
		    field.setEpoch(epoch);
		    field.setEquinox(equinox);
		    field.setEquinoxLetter(eqLetter);
		    field.setFrame(frame);
		 
		    Standard standard = new Standard(id);
		    standard.setField(field);
		    standard.setMagnitude(magField);
		    standard.setType(type);
		    System.err.println("Entered Standard: ("+type+") #"+is+"\n"+standard.toString());
		    fields.add(standard);
		    is++;
		}
	    }
	    
	}


	/** Chooses the leading and trailing Fields which appear best at this instance.
	 * Sets the references to currentLeadingField and currentTrailingField.
	 * This method and the 2 methods pickLeadingField() and pickTrailingField() are
	 * dependant on the standards selection model.
	 * @param duration The period (millis) during which this Field must remain visible 
	 * above the specified horizon.. 
	 * @param horizon The lowest elevation (rads) at which a Field can be viewed e.g.
	 * the dome limit.
	 */
	public void pickStandardFields(long duration, double horizon) {
	    pickLeadingField  (duration, horizon);
	    pickTrailingField (duration, horizon);
	    pickHighestField  ();
	}
	
	/** Chooses the leading Field which appears best at this instance.
	 * Sets the reference to currentLeadingField.
	 * This method is dependant on the standards selection model.
	 * @param duration The period (millis) during which this Field must remain visible
	 * above the specified horizon. 
	 * @param horizon The lowest elevation (rads) at which a Field can be viewed e.g.
	 * the dome limit.
	 */
	public void pickLeadingField(long duration, double horizon) {
	    if (fields == null) return; // error ??   
	    
	    ExtraSolarSource source  = null;
	    Standard bestField = null;
	    Standard field     = null;
	    Position position  = null;
	    double highestElev = 0.0;
	    double elev   = 0.0;
	    double HA     = 0.0;
	    double uptime = 0.0;
	 
	    Iterator it = fields.iterator();
	    while (it.hasNext()) {
		field    = (Standard)it.next(); 
		source   = field.getField();
		position = source.getPosition();		
		elev     = position.getAltitude();		
		HA       = position.getHA();
		uptime   = position.getUpTimeMillis(horizon);	   
		
		logger.log(3, "Standards", "", "pickLeadingSource", 
			   "Trying Source:  "+source.getName()+
			   "\nElevation:      "+Position.toDMSString(elev)+
			   "\nHour Angle:     "+Position.toHMSString(HA)+
			   "\nTime left above "+Position.toDegrees(horizon, 2)+" horizon: "+
			   (uptime/1000.0)+" seconds."+
			   "\nReq Duration:   "+(duration/1000.0)+" seconds.");
		
		if (elev < horizon) continue;
		// Elev > best so far and uptime > duration.
		if (elev > highestElev && uptime > duration) {
		    bestField   = field;	    
		    highestElev = elev;
		}
	    }
	    
	    if (bestField != null) {
		currentLeadingField = bestField;	
		ExtraSolarSource fieldSource = currentLeadingField.getField();		
		uptime   = fieldSource.getPosition().getUpTimeMillis(horizon);
		logger.log(1, "Standards", "", "pickLeadingField", 
			   "Selected LEAD Field: "+
			   "\n\tTime to set:   "+(uptime/1000.0)+" seconds."+
			   "\n\tField"+ 		 
			   "\n\t\tName:        "+fieldSource.getName()+
			   "\n\t\tMagnitude:   "+currentLeadingField.getMagnitude()+
			   "\n\t\tAzimuth:     "+Position.toDegrees(fieldSource.getPosition().getAzimuth(),2)+
			   "\n\t\tElevation:   "+Position.toDegrees(fieldSource.getPosition().getAltitude(), 2));
	    }	
	}

	/** Chooses the trailing source which appears best at this instance.
	 * Sets the reference to currentTrailingSource.
	 * This method is dependant on the standards selection model.
	 * @param duration The period (millis) during which this source must remain visible
	 * above the specified horizon. 
	 * @param horizon The lowest elevation (rads) at which a source can be viewed e.g.
	 * the dome limit.
	 */
	public void pickTrailingField(long duration, double horizon) {
	    if (fields == null) return; // error ?? 
	    ExtraSolarSource source  = null;
	    Standard bestField = null;
	    Standard field     = null;
	    Position position = null;
	    double lowestElev = Math.PI;
	    double lowMag = 0.0;
	    double elev   = 0.0;
	    double HA     = 0.0;
	    double uptime = 0.0;
	
	    Iterator it = fields.iterator();
	    while (it.hasNext()) {
		field    = (Standard)it.next(); 	  
		source   = field.getField();
		position = source.getPosition();		
		elev     = position.getAltitude();		
		HA       = position.getHA();
		uptime   = position.getUpTimeMillis(horizon);
		
		logger.log(3, "Standards", "", "pickTrailingSource", 
			   "Trying Source:  "+source.getName()+
			   "\nElevation:      "+Position.toDMSString(elev)+
			   "\nHour Angle:     "+Position.toHMSString(HA)+
			   "\nTime left above "+Position.toDegrees(horizon, 2)+" horizon: "+
			   (uptime/1000.0)+" seconds."+
			   "\nReq Duration:   "+(duration/1000.0)+" seconds.");
		
		if (elev < horizon) continue;
		// Elev < best so far and HA between 12H and 24H and uptime > duration.
		if (elev < lowestElev && elev > horizon && uptime > duration && HA > Math.PI && HA < 2*Math.PI ) {
		    bestField  = field;
		    lowestElev = elev;
		}
		
	    }
	    
	    if (bestField != null) {
		currentTrailingField = bestField;	   
		ExtraSolarSource fieldSource = currentTrailingField.getField();	  
		uptime   = fieldSource.getPosition().getUpTimeMillis(horizon);
		logger.log(1, "Standards", "", "pickTrailingField", 
			   "Selected TRAIL Field: "+
			   "\n\tTime to set:   "+(uptime/1000.0)+" seconds."+
			   "\n\tField"+ 		 
			   "\n\t\tName:        "+fieldSource.getName()+
			   "\n\t\tMagnitude:   "+currentTrailingField.getMagnitude()+
			   "\n\t\tAzimuth:     "+Position.toDegrees(fieldSource.getPosition().getAzimuth(),2)+
			   "\n\t\tElevation:   "+Position.toDegrees(fieldSource.getPosition().getAltitude(), 2));	    
	    }
	}
	
	/** Returns the currently highest (Elevation) source
	 * or null if none available or source table is empty.*/
	public void pickHighestField() {
	    if (fields == null) return; // error ??   
	    ExtraSolarSource source  = null;
	    Standard bestField = null;
	    Standard field     = null;
	    Position position = null;
	    double   highMag     = 0.0;
	    double   highestElev = 0.0;
	    double   elev   = 0.0;
	
	    Iterator it = fields.iterator();
	    while (it.hasNext()) {
		field    = (Standard)it.next(); 	  
		source   = field.getField();
		position = source.getPosition();		
		elev     = position.getAltitude();		
		
		// Elev > best so far.
		if (elev > highestElev) {
		    bestField   = field;	    
		    highestElev = elev;
		}
	    }	
	    if (bestField != null) {
		currentHighestField = bestField;
		ExtraSolarSource fieldSource = currentHighestField.getField();	  
		
		logger.log(1, "Standards", "", "pickHighestField", 
			   "Selected HIGH Field: "+			 
			   "\n\tField"+ 		 
			   "\n\t\tName:        "+fieldSource.getName()+
			   "\n\t\tMagnitude:   "+currentHighestField.getMagnitude()+
			   "\n\t\tAzimuth:     "+Position.toDegrees(fieldSource.getPosition().getAzimuth(),2)+
			   "\n\t\tElevation:   "+Position.toDegrees(fieldSource.getPosition().getAltitude(), 2));
	    }
	}
	
    } // [FieldSet].

    /** Holds information about a Standard field or focus star.*/
    public static class Standard {
	
	/** ID of this field.*/
	private String name;

	/** Create a Standard with the spcified Id.*/
	Standard(String name) {
	    this.name = name;
	    this.type = "TYPE-UNKNOWN";
	}

	/** Field RA/Dec.*/
	ExtraSolarSource field;

	/** Star or field magnitude.*/
	double magnitude;

	/** Type of Standards identifier (for logging).*/
	String type = "";
	
	/** Set the field position.*/
	public void setField(ExtraSolarSource field) { this.field = field; }

	/** Return the field position.*/
	public ExtraSolarSource getField() { return field; }

	/** Set the star or field magnitude.*/
	public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

	/** Return the star or field magnitude.*/
	public double getMagnitude() { return magnitude; }
	
	/** Set the type of Standard.*/
	public void setType(String type) { this.type = type; }

	/** Returns a useful string description.*/
	@Override
	public String toString() {
	    return "Standard: "+"("+type+")"+name+
		"\n\tField: "+field.toString()+
		"\n\t Mag:  "+magnitude;
	}
	    
    }

}

/** $Log: Standards.java,v $
/** Revision 1.1  2006/12/12 08:25:56  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:23  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Modified to use FitsHeaderCardImages.
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
