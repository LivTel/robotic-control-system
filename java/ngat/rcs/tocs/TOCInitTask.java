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

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.iss.*;
import ngat.sms.GroupItem;
import ngat.util.PersistentUniqueInteger;
import ngat.phase2.*;
import ngat.message.RCS_TCS.*;

/**
 * This Task manages ..
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOCInitTask.java,v 1.2 2008/05/20 07:11:14 eng Exp eng $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOCInitTask.java,v $
 * </dl>
 * 
 * @author $Author: eng $
 * @version $Revision: 1.2 $
 */
public class TOCInitTask extends TOOP_ControlTask {
	
	// ERROR_BASE: RCS = 6, TOCS = 50, T_INIT = 500
	
	/** Option: Rotator to SKY angle. */
	public static final int ROT_SKY = 1;

	/** Option: Rotator to MOUNT angle and FLOAT. */
	public static final int ROT_MOUNT = 2;

	/** Option: Rotator to FLOAT. */
	public static final int ROT_FLOAT = 3;

	/** Option: Focus Tracking to be switched OFF. */
	public static final int FOCUS_OFF = 3;

	/** Option: Focus Tracking to be left switched ON. */
	public static final int FOCUS_ON = 4;

	/** Option: A/G selected. */
	public static final int AG_SELECT = 5;

	/** Option: A/G NOT selected. */
	public static final int AG_NO_SELECT = 6;

	/** Mode for rotator. */
	int rotatorOption;

	/** Focus option. */
	int focusOption;

	/** A/g option. */
	int agOption;

	/**
	 * Create a TOInitTask.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 * @param rotatorOption
	 *            Rotator mode option.
	 * @param focusOption
	 *            Focus tracking option.
	 * @param agOption
	 *            A/G select option.
	 * @param instId
	 *            Instrument ID.
	 * @param alias
	 *            TCS alias of instrument.
	 */
	public TOCInitTask(String name, TaskManager manager, TOC_GenericCommandImpl implementor, int rotatorOption,
			int focusOption, int agOption) {

		super(name, manager, implementor);

		this.rotatorOption = rotatorOption;
		this.focusOption = focusOption;
		this.agOption = agOption;
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		// if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
		// resetFailedTask(task);
		// } else {
		failed(650501, "Temporary fail TOCInit operation due to subtask failure.." + task.getName(), null);
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

	@Override
	public void onCompletion() {
		super.onCompletion();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Completed TOC Init");
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		taskLog.log(WARNING, 1, CLASS, name, "onInit", "** Starting TOC Init");

		TOControlAgent tocAgent = (TOControlAgent) TOControlAgent.getInstance();
		TOControlAgent.ServiceDescriptor currentSA = tocAgent.getCurrentService();

		FITS_HeaderInfo.current_TAGID.setValue(currentSA.tagId);
		FITS_HeaderInfo.current_USERID.setValue(currentSA.userId);
		FITS_HeaderInfo.current_PROGID.setValue(currentSA.progId);
		FITS_HeaderInfo.current_PROPID.setValue(currentSA.proposalId);
		FITS_HeaderInfo.current_GROUPID.setValue(currentSA.groupId);
		ISS.setCurrentFocusOffset(0.0);
		ISS.setBeamControlInstrument(null);
		
		// TODO obtain a GRPUID and set the FITS header
		try {
			PersistentUniqueInteger puid = new PersistentUniqueInteger("%%group");
			int groupCounter = puid.increment();
			FITS_HeaderInfo.current_GRPUID.setValue(new Integer(groupCounter));
		} catch (Exception e) {
			taskLog.log(1, CLASS, name, "getNextJob", "Building symlink: Failed to increment group counter: " + e);
			FITS_HeaderInfo.current_GRPUID.setValue(new Integer(0));
		}

		FITS_HeaderInfo.current_ACQINST.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQIMG.setValue(new String("UNKNOWN"));
		FITS_HeaderInfo.current_ACQMODE.setValue("NONE");
		FITS_HeaderInfo.current_ACQXPIX.setValue("UNKNOWN");
		FITS_HeaderInfo.current_ACQYPIX.setValue("UNKNOWN");

	}

	/** Creates the TaskList for this TaskManager. */
	@Override
	protected TaskList createTaskList() {

		RotatorTask rotatorTask = null;
		RotatorTask rotatorFloatTask = null;

		// WARNING - ROT SKY is not really feasible without including CP - DO
		// NOT USE !!.

		switch (rotatorOption) {

		case ROT_SKY:

			rotatorTask = new RotatorTask("RotSky", this, 0.0, ROTATOR.SKY);
			taskList.addTask(rotatorTask);

			break;

		case ROT_FLOAT:

			rotatorTask = new RotatorTask("RotFlt", this, 0.0, ROTATOR.FLOAT);
			taskList.addTask(rotatorTask);

			break;

		case ROT_MOUNT:

			rotatorTask = new RotatorTask("RotMount", this, 0.0, ROTATOR.MOUNT);
			taskList.addTask(rotatorTask);

			rotatorFloatTask = new RotatorTask("RotFloat", this, 0.0, ROTATOR.FLOAT);
			rotatorFloatTask.setDelay(5000L);
			taskList.addTask(rotatorFloatTask);

			try {
				taskList.sequence(rotatorTask, rotatorFloatTask);
			} catch (TaskSequenceException tx) {
				errorLog.log(1, CLASS, name, "createTaskList", "Failed to create Task Sequence for TOC Init: " + tx);
				failed = true;
				errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
				errorIndicator.setErrorString("Failed to create Task Sequence for TOC Init.");
				errorIndicator.setException(tx);
				return null;
			}

			break;

		}

		if (agOption == AG_SELECT) {
			AgSelectTask agSelectTask = new AgSelectTask("AGSelect", this, AGSELECT.CASSEGRAIN);
			taskList.addTask(agSelectTask);
		}

		// ### NOTE what this might do nasty stuff if set OFF need to fix before
		// exiting
		// ### or do a default fix after we go into SCA or PCA or CCA next
		if (focusOption == FOCUS_OFF) {
			Track_Task trackFocusTask = new Track_Task("TrkFocusOff", this, TRACK.FOCUS, TRACK.OFF);
			taskList.addTask(trackFocusTask);
		}
		
		TOControlAgent tocAgent = (TOControlAgent) TOControlAgent.getInstance();
		TOControlAgent.ServiceDescriptor currentSA = tocAgent.getCurrentService();
		XGroup group = new XGroup();
		group.setName(currentSA.groupId);
		XIteratorComponent root = new XIteratorComponent("root", new XIteratorRepeatCountCondition(1));
	
		GroupItem groupItem = new GroupItem(group, root);
		
		XTag useTag = new XTag();
		useTag.setName(currentSA.tagId);
		XUser usePI = new XUser(currentSA.userId);
		XProgram useProgram = new XProgram(currentSA.progId);
		XProposal useProposal = new XProposal(currentSA.proposalId);
		useProposal.setPriority(IProposal.PRIORITY_Z);
		
		groupItem.setTag(useTag);
		groupItem.setUser(usePI);
		groupItem.setProgram(useProgram);
		groupItem.setProposal(useProposal);
		
		RCS_Controller.controller.getGroupOperationsMonitor().notifyListenersGroupSelected(groupItem);
		
		return taskList;
	}

}
