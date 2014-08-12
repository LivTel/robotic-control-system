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
import ngat.message.RCS_TCS.*;
import ngat.message.GUI_RCS.*;

/**
 * A leaf Task for performing a Telescope Slew. The source position passed in is
 * checked and an appropriate SLEW command is generated and sent to the
 * telescope control system.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: SlewTask.java,v 1.3 2006/12/13 13:01:38 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/SlewTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.3 $
 */
public class SlewTask extends Default_TaskImpl {

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** The Source whose position is to be slewed to. */
	protected Source source;

	/** Target RA offset. */
	double offsetRA;

	/** Target Dec offset. */
	double offsetDec;

	/** True if we want non-sideal tracking. */
	private boolean nsTracking;

	/**
	 * Create a Slew_Task using the supplied Source position.
	 * 
	 * @param source
	 *            The source whose position is to be slewed to.
	 * @param offsetRA
	 *            The RA offset ().
	 * @param offsetDec
	 *            The Dec offset ().
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public SlewTask(String name, TaskManager manager, Source source, double offsetRA, double offsetDec) {
		super(name, manager, "CIL_PROXY");
		this.source = source;

		// -------------------------------
		// Set up the appropriate COMMAND.
		// -------------------------------
		SLEW slew = new SLEW(name);
		slew.setSource(source);

		this.offsetRA = offsetRA;
		this.offsetDec = offsetDec;

		slew.setOffsetRA(offsetRA);
		slew.setOffsetDec(offsetDec);

		command = slew;

	}

	/**
	 * Create a Slew_Task using the supplied Source position and no offsets
	 * 
	 * @param source
	 *            The source whose position is to be slewed to.
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public SlewTask(String name, TaskManager manager, Source source) {
		this(name, manager, source, 0.0, 0.0);
	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();

		((SLEW) command).setNstrack(nsTracking);

		TCS_Status.Mechanisms mech = StatusPool.latest().mechanisms;
		// These are probably degrees.
		double caz = Math.toRadians(mech.azPos);
		double calt = Math.toRadians(mech.altPos);
		Position telpos = source.getPosition();
		// These are rads.
		double taz = telpos.getAzimuth();
		double talt = telpos.getAltitude();
		// ### THIS IS A QUICK FIX USING 2 deg/second to rads/sec.. #####
		double ttaz = Math.abs(taz - caz) * 28.65;
		double ttalt = Math.abs(talt - calt) * 28.65;
		logger.log(1, CLASS, name, "onInit", "Starting Slew From current position: (Az: " + Position.toDegrees(caz, 3)
				+ ", Alt: " + Position.toDegrees(calt, 3) + ")" + "\n To Target: " + source.getName() + "\n At: (Az: "
				+ Position.toDegrees(taz, 3) + ", Alt: " + Position.toDegrees(talt, 3) + ")" + "\n Offsets: ("
				+ offsetRA + " arcsec, " + offsetDec + " arcsec)" + "\n Tracking: "
				+ (nsTracking ? "Non-Sidereal" : "Sidereal") + "\n Estimated time: Az: " + (int) ttaz + "s, Alt: "
				+ (int) ttalt + "s, Cass: < 90s");

		// TODO FITS Skybrightness estimate
		//SkyBrightnessCalculator skycalc = new SkyBrightnessCalculator(RCS_Controller.controller.getSite());
		//XExtraSolarTarget target = new XExtraSolarTarget(source.getName());
		
		//TargetTrackCalculator track = new BasicTargetCalculator(target, site);
		//skycalc.getSkyBrightnessCriterion(track, time);
		
		// Telemetry.
		Telemetry.getInstance().publish(
				"OBS",
				new LogInfo(0L, "SLEW", "Starting Slew to target " + source.getName() + "\n Position: (Az: "
						+ Position.toDegrees(taz, 3) + ", Alt: " + Position.toDegrees(talt, 3) + ")" + "\n Offsets:  ("
						+ offsetRA + ", " + offsetDec + ") asec"));

		// Reset the mosaic offsets.
		ISS.setUserOffsets(0.0, 0.0);

	}

	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Completed Slew");

		// Telemetry.
		Telemetry.getInstance().publish("OBS", new LogInfo(0L, "SLEW", "Slew completed"));

	}

	public void setNsTracking(boolean ns) {
		this.nsTracking = ns;
	}

}

/**
 * $Log: SlewTask.java,v $ /** Revision 1.3 2006/12/13 13:01:38 snf /** Changed
 * nstrack refs /** /** Revision 1.2 2006/12/13 09:58:33 snf /** Added nsTrack
 * and preInit to handle non-sidereal tracking requirement. /** /** Revision 1.1
 * 2006/12/12 08:28:27 snf /** Initial revision /** /** Revision 1.1 2006/05/17
 * 06:33:16 snf /** Initial revision /** /** Revision 1.1 2002/09/16 09:38:28
 * snf /** Initial revision /**
 */
