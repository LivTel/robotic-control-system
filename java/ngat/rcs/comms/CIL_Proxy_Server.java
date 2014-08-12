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

import java.io.*;

import ngat.net.*;

/**
 * <br><br>
 * $Id: CIL_Proxy_Server.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public class CIL_Proxy_Server extends SlotServer {

    /** The singleton instance of the CIL_Proxy_Server.*/
    public static CIL_Proxy_Server instance = null;

    /** Create the singleton instance of the CIL_Proxy_Server.*/
    private CIL_Proxy_Server() {
	super(JMSMA_ProtocolImplFactory.getInstance(),
	      CIL_ProxyHandlerFactory.getInstance());
    }
    
    /** @return The singleton instance of CIL_Proxy_Server.*/
    public static CIL_Proxy_Server getInstance() {
	if (instance == null)
	    instance = new CIL_Proxy_Server();
	return instance;
    }


}

/** $Log: CIL_Proxy_Server.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/12 18:50:00  snf
/** Initial revision
/** */
