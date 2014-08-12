/**
 * 
 */
package ngat.rcs.scm.collation;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import ngat.message.RCS_TCS.TCS_Status;
import ngat.net.cil.CilResponseHandler;
import ngat.net.cil.CilService;
import ngat.net.cil.tcs.CilStatusExtractor;
import ngat.util.ConfigurationProperties;
import ngat.util.StatusCategory;

/**
 * @author eng
 *
 */
public class TcsCilMonitor implements StatusMonitorClient, CilResponseHandler {

	CilService cil;

    /** Name for this client.*/
    protected String name;
    /** True if the current status is valid.*/
    protected volatile boolean valid; 

    /** True if the network resource is available.*/
    protected volatile boolean networkAvailable;

    /** The time the latest network status was updated.*/
    protected long networkTimestamp;

    /** The time the latest validity data was updated.*/
    protected long validityTimestamp;
    
    /** The current status value.*/
    protected TCS_Status.Segment status;
    
	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#clientGetStatus()
	 */
	public void clientGetStatus() {
	
		//cil.sendMessage("SHOW ARSE"+,crh, 10000L);
		
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#configure(java.io.File)
	 */
	public void configure(File file) throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#configure(ngat.util.ConfigurationProperties)
	 */
	public void configure(ConfigurationProperties config) throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#getName()
	 */
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#getNetworkTimestamp()
	 */
	public long getNetworkTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#getStatus()
	 */
	public StatusCategory getStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#getValidityTimestamp()
	 */
	public long getValidityTimestamp() {
		return validityTimestamp;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#initClient()
	 */
	public void initClient() throws ClientInitializationException {
		// TODO Auto-generated method stub
		try {
			cil = (CilService)Naming.lookup("rmi://localhost/TcsCilService");
		} catch (Exception e) { 		  
		    throw new ClientInitializationException("TCSCilClient: Unable to access Cil service");
		}
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#isNetworkAvailable()
	 */
	public boolean isNetworkAvailable() {
		return networkAvailable;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#isStatusValid()
	 */
	public boolean isStatusValid() {
		return valid;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.scm.collation.StatusMonitorClient#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	public void actioned() throws RemoteException {
		// crh
	}

	public void completed(String arg0) throws RemoteException {
		// crh
		valid = true;
		validityTimestamp = System.currentTimeMillis();
		CilStatusExtractor cex = new CilStatusExtractor(555);
		//cex.;
		//status = 
	}

	public void error(int arg0, String arg1) throws RemoteException {
		// crh
		valid = true;
		networkAvailable = true;
		networkTimestamp = System.currentTimeMillis();	
	}

	public void timedout(String arg0) throws RemoteException {
		// crh
		networkAvailable = false;
		networkTimestamp = System.currentTimeMillis();	
	}

}
