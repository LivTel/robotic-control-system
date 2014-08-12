/**
 * 
 */
package ngat.rcs.scm.collation;

import java.rmi.RemoteException;
import java.util.Vector;

import ngat.ems.SkyModel;
import ngat.ems.SkyModelUpdateListener;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class SkyModelProvider implements SkyModelUpdateListener {
	
	private SkyModel skyModel;
	
	private Vector<SeeingStatus> fullHistory;

	/**
	 * 
	 */
	public SkyModelProvider(SkyModel skyModel) {
		this.skyModel =skyModel;
		fullHistory = new Vector<SeeingStatus>();
	}

	public void extinctionUpdated(long time, double ext) throws RemoteException {	
		System.err.println("SkyProvider:updated ext="+ext+", dont care.");
	
	}

	public void seeingUpdated(long time, double raw, double corrected, double prediction, double alt, double azm, double wav, boolean standard, String source, String targetName) throws RemoteException {
		System.err.println("SkyProvider:updated seeing: r="+raw+",c="+corrected+", adding to history.");
		
		// create new status entry for this sample...
		SeeingStatus status = new SeeingStatus();
		status.setCorrectedSeeing(corrected);
		status.setRawSeeing(raw);
		status.setTimeStamp(time);
		double mprediction = skyModel.getSeeing(700.0, 0.5*Math.PI, 0.0, time);
		status.setPrediction(prediction);
		status.setElevation(alt);
		status.setAzimuth(azm);
		status.setWavelength(wav);
		status.setStandard(standard);
		status.setSource(source);
		status.setTargetName(targetName);
		
		fullHistory.add(status);
		
	}

	public SeeingHistoryStatus getHistorySince(long time) {
		long now = System.currentTimeMillis();
		System.err.printf("SkyProvider:status requested since: %tF %tT (%6d ago) : \n",time, time, (now-time)/1000);
		
		// fill an array..
		Vector<SeeingStatus> recentHistory = new Vector<SeeingStatus>();
		
		// count any samples at samp.t > time
		for (int i = 0; i < fullHistory.size(); i++) {
			SeeingStatus seeing = fullHistory.get(i);
			if (seeing.getTimeStamp() > time)
				recentHistory.add(seeing);
		}
		
		SeeingHistoryStatus history = new SeeingHistoryStatus(recentHistory);
		return history;
	}



}
