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
package ngat.rcs.comms;

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
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/**
 * Factory for generating handlers for TCS commands received from the Robotic
 * Control System. This implementation creates subclasses of CIL_ProxyHandler
 * which act as relays to pass the command, via CIL to the TCS. <br>
 * This class can only be used via the singleton pattern, by calling the static
 * method getInstance(). A typical use might be as follows:-
 * 
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = CIL_ProxyCommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(someProtocolImpl, someCommand);
 *    ..
 *    ..
 * </pre>
 * 
 * <br>
 * <br>
 * $Id: CIL_ProxyHandlerFactory.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public class CIL_ProxyHandlerFactory implements RequestHandlerFactory {

	private static CIL_ProxyHandlerFactory instance = null;

	public static CIL_ProxyHandlerFactory getInstance() {
		if (instance == null)
			instance = new CIL_ProxyHandlerFactory();
		return instance;
	}

	/**
	 * Selects the appropriate handler for the specified command. May return
	 * <i>null</i> if the ProtocolImpl is not defined or not an instance of
	 * JMSMA_ProtocolServerImpl or the request is not defined or not an instance
	 * of RCS_TO_TCS.
	 */
	public RequestHandler createHandler(ProtocolImpl serverImpl, Object request) {
		// System.out.println("CIL_HandlerFactory: request to create ProxyHandler: using SImpl:"+
		// serverImpl+" req:"+request+" isa "+request.getClass().getName());
		// Deal with undefined and illegal args.
		if ((serverImpl == null) || !(serverImpl instanceof JMSMA_ProtocolServerImpl))
			return null;
		if ((request == null) || !(request instanceof RCS_TO_TCS))
			return null;

		// Cast to correct subclass.
		RCS_TO_TCS command = (RCS_TO_TCS) request;
		try {
			CIL_ProxyHandler handler = new CIL_ProxyHandler((JMSMA_ProtocolServerImpl) serverImpl, (RCS_TO_TCS) request);

			if (System.getProperty("TCS_MODE").equals("lt_sim")) {
				handler.setTranslatorFactory(LT_Sim_CommandTranslatorFactory.getInstance());
			} else {
				handler.setTranslatorFactory(LT_RGO_TCS_CommandTranslatorFactory.getInstance());
			}
			return handler;
		} catch (Exception e) {
			return null;
		}
	}

	/** Private contructor for singleton instance. */
	private CIL_ProxyHandlerFactory() {
	}

}

/**
 * $Log: CIL_ProxyHandlerFactory.java,v $ /** Revision 1.1 2006/12/12 08:29:13
 * snf /** Initial revision /** /** Revision 1.1 2006/05/17 06:30:59 snf /**
 * Initial revision /** /** Revision 1.3 2001/04/27 17:14:32 snf /** backup /**
 * /** Revision 1.2 2000/12/22 14:40:37 snf /** Backup. /** /** Revision 1.1
 * 2000/12/12 18:50:00 snf /** Initial revision /**
 */
