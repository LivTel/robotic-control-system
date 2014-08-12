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

/** Implementation of a Rule which is triggered when the monitored
 * variable passes a specified threshold in a given direction. 
 * The rule becomes disabled after firing until the monitored 
 * variable drops below the threshold level again. 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/SimpleThresholdRule.java,v $
 * <br><br>
 * $Id: SimpleThresholdRule.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class SimpleThresholdRule extends Rule implements Logging{
    
    /** Constant: Indicates the threshold is to be crossed
     * with the monitored variable increasing.*/
    public final static int UP = 1; 

    /** Constant: Indicates the threshold is to be crossed
     * with the monitored variable decreasing.*/
    public final static int DOWN = 2;
    
    /** The triggering (threshold) level.*/
    protected double threshold;
    
    /** Threshold direction. One of the constants:- {UP, DOWN}.*/
    protected int dirn;

    /** Create a SimpleThresholdRule with the specified Filter, 
     * threshold and direction.
     * @param filter The filter which supplies the readings to monitor.
     * @param threshold The threshold which the monitored filter reading
     * must pass in order to trigger the Rule.
     * @param dirn The direction of change of monitored variable which
     * will trigger the Rule.
     */
    public SimpleThresholdRule(Filter filter, double threshold, int dirn) {
	super(filter);
	this.threshold = threshold;
	this.dirn = dirn;	
    }
    
    /** Create a SimpleThresholdRule with the specified Filter, 
     * threshold and direction UP.
     * @param filter The filter which supplies the readings to monitor.
     * @param threshold The threshold which the monitored filter reading
     * must pass in order to trigger the Rule.
     */
    public SimpleThresholdRule(Filter filter, double threshold) {
	this(filter, threshold, UP);
    }
    
    /** Tests the supplied Number field to see if it has crossed
     * the threshold level in the specified direction while the
     * Rule is enabled.
     * @param number The number field to test against the threshold 
     * condition.
     * @return True if the number satisfies the threshold test.
     */
    @Override
	public boolean checkCondition(Number number) {
	double arg = number.doubleValue();
	
	switch (dirn) {
	case UP:
	    if (spy)
	    spyLog.log(3, "ThreshholdRule", name, "checkCondition", 
		       "Testing arg: "+arg+" Against ascending thresh: "+threshold);
	    if (arg > threshold) 
		return true;    
	    return false;
	case DOWN:
	    if (spy)
	    spyLog.log(3, "ThreshholdRule", name, "checkCondition", 
		       "Testing arg: "+arg+" Against descending thresh: "+threshold);
	    if (arg < threshold) 
		return true;    
	    return false;
	default:
	    return false;
	}
    }

}

/** $Log: SimpleThresholdRule.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
