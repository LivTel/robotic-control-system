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
/** Factory for generating ProtocolImplementors for the TOOP Control 
 * protocol.
 * <br><br>
 * $Id: TOC_ProtocolImplFactory.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOC_ProtocolImplFactory implements ProtocolImplFactory {

    /** Singleton instance of this class.*/
    private static TOC_ProtocolImplFactory instance = null;

    /** @return The singleton instance of TOC_ProtocolImplFactory.*/
    public static TOC_ProtocolImplFactory getInstance() {
	if (instance == null)
	    instance = new TOC_ProtocolImplFactory();
	return instance;
    }

    /** Private constructor to generate singleton instance.*/
    private TOC_ProtocolImplFactory() {}


    /** @return A JMS(MA) client-end protocol implementor.
     * @param client A generic client - implementors may make more specific.
     * @param connection The connection used for communication with a server.*/
    public ProtocolImpl createClientImpl(Client client, IConnection connection) {
	return null;
    }

    /** @return A JMS(MA) server-end protocol implementor.
     * @param connection The connection used for communication with a client.
     * @param factory An appropriate factory for producing RequestHandlers once a
     * specific command/request has been received.*/
    public ProtocolImpl createServerImpl(IConnection connection, RequestHandlerFactory factory) {
	return new TOC_ProtocolServerImpl(connection, factory);
    }

}

/** $Log: TOC_ProtocolImplFactory.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/09/03 14:52:03  snf
/** Initial revision
/**
/** Revision 1.2  2000/12/06 09:35:55  snf
/** Made singleton methods.
/**
/** Revision 1.1  2000/12/04 17:23:54  snf
/** Initial revision
/** */
