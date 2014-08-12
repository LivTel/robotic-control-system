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

import java.io.*;
import java.util.*;

import ngat.util.logging.*;

/** PetriNet implementation of a StateModelEffector.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: PetriNetStateModelEffector.java,v 1.1 2006/12/12 08:27:53 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/PetriNetStateModelEffector.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class PetriNetStateModelEffector 
    implements 
	StateModelEffector, 
	PetriNetTransitionHandler,
	PetriNetTransitionFiringSelectionModel {

    /** ClassID for logging.*/
    public static final String CLASS = "PNStateModelEffector";
  
    /** Maps StateModel.Variables to PetriNetPlaces.*/
    Map varPlace;

    /** The PetriNet representing the state Network.*/
    PetriNet petriNet;

    /** The enclosing StateModel.*/
    StateModel model;
   
    /** ControlAgent Logger.*/
    Logger ctrlLog;

    /** Create a PetriNetStateModelEffector for the specified StateModel.*/
    public PetriNetStateModelEffector(StateModel model) {
	this.model = model;
	varPlace = new HashMap();
	petriNet = new PetriNet("StateModelNetwork");
	petriNet.setTransitionHandler(this);
	ctrlLog = LogManager.getLogger("CTRL_AGENT");	
    }
    
    /** This is the StateModelEffector implementation method here.*/
    public void stateChanged(StateModel.Variable var) {	
	int state = var.getCurrentState();
	
	//System.err.println("Got state change notification from: "+var.name+" : "+state);
	ctrlLog.log(1, CLASS, "PNE", "stateChanged",
		    "StateChange on Var: "+var.name+" to State: "+var.toCodeString()+" ("+state+")");
	//Locate the Place associated to this Var and try to update.
	// ### NEEDS ROBUSTIFICATION HERE ######
	PetriNetPlace place =  ((PetriNetPlace)varPlace.get(var));
	if (place != null) {
	    String placeId = place.getName();
	    petriNet.updatePlace(placeId, state);
	    
	    // Here is the spot to broadcast an SM change.
	    // ### Telemetry.publish("STATE_MODEL", new StateModelChangeInfo(placeId, state)); ###
 
	}
    }
    
    /** Configure this StateModelEffector from the content of the supplied file.    
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any configuration problems.
     */
    public void configure(File file) throws IOException, IllegalArgumentException {
	BufferedReader in = new BufferedReader(new FileReader(file));
	
	// Scan phase.
	String line = null;
	Vector lineList = new Vector();
	while ((line = in.readLine()) != null) {
	    //System.err.println("Read:"+line);
	    line = line.trim();
	    // Skip blank or comment lines.
	    if ((line != "") && 
		!(line.startsWith("#")))
		lineList.add(line);
	}
	
	// Looking for places.
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    boolean clamped = false;
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("PLACE")) {
		if (tokenz.countTokens() == 5) {
		    tokenz.nextToken(); // Place
		    String name = tokenz.nextToken();
		    tokenz.nextToken(); // Maps
		    String vnm = tokenz.nextToken();
		    vnm = vnm.substring(4);
		    String typ = tokenz.nextToken(); // CLAMP or EVENT
		    if (typ.equals("CLAMPED")) clamped = true;
		    System.err.println("Creating a Linked Place called: "+name+" mapped to: "+vnm);
		    		   
		    StateModel.Variable var = model.getVariable(vnm); 
		    if (var != null) {
			// How many states has it got..
			if (var instanceof StateModel.BooleanVariable) {
			    StateModel.BooleanVariable bv = (StateModel.BooleanVariable)var;
			    if (clamped) {
				System.err.println("Setting "+typ+" values to: "+
						   bv.clearConst+" and "+
						   bv.setConst);	
				PetriNetStandardPlace place = 
				    petriNet.addStandardPlace(name, true);
				place.setName   = bv.setConst;
				place.clearName = bv.clearConst;
				varPlace.put(bv, place);
			    } else {
				System.err.println("Setting "+typ+" values to: "+
						   bv.clearConst+" and "+
						   bv.setConst);	
				PetriNetSwitchedPlace place = petriNet.addSwitchedPlace(name, false);
				place.addValue(bv.getSetConst(), 1);
				place.addValue(bv.getClearConst(), 0); 
				varPlace.put(bv, place);
			    }
			} else if
			    (var instanceof StateModel.RecordVariable) {
			    StateModel.RecordVariable rv =(StateModel.RecordVariable)var;
			    System.err.println("Setting "+typ+" values to: ALERT and CLEAR");
			    PetriNetStandardPlace place = 
				petriNet.addStandardPlace(name, clamped);
			    place.setName   = "ALERT";
			    place.clearName = "CLEAR"; 

			    varPlace.put(rv, place);
			} else if (var instanceof StateModel.StateVariable) {
			    StateModel.StateVariable sv = (StateModel.StateVariable)var;
			    PetriNetSwitchedPlace place = petriNet.addSwitchedPlace(name, clamped); 
			    varPlace.put(sv, place);
			    Iterator states = sv.listStates();
			    while (states.hasNext()) {
				String state = (String)states.next();
				place.addValue(state, sv.getStateCode(state));	  	
			    }
			}
		    }
		} else if
		    (tokenz.countTokens() == 2) {
		    tokenz.nextToken(); // Place
		    String name = tokenz.nextToken();
		    PetriNetStandardPlace place = petriNet.addStandardPlace(name, false);		
		    place.setName   = "SET";
		    place.clearName = "CLEAR"; 
		    
		}
	    }
	}
	
	// Looking for transitions.
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("TRANS")) {

		// TRANS T1 <action>[:<label>][+<delta>] MARKS <p1> <p2> .... <pN>

		// TRANS T1 EVENT:INIT_DONE+30 MARKS P1 P2

		int tc = tokenz.countTokens();
		if (tokenz.countTokens() >= 5) {
		    tokenz.nextToken(); // Trans.

		    String name = tokenz.nextToken();			
		    PetriNetTransition trans = petriNet.addTransition(name);

		    String info  = tokenz.nextToken();

		    String action = null;
		    String label  = null;
		    String stime  = null;
		    long   delta = 0L;

		    // Parse this into ( (action + label) + delta(secs) ) if possible.
		    
		    if (info.indexOf("+") != -1) {
			action = info.substring(0,info.indexOf("+")).trim();
			stime  = info.substring(1+info.indexOf("+")).trim();
		    } else {
			action = info;
		    }

		    info = action;
		    // Parse this into (action : label) if possible.

		    if (info.indexOf(":") != -1) {
			action = info.substring(0,info.indexOf(":")).trim();
			label  = info.substring(1+info.indexOf(":")).trim();
		    } else {
			action = info;
			label  = "";
		    }

		    try {
			delta = Long.parseLong(stime);
			delta = delta*1000L;
		    } catch (NumberFormatException nx) {
			delta = 0L;
		    }

		    trans.setAction(action);
		    trans.setLabel(label);
		    trans.setDelta(delta);

		    tokenz.nextToken(); // Marks.
		    for (int i = 5; i <= tc; i++) {
			String plc = tokenz.nextToken();	
			 PetriNetPlace place = petriNet.findPlace(plc);
			if (place != null)
			    trans.addReachable(place);
		    }
		}
	    }
	}
	
	// Looking for enablement conditions..
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("ENABLE")) {
		int tc = tokenz.countTokens();
		if (tokenz.countTokens() >= 6) {
		    tokenz.nextToken(); // ENABLE
		    String tname = tokenz.nextToken();
		    tokenz.nextToken(); // WHEN
		    String pname = tokenz.nextToken();
		    String op    = tokenz.nextToken(); // IS or NOT
		    String sval  =  tokenz.nextToken();
		    PetriNetTransition trans = petriNet.findTransition(tname);
		    if (trans == null)
			throw new IllegalArgumentException("Transition: "+tname+" has not been defined.");
		    PetriNetPlace place = petriNet.findPlace(pname);
		    if (place == null)
			throw new IllegalArgumentException("Place: "+pname+" has not been defined.");
		    System.err.println("Try to enable "+trans.getName()+
				       " from: "+place.getName()+
				       " using value: "+sval);
		    if (place instanceof PetriNetStandardPlace) {
			if ( ((PetriNetStandardPlace)place).setName.equals(sval))
			    trans.addEnabler(place, 1);
			else
			    trans.addEnabler(place, 0);
		    } else if
			(place instanceof PetriNetSwitchedPlace) {
			try {
			    int val = ((PetriNetSwitchedPlace)place).getStateCode(sval);
			    trans.addEnabler(place, val);
			} catch (Exception e) {
			    throw new IllegalArgumentException ("Place: "+pname+" State: "+sval+
								" is not defined.");			    
			}			
		    }
		}
	    }
	}

	// Look for initial markings. #### WE ONLY DO BOOLEAN PLACES TO THE 'SET' STATE NOW #####
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("MARK")) {
		int tc = tokenz.countTokens();
		if (tokenz.countTokens() == 2) {
		      tokenz.nextToken(); // MARK
		      String plc = tokenz.nextToken();
		      petriNet.updatePlace(plc, 1);
		}
	    }
	}


    }

    /** Print details to System.err .*/
    public void print() {
	petriNet.print();
    }
    
    /** Returns a readable String containing the current state of the Net.*/
    public String stateToString() {	
	return petriNet.markingToString();	
    }

    /** Returns a state model disposition - mapping from model items to settings.*/
    public Map getStateInfo() {
	return petriNet.getMarkingInfo();
    }
    
    /** Start the Effector, Starts a Thread to step the PetriNet.
     */
    public void start(long updateInterval) {
	final long modelUpdateInterval = updateInterval;
	Thread thread = new Thread(
			    (new Runnable() {
				public void run() {
				  while (true) {
				      try {Thread.sleep(modelUpdateInterval); } catch (InterruptedException e) {} 
				      //System.err.println("Step advance");
				      petriNet.step(PetriNetStateModelEffector.this);
				  }
				}
			    }), "PN_STEPPER");	
	thread.start();	
    }
    
    /** Handles PetriNet Transition firings. 
     * These are just passed onto the StateModel's StateModelEventListener.
     */
    public void transitionFired(PetriNetTransition trans) {
	//System.err.println("Transition fired: "+trans.getName());
	ctrlLog.log(1, CLASS, "PNE", "transitionFired",
	    "Firing Transition: "+trans.getName()+ 
		    " Action: "+trans.getAction()+
		    " Label: "+trans.getLabel()+" -> "+trans.getReachableString());
	
	if (trans.getDelta() <= 0L) {
	    if (model != null &&
		model.getEventListener() != null)
		model.getEventListener().stateModelEventOccurred
		    (new StateModelEvent(trans.getAction(), trans.getLabel()));
	} else {
	    if (model != null &&
		model.getEventListener() != null)
		model.getEventListener().stateModelEventOccurred
		    (new StateModelEvent(trans.getAction(), trans.getLabel(), trans.getDelta()));
	}
    }

    
    /** Handles the PetriNet selection model. 
     * ### JUST A CRUDE TEMP FIX HERE ##.
     */
    public PetriNetTransition selectTransitionToFire(List enabled) {
	if (enabled == null ||
	    enabled.size() == 0)
	    return null;
		
	//System.err.println("There are "+enabledTransitions.size()+" enabled Transitions");
	// We chose the randomly - very crude..
	int chose = (int)Math.max(0.0, Math.round(Math.random()*enabled.size())-1);
	//System.err.println("Choose "+chose);
	
	PetriNetTransition trans= 
	    (PetriNetTransition)enabled.get(chose);
	
	return trans;
    }

}

/** $Log: PetriNetStateModelEffector.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
