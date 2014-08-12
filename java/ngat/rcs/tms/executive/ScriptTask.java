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

import ngat.rcs.tms.*;
import ngat.util.logging.*;

import java.io.*;

import javax.swing.tree.*;
/** ScriptTask.
 * This class represents a Wrapper to allow shell scripts to be run as standard Tasks
 * by a TaskManager. These are intended for operations which may need to be configured 
 * from day to day without the need to recompile the RCS source distribution. The scripts 
 * could also be (re)-written without stopping the RCS.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: ScriptTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/ScriptTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class ScriptTask implements Task, Logging {

    protected final String CLASS = "ScriptTask";

    protected TaskManager manager;
    
    protected Thread  worker;

    protected ErrorIndicator errorIndicator;

    protected String name;

    protected String histFileExt;

    protected volatile boolean aborting;
    protected volatile boolean started;
    protected volatile boolean initialized;
    protected volatile boolean aborted;
    protected volatile boolean failed;
    protected volatile boolean done;
    protected volatile boolean suspended;
    
    /** Path to the Script to execute.*/
    protected String script;

    /** Process to execute the script.*/
    protected Process process;
 
    long delay;

    Logger taskLog;
    
    Logger errorLog;

    Logger traceLog;

    public static final int PROCESS_FAILURE   = 301;

    public static final int INTERRUPT_FAILURE = 302;

    public static final int EXECUTION_FAILURE = 303;

    private TaskDescriptor descriptor;
    
    public ScriptTask(String name, TaskManager manager, String script, String histFileExt) {    
	this.name        = name;
	this.manager     = manager;
	this.script      = script;
	this.histFileExt = histFileExt;
	descriptor = new TaskDescriptor(name, getClass().getSimpleName());
	errorIndicator = new BasicErrorIndicator();
	taskLog  = LogManager.getLogger("TASK");
	errorLog = LogManager.getLogger("ERROR");
	traceLog = LogManager.getLogger("TRACE");	
    }

    /** Returns a snapshot of this Task.*/
    public void snapshot(StringBuffer buffer, int level) {
	buffer.append("\nScript: "+script);
    }
    
    /** Returns a snapshot of this Task's state and other information.
     * @param node A TreeNode wherein to put the information. 
     */
    public void snapshot(TreeNode node) {
	
    }

    /** Set the startup delay.*/
    public void setDelay(long delay) { this.delay = delay; }

    /** Called by the Task's manager to prepare the Task for imminent execution.
     */
    public void init() {
	taskLog.log(3, CLASS, name, "init", "Initialized");
	initialized = true;	
    }

    /** Called by the Task's manager to cause the Task to carry out its defined
     * set of operations. 
     */
    public void perform() {
	worker = Thread.currentThread();
	
	try {Thread.sleep(delay);} catch (InterruptedException ix) {}

	// Create a Process for the script and execute it.
	try {
	    process = Runtime.getRuntime().exec(script+" "+((Task)manager).getName()+" "+name+" "+histFileExt);
	    //
	    // Call Script.csh <MgrID> <TaskID>	    
	    //
	    new Grabber(process.getErrorStream()).start();
	    started = true;
	    taskLog.log(3, CLASS, name, "exec", "Process running");
	} catch (IOException iox) { 
	    failed = true; 
	    errorLog.log(1, CLASS, name, "exec", "Process could not initialize: "+iox);
	    errorIndicator.setErrorCode(PROCESS_FAILURE);
	    errorIndicator.setErrorString("Failed to create process for: "+script);
	    errorIndicator.setException(iox);
	    manager.sigTaskFailed(this);
	    return;
	}
	
	// Wait for the Script to complete or fail.
	try {
	    process.waitFor();    
	} catch (InterruptedException ix) { 
	    failed = true; 
	    errorLog.log(1, CLASS, name, "exec", "Process interrupted:");
	    errorIndicator.setErrorCode(INTERRUPT_FAILURE);
	    errorIndicator.setErrorString("Interrupted running process for: "+script);
	    errorIndicator.setException(ix);
	    manager.sigTaskFailed(this);
	    return;
	}

	int status = process.exitValue();
	
	// Response.
	if (aborting) {
	    taskLog.log(3, CLASS, name, "exec", "Process aborted: Exit status: "+status);
	    aborted = true;
	    manager.sigTaskAborted(this);
	    return;
	}
	
	if (status == 0) {
	    taskLog.log(3, CLASS, name, "exec", "Process completed: Exit status: "+status);
	    done = true;
	    manager.sigTaskDone(this);
	    return;
	} else {
	    failed = true;
	    errorLog.log(1, CLASS, name, "exec", "Process failed: Exit status: "+status);
	    errorIndicator.setErrorCode(EXECUTION_FAILURE);
	    errorIndicator.setErrorString("Process failed with exit status: "+status+" for: "+script);
	    errorIndicator.setException(null);
	    manager.sigTaskFailed(this);
	    return;
	}

    }

    /** Called by the Task's manager to force the Task to abandon its execution.
     */
    public void abort() {
	aborting = true;
	taskLog.log(3, CLASS, name, "abort", "Sending KILL sugnal to process:");
	process.destroy();
    }
    
    /** Called by the Task's manager to cause the Task to abandon its execution but
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
    public void stop(long timeout) {}
    
    /** Called by the Task's manager to cause the Task to clear up its resources and
     * prepare to be killed off. A Task need not be killed off by the manager as
     * soon as it has completed its execution - there may be useful data stored
     * in it.*/
    public void dispose() {
	taskLog.log(3, CLASS, name, "dispose", "Disposing process: ");
	process = null;	
    }

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
    public void reset() { // Not feasible as a rule.
    }

    /** Cause the Task to suspend operation - typically => stop after the next 
     * subtask has completed.*/
    public void suspend() {}

    /** Restart the Task after a suspend() - typically => start the next subtask.*/
    public void resume() {}
    
    /** Returns true if this Task is allowed to be aborted - most are.*/
    public boolean canAbort() { return true; }

    /** Returns the current aborted state of this Task.
     * @return True if this Task is aborted.*/
    public boolean isAborted() { return aborted; }

    /** Returns the current completion state of this Task.
     * @return True if this Task is done.*/
    public boolean isDone() { return done; }
    
    /** Returns the current error state of this Task.
     * @return True if this Task has failed.*/
    public boolean isFailed() { return failed; }
    
    /** Returns the current suspension state of this Task.
     * @return True if this Task is suspended.*/
    public boolean isSuspended() { return suspended; }
    
    /** Returns the current initialization state of this Task.
     * This should return true if the init() method has been called.*/
    public boolean isInitialized() { return initialized; }

    /** Returns the current execution state of this Task.
     * This should return true if the exec_task() method has been called.*/ 
    public boolean isStarted() { return started; }

    /** Returns this Task's ErrorIndicator. If the Task has failed this 
     * should have been set.
     * @return This Task's ErrorIndicator.*/
    public ErrorIndicator getErrorIndicator() { return errorIndicator; }
    
    /** Return the name of this Task.
     * @return The name of this Task.*/
    public String getName() { return name; }
    
    /** Returns the task's manager.
     * @return The manager for this task - null if this is the top level Task.*/
    public TaskManager getManager() { return manager; }

    public Thread getWorker() { return worker; }
    
    class Grabber extends Thread {
        InputStream in;
        Grabber(InputStream in) {
            super();
            this.in = in;
        }
	
        @Override
		public void run() {
            int ic = 0;
            try {
                while ((ic = in.read()) != -1) {
                    System.err.write(ic);
                }
            } catch (IOException e) {}
        }
    }

	public TaskDescriptor getDescriptor() {
		return descriptor;
		}

}

/** $Log: ScriptTask.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
