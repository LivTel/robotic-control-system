/**
 * 
 */
package ngat.rcs.scm.collation;

import java.rmi.RemoteException;

import ngat.ems.CloudStatus;
import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class BcsCloudStatusProvider implements StatusProvider,
		MeteorologyStatusUpdateListener {

	private MappedStatusCategory cloudStatus;

	
	public BcsCloudStatusProvider() {
		cloudStatus = new MappedStatusCategory();
		cloudStatus.addKeyword("t.diff", MappedStatusCategory.DOUBLE_DATA,
				"Sky-Amb", "C");
		cloudStatus.addKeyword("t.ambient", MappedStatusCategory.DOUBLE_DATA, "Ambient",
				"C");
		cloudStatus.addKeyword("t.sensor", MappedStatusCategory.DOUBLE_DATA, "Sensor",
				"C");
		cloudStatus.addKeyword("heater", MappedStatusCategory.DOUBLE_DATA, "Heater", "ADU");
		cloudStatus.addKeyword("wet.flag", MappedStatusCategory.DOUBLE_DATA, "Wetness",
				"");
		cloudStatus.addKeyword("dt", MappedStatusCategory.DOUBLE_DATA, "Last Rdng", "sec");
	}

	/* (non-Javadoc)
	 * @see ngat.ems.MeteorologyStatusUpdateListener#meteorologyStatusUpdate(ngat.ems.MeteorologyStatus)
	 */
	public void meteorologyStatusUpdate(MeteorologyStatus status)
			throws RemoteException {
		
		if (status instanceof CloudStatus) {
			CloudStatus cstatus = (CloudStatus)status;
			cloudStatus.setTimeStamp(status.getStatusTimeStamp());
			cloudStatus.addData("t.diff", new Double(cstatus.getSkyMinusAmb()));
			cloudStatus.addData("t.ambient", new Double(cstatus.getAmbientTemp()));
			cloudStatus.addData("t.sensor", new Double(cstatus.getSensorTemp()));
			cloudStatus.addData("heater", new Double(cstatus.getHeater()));
			cloudStatus.addData("wet.flag", new Double(cstatus.getWetFlag()));
			cloudStatus.addData("dt", new Double(0.0)); // not available via CloudStatus
		}
		
	}

	/* (non-Javadoc)
	 * @see ngat.util.StatusProvider#getStatus()
	 */
	public StatusCategory getStatus() {
		return cloudStatus;
	}

}
