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
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;
import ngat.util.logging.*;

import java.util.*;

/** Provides methods for obtaining a handler for a given JMS command
 * and for registering and looking up sequence numbers.
 * <br><br>
 * $Id: CIL_ProxyRegistry.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public class CIL_ProxyRegistry {

    public static Map registry;

    public static int sequenceCount;

    public static Logger logger;

    /** Initialize the ProxyRegistry.*/
    public static void init(int seqStart) {
	registry = Collections.synchronizedMap(new HashMap());
	sequenceCount = seqStart;
	logger = LogManager.getLogger("CIL");
    }


    /** Regsiters a ProxyHandler against a sequence number.
     * @param handler The ProxyHandler which is requesting a sequence number.
     * @return The sequence number assigned to this ProxyHandler.*/
    public static int registerSequenceNo(CIL_ProxyHandler handler) {
	synchronized (registry) {
	    //sequenceCount++;
	    // Wrapover at MAXINT.
	    sequenceCount = ((sequenceCount + 1) % Integer.MAX_VALUE);
	    registry.put(new Integer(sequenceCount), handler);
	    return sequenceCount;
	}
    }

    /** Un-registers a ProxyHandler against its sequence number.
     * @param seqNo The sequence number of the ProxyHandler which is requesting 
     * to be deregistered.
     */
    public static void deregister(int seqNo) {
	synchronized (registry) {
	    Integer sq = new Integer(seqNo);
	    if (registry.containsKey(sq)) {
		CIL_ProxyHandler handler = (CIL_ProxyHandler)registry.get(sq);	   
		registry.remove(sq); 
		String hid = null;
		if (handler != null) 
		    hid = handler.getCommandId();	
		logger.log(2, "CIL_Registry", "-", "-",
			   "CILReg::Deregister: "+seqNo+
			   ", Id: "+hid+
			   ", CurrentSeq: "+sequenceCount+
			   ", RegistrySize: "+registry.size());
	    }
	}
    }
    
    /** Returns the ProxyHandler which is registered against the
     * supplied sequence number.
     * @param seqNo The sequence number.
     * @return The ProxyHandler registered against seqNo or null if none.*/
    public static CIL_ProxyHandler lookup(int seqNo) {
	synchronized (registry) {
	    Integer sq = new Integer(seqNo);
	    if (registry.containsKey(sq))   
		return (CIL_ProxyHandler)registry.get(sq);
	    logger.log(1, "CIL_Registry", "-", "-",
		       "Key not found: "+seqNo);
	    return null;
	}
    }
    
}

/** $Log: CIL_ProxyRegistry.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.4  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.3  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/12 18:50:00  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/07 14:51:34  snf
/** Initial revision
/**
/** Revision 1.3  2000/12/07 13:44:03  snf
/** *** empty log message ***
/**
/** Revision 1.2  2000/12/07 13:43:17  snf
/** Removed the factory method createHandler.
/**
/** Revision 1.1  2000/11/28 18:14:57  snf
/** Initial revision
/** */
