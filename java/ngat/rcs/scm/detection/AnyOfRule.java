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
package ngat.rcs.scm.detection;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.logging.*;

import java.util.*;

/** Implementation of a Rule which is triggered when the monitored
 * variable equals ANY OF the test values for a specified period.
 * 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/AnyOfRule.java,v $
 * <br><br>
 * $Id: AnyOfRule.java,v 1.2 2007/07/05 11:27:09 snf Exp $
 */
public class AnyOfRule extends Rule implements Logging{
    
  
    public static final String CLASS = "AnyOfRule";


    /** How long to monitor the values before triggering.*/
    protected long delay;
 
    /** The values to test the output of the (Discrete) filter against.*/
    protected List testValues;

    /** Records the timer start.*/
    protected long start;

    /** True if the Rule is enabled ready to fire after the delay/confidence period.*/
    protected boolean enabled;

    /** True if the Rule has fired at least once.*/
    protected boolean fired;
    
    protected Logger logger;

    /** Create an AnyOfRule with the specified Filter, 
     * and condition.
     * @param filter The filter which supplies the readings to monitor.   
     * @param testValues The value to test the output of the (Discrete) filter against.  
     */
    public AnyOfRule(Filter filter, List testValues, long delay) {
	super(filter);
	this.testValues = testValues;
	this.delay      = delay;
	enabled = false;
	fired   = false;
	logger = LogManager.getLogger("TRACE");
    }
    
    /** Tests the supplied Number field to see if it is equal to any of the test values
     * while the Rule is enabled.
     * @param number The discrete filter reading to test against test value.
     * @return True if the number satisfies the test.
     */
    @Override
	public boolean checkCondition(Number number) {

	long now = System.currentTimeMillis();

	int arg = number.intValue();
	if (spy)
	    spyLog.log(3, this.getClass().getName(), "Testing Filter-reading: "+arg+" Against Test-values");


	if (enabled) {
		
	    if (isValue(arg)) {
		
		// Timer has run full period.
		if ((now - start) > delay) {	
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Triggered, After delay: "+(now - start)/1000+" secs");
		    enabled = false;
		    fired   = true;
		    return true;
		} else {
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Enabled, Confidence: "+Math.rint(100*(now - start)/delay)+"%");
		}
	    } else {
		// Alternate reading.
		if (spy)
		    spyLog.log(3, CLASS, name, "checkCondition",
			       "Disabled on alternative value");
		enabled = false;
	    }
	} else {	
	    
	    if (! fired) {
		// Haven't fired yet.
		
		if (isValue(arg)) {
		    start = now;
		    enabled = true;
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Re-enabling on valid anyof reading");
		    
		    return false;
		} else {
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Currently disabled");
		    
		}
		
		return false;
	    } else {
		// Already fired at least once.
			
		if (isValue(arg)) {
		    // ok to re-enable
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Re-enabling on valid anyof reading");
		    fired = false;		    
		}		  
		return false;
	    }
	    
	}
	return false;
    }

    /** True if arg is one of the test values.*/
    private boolean isValue(int arg) {

	Iterator it = testValues.iterator();
	while (it.hasNext()) {
	    int tv = ((Integer)it.next()).intValue();
	    if (tv == arg)
		return true;
	}

	return false;

    }
    
}

/** $Log: AnyOfRule.java,v $
/** Revision 1.2  2007/07/05 11:27:09  snf
/** checkin
/**
/** Revision 1.1  2007/06/14 07:41:40  snf
/** Initial revision
/**
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
