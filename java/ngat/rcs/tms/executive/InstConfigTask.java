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
import ngat.rcs.sciops.ConfigTranslator;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.fits.*;
import ngat.phase2.*;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatusProvider;
import ngat.icm.BasicInstrument;
import ngat.instrument.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.GUI_RCS.*;

/**
 * A leaf Task for performing configuration of Instruemnt susbsystems. An
 * ngat.phase2.InstrumentConfig is checked to determine which instrument of
 * those available to send the config to via an ISS_INST.CONFIG command. The
 * instrument's current configuration is used to estimate the time which will be
 * required to complete the operation
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: InstConfigTask.java,v 1.2 2008/04/10 07:38:19 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/InstConfigTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class InstConfigTask extends Default_TaskImpl {

	// ERROR_BASE: RCS = 6, TMM/EXEC = 40, CONFIG = 200
	
	/** Constant indicating an unknown Instrument. */
	public static final int UNKNOWN_INSTRUMENT = 600201;

	/** Constant denoting the typical expected time for this Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** The InstrumentConfig which is to be setup. */
	protected IInstrumentConfig config;

	/** The Instrument to be configured. */
	// protected Instrument instrument;
	/** The Instrument to be used. */

	protected ngat.icm.InstrumentRegistry ireg;

	/** The Instrument to be used. */
	protected String instrumentName;

	protected InstrumentCapabilities icap;

	private InstrumentDescriptor instId;

	/**
	 * Create an InstConfigTask using the supplied InstConfig and settings.
	 * 
	 * @param instId
	 *            The name of the instrument.
	 * @param config
	 *            Tee InstrumentConfig to use.
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public InstConfigTask(String name, TaskManager manager, IInstrumentConfig config) {
		super(name, manager, config.getInstrumentName());
		this.config = config;

		// instrument = Instruments.findInstrument(instId);
		instrumentName = config.getInstrumentName();

		instId = new InstrumentDescriptor(instrumentName);
		try {
			ireg = RCS_Controller.controller.getInstrumentRegistry();
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640204, "Unable to locate instrument registry", e);
			return;
		}

		// Inst should not be null or the Mgr would have failed probably?
		InstrumentCapabilitiesProvider icp = null;
		try {
			icp = ireg.getCapabilitiesProvider(instId);
		} catch (Exception e) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640203, "Unable to locate instrument capabilities", e);
			return;
		}

		InstrumentConfig oconfig = null;

		if (icp == null) {
			failed = true;
			errorIndicator = new BasicErrorIndicator(640205, "Cannot select instrument for Config: "
					+ config.getClass().getName(), null);
			return;
			// FATAL
		} else {
			try {
				icap = icp.getCapabilities();
				oconfig = ConfigTranslator.translateToOldStyleConfig(config);
				logger.log(1, CLASS, name, "<<create>>", 
						"Successfully translated config to old version: "+oconfig); 
			} catch (Exception e) {
				e.printStackTrace();
				failed = true;
				errorIndicator = new BasicErrorIndicator(640206,
						"Cannot translate config: " + config.getClass().getName(), null);
				return;
			}
		}
		command = new CONFIG(name);
		((CONFIG) command).setConfig(oconfig);

	}

	/**
	 * Create an InstConfigTask using the supplied InstConfig and settings.
	 * 
	 * @param config
	 *            THe InstrumentConfig to use.
	 * @param name
	 *            The unique name/id for this TaskImpl - should be based on the
	 *            COMMAND_ID.
	 * @param manager
	 *            The Task's manager.
	 */
	// public InstConfigTask(String name,
	// TaskManager manager,
	// InstrumentConfig config) {
	// this(name, manager, Instruments.findInstrumentFor(config).getName(),
	// config);
	// }

	// /** Compute the estimated completion time.
	// * @return The initial estimated completion time in millis.*/
	// protected long calculateTimeToComplete() {
	// //return instrument.getReconfigurationTime(config);
	// }

	/** Returns the default time for this command to execute. */
	public static long getDefaultTimeToComplete() {
		// return RCS_Configuration.getLong("tcs_command.slew.timeout",
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

    @Override
	protected void logExecutionStatistics() {
	logger.log(3, CLASS, name, "onCompletion", "EXEC_TIME for " + CLASS + " : " + config.getInstrumentName()
		   + " : "+ (System.currentTimeMillis() - startTime));
    }
    
	/**
	 * Carry out subclass specific initialization.
	 * <ul>
	 * <li>Add some FITS headers.
	 * <li>Tell the ISS which filter we are about to use so it can subtract
	 * focus offsets
	 * </ul>
	 */
	@Override
	protected void onInit() {
		super.onInit();

		logger.log(1, CLASS, name, "onInit", "Starting Instrument Configuration using " + config);
	}

	/** Carry out subclass specific completion work. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		// Set the selected Instrument as current and set its config... why only
		// CCD ?
		// if (config instanceof CCDConfig) {
		// instrument.configure(config);
		// }
		// Instruments.currentInstrument = instrument;

		InstrumentStatusProvider isp = null;
		try {
			isp = ireg.getStatusProvider(instId);
			((BasicInstrument) isp).setCurrentConfig(config);
		} catch (Exception e) {
			logger.log(1, CLASS, name, "onCompletion", "Unable to locate instrument status provider for "
					+ instrumentName);
		}

		// set the SKYPA correction
		// double rotcorr = instrument.getRotatorAlignmentCorrection();
		logger.log(1, CLASS, name, "onCompletion", "Completed Configuration of " + instrumentName);

		// TODO this may be entirely wrong....
		double rotcorr = icap.getRotatorOffset();
		FITS_HeaderInfo.setRotatorSkyCorrection(rotcorr);

		// Telemetry.

	}

	// public Instrument getInstrument() {
	// return instrument;
	// }

}

/**
 * $Log: InstConfigTask.java,v $ /** Revision 1.2 2008/04/10 07:38:19 snf /**
 * added rotsky correction on completion /** /** Revision 1.1 2006/12/12
 * 08:28:27 snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:16
 * snf /** Initial revision /** /** Revision 1.1 2002/09/16 09:38:28 snf /**
 * Initial revision /**
 */
