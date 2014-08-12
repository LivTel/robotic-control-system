/**
 * 
 */
package ngat.rcs.scm.collation;

import java.rmi.RemoteException;

import ngat.tcm.AutoguiderActiveStatus;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class AgActiveProvider implements StatusProvider, TelescopeStatusUpdateListener {
	
	private AgActiveStatus agastatus;
	
	public AgActiveProvider() {
		agastatus = new AgActiveStatus();
	}
	
	/* (non-Javadoc)
	 * @see ngat.tcm.TelescopeStatusUpdateListener#telescopeNetworkFailure(long, java.lang.String)
	 */
	public void telescopeNetworkFailure(long arg0, String arg1) throws RemoteException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ngat.tcm.TelescopeStatusUpdateListener#telescopeStatusUpdate(ngat.tcm.TelescopeStatus)
	 */
	public void telescopeStatusUpdate(TelescopeStatus status) throws RemoteException {
		//System.err.println("AgProvider: Updated category: "+status.getCategoryName());
		if (status instanceof AutoguiderActiveStatus) {
			AutoguiderActiveStatus agstatus = (AutoguiderActiveStatus)status;
			//System.err.println("AgProvider: Updated with telescope status for: "+agstatus.getAutoguiderName());
			agastatus.setTimeStamp(agstatus.getStatusTimeStamp());
			agastatus.setActive(agstatus.isActiveStatus());
			agastatus.setName(agstatus.getAutoguiderName());
			agastatus.setOnLine(agstatus.isOnline());
			agastatus.setTemperature(agstatus.getTemperature());
			
			//System.err.println("AgProvider: Updated legacy carrier status: "+agastatus);
			
		}

	}

	/* (non-Javadoc)
	 * @see ngat.util.StatusProvider#getStatus()
	 */
	public StatusCategory getStatus() {
		//System.err.println("AgActiveProvider: Status requested: "+agastatus);
		return agastatus;
	}

}
