/**
 * 
 */
package ngat.rcs.telemetry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import ngat.net.telemetry.SecondaryCache;
import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class TextFileBackingStore implements SecondaryCache {

	private File file;
	
	private BufferedWriter bout;

	
	/**
	 * @param file
	 */
	public TextFileBackingStore(File file) throws Exception {		
		this.file = file;
		bout = new BufferedWriter(new FileWriter(file));
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.SecondaryCache#storeStatus(ngat.net.telemetry.StatusCategory)
	 */
	public void storeStatus(StatusCategory status) throws Exception {
		bout.write(status.toString());
		bout.write("\n");
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.SecondaryCache#retrieveStatus(long, long)
	 */
	public List<StatusCategory> retrieveStatus(long t1, long t2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
