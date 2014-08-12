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
package ngat.rcs.tms;

import ngat.rcs.*;
import ngat.rcs.tms.executive.*;
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

import java.io.Serializable;
import java.util.*;

/**
 * Holds knowledge about the state and linkages of a Task within a TaskList.
 * This knowledge is upto date as far as the Task's manager is concerned but may
 * infact be out of date as regards the Task itsself.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskInfo.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskInfo.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskInfo implements Serializable {

	/** Stores state names. */
	protected static final String[] states = new String[] { "UNKNOWN", "PENDING", "RUNNING", "ABORTING", "ABORTED",
			"FAILED", "DONE", "CANCELLED", "SKIPPED" };

	/** Task state, indicates that state has not been set yet. */
	public static final int UNKNOWN = 0;

	/** Task state, indicates that Task is currently waiting to execute. */
	public static final int PENDING = 1;

	/** Task state, indicates that Task is executing. */
	public static final int RUNNING = 2;

	/** Task state, indicates that Task is currently aborting. */
	public static final int ABORTING = 3;

	/** Task state, indicates that Task is now aborted. */
	public static final int ABORTED = 4;

	/** Task state, indicates that Task has failed . */
	public static final int FAILED = 5;

	/** Task state, indicates that Task has completed. */
	public static final int DONE = 6;

	/** Task state, indicates that Task has been cancelled and will never run. */
	public static final int CANCELLED = 7;

	/** Task state, indicates that Task has been skipped over. */
	public static final int SKIPPED = 8;

	/** Create a TaskInfo for the specified Task and set its state to UNKNOWN. */
	public TaskInfo() {}
	
	/** Create a TaskInfo for the specified Task and set its state to UNKNOWN. */
	public TaskInfo(Task task) {
		this(task, UNKNOWN);
	}

	/** Create a TaskInfo for the specified Task and set its state. */
	public TaskInfo(Task task, int state) {
		this();
		this.task = task;
		this.state = state;
		triggers = new Vector();
	}

	/**
	 * The current state of knowledge of the Task - may be out of date as far as
	 * Task is concerned.
	 */
	protected int state;

	/** The Task being described. */
	protected transient Task task;

	/**
	 * List of other Tasks which must complete or be skipped in order to trigger
	 * this Task.
	 */
	protected transient List triggers;

	/** Counts number of attempts to run. */
	protected int runCount;

	/** Sets the state of the Task. */
	public void setState(int state) {
		this.state = state;
	}

	/** Returns the state of the Task. */
	public int getState() {
		return state;
	}

	/** Sets the Task. */
	public void setTask(Task task) {
		this.task = task;
	}

	/** Returns the Task. */
	public Task getTask() {
		return task;
	}

	/** Increments by one the number of attempts to run. */
	public void incRunCount() {
		runCount++;
	}

	/** Returns the number of attempts to run. */
	public int getRunCount() {
		return runCount;
	}

	/** Adds a Task as a Trigger for the Task. */
	public void addTrigger(Task trigTask) {
		triggers.add(trigTask);
	}

	/**
	 * Remove a Task as a Trigger for the Task. Returns silently if trigTask is
	 * NOT a trigger for Task.
	 */
	public void removeTrigger(Task trigTask) {
		if (triggers.contains(trigTask))
			triggers.remove(trigTask);
	}

	/** Removes all triggers for task. */
	public void clearTriggers() {
		triggers.clear();
	}

	/** Returns the List of Triggers. */
	public List getTriggers() {
		return triggers;
	}

	/** Disposes this TaskInfo. */
	public void dispose() {
		triggers.clear();
		triggers = null;
	}

	/** Returns a readable representation of this TaskInfo. */
	@Override
	public String toString() {
		return (task != null ? ("TaskInfo: " + task.getName() + " State: " + toStateString(state)) : "TaskInfo: null");
	}

	/** Returns a readable name for the specified state. */
	public static String toStateString(int istate) {
		if (istate < 0 || istate > 8)
			return "??STATE??";
		else
			return states[istate];
	}

}

/**
 * $Log: TaskInfo.java,v $ /** Revision 1.1 2006/12/12 08:28:09 snf /** Initial
 * revision /** /** Revision 1.1 2006/05/17 06:32:59 snf /** Initial revision
 * /** /** Revision 1.1 2002/09/16 09:38:28 snf /** Initial revision /**
 */
