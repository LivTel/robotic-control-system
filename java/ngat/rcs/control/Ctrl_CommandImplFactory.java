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

import ngat.net.*;
import ngat.net.camp.*;
import ngat.message.base.*;
import ngat.message.GUI_RCS.*;

/**
 * Factory for generating handlers for Control commands received from theadmin
 * user. This class can only be used via the singleton pattern, by calling the
 * static method getInstance(). A typical use might be as follows:-
 * 
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = ISS_CommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(someProtocolImpl, someCommand);
 *    ..
 *    ..
 * </pre>
 * 
 * <br>
 * <br>
 * $Id: Ctrl_CommandImplFactory.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 */
public class Ctrl_CommandImplFactory implements CAMPRequestHandlerFactory {

	private static Ctrl_CommandImplFactory instance = null;

	public static Ctrl_CommandImplFactory getInstance() {
		if (instance == null)
			instance = new Ctrl_CommandImplFactory();
		return instance;
	}

	/**
	 * Selects the appropriate handler for the specified command. May return
	 * <i>null</i> if the ProtocolImpl is not defined or not an instance of
	 * JMSMA_ProtocolServerImpl or the request is not defined or not an instance
	 * of CTRL_TO_RCS.
	 */
	public CAMPRequestHandler createHandler(IConnection connection, COMMAND command) {

		// Deal with undefined and illegal args.
		if (connection == null)
			return null;

		if ((command == null) || !(command instanceof GUI_TO_RCS))
			return null;

		// Cast to correct subclass.
		GUI_TO_RCS guicmd = (GUI_TO_RCS) command;

		if (guicmd instanceof GET_STATUS)
			return new GET_STATUSImpl(connection, guicmd);
		else if (guicmd instanceof GET_STATE_MODEL)
			return new GET_STATE_MODELImpl(connection, guicmd);
		else if (guicmd instanceof GET_SEEING) 
			return new GET_SEEINGImpl(connection, guicmd);
		else if (guicmd instanceof GRAB_LOG)
			return new GRAB_LOGImpl(connection, guicmd);
		else if (guicmd instanceof START)
			return new STARTImpl(connection, guicmd);
		else if (guicmd instanceof ID)
			return new IDImpl(connection, guicmd);
		else if (guicmd instanceof SYSTEM)
			return new SYSTEMImpl(connection, guicmd);
		else if (guicmd instanceof SWITCH_MODE)
			return new SWITCH_MODEImpl(connection, guicmd);
		else if (guicmd instanceof TELEMETRY)
			return new TELEMETRYImpl(connection, guicmd);
		else if (guicmd instanceof SEND_EVENT)
			return new SEND_EVENTImpl(connection, guicmd);
		else if (guicmd instanceof SEND_LOG)
			return new SEND_LOGImpl(connection, guicmd);
		else if (guicmd instanceof CIL_STATE)
			return new CIL_STATEImpl(connection, guicmd);
		else if (guicmd instanceof ISS_SET_HEADERS)
			return new ISS_SET_HEADERSImpl(connection, guicmd);
		else if (guicmd instanceof SEND_RCI)
			return new SEND_RCIImpl(connection, guicmd);
		else if (guicmd instanceof SET_SEEING)
			return new SET_SEEINGImpl(connection, guicmd);
		else if (guicmd instanceof SET_EXTINCTION)
			return new SET_EXTINCTIONImpl(connection, guicmd);
		else if (guicmd instanceof GET_VERSION)
			return new GET_VERSIONImpl(connection, guicmd);

		return new UNKNOWNImpl(connection, guicmd);

	}

	/** Private contructor for singleton instance. */
	private Ctrl_CommandImplFactory() {
	}

}

/**
 * $Log: Ctrl_CommandImplFactory.java,v $ /** Revision 1.1 2006/12/12 08:26:29
 * snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:33:59 snf /**
 * Initial revision /** /** Revision 1.2 2001/04/27 17:14:32 snf /** backup /**
 * /** Revision 1.1 2001/03/15 16:04:21 snf /** Initial revision /** /**
 * Revision 1.1 2000/12/14 11:53:56 snf /** Initial revision /** /** Revision
 * 1.1 2000/12/07 16:51:05 snf /** Initial revision /**
 */
