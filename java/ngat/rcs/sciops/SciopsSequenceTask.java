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
package ngat.rcs.sciops;

import ngat.rcs.tms.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.iss.*;
import ngat.sms.GroupItem;
//import ngat.sms.ExecutionUpdateManager;
//import ngat.sms.ExecutionUpdater;
import ngat.message.SMS.EXECUTION_UPDATE;
import ngat.phase2.*;

import java.util.*;

/**
 * Basically implements the function of a Group sequence from scheduling thro
 * execution to DB update. This is an analog of GroupSequenceTask from science
 * observing.
 */
public class SciopsSequenceTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, SCI_SEQ = 1400
	
	static final String CLASS = "SciopsSeq";

	// private ScheduleItem schedule;

	/** Counts group executions. */
	private int groupCounter;

	/**
	 * @param groupCounter
	 *            the groupCounter to set
	 */
	public void setGroupCounter(int groupCounter) {
		this.groupCounter = groupCounter;
	}

	private GroupItem group;

	// private ExecutionUpdater xu;

	private ExecutionUpdateTask xut;

	/**
	 * Create an TestSequenceTask
	 * 
	 * @param name
	 *            The unique name/id for this Task.
	 * @param manager
	 *            The Task's manager.
	 */
	public SciopsSequenceTask(String name, TaskManager manager) {
		super(name, manager);
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);

		// Schedule fails - try again 1 time then fail ?
		if (task instanceof ScheduleRequestTask) {

			// we might retry after a suitable delay or just give up...
			failed(661401, "Unable to obtain schedule");

		} else if (task instanceof GroupExecutionTask) {

			GroupExecutionTask gxt = (GroupExecutionTask) task;

			// get the execution information
			// ExecutionResourceBundle erb = gxt.getExecutionResourceBundle();
			Map erb = gxt.getExecutionResourceBundle();

			Set qosMetrics = gxt.getQosMetrics();
			GroupItem group = gxt.getGroup();

			// set EFC from task failure context
			ErrorIndicator err = gxt.getErrorIndicator();
			IExecutionFailureContext efc = new XBasicExecutionFailureContext(err.getErrorCode(), err.getErrorString());
			// xut = new ExecutionUpdateTask(name + "/EXEC_UPDATE", this, xu,
			// group, efc, erb, qosMetrics);
			
			// TODO We really want to extract this from lower down ie the specific type of error.
			xut = new ExecutionUpdateTask(name + "/EXEC_UPDATE", this, group, false, efc, EXECUTION_UPDATE.VETO_LEVEL_MEDIUM);
			taskList.addTask(xut);

		} else if 
			(task instanceof ExecutionUpdateTask) {

			xut = (ExecutionUpdateTask) task;
			GroupItem group = xut.getGroup();
	
			// HELP Maybe we should warn someone ?

			ErrorIndicator err = xut.getErrorIndicator();
			System.err.println("MAJOR - Unable to update DB with execution results:-");
			System.err.println("      - Group:     "+(group != null ? group.getName()+"["+group.getID()+"]" : "NULL"));
			System.err.println("      - HistID:    "+(group != null ? ""+group.getHId() : "NULL"));
		
			System.err.println("      - Completion: "+(xut.getExecutionFailureContext() != null ? 
					"Failed:"+xut.getExecutionFailureContext() : "Success"));
		
	        System.err.println("      - TaskError:  " + err.getErrorCode() + ":"+ err.getErrorString());
			// TODO should also log the QOS and ERB info..in a neat format !
			taskList.skip(task);
		}

	}

	@Override
	public void onSubTaskAborted(Task task) {
		super.onSubTaskAborted(task);

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		if (task instanceof ScheduleRequestTask) {
			// we have a schedule, extract info and create a TST.

			ScheduleRequestTask srt = (ScheduleRequestTask) task;

			group = srt.getGroup();

			if (group == null) {
				failed(661402, "No group found in schedule");
				return;
			}

			GroupExecutionTask gxt = new GroupExecutionTask(name + "/Group(" + group.getName() + ")", this, group);
			taskList.addTask(gxt);

		} else if (task instanceof GroupExecutionTask) {
			// Sequence has completed - so it worked ?

			GroupExecutionTask gxt = (GroupExecutionTask) task;

			// get the execution information
			// ExecutionResourceBundle erb = gxt.getExecutionResourceBundle();
			Map erb = gxt.getExecutionResourceBundle();

			Set qosMetrics = gxt.getQosMetrics();
		
			group = gxt.getGroup();

			// set EFC null as successful
			IExecutionFailureContext efc = null;
			xut = new ExecutionUpdateTask(name + "/EXEC_UPDATE", this, group, true, efc, EXECUTION_UPDATE.VETO_LEVEL_NONE);

			taskList.addTask(xut);

		}

	}

	@Override
	public void onAborting() {
		super.onAborting();
		if (xut == null || (!xut.isInitialized())) {
			if (group != null) {
				opsLog.log(1, "Aborting Group-Sequence: Attempting abort-db-update. "+
				   "Group: "+(group != null ? group.getName() : "N/A")+
				   ", Code:    "+abortCode+
				   ", Message: "+abortMessage);
				IExecutionFailureContext efc = new XBasicExecutionFailureContext(abortCode, "Aborted: " + abortMessage);
				// we can definitely suggest a NO_VETO at this stage as its not the group's fault...
				xut = new ExecutionUpdateTask(name + "/ABORT_UPDATE", this, group, false, efc, EXECUTION_UPDATE.VETO_LEVEL_NONE);
				taskList.addTask(xut);
			}
		}
	}

	@Override
	public void onDisposal() {
		super.onDisposal();
	}

	@Override
	public void onCompletion() {
		super.onCompletion();
		taskLog.log(1, CLASS, name, "onInit", "** Completed Test sequence operation");
	}

	@Override
	public void preInit() {
		super.preInit();
	}

	/** Overridden to carry out specific work after the init() method is called. */
	@Override
	public void onInit() {
		super.onInit();
		opsLog.log(1, "Starting Group-Sequence: " + groupCounter);

		// FITS set GROUP_UNIQUE_ID groupCounter
		FITS_HeaderInfo.current_GRPUID.setValue(new Integer(groupCounter));
		// User's required focus offset
		ISS.setCurrentFocusOffset(0.0); 
		ISS.setInstrumentFocusOffset(0.0);
		// User and instrument required position offsets
		//ISS.setInstrumentOffsets(0.0, 0.0);
		ISS.setUserOffsets(0.0,0.0);
		
		ISS.setBeamControlInstrument(null);
	
	}

	/**
	 * Creates the TaskList for this TaskManager. The tasks are all run in
	 * parallel but with stepped delays.
	 */
	@Override
	protected TaskList createTaskList() {

		taskLog.log(WARNING, 1, CLASS, name, "CreateTaskList", "Creating initial tasklist");

		ScheduleRequestTask srt = new ScheduleRequestTask(name + "/AsynchSchedRequest", this);
		taskList.addTask(srt);

		return taskList;
	}

}
