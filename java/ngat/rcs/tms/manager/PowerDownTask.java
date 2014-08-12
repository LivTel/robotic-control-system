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
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
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
import ngat.tcm.SciencePayload;
import ngat.net.*;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.instrument.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/**
 * This Task manages the Powering down of subsystems as a result of a THREAT
 * ALERT received by the RCS Controller.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: PowerDownTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/PowerDownTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class PowerDownTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, CONFIG = 1200

	/** Instrument subsystem action - Indicates NO Action to be performed. */
	public static final int INST_NO_ACTION = 606000;

	/** Instrument subsystem action - Indicates ICS to be Re-datumed (L1). */
	public static final int INST_REDATUM = 606001;

	/** Instrument subsystem action - Indicates ICS to be Re-started (L2). */
	public static final int INST_RESTART = 606002;

	/** Instrument subsystem action - Indicates ICS to be Re-booted (L3). */
	public static final int INST_REBOOT = 606003;

	/** Instrument subsystem action - Indicates ICS to be Shutdown (L4). */
	public static final int INST_SHUTDOWN = 606004;

	/** What action to perform on the instrument subsystems. */
	protected int instrumentAction;

	/** String describing instrument subsystem action. */
	protected String instrumentModeStr;

	/** What action to perform on the RCS. */
	protected int shutdownAction;

	/** String describing instrument subsystem action. */
	protected String shutdownModeStr;

	/** Whether to callout operators. */
	protected boolean callout;

	/** Message for operators. */
	protected String calloutMessage;

	protected SciencePayload payload;

	protected InstrumentRegistry ireg;

	/**
	 * Create a PowerDownTask using the supplied settings.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	public PowerDownTask(String name, TaskManager manager) {
		super(name, manager);
	}

	/** This task cannot be aborted it is the final activity of the RCS. */
	@Override
	public void onSubTaskAborted(Task task) {
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(1, CLASS, name, "onInit", "Starting powerdown: RCS: " + shutdownModeStr + " Inst: "
				+ instrumentModeStr);
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	public void onCompletion() {
		super.onCompletion();
		taskLog.log(1, CLASS, name, "onCompletion", "Powerdown completed");
	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		try {
			payload = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload();
		} catch (Exception e) {
			e.printStackTrace();
			failed(641201, "Unable to locate science payload");
			return null;
		}

		try {
			ireg = RCS_Controller.controller.getInstrumentRegistry();
		} catch (Exception e) {
			e.printStackTrace();
			failed(641202, "Unable to locate instrument registry");
			return null;
		}

		int instrumentLevel = 0; // ####defunct
		instrumentModeStr = "NOOP";

		switch (instrumentAction) {
		case INST_REDATUM:
			instrumentModeStr = "REDATUM";
			instrumentLevel = 1;
			break;
		case INST_RESTART:
			instrumentModeStr = "RESTART";
			instrumentLevel = 2;
			break;
		case INST_REBOOT:
			instrumentModeStr = "REBOOT";
			instrumentLevel = 3;
			break;
		case INST_SHUTDOWN:
			instrumentModeStr = "SHUTDOWN";
			instrumentLevel = 4;
			break;
		}

		int shutdownLevel = 0;
		//
		shutdownModeStr = "NOOP";

		switch (shutdownAction) {
		case RCS_Controller.RESTART_ENGINEERING:
			shutdownModeStr = "RESTART_ENG";
			shutdownLevel = 1;
			break;
		case RCS_Controller.RESTART_ROBOTIC:
			shutdownModeStr = "RESTART_AUTO";
			shutdownLevel = 2;
			break;
		case RCS_Controller.HALT:
			shutdownModeStr = "HALT";
			shutdownLevel = 3;
			break;
		case RCS_Controller.REBOOT:
			shutdownModeStr = "REBOOT";
			shutdownLevel = 4;
			break;
		case RCS_Controller.SHUTDOWN:
			shutdownModeStr = "SHUTDOWN";
			shutdownLevel = 5;
			break;
		}

		// Only do if the instrument action is NOT NO_ACTION
		if (instrumentAction != INST_NO_ACTION) {

			List instList = null;
			try {
				instList = ireg.listInstruments();

				Iterator it = instList.iterator();
				while (it.hasNext()) {
					InstrumentDescriptor iid = (InstrumentDescriptor) it.next();
					if (iid != null) {
						int rbLevel = payload.getRebootLevelForInstrument(iid);
						taskList.addTask(new Reboot_Task(name + "/" + instrumentModeStr + "_" + iid.getInstrumentName()
								+ "_L" + rbLevel, this, iid.getInstrumentName(), rbLevel));

						// e.g. X_SHUTDOWN/REBOOT_RATCAM_L3
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return taskList;
	}

	/** Sets the action to be performed on Instrument Subsystems. */
	public void setInstrumentAction(int action) {
		this.instrumentAction = action;
	}

	/** Sets the action to be performed on the RCS. */
	public void setShutdownAction(int action) {
		this.shutdownAction = action;
	}

	/**
	 * Sets whether to callout operator.
	 * 
	 * @param callout
	 *            Sets True if an operator callout is to be performed.
	 * @param message
	 *            Holds the message to send to operators. (can be null).
	 */
	public void setCallout(boolean callout, String message) {
		this.callout = callout;
		this.calloutMessage = message;
	}

}

/**
 * $Log: PowerDownTask.java,v $ /** Revision 1.1 2006/12/12 08:28:54 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:33:38 snf /** Initial
 * revision /** /** Revision 1.1 2002/09/16 09:38:28 snf /** Initial revision
 * /** /** Revision 1.1 2001/04/27 17:14:32 snf /** Initial revision /**
 */
