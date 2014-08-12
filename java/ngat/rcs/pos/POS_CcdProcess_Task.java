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
import ngat.fits.*;
import ngat.phase2.*;
import ngat.phase2.nonpersist.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.POS_RCS.*;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.tree.*;

/** This Task creates a series of TCS and ICS Tasks to carry out the
 * configuration and slewing of the Telescope and setting up of the 
 * relevant instruments. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CcdProcess_Task.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CcdProcess_Task.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_CcdProcess_Task implements Task {

    /** ERROR_BASE for this Task type.*/
    //public static final int ERROR_BASE = XXXX;

    public static final String CLASS = "PCA_Process";
 
    /** Tha manager.*/
    protected TaskManager manager;

    /** ErrorContext.*/
    protected ErrorIndicator errorIndicator;

    /** Monitor.*/
    protected TaskMonitor monitor; 

    protected Logger pcaLog;

    protected Logger taskLog;

    protected Logger opsLog;

    /** Worker thread.*/
    protected TaskWorker worker;

    /** Name of the Task.*/
    protected String name;

    protected volatile boolean started;

    protected volatile boolean initialized;

    protected volatile boolean aborted;

    protected volatile boolean failed;

    protected volatile boolean done;

    protected volatile boolean suspended;

    protected long delay;

    /** The destination code.*/ 
    protected int  dest;

    /** Processing type code.*/
    protected int  procType;

    /** Source type code.*/
    protected int  sourceType;

    /** Start frame number.*/
    protected long startFrame;

    /** End frame number.*/
    protected long endFrame;
	
    /** A POS_CommandImplementor to handle the response data.*/
    protected POS_CommandImpl processor; 
   
    /** Stores the code used to signal an abort of this task.*/
    protected int abortCode;

    /** Stores the message sent with an abort of this task.*/
    protected String abortMessage;

    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();


    /** Create a POS_CcdObserve_Task using the supplied Observation and settings.
     * @param observation The Observation to perform.
     * @param processor A POS_CommandImpl to handle the response data. Note: that all
     * POS_CommandImpls implement POS_CommandProcessor also so are effectively both. We need
     * the Impl methods for the response not the Processor methods.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public POS_CcdProcess_Task(String          name,
			       TaskManager     manager,
			       POS_CommandImpl processor,
			       int             dest ,
			       int             procType,
			       int             sourceType,
			       long            startFrame,
			       long            endFrame) {
	this.name = name;
	this.manager = manager;

	pcaLog         = LogManager.getLogger("PCA");
	taskLog        = LogManager.getLogger("TASK");
	opsLog         = LogManager.getLogger("OPERATIONS");
	monitor        = new BasicTaskMonitor(manager, this);
	errorIndicator = new BasicErrorIndicator(0, "OK", null);
	
	this.processor  = processor;
	this.dest       = dest;
	this.procType   = procType;
	this.sourceType = sourceType;
	this.startFrame = startFrame;
	this.endFrame   = endFrame;
    }
    

    public void snapshot(StringBuffer buffer, int level) {
	buffer.append("");
    }
    
    /** Returns a snapshot of this Task's state and other information.
     * @param node A TreeNode wherein to put the information. 
     */
    public void snapshot(TreeNode node) {
	
    }

    public void init() {
	pcaLog.log(1, CLASS, name+"Initialized");
	initialized = true;
    }
    
    /** Task execution method. 2 Phases.
     * <ul>
     *  <li> Image Processing.
     *  <li> Transfer to Web Server.
     * </ul>  
     */
    public void exec_task() {
	pcaLog.log(1, CLASS, name,
		   "Started image processing using parameters:"+
		   "\n Dest:        "+dest+
		   "\n Proc:        "+procType+
		   "\n SrcType:     "+sourceType+
		   "\n Start Frame: "+startFrame+
		   "\n End Frame:   "+endFrame);

	taskLog.log(1, CLASS, name, "exec",
		    "Started image processing using parameters: "+
		    "\n Dest:        "+dest+
		    "\n Proc:        "+procType+
		    "\n SrcType:     "+sourceType+
		    "\n Start Frame: "+startFrame+
		    "\n End Frame:   "+endFrame);

	opsLog.log(1, CLASS, name, "exec",
		   "Started image processing"+
		    "\n Destination: "+dest+
		    "\n Process:     "+procType+
		    "\n TargetType:  "+sourceType+
		    "\n Frames:      "+startFrame+" -to- "+endFrame);
	
	
	worker  = (TaskWorker)Thread.currentThread(); 
	started = true;
	
	File processedFile = null;
	
	// 1) Carry out Image processing operation.
	// The POS_ImageProcessor calls MUST be blocking ......
	// If they create child processes they must wait on them.
	if ((!aborted) && (!suspended) && (!failed)) {		  
	    try {
		switch (procType) {
		case CCDPROCESS.COLOR_JPEG :
		    processedFile = POS_ImageProcessor.processCOLORJPEG(startFrame, endFrame, sourceType);
		    break;
		case CCDPROCESS.BEST_JPEG :
		    processedFile = POS_ImageProcessor.processBESTJPEG(startFrame, endFrame); 
		    break;
		case CCDPROCESS.BEST_FITS :
		    processedFile = POS_ImageProcessor.processBESTFITS(startFrame, endFrame); 
		    break;
		case CCDPROCESS.JPEG :
		    processedFile = POS_ImageProcessor.processJPEG(startFrame, endFrame, sourceType); 
		    break;
		case CCDPROCESS.FITS :
		    processedFile = POS_ImageProcessor.processFITS(startFrame, endFrame);
		    break;
		case CCDPROCESS.MOSAIC_FITS:
		    processedFile = POS_ImageProcessor.processMOSAICFITS(startFrame, endFrame);
		    break;
		case CCDPROCESS.MOSAIC_JPEG:
		    processedFile = POS_ImageProcessor.processMOSAICJPEG(startFrame, endFrame);
		    break;
		default:
		    pcaLog.log(1,"ImageProcessor: Unknown processing request type:"+procType);		   
		    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
					   "Unknown processing request type: "+procType,
					   null);
		    manager.sigTaskFailed(this);
		    return;
		}
	    } catch (POS_ImageProcessor.ImageProcessingException pix) {
		pcaLog.log(1,"ImageProcessor: Failed: "+pix);		
		processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				       "Image-Processing error: ",
				       pix);	
		manager.sigTaskFailed(this);     
		return;
	    }	
	}
	
	if (processedFile == null) {
	    pcaLog.log(1,"ImageProcessor: No file was generated for request type: "+procType);		   
	    processor.processError(POS_TO_RCS.UNSPECIFIED_ERROR,
				   "No file was generated for request type: "+procType,
				   null);
	    manager.sigTaskFailed(this);
	    return;
	}


	// 2) Transfer to remote (or local) web image server.
	if ((!aborted) && (!suspended) && (!failed)) {
	   
	    // Work out the URL of the Remote (Web Image) Server.
	    // We now need to forward the image via the DMZ Relay Server.
	    // Note: We use the Full filepath as the source-file but just the
	    //       filename part as the dest-file. Server will place it
	    //       relative to its own base/root directory.

	   
	    SSLFileTransfer.Client client = planetariumCA.getClient();

	    if (client == null) {
		pcaLog.log(1,"No Image-Transfer client was available");
		processor.processError(CCDPROCESS.TRANSFER_FAULT,
				       "No Image-Transfer client was available:",
				       null);
		manager.sigTaskFailed(this);    
		return;
	    }

	    // ProcessedFile is the name only e.g "proc-555.jpg"
	    // ImageFile is the full pathname inc. image-base-dir E.g. "/home/image/proc-555.jpg".
	    File imageFile = new File(planetariumCA.getImageBaseDir(), processedFile.getName());
	  
	    pcaLog.log(1, "Processed Image is: "+imageFile.getPath());
	    
	    switch (dest) {
	    case CCDPROCESS.SERVERPC:		
		try {
		    // Just the raw filename at the Web Server.
		    
		    // PUT $IMG_HOME/procfile.jpg procfile.jpg
		    if (planetariumCA.getServerPcLocal()) {
			client.send(imageFile.getPath(), processedFile.getName());
			pcaLog.log(1,"Pushed image to relay. Store as: "+processedFile.getPath());
		    } else {
			String spcHost = planetariumCA.getServerPcHost();
			int    spcPort = planetariumCA.getServerPcPort();
			client.forward(spcHost, spcPort, 
				       imageFile.getPath(), processedFile.getName());
			pcaLog.log(1,"Forwarded image to: "+spcHost+":"+spcPort+" Store as: "+
				   processedFile.getName());
		    }
		} catch (Exception iox) {
		    pcaLog.log(1,"Image-Transfer error while sending: "+iox);
		    processor.processError(CCDPROCESS.TRANSFER_FAULT,
					   "Image-Transfer error:",
					   iox);
		    manager.sigTaskFailed(this);    
		    return;
		}
		break;
	    default:
		// Lookup alternative dest-N in PCS configuration.		
		URL url = planetariumCA.getAlternativeDestination(dest);
		try { 
		    // Just the raw filename at the Web Server.
		  
		    // FWD $IMG_HOME/procfile.jpg procfile.jpg

		    client.forward(url.getHost(), url.getPort(), imageFile.getPath(), processedFile.getName());
		    pcaLog.log(1,"Forwarded image to: "+url.toString()+" Store as: "+processedFile.getName());
		} catch (Exception iox) {
		    pcaLog.log(1,"Image-Transfer error while forwarding: "+iox);
		    processor.processError(CCDPROCESS.TRANSFER_FAULT,
					   "Image-Transfer error:",
					   iox); 
		    manager.sigTaskFailed(this);  
		    return;
		}
		break;
	    }
	}

	opsLog.log(1, CLASS, name, "exec",
		   "Completed PCA Processing successfully");
	
	if (aborted) {
	    manager.sigTaskAborted(this);
	    return;
	}
	
	if (suspended) 
	    ; // nothing.
	else {
	    done = true; 
	    CCDPROCESS_DONE cdone = new CCDPROCESS_DONE(name);
	    cdone.setSuccessful(true);
	    cdone.setErrorNum(0);
	    cdone.setErrorString("");
	   
	    cdone.setFilename(processedFile.getName());
	    processor.processDone(cdone);
	   
	    manager.sigTaskDone(this);
	}
    }

    
    public void abort() {
	aborted = true;
	processor.processError(abortCode,
			       abortMessage,
			       null);
	pcaLog.log(1,name+"-Aborting");	
    }
    
   
    public void stop(long timeout) {}
    
   
    public void dispose() {
	pcaLog.log(1,name+"-Disposed");
    }

    
    public void reset() {
	done        = false;
	failed      = false;
	started     = false;
	aborted     = false;
	initialized = false;
	pcaLog.log(1,name+"-Reset");
    }

    public void suspend() { 
	suspended = true; 
	pcaLog.log(1,name+"-Suspended");
    }

   
    public void resume() {
	suspended = false; 
	pcaLog.log(1,name+"-Resuming");
    }
    
    public boolean canAbort() { return true;}
   
    public boolean isAborted() { return aborted; }

   
    public boolean isDone() { return done; }
    
    
    public boolean isFailed() { return failed; }
    
   
    public boolean isSuspended() { return suspended; }
    
   
    public boolean isInitialized() { return initialized; }

   
    public boolean isStarted() { return started; }

   
    public ErrorIndicator getErrorIndicator() { return errorIndicator; }
    
   
    public String getName() { return name; }

    /** Set the startup delay.*/
    public void setDelay(long delay) { this.delay = delay; }

   
    public TaskMonitor getMonitor() { return monitor; }
    
   
    public TaskManager getManager() { return manager; }
    
     /** Returns this Task's Worker Thread. The Worker thread is generally
      * allocated when the Task's exec() method is called.*/
    public Thread getWorker() { return worker; }
   
    public String toString() {
	return "POS_CcdProcess: "+name+
	    (initialized ? ":init"      : ":no-init")+
	    (started ?     ":started"   : ":no-started")+
	    (aborted ?     ":aborted"   : ":no-aborted")+
	    (done ?        ":done"      : ":no-done")+
	    (failed ?      ":failed"    : ":no-failed")+
	    (suspended ?   ":suspended" : ":no-suspended");
    }
    
    /** Set the abort code and message - called just prior to abort() by manager.*/
    public void setAbortCode(int abortCode, String abortMessage) {
	this.abortCode    = abortCode;
	this.abortMessage = abortMessage;
    }
     
}

/** $Log: POS_CcdProcess_Task.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */

