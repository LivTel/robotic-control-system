package ngat.rcs.newstatemodel;

import ngat.rcs.emm.*;

import java.util.*;

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
	    EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	}
	
    }
    
    /** Returns the EventSubscriber ID.*/
    public String getSubscriberId() { return "SM_SV_"+this.name; }
    
}
