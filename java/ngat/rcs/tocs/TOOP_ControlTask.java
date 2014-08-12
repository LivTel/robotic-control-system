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
import ngat.rcs.tms.manager.*;
import ngat.util.logging.*;
import ngat.message.base.*;

/** This Task is used to control one or more parallel subtasks responsible
 * for configuring instruments and telescope, slewing to source and taking
 * exposures under the control of a TO service Agent (SA). Individual tasks
 * are added to the TOOP_ControlTask using addTask(Task). These do not go
 * straight onto the TaskList but are held until runtime. They are then compiled 
 * into a parallel executing (no constraints) TaskList. It is expected that
 * only single tasks are added for now.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TOOP_ControlTask.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tocs/RCS/TOOP_ControlTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public abstract class TOOP_ControlTask extends ParallelTaskImpl implements Logging {

    public static long ACK_TIMEOUT = 60000L;

    /** Reference to the TO Control Agent.*/
    TOControlAgent tocAgent = (TOControlAgent)TOControlAgent.getInstance();
   
    /** The command implementor.*/
    protected TOC_GenericCommandImpl implementor;

    /** The wrapped Task.*/
    Task subTask;

    /** Flag to indicate this Task has been isssued kill() before it could be run.*/
    protected volatile boolean killed = false;

    /** Holds response data for the client. e.g. filename.*/
    protected StringBuffer completionReply;

    /** Create a TOOP_ControlTask using the supplied settings. 
     * @param implementor The command implemenotr which is handlinng the
     * communications with the client (TOOP-PE)
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public TOOP_ControlTask(String                 name,
			    TaskManager            manager,
			    TOC_GenericCommandImpl implementor) {
	super(name, manager);
	this.implementor = implementor;	
 
	completionReply = new StringBuffer("");	
    }

    /** This method is used to wipe the Task before it is executed in the
     * event of pre-emption or the likes. If the task is already running this
     * method returns immediately with no effect.
     */
    public void kill() {
	if (initialized) return;		
	subTask.dispose();
	killed = true;
	dispose();
    }

    @Override
	public void reset() {
	super.reset();	
	killed   = false;
	completionReply.delete(0, completionReply.length());	
    }
    
    /** Deal with failed subtask - not much we can do really except fail.*/
    @Override
	public void onSubTaskFailed(Task task) {	
	super.onSubTaskFailed(task);	
	failed(task.getErrorIndicator());	
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
	public void onAborting()   {
	super.onAborting();
	//if (exposing)
	//  taskList.addTask(new Abort_Task(name+"-(ExposeAbort)", this, "RATCAM")); /// WHY RAT ????	   
	
    }
    
    /** This is called if we are aborted by manager or fail badly. In this instance we
     * need to warn the client. This method is also called after successful completion
     * after the onCompletion() method. If the Task is killed off before starting then
     * this is also called after the prepared tasks have been disposed of.
     */
    @Override
	public void onDisposal()   {
	super.onDisposal();
	System.err.println("**TOOP: Dispose");
	if (aborted) {
	    String reply = "ERROR ABORTED Code="+abortCode+", message="+abortMessage;
	    if (implementor != null)
		implementor.processReply(reply);
	} else if
	    (failed) {
	    String reply = "ERROR GENERIC Code="+
		errorIndicator.getErrorCode()+", message="+
		errorIndicator.getErrorString()+", exception="+
		errorIndicator.getException();
	    if (implementor != null)
		implementor.processReply(reply);
	} else if
	    (killed) {
	    String reply = "ERROR KILLED";
	    if (implementor != null)
		implementor.processReply(reply);
	}
	implementor = null;
    }

    @Override
	public void onFailure() {
	super.onFailure();
	
    }

    
    /** Overridden to send the appropriate response to the client when the task
     * completes successfully. The field (completionReply) holds any relevant info
     * setup by a successfully completed subtask.
     */
    @Override
	public void onCompletion() {
	super.onCompletion();		
	String reply = "OK "+completionReply.toString();
	implementor.processReply(reply);
    }
    
    /** Overridden to carry out specific work after the init() method is called.
    */
    @Override
	public void onInit() {
	super.onInit();
    }
    
   //  /** Creates the TaskList for this TaskManager. The set of managed tasks will
//      * have already been created by the TOOP_CommandImpl and added to the subtasks 
//      * Vector.
//      */
//     protected TaskList createTaskList(TaskMonitorFactory tmfactory) {	
// 	if (subTask != null)
// 	    taskList.addTask(subTask);
// 	return taskList;
//     }  

   

//     /** Set the specified Task as wrapped task.
//      * The exposing boolean is set if the added task is an Exposure_Task
//      * @param task The Task to manage.
//      */
//     public void setTask(Task task) {
// 	subTask = task;
//     }
    
    /** Adds text to the completion response data.
     * @param data The response data.
     */
    public void concatCompletionReply(String data) { completionReply.append(data); }

    @Override
	public void sigMessage(Task source, int type, Object message) {
        System.err.println("TOCSTask::SigMesg():Type: "+type+" From: "+source+" As: "+message);
	
	switch (type) {
	case JMSMA_TaskImpl.ACK_RECEIVED:
	    // We need to somehow pass this bugger back to the implementor...
	    long acktimeout = ACK_TIMEOUT;
	    if (message instanceof ACK) {
		ACK ack = (ACK)message;		
		acktimeout = ack.getTimeToComplete();
		System.err.println("TOCSTask::SigMesg():ACK with timeout: "+acktimeout);
	    }		
	    if (implementor != null)
		implementor.processAck(acktimeout);	    
	    break;
	default:
	    break;
	}
    }

    
}

/** $Log: TOOP_ControlTask.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/** 
/** Revision 1.1  2001/09/04 08:19:16  snf 
/** Initial revision 
/** */
