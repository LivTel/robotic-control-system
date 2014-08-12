/*   
    Copyright 2006, Astrophysics Research Institute, Liverpool John Moores University.

    This file is part of Robotic Control System.

     Robotic Control Systemis free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Robotic Control System is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Robotic Control System; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package ngat.rcs.scm.collation;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.ops.OperationsManager;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.io.*;

import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;
import ngat.message.GUI_RCS.*;
import ngat.message.base.*;

/** 
 * Status grabber client for extracting status from the TCS.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: RCSInternalStatusClient.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/RCSInternalStatusClient.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class RCSInternalStatusClient implements StatusMonitorClient {

    /** Holds RCS internal status.*/
    private InternalStatus status;

   /** Name for this client.*/
    protected String name;

    /** The time the status was updated.*/
    protected long timeStamp;

    public RCSInternalStatusClient() {
	status = new InternalStatus();
    }

    /** Sets the name for this client.*/
    public void setName(String name) {this.name = name;}
    
    /** Returns the name.*/
    public String getName(){ return name;}
    
    /** Configure from File.
     * @param file File to read configuration from.
     * @throws IOException If there is a problem opening or reading from the file.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();	
	config.load(new FileInputStream(file));
	configure(config);
    }
	
    /** Configure from properties.
     * @param config The configuration properties.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(ConfigurationProperties config) throws IllegalArgumentException {}

	    
    /** Initialize the client.*/
    public void initClient() throws ClientInitializationException {
	timeStamp = System.currentTimeMillis();
    }

    /** Client should attempt to get status from network resource.*/
    public void clientGetStatus() {
	
	status.setControl(ID.RCS_PROCESS);

	status.setOperational(RCS_Controller.controller.isOperational());
	//status.setEngineering(RCS_Controller.controller.isEngineering());
	// OP or ENG otherwise it is STBY

	status.setAgentActivity("not-available"); // not yet
	status.setTimeStamp(timeStamp);

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

    }

    /** Returns true if the status is valid.*/
    public boolean isStatusValid() {return true;}

    /** Returns true if the network resource is available.*/
    public boolean isNetworkAvailable() {return true;}

    /** Returns the time the latest network status was updated.*/
    public long getNetworkTimestamp() { return timeStamp;}

    /** Returns the time the latest validity data was updated.*/
    public long getValidityTimestamp() { return timeStamp;}

    /** Provides clients with the opportunity to return a lightweight implementation
     * of StatusCategory. Clients can just return 'this' as they already implement
     * the StatusCategory interface anyway.
     */
    public StatusCategory getStatus() {return status;}

}
