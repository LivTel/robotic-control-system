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

import java.util.*;

/** ConnectionFactory implementation for creating IConnections to the
 * various telescope subsystems. The methods addSocketResource() and
 * AddSlotResource() are used to insert information into the resource
 * table to allow the named-resource connection generator method - 
 * createConnection(String) to create the correct type of IConnection.
 * In order to use this class, call the static method getInstance() to
 * create a singleton then use the addXXResource() methods to set up
 * the resource table.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: RCS_SubsystemConnectionFactory.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/comms/RCS/RCS_SubsystemConnectionFactory.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RCS_SubsystemConnectionFactory implements ConnectionFactory {

    static final int CRYPTO_SOCKET = 1;

    static final int STANDARD_SOCKET = 0;

    /** Holds the information to allow an appropriate type of connection
     * to be made for the named resources.*/
    protected Map resources;
    
    /** A singleton instance.*/
    protected static RCS_SubsystemConnectionFactory instance = null;
    
    /** Create an instance of RCS_SubsystemConnectionFactory.*/
    private RCS_SubsystemConnectionFactory() {
	resources = Collections.synchronizedMap(new HashMap());
    }
    
    /** Returns a reference to the resource map.*/
    public Map getResources() { return resources; }

    /** Returns a reference to the singleton instance.*/
    public static RCS_SubsystemConnectionFactory getInstance() {
	if (instance == null)
	    instance = new RCS_SubsystemConnectionFactory();
	return instance;
    }
    
    /** Add information to allow a SocketConnection for a named subsystem 
     * resource to be made.
     * @param name The name/id of the resource.
     * @param host The subsystem's hostname.
     * @param port The port to use.*/
    protected void doAddSocketResource(String name, String host, int port, int type) {
	synchronized (resources) {
	    resources.put(name, new SocketResource(host, port, type));
	    System.err.println("New SocketResource: "+name+"="+host+":"+port);
	}
    }

    /** Add information to allow a SocketConnection for a named subsystem 
     * resource to be made.
     * @param name The name/id of the resource.
     * @param host The subsystem's hostname.
     * @param port The port to use.*/
    public static void addSocketResource(String name, String host, int port) {	
	getInstance().doAddSocketResource(name, host, port, STANDARD_SOCKET);
    }
    
    /** Add information to allow a CryptoSocketConnection for a named subsystem 
     * resource to be made.
     * @param name The name/id of the resource.
     * @param host The subsystem's hostname.
     * @param port The port to use.*/
    public static void addCryptoSocketResource(String name, String host, int port) {	
	getInstance().doAddSocketResource(name, host, port, CRYPTO_SOCKET);
    }
    /** Add information to allow a SlotConnection for a named subsystem 
     * resource to be made.
     * @param name The name/id of the resource.
     * @param server The SlotServer at that subsystem.*/
    protected void doAddSlotResource(String name, SlotServer server) {
	synchronized (resources) {	    
	    resources.put(name, server);
	    System.err.println("New SlotResource: "+name+"="+server);
	}
    }
    
    /** Add information to allow a SlotConnection for a named subsystem 
     * resource to be made.
     * @param name The name/id of the resource.
     * @param server The SlotServer at that subsystem.*/
    public static void addSlotResource(String name, SlotServer server) {
	getInstance().doAddSlotResource(name, server);
    }
   
 
    /** Create an IConnection of unspecified type to the named resource. 
     * @param name The name/id of the resource to connect to. 
     * @return An appropriate IConnection for the named subsystem resource.
     * @exception UnknownResourceException If the named resource does not exist.*/
    public IConnection createConnection(String name) throws UnknownResourceException {
	synchronized (resources) {
	    if (resources.containsKey(name)) {
		Object resource = resources.get(name);
		if 
		    (resource instanceof SlotServer) {
		    return new SlotConnection((SlotServer)resource, 5);
		}
		else if
		    (resource instanceof SocketResource) {		   
		    SocketResource socks = (SocketResource)resource; 
		    switch (socks.getType()) {
		    case STANDARD_SOCKET:
			System.err.println("Creating socket for "+name+" on: "+socks.getHost()+":"+socks.getPort());
			return new SocketConnection(socks.getHost(), socks.getPort());
		    case CRYPTO_SOCKET:
			return new CryptoSocketConnection(socks.getHost(), socks.getPort());
		    }
		}
	    }
	}
	throw new UnknownResourceException("No such subsystem: "+name);
    }

    /** Stores information relating to a Socket-based subsystem.*/
    class SocketResource {

	/** The host name.*/
	private String host;

	/** The port used for this resource.*/
	private int port;

	/** Indicates subtype.*/
	private int type;

	/** Create a SocketResource using the supplied hostname and port.
	 * @Param host The name of the host.
	 * @param port The port to use.*/
	SocketResource(String host, int port, int type) {
	    this.host = host;
	    this.port = port;
	    this.type = type;
	}
	
	/** Returns the name of the Socket-based resource's host.
	 * @return Name of the host.*/
	public String getHost() { return host; }

	/** Returns the port used for the Socket-based resource.
	 * @return The port used for the Socket-based resource.*/
	public int getPort() { return port; }
	
	/** Returns the type of this Socket.
	 * @return The Socket subclass type.*/
	public int getType() { return type; }

    }

}

/** $Log: RCS_SubsystemConnectionFactory.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.2  2001/02/23 18:52:17  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */
