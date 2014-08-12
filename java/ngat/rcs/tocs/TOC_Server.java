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

import java.net.*;
import java.io.*;

/** Implementation of the TOC server for handling commands sent by the
 * Target of Opportunity Program -Processing Engine TOOPPE.
 * <br><br>
 * $Id: TOC_Server.java,v 1.1 2006/12/12 08:32:07 snf Exp $
 */
public class TOC_Server extends TelnetSocketServer {

    /** The single instance of TOC_Server.*/
    private static TOC_Server instance = null;
    
    /** True if the TOC is accepting requests.*/
    protected boolean accept;
    
    /** Name of the Host where the currently invoked SA is running.*/
    protected String acceptHost;

    /** Create a TOC_Server bound to the specified port. <br>
     * This should have been defined in an RCS configuration file 
     * as <b>TOC.port</b>.
     * @param port The port to bind to.
     */
    private TOC_Server(int port) throws IOException {
	super(port);
	rhFactory = TOC_CommandImplFactory.getInstance();
	piFactory = TOC_ProtocolImplFactory.getInstance();
    }
    
    /** Create the single instance of the ISS server. If the
     * single instance already exists returns silently. 
     * @param port The port to listen on.
     * @exception IOException If the ServerSocket fails to bind 
     * to the specified port for any reason.*/
    public static void bindInstance(int port) throws IOException {
	if (instance == null)
	    instance = new TOC_Server(port);
    }
    

    /** Starts up the server. Just calls start() on its execution
     * thread.*/
    public static void launch() {
	instance.start();
    }

    /** @return The single instance of ISS_Server. If no instance
     * has yet been created will return null.*/
    public static TOC_Server getInstance() {
	return instance;
    }
    
    /** Creates a ServerConnectionThread to handle a client connection 
     * and starts it. If the TOC_Server is not accepting connections then
     * this method returns silently. 
     * @param socket The socket to be used for this client.*/
    @Override
	protected void spawnConnectionThread(Socket socket) throws IOException {
	System.err.println("Spawn connection..");
	//if (!accept) {
	    // log not accepting from any SA
	//  System.err.println("Not accepting from any SA");
	//  socket.close();
	//  return;
	//} else {
	    // We dont check the host at present.
	    super.spawnConnectionThread(socket);	
	    //}
    }

    /** Sets True if accepting SA commands. This is used to prevent access while SAs are being
     * swapped over.*/
    public static void setAccept(boolean accept) {
	instance.accept = accept;
    }

    /** Returns True if accepting SA commands.*/
    public static boolean accepting() { return instance.accept; }

    /** Sets the SA host.*/
    public static void setAcceptHost(String acceptHost) {
	instance.acceptHost = acceptHost;
    }
    
}

/** $Log: TOC_Server.java,v $
/** Revision 1.1  2006/12/12 08:32:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:48  snf
/** Initial revision
/**
/** Revision 1.3  2003/12/15 14:46:20  snf
/** *** empty log message ***
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/09/03 14:38:48  snf
/** Initial revision
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2001/03/15 15:10:49  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */
