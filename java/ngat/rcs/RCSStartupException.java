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
package ngat.rcs;

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
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;


/** Exception thrown by the RCS_Controller if a FATAL error occurs during
 * startup/configuration. The exitCode is used to determine the source
 * of the error.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: RCSStartupException.java,v 1.1 2006/12/12 08:25:35 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/RCS/RCSStartupException.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RCSStartupException extends Exception {
	
	/** This code is used by the RCS_Controller's constructor to signal
	 * the cause of a startup failure.*/
	protected int exitCode;
	
	/** Create a RCSStartupException with no message.*/
	public RCSStartupException() {
	    super();
	    this.exitCode = 0;
	}
	
	/** Create a RCSStartupException with the specified message.
	 * @param message The message to set.*/
	public RCSStartupException(String message) {
	    this(message, 0);
	}
	
	/** Create a RCSStartupException with the specified message and exit code.
	 * @param message The message to set.
	 * @param exitCode The code to set.*/
	public RCSStartupException(String message, int exitCode) {
	    super(message);
	    this.exitCode = exitCode;
	}
	
	/** Returns the exit code - defined by the failure mechanism.*/
	public int getExitCode() { return exitCode; }

	/** Overridden to include the exitCode.*/
	@Override
	public String toString() { return super.toString()+" ExitCode: "+exitCode; }
	
}

/** $Log: RCSStartupException.java,v $
/** Revision 1.1  2006/12/12 08:25:35  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:29:55  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
