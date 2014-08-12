/**
 * 
 */
package ngat.rcs.scm.collation;

import ngat.message.GUI_RCS.ID;
import ngat.rcs.RCS_Controller;
import ngat.rcs.ops.OperationsManager;
import ngat.rcs.tms.TaskOperations;
import ngat.rcs.tms.manager.DefaultModalTask;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * @author eng
 *
 */
public class InternalStatusProvider implements StatusProvider {

	/**
	 * 
	 */
	public InternalStatusProvider() {
		
	}

	/** Provide internal status on demand. This is extracted as required.
	 * @see ngat.util.StatusProvider#getStatus()
	 */
	public StatusCategory getStatus() {
		
		InternalStatus status = new InternalStatus();

		status.setControl(ID.RCS_PROCESS);

		status.setOperational(RCS_Controller.controller.isOperational());
		//status.setEngineering(RCS_Controller.controller.isEngineering());
		// OP or ENG otherwise it is STBY

		status.setAgentActivity("not-available"); // not yet
		status.setTimeStamp(System.currentTimeMillis());
	
		// Find out from OpsMgr which MCA is current
	        OperationsManager opsMgr = TaskOperations.getInstance().getOperationsManager();

	        DefaultModalTask mca = opsMgr.getCurrentModeController();

	        if (mca != null) {
	            String mcaName = mca.getAgentDesc();
	            if (mcaName != null)
			status.setAgentName(mca.getAgentDesc());
	            else
			status.setAgentName("None");

	            String mcaId = mca.getAgentId();
	            if (mcaId != null)
			status.setAgentInControl(mcaId);
	            else
			status.setAgentInControl("None");
		} else {
		    status.setAgentName("Idle");
		    status.setAgentInControl("Idle");
	        }

		return status;
	}

}
