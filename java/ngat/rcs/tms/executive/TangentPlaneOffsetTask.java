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
package ngat.rcs.tms.executive;

import ngat.rcs.*;
import ngat.rcs.tms.*;
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
import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing a Telescope Slew. The source position passed
 * in is checked and an appropriate SLEW command is generated using the source's
 * current position and the tangent plane offsets and then
 * sent to the telescope control system.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: TangentPlaneOffsetTask.java,v 1.2 2007/10/25 09:58:28 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/TangentPlaneOffsetTask.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class TangentPlaneOffsetTask extends Default_TaskImpl {
  
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** Error code due to being too near pole and thus causing potential divzero error.*/
    public static final int TOO_CLOSE_TO_POLE = 640601;

    /** Throws a wobbly if Colatitude is less than this to avoid divzero.*/
    public static final double MIN_COLAT = Math.toRadians(1.00);

    /** The Source whose position is to be slewed to.*/
    protected Source source;

    /** Tangent plane X-offset (rads).*/
    protected double tpXOffset;

    /** Tangent plane X-offset (rads).*/
    protected double tpYOffset;

    /** Tangent plane rotation (rads c/wise).*/
    protected double rotation;

    /** Calculated RA Offset.*/
    double dra;

    /** Calculated Dec Offset.*/
    double ddec;

    /** Create a TangentPlaneOffset_Task using the supplied Source position.
     * If the subsystem resource (TelescopeControlSystem) cannot be found ???.
     * @param source The source whose position is to be slewed to.
     * @param tpXOffset Tangent plane X-offset (rads).
     * @param tpYOffset Tangent plane Y-offset (rads).
     * @param rotation Tangent plane rotation (rads c/wise.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public TangentPlaneOffsetTask(String      name,
				  TaskManager manager,
				  Source      source,
				  double      tpXOffset,
				  double      tpYOffset,
				  double      rotation) {
	super(name, manager, "CIL_PROXY");
	this.source    = source;
	this.tpXOffset = tpXOffset;
	this.tpYOffset = tpYOffset;
	this.rotation  = rotation;

	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	OFFBY offby = new OFFBY(name);
	double cospa = Math.cos(rotation);
	double sinpa = Math.sin(rotation);

	double colat = (Math.PI/2.0) - source.getPosition().getDec();

	if (colat < MIN_COLAT) {
	    logger.log(1, CLASS, name, "Constructor", 
			 "Too close to pole - divzero problem");
	    failed = true;
	    errorIndicator.setErrorCode(TOO_CLOSE_TO_POLE);
	    errorIndicator.setErrorString("Too close to pole: colat: "+Position.toDegrees(colat,3)+
					  " Must be at least: "+Position.toDegrees(MIN_COLAT, 3));
	    errorIndicator.setException(null);
	    return;	    
	}
	
	//double cosdec= Math.cos(source.getPosition().getDec());
	
	//dra   = (tpXOffset*cospa - tpYOffset*sinpa)/cosdec;
	// We dont need to scale these as they are ARC offsets.
	dra   = tpXOffset*cospa - tpYOffset*sinpa;
	ddec  = tpXOffset*sinpa + tpYOffset*cospa;

	// These are now rads...
	offby.setMode(OFFBY.ARC);
	offby.setOffsetRA(dra);
	offby.setOffsetDec(ddec);
	
	command = offby;
		
    }     

    @Override
	protected void onInit() {
	super.onInit();
	logger.log(1, CLASS, name, "onInit", 
		   "Starting TangentPlane offset "+
		   "  Xoff: "+((int)(Math.toDegrees(tpXOffset)*3600.0))+" arcsec"+
		   ", Yoff: "+((int)(Math.toDegrees(tpYOffset)*3600.0))+" arcsec"+
		   ", Field rotation: "+Position.toDegrees(rotation, 3)+" degs"+
		   ", D_RA: "+Position.toDegrees(dra, 5)+" degs"+
		   ", D_Dec: "+Position.toDegrees(ddec, 5)+" degs");	
	
	double nra  = source.getPosition().getRA()  + dra;
	double ndec = source.getPosition().getDec() + ddec;

// 	FITS_HeaderInfo.current_CAT_RA.setValue (Position.formatHMSString(nra, ":"));
// 	FITS_HeaderInfo.current_APP_RA.setValue (Position.formatHMSString(nra, ":"));				 
// 	FITS_HeaderInfo.current_RA.setValue     (Position.formatHMSString(nra, ":"));

// 	FITS_HeaderInfo.current_CAT_DEC.setValue(Position.formatHMSString(ndec, ":"));
// 	FITS_HeaderInfo.current_APP_DEC.setValue(Position.formatHMSString(ndec, ":"));
// 	FITS_HeaderInfo.current_DEC.setValue    (Position.formatHMSString(ndec, ":"));
	
// 	// Starting TangentPlane offset  Xoff: 232 arcsec, Yonra, ":"));
// 	FITS_HeaderInfo.current_APP_RA.setValue (Position.formatHMSString(nra, ":"));				 
// 	FITS_HeaderInfo.current_RA.setValue     (Position.formatHMSString(nra, ":"));

// 	FITS_HeaderInfo.current_CAT_DEC.setValue(Position.formatHMSString(ndec, ":"));
// 	FITS_HeaderInfo.current_APP_DEC.setValue(Position.formatHMSString(ndec, ":"));
// 	FITS_HeaderInfo.current_DEC.setValue    (Position.formatHMSString(ndec, ":"));
	
	// Starting TangentPlane offset  Xoff: 232 arcsec, Yoff: 415 arcsec, Field Rot: 45.64 degs
	//                               D_RA: 0.15 degs, D_Dec: 0.0023 degs
    }
    
    /** Carry out subclass specific completion work.*/
    @Override
	protected void onCompletion(COMMAND_DONE response) {
	super.onCompletion(response);
	
	logger.log(1, CLASS, name, "onCompletion", 
		   "Completed TangentPlane offset");
    }


    
}


/** $Log: TangentPlaneOffsetTask.java,v $
/** Revision 1.2  2007/10/25 09:58:28  snf
/** commented any setting of fits ra/dec
/**
/** Revision 1.1  2006/12/12 08:28:27  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:33:16  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
