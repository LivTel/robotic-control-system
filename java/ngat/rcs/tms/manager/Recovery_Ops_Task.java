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
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
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
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;
import java.io.*;

/** This Task creates a series of subTasks to carry out the
 * required Recovery plan.
 *
 * ###### This Agent needs some serious upgrading. ######
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Recovery_Ops_Task.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/Recovery_Ops_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Recovery_Ops_Task extends DefaultModalTask implements EventSubscriber, Logging {

    protected static final String CLASS = "RecoveryMCA";

    protected static Recovery_Ops_Task instance;
       
    /** Standard date format.*/
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    SortedMap jobs;

    Job currentJob;

    /** Create a Recovery_Ops_Task using the supplied settings.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public Recovery_Ops_Task(String name,
			      TaskManager manager) {
	super(name, manager);		
	jobs = new TreeMap();
	currentJob = null;
    }
    
    /** Creates the initial instance of the Recovery_Ops_Task
     */
    @Override
	public void initialize(ModalTask mt) {
	instance = (Recovery_Ops_Task)mt;	
	EventRegistry.subscribe("RATCAM_OFFLINE",      instance);
	EventRegistry.subscribe("SUPIRCAM_OFFLINE",    instance);
	EventRegistry.subscribe("MESSPEC_OFFLINE",     instance);
	EventRegistry.subscribe("NUVSPEC_OFFLINE",     instance);
	EventRegistry.subscribe("OSS_OFFLINE",         instance);

	EventRegistry.subscribe("AZ_LIMIT_SOFT_ALERT", instance);
	EventRegistry.subscribe("AZ_LIMIT_HARD_ALERT", instance);
	EventRegistry.subscribe("AZ_LIMIT_CLEAR",      instance);

	EventRegistry.subscribe("ALT_LIMIT_SOFT_ALERT",instance);
	EventRegistry.subscribe("ALT_LIMIT_HARD_ALERT",instance);
	EventRegistry.subscribe("ALT_LIMIT_CLEAR",     instance);

	EventRegistry.subscribe("ROT_LIMIT_SOFT_ALERT",instance);
	EventRegistry.subscribe("ROT_LIMIT_HARD_ALERT",instance);
	EventRegistry.subscribe("ROT_LIMIT_CLEAR",     instance);

	// ##TEMP for Debugging...
	EventRegistry.subscribe("RATCAM_PSU_LOW_VOLTAGE_PLUS_ALERT", instance);
	EventRegistry.subscribe("RATCAM_PSU_LOW_VOLTAGE_MINUS_ALERT",instance);
	EventRegistry.subscribe("RATCAM_PSU_HIGH_VOLTAGE_ALERT",     instance);
	EventRegistry.subscribe("RATCAM_DEWAR_TEMPERATURE_ALERT"  ,  instance);  
	EventRegistry.subscribe("RATCAM_UTE_BOARD_TEMPERATURE_ALERT",instance);

    }
    
    /** Returns a reference to the singleton instance.*/
    public static ModalTask getInstance() {
	return instance;
    }
    
    /** Configure from File. Does nothing at present will be used to:-
     * Set timelimits etc.
     *
     *
     * @param file Configuration file.
     * @exception IOException If any problem occurs reading the file or does not exist.
     * @exception IllegalArgumentException If any config information is dodgy.
     */    
    @Override
	public void configure(File file) throws IOException, IllegalArgumentException {}

    /** Creates the TaskList for this TaskManager. 
     */
    @Override
	protected TaskList createTaskList() {		
	return taskList;
    }  
    
    /** Overriden to return the time at which this ModalControlAgent will next request
     * control.
     * ##### CURRENTLY FAKED TO RETURN NOW ########
     * @return Time when this MCA will next want/be able to take control (millis 1970).
     */
    @Override
	public long demandControlAt() { 
	ObsDate obsDate = RCS_Controller.getObsDate();
	long    now     = System.currentTimeMillis();
	return now;
    }

    @Override
	public Task getNextJob() {		
	if (!jobs.isEmpty()) {
	    Job job = (Job)jobs.get(jobs.firstKey());
	    currentJob = job;
	    Task nextTask = job.getRecoveryTask();
	    if (nextTask == null) {
		jobs.remove(jobs.firstKey());
		currentJob.clearAttempts();
	    }
	    return  job.getRecoveryTask();
	}
	return null;
    }
    
    /** Overridden to handle recovery from failure of a subTask. 
     * May send a new event off.
     * @param task The subTask which has failed.
     */
    @Override
	public void onSubTaskFailed(Task task) {	
	super.onSubTaskFailed(task);
	// ### FOR NOW - RUNS N-Times ONLY ###
	currentJob.incAttempts();
	if (currentJob.getAttempts() == 5) {	
	    jobs.remove(jobs.firstKey());
	    currentJob.clearAttempts();
	} 
    }
    
    /** Overridden to handle completion of a subTask. 
     * Generally resets the recoveryInProgress flag to off.
     * @param task The subTask which has done.
     */
    @Override
	public void onSubTaskDone(Task task){
	super.onSubTaskDone(task); 
	jobs.remove(jobs.firstKey());
	currentJob.clearAttempts();
    }
    

    /** Overridden to carry out specific work after the init() method is called.
     * Sets a number of FITS headers and subscribes to any required events.
     */
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(1, CLASS, name, "onInit", 
		    "\n***********************************"+
		    "\n** Recovery Agent is initialized **"+
		    "\n***********************************\n");
	opsLog.log(1, "Starting Recovery-Operations Mode.");
	FITS_HeaderInfo.current_TELMODE.setValue("RECOVERY");		
    }
    
    @Override
	public void onCompletion() {
	super.onCompletion();
	opsLog.log(1, "Completed Recovery-Operations Mode.");
    }

    /** Override to return <i>true</i> if any Recovery request signal has been received.
     */
    @Override
	public boolean acceptControl() { 	
	taskLog.log(1, CLASS, 
		    "AcceptControl:: There are "+jobs.size()+" Recovery Operations pending.");
	return (jobs.size() != 0);
    } 
    
    /** Overridden to carry out subclass-specific abort processing prior to aborting subtasks.
     */
    @Override
	public void onAborting() {
	super.onAborting();
    }

     /** Overridden to carry out subclass-specific processing on disposal.
      * Unsubscribe all events.
      */
    @Override
	public void onDisposal() {
	super.onDisposal();	
    }
  
    
    /** EventSubscriber method. <br>    
     */
    @Override
	public void notifyEvent(String eventId, Object data) {
	taskLog.log(1, CLASS, name, "notifyEvent",  
		    "** Event_Notification ["+eventId+"] **");	

	ObsDate obsDate = RCS_Controller.getObsDate();
	long    now     = System.currentTimeMillis();

	if (obsDate.isNight(now)) {
	    // NIGHT - We need to try and recover stuff.
	    if (eventId.equals("RATCAM_OFFLINE")) {	  
		jobs.put("RATCAM", new Job("RATCAM"));
	    } else if
		(eventId.equals("SUPIRCAM_OFFLINE")) {  
		jobs.put("SUPIRCAM", new Job("SUPIRCAM"));
	    } else if
		(eventId.equals("MESSPEC_OFFLINE")) {	
		jobs.put("MESSPEC", new Job("MESSPEC"));    
	    } else if
		(eventId.equals("NUVSPEC_OFFLINE")) {
		jobs.put("NUVSPEC", new Job("NUVSPEC"));
	    } else if
		(eventId.equals("OSS_OFFLINE")) { 
		jobs.put("OSS_COMMAND", new Job("OSS_COMMAND"));
	    } else if
		(eventId.equals("AZ_LIMIT_SOFT_ALERT")) { 
		jobs.put("AZ_LIMIT", new Job("AZ_LIMIT"));   
	    } else if
		(eventId.equals("AZ_LIMIT_CLEAR")) {	 
		jobs.remove("AZ_LIMIT");
	    }
	} else {
	    // DAY - We just dont care.
	}
	
	
	
    }
    
    private void warn(String text) {
	System.err.println("\n------------------------------------------------------------------"+
			   "\n| "+text+
			   "\n------------------------------------------------------------------\n");
    }

    /** EventSubscriber method.
     */
    @Override
	public String getSubscriberId() { return name; } 

    /** Encapsulates information about a recovery job.*/
    class Job {

	private String jobId;

	private int attempts;

	Job(String jobId) {
	    this.jobId = jobId;
	    attempts = 0;
	}

	public Task getRecoveryTask() {
	    if (jobId.equals("RATCAM")) {
		String hfx = RCS_Controller.getObsDate().getDateStamp();
		return  new ScriptTask("RECOVERY-RATCAM-(ICSD_REBOOT)", 
				       Recovery_Ops_Task.this, 
				       "scripts/test/send_icsd_reboot",
				       hfx);		
	    } else if
		(jobId.equals("SUPIRCAM")) {
		return new Reboot_Task("REC_REBOOT_L4", Recovery_Ops_Task.this, "SUPIRCAM", 3);
	    } else if
		(jobId.equals("MESSPEC")) {
		return new Reboot_Task("REC_REBOOT_L4", Recovery_Ops_Task.this, "MESSPEC", 3);
	    } else if
		(jobId.equals("NUVSPEC")) {
		return new Reboot_Task("REC_REBOOT_L4", Recovery_Ops_Task.this, "NUVSPEC", 3);
	    } else if
		(jobId.equals("OSS_COMMAND")) {
		return null; // for now..
	    } else if
		(jobId.equals("AZ_LIMIT")) {
		return new UnwrapTask("REC_UNWRAP_AZ", Recovery_Ops_Task.this, UNWRAP.AZIMUTH);
	    } else
		return null;
	}

	public void incAttempts()   { attempts++; }

	public void clearAttempts() { attempts = 0; }

	public int  getAttempts() { return attempts; }

    } // Job.

}

/** $Log: Recovery_Ops_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:54  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:38  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
