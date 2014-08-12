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
import ngat.util.*;
import ngat.util.logging.*;

/** A Thread for executing Tasks. All access to the Tasks are made via
 * their TaskMonitors.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskWorker.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskWorker.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskWorker extends ControlThread {

    protected static final String CLASS = "TaskWorker";
    
    /** Holds the Task currently being executed.*/
    protected Task task;

    /** True if this is a resume rather than a startup.*/
    boolean resuming;

    /** Incrementing counter for identifying Worker threads. */
    protected static int threadNumber = 0;

    /** Task logging.*/
    protected Logger taskLog;

    /** Create a TaskWorker with the specified name.
     * @param name The name/id for this Worker.*/
    public TaskWorker(String taskName, Task task) {
	super("-(*XT-"+(threadNumber++)+"*)", false);
	taskLog = LogManager.getLogger("TASK");
	// E.g. Control/Startup_Tk-(*XT-213*)
	this.task = task;
	resuming = false;
    }
    
    /** Set up this Worker.*/
    @Override
	protected void initialise() {
	
    }
    
    /** Carry out the main processing operations. i.e tell the Task to
     * execute. */
    @Override
	protected void mainTask() {
	if (resuming) {
	    taskLog.log(3, CLASS, getName(), "main", "Worker "+getName()+" Resuming task");
	    task.resume();
	} else {
	    taskLog.log(3, CLASS, getName(), "main", "Worker "+getName()+" Beginning task: "+task.getName());
	    task.perform();
	}
    }

    /** Shutdown the thread and clear up.*/
    @Override
	protected void shutdown() {
	
    }

    /** Attempt to execute the specified Task.
     * @param task The Task to execute.*/
    public void beginJob() {
	resuming = false;
	start();
    }
    
    /** Attempt to restart the specified Task.
     * @param task The Task to execute.*/
    public void resumeJob() {
	resuming = true;
	start();
    }



    
}

/** $Log: TaskWorker.java,v $
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
/** Modified for use with parallel tasks.
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */ 
