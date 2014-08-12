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

/** A leaf Task for performing an Instrument Control System reboot. The configpassed
 * in is checked and an appropriate REBOOT command subclass is generated and
 * sent to the relevant instrument control system.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Reboot_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Reboot_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Reboot_Task extends Default_TaskImpl {
      
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 120000L;
    
    /** The REBOOT level chosen from the following:-
     * 1  = REDATUM   upto 1 minute.
     * 2  = SOFTWARE  upto 1 minute.
     * 3  = HARDWARE  upto 4 minutes.
     * 4  = POWER_OFF upto 2 minutes. */
    protected int level;

    /** Instrument name.*/
    protected String instId;
    
    /** Create a Reboot_Task using the supplied Instrument and settings.
     * Creates a Connection to the Instrument ControlSystem and send a REBOOT.
     * If the subsystem resource (ControlSystem) cannot be found ???.
     * @param instId The name of the Instrument CS to be rebooted. 
     * @param level The REBOOT level to use.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public Reboot_Task(String      name,
		       TaskManager manager,
		       String      instId,
		       int         level) {
	super(name, manager, instId);
	this.instId = instId;
	this.level  = level;

	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	
	REBOOT reboot = new REBOOT(name);
	reboot.setLevel(level);
	
	command = reboot;

    }

    @Override
	protected void onInit() {
	super.onInit();
	
	logger.log(1, CLASS, name, "onInit", 
		   "Starting reboot of ICS:"+instId+" at level "+level);
    }

    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	
	logger.log(1, CLASS, name, "onCompletion", 
		   "ICS reboot acknowledged for "+instId);
    }


    /** ####Temp override to display its name.####*/
    @Override
	public String getName() {
	return super.getName()+":L"+level;
    }

    public int getLevel() { return level; }

    public void setLevel(int level) { this.level = level; }

}

/** $Log: Reboot_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/20 09:35:57  snf
/** Temp modified getName()
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
