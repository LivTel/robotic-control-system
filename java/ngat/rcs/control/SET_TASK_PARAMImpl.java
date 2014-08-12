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
package ngat.rcs.control;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

import java.io.*;
import java.util.*;

/** Sets a task config parameter dynamically. 
 * This is not persistant - will revert to configured value on restart of RCS.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: SET_TASK_PARAMImpl.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/control/RCS/SET_TASK_PARAMImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SET_TASK_PARAMImpl extends CtrlCommandImpl {

    public SET_TASK_PARAMImpl(IConnection connection, GUI_TO_RCS command) {
	super(connection, command);
    }

    public void handleRequest() {

	SET_TASK_PARAM stp = (SET_TASK_PARAM)command;

	SET_TASK_PARAM_DONE done = new SET_TASK_PARAM_DONE(command.getId());

	String taskClassName = stp.getTaskName();

	if (taskClassName == null) {
	    sendError(done, 777, "(Temp code) No task class specified");
	    return;
	}
	
	TaskConfigRegistry reg = ParallelTaskImpl.getTaskConfigRegistry();
	if (reg == null) {
	    sendError(done, SET_TASK_PARAM.MISSING_RESOURCE, "TaskConfigRegistry not found");
	    return;
	}
	
	ConfigurationProperties config = reg.getTaskConfig(taskClassName);
	if (config == null) {
	    sendError(done, SET_TASK_PARAM.UNKNOWN_TASK, "No config for "+taskClassName);
	    return;
	}

	String key = stp.getParam();
	if (key == null) {
	    sendError(done, 777, "(Temp code) No param specified");
	    return;
	}

	String value = stp.getValue();	
	if (value == null) {
	    sendError(done, 777, "(Temp code) No value specified");
	    return;
	}

	config.setProperty(key, value);

	done.setSuccessful(true);
	sendDone(done);

    }

}
