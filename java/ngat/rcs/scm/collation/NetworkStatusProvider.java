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
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;

/** 
 * Provider of Network Status information.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 *    <dd>$Id: NetworkStatusProvider.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 *    <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/NetworkStatusProvider.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class NetworkStatusProvider implements StatusProvider {

    /** Network status.*/
    protected NetworkStatus netStatus;

    /** Create a NetworkStatusProvider to provide the specified status.
     * @param netStatus The NetworkStatus.
     */
    public NetworkStatusProvider(NetworkStatus netStatus) {
	this.netStatus = netStatus;
    }
    
    /** Returns the NetworkStatus.*/
    public StatusCategory getStatus() {
	return netStatus;
    }

}
