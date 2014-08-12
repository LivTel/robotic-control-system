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
import ngat.instrument.*;
import ngat.astrometry.*;
import ngat.message.RCS_TCS.*;

public class RotatorCardinalPointingTask extends ParallelTaskImpl {

    /** Failure code for target is/will become non-visible.*/
    public static final int TARGET_NOT_VISIBLE = 641301;

    /** The target to track.*/
    Source source;

    /** The instrument in use.*/
    Instrument instrument;

    /** Estimated execution time (ms).*/
    long execTime;

    /** Task to handle the rotator selection.*/	
    RotatorTask rotatorTask = null;

    /** Task to handle optional rotator float.*/	
    RotatorTask rotatorFloatTask = null;

    /** Flag set if no suitable CP solution can be found.*/
    boolean noSuitableCardinalPoints = false;

    /** Create a cardinal pointing task for the supplied instrument and target over the specified period.
     *@param name The name of the task.
     *@param manager The manager task.
     *@param source The target to track
     *@param instrument The instrument in use.
     *@param execTime Estimated execution time (ms).*
     */
    public RotatorCardinalPointingTask(String      name,
				       TaskManager manager,
				       Source      source,
				       Instrument  instrument,
				       long        execTime) {
	super(name, manager);
	  
	this.source = source;
	this.instrument = instrument;
	this.execTime = execTime;

    }

    /** Perform calculations before execution.*/
    @Override
	public void preInit() {
	super.preInit();
	
    
    }

    @Override
	public void onSubTaskFailed(Task task) {	
	
	super.onSubTaskFailed(task);
	ErrorIndicator ei = task.getErrorIndicator();
	
	taskLog.log(2, CLASS, name, "onSubTaskFailed",
		    "During CP routine: "+task.getName()+" failed due to: "+ei.getErrorString());
	
	RotatorTask rt = (RotatorTask)task;
	
	int runs = rt.getRunCount();	
	if (runs < 3) {
	    resetFailedTask(rt); 	
	} else {
	    taskLog.log(2, CLASS, name, "onSubTaskFailed",
			"Error: code: "+ei.getErrorCode()+" msg: "+ei.getErrorString());
	    failed(ei.getErrorCode(),
		   ei.getErrorString(),
		   ei.getException());
	}
	
    }
    
