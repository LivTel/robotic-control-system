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
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;

/** A leaf Task for performing an Telescope AGCENTROID operation.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: AG_Centroid_Task.java,v 1.1 2006/12/12 08:28:27 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/executive/RCS/AG_Centroid_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class AG_Centroid_Task extends Default_TaskImpl {
    
    /** Constant denoting the typical expected time for this Task to complete.*/
    public static final long DEFAULT_TIMEOUT = 60000L;

    /** The AG focus position (mm).*/
    protected double focus;

    /** The Autoguider FWHM recieved as a result of the AGCENTROID operation.*/
    protected double fwhm;

    /** The peak counts from the autoguider CCD.*/
    protected int peak;

    /** X pixel position of guide source.*/
    protected double xPixel;

    /** Y pixel position of guide source.*/
    protected double yPixel;
    
    /** Create an AG_Centroid_Task.
     * @param name The unique name/id for this TaskImpl - 
     * should be based on the COMMAND_ID.
     * @param manager The Task's manager.
     */
    public AG_Centroid_Task(String name,
			    TaskManager manager) {
	super(name, manager);
	
	try {
	    createConnection("CIL_PROXY");  
	    //createConnection(RCS_Controller.getCilServerId());
	} catch (UnknownResourceException e) {
	    logger.log(1, CLASS, name, "Constructor", 
		       "Unable to establish connection to subsystem: CIL_PROXY.");
	    failed = true; 
	    errorIndicator.setErrorCode(CONNECTION_RESOURCE_ERROR);
	    errorIndicator.setErrorString("Creating connection: Unknown resource CIL_PROXY");
	    errorIndicator.setException(e);	    
	    return;
	    // FATAL
	}
    	
	// -------------------------------
	// Set up the appropriate COMMAND.
	// -------------------------------
	
	AGCENTROID agcentroid = new AGCENTROID(name);
	
	command = agcentroid;	
    }
       
    /** Carry out subclass specific initialization.*/
    @Override
	public void onInit() {	
	super.onInit();
	logger.log(1, CLASS, name, "onInit",
		   "Starting AG Centroid");
    }
    
    /** Carry out subclass specific completion work.
     * Save the received value of AG-FWHM.*/
    @Override
	public void onCompletion(COMMAND_DONE response) {
	if (response instanceof AGCENTROID_DONE) {
	    AGCENTROID_DONE agdone = (AGCENTROID_DONE)response;
	    logger.log(1, CLASS, name, "onCompletion", 
		       "Completed AG Centroid:"+
		       "\n FWHM:        "+agdone.getFwhm()+
		       "\n Peak counts: "+agdone.getPeak()+
		       "\n X-pix:       "+agdone.getXPixel()+
		       "\n Y-pix:       "+agdone.getYPixel());
	    fwhm   = agdone.getFwhm();
	    peak   = agdone.getPeak();
	    xPixel = agdone.getXPixel();
	    yPixel = agdone.getYPixel();
	}
    }
    
    /** Carry out subclass specific disposal work.   ## NONE ##.*/
    @Override
	public void onDisposal() {
	
    }
    
    /** Returns the Autoguider FWHM recieved as a result of the AGCENTROID operation.*/
    public double getFwhm()   { return fwhm;}
    
    /** Returns the peak counts from the autoguider CCD.*/
    public int    getPeak()   { return peak;}
    
    /** Returns the X pixel position of guide source.*/
    public double getXPixel() { return xPixel;}
    
    /** Returns the Y pixel position of guide source.*/
    public double getYPixel() { return yPixel;}
      
}
