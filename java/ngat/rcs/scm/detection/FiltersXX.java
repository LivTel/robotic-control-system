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
package ngat.rcs.scm.detection;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.logging.*;

import java.util.*;
import java.io.*;

/** Holds information about all the configured Filters.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: FiltersXX.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/FiltersXX.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class FiltersXX {
     
    // Filters Configuration PLUGIN.

    public static final String CLASS = "FiltersXX";

    /** Store the Filters, indexed by name.*/
    protected static Map filterMap = new HashMap();
   
    /** Logger.*/
    protected static Logger logger = LogManager.getLogger("FILTERS");
    
    public static void main(String args[]) {
	try {
	    new SensorsXX();
		SensorsXX.configure(new File(args[0]));
	    new FiltersXX();
		FiltersXX.configure(new File(args[1]));
	} catch (Exception e) {
	    System.err.println("Error running FiltersXX: "+e);
	    return;
	}
    }
    
    /** Configure the Reactive Control Subsystem: Filters.
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
	
	// Filter collection phase.
	// Format: FILTER <name> READS <sensor> [extra-info]
	//
	//   e.g. (1) FILTER F_WIND_SPEED READS SS_WIND_SPEED AVER 5
	//   e.g. (2) FILTER F_AZ_STATE READS SS_AZ_STATE MODAL 5 NODE_STATES NODE_UNKNOWN
	//
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("FILTER")) {
		int tc = tokenz.countTokens();
		if ( tc > 4) {
		    tokenz.nextToken();	// Filter.
		    String fname = tokenz.nextToken();	
		    tokenz.nextToken(); // reads.		
		    String sname = tokenz.nextToken();	
		    String type  = tokenz.nextToken();	
		    logger.log(1, CLASS, "-", "configure",
			       "Create: Filter: "+fname+" Reads sensor: "+sname);

		    // Locate the Sensor to filter.
		    Sensor sensor = SensorsXX.getSensor(sname);
		    if (sensor == null)
			throw new 
			    IllegalArgumentException("Filters::config: Filter: "+fname+" No such Sensor: "+sname);
		    
		    Filter filter = null;
		    if (type.equals("AVER")) {
			if (tc == 6) {			   
			    filter = processAvFilter(fname, sensor, tokenz);			   
			}
		    } else if
			(type.equals("WAV")) {
			if (tc >= 6)  {			
			    filter = processWeightAvFilter(fname, sensor, tokenz); 			   
			}
		    } else if
			(type.equals("EXPAV")) {
			if (tc >= 6) {
			    filter = processExpAvFilter(fname, sensor, tokenz);			
			}
		    } else if
			(type.equals("MODAL")) {
			if (tc == 8) {
			    filter = processModalFilter(fname, sensor, tokenz);			  
			}
		    } else if
			(type.equals("STEADY")) {
			if (tc == 8){
			    filter = processSteadyStateFilter(fname, sensor, tokenz);
			}
		    } else if
			(type.equals("PERIOD")) {
			if (tc == 8){
			    filter = processTimedSteadyStateFilter(fname, sensor, tokenz);
			}
		    } else if
			(type.equals("TIME")) {
			if (tc == 5){
			    filter = processTemporalFilter(fname, sensor, tokenz);
			}	
		    } else if
			(type.equals("WMS")) {
			if (tc == 6){
			    filter = processWmsFilter(fname, sensor, tokenz);
			}		
		
		    } else if 
			(type.equals("WMSBAD")) {
			if (tc == 6) {
			    filter = processWmsBadFilter(fname, sensor, tokenz);
			}
		    } else
			System.err.println("Unknown type: "+type);
		    
		    if (filter != null) 
			filterMap.put(fname, filter); 
		}
	    }
	}


	// Multiplexer collection phase.
	// Format: MUX <name> COMBINE [<filter> <weight> ]*
	//
	//   e.g. (1) MUX M_SEEING combines F_CCD_SEEING 0.3 F_AG_SEEING 0.6
	//
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    
	    if (line.startsWith("MUX")) {
		
		int tc = tokenz.countTokens();
		System.err.println("MUX line:"+line+" with "+tc+" tokens");

		if (tc >= 5) {
		    
		    System.err.println("MUX create");

		    tokenz.nextToken();	// MUX
		    String mname = tokenz.nextToken();	
		    tokenz.nextToken(); // combines.
		    
		    Multiplexer mux = new Multiplexer(mname);
		    logger.log(1, CLASS, "-", "configure",
			       "Create: Mux: "+mname);

		    // Must be even no of tokens left
		    if (((tc-3) % 2) != 0) 
				throw new IllegalArgumentException("Filters::config: Mux: "+mname+" Odd number of tokens");
		    
		    int nf = (tc-3)/2;
		    
		    for (int i = 0; i < nf; i++) {
			
			String fname = tokenz.nextToken();	
			String swgt  = tokenz.nextToken();	
			
			Filter f = (Filter)filterMap.get(fname);
			
			if (f == null)
			    throw new IllegalArgumentException("Filters::config: Mux: "+mname+" Unknown filter: "+fname);
			
			try {
			    double w = Double.parseDouble(swgt);
			    
			    mux.addElement(w, f);
			    
			    logger.log(1, CLASS, "-", "configure",
				       "Mux: "+mname+" Combine filter: "+fname+" with weighting: "+w);	
			    
			    
			} catch (NumberFormatException nx) {
			    throw new IllegalArgumentException("Filters::config: Error parsing weight: "+swgt);
			    
			}
			
		    }
		    
		    filterMap.put(mname, mux); 
		    
		} else {
		    throw new IllegalArgumentException("Filters::config: Mux: Missing tokens");
		}
	    }
	}
    }
    
    /** Create an Averaging Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an AveragingFilter.
     */
    protected static Filter processAvFilter(String name, Sensor sensor, StringTokenizer tokenz) 
      	throws IllegalArgumentException {      	
      	try {
      	    int nSamp = Integer.parseInt(tokenz.nextToken());      	  
      	    Filter f = new AveragingFilter(sensor, nSamp);
      	    f.setName(name); 
	    logger.log(1, CLASS, "-", "configure",
		       "Created AV Filter: "+name+" On: "+sensor.getName()+" Using: "+nSamp+" readings");
	    return f;
    	} catch (NumberFormatException nx) {
      	    throw new IllegalArgumentException("Filters::config: Error parsing AV Filter: "+nx);
      	}
    }
    
    /** Create a throuigh-pass temporal Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an TemporalFilter.
     */
    protected static Filter processTemporalFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	

	Filter f = new TemporalFilter(sensor);
	f.setName(name); 
	logger.log(1, CLASS, "-", "configure",
		   "Created TIME Filter: "+name+" On: "+sensor.getName());
	return f;
    }
    
    /** Create a WMS state Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @return a WmsFilter.
     */
    protected static Filter processWmsFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	

	try {
	    long gtime = Long.parseLong(tokenz.nextToken());
	    Filter f = new WmsFilter(sensor, name, gtime);
	    logger.log(1, CLASS, "-", "configure",
		       "Created WMS Filter: "+name+" On: "+sensor.getName()+" with conf.time: "+gtime);
	    return f;
    	} catch (NumberFormatException nx) {
      	    throw new IllegalArgumentException("Filters::config: Error parsing WMSGD Filter: "+nx);
      	}
    }

    /** Create a WMS bad-state Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @return a WmsFilter.
     */
    protected static Filter processWmsBadFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	

	try {
	    int bad = Integer.parseInt(tokenz.nextToken());
	    Filter f = new WmsFilter(sensor, name, bad);
	    logger.log(1, CLASS, "-", "configure",
		       "Created WMS BAD Filter: "+name+" On: "+sensor.getName()+" with bad.count: "+bad);
	    return f;
    	} catch (NumberFormatException nx) {
      	    throw new IllegalArgumentException("Filters::config: Error parsing WMSBAD Filter: "+nx);
      	}
    }



    
    /** Create an WeightedAveraging Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an WeightedAveragingFilter.
     */
    protected static Filter processWeightAvFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	
	int tc = tokenz.countTokens();
	double wgts[] = new double[tc];
	double w = 0.0;
	for (int i = 0; i < tc; i++) {
	    try {
		w = Double.parseDouble(tokenz.nextToken());
		wgts[i] = w;		
	    } catch (NumberFormatException nx) {
		throw new IllegalArgumentException("Filters::config: Error parsing WAV Filter: "+nx);
	    }  
	}
	Filter f = new WeightedAveragingFilter(sensor, wgts);
	f.setName(name); 
	logger.log(1, CLASS, "-", "configure",
		   "Created WAV Filter: "+name+" On: "+sensor.getName()+" Using wgts: {"+wgts+"}");
	return f;
    }


    /** Create an ExponentialWeightedAveraging Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an WeightedAveragingFilter.
     */
    protected static Filter processExpAvFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	
	try {
      	    int    nSamp     = Integer.parseInt(tokenz.nextToken());
	    double timeConst = Double.parseDouble(tokenz.nextToken());  
      	    Filter f = new ExponentialWeightedAveragingFilter(sensor, nSamp, timeConst);
      	    f.setName(name); 
	    logger.log(1, CLASS, "-", "configure",
		       "Created AV Filter: "+name+" On: "+sensor.getName()+
		       " Using: "+nSamp+" readings"+
		       " Time-Const: "+timeConst+" millis.");
	    return f;
    	} catch (NumberFormatException nx) {
      	    throw new IllegalArgumentException("Filters::config: Error parsing EWAV Filter: "+nx);
      	}
    }

    /** Create a Modal Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an ModalFilter.
     */
    protected static Filter processModalFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	
	try {
	    int    nSamp = Integer.parseInt(tokenz.nextToken());
	    String list  = tokenz.nextToken();
	    String def   = tokenz.nextToken();
	   
	    Map map = SensorsXX.getList(list);
	    if (map == null)
		throw new IllegalArgumentException("Filters::config: No such Map: "+list);
	    Integer var = (Integer)map.get(def);
	    if (var == null)
		throw new IllegalArgumentException("Filters::config: No such var: "+def+" in "+list);
	    Filter f = new ModalFilter(sensor, nSamp, map.values(), var.intValue());
	    f.setName(name); 
	    logger.log(1, CLASS, "-", "configure",
		       "Created Modal Filter: "+name+
		       " Using: "+nSamp+" readings"+
		       " Based on: "+list+"("+map+")"+
		       " Default: "+def+" ("+var+")"); 
	    return f;
	} catch (NumberFormatException nx) {
	    throw new IllegalArgumentException("Filters::config: Error parsing Modal Filter n-readings:");
	}	
    }


    /** Create a SteadyState Filter.
     * @parm name The name of the Filter.
     * @parm sensor The Sensor to obtain readings from.
     * @parm tokenz The remaining parser tokens.
     * @return an ModalFilter.
     */
    protected static Filter processSteadyStateFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	
	try {
	    int    nSamp = Integer.parseInt(tokenz.nextToken());
	    String list  = tokenz.nextToken();
	    String def   = tokenz.nextToken();
	   
	    Map map = SensorsXX.getList(list);
	    if (map == null)
		throw new IllegalArgumentException("Filters::config: No such Map: "+list);
	    Integer var = (Integer)map.get(def);
	    if (var == null)
		throw new IllegalArgumentException("Filters::config: No such var: "+def+" in "+list);
	    Filter f = new SteadyStateFilter(sensor, nSamp, map.values(), var.intValue());
	    f.setName(name); 
	    logger.log(1, CLASS, "-", "configure",
		       "Created SteadyState Filter: "+name+
		       " Using: "+nSamp+" readings"+
		       " Based on: "+list+"("+map+")"+
		       " Default: "+def+" ("+var+")"); 
	    return f;
	} catch (NumberFormatException nx) {
	    throw new IllegalArgumentException("Filters::config: Error parsing SteadyState Filter n-readings:");
	}	
    }

    /** Create a TimedSteadyState Filter.
     * @param name The name of the Filter.
     * @param sensor The Sensor to obtain readings from.    
     * @param tokenz The remaining parser tokens.
     * @return an ModalFilter.
     */
    protected static Filter processTimedSteadyStateFilter(String name, Sensor sensor, StringTokenizer tokenz) 
	throws IllegalArgumentException {	
	try {
	    long   period = Long.parseLong(tokenz.nextToken());
	    String list   = tokenz.nextToken();
	    String def    = tokenz.nextToken();
	   
	    Map map = SensorsXX.getList(list);
	    if (map == null)
		throw new IllegalArgumentException("Filters::config: No such Map: "+list);
	    Integer var = (Integer)map.get(def);
	    if (var == null)
		throw new IllegalArgumentException("Filters::config: No such var: "+def+" in "+list);
	    Filter f = new TimedSteadyStateFilter(sensor, period, map.values(), var.intValue());
	    f.setName(name); 
	    logger.log(1, CLASS, "-", "configure",
		       "Created PeriodSteadyState Filter: "+name+
		       " Using: "+period+" holding period"+
		       " Based on: "+list+"("+map+")"+
		       " Default: "+def+" ("+var+")"); 
	    return f;
	} catch (NumberFormatException nx) {
	    throw new IllegalArgumentException("Filters::config: Error parsing TimedSteadyState Filter period:");
	}	
    }
    

    /** Returns a Filter, identified by name or null if not found.*/
    public static Filter getFilter(String name) {
	if (filterMap.containsKey(name))
	    return (Filter)filterMap.get(name);
	return null;
    }
    
    }
    



