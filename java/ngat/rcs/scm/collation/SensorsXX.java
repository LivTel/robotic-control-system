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
package ngat.rcs.scm.collation;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.logging.*;

import java.util.*;
import java.io.*;

/** Holds information about all the configured Sensors.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: SensorsXX.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/SensorsXX.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class SensorsXX {
    
    // Sensors Configuration PLUGIN.


    public static final String CLASS = "SensorsXX";
    
    /** Stores the Sensors, indexed by name.*/
    protected static Map sensorMap = new HashMap();

    /** Stores the lists of discrete variables , indexed by name.*/
    protected static Map varMap    = new HashMap();
    
    /** Logger.*/
    protected static Logger logger = LogManager.getLogger("SENSORS");
    
    public static void main(String args[]) {
	try {
	    new SensorsXX();
		SensorsXX.configure(new File(args[0]));
	} catch (Exception e) {
	    System.err.println("Error running SensorsXX: "+e);
	    return;
	}
    }
    
    /** Configure the Reactive Control Subsystem: Sensors.
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any syntax problems.
     */
    public static void configure(File file) throws IOException, IllegalArgumentException {
	BufferedReader in = new BufferedReader(new FileReader(file));
	
	// Scanning phase.
	String line = null;
	Vector lineList = new Vector();
	while ((line = in.readLine()) != null) {
	    //System.err.println("Read:"+line);
	    line = line.trim();
	    // Skip blank or comment lines.
	    if ((line != "") && 
		!(line.startsWith("#")))
		lineList.add(line);
	}
	
	// Variable List definition phase.
	// Format: LIST name
	// 
	// e.g. LIST NODE_STATES 
	//      LIST RAIN_STATES
	//      LIST SW_STATES
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("LIST")) {
		if (tokenz.countTokens() == 2) {
		    tokenz.nextToken();		
		    String name = tokenz.nextToken();
		    Map listMap = new HashMap();
		    varMap.put(name, listMap);
		    logger.log(1, CLASS, "-", "configure",
			       "Storing a Discrete-State-Map (DSM): "+name);
		}
	    }
	}
	
	// List population phase.
	// Format: <listName> <discrete-state-name> <value>
	//
	// e.g. (1) NODE_STATES STATE_OKAY 217
	// e.g. (2) NODE_STATES STATE_WARN 220	
	Iterator vars = varMap.keySet().iterator();
	while (vars.hasNext()) {
	    String listName = (String)vars.next();
	    tokenz = null;
	    lines  = lineList.iterator();
	    while (lines.hasNext()) {
		line   = (String)lines.next();
		tokenz = new StringTokenizer(line);
		if (line.startsWith(listName)) {
		    if (tokenz.countTokens() == 3) {
			Map map = (Map)varMap.get(listName);
			if (map != null) {
			    tokenz.nextToken();
			    String varName = tokenz.nextToken();
			    try {
				int varValue = Integer.parseInt(tokenz.nextToken());
				map.put(varName, new Integer(varValue));
				logger.log(1, CLASS, "-", "configure",
					   "Storing a DSM Variable: Map: "+listName+
					   " Var: "+varName+" = "+varValue);
			    } catch (NumberFormatException nx) {
				throw new IllegalArgumentException("Sensors::Config: List: ["+listName+
								   "] Error parsing var: ["+varName+"] : "+nx+
								   " Line: {"+line+"}");   	    
			    }
			}
		    }
		}
	    }
	}
	
	// Sensor collection phase.
	// Format: SENSOR  <name> { I | D | T } <cat> <key> 
	//
	//   e.g. (1) SENSOR  SS_RAIN           I   METEO    rain.state
	//   e.g. (2) SENSOR  IS_RAT_DWR_TEMP   D   RATCAM   dewar.temperature
	//
	
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    int tc = tokenz.countTokens();

	    System.err.println("SensorsXX: Scan line: ["+line+"] with "+tc+" tokens");

	    if (line.startsWith("SENSOR")) {
		if (tc == 5) {
		    tokenz.nextToken();		  
		    String name = tokenz.nextToken();
		    String type = tokenz.nextToken();
		    String cat  = tokenz.nextToken();
		    String key  = tokenz.nextToken();
		    int t = 0;
		    if (type.equals("D")) 
			t = PoolLookupSensor.CONTINUOUS_READOUT;		
		    else if 
			(type.equals("I")) 
			t = PoolLookupSensor.DISCRETE_READOUT;		   		
		    else
			throw new IllegalArgumentException("Sensors::Config: Illegal sensor type: ["+type+"]");
		    
		    // Replace UNDERSCORE with SPACE for dodgy CCS keywords.
		    if (key.indexOf("_") != -1)
			key = key.replace('_', ' ');
		    
		    logger.log(1, CLASS, "-", "configure",
			       "Creating Sensor: "+name+
			       " Looks in: ["+cat+
			       "] For Key: ("+key+")");				      
		    Sensor sensor = new PoolLookupSensor(name, cat, key, t);
		    sensorMap.put(name, sensor);			   	  
		} else if
		    (tc == 4) {
		    tokenz.nextToken();		  
		    String name = tokenz.nextToken();
		    String type = tokenz.nextToken();
		    String cat  = tokenz.nextToken();

		    System.err.println("SensorsXX: tc4 type: "+type);

		    int t = 0;
		    if (type.equals("T"))		
			t = PoolLookupSensor.TIMESTAMP_READOUT;	
		    else if
			(type.equals("Y"))
			t = PoolLookupSensor.TIMEDIFF_READOUT;	
		    else		    
		    	throw new IllegalArgumentException("Sensors::Config: Only 4 Tokens but not a Timestamp");	
		    
		    logger.log(1, CLASS, "-", "configure",
			       "Creating Sensor: "+name+
			       " Looks in: ["+cat+
			       "] For Timestamp");	
		    Sensor sensor = new PoolLookupSensor(name, cat, null, t);
		    sensorMap.put(name, sensor);
		    
		}
	    }	    
	}	
    }
    
    /** Returns a DSM.*/
    public static Map getList(String name) {
	if (varMap.containsKey(name))
	    return (Map)varMap.get(name);
	return null;
    }
    
    /** Returns a Sensor, identified by name or null if not found.*/
    public static Sensor getSensor(String name) {
	if (sensorMap.containsKey(name))
	    return (Sensor)sensorMap.get(name);
	return null;
    }

}

/** $Log: SensorsXX.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */
