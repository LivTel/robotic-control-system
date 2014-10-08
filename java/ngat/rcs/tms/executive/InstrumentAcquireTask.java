package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
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
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;

import java.util.*;
import java.text.*;

/**
 * This task sends an ACQUIRE command to the relevant acquisition instrument.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: InstrumentAcquireTask.java,v 1.6 2008/01/07 10:47:15 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/InstrumentAcquireTask
 * .java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.6 $
 */
public class InstrumentAcquireTask extends Default_TaskImpl {

	/** Name of acquisition instrument.*/
	protected String acqInstName;
	
	/** RA of target at rateTime.*/
	protected double ra;
	
	/** Dec of target at rateTime.*/
	protected double dec;
	
	/** Focal plane offset X.*/
	protected int offsetX;
	
	/** Focal plane offset Y.*/
	protected int offsetY;
	
	/**
	 * How close the brightest object or target RA/Dec has to be to the
	 * target pixel position (offsetX,offsetY) for the acquisition to succeed.
	 * The value is in decimal arcseconds. 
	 */
	protected double acquisitionThreshold;
	
	/** Acquisition mode: WCS or BRIGHTEST.*/
	protected int acqMode;

	/** Tracking rate in RA.*/
	protected double raRate;
	
	/** Tracking rate in dec.*/
	protected double decRate;
	
	/** Time at which the supplied tracking rates and ra/dec were calculated.*/
	protected long rateTime;
	
	/** True if this is a moving (NS tracking) target.*/
	protected boolean moving;
	
	/** Records last acquisition image filename. */
	protected String lastAcquireImageFileName;

	
	/**
	 * Create an AcquireTask using the supplied settings.
	 * 
	 * @param
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 * @param acqInstName
	 *            The name of the instrument to use.
	 * @param ra
	 *            The RA of the target (J2K hopefully).
	 * @param dec
	 *            The dec of the target (J2K hopefully).
	 * @param offsetX
	 *            The X offset pixel.
	 * @param offsetY
	 *            The Y offset pixel.
	 * @param acquisitionThreshold
	 *            How close the brightest object or target RA/Dec has to be to the
	 *            target pixel position (offsetX,offsetY) for the acquisition to succeed.
	 *            The value is in decimal arcseconds. 
	 * @param acqMode
	 *            Acquisition mode.
	 */
	public InstrumentAcquireTask(String name, TaskManager manager, 
			String acqInstName, 
			double ra, double dec, 
			boolean moving,
			double raRate, 
			double decRate,
			long rateTime,
			int offsetX, int offsetY, double acquisitionThreshold,int acqMode) {

		super(name, manager, acqInstName);
		this.acqInstName = acqInstName;
		this.ra = ra;
		this.dec = dec;
		this.moving = moving;
		this.raRate = raRate;
		this.decRate = decRate;
		this.rateTime = rateTime;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.acquisitionThreshold = acquisitionThreshold;
		this.acqMode = acqMode;
		ACQUIRE acquire = new ACQUIRE(name);
		acquire.setRA(ra);
		acquire.setDec(dec);
		
		// we may not need these params yet
		acquire.setRARate(raRate);
		acquire.setDecRate(decRate);
		acquire.setCalculationTime(rateTime);		
		acquire.setMoving(moving);
		
		acquire.setXPixel(offsetX);
		acquire.setYPixel(offsetY);
		acquire.setThreshold(acquisitionThreshold);
		acquire.setAcquisitionMode(acqMode);
		command = acquire;

	}

	/** Carry out subclass specific initialization. */
	@Override
	protected void onInit() {
		super.onInit();
		logger.log(1, CLASS, name, "onInit", "Starting Acquisition setup for target at J2K position: " + "RA: "
				+ Position.toHMSString(ra) + ", Dec: " + Position.toDMSString(dec) + ", Using Acq Inst: " + acqInstName
				+ ", Offsets: (" + offsetX + "," + offsetY + ") pix " + ", threshold = "+acquisitionThreshold+" arcseconds, AcqMode = "
				+ TelescopeConfig.toAquireModeString(acqMode));
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion", "Acquisition completed");
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	protected void onDisposal() {
		super.onDisposal();
	}

	/** Handle ACKs from Acquisition instrument. */
	@Override
	public void handleAck(ACK ack) {
		super.handleAck(ack);

		logger.log(1, CLASS, name, "handleAck", "Ack timeToComplete: " + ack.getTimeToComplete() + " ms");

		if (ack instanceof ACQUIRE_DP_ACK) {

			ACQUIRE_DP_ACK aack = (ACQUIRE_DP_ACK) ack;

			String fileName = aack.getFilename();
			if (fileName == null)
				fileName = "NO_DP_FILENAME_AVAILABLE";
			// record most recent acquisition image file name.
			lastAcquireImageFileName = fileName;

		}
	}

	/** Returns the last recorded acquisition image file name. */
	public String getLastAcquireImageFileName() {
		return lastAcquireImageFileName;
	}

}
