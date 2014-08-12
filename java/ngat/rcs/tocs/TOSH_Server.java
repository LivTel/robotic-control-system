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

import ngat.net.*;
import java.io.*;

/** Implementation of the TOSH server for handling commands sent by
 * the Gamma-Ray-Burst Coordinates Network (GCN) relay server at ARI.
 * <br><br>
 * $Id: TOSH_Server.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOSH_Server extends SocketServer {

    /** The single instance of TOSH_Server.*/
    private static TOSH_Server instance = null;

    /** Create an TOSH_Server bound to the specified port. <br>
     * This should have been defined in an RCS configuration file 
     * as <b>rcs.tosh.port</b>.
     * @param port The port to bind to.
     */
    private TOSH_Server(int port) throws IOException {
	super(port);
	rhFactory = TOSH_CommandImplFactory.getInstance();
	piFactory = JMSMA_ProtocolImplFactory.getInstance();
    }
    
    /** Create the single instance of the TOSH server. If the
     * single instance already exists returns silently. 
     * @param port The port to listen on.
     * @exception IOException If the ServerSocket fails to bind 
     * to the specified port for any reason.*/
    public static void bindInstance(int port) throws IOException {
	if (instance == null)
	    instance = new TOSH_Server(port);
    }
    

    /** Starts up the server. Just calls start() on its execution
     * thread.*/
    public static void launch() {
	instance.start();
    }

    /** @return The single instance of TOSH_Server. If no instance
     * has yet been created will return null.*/
    public static TOSH_Server getInstance() {
	return instance;
    }
	
}

/** $Log: TOSH_Server.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/**
/** Revision 1.1  2001/03/15 15:11:43  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */
