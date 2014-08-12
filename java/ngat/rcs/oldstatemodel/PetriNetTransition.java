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
package ngat.rcs.oldstatemodel;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.util.*;

/** Represents a PetriNet Transition.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: PetriNetTransition.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/PetriNetTransition.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/
public class PetriNetTransition {

    String name;

    String action;

    String label;

    /** Time delay before firing action.*/
    long delta;

    Map enablers;

    List reachables;

    PetriNetTransition(String name) {
	this.name = name;
	enablers   = new HashMap();
	reachables = new Vector();
	System.err.println("PN-TRAN::Create new Trans: "+name);
    }

    /** Returns the name of this Transition.*/
    public String getName() { return name; }

    /** Sets the action associated with this Transition.*/
    public void setAction(String action) { this.action = action;}

    /** Returns the action associated with this Transition.*/
    public String getAction() {  return action; }

    /** Sets the label for the firing.*/
    public void setLabel(String label) { this.label = label; }

    /** Returns the label for the firing.*/
    public String getLabel() { return label; }

    /** Sets the action delay (millis).*/
    public void setDelta(long delta) { this.delta = delta; }
    
    /** Returns the action delay (millis).*/
    public long getDelta() { return delta; }
    
    public void addEnabler(PetriNetPlace place, int enableValue) {
	enablers.put(place, new Integer(enableValue));
	System.err.println("PN-TRAN::Add enabler: "+place.getName()+" Using marking: "+enableValue);
    }
    
    public void addReachable(PetriNetPlace place) {
	reachables.add(place);
	System.err.println("PN-TRAN::Add firing Link to: "+place.getName()); 
    }
    
    public Iterator listEnablingPlaces() {
	return enablers.keySet().iterator();
    }

    public Iterator listReachablePlaces() {
	return reachables.iterator();
    }
    
    public int getEnablementValue(PetriNetPlace place) throws NoSuchElementException {
	if (enablers.containsKey(place))
	    return ((Integer)enablers.get(place)).intValue();
	throw new NoSuchElementException((place == null ? 
					  "No Place specified" : 
					  "Unknown enabler: "+place.getName()));	
    }
    
    public void fire(PetriNetTransitionHandler handler) {
	if (handler != null)
	    handler.transitionFired(this);
    }

    @Override
	public String toString() {
	StringBuffer buff = new StringBuffer();
	buff.append("Transition: "+name);
	PetriNetPlace place = null;

	Iterator e = listEnablingPlaces();
	while (e.hasNext()) {
	    place = (PetriNetPlace)e.next();
	    buff.append("\n\tEnabled when: "+place.getName()+" is: "+getEnablementValue(place));
	}

	Iterator r = listReachablePlaces();
	while (r.hasNext()) {
	     place = (PetriNetPlace)r.next();
	     buff.append("\n\tFires: "+place.getName());
	}
	return buff.toString();
    }

    public String getReachableString() {
	StringBuffer buff = new StringBuffer("[");

	PetriNetPlace place = null;

	Iterator r = listReachablePlaces();
	while (r.hasNext()) {
	     place = (PetriNetPlace)r.next();
	     buff.append(":"+place.getName());
	}
	buff.append("]");
	return buff.toString();

    }
    
}

/** $Log: PetriNetTransition.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
