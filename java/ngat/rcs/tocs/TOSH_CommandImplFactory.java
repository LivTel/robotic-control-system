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
package ngat.rcs.tocs;

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
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;

/** Factory for generating handlers for TOSH commands received from
 * the Gamma-Ray-Burst Coordinates Network (GCN).
 * This class can only be used via the singleton pattern, by calling
 * the static method getInstance(). A typical use might be as follows:-
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = TOSH_CommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(someProtocolImpl, someCommand);
 *    ..
 *    ..
 * </pre>
 * <br><br>
 * $Id: TOSH_CommandImplFactory.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOSH_CommandImplFactory implements RequestHandlerFactory {

    private static TOSH_CommandImplFactory instance = null;

    public static TOSH_CommandImplFactory getInstance() {
	if (instance == null)
	    instance = new TOSH_CommandImplFactory();
	return instance;
    }

    /** Selects the appropriate handler for the specified command. 
     * May return <i>null</i> if the ProtocolImpl is not defined or not an
     * instance of JMSMA_ProtocolServerImpl or the request is not
     * defined or not an instance of TOSH_TO_RCS. */
    public RequestHandler createHandler(ProtocolImpl serverImpl,
					Object request) {
	
	// Deal with undefined and illegal args.
	if ( (serverImpl == null) ||
	     ! (serverImpl instanceof JMSMA_ProtocolServerImpl) ) return null;
	if ( (request == null)    || 
	     ! (request instanceof COMMAND) ) return null;
	
	// Cast to correct subclass.
	COMMAND command = (COMMAND) request;
	
	// Choose an TOSH_CommandImpl - for now mostly generic.
	
	return null;
    }

    /** Private contructor for singleton instance.*/
    private TOSH_CommandImplFactory() {}

}    

/** $Log: TOSH_CommandImplFactory.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2001/03/15 16:07:12  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/07 16:51:05  snf
/** Initial revision
/** */
