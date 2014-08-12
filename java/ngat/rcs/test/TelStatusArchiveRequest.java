/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.util.List;

import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusArchive;

/**
 * @author eng
 *
 */
public class TelStatusArchiveRequest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			// time minutes
			long back = 60000*Long.parseLong(args[0]);
						
			long t2 = System.currentTimeMillis();
			long t1 = t2 - back;
			
			TelescopeStatusArchive tar = (TelescopeStatusArchive)Naming.lookup("rmi://ltsim1/TelescopeGateway");
			System.err.printf("Requesting archive data for %4d mins, from: %tT to %tT",(back/60000),t1,t2);
			
			long st0 = System.currentTimeMillis();
			List<TelescopeStatus> list = tar.getTelescopeStatusHistory(t1, t2);
			long st1 = System.currentTimeMillis();
			
			System.err.println("Request returned "+list.size()+" entries in "+(st1-st0)+"ms");
			
			for (int is = 0; is < list.size(); is++) {
				TelescopeStatus status = list.get(is);
				System.err.printf("%6d : %tT : %s\n", is, status.getStatusTimeStamp(), status);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
