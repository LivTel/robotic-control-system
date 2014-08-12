package ngat.rcs.tms;

import java.rmi.*;
import java.util.*;

public interface TaskModeManagement extends Remote {

    /** Return an ierator over the set of Mode controllers.*/
    public List listModeControllers() throws RemoteException;

    /** Returns a Mode controller identified by name.*/
    public TaskModeController getModeController(String name) throws RemoteException;

}
