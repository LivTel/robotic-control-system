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
package ngat.rcs.scm.detection;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentStatus;
import ngat.icm.InstrumentStatusUpdateListener;

/**
 * Monitors status of an instrument and notifies observers when lost.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: DefaultAutoguiderMonitor.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/DefaultAutoguiderMonitor.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

//public class DefaultAutoguiderMonitor implements Observer {
public class DefaultInstrumentMonitor extends UnicastRemoteObject implements InstrumentStatusUpdateListener, InstrumentMonitor {
	
	protected Object lock = new Object();
	
	//private InstrumentDescriptor instId;
	private Vector listeners;

	public DefaultInstrumentMonitor() throws RemoteException {
		super();
		//this.instId = instId;
		listeners = new Vector();
	}


	public void instrumentStatusUpdated(InstrumentStatus status) throws RemoteException {
		if ( (!status.isOnline()) || (!status.isFunctional())) {
			for (int il = 0; il < listeners.size(); il++) {
				InstrumentStatusListener isl = (InstrumentStatusListener)listeners.get(il);
				//System.err.println("InstMon:: TODO Instrument Lost Notification for ["+status.getInstrument().getInstrumentName()+"] to ISL[" + (il) + "] " + isl);
				//isl.instrumentLost(iid);
			}		
		}
	}

	public void addInstrumentStatusListener(InstrumentStatusListener isl) throws RemoteException {
		if (!listeners.contains(isl)) {
			listeners.add(isl);
			System.err.println("InstMon::Added InstStatusListener: " + isl);
		}
	}

	public void removeInstrumentStatusListener(InstrumentStatusListener isl) throws RemoteException {
		System.err.println("InstMon::Requested to remove InstStatusListener: " + isl);
		if (!listeners.contains(isl))
			return;
		listeners.remove(isl);
		System.err.println("InstMon::Removed InstStatusListener: " + isl);
	}

	public void triggerInstrumentLost(InstrumentDescriptor instId) throws RemoteException {
		System.err.println("InstMon::Received external trigger");
		for (int il = 0; il < listeners.size(); il++) {
			InstrumentStatusListener isl = (InstrumentStatusListener)listeners.get(il);
			System.err.println("InstMon::Ext Trig InstrumentLost Notification ["+instId.getInstrumentName()+
					"] to ISL[" + (il) + "] " + isl);
			isl.instrumentLost(instId);
		}
	}

}
