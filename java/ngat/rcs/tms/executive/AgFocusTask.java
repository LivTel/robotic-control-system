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
 * A leaf Task for performing an Telescope AGFOCUS operation.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AgFocusTask.java,v 1.1 2007/12/03 14:28:58 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/AgFocusTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class AgFocusTask extends Default_TaskImpl {
	
	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** The AG focus position (mm). */
	protected double focus;

	/**
	 * Create an AgFocusTask using the supplied focus position.
	 * 
	 * @param focus
	 *            The AG focus position (mm).
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	public AgFocusTask(String name, TaskManager manager, double focus) {
		super(name, manager, "CIL_PROXY");
		this.focus = focus;

	}

	/** Carry out subclass specific initialization. */
	@Override
	public void onInit() {
		super.onInit();
		// Set up the appropriate COMMAND.
		AGFOCUS agfocus = new AGFOCUS(name);
		agfocus.setFocus(focus);

		command = agfocus;

		logger.log(1, CLASS, name, "onInit", "Starting AG Focus move to position: " + focus + " mm.");

	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	public void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);

		logger.log(1, CLASS, name, "onCompletion", "Completed AG Focus move");
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	public void setFocus(double focus) {
		this.focus = focus;
	}

	public double getFocus() {
		return focus;
	}

}
