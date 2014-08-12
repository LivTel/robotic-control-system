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
import ngat.message.base.*;

/**
 * Standard TaskImpl representing a JMSMA_TaskImpl.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Default_TaskImpl.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/Default_TaskImpl.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Default_TaskImpl extends JMSMA_TaskImpl {

	/** Constant denoting the typical expected time for a Task to complete. */
	public static final long DEFAULT_TIMEOUT = 60000L;

	/** Create a Default_TaskImpl - unconnected. */
	public Default_TaskImpl(String name, TaskManager manager) {
		super(name, manager);
		CLASS = getClass().getName();
		errorIndicator = new BasicErrorIndicator(0, "OK", null);		
	}

	/**
	 * Create a Default_TaskImpl using specified connectionID. This is the
	 * preferred constructor as it fails with error
	 * JMSMA_TaskImpl.CONNECTION_RESOURCE_ERROR (600001) if the specified cid is
	 * not known to the system.
	 * 
	 * @param name
	 *            The unique name/id for this TaskImpl.
	 * @param manager
	 *            The Task's manager.
	 * @param cid
	 *            The connection resource id.
	 */
	public Default_TaskImpl(String name, TaskManager manager, String cid) {
		super(name, manager, cid);
		CLASS = getClass().getName();
		errorIndicator = new BasicErrorIndicator(0, "OK", null);		
	}

	/** Returns the default time for this command to execute. */
	public static long getDefaultTimeToComplete() {

		return DEFAULT_TIMEOUT;
	}

	/**
	 * Compute the estimated completion time.
	 * 
	 * @return The initial estimated completion time in millis.
	 */
	@Override
	protected long calculateTimeToComplete() {
		return getDefaultTimeToComplete();
	}

	/**
	 * Reset the task parameters to allow this task to be reinserted into the
	 * taskList.
	 */
	@Override
	public void reset() {
		super.reset();
		logger.log(3, CLASS, name, "reset", "DTask: " + name + ": Resetting");
	}

	/** Carry out subclass specific initialization. ## NONE ## */
	@Override
	protected void onInit() {
		logger.log(3, CLASS, name, "onInit", "DTask: " + name + ": Starting Special initialization");
		
		// log valid start time - may be already done		
	}

	/** Carry out subclass specific completion work. ## NONE ##. */
	@Override
	protected void onCompletion(COMMAND_DONE response) {

		logger.log(3, CLASS, name, "onCompletion", "Returned "
				+ (response.getSuccessful() ? "Successful response: " + response : "Failure: " + response
						+ ", ErrorCode=" + response.getErrorNum() + ", Message=" + response.getErrorString()));

		// log valid completion time
		//logg	"EXEC_TIME for "+CLASS+" : "+(System.currentTimeMillis()-startTime));
		
		logExecutionStatistics();
		
	}

	/** Log execution stats.
	 *  Subclasses may override to log additional information
	 */
	protected void logExecutionStatistics() {
		logger.log(3, CLASS, name, "onCompletion",
				"EXEC_TIME for "+CLASS+" : "+(System.currentTimeMillis()-startTime));
	}

	/** Carry out subclass specific disposal work. ## NONE ##. */
	@Override
	protected void onDisposal() {
		logger.log(3, CLASS, name, "onDisposal", "DTask: " + name + ": Starting Special disposal");
	}

	/** Carry out subclass specific exception handling. ## NONE ## */
	@Override
	public void exceptionOccurred(Object source, Exception e) {
		logger.log(1, CLASS, name, "NET exception-handler", "JMS(MA) Exception handler: " + "Source: " + source
				+ "Exception: " + e);
		logger.dumpStack(1, e);
		super.exceptionOccurred(source, e);
	}

	public void sendCommand(COMMAND command) {
	}

}

/**
 * $Log: Default_TaskImpl.java,v $ /** Revision 1.1 2006/12/12 08:28:27 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:33:16 snf /** Initial
 * revision /** /** Revision 1.2 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.1 2001/06/08 16:27:27 snf /** Initial revision
 * /**
 */
