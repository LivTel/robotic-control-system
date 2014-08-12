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
import ngat.util.*;

import java.util.*;

/** PetriNet representation.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: PetriNet.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/PetriNet.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public class PetriNet {

    /** Name for this PetriNet.*/
    String name;

    /** List of Places indexed by name.*/
    Map places;

    /** List of Transitions indexed by name.*/
    Map transitions;

    /** List of currently enabled Transitions.*/
    List enabledTransitions;

    /** Reference to the TransitionHandler for this Net.*/
    PetriNetTransitionHandler transitionHandler;

    /** Create a PetriNet.*/
    public PetriNet(String name) {
	this.name = name;
	places      = new HashMap();
	transitions = new HashMap();
	enabledTransitions = new Vector();

    }

    /** Add a StandardPlace which may be clamped.*/
    public PetriNetStandardPlace addStandardPlace(String name, boolean clamped) {
	if (places.containsKey(name)) return null;
	PetriNetStandardPlace place = new PetriNetStandardPlace(name);
	place.setClamped(clamped);
	places.put(name, place);
	return place;
    }

    /** Add a SwitchedPlace which may be clamped.*/
    public PetriNetSwitchedPlace addSwitchedPlace(String name, boolean clamped) {
	if (places.containsKey(name)) return null;
	PetriNetSwitchedPlace place = new PetriNetSwitchedPlace(name);	
	place.setClamped(clamped);
	places.put(name, place);
	return place;
    }

    /** Add a Transition.*/
    public PetriNetTransition addTransition(String name) {
	if (transitions.containsKey(name)) return null;
	PetriNetTransition trans = new PetriNetTransition(name);
	transitions.put(name, trans);
	return trans;
    }

    /** Return the named Place or null if not found.*/
    public PetriNetPlace findPlace(String name) {
	if (!places.containsKey(name))
	    return null;
	return (PetriNetPlace)places.get(name);
    }

    /** Return the named Transition or null if not found.*/
    public PetriNetTransition findTransition(String name) {
	if (!transitions.containsKey(name))
	    return null;
	return (PetriNetTransition)transitions.get(name);
    }

    /** Update the named Place with the supplied value. If the Place does
     * not exist then no action. If the value is not valid for the specified
     * Place then no action is taken.
     */
    public void updatePlace(String name, int value) {
	PetriNetPlace place = (PetriNetPlace)places.get(name);
	if (place == null) return;
	place.mark(value);
    }

    /** Specify the TransitionHandler.*/
    public void setTransitionHandler(PetriNetTransitionHandler handler) {
	this.transitionHandler = handler;
    }

    /** Step the net forward one move. The supplied selection model determines 
     * which of the enabled Transitions (if any) will be fired.
     */
    public void step(PetriNetTransitionFiringSelectionModel selectionModel) {
   
	enabledTransitions.clear();
	
	String     tn     = null;
	PetriNetTransition trans  = null;
	Iterator   iTrans = transitions.keySet().iterator();
	// Look at EACH Transition.
	while (iTrans.hasNext()) {
	    
	    tn = (String)iTrans.next();
	    if (tn == null) continue;
	    
	    trans = (PetriNetTransition)transitions.get(tn);
	    if (trans == null) continue;
	    
	    PetriNetPlace place   = null;
	    boolean  okSoFar = true;
	    Iterator iEnable = trans.listEnablingPlaces();
	    // Check that ALL enablers are valid.
	    while (iEnable.hasNext()) {
		
		place = (PetriNetPlace)iEnable.next();
		if (place == null) continue;
		    int eval = trans.getEnablementValue(place);
		    //System.err.println("Checking enabler: "+place.getName()+" for Trans: "+trans.getName());
		    //System.err.println("   Mark Required: "+eval+" Actual: "+place.getMarking());
		    if (place.getMarking() != eval) 
			okSoFar = false;		    
	    }
	    //System.err.println("After checking enablers: "+okSoFar);
	    if (okSoFar) {
		enabledTransitions.add(trans);
		//System.err.println("Enabled: "+trans.getName());
	    }
	}
	
	// Select a Transition to fire.
	PetriNetTransition nominatedTransition = 
	    selectionModel.selectTransitionToFire(enabledTransitions);

	
	if (nominatedTransition != null) { 
	    
	    PetriNetPlace place = null;
	    
	    // Remove tokens from enablers.
	    Iterator iEnable =  nominatedTransition.listEnablingPlaces();		
	    while (iEnable.hasNext()) {		   
		place = (PetriNetPlace)iEnable.next();
		if (place == null) continue;
		// We dont unmark clamped places..
		if (place.isClamped()) continue;		
		place.unMark();
	    }
	    
	    // Ok fire it.
	    //System.err.println("About to fire: "+nominatedTransition.getName());
	    nominatedTransition.fire(transitionHandler);
	    
	    // Mark all the outgoing Places. ALWAYS marked with '1'.
	    Iterator iReach = nominatedTransition.listReachablePlaces();
	    while (iReach.hasNext()) {
		place = (PetriNetPlace)iReach.next();
		if (place == null) continue;
		place.mark(1);		
	    }
	}
	
    }
    
    public void print() {
	System.err.println("PetriNet: "+name);

	PetriNetPlace place = null;
	Iterator p = places.values().iterator();
	while (p.hasNext()) {
	    place = (PetriNetPlace)p.next();
	    System.err.println(place.toString());
	}

	PetriNetTransition transition = null;
	Iterator t = transitions.values().iterator();
	while (t.hasNext()) {
	    transition = (PetriNetTransition)t.next();
	    System.err.println(transition.toString());
	}
	
    }


    public String markingToString() {
	StringBuffer buff = new StringBuffer();
	//buff.append("PetriNet: "+name);

	PetriNetPlace place = null;
	int ct = 0;
	Iterator p = places.values().iterator();
	while (p.hasNext()) {
	    place = (PetriNetPlace)p.next();
	    
	    buff.append(
			StringUtilities.pad(place.getName(), 20)+
			StringUtilities.pad("\033[43m"+place.getStateName(place.getMarking())+"\033[0m", 30));
	    
	    ct++;	
	    if (ct % 3 == 0) buff.append("\n");
	}
	return buff.toString();
    }

    /** Returns a map from place to current statename.*/
    public Map getMarkingInfo() {
	Map map = new HashMap();	
	PetriNetPlace place = null;
	Iterator p = places.values().iterator();
	while (p.hasNext()) {
	    place = (PetriNetPlace)p.next();
	    map.put(place.getName(), place.getStateName(place.getMarking()));
	}
	return map;	
    }

}

/** $Log: PetriNet.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
