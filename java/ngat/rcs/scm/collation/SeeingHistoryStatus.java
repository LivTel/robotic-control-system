/**
 * 
 */
package ngat.rcs.scm.collation;

import java.util.Vector;

import ngat.util.SerializableStatusCategory;
import ngat.util.StatusCategory;



/**
 * @author eng
 *
 */
public class SeeingHistoryStatus implements SerializableStatusCategory {

	/** Historic list of statii.*/
	private Vector<SeeingStatus> history;
	


	public SeeingHistoryStatus(Vector<SeeingStatus> history) {
		this.history = history;
	}


	
	
	public Vector<SeeingStatus> getHistory() {
		return history;
	}




	public double getStatusEntryDouble(String arg0)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return 0;
	}


	public String getStatusEntryId(String arg0) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}


	public int getStatusEntryInt(String arg0) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return 0;
	}


	public String getStatusEntryRaw(String arg0)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}


	public long getTimeStamp() {
		// TODO Auto-generated method stub
		return 0;
	}


	
	
	
	
	

}
