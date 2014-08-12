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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

/** Carries out the implementation of a POS CCDOBSERVE command.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CCDOBSERVE_CommandImpl.java,v 1.4 2007/05/25 07:29:45 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CCDOBSERVE_CommandImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.4 $
 */
public class POS_CCDOBSERVE_CommandImpl extends POS_CommandImpl {

    public static final String CLASS = "POS_CCDOBSERVE_CommandImpl";
      
    public static final int LEVEL = 0;
    
    public static final int ERROR_BASE   = 9300;

    public static final int CONFIG_ERROR          = 609301;

    public static final int UNKNOWN_MOVING_SOURCE = 609302;

    public static final int GENERAL_EXCEPTION     = 609303;


    public static final long SLEW_OVERHEAD   = 180*1000L;
    
    public static final long CONFIG_OVERHEAD = 60*1000L;

    protected ParallelTaskImpl ccdSubTask;
  
    public POS_CCDOBSERVE_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, POS_TO_RCS command) {
	super(serverImpl, command);	
    }

    /** Returns true if this command should be queued. */
    public boolean enqueue() { return true; }
    
    /** Returns the queue level of this command. */
    public int     getLevel() { return LEVEL; }

    /** Returns the expected maximum duration of this command processing.
     * Slew time + Config time + exposure time, (Note: exposure is in milli-secs).*/
    public long getDuration() {
	return SLEW_OVERHEAD + CONFIG_OVERHEAD + (long) Math.abs(((CCDOBSERVE)command).getExposure());	    
    }
    
   
    public COMMAND_DONE processCommand() {return null;}

    /** Creates a Task to be implemented to carry out this request.
     * Non queued handlers will not need this.*/
    public  Task createTask() {
    
	PlanetariumControlAgent planetariumCA = 
	    (PlanetariumControlAgent)PlanetariumControlAgent.getInstance();
	
	// Grab the basic configs from PCA - die if they're null or dodgy..
	CCDConfig        posInstConfig = (CCDConfig)planetariumCA.getDefaultInstrumentConfig();

	// NOTE we need: CCDOBSERVE. {CONFIG_ERROR, or SOFTWARE_ERROR}

	if (posInstConfig == null) {
	    processError(CCDOBSERVE.UNSPECIFIED_ERROR, 
			 "PCA Could not generate an InstrumentConfig", null);
	    return null;  // FATAL
	}

	TelescopeConfig  posTeleConfig = planetariumCA.getDefaultTelescopeConfig();
	if (posTeleConfig == null) {
	    processError(CCDOBSERVE.UNSPECIFIED_ERROR, 
			 "PCA Could not generate a TelescopeConfig", null);
	    return null;  // FATAL
	}

	CCDOBSERVE  ccdobs = (CCDOBSERVE)command;
	
	// Offsets. - converted to rads from arcsec.
	double xoff = Math.toRadians(ccdobs.getXOffset()/3600.0);
	double yoff = Math.toRadians(ccdobs.getYOffset()/3600.0);
	
	// -----------------
	// Setup the Source.
	// -----------------
	Source      source = null; 
	String sourceType = "UNKNOWN";
	// Check to see if this is a Fixed or Moving source.
	if (ccdobs instanceof CCDFIXED) {
	    // ### USe the new (TBD) srcName parameter off CCDFIXED for the ID field.
	    ExtraSolarSource posSource = 
		new ExtraSolarSource(((CCDFIXED)ccdobs).getSourceId()+":"+ccdobs.getRequestNumber());
	    // Source
	    posSource.setRA(((CCDFIXED)ccdobs).getPosition().getRA());
	    posSource.setDec(((CCDFIXED)ccdobs).getPosition().getDec());
	    posSource.setPmRA(0.0);
	    posSource.setPmDec(0.0);
	    posSource.setParallax(0.0);
	    posSource.setRadialVelocity(0.0);
	   
	    source = posSource;

	    //sourceType = ((CCDFIXED)ccdobs).getSourceType();
	    
	} else if
	    (ccdobs instanceof CCDMOVING) {
	   
	    switch (((CCDMOVING)ccdobs).getSrcId()) {
	    case CCDMOVING.MOON:		
		source = new CatalogSource("MOON("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.MOON);
		sourceType = "MOON";
		break;
	    case CCDMOVING.MERCURY:		
		source = new CatalogSource("MERCURY("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.MERCURY);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.VENUS:		
		source = new CatalogSource("VENUS("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.VENUS);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.MARS:		
		source = new CatalogSource("MARS("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.MARS);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.JUPITER:		
		source = new CatalogSource("JUPITER("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.JUPITER);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.SATURN:		
		source = new CatalogSource("SATURN("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.SATURN);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.URANUS:		
		source = new CatalogSource("URANUS("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.URANUS);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.NEPTUNE:		
		source = new CatalogSource("NEPTUNE("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.NEPTUNE);
		sourceType = "MAJORPLANET";
		break;
	    case CCDMOVING.PLUTO:		
		source = new CatalogSource("PLUTO("+xoff+","+yoff+")"); 
		((CatalogSource)source).setCatalogId(CatalogSource.PLUTO);
		sourceType = "MAJORPLANET";
		break;
	    default: 
		// This should not be possible - the POS_Relay should have trapped this.
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
			   "Unknown moving source: ["+((CCDMOVING)ccdobs).getSrcId()+"]");
		processError(CCDOBSERVE.UNSPECIFIED_ERROR, 
			     "Unknown moving source: ["+((CCDMOVING)ccdobs).getSrcId()+"]", null);
		return null;  // FATAL
	    }
	    // Any Other source stuff ??
	}
	
	source.setEquinox(2000.0f);
	source.setFrame(Source.FK5);
	source.setEquinoxLetter('J');
	source.setEpoch(2000.0f);
	
	// Check whether the source is actually visible at this time - it may have been
	// queued a while ago when it was still up.
	double domeLimit = planetariumCA.getDomeLimit();
	Position target = source.getPosition();

	pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor",
		   "Target alt: "+Position.toDegrees(target.getAltitude(),3)+
		   " Dome low-limit: "+Position.toDegrees(domeLimit,3));

	if (target.getAltitude() < domeLimit) {
	    pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
		       "Target has now set below dome limit "+Position.toDegrees(domeLimit, 3));
	    processError(CCDOBSERVE.OBJECT_SET,
			 "Target has now set below dome limit "+Position.toDegrees(domeLimit, 3), null);
	    return null; // FATAL
	}
	
	if (ccdobs.getExposure() < 0.0) {
	    // Slew only.
	    POS_Slew_Task posSlewTask = 
		new POS_Slew_Task(ccdobs.getId(), 
				  planetariumCA,
				  this, 
				  source);
	    ccdSubTask = posSlewTask;
	    return posSlewTask;
	} else {
	    // Slew and expose.

	    Observation obs    = new Observation(ccdobs.getId());

	    String rootId = RCS_Controller.controller.getTelescopeId()+"_Planetarium";
	    String tagId  = planetariumCA.getControllerId();
	    String userId = planetariumCA.getCurrentUserName();
	    obs.setPath("/"+rootId+"/"+tagId+"/"+userId+"/ccdgroup");
	    
	    // ---------------------------
	    // Setup the InstrumentConfig.
	    // ---------------------------
	    
	    posInstConfig.setName(ccdobs.getId()+"-CCD-Config");	
	    // e.g. POS-CCDOBSERVE-[LM-20010602-DJF124]-CCD-Config
	    posInstConfig.setLowerFilterWheel(ccdobs.getFilter1());
	    pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
		       "Setting Lower filter type to:"+ccdobs.getFilter1());
	    posInstConfig.setUpperFilterWheel(ccdobs.getFilter2());
	    pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
		       "Setting Upper filter type to:"+ccdobs.getFilter2());
	    try {
		CCDDetector detector = (CCDDetector)posInstConfig.getDetector(0);
		detector.setXBin(ccdobs.getBin());
		detector.setYBin(ccdobs.getBin());
		detector.clearAllWindows();
	    } catch (IllegalArgumentException e) {
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
			   "Failed to configure CCD Detector: "+e);
		processError(CCDOBSERVE.UNSPECIFIED_ERROR, 
			     "Failed to configure CCD Detector:", e);
		return null; // FATAL
	    }
	    
	    // Check to see if the binning and filter selection are valid - assumes we
	    // actually know these - SHOULD be stored in Instruments.RAT_CAM.
	    //if ( ccdobs.getBin() < 1 || ccdobs.getBin() > 
	    //Instruments.RAT_CAM.getConfig().getDetector(0).getMaxXBins() ) {
	    
	    if ( ccdobs.getBin() < 1 || ccdobs.getBin() > 4) {
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
			   "Unable to configure CCD Camera: Illegal binning");
		processError(CCDOBSERVE.BAD_BINNING, 
			     "Unable to configure CCD Camera: Illegal binning", null);
		return null; // FATAL
	    }
	    
	    // This is the RCS view of the filters - the CCS may not agree and will send an
	    // appropriate message when it is configured by CONFIG - i.e. CONFIG will return
	    // an error code of XX 000102 XX indicating illegal filter - we could try and catch this
	    // message and propagate it upwards thro the task hierarchy but really the fault
	    // will be due to a configuration mismatch between the RCS/CCS.
	    CCD ratcam = (CCD)Instruments.findInstrument("RATCAM");
	    if ( ! (ratcam.upperFilterWheelHasFilterType(ccdobs.getFilter2())) ||
		 ! (ratcam.lowerFilterWheelHasFilterType(ccdobs.getFilter1())) ) {
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "Constructor", 
			   "Unable to configure CCD Camera: Illegal filter settings");
		processError(CCDOBSERVE.BAD_FILTER, 
			     "Unable to configure CCD Camera: Illegal filter settings", null);
		
		return null; // FATAL
	    }	 
	    
	    // --------------------------
	    // Setup the TelescopeConfig.
	    // --------------------------
	    
	    posTeleConfig.setId(ccdobs.getId()+"-Tele-Config");	
	    // e.g. POS-CCDOBSERVE-[F2-20010712-AKB06]-Tele-Config
	    // Note: We set the rotator parameters here as ROT SKY <rot>
	    //       If this is a one-off then the rotator setting will be SKY <rot>
	    //       But If its part of a mosaic then we will need to reset to FLOAT
	    //       once in position at offset 0,0 then do the offset with the rotator
	    //       tracking as if at the 0,0 position ! 
	    // Note: The CCDOBS-rotation parameter is cwise, TC.rot is a/cwise.
	    //
	    //posTeleConfig.setSkyAngle(2.0*Math.PI - ccdobs.getRotation());
	    posTeleConfig.setSkyAngle(ccdobs.getRotation());
	   
	    // -----------------------------
	    // Setup a dummy PipelineConfig.
	    // -----------------------------
	    
	    PipelineConfig   posPipeConfig = new PipelineConfig("POS-CCDOBSERVE-["+ccdobs.getId()+"]-Pipe-Config");
	    
	    // ----------------------------------------------------------
	    // Setup the Observation mosaic pattern and exposure details.
	    // ----------------------------------------------------------
	    
	    Mosaic mosaic = new Mosaic();
	    mosaic.setPattern(Mosaic.SINGLE);				
	    obs.setMosaic(mosaic);
	    obs.setNumRuns(1);
	    obs.setExposeTime((float)(ccdobs.getExposure()));
	 
	    // Link in all the resources.
	    
	    obs.setSource           (source);
	    obs.setInstrumentConfig (posInstConfig);
	    obs.setTelescopeConfig  (posTeleConfig);
	    obs.setPipelineConfig   (posPipeConfig);

	    // TODO insert code to transfer rotator mode/angle/autoguider mode etc info into Observation
	    // TODO these were previously in teleconfig
	    // obs.setRotMode(?)
	    // obs.setRotAngle(?)
	    // obs.setAutoMode(?)

	    
