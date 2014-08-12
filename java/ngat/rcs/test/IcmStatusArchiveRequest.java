/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.util.List;

import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusArchive;

/**
 * @author eng
 *
 */
public class IcmStatusArchiveRequest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			
			// time minutes
			long back = 60000L;
			
			long t2 = System.currentTimeMillis();
			long t1 = t2 - back;
			
			InstrumentStatusArchive iar = (InstrumentStatusArchive)Naming.lookup("rmi://ltsim1/InstrumentGateway");
			System.err.printf("Requesting archive data for %4d mins, from: %tT to %tT",(back/60000),t1,t2);
			
			long st0 = System.currentTimeMillis();
			List<InstrumentStatus> list = iar.getInstrumentStatusHistory(t1, t2);
			long st1 = System.currentTimeMillis();
			
			System.err.println("Request returned "+list.size()+" entries in "+(st1-st0)+"ms");
			
			for (int is = 0; is < list.size(); is++) {
				InstrumentStatus status = list.get(is);
				System.err.printf("%6d : %tT : %8s %8s \n", is, status.getStatusTimeStamp(), 
						(status.isOnline() ? "ONLINE" : "OFFLINE" ), 
						(status.isFunctional() ? "OKAY" : "FAIL"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
