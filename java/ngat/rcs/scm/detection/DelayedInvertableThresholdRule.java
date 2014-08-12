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


/** Implements a Rule which triggers after the monitor variable <i>x</i> crosses 
 * the specified threshold level <i>T</i> in the given direction and stays below
 * /above an inversion level <i>&theta;</i> for at least the specified delay time 
 * <i>&tau;</i>. Once <i>x</i> has crossed the threshold level the rule becomes 
 * <b>enabled</b> i.e. it is allowed to fire, if <i>x</i> re-crosses the 
 * inversion level while the timer is running then the rule is disabled and the 
 * timer will then only be restarted if <i>x</i> drops below <i>T</i> again.
 * Once the rule has fired the <b>fired</b> flag is set and is not unset until 
 * <i>x</i> crosses back over the inversion level.
 *
 * <br><br> 
 * <dl>
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/DelayedInvertableThresholdRule.java,v $
 * </dl>
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: DelayedInvertableThresholdRule.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class DelayedInvertableThresholdRule extends SimpleThresholdRule {
   
    public static final String CLASS = "DelInvThreshRule";

    /** Specifies the timer delay before triggerring.*/
    protected long delay;

    /** Specifies the invert level.*/
    protected double invert;

    /** Records the timer start.*/
    protected long start;

    /** True if the Rule is enabled ready to fire after the delay/confidence period.*/
    protected boolean enabled;

    /** True if the Rule has fired at least once.*/
    protected boolean fired;
    
    /** Create a ReinforcedThresholdRule using the specified parameters.
     * @param filter    The Filter which supplies the readings to monitor.
     * @param threshold Threshold level for triggerring the Rule.
     * @param dirn      Direction in which the monitored variable must be
     *                  changing to trigger the Rule.
     * @param invert    Level at which we stop the delay timer.
     * @param delay     Time we must remain below invert level before triggering.
     */
    public DelayedInvertableThresholdRule(Filter filter, 
					  double threshold, 
					  int    dirn, 
					  double invert, 
					  long   delay) {
	super(filter, threshold, dirn);
	this.invert = invert;
	this.delay = delay;
	enabled = false;
	fired   = false;
    }
    
    /** Tests the supplied Number field to see if it has crossed
     * the threshold level in the specified direction while the
     * Rule is enabled and increments the confidence if so. If the 
     * monitored variable drops below the trigger level before the
     * Rule fires, the confidence is decremented.
     * @param number The number field to test against the threshold 
     * condition.
     * @return True if the number satisfies the threshold test and
     * the confidence has built up to the required level.
     */
    @Override
	public boolean checkCondition(Number number) {
	double arg = number.doubleValue();
	
	long now = System.currentTimeMillis();

	if (spy)
	    spyLog.log(3, CLASS, name, "checkCondition",		      
		       (enabled ? "" : "NOT ")+"enabled"+
		       (fired ? "" : "NOT ")+" fired"+
		       ", N-S="+(now-start));
	
	switch (dirn) {
	case UP:
	    if (enabled) {
		if (arg > invert) {		 
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
		    // Popped up over the inversion level.
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Disabled on rising inversion crossing ");
		    enabled = false;
		}
	    }
	    else if
		(! enabled){	

		if (! fired) {
		    // Haven't fired yet.
		    
		    if (arg > threshold) {
			start = now;
			enabled = true;
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Re-enabling on falling threshold crossing");
			
			return false;
		    } else {
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Currently disabled");
			
		    }
		    
		    return false;
		} else {
		    // Already fired at least once.
		    
		    //if (arg < threshold && arg > invert) {

		    if (arg < invert) {
			// ok to re-enable
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Re-enabling on entering inversion band");
			fired = false;		    
		    }		  
		    return false;
		}
	    }
	    return false;
	
	case DOWN:
	    if (enabled) {
		if (arg < invert) {		 
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
		    // Popped up over the inversion level.
		    if (spy)
			spyLog.log(3, CLASS, name, "checkCondition",
				   "Disabled on rising inversion crossing ");
		    enabled = false;
		}
	    }
	    else if
		(! enabled){	
		
		if (! fired) {
		    // Haven't fired yet.
		    
		    if (arg < threshold) {
			start = now;
			enabled = true;			
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Re-enabling on falling threshold crossing");
			
			return false;
		    } else {
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Currently disabled");
			
		    }
		    
		    return false;
		} else {
		    // Already fired at least once.
		    
		    // if (arg > threshold && arg < invert) { 
		    if (arg > invert) {
			// ok to re-enable
			if (spy)
			    spyLog.log(3, CLASS, name, "checkCondition",
				       "Re-enabling on entering inversion band");
			fired = false;		    
		    }		  
		    return false;
		}
	    }
	    return false;
	default:
	    return false;
	}
    }
    
}

/** $Log: DelayedInvertableThresholdRule.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/** */
