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
package ngat.rcs.tocs;

import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.phase2.*;
import ngat.message.GUI_RCS.*;

/**
 * This Task manages a set of Tasks to perform an exposure.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCExposeTask.java,v 1.2 2007/09/27 08:22:04 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCExposeTask.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class TOCExposeTask extends TOOP_ControlTask {

	// ERROR_BASE: RCS = 6, TOCS = 50, T_EXP = 400
	
	// Observation observation;
	IExposure exposure;

	int runs;
	float expose;
	boolean dpflag;

	String instrumentName;

	/** Stores: Counts. */
	protected int counts;

	/** Stores: XPix. */
	protected int xpix;

	/** Stores: YPix. */
	protected int ypix;

	/** Stores: Seeing (arcsec). */
	protected double seeing;

	/** Stores: Sky brightness (mag/asec2). */
	protected double skyBright;

	/** Stores: Photometricity (mag-ext). */
	protected double photom;

	/** Counts files received from (Multrun) exposure. */
	int countFiles = 0;

	/**
	 * Create a TOCExposeTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 */
	public TOCExposeTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor, int runs, float expose,
			boolean dpflag, String instrumentName) {

		super(name, manager, implementor);

		this.runs = runs;
		this.expose = expose;
		this.dpflag = dpflag;
		this.instrumentName = instrumentName;
		// this.config = config;

		// observation = new Observation("TOC_Observe");
		// observation.setNumRuns(runs);
		// Mosaic mosaic = new Mosaic();
		// mosaic.setPattern(Mosaic.SINGLE);
		// observation.setMosaic(mosaic);
		// observation.setExposeTime(expose);
		// observation.setInstrumentConfig(config);

		exposure = new XMultipleExposure(expose, runs);
		((XMultipleExposure) exposure).setName("TOCExpose");

	}

	@Override
	public void reset() {
		super.reset();
		countFiles = 0;
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650401, "Temporary fail TOC Exposure Sequence operation due to subtask failure.." + task.getName(), null);
		// }
	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);
	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
	}

	@Override
	public void onAborting() {
		super.onAborting();
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	/**
	 * Super.onCompletion() sends the message back to the client so we need to
	 * append any extra data here first.
	 */
	@Override
	public void onCompletion() {

		if (dpflag) {
			concatCompletionReply(", seeing=" + seeing + ", counts=" + counts + ", photom=" + photom + ", skybright="
					+ skyBright + ", xpix=" + xpix + ", ypix=" + ypix);
		}

		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Exposure Sequence with: " + countFiles
				+ (dpflag ? "reduced" : "raw") + " image files");
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Exposure Sequence");

	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		// TODO translate old config to a new one for exposure ...WE HAVE ADDED A NULL COLLATOR = CAREFULL !!
		Exposure_Task exposureTask = new Exposure_Task("Expose", this, exposure, instrumentName, "/TOCS", null);

		// TODO reset the obs pipeline config here or add extra params
		// to TOCS EXPOSE command to set this properly
		exposureTask.setDprt(dpflag);

		taskList.addTask(exposureTask);

		return taskList;

	}

	/**
	 * Catch any DP/non-DP processed filenames from a Multrun Exposure. If we
	 * have DP flag set then we only look at EXPOSURE_FILE messages otherwise we
	 * look at EXPOSURE_ELEMENT messages.
	 */
	@Override
	public void sigMessage(Task source, int type, Object message) {
		System.err.println("SIGMESG: Type: " + type + " From: " + source + " : " + message);
		switch (type) {
		case Exposure_Task.EXPOSURE_DATA:

			if (dpflag && message instanceof ReductionInfo) {
				countFiles++;

				ReductionInfo rinfo = (ReductionInfo) message;

				if (countFiles == 1)
					concatCompletionReply(" file1=" + rinfo.getFileName());
				else
					concatCompletionReply(" ,file" + countFiles + "=" + rinfo.getFileName());

				seeing = rinfo.getSeeing();
				counts = rinfo.getCounts();
				seeing = rinfo.getSeeing();
				photom = rinfo.getPhotometricity();
				skyBright = rinfo.getSkyBrightness();
				xpix = rinfo.getXpix();
				ypix = rinfo.getYpix();

			} else if (!dpflag && message instanceof ExposureInfo) {
				countFiles++;

				ExposureInfo einfo = (ExposureInfo) message;

				if (countFiles == 1)
					concatCompletionReply(" file1=" + einfo.getFileName());
				else
					concatCompletionReply(" ,file" + countFiles + "=" + einfo.getFileName());

			}
			break;
		case Exposure_Task.EXPOSURE_FILE:

			String tagName = (String) FITS_HeaderInfo.current_TAGID.getValue();
			String userName = (String) FITS_HeaderInfo.current_USERID.getValue();
			String propName = (String) FITS_HeaderInfo.current_PROPID.getValue();
			String grpName = (String) FITS_HeaderInfo.current_GROUPID.getValue();
			String obsName = exposure.getActionDescription();

			obsLog.log(1, "TOCA Program:" + tagName + " : " + userName + " : " + propName + " : " + grpName + " : "
					+ obsName + ": Exposure Completed, File: " + message);

			break;
		default:
			super.sigMessage(source, type, message);
		}
	}
}
