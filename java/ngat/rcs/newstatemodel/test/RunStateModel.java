package ngat.rcs.newstatemodel.test;

import java.rmi.*;
import ngat.rcs.newstatemodel.*;

public class RunStateModel {

    public static void main(String args[]) {
	try {
	    StandardStateModel tsm = new StandardStateModel(30000L);   
	    Naming.rebind("rmi://localhost/StateModel", tsm);
	    System.err.println("TSM bound");
	    
	    (new Thread(tsm)).start();
	    System.err.println("TSM running...");
	    
	    while (true) {try {Thread.sleep(60000L);} catch (InterruptedException e) {}}
	
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}