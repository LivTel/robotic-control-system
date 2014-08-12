package ngat.rcs.tms;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

public class BasicModeManagement extends UnicastRemoteObject implements TaskModeManagement {

    Map controllers;

    public BasicModeManagement() throws RemoteException {
	super();
	controllers = new HashMap();
    }
    
    /** Add a new mode controller.*/
    public void addModeController(TaskModeController tmc) throws Exception {
	controllers.put(tmc.getModeName(), tmc);
    }

    /** Return an iterator over the set of Mode controllers.*/
    public List listModeControllers() throws RemoteException {
	List list = new Vector();
	Iterator ic = controllers.values().iterator();
	while (ic.hasNext()) {
	    list.add(ic.next());
	}
	return list;
    }
    
    /** Returns a Mode controller identified by name or NULL.*/
    public TaskModeController getModeController(String name) throws RemoteException {
	return (TaskModeController)controllers.get(name);
    }

}