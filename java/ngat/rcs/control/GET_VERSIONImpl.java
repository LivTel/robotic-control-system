/**
 * 
 */
package ngat.rcs.control;

import ngat.message.GUI_RCS.GET_VERSION;
import ngat.message.GUI_RCS.GET_VERSION_DONE;
import ngat.message.GUI_RCS.GUI_TO_RCS;
import ngat.net.IConnection;
import ngat.rcs.RCS_Controller;

/**
 * @author eng
 *
 */
public class GET_VERSIONImpl extends CtrlCommandImpl {


    public GET_VERSIONImpl(IConnection connection, GUI_TO_RCS command) {
    	super(connection, command);
    }
	
    public void handleRequest() {

	//	System.err.println("A request for GET_VERSION was made");
    	
    	GET_VERSION getv = (GET_VERSION)command;

    	GET_VERSION_DONE done = new GET_VERSION_DONE(command.getId());
    	done.setMajorVersion(RCS_Controller.MAJOR_VERSION);
    	done.setMinorVersion(RCS_Controller.MINOR_VERSION);
    	done.setPatchVersion(RCS_Controller.PATCH_VERSION);
    	done.setReleaseName(RCS_Controller.RELEASE_NAME);
    	done.setBuildDate(RCS_Controller.BUILD_DATE);
    	done.setBuildNumber(RCS_Controller.BUILD_NUMBER);
    	done.setSuccessful(true);
    	sendDone(done);
    }
    
    
}
