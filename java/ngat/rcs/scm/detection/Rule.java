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

/** Abstraction of the knowledge of a (set) of condition(s) 
 * which must be satisfied when invoked in order to fire.
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Rule.java,v $
 * <br><br>
 * $Id: Rule.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public abstract class Rule {

    /** The Filter which a Rule examines to determine whether 
     *to fire.*/
    protected Filter filter;

    /** Set to indicate that this Rule's detailed operations are to be logged.*/
    protected boolean spy;

    /** Detailed operations 'spy' logger.*/
    protected Logger spyLog;

    /** Rules name.*/
    protected String name;

    /** Create a Rule using the specified Filter as the source
     * of readings to test against the firing condition.
     * @param filter The filter which supplies the readings to test.
     */
    public Rule(Filter filter) {
	this.filter = filter;	
    }

  
    /** The conditions applicable to this rule are applied by
     * calling checkCondition() on the results of the attached
     * Filter's filteredReading() - the Rule either fires or fails
     * depending on whether the reading passed the test.
     * @return True if the Rule's conditions are satisfied.
     */
    public boolean invoke() {
	//System.out.println("RULE-Invoke:");
	return (checkCondition(filter.readout()));
    }
    
    /** Concrete subclasses should override this to supply a
     * suitable test on the supplied Number field.
     * @param n The number field to test against the Rule's 
     * firing condition.
     * @return True if the number satisfies the Rule's test.
     */
    public abstract boolean checkCondition(Number n);

    /** Set the filter to use as source of readings to test
     * against the firing condition. If the filter is null
     * this returns silently and does nothing.
     * @param filter The <b>non null</b> filter to use.
     */
    public void setFilter(Filter filter) {
	if (filter != null)
	    this.filter = filter;
    }

    /** @return The current Filter in use.*/
    public Filter getFilter() { return filter; }

    /** Sets the name.*/
    public void setName(String name) { this.name = name;}

    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }

    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName) { spyLog = LogManager.getLogger(spyLogName); }

}

/** $Log: Rule.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.3  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
