 package ngat.rcs.newstatemodel;

import ngat.rcs.emm.*;

import java.util.*;

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
		
		EventQueue.postEvent("NSM_"+name+"_"+toCodeString());
	    }
	}

	/** Remove the specified alert from the current list.
	 * Notify the Effector so it can  deal with it or pass it onwards.*/
	public void clearAlert(String alert) {
	    if (alertsInForce.contains(alert)) {
		alertsInForce.remove(alert);
		currentState = (alertsInForce() ? 1 : 0);
		
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

