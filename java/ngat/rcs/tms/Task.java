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
/** Task is implemented by any classes which perform Task based operations.
 * Implementors use the Task methods to setup and execute some series of 
 * operations. Tasks should set a state variable to indicate their current
 * state using the constants defined below as follows:-
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Task.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface Task {

    /** Called by a Task's manager to prepare the Task for imminent execution.
     * This method is used to carry out last minute setup, just before the
     * exec() method is called. The Task may have been instantiated some time
     * earlier when it might not be appropriate to carry out the init operations
     * e.g. the data used by the init might not be accurate enough or may
     * have been relevant to a different Task. Tasks should generally work out
     * their expected TimeToComplete here so it can be obtained by their manager.*/
    public void init();
    
    /** Called by  a Task's manager to cause the Task to carry out its defined
     * set of operations. A Task may itself be a TaskManager and so might spawn
     * additional (sub) Tasks as part or all of this method call.*/
    public void perform();

    /** Called by a Task's manager to force the Task to abandon its execution
     * and the execution of any sub Tasks it may have spawned. As part of this
     * operation the Task may have to carry out recovery to a previous or 'safe' 
     * state - subtasks would also perform their own recovery actions.*/
    public void abort();
    
    /** Called by a Task's manager to cause a Task to abandon its execution but
     * only after the current subTask (if any) has completed. The manager may 
     * specify a timeout period by which this operation should complete after
     * which, if the Task has not reached a 'safe' state the manager may decide to
     * abort the Task anyway.
     * @param timeout The period by which the Task (and subtasks) should have 
     * reached a 'safe' state. The implementation of the TaskManager may decide
     * to ignore this timeout. There is no specification as to how a Task should
     * propagate this value to subtasks e.g. It may insist the subtask complete
     * within half this period to give the task itself time to clear up or it may 
     * just abort the subtask. A timeout of 0 (zero) is interpreted as no timeout
     * i.e. abandon whenever the current subtask has completed.*/
    public void stop(long timeout);
    
    /** Called by a Task's manager to cause a Task to clear up its resources and
     * prepare to be killed off. A Task need not be killed off by the manager as
     * soon as it has completed its execution - there may be useful data stored
     * in it.*/
    public void dispose();

    /** Called by a Task's manager to cause the Task to revert to its initial state
     * In this state it will be as it was just after construction but prior to being
     * initialized ( by init() ). This method is most likely to be called when a
     * Task has failed and is being rerun. The state variables should be set
     * generally as the following:-
     * <table>
     * <tr><td>done</td>        <td>false</td></tr>
     * <tr><td>failed</td>      <td>false</td></tr>
     * <tr><td>started</td>     <td>false</td></tr>
     * <tr><td>aborted</td>     <td>false?</td></tr>
     * <tr><td>initialized</td> <td>false</td></tr>
     * <tr><td>suspended</td>   <td>unchanged</td></tr>
     * </table>
     *
     * If the Task is also a manager it will most likely want to reset all subtasks
     * also and re-construct its TaskList again.
     */
    public void reset();

    /** Cause the Task to suspend operation - typically => stop after the next 
     * subtask has completed.*/
    public void suspend();

    /** Restart the Task after a suspend() - typically => start the next subtask.*/
    public void resume();
    
    /** Returns true if this Task is allowed to be aborted - most are.*/
    public boolean canAbort();

    /** Returns the current aborted state of this Task.
     * @return True if this Task is aborted.*/
    public boolean isAborted();

    /** Returns the current completion state of this Task.
     * @return True if this Task is done.*/
    public boolean isDone();
    
    /** Returns the current error state of this Task.
     * @return True if this Task has failed.*/
    public boolean isFailed();
    
    /** Returns the current suspension state of this Task.
     * @return True if this Task is suspended.*/
    public boolean isSuspended();
    
    /** Returns the current initialization state of this Task.
     * This should return true if the init() method has been called.*/
    public boolean isInitialized();

    /** Returns the current execution state of this Task.
     * This should return true if the exec_task() method has been called.*/ 
    public boolean isStarted();

    /** Returns this Task's ErrorIndicator. If the Task has failed this 
     * should have been set.
     * @return This Task's ErrorIndicator.*/
    public ErrorIndicator getErrorIndicator();
    
    /** Return the name of this Task.
     * @return The name of this Task.*/
    public String getName();
    
    /** Returns the task's manager.
     * @return The manager for this task - null if this is the top level Task.*/
    public TaskManager getManager();
    
    /** Returns the descriptor for this task.
     * @return The descriptor for this task.*/
    public TaskDescriptor getDescriptor();
    
    /** Returns the thread running this Task if available else null.*/
    public Thread getWorker();

    /** Sets the delay time before this task starts after initialized.*/
    public void setDelay(long delay);

   // /** Provide a snapshot of this task and its subtask hierarchy if any.*/
    //public TaskData snapshot();
    
}

/** $Log: Task.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Added reset().
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */
