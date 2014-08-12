/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Vector;

import ngat.rcs.tms.events.TaskAbortedEvent;
import ngat.rcs.tms.events.TaskCancelledEvent;
import ngat.rcs.tms.events.TaskCompletedEvent;
import ngat.rcs.tms.events.TaskFailedEvent;
import ngat.rcs.tms.events.TaskInitializedEvent;
import ngat.rcs.tms.events.TaskLifecycleEvent;
import ngat.rcs.tms.events.TaskStartedEvent;

/**
 * @author eng
 * 
 */
public class BasicTaskMonitor extends UnicastRemoteObject implements TaskMonitor {

	private List<TaskLifecycleListener> liveListeners;
	private List<TaskLifecycleListener> deadListeners;
	private List<TaskLifecycleListener> newListeners;

	private List<TaskLifecycleEvent> events;

	private EventDespatcher despatcher;
	
	/**
	 * @throws RemoteException
	 */
	public BasicTaskMonitor() throws RemoteException {
		super();
		liveListeners = new Vector<TaskLifecycleListener>();
		deadListeners = new Vector<TaskLifecycleListener>();
		newListeners = new Vector<TaskLifecycleListener>();

		events = new Vector<TaskLifecycleEvent>();
		
		despatcher = new EventDespatcher();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seengat.rcs.tmm.TaskMonitor#addTaskEventListener(ngat.rcs.tmm.
	 * TaskLifecycleListener)
	 */
	public void addTaskEventListener(TaskLifecycleListener l) throws RemoteException {
		newListeners.add(l);
		//		System.err.println("BTM::addListener: " + l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seengat.rcs.tmm.TaskMonitor#removeTaskEventListener(ngat.rcs.tmm.
	 * TaskLifecycleListener)
	 */
	public void removeTaskEventListener(TaskLifecycleListener l) throws RemoteException {
		deadListeners.add(l);
	}

	/**
	 * Called when a task is newly created (usually by its manager).
	 * 
	 * @param t
	 * @throws RemoteException
	 */
	public void notifyListenersTaskCreated(TaskDescriptor mgr, TaskDescriptor t) throws RemoteException {
	    //System.err.println("BTM::notifyListenersTaskCreated: "+mgr+" >> "+t);
		//		events.add(new TaskCreatedEvent(System.currentTimeMillis(), mgr, t));
	}

	/**
	 * Called when a task has been initialized.
	 * 
	 * @param t
	 * @throws RemoteException
	 */
	public void notifyListenersTaskInitialized(TaskDescriptor t) throws RemoteException {
		events.add(new TaskInitializedEvent(System.currentTimeMillis(), t));
	}

	/**
	 * Called when a task's worker thread has been assigned.
	 * 
	 * @param tTaskDescriptor t
	 * @throws RemoteException
	 */
	public void notifyListenersTaskStarted(TaskDescriptor t) throws RemoteException {
		events.add(new TaskStartedEvent(System.currentTimeMillis(), t));
	}

	/**
	 * Called when a task has successfully completed.
	 * 
	 * @param t
	 * @throws RemoteException
	 */
	public void notifyListenersTaskCompleted(TaskDescriptor t) throws RemoteException {
		events.add(new TaskCompletedEvent(System.currentTimeMillis(), t));
	}

	/**
	 * Called when a task has failed due to some error.
	 * 
	 * @param t
	 * @param error
	 * @throws RemoteException
	 */
	public void notifyListenersTaskFailed(TaskDescriptor t, ErrorIndicator error) throws RemoteException {
		events.add(new TaskFailedEvent(System.currentTimeMillis(), t, error));
	}

	/**
	 * CAlled when a task has been aborted (usually by its manager).
	 * 
	 * @param t
	 * @param error
	 * @throws RemoteException
	 */
	public void notifyListenersTaskAborted(TaskDescriptor t, ErrorIndicator error) throws RemoteException {
		events.add(new TaskAbortedEvent(System.currentTimeMillis(), t, error));
	}
	
	/**
	 * CAlled when a task has been cancelled by its manager).
	 * 
	 * @param t
	 * @throws RemoteException
	 */
	public void notifyListenersTaskCancelled(TaskDescriptor t) throws RemoteException {
		events.add(new TaskCancelledEvent(System.currentTimeMillis(), t));	
	}
	
	public void startEventDespatcher() {
		if (! despatcher.isAlive())
		despatcher.start();
	}
	
	/**
	 * A thread to process the task events.
	 * 
	 * @author eng
	 * 
	 */
	private class EventDespatcher extends Thread {

		@Override
		public void run() {

			while (true) {

				// despatch any events to all known statusListeners
			    int ls = events.size();
				for (int j = 0; j < events.size(); j++) {
				    TaskLifecycleEvent e = events.get(j);

				    //TaskLifecycleEvent e = events.remove(j);
					TaskDescriptor t = e.getTask();
					//		System.err.println("BTM:Despatcher:: Despatch event: " + e);

					// check the pending statusListeners list and add any new chaps
					//System.err.println("BTM:Despatcher::Checking for new statusListeners...");
					for (int i = 0; i < newListeners.size(); i++) {
						TaskLifecycleListener l = newListeners.remove(i);
						if (!liveListeners.contains(l)) {
							liveListeners.add(l);
							//	System.err.println("BTM:Despatcher::addListener: " + l);
						}
					}
					// check the bad statusListeners list and take em out
					//System.err.println("BTM:Despatcher::Checking for dead statusListeners...");
					for (int i = 0; i < deadListeners.size(); i++) {
						TaskLifecycleListener l = deadListeners.remove(i);
						if (liveListeners.contains(l)) {
							liveListeners.remove(l);
							//System.err.println("BTM:Despatcher::removeListener: " + l);
						}
					}

					//System.err.println("BTM:Despatcher::notifying upto: " + liveListeners.size() + " statusListeners");

					TaskLifecycleListener l = null;
					for (int i = 0; i < liveListeners.size(); i++) {
						l = liveListeners.get(i);

						try {
							l.taskLifecycleEventNotification(e);
						} catch (Exception ex) {
							ex.printStackTrace();
							//	System.err.println("BTM:Despatcher::record failed handler: " + l);
							deadListeners.add(l);
						}
					} // next listener

				} // next event

				// remove the items from start of list
				events.subList(0, ls).clear();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException ix) {
				}

			} // repeat forever

		} // run()

	} // [despatcher]

	

}
