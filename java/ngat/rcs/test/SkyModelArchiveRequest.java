/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.util.List;

import ngat.ems.SkyModelArchive;
import ngat.ems.SkyModelUpdate;

/**
 * @author eng
 *
 */
public class SkyModelArchiveRequest {

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
			
			SkyModelArchive sar = (SkyModelArchive)Naming.lookup("rmi://ltsim1/SkyModelGateway");
			System.err.printf("Requesting archive data for %4d mins, from: %tT to %tT",(back/60000),t1,t2);
			
			long st0 = System.currentTimeMillis();
			List<SkyModelUpdate> list = sar.getSkyModelHistory(t1, t2);
			long st1 = System.currentTimeMillis();
			
			System.err.println("Request returned "+list.size()+" entries in "+(st1-st0)+"ms");
			
			for (int is = 0; is < list.size(); is++) {
				SkyModelUpdate status = list.get(is);
				System.err.printf("%6d : %tT : %s\n", is, status.getStatusTimeStamp(), status);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
