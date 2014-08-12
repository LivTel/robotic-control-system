/**
 * 
 */
package ngat.rcs.tms.test;

import java.rmi.RemoteException;

import ngat.rcs.tms.BasicTaskMonitor;
import ngat.rcs.tms.TaskDescriptor;
import ngat.rcs.tms.TaskLifecycleListener;
import ngat.rcs.tms.events.TaskLifecycleEvent;

/**
 * @author eng
 *
 */
public class TestTaskMonitorCallback {

	private void exec() throws Exception {

		BasicTaskMonitor btm = new BasicTaskMonitor();
	
		btm.startEventDespatcher();
		
		// add some callback handlers
		//		for (int i = 0; i < 10; i++) {
			TestCallbackHandler tch = new TestCallbackHandler("a");
			btm.addTaskEventListener(tch);
			//	}
		
		
		// now create some events..
		for (int j = 0; j < 100; j++) {
			try {Thread.sleep(100);} catch(InterruptedException e) {}
			
			System.err.println("Create new event: "+j);
			btm.notifyListenersTaskCreated(new TaskDescriptor("manager", "Manager"), 
					new TaskDescriptor("subtask-"+j, "Exec"));
			
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			TestTaskMonitorCallback tcb = new TestTaskMonitorCallback();
			tcb.exec();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * @author eng
	 *
	 */
	public class TestCallbackHandler implements TaskLifecycleListener {

		private String n;

	    private int ic;

		/**
		 * @param n
		 */
		public TestCallbackHandler(String n) {
			super();
			this.n = n;
		}

	
		@Override
		public String toString(){
			return "tcbh["+n+"]";
		}


		public void taskLifecycleEventNotification(TaskLifecycleEvent event) throws RemoteException {
		    ic++;
		    System.err.println("TCH:"+n+" : event: "+ic+" "+event);			
		}
	}
}
