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

/**
 * A leaf Task for performing an Telescope Offset. The observation passed in is
 * checked and an appropriate EXPOSE command subclass is generated and sent to
 * the relevant instrument control system.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Offset_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Offset_Task.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Offset_Task extends Default_TaskImpl {

	public static final String CLASS = "Offset_Task";

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** The Offset in RA (rads). */
	protected double deltaRA;

	/** The Offset in Dec (rads). */
	protected double deltaDec;

	/**
	 * Create an Exposure_Task using the supplied Observation and settings. Sets
	 * the Instrument and creates a Connection to its ControlSystem. If the
	 * subsystem resource (ControlSystem) cannot be found ???.
	 * 
	 * @param deltaRA
	 *            The Offset in RA (rads).
	 * @param deltaDec
	 *            The Offset in Dec (rads).
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public Offset_Task(String name, TaskManager manager, double deltaRA, double deltaDec) {
		super(name, manager, "CIL_PROXY");
		this.deltaRA = deltaRA;
		this.deltaDec = deltaDec;

		// -------------------------------
		// Set up the appropriate COMMAND.
		// -------------------------------

		OFFBY offby = new OFFBY(name);
		offby.setMode(OFFBY.ARC);
		offby.setOffsetRA(deltaRA);
		offby.setOffsetDec(deltaDec);

		command = offby;
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();
		logger.log(1, CLASS, name, "onInit", "Starting temporary position offset:" + " d-RA: "
				+ (3600.0 * Math.toDegrees(deltaRA)) + " asec, " + " d-Dec: " + (3600.0 * Math.toDegrees(deltaDec)));

	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		// Set the temp offsets.
		ISS.setUserOffsets(deltaRA, deltaDec);
		logger.log(1, CLASS, name, "onCompletion", "Completed temporary position offset - FITS Headers updated.");

	}

}
