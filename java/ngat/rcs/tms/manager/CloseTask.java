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
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/** This Task manages the closure of the dome and other mechanisms
 * as a result of an THREAT ALERT received by the RCS Controller.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: CloseTask.java,v 1.1 2006/12/12 08:28:54 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/manager/RCS/CloseTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class CloseTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, CLOSE = 900
	
    int recCount;

    Enclosure_Task   enclosureTask;
   
    MirrorCover_Task mirrorCoverTask;

    DarkSlideTask    darkSlideTask;

    boolean doDarkSlide = false;
    
    int std = 0;
  
    static final SimpleDateFormat sd1 = new SimpleDateFormat("yyyyMMdd");
     
    /** Create an CloseTask.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public CloseTask(String name,
		     TaskManager manager) {
	super(name, manager);	
    }

    /** Reset anything which might be changed by preInit().*/  
    @Override
	public void reset() {
	super.reset();
	doDarkSlide = false;
    }

    @Override
	public void onSubTaskFailed(Task task) {
	super.onSubTaskFailed(task); 
	//if (((JMSMA_TaskImpl)task).getRunCount() < 3) {
	//  resetFailedTask(task);
	//} else {
	failed(640901, "Temporary fail close operation due to subtask failure.."+task.getName(), null);
	//}	
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
    }

    /** Overridden to carry out specific work before the init() method is called.*/
   @Override
public void preInit() {
	super.preInit();

	doDarkSlide  = (config.getProperty("close.dark.slide") != null);

   }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(WARNING, 1, CLASS, name, "onInit",
		    "Starting closure task");
    }
    
    /** Creates the TaskList for this TaskManager. */
    @Override
	protected TaskList createTaskList() {
	
	// ENCLOSURE
	enclosureTask   = new Enclosure_Task(name+"/ENC_CLOSE", 
					     this, 
					     ENCLOSURE.BOTH, 
					     ENCLOSURE.CLOSE);
	taskList.addTask(enclosureTask);
	
	// MIRRCOVER
	mirrorCoverTask = new MirrorCover_Task(name+"/MIR_COVER_CLOSE", 
					       this, 
					       MIRROR_COVER.CLOSE);
	
	taskList.addTask(mirrorCoverTask);

	// DARKSLIDE
	if (doDarkSlide) {
	    darkSlideTask = new DarkSlideTask(name+"/DARK_SLIDE_CLOSE",
					      this,
					      DARKSLIDE.CLOSE);
	    taskList.addTask(darkSlideTask);
	}

	return taskList;
    }
 
}

/** $Log: CloseTask.java,v $
/** Revision 1.1  2006/12/12 08:28:54  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:38  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
