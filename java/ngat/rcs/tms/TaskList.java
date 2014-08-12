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

import java.util.*;
/**Class which is capable of delivering information about the current states 
 * of the Tasks managed by a TaskManager.
 * Note: As a rule when making calls to the actual implemented methods it 
 * would generally be wise to make a synchronization lock (locks) available
 * in order to avoid concurrent update problems. As a minimum the TaskList
 * implementation itself should be locked prior to making concurrent access.
 * Tasks should be added to the Tasklist using the addTask(Task) method then
 * linked into the sequential / parallel dependancy using the methods
 * sequence(Task, Task), removeTrigger(Task, Task).
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskList.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskList.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskList {
 
    /** Error code - represents a task-sequencing error.*/
    public static int TASK_SEQUENCE_ERROR = 640000;

    /** Internal Mapping.*/
    protected Map map;
    
    /** Create a TaskList.*/
    public TaskList() {
	map = new HashMap();
    }
    
    /** Clear the TaskList of all Tasks.*/
    public void clear() {
	map.clear();
    }
    
    /** Add a task to the list and set its info-state to PENDING.*/
    public void addTask(Task task) {
	addTask(task, TaskInfo.PENDING);
    }
    
    /** Add a task to the list and set its info-state.*/
    public void addTask(Task task, int initState) {
	map.put(task, new TaskInfo(task, initState));
    }

    /** Link t1 and t2 sequentially: 
     * - i.e t1 must complete or be skipped before t2 can start.
     * This is used to emulate <i>forking</i> components.
     */
    public void sequence(Task t1, Task t2) throws TaskSequenceException {
	if ( ! map.containsKey(t1))
	    throw new TaskSequenceException("Link from: "+t1.getName()+" Not in TaskList.");
	if ( ! map.containsKey(t2))
	     throw new TaskSequenceException("Link to: "+t2.getName()+" Not in TaskList.");
	TaskInfo tinfo = (TaskInfo)map.get(t2);
	tinfo.addTrigger(t1);
    } 
    

    /** Unlink t1 and t2 sequentially:
     * - i.e. t1 is removed as a trigger for t2.
     * Returns silently if t1 is not a trigger for t2.
     */
    public void unsequence(Task t1, Task t2) {
	if ( !map.containsKey(t2))
	    return;
	TaskInfo tinfo = (TaskInfo)map.get(t2);
	// Returns silently if t1 is not a trigger for t2.
	tinfo.removeTrigger(t1);
    }    

    /** Remove a Task from the TaskList. No check is made whether this Task is
     * a trigger for others or to its state - use carefully.
     * If task is not in the list returns silently.*/
    public void removeTask(Task task) {
	if (map.containsKey(task))
	    map.remove(task);
    }
  
    /** Insert a a new Task (t2) into the TaskList immediately before
     * Task (t1), whilst maintaining the validity of the structure.
     * - i.e. All of t1's triggers become triggers for t2 and t2 
     * becomes a (the only) trigger for t1.
     * @param t1 The Task to be preceded by t2.
     * @param t2 The Task to insert before t1.
     */
    public void insertBefore(Task t1, Task t2)  throws TaskException {
	if ( ! map.containsKey(t1))
	    throw new TaskSequenceException("Link to: "+t1.getName()+" Not in TaskList.");
	if ( ! map.containsKey(t2))
	    throw new TaskSequenceException("Link from: "+t2.getName()+" Not in TaskList.");
	TaskInfo t1info = (TaskInfo)map.get(t1);
	TaskInfo t2info = (TaskInfo)map.get(t2);

	List triggers = t1info.getTriggers();
	// Add all t1's triggers to t2.
	// Watch for cyclic !
	for (int i = 0; i < triggers.size(); i++) {
	    Task trig = (Task)triggers.get(i);
	    if (trig == t2)
		throw new TaskException("Cyclic link: "+t2.getName());
	    t2info.addTrigger(trig);
	}
	// Clear t1's triggers.
	t1info.clearTriggers();

	// Add t2 as trigger for t1.
	t1info.addTrigger(t2);

    }

    //  /** Override to return true if there are no more uncompleted tasks left.*/
    //public boolean finished();
    
    /** Returns True only if the specified task is free to run:-
     * It must be PENDING, Any and All Triggers must be DONE or SKIPPED.
     */
    public boolean canRun(Task task) {
	if ( ! map.containsKey(task) )
	    return false;
	
	TaskInfo tinfo     = (TaskInfo)map.get(task);
	if (tinfo.getState() != TaskInfo.PENDING)
	    return false;
	List     triggers  = tinfo.getTriggers();
	
	if (triggers.isEmpty())
	    return true;

	for (int i = 0; i < triggers.size(); i++) {
	    Task     trig     = (Task)triggers.get(i);
	    TaskInfo triginfo = (TaskInfo)map.get(trig);
	    if ( (triginfo.getState() != TaskInfo.DONE) &&
		 (triginfo.getState() != TaskInfo.SKIPPED))
		return false;
	}
	return true;
    }

    /** Returns the TaskInfo for specified Task.
     * If Task is not in list returns null.
     */
    public TaskInfo getInfo(Task task) {
	if (map.containsKey(task))
	    return (TaskInfo)map.get(task);
	return null;
    }
	

    /** Returns a list of all tasks in the TaskList whetever
     * their current states. The iterator returned is not thread-safe and
     * so synchronizing (on this TaskList) is advised.
     */
    public Iterator listAllTasks() {
	return map.keySet().iterator();
    } 
   
    /** Override to skip the execution of the specified Task:
     * - This can be used to allow the TaskList to proceed even if a Task
     * fails for some reason. This method effectively performs the converse
     * of block() - it permits Tasks which would otherwise block the
     * execution from stopping it.
     */
    public void skip(Task task) {
	if (map.containsKey(task)) 
	    ((TaskInfo)map.get(task)).setState(TaskInfo.SKIPPED);	
    }
    
}

/** $Log: TaskList.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.4  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.3  2001/06/08 16:27:27  snf
/** Added new methods for PArallel impl.
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */
