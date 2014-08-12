package ngat.rcs.tms;

import java.rmi.*;

/** Interface which allows task controllers to be managed.*/
public interface TaskModeControllerManagement extends Remote {

    /** Promote this controller.*/
    public void promote() throws RemoteException;

    /** Demote this controller.*/
    public void demote() throws RemoteException;

    /** Request the controller to execute the supplied sequence, reporting back to a listener.*/
    public void executeControlSequence(TaskSequence ts, TaskSequenceListener tl) throws RemoteException;

    /** Disable this controller.*/
    public void disable() throws RemoteException;
    
    /** Enable this controller.*/
    public void enable() throws RemoteException;
    
    /** @return true if enabled.*/
    public boolean isEnabled() throws RemoteException;
    
}