package ngat.rcs.newstatemodel;

import ngat.rcs.emm.*;

import java.util.*;

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
		
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	}
	
    }
    
    /** Returns the EventSubscriber ID.*/
    public String getSubscriberId() { return "SM_BV_"+this.name; }
    
}

