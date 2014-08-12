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
package ngat.rcs.tms.manager;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
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
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusProvider;
import ngat.instrument.*;
import ngat.astrometry.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;
import java.text.*;

/** This Task manages a temporary solution for the AutoFlats problem.
 */
public class TemporaryAutoFlatsTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, TMM/MGR = 40, CONFIG = 1400
	
    TwilightCalibrationTask twilightTask;
    
    Task twiTrkAz;
    Task twiTrkAlt;
  
    Task twiSlew;
    Task twiRot;
    Task twiRotFlt;

    /** ### Temp feature.*/
    String twilightInstId;

    /** ### Temp feature.*/
    long twilightTime;

    /** ### Temp feature.*/
    long startExposing;

    /** List of blank areas as exSol targets.*/
    Vector blanks;

    Catalog catalog;

    protected boolean badZone = false;
    protected double badNegLimit;
    protected double badPosLimit;

    /** The area to observe.*/
    private ExtraSolarSource blankArea;

    public TemporaryAutoFlatsTask(String      name,
				  TaskManager manager) {
	super(name, manager);	
	blanks = new Vector();
    }
  
    @Override
	public void onSubTaskFailed(Task task) {
	super.onSubTaskFailed(task); 
	failed(641401, "Temporary fail TempAutoflats operation due to subtask failure.."+task.getName(), null);
    }	
    
   
    @Override
	public void onSubTaskAborted(Task task) {
	super.onSubTaskAborted(task);
    }
    
    @Override
	public void onSubTaskDone(Task task) {
	super.onSubTaskDone(task);	
    }
    
    @Override
	public void onAborting() {
	super.onAborting();
    }
       
    @Override
	public void onDisposal() {
	super.onDisposal();
    }    
  
    @Override
	public void onCompletion() {
	super.onCompletion();	
	taskLog.log(WARNING, 1, CLASS, name, "onCompletion",
		    "Completed Temporary AutoFlats operation");
    }

    /** Generate parameters for subtasks.*/
    @Override
	public void preInit() {
	
	super.preInit();

	long now = System.currentTimeMillis();
	
	twilightInstId = config.getProperty("twilight.instrument");
	  
	if (twilightInstId == null) {
	    failed(641402, 
		   "Fail TempAutoflats operation due to no instrument specified");
	    return;
	}

	//Instrument twiCalInst = Instruments.findInstrument(twilightInstId);
	InstrumentDescriptor tid = new InstrumentDescriptor(twilightInstId);
	/*if (twiCalInst == null) {
	    failed(567, "Fail TempAutoflats operation due to unknown instrument: "+twilightInstId);
	}*/
	
	try {
	InstrumentCapabilitiesProvider tcap = ireg.getCapabilitiesProvider(tid);
	if (tcap == null) {
		 failed(641403, "Fail TempAutoflats operation due to unknown instrument: "+twilightInstId);
	}
	
	InstrumentStatusProvider tsp = ireg.getStatusProvider(tid);
	InstrumentStatus tstat = tsp.getStatus();
	if (((!tstat.isOnline()) || (!tstat.isFunctional()))) {
	    failed(641404, "Fail TempAutoflats operation due to instrument: "+twilightInstId+ " offline or non-operational");
	}
	
	} catch (Exception e) {
		e.printStackTrace();
		failed(641405, "Unable to determine operational status for twilight instrument: "+twilightInstId);
	}
	
	
	long sunrise = RCS_Controller.getObsDate().getSunrise();
	if (sunrise < now)
	    sunrise += 24*3600*1000L;
	long ttsunrise = sunrise - now;

	// ### TEMP Work out when we will actually want to start exposing i.e. sun elev = -8.8 degs

	Position sun = Astrometry.getSolarPosition();
	double alt = Math.abs(sun.getAltitude());

	// This is when we expect to start exposing hopefully (now+ttx)	
	//  just interpolate for sun hitting this elevation

	// NOTE we enter the negative of the angle in the config i.e. 8.8 means -8.8.

	double maxSunAngle = config.getDoubleValue("max.neg.sun.angle", 8.8);
	double ttx = ((alt-Math.toRadians(maxSunAngle))/alt) * ttsunrise;
	
	if (ttx < 0.0)
	    ttx = 0.0;

	// allow 10 minutes less than the actual time between sun at -maxSunAngle and sunrise
	twilightTime = ttsunrise - (long)ttx - 10*60*1000L;
	
	// Delay exposure after slew for ttx - 2 minutes assumed for slew!
	startExposing = (long)ttx - 2*60*1000L;

	if (startExposing < 0L)
	    startExposing = 0L;

	// If this is excessive > 80M, something is badly wrong so ignore..
	if (twilightTime > 80*60*1000L) {	    
	    taskLog.log(WARNING, 1, CLASS, name, "preInit",
			"** Calculated twilight calib time: "+
			(twilightTime/1000)+" secs looks too long, aborting");
	    
	    failed(641406, 
		   "Fail TempAutoflats operation due to dubious time calculation");		
	    return;
	}

	// If its too short we also fail
	if (twilightTime < 5*60*1000L) {
	    taskLog.log(WARNING, 1, CLASS, name, "preInit",
			"** Calculated twilight calib time: "+
			(twilightTime/1000)+" secs looks too short, aborting");
	    
	    failed(641407, 
		   "Fail TempAutoflats operation due to dubious time calculation");
	    return;
	}

	badZone = (config.getProperty("bad.zone") != null);
	badNegLimit = Math.toRadians(config.getDoubleValue("bad.zone.min", 400.0));
	badPosLimit = Math.toRadians(config.getDoubleValue("bad.zone.max", 600.0));

	// Check to see if we have any blank areas.
	blanks.clear();

	// Load the list of blanks from a catalog...

	File catfile = new File(config.getProperty("catalog"));
	if (catfile  != null) {
	    try {
		catalog = Astrometry.loadCatalog("SKY_FLATS", catfile);		
		taskLog.log(WARNING, 1, CLASS, name, "preInit",
			    "Loaded catalog: SKY_FLATS");	    
	    } catch (Exception e) {
		taskLog.log(WARNING, 1, CLASS, name, "preInit",
			    "Error reading blank sky catalog: "+e);
	    }
	}
    }
    
    /** Overridden to carry out specific work after the init() method is called.*/
    @Override
	public void onInit() {
	super.onInit();
	taskLog.log(INFO, 1, CLASS, name, "onInit",
		    "Starting Temporary AutoFlats operation: "+
		    "Waiting for "+(startExposing/1000)+" secs after slew before exposing"+
		    ", Time available: "+(twilightTime/1000)+" secs.");


	FITS_HeaderInfo.current_TELMODE.setValue("CALIBRATION");

	FITS_HeaderInfo.current_TAGID.setValue   ("CALIB"); 
	FITS_HeaderInfo.current_USERID.setValue  ("CALIB");
	FITS_HeaderInfo.current_PROPID.setValue  ("CALIB"); 
	FITS_HeaderInfo.current_GROUPID.setValue ("FLATS");

	FITS_HeaderInfo.current_GRPUID.setValue(new Integer(-1));
	FITS_HeaderInfo.current_GRPSEECO.setValue("NONE");
	// TODO any other constraints here ?
	FITS_HeaderInfo.current_GRPSKYCO.setValue("NONE");
	FITS_HeaderInfo.current_GRPNUMOB.setValue(new Integer(-1));

	FITS_HeaderInfo.current_GRPTIMNG.setValue("NONE");
	FITS_HeaderInfo.current_GRPMONP.setValue(new Double(0.0));
	FITS_HeaderInfo.current_GRPMONWN.setValue(new Double(0.0));
	
	FITS_HeaderInfo.current_RADECSYS.setValue("FK5");	
	FITS_HeaderInfo.current_EQUINOX.setValue(new Double(blankArea.getEquinox()));
	Position target = blankArea.getPosition();

// 	FITS_HeaderInfo.current_CAT_RA.setValue(FITS_HeaderInfo.toHMSString(target.getRA()));
// 	FITS_HeaderInfo.current_CAT_DEC.setValue(FITS_HeaderInfo.toDMSString(target.getDec()));
	FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(blankArea.getEpoch()));
	FITS_HeaderInfo.current_CAT_NAME.setValue(blankArea.getName());
	FITS_HeaderInfo.current_OBJECT.setValue(blankArea.getName());
	FITS_HeaderInfo.current_SRCTYPE.setValue("EXTRASOLAR");

	FITS_HeaderInfo.current_PM_RA.setValue(new Double(blankArea.getPmRA()));
	FITS_HeaderInfo.current_PM_DEC.setValue(new Double(blankArea.getPmDec()));
	FITS_HeaderInfo.current_PARALLAX.setValue(new Double(blankArea.getParallax()));
	//FITS_HeaderInfo.current_RADVEL   = blankArea.getRadialVelocity();
	FITS_HeaderInfo.current_RATRACK.setValue(new Double(0.0));
	FITS_HeaderInfo.current_DECTRACK.setValue(new Double(0.0));	

    }

    /** Creates the TaskList for this TaskManager. */
    @Override
	protected TaskList createTaskList() {

	twiTrkAz = new Track_Task(name+"/TT_AZ_ON",
				  this,
				  TRACK.AZIMUTH,
				  TRACK.ON);
	taskList.addTask(twiTrkAz);
	
	twiTrkAlt = new Track_Task(name+"/TT_ALT_ON",
				   this,
				   TRACK.ALTITUDE,
				   TRACK.ON);
	taskList.addTask(twiTrkAlt);
	
	// Create a target area near ra=lst.
	double lst = JSlalib.getGMST(System.currentTimeMillis(), RCS_Controller.getLongitude());
       
	blankArea = null;

	// Check we have any BAs.
	double close = 9999.0;
	double domeLimit = RCS_Controller.getDomelimit();

	// Locate a blank area nearest to LST.	
	//Iterator ib = blanks.iterator();
		    
	Iterator ib = catalog.listTargets().iterator();
	while (ib.hasNext()) {
	    ExtraSolarSource src = (ExtraSolarSource)ib.next();
	    double distance = Math.abs(src.getRA() - lst);
	    double altitude = src.getPosition().getAltitude();
	    double azimuth  = src.getPosition().getAzimuth();
	    if ((distance < close) &&
		(altitude > domeLimit ) &&
		(!badZone || ((azimuth < badNegLimit) || (azimuth > badPosLimit)))) 				
		{
		    close     = distance;
		    blankArea = src;
		}
	}
	
	// No blanks visible, use default.
	if (blankArea == null ) {

	    // RA = LST, Dec = 70-colat // This does not work in the Southern hemisphere.
	    double siteLat = RCS_Controller.getLatitude();
	    double dec = 0.0;
	    if (siteLat < 0.0) 
		dec = Math.toRadians(-70.0) - siteLat;
	    else
		dec = Math.toRadians(70.0) - siteLat;

	    blankArea = new ExtraSolarSource("blankarea@"+Math.rint(Math.toDegrees(lst)/15.0)+"H"+Math.rint(Math.toDegrees(dec))+"D");
	    // blankarea@22H15D
	    blankArea.setRA(lst);
	    blankArea.setDec(dec);
	    blankArea.setFrame(Source.FK5);
	    blankArea.setEquinox(2000.0f);
	    blankArea.setEpoch(2000.0f);
	    blankArea.setEquinoxLetter('J');

	}

	
	twiSlew = new SlewTask(name+"/TT_SLEW",
			       this,
			       blankArea);
	taskList.addTask(twiSlew);
	
	twiRot = new RotatorTask(name+"/TT_ROTMNT",
				 this,
				 0.0,
				 ROTATOR.MOUNT);
	taskList.addTask(twiRot);
	twiRot.setDelay(5000L);

	twiRotFlt = new RotatorTask(name+"/TT_ROTFLT",
				    this,
				    0.0,
				    ROTATOR.FLOAT);
	
	taskList.addTask(twiRotFlt);

	
	twilightTask = new TwilightCalibrationTask(name+"/TT_CALIB",
						   this,
						   twilightInstId,
						   twilightTime);
	taskList.addTask(twilightTask);
	twilightTask.setDelay(startExposing);
	// 2 minute delay after slewing.

	// (TrkAz+TrkAlt)&(Slew+(RotMnt:D5&RotFlt))&Cal
	try {
	    taskList.sequence(twiTrkAz,  twiSlew);
	    taskList.sequence(twiTrkAlt, twiSlew);
	    taskList.sequence(twiTrkAz,  twiRot);
	    taskList.sequence(twiTrkAlt, twiRot);	    
	    taskList.sequence(twiSlew,   twilightTask);
	    taskList.sequence(twiRot,    twiRotFlt);
	    taskList.sequence(twiRotFlt, twilightTask);

	} catch (TaskSequenceException tx) {
	    errorLog.log(1, CLASS, name, "createTaskList", 
			 "Failed to create Task Sequence for TemporaryAutoFlatsTask: "+tx);
	    failed = true;
	    errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
	    errorIndicator.setErrorString("Failed to create Task Sequence for TemporaryAutoFlatsTask.");
	    errorIndicator.setException(tx);
	    return null;
	}

	return taskList;	

    }


}