    /** Creates the TaskList for this TaskManager.*/
    @Override
	protected TaskList createTaskList() {

	long now = System.currentTimeMillis();

	double latitude = RCS_Controller.getLatitude();
	double domeLimit = RCS_Controller.getDomelimit();

	// work out the altaz at start and end of observation
	Position targetStart = source.getPosition();
	double dec = targetStart.getDec();
	double azm1 = targetStart.getAzimuth(now);
	double alt1 = targetStart.getAltitude(now);

	Position targetEnd = source.getPosition();
	double azm2 = targetEnd.getAzimuth(now+execTime);
	double alt2 = targetEnd.getAltitude(now+execTime);

	 // Can we see the bugger ?
	if (alt1 < domeLimit) {		
	    taskLog.log(2, CLASS, name, "", 
			"CP routine: Start position: Target Azm = "+Position.toDegrees(azm1,2)+
			", Alt = "+Position.toDegrees(alt1,2)+" TARGET IS SET");
	  
	    // FAIL soas to alert mgr.
	    failed = true;
	    errorIndicator.setErrorCode(TARGET_NOT_VISIBLE);
	    errorIndicator.setErrorString("Failed to create Cardinal pointing solution as target is non-visible.");
	    return null;
	}

	// Will we see the bugger ?
	if (alt2 < domeLimit) {		
	    taskLog.log(2, CLASS, name, "", 
			"CP routine: End position: Target Azm = "+Position.toDegrees(azm2,2)+
			", Alt = "+Position.toDegrees(alt2,2)+" TARGET WILL SET");
	    failed = true;
	    errorIndicator.setErrorCode(TARGET_NOT_VISIBLE);
	    errorIndicator.setErrorString("Failed to create Cardinal pointing solution as target likely to become non-visible.");
	    return null;  	   
	}
	
	taskLog.log(2, CLASS, name, "", 
		    "CP routine: Start position: Target Azm = "+Position.toDegrees(azm1,2)+
		    ", Alt = "+Position.toDegrees(alt1,2)+
		    ", End position: Target Azm = "+Position.toDegrees(azm2,2)+
		    ", Alt = "+Position.toDegrees(alt2,2));

	// transform to [0-360] acw from south for calcBearing. 
	double baz1 = azm1;
	if (azm1 < Math.PI)
	    baz1 = Math.PI-azm1;
	else
	    baz1 = 3.0*Math.PI-azm1;
	
	// calls SlaDbear(az1, alt1, az2, alt2)
	double p = JSlalib.calcBearing(baz1,alt1,Math.PI,latitude);

	// transform to [0-360] acw from south for calcBearing. 
	double baz2 = azm2;
	if (azm2 < Math.PI)
	    baz2 = Math.PI-azm2;
	else
	    baz2 = 3.0*Math.PI-azm2;
	
	// calls SlaDbear(az1, alt1, az2, alt2)
	double p2 = JSlalib.calcBearing(baz2,alt2,Math.PI,latitude);

	double instOffset = instrument.getRotatorOffset();
	taskLog.log(2, CLASS, name, "", 
		    "CP routine: Using instrument rotator offset: "+Position.toDegrees(instOffset,2));
	
	double maxDtl      = -999; // detect maximum Dist-to-limit
	double selectAngle = -1;   // selected CP value
	int    countValid  = 0;    // count valid CPs

	// Checking for valid CPs
	for (int isky = 0; isky < 4; isky++) {
	    
	    double sky = Math.toRadians(isky*90.0);
	    
	    double pc = p + instOffset + sky; // add instrument rotation correction and sky PA
	    double pd = p2 + instOffset + sky; // add instrument rotation correction and sky PA

	    // back into range: if inst-offset is ever negative this might not work...may need to upwards correct
	    
	    // 1. NOW
		
	    // correct angles
	    while (pc > Math.toRadians(240))
		    pc -= 2.0*Math.PI;
	    
	    while (pc < Math.toRadians(-240))
		pc += 2.0*Math.PI;
	    
	    double pc2 = pc;
	    System.err.println("CP routine: Start of obs...");

	    // calculate alternative wrap angles...
	    if (Math.toDegrees(pc) < -120.0) {
		pc2 = pc + 2.0*Math.PI;
		
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R1 = "+Position.toDegrees(pc,3)+" R2 =  "+Position.toDegrees(pc2,3));		    
	    } else if 
		(Math.toDegrees(pc) > 120.0) {
		pc2 = pc - 2.0*Math.PI;
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R1 = "+Position.toDegrees(pc,3)+" R2 = "+Position.toDegrees(pc2,3));	     
	    } else {
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R = "+Position.toDegrees(pc,3));
	    }

	    // 2. LATER
	    
	    while (pd > Math.toRadians(240))
		pd -= 2.0*Math.PI;
	    
	    while (pd < Math.toRadians(-240))
		pd += 2.0*Math.PI;
	    
	    double pd2 = pd;
	    System.err.println("CP routine: End of obs...");
	    
	    // calculate alternative wrap angles...
	    if (Math.toDegrees(pd) < -120.0) {
		pd2 = pd + 2.0*Math.PI;
		
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R1 = "+Position.toDegrees(pd,3)+" R2 =  "+Position.toDegrees(pd2,3));
	    } else if 
		(Math.toDegrees(pd) > 120.0) {
		pd2 = pd - 2.0*Math.PI;
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R1 = "+Position.toDegrees(pd,3)+" R2 = "+Position.toDegrees(pd2,3));	     
	    } else {
		System.err.println("CP routine: Sky: "+Position.toDegrees(sky,2)+
				   " R = "+Position.toDegrees(pd,3));
	    }
	    
	    // work out if pc or pc2 is ok and if pd or pd2 is ok within limits..
	    boolean pcok = false;
	    boolean pc2ok = false;
	    boolean pdok = false;
	    boolean pd2ok = false;
	    if (-90 < Math.toDegrees(pc) && Math.toDegrees(pc) < 90)
		pcok = true;
	    
	    if (-90 < Math.toDegrees(pc2) && Math.toDegrees(pc2) < 90)
		pc2ok = true;
	    
	    if (-90 < Math.toDegrees(pd) && Math.toDegrees(pd) < 90)
		pdok = true;
	    
	    if (-90 < Math.toDegrees(pd2) && Math.toDegrees(pd2) < 90)
		pd2ok = true;
	    
	    
	    if ((pcok || pc2ok) &&
		(pdok || pd2ok)) {
		System.err.println("CP routine: SKY "+Position.toDegrees(sky,2)+" IS VALID CARDINAL POINT");
		countValid++;
		
		// work out which end point is nearest to a limit
		// either pc to pd or pc2 to pd2? determine from dec which side of Z then decide
		// on direction of rotation cw or acw
		if (dec < latitude) {
		    // rotator angle increasing (towards +90)
		    
		    double dd = Math.abs(Math.toDegrees(pd)+90);
		    double dd2 = Math.abs(Math.toDegrees(pd2)+90);
		    double d = Math.min(dd,dd2);
		    if (d > maxDtl) {
			selectAngle = sky;  
			maxDtl = d;
		    }
		    System.err.println("CP routine: Approaches within "+d+" of -90 limit");
		    
		} else {
		    // rotator angle decreasing (towards -90)
		    
		    double dd = Math.abs(90 - Math.toDegrees(pd));
		    double dd2 = Math.abs(90 - Math.toDegrees(pd2));
		    double d = Math.min(dd,dd2);
		    if (d > maxDtl) {
			selectAngle = sky;
			maxDtl = d;
		    }
		    System.err.println("CP routine: Approaches within "+d+" of +90 limit");
		}
		
	    }  else
		System.err.println("CP routine: SKY "+Position.toDegrees(sky,2)+" IS NOT USABLE");
	    
	    
	} // next rot angle
	
	if (countValid == 0) {
	    taskLog.log(2, CLASS, name, "", 
			"CP routine: There were NO valid CARDINAL POINTS, reverting to Mount 0 followed by Float.");
	    
	    rotatorTask = new RotatorTask(name+"/ROT_MNT", 
					  this,
					  0.0, 
					  ROTATOR.MOUNT);  	  
	    taskList.addTask(rotatorTask);		    		  	    
	    rotatorFloatTask = new RotatorTask(name+"/ROT_FLOAT", 
					       this,
		    			       0.0, 
					       ROTATOR.FLOAT);	    	  	   
	    taskList.addTask(rotatorFloatTask);

	    try {
		taskList.sequence(rotatorTask, rotatorFloatTask);
	    } catch (Exception tx) {
		errorLog.log(1, CLASS, name, "createTaskList", 
			     "Failed to create Task Sequence for Cardinal pointing while reverted to mount 0/ float: "+tx);
		failed = true;
		errorIndicator.setErrorCode(TaskList.TASK_SEQUENCE_ERROR);
		errorIndicator.setErrorString("Failed to create Task Sequence for Observation_Sequence.");
		errorIndicator.setException(tx);
		return null;
	    }

	} else {
	    taskLog.log(2, CLASS, name, "", 
			"CP routine: There were "+countValid+" Valid Cardinal points, Selecting sky offset: "+Position.toDegrees(selectAngle,2)+
			       " with "+maxDtl+" degs to limit");
	    
	    rotatorTask = new RotatorTask(name+"/ROT_SKY", 
					  this,
					  selectAngle, 
					  ROTATOR.SKY);  	 
	    taskList.addTask(rotatorTask);		    	

	}
	
	
	
	return taskList;
    }
    
}
