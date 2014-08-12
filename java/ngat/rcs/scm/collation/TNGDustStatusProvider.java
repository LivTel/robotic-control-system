/**
 * 
 */
package ngat.rcs.scm.collation;

import java.rmi.RemoteException;

import ngat.ems.DustStatus;
import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeStatusProvider;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class TNGDustStatusProvider implements StatusProvider,
		MeteorologyStatusUpdateListener {

	private MappedStatusCategory dustStatus;

	
	
	public TNGDustStatusProvider() {
		dustStatus = new MappedStatusCategory();
		dustStatus.addKeyword("dust", MappedStatusCategory.DOUBLE_DATA, "Dust level", "?");
	}

	
	/* (non-Javadoc)
	 * @see ngat.util.StatusProvider#getStatus()
	 */
	public StatusCategory getStatus() {
		return dustStatus;
	}


	public void meteorologyStatusUpdate(MeteorologyStatus status)
			throws RemoteException {
		
		if (status instanceof DustStatus) {
			dustStatus.setTimeStamp(status.getStatusTimeStamp());
			dustStatus.addData("dust", ((DustStatus) status).getDust());
		}
		
	}

}
