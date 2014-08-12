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


import java.util.*;
import java.net.*;
import java.io.*;
import java.text.*;

import ngat.net.*;
import ngat.math.*;
import ngat.astrometry.*;
import ngat.util.logging.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;
import ngat.message.POS_RCS.*;
import ngat.rcs.gui.*;

/** Test version of a Generic command handler for POS_RCS commands.
 * Just returns some made up data for now.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_GenericCommandImpl.java,v 1.2 2007/05/25 07:29:10 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_GenericCommandImpl.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class POS_GenericCommandImpl extends POS_CommandImpl {

    public static final String CLASS = "POS_Generic_CommandImpl";

    static final int DEFAULT_LEVEL = 1;

    static final int ZERO = 0;

    static final long DEFAULT_HANDLING_TIME = 60000L;

    static int frameCount = 0;

    static Hashtable ccdStatus;

    static Hashtable metStatus;

    static Hashtable telStatus;

    protected NumberFormat nf;

    protected static Logger pcaLog = LogManager.getLogger("PCA");

    PlanetariumControlAgent planetariumCA = 
	(PlanetariumControlAgent)PlanetariumControlAgent.getInstance();

    POS_Queue pQ = POS_Queue.getInstance();

    static {
	ccdStatus = new Hashtable();
	ccdStatus.put("CCDSTATE", "EXPOSING");
	ccdStatus.put("CCDTEMP", "-24.5");
	ccdStatus.put("CCDBIN", "2");
	ccdStatus.put("CCDFILT0", "Sloan-B");
	ccdStatus.put("CCDFILT1", "clear");
	ccdStatus.put("REQUESTEDEXPOSURE", "20.0");
	ccdStatus.put("ELAPSEDEXPOSURE", "15.0");
	
	metStatus = new Hashtable();
	metStatus.put("METSTATE", "OPERATIONAL");
	metStatus.put("HUMIDITY", "70");
	metStatus.put("TEMP", "+24.44");
	metStatus.put("WINDSPEED", "23");
	metStatus.put("WINDDIRECTION", "240");
	metStatus.put("PRESSURE", "780");
	metStatus.put("RAIN", "CLEAR");

	telStatus = new Hashtable();
	telStatus.put("UT1", "15:23:24.6");
	telStatus.put("LST", "03:12:23.3");
	telStatus.put("MJD", "58102.345");
	telStatus.put("TARGET_RA", "15:24:45.45");
	telStatus.put("TARGET_DEC", "+12:13:34.4");
	telStatus.put("AZIMUTH_DEMAND", "124.4");
	telStatus.put("AZIMUTH_ACTUAL", "112.2");
	telStatus.put("ALTITUDE_DEMAND", "63.34");
	telStatus.put("ALTITUDE_ACTUAL", "65.3");
	telStatus.put("ROTATOR_DEMAND", "232.3");
	telStatus.put("ROTATOR_ACTUAL", "165.3");
	telStatus.put("ENCLOSURE1", "OPEN");
	telStatus.put("ENCLOSURE2", "OPEN");
	telStatus.put("MIRROR_COVER", "OPEN");
	telStatus.put("FOLD_MIRROR", "POSITION1");
	telStatus.put("TELSTATE", "MOVING");
	
    }

    public POS_GenericCommandImpl(JMSMA_ProtocolServerImpl serverImpl, POS_TO_RCS command) {
	super(serverImpl, command);
	nf = NumberFormat.getInstance();
	nf.setMaximumFractionDigits(4);
    }
    
    /** Generic command impl is used by commands which do not need to queue.
     * This method returns ZERO (=0).*/
    public int getLevel() { return ZERO; }
    
    /** Generic command impl is used by commands which do not need to queue.
     * This method returns false.*/
    public boolean enqueue() { return false; }


    /** Returns the expected maximum duration of this command processing.
     * For these unqueued implementations just a small overhead.*/
    public long getDuration() {
	return DEFAULT_HANDLING_TIME;	    
    }

    
    public long getDefaultHandlingTime() { return DEFAULT_HANDLING_TIME; }
    

    public Task createTask() { return null; }

    public COMMAND_DONE processCommand() {
	
	POS_TO_RCS_DONE done = null;

	if (command instanceof ABORT) {
	    // #### ABORT needs an isAll() method to say if its ALL rather than a silly int code.

	    done  = new ABORT_DONE(command.getId()); 

	    // Is client known to us.
	    if (!planetariumCA.isAuthorized(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.INVALID_RTOC);
		done.setErrorString("Unknown RTOC: "+command.getControllerAddress());
		done.setSuccessful(false);
		return done;
	    }
	    
	    // Are they supposed to be IN_CONTROL
	    if (!planetariumCA.isInControl(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.NOT_IN_CONTROL);
		done.setErrorString(command.getControllerAddress()+" is not in control at this time");
		done.setSuccessful(false);
		return done;
	    }
	   
	    int requestCode = ((ABORT)command).getRequestCode();
	    if ( ! ((ABORT)command).getAll()) {
		// Just one to do. See if its running.
		// Search the POS_Queue for a PD with requestNumber == requestCode.
		synchronized (pQ) {		    
		    // See if its executing now.
		    POS_CommandImpl exec = pQ.getExecutor();
		    if (exec != null &&
			exec.getCommand().getRequestNumber() == requestCode) {
			// Abort the task itself via the handler's abort(Str, Str) method.
			exec.abort(POS_TO_RCS.CLIENT_ABORTED,
				   "Task aborted during execution");
			
			done.setSuccessful(true);
			return done;   
		    }
		    
		    // See if its pending.
		    try {
			POS_CommandImpl handler = pQ.findRequest(requestCode); // OK got it
			// Guaranteed not null ?
			if (handler != null) {			 				
			    // Force the handler to send fail response to its client.
			    // It will unqueue itself when it is disposed... Maybe we should do that now
			    // while we have the synch lock on the POS_Q as dispose() is called by its SCT.
			    //##
			    //## pQ.removeRequest(handler); If it fails so what..
			    //##
			    POS_TO_RCS_DONE pdone  = new POS_TO_RCS_DONE(handler.getCommand().getId());
			    pdone.setSuccessful(false);
			    pdone.setErrorNum(POS_TO_RCS.CLIENT_ABORTED); 
			    pdone.setErrorString("Task aborted while pending by command: ABORT "+requestCode);
			    handler.processDone(pdone);
			}			
			
			done.setSuccessful(true);
			return done;   
		    } catch (NoSuchElementException nsx) {
			// No such request on queue.
			
			done.setSuccessful(false);
			done.setErrorNum(ABORT.NO_SUCH_REQUEST);
			done.setErrorString("Failed to abort Request: "+requestCode+
					    " No such request was queued - perhaps its already completed.");
			return done;
		    }
		}
	    } else {
		
		synchronized (pQ) {
		    // Abort Executing process.
		    POS_CommandImpl exec = pQ.getExecutor();
		    if (exec != null) {
			exec.abort(POS_TO_RCS.CLIENT_ABORTED,
				   "Task aborted during execution");
		    }
		    
		    // Abort all pending processes.		  
		    POS_CommandImpl handler = null;
		    
		    Iterator pending = pQ.listElements().iterator();
		    while (pending.hasNext()) {
			handler = (POS_CommandImpl)pending.next();
			if (handler != null) {	
			    // Force the handler to send fail response to its client.
			    POS_TO_RCS_DONE pdone  = new POS_TO_RCS_DONE(handler.getCommand().getId());
			    pdone.setSuccessful(false);
			    pdone.setErrorNum(POS_TO_RCS.CLIENT_ABORTED); 
			    pdone.setErrorString("Task aborted while pending by command: ABORT "+requestCode);
			    handler.processDone(pdone);
			}
		    }
		   
		    done.setSuccessful(true);
		    return done;   		    
		}   
	    }
	} else if
	    // NO DO ######
	    (command instanceof CCDOBSERVE) {
	    done = new CCDOBSERVE_DONE("DODGY-CCDOBSERVE");
	    ((CCDOBSERVE_DONE)done).setFrameNumber(++frameCount);
	    return done;
	} else if
	    // NO DO ######
	    (command instanceof CCDPROCESS) {
	    done = new CCDPROCESS_DONE("DODGY-CCDPROCESS");
	    return done;
	} else if
	    (command instanceof CCDSTATUS) {

	    // We need to set this from the latest RATCAM status via:-	
	    String posInstName = planetariumCA.getInstrumentName();
	    if (posInstName == null || posInstName.equals("")) {
		ccdStatus.put("CCDSTATE",   "UNKNOWN");
		ccdStatus.put("INSTRUMENT", "UNKNOWN"); // ## TEMp ##
	    } else {		
		CCS_Status ccsStatus = CCSPool.getInstance(posInstName).latest();
		
		// .. now translate all the required values ### CHECK THE UNITS  ### 
		// .. and convert state ints to String messages.
		
		ccdStatus.put("CCDSTATE",          ccsStatus.toModeString(ccsStatus.currentMode));			    
		ccdStatus.put("FILTERWHEEL",       ccsStatus.toFilterStatusString(ccsStatus.filterWheelStatus));
		ccdStatus.put("CCDTEMP",           ""+ccsStatus.temperature);
		ccdStatus.put("CCDBIN",            ""+ccsStatus.nSBin+"x"+ccsStatus.nPBin);
		ccdStatus.put("CCDFILT0",          (ccsStatus.lowerFilterWheelType != null ?
						    ccsStatus.lowerFilterWheelType : "UNKNOWN")); // #### Swap ?
		ccdStatus.put("CCDFILT1",          (ccsStatus.upperFilterWheelType != null ?
						    ccsStatus.upperFilterWheelType : "UNKNOWN")); // #### Swap ??
		ccdStatus.put("REQUESTEDEXPOSURE", ""+(ccsStatus.exposureLength/1000.0));
		ccdStatus.put("ELAPSEDEXPOSURE",   ""+(ccsStatus.elapsedExposureTime/1000.0));
	    }
	    done = new CCDSTATUS_DONE("TEST-CCDSTATUS");	   
	    ((CCDSTATUS_DONE)done).setStatus(ccdStatus);
	} else if
	    (command instanceof METSTATUS) {

	    done = new METSTATUS_DONE("TEST-METSTATUS"); 
	    TCS_Status tcsStatus = StatusPool.latest();
	    metStatus.put("METSTATE",      TCS_Status.codeString(tcsStatus.meteorology.wmsStatus));
	    metStatus.put("HUMIDITY",      nf.format(tcsStatus.meteorology.humidity*100.0));
	    metStatus.put("TEMP",          nf.format(tcsStatus.meteorology.extTemperature));
	    metStatus.put("WINDSPEED",     nf.format(tcsStatus.meteorology.windSpeed));
	    // This may be (360-angle) ????
	    metStatus.put("WINDDIRECTION", nf.format(tcsStatus.meteorology.windDirn));
	    metStatus.put("PRESSURE",      nf.format(tcsStatus.meteorology.pressure));
	    metStatus.put("RAIN",          TCS_Status.codeString(tcsStatus.meteorology.rainState));	    
	    ((METSTATUS_DONE)done).setStatus(metStatus);
	} else if
	    (command instanceof TELSTATUS) {

	    done = new TELSTATUS_DONE("TEST-TELSTATUS");
	    TCS_Status tcsStatus = StatusPool.latest();
	    telStatus.put("UT1",             Position.formatHMSString(
						tcsStatus.time.ut1/13750.98, ":").trim());
	    telStatus.put("LST",             Position.formatHMSString(
						tcsStatus.time.lst/13750.98, ":").trim());
	    telStatus.put("MJD",             ""+tcsStatus.time.mjd);
	    telStatus.put("TARGET_RA",       Position.formatHMSString(tcsStatus.source.srcRa, ":").trim());
	    telStatus.put("TARGET_DEC",      Position.formatDMSString(tcsStatus.source.srcDec, ":").trim());
	    telStatus.put("AZIMUTH_DEMAND",  nf.format(tcsStatus.mechanisms.azDemand));
	    telStatus.put("AZIMUTH_ACTUAL",  nf.format(tcsStatus.mechanisms.azPos));
	    telStatus.put("ALTITUDE_DEMAND", nf.format(tcsStatus.mechanisms.altDemand));
	    telStatus.put("ALTITUDE_ACTUAL", nf.format(tcsStatus.mechanisms.altPos));
	    telStatus.put("ROTATOR_DEMAND",  nf.format(tcsStatus.mechanisms.rotDemand));
	    telStatus.put("ROTATOR_ACTUAL",  nf.format(tcsStatus.mechanisms.rotPos));
	    telStatus.put("ENCLOSURE1",      TCS_Status.codeString(tcsStatus.mechanisms.encShutter1Pos));
	    telStatus.put("ENCLOSURE2",      TCS_Status.codeString(tcsStatus.mechanisms.encShutter2Pos));
	    telStatus.put("MIRROR_COVER",    TCS_Status.codeString(tcsStatus.mechanisms.primMirrorCoverPos));
	    telStatus.put("FOLD_MIRROR",     TCS_Status.codeString(tcsStatus.mechanisms.foldMirrorPos).replace(' ', '_')) ;   			  
	    telStatus.put("TELSTATE",        TCS_Status.codeString(tcsStatus.state.telescopeState)); 

	    // We need the file tail without any Root or URL stuff pre-pended.
	    File schedule =  new File(TMM_TaskSequencer.getInstance().getCurrentScheduleFilePath());	    
	    telStatus.put("SCHEDULE_FILE",   (schedule != null ? schedule.getName() : "UNKNOWN"));
	    ((TELSTATUS_DONE)done).setStatus(telStatus);
	} else if
	    (command instanceof GETQUEUE) {

	    // Get the list of processes.
	    done = new GETQUEUE_DONE("TEST-GETQUEUE");
	    Vector vec =  POS_Queue.getInstance().listElements();
	   
	    Vector svec = new Vector(); 
	    POS_CommandImpl exec = pQ.getExecutor();
	    if (exec != null) {
		svec.add(new IntegerPair(exec.getCommand().getRequestNumber(), GETQUEUE.EXECUTING));
		//System.err.println("\nQueue: 0"+
		//	   " "+exec.getCommand().getRequestNumber()+
		//	   " : EXECUTING");
	    }
	    
	    for (int i = 0; i < vec.size(); i++) {
		POS_CommandImpl handler = (POS_CommandImpl)vec.get(i);
		//System.err.println("\nQueue: "+i+
		//	   " "+handler.getCommand().getRequestNumber()+
		//	   " : PENDING");
		svec.add(new IntegerPair(handler.getCommand().getRequestNumber(), GETQUEUE.PENDING));
	    } 
	    
	    ((GETQUEUE_DONE)done).setProcessList(svec);	
	} else if
	    (command instanceof TESTLINK) {

	    // Set the ReturnCode depending on current mode of operation.
	    // TESTLINK.PLANETARIUM_MODE or TESTLINK.NOT_PLANETARIUM_MODE

	    // ## We need to extract the current RCS and TCS subsystem statii here. TBD ###
	    if (! RCS_Controller.controller.isOperational()) {
		done = new TESTLINK_DONE("TEST-TESTLINK");
		((TESTLINK_DONE)done).setReturnCode(POS_TO_RCS.ENGINEERING_MODE);		
	    } else {

		boolean up = pQ.accept(); // True if PCA is aic AND PCA NOT initializing
		done = new TESTLINK_DONE("TEST-TESTLINK");
	    
		// Is client known to us.
		if (!planetariumCA.isAuthorized(command.getControllerAddress())) {	
		    ((TESTLINK_DONE)done).setReturnCode(POS_TO_RCS.INVALID_RTOC);
		}

		// Are they supposed to be IN_CONTROL
		if (!planetariumCA.isInControl(command.getControllerAddress())) {	
		    ((TESTLINK_DONE)done).setReturnCode(POS_TO_RCS.NOT_IN_CONTROL);
		}

		
		if (up) {
		    if (planetariumCA.getInitializedStatus())
			((TESTLINK_DONE)done).setReturnCode(TESTLINK.PLANETARIUM_MODE);
		    else		   
			((TESTLINK_DONE)done).setReturnCode(TESTLINK.PLANETARIUM_INITIALIZING);
		} else {
		    if (RCS_Controller.controller.isOperational()) {
			
			if (planetariumCA.isScheduledWindow()) {
			    if (planetariumCA.getPcaInit()) {
				((TESTLINK_DONE)done).setReturnCode(TESTLINK.PLANETARIUM_INITIALIZING);
			    } else {
				// CA may be operational and PCA in-window but overridden.
				((TESTLINK_DONE)done).setReturnCode(TESTLINK.OVERRIDDEN);
			    }
			} else {
			    // CA up but PCA not in-window 
			    ((TESTLINK_DONE)done).setReturnCode(TESTLINK.NOT_PLANETARIUM_MODE);
			}
			
		    } else {
			((TESTLINK_DONE)done).setReturnCode(POS_TO_RCS.NOT_OPERATIONAL);
		    }
		}   
	    }  
	} else if
	    (command instanceof OFFLINE) {
	    // ## TBD Check the WSF to see if the time is over the limit of the current window.
	    // ## post PLANETARIUM.OFF then INTERRUPT that should do it
	    // ## maybe setAbortCode for PCA to OFFLINE_COMMAND etc ???
	    done = new OFFLINE_DONE("TEST-OFFLINE");

	    // Is client known to us.
	    if (!planetariumCA.isAuthorized(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.INVALID_RTOC);
		done.setErrorString("NOT_A_VALID_RTOC");
		done.setSuccessful(false);
		return done;
	    }
	    
	    // Are they supposed to be IN_CONTROL
	    if (!planetariumCA.isInControl(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.NOT_IN_CONTROL);
		done.setErrorString("NOT_IN_CONTROL");
		done.setSuccessful(false);
		return done;
	    }

	    // Is the end time beyond the end of current Window.
	    long until = ((OFFLINE)command).getUntil();
	    long winend = planetariumCA.getEndCurrentWindow();
	    System.err.println("POS:GCI::Checking OFFLINE TIMES: PCS-Window until: "+winend+" Requested: "+until);
	    if (winend < until) {
		done.setErrorNum(OFFLINE.WINDOW_EXCEEDED);
		done.setErrorString("WINDOW_EXCEEDED");
		done.setSuccessful(false);
		return done;
	    }
	    System.err.println("Pretending to go offline ### TBD ###");
	    EventQueue.postEvent("PLANETARIUM.OVERRIDE");
	    EventQueue.postEvent("interrupt");
	    done = new OFFLINE_DONE("TEST-OFFLINE");
	} else if
	    (command instanceof USERID) { 
	    done = new USERID_DONE("TEST-USERID");
	   
	    String uid = ((USERID)command).getUserId();

	    // Is client known to us.
	    if (!planetariumCA.isAuthorized(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.INVALID_RTOC);
		done.setErrorString("NOT_A_VALID_RTOC");
		done.setSuccessful(false);
		return done;
	    }
	    
	    // Are they supposed to be IN_CONTROL
	    if (!planetariumCA.isInControl(command.getControllerAddress())) {
		done.setErrorNum(POS_TO_RCS.NOT_IN_CONTROL);
		done.setErrorString("NOT_IN_CONTROL");
		done.setSuccessful(false);
		return done;
	    }
	   
	    // ## Set the currentUser param in PCA ?
	    // ## That should be all - DO WE WANT TO TRACK_OFF now
	    // ## as this indicates the start of a new SLOT ???.
	    pcaLog.log(1, "Setting POS UserID: "+uid);
	    planetariumCA.setCurrentUserName(uid);
	    done = new USERID_DONE("TEST-USERID");
	} else if
	    (command instanceof SET_WINDOWS) {
	    
	    done = new SET_WINDOWS_DONE("TEST-SET-WIN");
	    TreeSet windows = ((SET_WINDOWS)command).getWindows();

	    TMM_TaskSequencer tmm = TMM_TaskSequencer.getInstance();
	    
	    try {
		tmm.replaceWindows(windows);
		pcaLog.log(1, "Replaced current windows:");
		
		windows = tmm.getScheduledWindows();
		WindowSchedule.TimeWindow win = null;

		Iterator it = windows.iterator();
		while (it.hasNext()) {
		    win = (WindowSchedule.TimeWindow)it.next();
		    System.err.println("-"+win);
		}

		try {
		    tmm.saveSchedule();		    
		    pcaLog.log(1, "Successfully saved new windows to file");
		} catch (IOException iox) {
		    // needa better error message.
		    done.setErrorNum(SET_WINDOWS.ILLEGAL_WINDOWS);
		    done.setErrorString("Unable to save new schedule: "+iox);
		    done.setSuccessful(false);
		    return done;
		}

	    } catch (IllegalArgumentException iax) {
		done.setErrorNum(SET_WINDOWS.ILLEGAL_WINDOWS);
		done.setErrorString("Unable to enter new schedule: "+iax);
		done.setSuccessful(false);
		pcaLog.dumpStack(1, iax);
		return done;
	    }	    
	} else if
	    (command instanceof READ_WINDOWS) { 
	    done = new READ_WINDOWS_DONE("TEST-READ-WIN");
	    
	    TMM_TaskSequencer tmm = TMM_TaskSequencer.getInstance();
	    TreeSet windows = tmm.getScheduledWindows();
	    ((READ_WINDOWS_DONE)done).setWindows(windows);	    
	}
	
	done.setSuccessful(true);
	return done;
    }

    /** Makes up an <i>appropriate</i> DONE and set its error code.*/
    public void processError(int errorCode, String errorMessage, Exception e) {
	POS_TO_RCS_DONE done = new POS_TO_RCS_DONE(command.getId());
	done.setSuccessful(false);
	done.setErrorNum(errorCode);
	done.setErrorString(errorMessage+" : "+e);
	processDone(done);
    }
    
    public void dispose() {}

    /** This method is used to try and abort any attached task (ignore!).*/
    public void abort(int code, String message) {}

    public void exceptionOccurred(Object source, Exception exception) {
	pcaLog.log(1, CLASS, command.getId(), "ExceptionCallback",
		   "POS_GenericImpl::Source: "+source+" Exception: "+exception);
    }


}

/** $Log: POS_GenericCommandImpl.java,v $
/** Revision 1.2  2007/05/25 07:29:10  snf
/** changed logging for exception callback.
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/06/08 16:27:27  snf
/** Initial revision
/** */
