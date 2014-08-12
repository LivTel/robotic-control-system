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
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;

/** A leaf Task for performing an Instrument command ABORT. 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Abort_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Abort_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Abort_Task extends Default_TaskImpl {
    
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** Constant denoting the time (millis) to pause the Instrument's SMM Monitor
     * before the abort is called to avoid aborting a GET_STATUS command rather than
     * the actual command. (We NEVER bother to abort a GET_STATUS command).
     */
     public static final long DEFAULT_STATUS_MONITOR_PAUSE_INTERVAL = 10000L;

    /** Constant denoting the time (millis) we expect a GET_STATUS command to take
     * to get a response from its ICS.
     */
    public static final long DEFAULT_STATUS_MONITOR_REPLY_INTERVAL = 3000L;

    /** Name of the Instrument.*/
    protected String instId;

    /** Create an Abort_Task using the supplied Observation and settings.
     * Sends an ABORT to the instrument's ControlSystem.
     * @param instId The name of the instrument subsystem.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public Abort_Task(String      name,
		      TaskManager manager,
		      String      instId) {
	super(name, manager, instId);
	this.instId = instId;

    }
    
    /** Compute the estimated completion time. We will be waiting for the
     * Instrument Status Monitor to clear any reply before we try to send.
     * @return The initial estimated completion time in millis.*/
    @Override
	protected long calculateTimeToComplete() {
	return getDefaultTimeToComplete() + DEFAULT_STATUS_MONITOR_REPLY_INTERVAL;
    }
    
    /** This task can NOT be aborted when it is running - let it complete.*/
    @Override
	public boolean canAbort() { return false; }
        
    /** Carry out subclass specific initialization.
     * Try to pause the SMM's status monitor for this Instrument.
     * Wait for a short period to ensure a currently executing GET_STATUS
     * command has time to complete before executing the ABORT.
     */
    @Override
	protected void onInit() {	
	super.onInit();
	//CCS_StatusMonitorThread sm = RCS_Controller.getCCSStatusMonitor(instId);
	//if (sm != null) {
	 //   sm.linger(DEFAULT_STATUS_MONITOR_PAUSE_INTERVAL);	  
	  //  setDelay(DEFAULT_STATUS_MONITOR_REPLY_INTERVAL);
	//}
	
	logger.log(1, CLASS, name, "onInit",
		   "Starting ICS abort for: "+instId);
	
	// here we decide what sort of abort to send..
	
	if (instId.equals("FRODO_RED")) {
		 FRODOSPEC_ABORT fredAbort = new FRODOSPEC_ABORT("ABORT_RED");
		 fredAbort.setArm(FrodoSpecConfig.RED_ARM);		
		 command = fredAbort;
	} else if (instId.equals("FRODO_BLUE")){
		FRODOSPEC_ABORT fblueAbort = new FRODOSPEC_ABORT("ABORT_BLUE");
		 fblueAbort.setArm(FrodoSpecConfig.BLUE_ARM);
		 command = fblueAbort;
	} else {
		ABORT abort = new ABORT("ABORT_"+instId);
		command = abort;
	}


    }
    
    /** Carry out subclass specific disposal work. 
	Try to resume the SMM's status monitor for this Instrument.*/
    @Override
	protected void onDisposal() {
	//if (RCS_Controller.getCCSStatusMonitor(instId) != null)
	 //   RCS_Controller.getCCSStatusMonitor(instId).awaken();
	super.onDisposal();

	logger.log(1, CLASS, name, "onDisposal",
		   "ICS abort acknowledged");
	
    }

}

/** $Log: Abort_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
