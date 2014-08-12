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

/** Multiplexer combines the outputs of several Filters.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: Multiplexer.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Multiplexer.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Multiplexer implements Filter {

    protected String name;

    protected List elements;

    protected boolean spy;

    protected Logger spyLog;

    /** Create a Multiplexer.*/
    public Multiplexer(String name) {
	this.name = name;
	elements  = new Vector();
    }

    /** Combine specified Filter output with specified weight.*/
    public void addElement(double weight, Filter filter) {
	elements.add(new Weight(weight, filter));
    }

    /** Return the filter reading.*/
    public Number readout() {

	Weight element = null;
	double weight = 0.0;
	Filter filter = null;
	Number reading = null;
	double x = 0.0;
	double combine = 0.0;
	
	if (spy)
		spyLog.log(2, "Mux:"+name+
			   "Initializing for readout");
	
	Iterator els = elements.iterator();
	while (els.hasNext()) {
	    
	    element = (Weight)els.next();

	    weight = element.getWeight();

	    filter = element.getFilter();

	    reading = filter.readout();
	    
	    x = ((Double)reading).doubleValue();

	    combine += weight*x;

	    if (spy)
		spyLog.log(2, "Mux:"+name+
			   "Adding reading for filter: "+filter.getName()+
			   ", value= "+x+
			   ", weight= "+weight);
	    
	}

	if (spy)
	    spyLog.log(2, "Mux:"+name+", combined= "+combine);
	
	return new Double(x);
	
    }

    /** Set the name of this Filter.
     * @param name The anme to set.*/
    public void setName(String name) { this.name = name; }

    /** Returns the name/id of this Filter.
     * @return The anme/id of this Filter.*/
    public String getName() { return name; }
    
    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy) { this.spy = spy; }
    
    /** Sets the name of and links to the spy-logger.*/
    public void setSpyLog(String spyLogName) { spyLog = LogManager.getLogger(spyLogName); }
    
    /** Represents a weight element.*/
    class Weight {

	protected double weight;

	protected Filter filter;

	/** Create a weighted filter element.*/
	Weight(double weight, Filter filter) {
	    this.weight = weight;
	    this.filter = filter;
	}

	public double getWeight() { return weight; }

	public Filter getFilter() { return filter; }

    }

}

/** $Log: Multiplexer.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/** */
