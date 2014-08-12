/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusProvider;
import ngat.tcm.TelescopeStatusUpdateListener;

/**
 * @author eng
 *
 */
public class TcmGatewayReader extends UnicastRemoteObject implements TelescopeStatusUpdateListener {

	public TcmGatewayReader() throws RemoteException {
		super();		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			TcmGatewayReader gwr = new TcmGatewayReader();
		
			TelescopeStatusProvider tsp = (TelescopeStatusProvider)Naming.lookup("rmi://ltsim1/TelescopeGateway");
			
			tsp.addTelescopeStatusUpdateListener(gwr);
			System.err.println("GWR: Bound to Gateway");
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void telescopeStatusUpdate(TelescopeStatus status) throws RemoteException {
		System.err.printf("GWR: Update received: %tT %s\n",status.getStatusTimeStamp(), status);
	}

	public void telescopeNetworkFailure(long time, String arg0) throws RemoteException {
		//System.err.printf("GWR: Update received failure message: %tT %s\n",status.getStatusTimeStamp(), status);
	}

}
