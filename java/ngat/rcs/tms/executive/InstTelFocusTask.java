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
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentDescriptor;
import ngat.instrument.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.math.CartesianCoordinatePair;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.tcm.*;
import ngat.tcm.test.*;

import java.awt.*;
import java.util.*;
import java.text.*;
import java.io.*;

/**
 * A leaf Task for performing the telescope focus calibration using a specified
 * instrument.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: InstTelFocusTask.java,v 1.3 2008/08/21 13:01:18 eng Exp eng $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/InstTelFocusTask.java,v $
 * </dl>
 * 
 * @author $Author: eng $
 * @version $Revision: 1.3 $
 */
public class InstTelFocusTask extends Default_TaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, INST_TELFOCUS = 300
	
	/** Acceptable fwhm error (arcsec). */
	static double MAX_FWHM_ERROR = 0.1;

	/** Constant indicating that instrument filters have not been setup. */
	public static final int FILTER_CONFIG_ERROR = 640301;

	/** Constant indicating an unknown Instrument. */
	public static final int UNKNOWN_INSTRUMENT_ERROR = 640302;

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** Stores the set of focus/fwhm measurements. */
	protected java.util.List focusResultsTable;

	/** Start of range for focus calib. (mm). */
	protected double focusStart;

	/** End of range for focus calib. (mm). */
	protected double focusStop;

	/** Focus change step size (mm). */
	protected double focusInc;

	/** Exposure time (millis). */
	protected int exposeTime;

	/** Counts the number of TelFocus exposures reduced so far. */
	protected int countReducedExposures;

	/** The Instrument to be used. */
	protected String instrumentName;

	/** Calibration monitor end point. */
	protected BasicCalibrationMonitor bcalMonitor;

	/**
	 * Create an Inst_TelFocus_Task using the supplied InstConfig and settings.
	 * 
	 * @param focusStart
	 *            Start of focus range (mm).
	 * @param focusStop
	 *            End of focus range (mm).
	 * @param focusInc
	 *            Focus range step size (mm).
	 * @param exposeTime
	 *            The exposure time in millis.
	 * @param instId
	 *            The ID of the Instrument - used for connection and setup.
	 * @param sourceMagnitude
	 *            The magnitude of the calibration source.
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	public InstTelFocusTask(String name, TaskManager manager, String instrumentName, double focusStart, double focusStop,
			double focusInc, int exposeTime) {
		super(name, manager, instrumentName);
		this.instrumentName = instrumentName;
		this.focusStart = focusStart;
		this.focusStop = focusStop;
		this.focusInc = focusInc;
		this.exposeTime = exposeTime;
		focusResultsTable = new Vector();
		// ----------------------------
		// 1. Decide on the Instrument.
		// ----------------------------
	
		countReducedExposures = 0;

		// ----------------------------------
		// 2. Set up the appropriate COMMAND.
		// ----------------------------------

		TELFOCUS telfocus = new TELFOCUS(name);
		telfocus.setStartFocus((float) focusStart);
		telfocus.setEndFocus((float) focusStop);
		telfocus.setStep((float) focusInc);
		telfocus.setExposureTime(exposeTime);

		command = telfocus;
	}

	/** Returns the default time for this command to execute. */
	public static long getDefaultTimeToComplete() {
		// return RCS_Configuration.getLong("iss_command.telfocus.timeout",
		// DEFAULT_TIMEOUT);
		return DEFAULT_TIMEOUT;
	}

	/**
	 * Compute the estimated completion time.
	 * 
	 * @return The initial estimated completion time in millis.
	 */
	@Override
	protected long calculateTimeToComplete() {
		return getDefaultTimeToComplete();
	}

	/**
	 * Carry out subclass specific initialization. Add some FITS headers.
	 */
	@Override
	protected void onInit() {
		super.onInit();

		logger.log(1, CLASS, name, "onInit", "Starting InstTelfocus for: "
				+ instrumentName);

		// external monitoring gui
		bcalMonitor = RCS_Controller.controller.getCalibrationMonitor();

		long now = System.currentTimeMillis();

		double vf = StatusPool.latest().mechanisms.secMirrorPos;
		double alt = StatusPool.latest().mechanisms.altPos;
		double temp = StatusPool.latest().meteorology.serrurierTrussTemperature;

		bcalMonitor.notifyListenersStartingTelfocus(now, focusStart, focusStop, focusInc, 100.0, instrumentName, vf, alt, temp);

	}

	/** This task can NOT be aborted when it is running - let it fail. */
	@Override
	public boolean canAbort() {
		return false;
	}

	/**
	 * Overridden. When a TELFOCUS_DP_ACK is received the seeing information is
	 * used along with the current focus (calculated as focusStart + i *
	 * focusInc) for ith received DP_ACK, to plot on the GraphPlot.
	 */
	@Override
	public void handleAck(ACK ack) {
		super.handleAck(ack);

		if (ack instanceof TELFOCUS_DP_ACK) {

			TELFOCUS_DP_ACK tack = (TELFOCUS_DP_ACK) ack;
			float focus = (float) (focusStart + (countReducedExposures * focusInc));
			double fwhm = tack.getSeeing();
			logger.log(1, CLASS, name, "handleAck", "TELFOCUS_DP_ACK received:" + "\nAt Focus:    " + focus + " mm."
					+ "\nFile:        " + tack.getFilename() + "\nCounts:      " + tack.getCounts() + "\nSeeing:      "
					+ tack.getSeeing() + " arcsec." + "\nPhotometric: " + tack.getPhotometricity() + " mags-ext."
					+ "\nSaturation:  " + tack.getSaturation() + "\nSky Bright:  " + tack.getSkyBrightness()
					+ " mag/arsec^2." + "\nBright Obj:" + "\n  X-pixel:   " + tack.getXpix() + "\n  Y-pixel:   "
					+ tack.getYpix());

			focusResultsTable.add(new CartesianCoordinatePair(focus, fwhm));

			countReducedExposures++;

			// external monitoring gui
			long now = System.currentTimeMillis();
			bcalMonitor.notifyListenersFocusUpdate(now, focus, fwhm);

		} else {
			logger.log(1, CLASS, name, "handleAck", ack.getClass().getName() + " received:");
		}
	}

	/** Carry out subclass specific completion work. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		if (response instanceof TELFOCUS_DONE) {
			TELFOCUS_DONE teldone = (TELFOCUS_DONE) response;
			float fwhm = teldone.getSeeing();
			float focus = teldone.getCurrentFocus();
			double aa = teldone.getA();
			double bb = teldone.getB();
			double cc = teldone.getC();
			double chi = teldone.getChiSquared();

			// some additonal calculated parameters - not used yet..
			double w0 = cc - bb * bb / (4.0 * aa);
			double fs = Math.sqrt(MAX_FWHM_ERROR / (aa * w0));

			// Calculate the rms diffs between Model and Observed at each
			// measured point.
			Iterator it = focusResultsTable.iterator();
			int np = 0;
			double diff = 0.0;
			double sum = 0.0;
			double model = 0.0;
			CartesianCoordinatePair point = null;
			while (it.hasNext()) {
				point = (CartesianCoordinatePair) it.next();
				model = aa * point.x * point.x + bb * point.x + cc;
				diff = model - point.y;
				sum = sum + diff * diff;
				np++;
			}
			double sigma = sum / (np - 3); // N-M = Point - Degrees of freedom.

			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(5);

			// optimumFocus = 5.0;
			Tabulator table = new Tabulator("Telescope Focus Calibration", new int[] { 40, 20 });
			table.putPair("Quadratic Fit param A", "" + nf.format(aa));
			table.hline('-');
			table.putPair("Quadratic Fit param B", "" + nf.format(bb));
			table.hline('-');
			table.putPair("Quadratic Fit param C", "" + nf.format(cc));
			table.hline('-');
			table.putPair("Chi square value", "" + nf.format(chi));
			table.hline('-');
			table.putPair("Optimum focus", "" + nf.format(focus) + " mm");
			table.hline('-');
			table.putPair("Min. FWHM at optimum", "" + nf.format(w0) + " arcsec.");
			table.hline('-');
			table
					.putPair("Allowed Focus Spread for FWHM error (" + MAX_FWHM_ERROR + ")", "+/-" + nf.format(fs)
							+ " mm");
			table.hline('-');
			table.putPair("FWHM Measurement Sigma", "" + nf.format(sigma) + " pixels");
			table.hline('-');

			// external monitoring gui
			long now = System.currentTimeMillis();
			bcalMonitor.notifyListenersFocusCompleted(now, focus, fwhm, aa, bb, cc, chi);

			logger.log(1, "Completed Telescope-Focus Calibration/Measurement-cycle." + "\n" + table.getBuffer());

			// Keyword based logging
			logger.log(1, "TELFOCUS QFIT_A " + nf.format(aa));
			logger.log(1, "TELFOCUS QFIT_B " + nf.format(bb));
			logger.log(1, "TELFOCUS QFIT_C " + nf.format(cc));
			logger.log(1, "TELFOCUS CHI " + nf.format(chi));
			logger.log(1, "TELFOCUS OPT_FOCUS " + nf.format(focus) + " mm");
			logger.log(1, "TELFOCUS MIN_FWHM " + nf.format(w0) + " arcsec");
			logger.log(1, "TELFOCUS MAX_ERR " + MAX_FWHM_ERROR);
			logger.log(1, "TELFOCUS SPREAD " + nf.format(fs));
			logger.log(1, "TELFOCUS SIGMA " + nf.format(sigma) + " pixels");

			// TODO new style context logging...
			// START NEW LOGGING
			// logger.prepare().block("onCompletion").severity(Logging.SEVERITY_INFO).level(1).
			// log("Telfocus calibration results").
			// context("TELFOCUS QFIT_A", nf.format(aa)).
			// context("TELFOCUS QFIT_B", nf.format(bb)).
			// context("TELFOCUS QFIT_C", nf.format(cc)).
			// context("TELFOCUS CHI", nf.format(chi)).
			// context("TELFOCUS OPT_FOCUS",nf.format(focus)+" mm").
			// context("TELFOCUS MIN_FWHM", nf.format(w0)" arcsec").
			// context("TELFOCUS MAX_ERR", MAX_FWHM_ERROR).
			// context("TELFOCUS SPREAD", nf.format(fs)).
			// context("TELFOCUS SIGMA", nf.format(sigma)+" pixels").
			// send();
			// END NEW LOGGING

			// Check if the focus is very different from default
			// and maybe the seeing appears crap this may indicate
			// that the TelFocus was actually bad - e.g. cloud
			// passed by during exposure(s) FAIL (BAD_TELFOCUS_ERROR)
			// ## WATCH THE ORDER HERE The manager may have already moved on
			// now!!

		}
	}

}

/**
 * $Log: InstTelFocusTask.java,v $ /** Revision 1.3 2008/08/21 13:01:18 eng /**
 * typo /** /** Revision 1.2 2008/08/21 13:00:29 eng /** added extra keyword
 * based logging. /** /** Revision 1.1 2006/12/12 08:28:27 snf /** Initial
 * revision /** /** Revision 1.1 2006/05/17 06:33:16 snf /** Initial revision
 * /** /** Revision 1.1 2002/09/16 09:38:28 snf /** Initial revision /**
 */
