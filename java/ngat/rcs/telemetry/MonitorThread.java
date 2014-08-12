/**
 * 
 */
package ngat.rcs.telemetry;

import java.util.Iterator;
import java.util.List;

import ngat.phase2.IExecutionFailureContext;
import ngat.rcs.ops.GroupCompletedEvent;
import ngat.rcs.ops.GroupSelectedEvent;
import ngat.rcs.ops.OperationsEventListener;
import ngat.sms.GroupItem;

/** Monitors the GOM event list.
 * @author eng
 *
 */
public class MonitorThread extends Thread {

	List <MonitorEvent> events;

	List<GroupOperationsListener> listeners;
	
	List<OperationsEventListener> oelisteners;
	
	/**
	 * @param events
	 */
	public MonitorThread(List<MonitorEvent> events, List<GroupOperationsListener> listeners, List<OperationsEventListener> oelisteners) {
		super("GOMEventDespatcher");
		this.events = events;
		this.listeners = listeners;
		this.oelisteners = oelisteners;
	}
	
	@Override
	public void run() {
		
		System.err.println("GOMEventDespatcher:: Starting...");
		
		while (true) {
			
			try {Thread.sleep(5000L);} catch (InterruptedException ix) {}
			
			while(!events.isEmpty()) {
				
				System.err.println("GOMEventDespatcher:: Check event list..."+events.size()+" events");
				
				// ----------------------------------------------------------------------------
				// TODO NEED TO TRANSLATE MON_EVENTS INTO OPS_EVENTS TO DESPATCH TO OPSGATEWAY
				// ----------------------------------------------------------------------------
				
				MonitorEvent mev = events.remove(0);
				System.err.println("GOMEventDespatcher:: Process event: "+mev);
				if (mev instanceof SelectionEvent) {
					GroupItem group = ((SelectionEvent)mev).getGroup();
					Iterator<GroupOperationsListener> il = listeners.iterator();
					while (il.hasNext()) {
						GroupOperationsListener l = il.next();
					
						try {
							System.err.println("GOMEventDespatcher:: Notify listener: "+l);
							l.groupSelected(group);	
							
						} catch (Exception e) {
							il.remove();
							System.err.println("GOMEventDespatcher::Removed unresponsive listener: "+l);
							e.printStackTrace();
						}

					}
					
					// Translate into OperationsEvent
					GroupSelectedEvent gse = new GroupSelectedEvent(mev.getTime(), ((SelectionEvent) mev).getGroup());
					for (int i = 0; i < oelisteners.size(); i++) {
						OperationsEventListener oel = oelisteners.get(i);
						try {
							System.err.println("GOMEventDespatcher:: Notify OElistener : "+oel.getClass().getSimpleName()+" "+gse);
							oel.operationsEventNotification(gse);					
						} catch (Exception e) {							
							e.printStackTrace();
						}
						
					}
					
				} else if
				(mev instanceof CompletionEvent) {
					GroupItem group = ((CompletionEvent)mev).getGroup();
					IExecutionFailureContext error = ((CompletionEvent)mev).getError();
					Iterator<GroupOperationsListener> il = listeners.iterator();
					while (il.hasNext()) {
						GroupOperationsListener l = il.next();
					
						try {
							System.err.println("GOMEventDespatcher:: Notify listener: "+l);						
							l.groupCompleted(group, error);
						} catch (Exception e) {
							il.remove();
							System.err.println("GOMEventDespatcher::Removed unresponsive listener: "+l);
							e.printStackTrace();
						}

					}
					
					// Translate into OperationsEvent
					GroupCompletedEvent gce = new GroupCompletedEvent(mev.getTime(), ((CompletionEvent) mev).getGroup(), ((CompletionEvent) mev).getError());
					for (int i = 0; i < oelisteners.size(); i++) {
						OperationsEventListener oel = oelisteners.get(i);
						try {
							System.err.println("GOMEventDespatcher:: Notify OElistener: "+oel.getClass().getSimpleName()+" "+gce);
							oel.operationsEventNotification(gce);					
						} catch (Exception e) {							
							e.printStackTrace();
						}
						
					}
					
					
				}
				
				System.err.println("GOMEventDespatcher:: Finished processing events");
				
			} // next event
						
		}			
	}
	
}
