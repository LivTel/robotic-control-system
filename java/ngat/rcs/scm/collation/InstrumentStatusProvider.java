/**
 * 
 */
package ngat.rcs.scm.collation;

import java.rmi.RemoteException;
import java.util.Hashtable;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusUpdateListener;
import ngat.instrument.Instrument;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class InstrumentStatusProvider implements StatusProvider, InstrumentStatusUpdateListener {

	private InstrumentDescriptor instId;
	
	private ngat.rcs.scm.collation.InstrumentStatus oinstat;
	
	/**
	 * @param instId
	 */
	public InstrumentStatusProvider(InstrumentDescriptor instId) {
		super();
		this.instId = instId;
		oinstat = new ngat.rcs.scm.collation.InstrumentStatus();
	}



	/* (non-Javadoc)
	 * @see ngat.util.StatusProvider#getStatus()
	 */
	public StatusCategory getStatus() {
		// TODO Auto-generated method stub
		//System.err.println("OISP: "+instId.getInstrumentName()+" : requested oistat: "+oinstat);
		return oinstat;
	}



	public void instrumentStatusUpdated(InstrumentStatus istat) throws RemoteException {
		//System.err.println("OISP: "+instId.getInstrumentName()+" : recieved update: "+istat);	
		//System.err.println("OISP: "+instId.getInstrumentName()+" : Map: "+istat.getStatus());
		oinstat.setOnlineStatus(istat.isOnline() ? Instrument.ONLINE : Instrument.OFFLINE);
		oinstat.setTimeStamp(System.currentTimeMillis());
		if (istat.getStatus() != null)
		oinstat.update(new Hashtable(istat.getStatus()));
	}

}
