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
import ngat.tcm.SciencePayload;
import ngat.net.*;
import ngat.phase2.*;
import ngat.icm.InstrumentDescriptor;
import ngat.instrument.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;

import java.io.*;

/**
 * A leaf Task for performing Twilight calibration of the science camera.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TwilightCalibrationTask.java,v 1.2 2008/04/21 08:49:11 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/TwilightCalibrationTask
 * .java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class TwilightCalibrationTask extends Default_TaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, TWI_CAL = 700

	public static final String CLASS = "TwilightCalibTask";

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** The time available for this task to complete (msec). */
	protected long timeAvailableToComplete;

	/** Counts the number of exposure frames. */
	protected int countFrames;

	protected SciencePayload payload;

    private InstrumentDescriptor instId;

	/** The Instrument to be used. */
	protected String instrumentName;

	/** Blank field target. */
	protected Source blankField;

	/**
	 * Create a TwilightCalibrationTask using the supplied timeToComplete. Sets
	 * the Instrument and creates a Connection to its ControlSystem.
	 * 
	 * @param timeAvailableToComplete
	 *            The maximum time available for this task to complete by.
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	public TwilightCalibrationTask(String name, TaskManager manager, String instrumentName, long timeAvailableToComplete) {
		// TODO required or not ? Source blankField,
		super(name, manager, instrumentName);
		this.timeAvailableToComplete = timeAvailableToComplete;
		// this.blankField = blankField;
		// Position target = blankField.getPosition();
		this.instrumentName = instrumentName;

		// Inst should not be null or the Mgr would have failed probably?
		if (instrumentName == null) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640702, "Cannot select instrument for calibration: " + instrumentName, null);
			return;
			// FATAL
		}

		// get science pyaload ref
		try {
			payload = RCS_Controller.controller.getTelescope().getTelescopeSystem().getSciencePayload();
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640701, "Unable to locate science payload", e);
		}
		
		// -------------------------------
		// Set up the appropriate COMMAND.
		// -------------------------------
		TWILIGHT_CALIBRATE twicalib = new TWILIGHT_CALIBRATE("TT_CALIB");
		// TODO Check if TWI_CAL command requires radians or degrees, assume
		// rads for now
		// twicalib.setTelescopeAzimuth(target.getAzimuth());
		// twicalib.setTelescopeAltitude(target.getAltitude());
		twicalib.setTimeToComplete(timeAvailableToComplete);

		command = twicalib;

		countFrames = 0;
	}

	/**
	 * Overridden. When a TWILIGHT_CALIBRATE_ACK is received the filename of the
	 * latest calib-frame is logged. When the associated
	 * TWILIGHT_CALIBRATE_DP_ACK is received the filename and counts are logged.
	 */
	@Override
	public void handleAck(ACK ack) {
		super.handleAck(ack);

		InstrumentDescriptor instId = new InstrumentDescriptor(instrumentName);

		if (ack instanceof TWILIGHT_CALIBRATE_DP_ACK) {
			TWILIGHT_CALIBRATE_DP_ACK dack = (TWILIGHT_CALIBRATE_DP_ACK) ack;
			logger.log(1, CLASS, name, "handleAck", "TWILIGHT_CALIBRATE_DP_ACK received:" + "\nProcessed File: "
					+ dack.getFilename() + "\n\tMean Counts: " + dack.getMeanCounts() + "\n\tPeak Counts: "
					+ dack.getPeakCounts());
		} else if (ack instanceof TWILIGHT_CALIBRATE_ACK) {
			TWILIGHT_CALIBRATE_ACK dack = (TWILIGHT_CALIBRATE_ACK) ack;
			logger.log(1, CLASS, name, "handleAck", "TWILIGHT_CALIBRATE_ACK received:" + "\nRaw File: "
					+ dack.getFilename());
			countFrames++;
			FITS_HeaderInfo.current_OBSID.setValue("TT_CALIB:FRAME#" + countFrames);

			if (manager != null) {
				manager.sigMessage(this, Exposure_Task.EXPOSURE_FILE, payload.getMountPointForInstrument(instId) + "/"
						+ (new File(dack.getFilename())).getName());
			}

		} else {
			logger.log(1, CLASS, name, "handleAck", ack.getClass().getName() + " received:");
		}
	}

	/** This task can NOT be aborted when it is running - let it fail. */
	@Override
	public boolean canAbort() {
		return false;
	}

	/**
	 * Carry out subclass specific initialization. Set the calibration headers.
	 */
	@Override
	public void onInit() {
		super.onInit();
		logger.log(1, "Starting Twilight-Calibration." + "\n Instrument:     " + connectionId + "\n Time Available: "
				+ (timeAvailableToComplete / 1000.0) + " secs.");

	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	public void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, "Completed Twilight-Calibration.");
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	public void onDisposal() {
		super.onDisposal();
	}

}

/**
 * $Log: TwilightCalibrationTask.java,v $ /** Revision 1.2 2008/04/21 08:49:11
 * snf /** added target spec to allow telescope alt/az to be sent to instrument
 * /** /** Revision 1.1 2006/12/12 08:28:27 snf /** Initial revision /** /**
 * Revision 1.1 2006/05/17 06:33:16 snf /** Initial revision /** /** Revision
 * 1.1 2002/09/16 09:38:28 snf /** Initial revision /**
 */