//  	    // Make a Task, pass to PCA as manager and callback to this handler/processor.
//  	    POS_CcdObserve_Task ccdObsTask = 
//  		new POS_CcdObserve_Task(ccdobs.getId(), 
//  					planetariumCA,
//  					this, 
//  					obs);
//  	    ccdObsTask.setTpXOffset(xoff);
//  	    ccdObsTask.setTpYOffset(yoff);
	    
	    // Handle MOSAIC here.
	    
	    switch (ccdobs.getMode()) {
	    case CCDOBSERVE.SINGLE:	
		POS_ObserveTask  ccdObsTask = 
		    new POS_ObserveTask("CCDOBS-"+ccdobs.getId(), 
					planetariumCA,
					this, 
					obs,
					sourceType);
		    
		ccdSubTask = ccdObsTask;	
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "createTask",
			   "CCDOBSERVE: Mode is: SINGLE: Creating a POS_ObserveTask");
		return ccdObsTask;
	    case CCDOBSERVE.MOSAIC_SETUP:
		POS_MosaicSetupTask ccdMosaicSetupTask =
		    new POS_MosaicSetupTask(ccdobs.getId(), 
					    planetariumCA,
					    this, 
					    obs);
		ccdMosaicSetupTask.setTpXOffset(xoff);
		ccdMosaicSetupTask.setTpYOffset(yoff);

		double rot = 2.0*Math.PI - ccdobs.getRotation();
		if (rot > 2.0*Math.PI) 
		    rot = rot - (2.0*Math.PI);

		ccdMosaicSetupTask.setRotation(rot);
		ccdSubTask = ccdMosaicSetupTask;	
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "createTask",
			   "CCDOBSERVE: Mode is: MOSAIC_SETUP: Creating a POS_MosaicSetupTask");
		return ccdMosaicSetupTask;
	    case CCDOBSERVE.MOSAIC:
		POS_MosaicObserveTask ccdMosaicObsTask =
		    new POS_MosaicObserveTask(ccdobs.getId(), 
					      planetariumCA,
					      this, 
					      obs);

		ccdMosaicObsTask.setTpXOffset(xoff);
		ccdMosaicObsTask.setTpYOffset(yoff);

		//ccdMosaicObsTask.setRotation(2.0*Math.PI - ccdobs.getRotation());
		rot = 2.0*Math.PI - ccdobs.getRotation();
		if (rot > 2.0*Math.PI) 
		    rot = rot - (2.0*Math.PI);
		ccdMosaicObsTask.setRotation(rot);
		
		ccdSubTask = ccdMosaicObsTask;	
		pcaLog.log(1, "POS_CCDOBSERVE_CommandImpl", command.getId(), "createTask",
			   "CCDOBSERVE: Mode is: MOSAIC: Creating a POS_MosaicObserveTask");
		return ccdMosaicObsTask;
	    default:	
		processError(CCDOBSERVE.UNSPECIFIED_ERROR, 
			     "Failed to create a valid Task",
			     null);
		return null; // FATAL
	    }
	    
	}
    }
    
    /** Makes up a CCDOBSERVE_DONE and sets its error code etc.*/
    public void processError(int errorCode, String errorMessage, Exception e) {
	CCDOBSERVE_DONE done = new CCDOBSERVE_DONE(command.getId());
	done.setSuccessful(false);
	done.setErrorNum(errorCode);
	done.setErrorString(errorMessage + " : " + e);

	pcaLog.log(1,"POS_CCDObserveImpl:: Sending error reply: Code="+errorCode+" Msg="+errorMessage);

	processDone(done);
    }

    /** Clear up on disposal. Calls super to unqueue from the POSQueue
     * This method is called by ServerImpl as the final operation before it dies off.
     */
    public void dispose() {
	super.dispose();
	ccdSubTask = null;
    }

    /** This method is used to try and abort the attached task.*/
    public void abort(int code, String message) {
	if (ccdSubTask != null) {
	    ccdSubTask.setAbortCode(code, message);
	    ccdSubTask.abort();
	}
    }
    
    /** Watch this we DEFINITELY DONT need to set an error code and return it to client.
     * This is only called if we have some sort of error when sending ack/done to the client
     * so if that fails were done anyway, just cant tell them. A typical case is where
     * the pipe to the client breaks.
     */
    public void exceptionOccurred(Object source, Exception exception) {
	pcaLog.log(1, CLASS, command.getId(), "ExceptionCallback",
		   "POS_CCDObserveImpl::Source: "+source+" Exception: "+exception);
	//CCDOBSERVE_DONE done = new CCDOBSERVE_DONE(command.getId());
	//done.setSuccessful(false);
	//done.setErrorNum(GENERAL_EXCEPTION); 
	//done.setErrorString("General Exception: From: "+source+" : " + exception);
	
	// we really need to limit this to avoid a stack overflow.....
	
	//processDone(done);
    }


}

/** $Log: POS_CCDOBSERVE_CommandImpl.java,v $
/** Revision 1.4  2007/05/25 07:29:45  snf
/** added static CLASS identifier
/**
/** Revision 1.3  2007/05/25 07:26:54  snf
/** removed attempt to call send(reply) on exceptionHandler - causes stackoverflow due to infinite looping feedback from JMSMA_ServerImpl.
/** This is because this class acts as sender and exception handler so must not  attempt to resend.
/**
/** Revision 1.2  2007/02/21 10:30:00  snf
/** Added logging for Error reply
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
