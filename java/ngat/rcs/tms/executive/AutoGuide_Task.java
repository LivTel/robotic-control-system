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

import ngat.rcs.*;
import ngat.rcs.tms.*;
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
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing a Telescope Autoguide ON or OFF. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AutoGuide_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/AutoGuide_Task.java,v $
 * </dl>Slew
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class AutoGuide_Task extends Default_TaskImpl {

    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    public static final String CLASS = "AutoGuide_Task";

    protected TelescopeConfig agConfig;

    /** Determines whether this is an ON, OFF, SUSPEND or RESUME.*/
    protected int state;

    /** Name of Autoguider mode.*/
    protected String modeName;

    /** Name of Autoguider state.*/
    protected String stateName;
  
    /** Create an AutoGuide_Task using the supplied settings.
     * @param state Whether ON or OFF (also SUSPEND, RESUME).
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public AutoGuide_Task(String          name,
			  TaskManager     manager,
			  TelescopeConfig agConfig,
			  int             state) {
	super(name, manager, "CIL_PROXY");
	this.agConfig = agConfig;
	this.state    = state;

	stateName = "UNKNOWN";
	modeName  = "UNKNOWN";

	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	
	AUTOGUIDE autoguide = new AUTOGUIDE(name);


	switch (state) { 
	case AUTOGUIDE.OFF:
	    stateName = "OFF";
	    modeName  = "N/A";
	    autoguide.setState(AUTOGUIDE.OFF);
	    break;
	case AUTOGUIDE.ON: 
	    stateName = "ON";
	    autoguide.setState(AUTOGUIDE.ON);
	    switch (agConfig.getAutoGuiderStarSelectionMode()) {
	    case TelescopeConfig.STAR_SELECTION_RANK:
		modeName = "RANK "+agConfig.getAutoGuiderStarSelection1();
		autoguide.setMode(AUTOGUIDE.RANK);
		autoguide.setRank(agConfig.getAutoGuiderStarSelection1());
		break;
	    case TelescopeConfig.STAR_SELECTION_RANGE:
		modeName = "RANGE "+agConfig.getAutoGuiderStarSelection1()+
		    " to "+agConfig.getAutoGuiderStarSelection2();
		autoguide.setMode(AUTOGUIDE.RANGE);
		autoguide.setRange1(agConfig.getAutoGuiderStarSelection1());
		autoguide.setRange2(agConfig.getAutoGuiderStarSelection2());
		break;
	    case TelescopeConfig.STAR_SELECTION_PIXEL:
		modeName = "PIXEL @ ("+agConfig.getAutoGuiderStarSelection1()+
		    ", "+agConfig.getAutoGuiderStarSelection2()+")";
		autoguide.setMode(AUTOGUIDE.PIXEL);
		break;
	    default:
		modeName = "MODE:"+agConfig.getAutoGuiderStarSelectionMode();
	    }
	    break;
	case AUTOGUIDE.SUSPEND: 
	    stateName = "SUSPEND"; 
	    modeName  = "N/A";
	    autoguide.setState(AUTOGUIDE.SUSPEND);
	    break;
	case AUTOGUIDE.RESUME: 
	    stateName = "RESUME";
	    modeName  = "N/A";
	    autoguide.setState(AUTOGUIDE.RESUME);
	    break;
	default:
	    stateName = "STATE:"+state;
	}
	
	command = autoguide;
	
    }
    
    
    /** Carry out subclass specific initialization.*/
    @Override
	protected void onInit() {	
	super.onInit();
	logger.log(1, CLASS, name, "onInit",
		   "Starting Autoguider setup to "+stateName+" using mode "+modeName);
    }
    
    /** Carry out subclass specific completion work. ## NONE ##.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	logger.log(1, CLASS, name, "onCompletion",
		   "Autoguider setup completed");
    }
    
    /** Carry out subclass specific disposal work.   ## NONE ##.*/
    @Override
	protected void onDisposal() {
	super.onDisposal();
    }
  
}

/** $Log: AutoGuide_Task.java,v $
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
