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
package ngat.rcs.iss;

import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.util.logging.*;

/** Factory for generating handlers for ISS commands received from
 * the an Instrument Control System. This implementation creates
 * subclasses of ISS_CommandImpl which act as relays to pass the
 * command, suitably transformed into a RCT_TO_TCS to send via the
 * CIL Proxy layer to the TCS. Some of the handlers carry out extra
 * processing to grab information from either the command or from  
 * the response (COMMAND_DONE) returned via CIL Proxy by the TCS.
 * This class can only be used via the singleton pattern, by calling
 * the static method getInstance(). A typical use might be as follows:-
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = ISS_CommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(someProtocolImpl, someCommand);
 *    ..
 *    ..
 * </pre>
 * <br><br>
 * $Id: ISS_CommandImplFactory.java,v 1.2 2007/12/05 14:09:38 snf Exp $
 */
public class ISS_CommandImplFactory implements RequestHandlerFactory {

    private static final String CLASS = "ISS_CommandImplFactory";

    private static ISS_CommandImplFactory instance = null;

    private static Logger issLog = LogManager.getLogger("ISS");

    public static ISS_CommandImplFactory getInstance() {
	if (instance == null)
	    instance = new ISS_CommandImplFactory();
	return instance;
    }

    /** Selects the appropriate handler for the specified command. 
     * May return <i>null</i> if the ProtocolImpl is not defined or not an
     * instance of JMSMA_ProtocolServerImpl or the request is not
     * defined or not an instance of INST_TO_ISS. */
    public RequestHandler createHandler(ProtocolImpl serverImpl,
					Object request) {
	
	// Deal with undefined and illegal args.
	if ( (serverImpl == null) ||
	     ! (serverImpl instanceof JMSMA_ProtocolServerImpl) ) return null;
	if ( (request == null)    || 
	     ! (request instanceof INST_TO_ISS) ) return null;
	
	// Cast to correct subclass.
	COMMAND command = (COMMAND) request;

	issLog.log(2, CLASS, "-", "createHandler",
		   "Building handler for command class: "+		   
		   (command != null ? command.getClass().getName() : "UNKNOWN"));
	
	// Choose an ISS_CommandImpl - for now mostly generic.
	if (command instanceof GET_FITS) 
	    return new ISS_GET_FITS_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	
	if (command instanceof OFFSET_RA_DEC) 
	    return new ISS_OFFSET_RA_DEC_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	
	if (command instanceof OFFSET_FOCUS) {

	    if (command instanceof OFFSET_FOCUS_CONTROL)
		return new ISS_OFFSET_FOCUS_CONTROL_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	    else
		return new ISS_OFFSET_FOCUS_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	}

	if (command instanceof FOCUS_CONTROL)
	    return new ISS_FOCUS_CONTROL_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);

	if (command instanceof OFFSET_X_Y)
	    return new ISS_OFFSET_X_Y_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);

	if (command instanceof OFFSET_ROTATOR) 
	    return new ISS_OFFSET_ROTATOR_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	
	if (command instanceof SET_FOCUS) 
	    return new ISS_SET_FOCUS_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	
	if (command instanceof MOVE_FOLD) 
	    return new ISS_MOVE_FOLD_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	  //return new ISS_MOVE_FOLD_RETRY_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	if (command instanceof AG_STOP) 
	    return new ISS_AG_STOP_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	
	if (command instanceof AG_START) 
	    return new ISS_AG_START_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);

	return null;
    }

    /** Private contructor for singleton instance.*/
    private ISS_CommandImplFactory() {}

}    

/** $Log: ISS_CommandImplFactory.java,v $
/** Revision 1.2  2007/12/05 14:09:38  snf
/** added offsetxy handler check
/**
/** Revision 1.1  2006/12/12 08:30:20  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:28  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/07 16:51:05  snf
/** Initial revision
/** */
