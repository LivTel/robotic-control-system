/**
 * 
 */
package ngat.rcs.control;

import java.util.Vector;

import ngat.message.GUI_RCS.GET_SEEING;
import ngat.message.GUI_RCS.GET_SEEING_DONE;
import ngat.message.GUI_RCS.GET_STATUS;
import ngat.message.GUI_RCS.GUI_TO_RCS;
import ngat.net.IConnection;
import ngat.rcs.emm.LegacyStatusProviderRegistry;
import ngat.rcs.scm.collation.SeeingHistoryStatus;
import ngat.rcs.scm.collation.SeeingStatus;
import ngat.rcs.scm.collation.SkyModelProvider;

/**
 * @author eng
 *
 */
public class GET_SEEINGImpl extends CtrlCommandImpl {

	/**
	 * @param connection
	 * @param command
	 */
	public GET_SEEINGImpl(IConnection connection, GUI_TO_RCS command) {
		super(connection, command);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see ngat.net.camp.CAMPRequestHandler#handleRequest()
	 */
	public void handleRequest() {
	
		GET_SEEING getsee = (GET_SEEING) command;
		
		GET_SEEING_DONE done = new GET_SEEING_DONE(command.getId());
		
		LegacyStatusProviderRegistry emm = LegacyStatusProviderRegistry
				.getInstance();
		if (emm == null) {
			sendError(done, GET_STATUS.NOT_AVAILABLE, "EMM Registry not found");
			return;
		}
		
		long time = getsee.getLastSampleTime();
		SeeingHistoryStatus seeingHistory = null;
		Vector<SeeingStatus> samples = null;
		try {
			SkyModelProvider smp = emm.getSkyModelProvider();
			seeingHistory = smp.getHistorySince(time);
			samples = seeingHistory.getHistory();
			System.err.println("GET_SEEING_HISTORY: extracted: "+(samples != null ? samples.size() : "zero")+" samples");
			
		} catch (Exception e) {
			e.printStackTrace();
			sendError(done, GET_STATUS.NOT_AVAILABLE, "Not available: SEEING");
			return;
		}

		done.setSuccessful(true);
		done.setSeeingData(samples);
		sendDone(done);
		
	}

}
