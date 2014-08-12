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
import ngat.net.*;
import ngat.net.camp.*;

import java.io.*;

/** Implementation of the RCS Control server for
 * handling RCS commands sent by the admin user.
 * <br><br>
 * $Id: Ctrl_Server.java,v 1.1 2006/12/12 08:26:29 snf Exp $
 */
public class Ctrl_Server extends CAMPServer {

    /** The single instance of Ctrl_Server.*/
    private static Ctrl_Server instance = null;

    /** Create an Ctrl_Server. 
     * @param name The name of the server.
     */
    private Ctrl_Server(String name) throws IOException {
	super(name);
	handlerFactory = Ctrl_CommandImplFactory.getInstance();
    }
    
    /** Create the single instance of the Ctrl server. If the
     * single instance already exists returns silently. 
     * @param name The name of the server.
     * @param port The port to listen on.
     * @exception IOException If the ServerSocket fails to bind 
     * to the specified port for any reason.*/
    public static void bindInstance(String name, int port) throws IOException {
	if (instance == null) {
	    instance = new Ctrl_Server(name);
	    instance.bind(port);
	}
    }
	
    /** Starts up the server. Just calls start() on its execution
     * thread.*/
    public static void launch() {
	instance.start();
    }

    /** @return The single instance of Ctrl_Server. If no instance
     * has yet been created will return null.*/
    public static Ctrl_Server getInstance() {
	return instance;
    }
    
}

/** $Log: Ctrl_Server.java,v $
/** Revision 1.1  2006/12/12 08:26:29  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:59  snf
/** Initial revision
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2001/03/15 15:12:59  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */
