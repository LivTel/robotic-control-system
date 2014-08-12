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
import java.lang.reflect.*;

import ngat.util.*;

/** 
 * Representation of a State Model.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: StateModel.java,v 1.1 2006/12/12 08:27:53 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/StateModel.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class StateModel {

    protected static final String EFFECTOR_CLASS_KEY           = "state.model.effector.class";

    protected static final String VARIABLE_DEFINITION_FILE_KEY = "variable.definition.file";

    protected static final String NETWORK_DESCRIPTION_FILE_KEY = "network.description.file";

    /** An Effector to carry out the specific implementation 
     * e.g. to map to a network representation of the conditional 
     * dependancies between states and the transient action firing.
     */
    protected StateModelEffector effector;

    /** Handles notifications of changes of state. This is used by the
     * Effector rather than directly by the StateModel itself.
     */
    protected StateModelEventListener eventListener;

    /** List of Variables indexed by Name.*/
    protected Map variables;

    /** A name for the Model.*/
    protected String name;
    
    /** Create a StateModel.*/
    public StateModel(String name) {
	this.name = name;
	variables = new HashMap();
    }
    
    /** Sets the StateModelEffector for this model.*/
    public void setEffector(StateModelEffector effector) {
	this.effector = effector;
    }

    /** Returns the StateModelEffector for this model.*/
    public StateModelEffector getEffector() { return effector; }

    /** Sets the StateModelEventListener for this model.*/
    public void setEventListener(StateModelEventListener eventListener) {
	this.eventListener = eventListener;
    }

    /** Returns the StateModelEventListener for this model.*/
    public StateModelEventListener getEventListener() { return eventListener; }

    /** Returns a reference to the List of variables.*/
    public Map getVariableList() { return variables; }

    /** Returns an Iterator over the List of variable names.*/
    public Iterator listVariableNames() { return variables.keySet().iterator(); }

    /** Returns an Iterator over the List of variables.*/
    public Iterator listVariables() { return variables.values().iterator(); }

    /** Returns the named Variable or null if not found.*/
    public Variable getVariable(String name) {
	if (variables.containsKey(name))
	    return (Variable)variables.get(name);
	return null;
    }

    /** Returns the name of this Model.*/
    public String getName() { return name; }
    
    /** Returns a readable description of this Model.*/
    @Override
	public String toString() { 
	return "StateModel: "+name+
	    "\n\tEffector: "+
	    (effector != null ? effector.getClass().getName() : "NONE")+
	    "\n\tEventListener: "+eventListener;
    }

    /** Returns a readable description of this Model's current state.*/
    public String stateToString() {
	StringBuffer buff = new StringBuffer();	
	buff.append(effector.stateToString());
	return buff.toString();
    }

    /** Returns a Map of current state nodes - value/names.*/
    public Map getStateInfo() {
	return  effector.getStateInfo();
    }

    /** Configure this StateModel from the content of the supplied file. 
     * This config information contains details of the Effector class
     * and location of files containing the:-
     * <ul>
     *  <li> State Model Variable Data Definition.     (smvdl)
     *  <li> State Model Effector Network Description. (smndl)
     * </ul>
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any configuration problems.
     */
    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));

	String effectorClassName = config.getProperty(EFFECTOR_CLASS_KEY);
	if (effectorClassName == null)
	    throw new IllegalArgumentException("Keyword: ["+EFFECTOR_CLASS_KEY+
					       "] : No Effector class was specified.");

	// Try to create an Effector now.
	try {
	    Class effectorClazz = Class.forName(effectorClassName);
	    Constructor     con = effectorClazz.getConstructor(new Class[] {StateModel.class});
	    effector            = (StateModelEffector)con.newInstance(new Object[] {this});
	} catch (Exception e) {
	    throw new IllegalArgumentException("Unable to create StateModelEffector: "+
					       effectorClassName+" : "+e);
	}
	
	// Locate the Variable definition file. 
	File varDefinitionFile = config.getFile(VARIABLE_DEFINITION_FILE_KEY);
	if (varDefinitionFile == null)
	    throw new IllegalArgumentException("Keyword: ["+VARIABLE_DEFINITION_FILE_KEY+
					       "] : No file was specified.");
	
	// Load the variable definitions.
	configureVariables(varDefinitionFile);

	// Locate the Network description file.
	File networkDescriptionFile = config.getFile(NETWORK_DESCRIPTION_FILE_KEY);
	if (networkDescriptionFile == null)
	    throw new IllegalArgumentException("Keyword: ["+NETWORK_DESCRIPTION_FILE_KEY+
					       "] : No file  was specified.");
	// Configure the Effector.
	effector.configure(networkDescriptionFile);
    }

    /** Configure the State Model Variable Data Definition.     (smvdl)    
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any syntax problems.
     */
    public void configureVariables(File file) throws IOException, IllegalArgumentException {
	BufferedReader in = new BufferedReader(new FileReader(file));

	Vector stateVarList  = new Vector();
	Vector boolVarList   = new Vector();
	Vector recordVarList = new Vector();

	// Scanning phase.
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
	
	// Variable collection phase.
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("VAR_STATE")) {
		if (tokenz.countTokens() == 2) {
		    tokenz.nextToken();
		    String name = tokenz.nextToken();
		    StateVariable var = new StateVariable(name);
		    System.err.println("Creating StateVar: "+name);		  
		    stateVarList.add(var);	
		    variables.put(name, var); 		  
		}
	    }
	    if (line.startsWith("VAR_BOOL")) {
		if (tokenz.countTokens() == 2) {
		    tokenz.nextToken();
		    String name = tokenz.nextToken();
		    BooleanVariable var = new BooleanVariable(name);
		    System.err.println("Creating BoolVar: "+name);		  
		    boolVarList.add(var);	
		    variables.put(name, var); 		
		}
	    }
	    if (line.startsWith("VAR_RECORD")) {
		if (tokenz.countTokens() == 2) {
		    tokenz.nextToken();
		    String name = tokenz.nextToken();
		    RecordVariable var = new RecordVariable(name);
		    System.err.println("Creating RecordVar: "+name+" Set to ALERT");		  
		    recordVarList.add(var);	
		    variables.put(name, var);    

		    /// Set to ALERT to start with.
		    var.currentState = 1;
		}
	    }
	}
	
	// Search for StateVar constants.
	Iterator vars = stateVarList.iterator();
	while(vars.hasNext()) {
	    StateVariable var = (StateVariable)vars.next();
	    tokenz = null;
	    lines  = lineList.iterator();
	    while (lines.hasNext()) {
		line   = (String)lines.next();
		tokenz = new StringTokenizer(line);
		if (line.startsWith(var.name)) {
		    if (tokenz.countTokens() == 4) {
			tokenz.nextToken();
			String act = tokenz.nextToken();
			String stt = tokenz.nextToken();
			String con = tokenz.nextToken();
			try {
			    int ist = Integer.parseInt(stt);
			    var.addState(ist, con);
			    var.addTriggerEvent(act, con);
			    System.err.println("Added State: #"+ist+" Trigger: "+act+" Const: "+con+" To: "+var.name);
			} catch (NumberFormatException nx) {
			    throw new IllegalArgumentException("Var: "+var.name+" Illegal format: "+nx);
			}
		    }
		}
	    }
	}

	// Search for BoolVar activators.
	vars = boolVarList.iterator();
	while(vars.hasNext()) {
	    BooleanVariable var = (BooleanVariable)vars.next();
	    tokenz = null;
	    lines  = lineList.iterator();
	    while (lines.hasNext()) {
		line   = (String)lines.next();
		tokenz = new StringTokenizer(line);
		if (line.startsWith(var.name)) {
		    if (tokenz.countTokens() == 4) {
			tokenz.nextToken();
			String act = tokenz.nextToken();
			String typ = tokenz.nextToken();
			String con = tokenz.nextToken();
			if (typ.equals("SET")) {
			    var.setSetConst(con);
			    var.addSetTrigger(act);
			    System.err.println("Added Set Trigger: "+act+" Const: "+con+" To: "+var.name);
			}
			if (typ.equals("CLEAR")) {
			    var.setClearConst(con);
			    var.addClearTrigger(act);
			    System.err.println("Added Clear Trigger: "+act+" Const: "+con+" To: "+var.name);
			}
		    }
		}
	    }
	}

	// Search for RecordVar alerts and clears.
	vars = recordVarList.iterator();
	while(vars.hasNext()) {
	    RecordVariable var = (RecordVariable)vars.next();
	    tokenz = null;
	    lines  = lineList.iterator();
	    while (lines.hasNext()) {
		line   = (String)lines.next();
		tokenz = new StringTokenizer(line);
		if (line.startsWith(var.name)) {
		    if (tokenz.countTokens() == 3) {
			tokenz.nextToken();tokenz.nextToken();
			// ALERT ?
			String alt = tokenz.nextToken();
			var.addAlertTrigger(alt);
			System.err.println("Added ALERT: "+alt+" For RecordVar: "+var.name);
		    }
		    if (tokenz.countTokens() == 5) {
			tokenz.nextToken();tokenz.nextToken();
			// CLEAR ?
			String clr = tokenz.nextToken();
			tokenz.nextToken();
			String can = tokenz.nextToken();
			var.addClearTrigger(clr, can);
			System.err.println("Added CLEAR: "+clr+" cancels alert: "+can+" For RecordVar: "+var.name);
		    }
		}
	    }
	}
	
	System.err.println("State Variable List.....");
	vars = stateVarList.iterator();
	while(vars.hasNext()) {
	    StateVariable var = (StateVariable)vars.next();  
	    System.err.println(var.toString());
	}
	System.err.println("Boolean Variable List.....");
	vars = boolVarList.iterator();
	while(vars.hasNext()) {
	    BooleanVariable var = (BooleanVariable)vars.next();  
	    System.err.println(var.toString());
	}
	System.err.println("Record Variable List.....");
	vars = recordVarList.iterator();
	while(vars.hasNext()) {
	    RecordVariable var = (RecordVariable)vars.next();
	    System.err.println(var.toString());
	}  
		
	try {
	    in.close();
	} catch (Exception e) {
	    System.err.println("Error closing file: "+e);
	}
	
	in = null;

	// Dispose of resources.
	stateVarList.clear();
	stateVarList = null;
	boolVarList.clear();
	boolVarList = null;
	recordVarList.clear();
	recordVarList = null;

    }

    /** Instructs the StateModel to start its Effector. The Effector may want to start
     * a new Thread to carry out its operations.*/
    public void start(long updateInterval) {
	effector.start(updateInterval);
    }


    /** Gerneric Variable class.*/
    public abstract class Variable implements EventSubscriber {
	
	/** Variable identifier.*/
	public String name;
	
	/** The current state of this StateVariable.*/
	int currentState;
	
	Variable(String name) {
	    this.name = name;
	    currentState = 0;
	}
	
	/** Returns the currentState.*/
	public int getCurrentState() { return currentState; }

	/** Returns the code-string for the current state.*/
	public abstract String toCodeString();

    }

    /** Variable representing a state - can take discrete values from a list.*/
     public class StateVariable extends Variable {
	
	 /** Maps constant-names to int codes.*/
	 Map constants;
	 
	 /** Reverse maps the int codes to constant-names.*/
	 Map names;
	 
	 /** Maps trigger messages to state int codes.*/
	 Map triggers;
	
	StateVariable(String name) {
	    super(name);
	    constants  = new HashMap();
	    names      = new HashMap();
	    triggers   = new HashMap();
	}

	/** Add a named constant for an int state.*/
	public void addState(int state, String constantName) {
	    Integer istate = new Integer(state);	  
	    constants.put(constantName, istate);
	    names.put(istate, constantName);
	}

	 public int getStateCode(String constantName) {
	     return ((Integer)constants.get(constantName)).intValue();
	 }
	 
	 /** Returns the code-string for the current state.*/
	 @Override
	public String toCodeString() {
	     Integer icode = new Integer(currentState);
	     if (names.containsKey(icode))
		 return (String)names.get(icode);
	     return "UNKNOWN";
	 }
	 
	 /** Add a message code to force transition to the named const state.*/
	 public void addTriggerEvent(String trigMesg, String constantName) {
	    if (constants.containsKey(constantName)) {
		triggers.put(trigMesg, constants.get(constantName));
		// Add an external reference to the EventRegistry.
		EventRegistry.subscribe(trigMesg, this);
	    }
	}

	@Override
	public String toString() {
	    StringBuffer buff = new StringBuffer();
	    buff.append("StateVariable: "+this.name);
	    Iterator it = constants.keySet().iterator();
	    while (it.hasNext()) {
		buff.append("\n\tState: "+(String)it.next());
	    }

	    it = triggers.keySet().iterator();
	    while (it.hasNext()) {
		buff.append("\n\tTrigger: "+(String)it.next());
	    }
	    return buff.toString();
	}

	/** Lists the valid state names.*/	
	public Iterator listStates() { return constants.keySet().iterator(); }

	/** Implements the event notification handling.*/
	public void notifyEvent(String topic, Object data) {
	    // Check its a trigger we know about.
	    if (triggers.containsKey(topic)) {
		currentState = ((Integer)triggers.get(topic)).intValue();
		effector.stateChanged(this);
		//		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	    }
	
	}

	/** Returns the EventSubscriber ID.*/
	public String getSubscriberId() { return "SM_SV_"+this.name; }

    }
    
    /** Represents a class of Variables whose state is determined by
     * the content of a List (empty = CLEAR, non-empty = ALERT) These may
     * be added to later.
     */
     public class RecordVariable extends Variable {
	
	List activators;
	Map  cancelators;
	List alertsInForce;

	RecordVariable(String name) {
	    super(name);
	    activators  = new Vector();
	    cancelators = new HashMap();
	    alertsInForce = new Vector();
	}

	public void addAlertTrigger(String alert) {
	    activators.add(alert);	  
	    // ER Subs.
	    EventRegistry.subscribe(alert, this);
	}

	public void addClearTrigger(String clear, String alert) {
	    cancelators.put(clear, alert);
	    // ER Subs.
	    EventRegistry.subscribe(clear, this);
	}

	/** Add the specified alert to the current list.
	 * Notify the Effector so it can  deal with it or pass it onwards.*/
	public void addAlert(String alert) {
	    if (!alertsInForce.contains(alert)) {
		alertsInForce.add(alert);
		currentState = (alertsInForce() ? 1 : 0);
		effector.stateChanged(this);
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	    }
	}

	/** Remove the specified alert from the current list.
	 * Notify the Effector so it can  deal with it or pass it onwards.*/
	public void clearAlert(String alert) {
	    if (alertsInForce.contains(alert)) {
		alertsInForce.remove(alert);
		currentState = (alertsInForce() ? 1 : 0);
		effector.stateChanged(this);
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	    }
	}

	/** Alert is in force if there are any activators.*/
	public boolean alertsInForce() { return ! alertsInForce.isEmpty(); }
	
	@Override
	public String toString() {
	    StringBuffer buff = new StringBuffer();
	    buff.append("RecordVariable: "+this.name);
	    Iterator it = activators.iterator();
	    while (it.hasNext()) {
		buff.append("\n\tAlert: "+(String)it.next());
	    }

	    it = cancelators.keySet().iterator();
	    while (it.hasNext()) {
		String can = (String)it.next();
		buff.append("\n\tClear: "+can+" Cancels: "+cancelators.get(can));
	    }

	    it = alertsInForce.iterator();
	    while (it.hasNext()) {
		buff.append("\n\tWaiting: "+(String)it.next());
	    }

	    return buff.toString();
	}

	 /** Returns the code-string for the current state.*/
	 @Override
	public String toCodeString() {
	     return (alertsInForce() ? "ALERT" : "CLEAR");
	 }

	/** Implements the event notification handling.*/
	public void notifyEvent(String topic, Object data) {
	    // Check its a trigger we know about.
	    if (activators.contains(topic)) 
		addAlert(topic);
	    
	    else if 
		(cancelators.containsKey(topic))
		clearAlert((String)cancelators.get(topic));
	    
	}

	/** Returns the EventSubscriber ID.*/
	public String getSubscriberId() { return "SM_RV_"+this.name; }

    }

    /** Variable representing a boolean condition. It has states SET and CLEAR.*/
     public class BooleanVariable extends Variable {

	Map triggers;

	/** Const name for Set state.*/
	String setConst;

	/** Const name for clear state.*/
	String clearConst;

	BooleanVariable(String name) {
	    super(name);
	    triggers = new HashMap();
	}
	
	/** Sets the printable-name of the SET state.*/
	public void setSetConst(String s) { setConst = s;}

	/** Returns the printable-name of the SET state.*/
	public String getSetConst() { return setConst; }

	/** Sets the printable-name of the CLEAR state.*/
	public void setClearConst(String c) { clearConst = c;}

	/** Returns the printable-name of the CLEAR state.*/
	public String getClearConst() { return clearConst; }

	/** Adds a named SET trigger event message.*/
	public void addSetTrigger(String mesg) {
	    triggers.put(mesg, new Integer(1));
	    // ER Subs.
	    EventRegistry.subscribe(mesg, this);
	}
	
	/** Adds a named CLEAR trigger event message.*/
	public void addClearTrigger(String mesg) {
	    triggers.put(mesg, new Integer(0)); 
	    // ER Subs.
	    EventRegistry.subscribe(mesg, this);
	}

	/** Returns a printable description of the Variable.*/
	@Override
	public String toString() {
	    StringBuffer buff = new StringBuffer();
	    buff.append("BooleanVariable: "+this.name);
	   
	    buff.append("\n\tSet   State:   "+setConst);
	    buff.append("\n\tClear State: "+clearConst);
	    
	    Iterator it = triggers.keySet().iterator();
	    while (it.hasNext()) {
		buff.append("\n\tTrigger: "+(String)it.next());
	    }
	    return buff.toString();
	}

	 /** Returns the code-string for the current state.*/
	 @Override
	public String toCodeString() {
	     return (currentState == 0 ? "CLEAR" : "SET");
	 }

	/** Implements the event notification handling.*/
	public void notifyEvent(String topic, Object data) {
	    // Check its a trigger we know about.
	    if (triggers.containsKey(topic)) {
		currentState = ((Integer)triggers.get(topic)).intValue();
		effector.stateChanged(this);
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	    }

	}

	/** Returns the EventSubscriber ID.*/
	public String getSubscriberId() { return "SM_BV_"+this.name; }

    }

}

/** $Log: StateModel.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
