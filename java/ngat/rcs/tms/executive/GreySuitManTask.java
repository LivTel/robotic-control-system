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

/** GreySuitMan Task: Does bugger all but sleep.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: GreySuitManTask.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/GreySuitManTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class GreySuitManTask implements Task, Logging {
    
    String name;
    
    TaskManager manager;
    
    private volatile boolean aborted;
    private volatile boolean aborting;
    private volatile boolean suspended;
    private volatile boolean initialized;
    private volatile boolean started;
    private volatile boolean keepAlive;
    private volatile boolean done;
    private volatile boolean failed;

    TaskWorker worker;

    Logger logger;

    long delay;

    public GreySuitManTask(String      name, 
			   TaskManager manager, 
			   long        delay) {
	
	this.name    = name;
	this.manager = manager;

	this.delay   = delay;

	logger = LogManager.getLogger("TASK");

    }
    
    public void init() {
	initialized = true;
	logger.log(1, "GreySuitManTask", name, name+" ** Initializing **");	
    }
    
    /** The GSM Task never completes it just keeps waking up to see if its 
     * had an abort request. Then after the next sleep it signals aborted to its manager.
     */
    public void perform() {	
	worker  = (TaskWorker)Thread.currentThread();
	started = true;
	
	while (true) {
	    try {Thread.sleep(delay);} catch (InterruptedException e){}
	    if (aborting) {			    
		manager.sigTaskAborted(this);
		return;
	    }	
	}
    }

    public void reset(){
	aborting = false;
	suspended = false;
	initialized = false;
	started = false;
	keepAlive = false;
	done = false;
	failed = false;
    }

    public void abort() {
	logger.log(1, "GreySuitManTask", name, name+" ** Aborting **");
	aborting = true;

    } 
    
    public void stop(long timeout) {
	
    }

    public void dispose() {
	logger.log(1, "GreySuitManTask", name, name+" ** Disposing - I'LL BE BACK  **");
    }

    public void suspend() {
	logger.log(1, "GreySuitManTask", name, name+" ** Suspending execution **");	
	suspended = true;
    }

    public void resume() {
	logger.log(1, "GreySuitManTask", name, name+" ** Resuming execution **");	
	suspended = false;
	perform();
    }

    public boolean isAborted() { return aborted; }
    
    public boolean isDone() { return done; }
    
    public boolean isFailed() { return failed; }
    
    public boolean isSuspended() { return suspended; }
    
    /** This should return true if the init() method has been called.*/
    public boolean isInitialized() { return initialized; }
    
    /** Returns the current execution state of this Task.
     * This should return true if the exec_task() method has been called.*/ 
    public boolean isStarted() { return started; }

    public ErrorIndicator getErrorIndicator() { return null; }

    public TaskManager getManager() { return manager; }

    public String getName() { return name; }


    /** Returns true if this Task is allowed to be aborted - most are.*/
    public boolean canAbort() { return true; }
    
    /** Returns the thread running this Task if available else null.*/
    public Thread getWorker() { return worker; }

    /** Sets the delay time before this task starts after initialized.*/
    public void setDelay(long delay) { this.delay = delay; }

	public TaskDescriptor getDescriptor() {
		return null;
	}

}

/** $Log */
