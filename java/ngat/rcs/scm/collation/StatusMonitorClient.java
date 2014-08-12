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
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;

import java.io.*;

/** StatusMonitorClient is implemented by any classes which will acts as requestors
 * of Status information and thence as Providers of status.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: StatusMonitorClient.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/StatusMonitorClient.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface StatusMonitorClient extends StatusProvider {

    /** Sets the name for this client.*/
    public void setName(String name);
    
    /** Returns the name.*/
    public String getName();
    
    /** Configure from File.
     * @param file File to read configuration from.
     * @throws IOException If there is a problem opening or reading from the file.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(File file) throws IOException, IllegalArgumentException;
	
    /** Configure from properties.
     * @param config The configuration properties.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(ConfigurationProperties config) throws IllegalArgumentException;
	    
    /** Initialize the client.*/
    public void initClient() throws ClientInitializationException;

    /** Client should attempt to get status from network resource.*/
    public void clientGetStatus();

    /** Returns true if the status is valid.*/
    public boolean isStatusValid();

    /** Returns true if the network resource is available.*/
    public boolean isNetworkAvailable();

    /** Returns the time the latest network status was updated.*/
    public long getNetworkTimestamp();

    /** Returns the time the latest validity data was updated.*/
    public long getValidityTimestamp();

    /** Provides clients with the opportunity to return a lightweight implementation
     * of StatusCategory. Clients can just return 'this' as they already implement
     * the StatusCategory interface anyway.
     */
    public StatusCategory getStatus();

}

/** $Log: StatusMonitorClient.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */
