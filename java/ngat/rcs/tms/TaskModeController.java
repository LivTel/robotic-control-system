package ngat.rcs.tms;

import java.rmi.*;

public interface TaskModeController extends Remote {

    /** Return true if wants control at time.*/
    public boolean wantsControl(long time) throws RemoteException;

    /** How long till this controller will definitely want control from time.*/
    public long nextWantsControl(long time) throws RemoteException;

    /** Returns the mode's priority.*/
    public int getPriority() throws RemoteException;

    /** Returns the name of the mode.*/
    public String getModeName() throws RemoteException;

    public String getModeDescription() throws RemoteException;

}