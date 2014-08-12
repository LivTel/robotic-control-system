package ngat.rcs.newstatemodel;

import ngat.rcs.emm.*;

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
