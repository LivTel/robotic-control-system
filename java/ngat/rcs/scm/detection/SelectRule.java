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
 * variable equals the test value.
 * 
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/SelectRule.java,v $
 * <br><br>
 * $Id: SelectRule.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public class SelectRule extends Rule implements Logging{
    
    /** Set to indicate that this rule triggers If the filter reading is 
     * EQUAL TO test value. If false, the rule triggers if the filter reading
     * IS NOT EQUAL TO the test value.*/
    protected boolean isValue;

    /** The value to test the output of the (Discrete) filter against.*/
    protected int testValue;

    protected Logger logger;

    /** Create a SelectRule with the specified Filter, 
     * and condition.
     * @param filter The filter which supplies the readings to monitor.   
     * @param testValue The value to test the output of the (Discrete) filter against.  
     * @param isValue True if the test is for EQUALS otherwise NOT_EQUALS.
     */
    public SelectRule(Filter filter, int testValue, boolean isValue) {
	super(filter);
	this.testValue = testValue;
	this.isValue   = isValue;
	logger = LogManager.getLogger("TRACE");
    }
    
    /** Tests the supplied Number field to see if it is equal to the test value
     * while the Rule is enabled.
     * @param number The discrete filter reading to test against test value.
     * @return True if the number satisfies the test.
     */
    @Override
	public boolean checkCondition(Number number) {
	int arg = number.intValue();
	if (spy)
	    spyLog.log(3, this.getClass().getName(), "Testing Filter-reading: "+arg+" Against Test-value: "+testValue);
	if (isValue) {
	    if (arg == testValue) 	
		return true;  
	    return false;
	} else {
	    if (arg != testValue) 	
		return true; 
	    return false;
	}	
    }
    
}

/** $Log: SelectRule.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
