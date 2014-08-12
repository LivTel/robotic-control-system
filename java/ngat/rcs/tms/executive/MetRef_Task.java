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
package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
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
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing a Meteorological refraction setup. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: MetRef_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/MetRef_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class MetRef_Task extends Default_TaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, MET = 500

    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIME = 60000L; 

    public static final int UNKNOWN_REFRACTION_PARAMETER = 640501;

    public static final int PRESSURE_DATA    = 640501;
    
    public static final int HUMIDITY_DATA    = 640502;
    
    public static final int TEMPERATURE_DATA = 640503;
    
    public static final int WAVELENGTH_DATA  = 640504;

    int refdata;
    
    String paramName;

    double value;
    
    /** Create an MetRef_Task using the supplied settings.
     * This is used to pass environment and instrument data to the TCS for use
     * in the refraction corrections to pointing direction.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     * @param refData Specifies the class of data,
     * - one of {PRESSURE_DATA, HUMIDITY_DATA,  TEMPERATURE_DATA, WAVELENGTH_DATA}.
     * @param value The value of the data field to pass across to the TCS.
     */
    public MetRef_Task(String      name,
		       TaskManager manager,
		       int         refdata,
		       double      value) {	
	super(name, manager, "CIL_PROXY");
	this.refdata = refdata;
	this.value   = value;
	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------

	paramName = "UNKNOWN";

	switch (refdata) {
	case PRESSURE_DATA :
	    command = new PRESSURE(name, value);
	    paramName = "PRESSURE";
	    break;
	case HUMIDITY_DATA:
	    command = new HUMIDITY(name, value);
	    paramName = "HUMIDITY";
	    break;
	case TEMPERATURE_DATA:
	    command = new TEMPERATURE(name, value);
	    paramName = "TEMPERATURE";
	    break;
	case WAVELENGTH_DATA:
	    command = new WAVELENGTH(name, value);
	    paramName = "WAVELENGTH";
	    break;
	default:
	    logger.log(1, "MetRef_Task", name, "Constructor", 
			 "Illegal refraction parameter code: "+refdata);
	    failed = true;
	    errorIndicator.setErrorCode(UNKNOWN_REFRACTION_PARAMETER);
	    errorIndicator.setErrorString("Illegal refraction parameter code: "+refdata);
	    errorIndicator.setException(null);
	    return;
	}
		
    }
    
    /** Returns the default time for this command to execute.*/
    public static long getDefaultTimeToComplete() {
	//return RCS_Configuration.getLong("tcs_command.refraction.timeout", DEFAULT_TIMEOUT);
	return DEFAULT_TIMEOUT;
    }

    /** Compute the estimated completion time.
     * @return The initial estimated completion time in millis.*/
    @Override
	protected long calculateTimeToComplete() {
	return getDefaultTimeToComplete();
    }

    @Override
	protected void onInit() {
	super.onInit();
	logger.log(1, CLASS, name, "onInit", 
		   "Starting MetRef "+paramName);
	
    }
    
    /** Carry out subclass specific completion work.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	
	logger.log(1, CLASS, name, "onCompletion", 
		   "Completed MetRef "+paramName);
    }
   
    
    /** Carry out subclass specific disposal work.   ## NONE ##.*/
    @Override
	protected void onDisposal() {
	super.onDisposal();
    }

}

/** $Log: MetRef_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
