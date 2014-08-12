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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import ngat.net.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

/** Factory for generating handlers for POS commands received from
 * the Planetarium Control System.
 * This class can only be used via the singleton pattern, by calling
 * the static method getInstance(). A typical use might be as follows:-
 * <pre>
 *    ..
 *    ..
 *    RequestHandlerFactory factory = POS_CommandImplFactory.getInstance();
 *    RequestHandler handler = factory.createHandler(someProtocolImpl, someCommand);
 *    ..
 *    ..
 * </pre>
 * <br><br>
 * $Id: POS_CommandImplFactory.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 */
public class POS_CommandImplFactory implements RequestHandlerFactory {

    private static POS_CommandImplFactory instance = null;

    protected static Logger pcaLog = LogManager.getLogger("PCA");

    public static POS_CommandImplFactory getInstance() {
	if (instance == null)
	    instance = new POS_CommandImplFactory();
	return instance;
    }

    /** Selects the appropriate handler for the specified command. 
     * May return <i>null</i> if the ProtocolImpl is not defined or not an
     * instance of JMSMA_ProtocolServerImpl or the request is not
     * defined or not an instance of POS_TO_RCS. */
    public RequestHandler createHandler(ProtocolImpl serverImpl,
					Object request) {
	//pcaLog.log(2, "POS_CIFactory::createHandler: "+serverImpl+":"+ request);
	// Deal with undefined and illegal args.
	if ( (serverImpl == null) ||
	     ! (serverImpl instanceof JMSMA_ProtocolServerImpl) ) return null;
	if ( (request == null)    || 
	     ! (request instanceof POS_TO_RCS) ) return null;
	
	// Cast to correct subclass.
	POS_TO_RCS command = (POS_TO_RCS) request;
	
	pcaLog.log(2, "CreateHandler: Command: "+command);
	
	// Choose an POS_CommandImpl - for now mostly generic.
	if (command instanceof TESTLINK) {
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if
	    (command instanceof CCDSTATUS) { 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if
	    (command instanceof METSTATUS) { 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if
	    (command instanceof TELSTATUS) { 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if
	    (command instanceof GETQUEUE) { 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if
	    (command instanceof ABORT) {	 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);	
	} else if
	    (command instanceof OFFLINE) {	 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);		    
	} else if
	    (command instanceof USERID) {	 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);	
	} else if
	    (command instanceof SET_WINDOWS) {	 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);	
	} else if
	    (command instanceof READ_WINDOWS) {	 
	    return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);		    
	} else if (command instanceof CCDOBSERVE) {
	    return new POS_CCDOBSERVE_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
	} else if (command instanceof CCDPROCESS) {
	    return new POS_CCDPROCESS_CommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);	   
	} else 
	    return null;
	
	//return new POS_GenericCommandImpl((JMSMA_ProtocolServerImpl)serverImpl, command);
    }
    
    /** Private constructor for singleton instance.*/
    private POS_CommandImplFactory() {}
    
}    

/** $Log: POS_CommandImplFactory.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.4  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.3  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.2  2001
/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2001/03/15 15:27:35  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/07 16:51:05  snf
/** Initial revision
/** */
