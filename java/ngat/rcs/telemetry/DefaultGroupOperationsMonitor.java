/**
 * 
 */
package ngat.rcs.telemetry;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.phase2.IExecutionFailureContext;
import ngat.rcs.ops.OperationsEventListener;
import ngat.rcs.ops.OperationsMonitor;
import ngat.sms.GroupItem;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;



/**
 * @author eng
 *
 */
public class DefaultGroupOperationsMonitor extends UnicastRemoteObject implements GroupOperationsMonitor, OperationsMonitor {

	List<GroupOperationsListener> listeners;
	List<OperationsEventListener> oelisteners;
	List<MonitorEvent> events;
	
	Runnable r;
	
	/** Logger. */
	private LogGenerator slogger;
	
	/** Create a DefaultGroupOperationsMonitor.
	 * @throws RemoteException
	 */
	public DefaultGroupOperationsMonitor() throws RemoteException {
		super();
		listeners = new Vector<GroupOperationsListener>();
		oelisteners = new Vector<OperationsEventListener>();
		events = new Vector<MonitorEvent>();
		
		Logger alogger = LogManager.getLogger("OPS"); // probably should be
		// RCS.Telem
		slogger = alogger.generate().system("RCS").subSystem("Telemetry").srcCompClass(this.getClass().getSimpleName())
				.srcCompId("GOM");
		
		MonitorThread mt = new MonitorThread(events, listeners, oelisteners);
		mt.start();
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.GroupOperationsMonitor#addGroupOperationsListener(ngat.rcs.telemetry.GroupOperationsListener)
	 */
	public void addGroupOperationsListener(GroupOperationsListener l) throws RemoteException {
		if (listeners.contains(l))
			return;
		listeners.add(l);		
		slogger.create().info().level(2).msg("Add group listener: "+l).send();
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.GroupOperationsMonitor#removeGroupOperationsListener(ngat.rcs.telemetry.GroupOperationsListener)
	 */
	public void removeGroupOperationsListener(GroupOperationsListener l) throws RemoteException {
		if (! listeners.contains(l))
			return;
		listeners.remove(l);
	}

	public void notifyListenersGroupSelected(GroupItem groupß) {
		
		slogger.create().info().level(2).msg("Notify Listeners group selected: "+groupß).send();
		
		SelectionEvent sev = new SelectionEvent(groupß);
		sev.setTime(System.currentTimeMillis());
		events.add(sev);
		/*Iterator<GroupOperationsListener> il = statusListeners.iterator();
		while (il.hasNext()) {
			GroupOperationsListener l = (GroupOperationsListener)il.next();
		
			try {
				System.err.println("GOM: Notify listener: "+l);
				l.groupSelected(group);	
				
			} catch (Exception e) {
				il.remove();
				System.err.println("GOM::Removed unresponsive listener: "+l);
				e.printStackTrace();
			}

		}*/
		
	}
	
	public void notifyListenersGroupCompleted(GroupItem  group, IExecutionFailureContext error) {
		
		slogger.create().info().level(2).msg("Notify Listeners group completed: "+ group+", "+error).send();
		
		CompletionEvent cev = new CompletionEvent( group, error);
		cev.setTime(System.currentTimeMillis()); // True ??
		events.add(cev);
		/*Iterator<GroupOperationsListener> il = statusListeners.iterator();
		while (il.hasNext()) {
			GroupOperationsListener l = (GroupOperationsListener)il.next();
		
			try {
				System.err.println("GOM: Notify listener: "+l);
				l.groupCompleted(group, error);
				
			} catch (Exception e) {
				il.remove();
				System.err.println("GOM::Removed unresponsive listener: "+l);
				e.printStackTrace();
			}

		}*/
		
	}

	public void addOperationsEventListener(OperationsEventListener l) throws RemoteException {
		if (oelisteners.contains(l))
			return;
		oelisteners.add(l);	
		slogger.create().info().level(2).msg("Add Operations listener: "+l).send();
	}

	public void removeOperationsEventListener(OperationsEventListener l) throws RemoteException {
		if (! oelisteners.contains(l))
			return;
		oelisteners.remove(l);
		slogger.create().info().level(2).msg("Remove Operations listener: "+l).send();
	}
	
	//public void notifyListenerGroupUpdate(GroupItem group, ExposureInfo
	
}
