/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.util.List;
import ngat.ems.*;


/**
 * @author eng
 *
 */
public class MeteoArchiveRequest {

    /**
     * @param args
     */
    public static void main(String[] args) {
	
	try {
			
	    // time minutes
	    //long back = 60000*Long.parseLong(args[0]);
	
	    long back = 60000*10;
	
	    long t2 = System.currentTimeMillis();
	    long t1 = t2 - back;
			
	    MeteorologyStatusArchive sar = (MeteorologyStatusArchive)Naming.lookup("rmi://ltsim1/MeteorologyGateway");
	    System.err.printf("Requesting archive data for %4d mins, from: %tT to %tT",(back/60000),t1,t2);
			
	    long st0 = System.currentTimeMillis();
	    List<MeteorologyStatus> list = sar.getMeteorologyStatusHistory(t1, t2);
	    long st1 = System.currentTimeMillis();
			
	    System.err.println("Request returned "+list.size()+" entries in "+(st1-st0)+"ms");
			
	    for (int is = 0; is < list.size(); is++) {
		MeteorologyStatus status = list.get(is);
		System.err.printf("%6d : %tT : %s\n", is, status.getStatusTimeStamp(), status);
	    }
			
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
