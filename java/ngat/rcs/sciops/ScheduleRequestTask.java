/**
 * 
 */
package ngat.rcs.sciops;

import ngat.ems.SkyModel;
import ngat.message.SMS.SCHEDULE_REQUEST;
import ngat.message.SMS.SCHEDULE_REQUEST_DONE;
import ngat.message.base.COMMAND_DONE;
import ngat.net.UnknownResourceException;
import ngat.rcs.RCS_Controller;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.tms.BasicErrorIndicator;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.Default_TaskImpl;
//import ngat.sms.ScheduleDespatcher;
//import ngat.sms.ScheduleItem;
import ngat.sms.GroupItem;

/**
 * This has been temporarily engineered to use SMS messages.
 * 
 * @author eng
 * 
 */
public class ScheduleRequestTask extends Default_TaskImpl {

	/** Elevation angle for Zenith. */
	public static final double ELEVATION_ZENITH = Math.PI / 2.0;

	/** Azimuth angle for direction South. */
	public static final double AZIMUTH_SOUTH = 0.0;

	/** Contains the group details. */
	private GroupItem group;

	/**
	 * @param name
	 * @param manager
	 */
	public ScheduleRequestTask(String name, TaskManager manager) {
		super(name, manager); // --------------------------
		// Set the connection to use.
		// --------------------------
		try {
			createConnection("SMS_COMMAND");
		} catch (UnknownResourceException e) {
			logger.log(1, "Schedule_Task", name, "Constructor",
					"Unable to establish connection to subsystem: SMS_COMMAND: " + e);
			failed = true;
			errorIndicator = new BasicErrorIndicator(101, "Creating connection: Unknown resource SMS_COMMAND", e);
			return;
		}

		// ----------------------------------------------------------
		// Set up the appropriate COMMAND - including authentication.
		// ----------------------------------------------------------

		boolean dolock = true; // ### NORMAL

		SCHEDULE_REQUEST schedule = new SCHEDULE_REQUEST(name);

		command = schedule;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.executive.Default_TaskImpl#onInit()
	 */
	@Override
	protected void onInit() {
		super.onInit();
		SkyModel skyModel = RCS_Controller.controller.getSkyModel();

		double seeing = 99.99;
		double extinction = 99.99;
		try {
			seeing = skyModel.getSeeing(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, System.currentTimeMillis());
			if (Double.isNaN(seeing) || Double.isInfinite(seeing)) {
				seeing = 99.99;
			}
		} catch (Exception e) {
			e.printStackTrace();
			seeing = 99.99;
		}

		FITS_HeaderInfo.current_SCHEDSEE.setValue(new Double(seeing));

		try {
			extinction = skyModel.getExtinction(700.0, ELEVATION_ZENITH, AZIMUTH_SOUTH, System.currentTimeMillis());
			String extCat = getExtinctionCategoryName(extinction);
			FITS_HeaderInfo.current_SCHEDPHT.setValue(extCat);
		} catch (Exception e) {
			e.printStackTrace();
			FITS_HeaderInfo.current_SCHEDPHT.setValue("UNKNOWN");
		}

	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {
		super.onCompletion(response);
		logger.log(1, CLASS, name, "onCompletion",
				"Returned response: " + "\nType: " + response.getClass().getName() + "\nId: " + response.getId()
						+ "\nSuccess: " + response.getSuccessful() + "\nError: " + response.getErrorNum()
						+ "\nMessage: " + response.getErrorString());

		SCHEDULE_REQUEST_DONE sched = (SCHEDULE_REQUEST_DONE) response;
		group = sched.getGroup();

		// ITag tag = group.getTag();
		// IUser user = group.getUser();
		// IProgram program = group.getProgram();
		// IProposal proposal = group.getProposal();
		// ISequenceComponent sequence = group.getSequence();
		// long historyId = sched.getHistoryId();

	}

	/**
	 * @return the groupItem
	 */
	public GroupItem getGroup() {
		return group;
	}

	/** Calculates which extinction band the specified extinction is in. */
	private String getExtinctionCategoryName(double extinction) {

		if (extinction > 0.5)
			return "SPECTROSCOPIC";
		else if (extinction > 0.0)
			return "PHOTOMETRIC";
		else
			return "UNKNOWN";
	}

}
