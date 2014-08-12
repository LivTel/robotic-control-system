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
package ngat.rcs.tms;

import java.io.Serializable;

import ngat.rcs.*;
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


/** This interface should be implemented by any classes which wish to supply an
 * indication of an error condition. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: BasicErrorIndicator.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/BasicErrorIndicator.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class BasicErrorIndicator implements ErrorIndicator, Serializable {

    protected int errorCode;

    protected String errorString;

    protected Exception exception;
    
    public BasicErrorIndicator(int errorCode, String errorString, Exception exception) {
	this.errorCode = errorCode;
	this.errorString = errorString;
	this.exception = exception;
    }
    
    /** Create an ErrorIndicator with no code, message or exception set.*/
    public BasicErrorIndicator() {
	this(0, "", null);
    }
    
    /** Sets the error code for this ErrorIndicator.
     * @param errorCode The code to set.*/
    public void      setErrorCode(int errorCode) { this.errorCode = errorCode; }
    
    /** Implementors should return a code to indicate the nature of the error.
     * @return A code to indicate the nature of the error.*/
    public int       getErrorCode()   { return errorCode; }
    
    /** Sets the error String for this ErrorIndicator.
     * @param errorString The error String to set.*/
    public void      setErrorString(String errorString) { this.errorString = errorString; }
    
    /** Implementors should return a String to indicate more detail of the error.
     * @return A String to indicate more detail of the error.*/
    public String    getErrorString() { return errorString; }
    
    /** Sets the exception for this ErrorIndicator.
     * @param exception The Exception to set.*/
    public void      setException(Exception exception) { this.exception = exception; }
    
    /** Implementors should return the java.lang.Exception which caused an error condition.
     * @return The java.lang.Exception which caused an error condition.*/
    public Exception getException()   { return exception; }
    
}

/** $Log: BasicErrorIndicator.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */
