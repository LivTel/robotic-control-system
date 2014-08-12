/**
 * 
 */
package ngat.rcs.ers.test;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusProvider;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.ems.WmsStatus;
import ngat.rcs.ers.AverageFilter;
import ngat.rcs.ers.ContinuousFilter;
import ngat.rcs.ers.Criterion;
import ngat.rcs.ers.GTCriterion;
import ngat.rcs.ers.LTCriterion;
import ngat.rcs.ers.ReactiveSystemMonitor;
import ngat.rcs.ers.ReactiveSystemUpdateListener;
import ngat.rcs.ers.Rule;
import ngat.rcs.ers.TimeRule;

/**
 * @author eng
 * 
 */
public class TestSystem extends UnicastRemoteObject implements MeteorologyStatusUpdateListener,  ReactiveSystemMonitor {

	
	List<ReactiveSystemUpdateListener> listeners;

	ContinuousFilter humSlow;
	ContinuousFilter humFast;
	
	ContinuousFilter wsSlow;
	ContinuousFilter wsFast;
	
	Criterion c1;
	Criterion c2;
	Criterion c3;
	Criterion c4;
	Criterion c5;
	Criterion c6;
	
	Rule r1;
	Rule r2;
	Rule r3;
	Rule r4;
	Rule r5;
	Rule r6;
	/**
	 * 
	 */
	public TestSystem() throws RemoteException {
		super();
		listeners = new Vector<ReactiveSystemUpdateListener>();
		setup();
	}

	private void setup() {
		
		// create some filters...
		humSlow = new AverageFilter("H_S", 60000L);
		humFast = new AverageFilter("H_F", 10000L);
		wsSlow = new AverageFilter("WS_S", 60000L);
		wsFast = new AverageFilter("WS_F", 10000L);
		
		// create some crits...
		c1 = new GTCriterion("C1", 0.8);
		c2 = new LTCriterion("C2", 0.8);
		c3 = new LTCriterion("C3", 0.75);
		c4 = new GTCriterion("C4", 15.0);
		c5 = new LTCriterion("C5", 15.0);
		c6 = new LTCriterion("C6", 10.0);		
		
		// create some rules...
		r1 = new TimeRule("R1", 20000L);
		r2 = new TimeRule("R2", 1800000L);
		r3 = new TimeRule("R3", 1200000L);
		r4 = new TimeRule("R4", 20000L);
		r5 = new TimeRule("R5", 1800000L);
		r6 = new TimeRule("R6", 1200000L);
		
		
	}
	
	
	/**
	 * @see
	 * ngat.rcs.ers.ReactiveSystemMonitor#addReactiveSystemUpdateListener
	 * (ngat.rcs.ers.ReactiveSystemUpdateListener)
	 */
	public void addReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {
		if (listeners.contains(l))
			return;
		listeners.add(l);
		System.err.println("TS::Adding listener: "+l);
	}

	/**
	 * @see
	 * ngat.rcs.ers.ReactiveSystemMonitor#removeReactiveSystemUpdateListener
	 * (ngat.rcs.ers.ReactiveSystemUpdateListener)
	 */
	public void removeReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {
		if (!listeners.contains(l))
			return;
		listeners.remove(l);
	}

	private void notifyListenersFilterUpdated(String filterName, long time, Number updateValue, Number filterOutputValue) {
		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();
			
			try {
				rsul.filterUpdated(filterName, time, updateValue, filterOutputValue);
			} catch (Exception e) {
				System.err.println("Removing unresponsive filter update listener: "+rsul);
				il.remove();
			}
		}
	}

	private void notifyListenersCriterionUpdated(String critName, long time, boolean critOutputValue) {
		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();
			
			try {
				rsul.criterionUpdated(critName, time, critOutputValue);
			} catch (Exception e) {
				System.err.println("Removing unresponsive criterion update listener: "+rsul);
				il.remove();
			}
		}
		
		
	}

	private void notifyListenersRuleUpdated(String ruleName, long time, boolean ruleOutputValue) {
		
		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();
			
			try {
				rsul.ruleUpdated(ruleName, time, ruleOutputValue);
			} catch (Exception e) {
				System.err.println("Removing unresponsive rule update listener: "+rsul);
				il.remove();
			}
		}
		
	}


	public void meteorologyStatusUpdate(MeteorologyStatus status) throws RemoteException {
		if (! (status instanceof WmsStatus))
			return;
		
		WmsStatus wms = (WmsStatus)status;
		
		System.err.println("TS::Received: "+status);
		
		long time = wms.getStatusTimeStamp();
		double hum = wms.getHumidity();
		double ws = wms.getWindSpeed();
		
		
		System.err.println("TS:: Hum="+hum+", WS="+ws);
		
		// update filters
		double hfu = humFast.filterUpdate(time, hum);
		notifyListenersFilterUpdated(humFast.getFilterName(), time, hum, hfu);
		double hsu = humSlow.filterUpdate(time, hum);
		notifyListenersFilterUpdated(humSlow.getFilterName(), time, hum, hsu);
		
		double wfu = wsFast.filterUpdate(time, ws);
		notifyListenersFilterUpdated(wsFast.getFilterName(), time, ws, wfu);
		double wsu = wsSlow.filterUpdate(time, ws);
		notifyListenersFilterUpdated(wsSlow.getFilterName(), time, ws, wsu);
		
		// update crits
		boolean b1 = c1.criterionUpdate(time, hfu);
		notifyListenersCriterionUpdated(c1.getCriterionName(), time, b1);
		
		boolean b2 = c2.criterionUpdate(time, hsu);
		notifyListenersCriterionUpdated(c2.getCriterionName(), time, b2);
		
		boolean b3 = c3.criterionUpdate(time, hsu);
		notifyListenersCriterionUpdated(c3.getCriterionName(), time, b3);
		
		boolean b4 = c4.criterionUpdate(time, wfu);
		notifyListenersCriterionUpdated(c4.getCriterionName(), time, b4);
		
		boolean b5 = c5.criterionUpdate(time, wsu);
		notifyListenersCriterionUpdated(c5.getCriterionName(), time, b5);
		
		boolean b6 = c6.criterionUpdate(time, wsu);
		notifyListenersCriterionUpdated(c6.getCriterionName(), time, b6);
		
		// update rules
		double r1v = r1.ruleUpdate(time, b1);
		notifyListenersRuleUpdated(r1.getRuleName(), time, (r1v > 0.5));
		
		double r2v = r2.ruleUpdate(time, b2);
		notifyListenersRuleUpdated(r2.getRuleName(), time, (r2v > 0.5));
		
		double r3v = r3.ruleUpdate(time, b3);
		notifyListenersRuleUpdated(r3.getRuleName(), time, (r3v > 0.5));
		
		double r4v = r4.ruleUpdate(time, b4);
		notifyListenersRuleUpdated(r4.getRuleName(), time, (r4v > 0.5));
		
		double r5v = r5.ruleUpdate(time, b5);
		notifyListenersRuleUpdated(r5.getRuleName(), time, (r5v > 0.5));
		
		double r6v = r6.ruleUpdate(time, b6);
		notifyListenersRuleUpdated(r6.getRuleName(), time, (r6v > 0.5));
		
	}
	
	public static void main(String args[]) {
		
		try {
			
			// locate EMS MeteoProvider
			
			MeteorologyStatusProvider meteo = (MeteorologyStatusProvider)Naming.lookup("rmi://ltsim1/Meteorology");
			
			System.err.println("TS:: Located Meteo provider: "+meteo);
			
			TestSystem ts = new TestSystem();
			ts.addReactiveSystemUpdateListener(new InternalListener());
			
			meteo.addMeteorologyStatusUpdateListener(ts);
			System.err.println("TS:: Bound to provider");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @author eng
	 *
	 */
	public static class InternalListener implements ReactiveSystemUpdateListener {

		/* (non-Javadoc)
		 * @see ngat.rcs.newenv.ReactiveSystemUpdateListener#filterUpdated(java.lang.String, long, java.lang.Number, java.lang.Number)
		 */
		public void filterUpdated(String filterName, long time, Number updateValue, Number filterOutputValue)
				throws RemoteException {
			System.err.printf("Filter updated: %tF %tT %6s %4.2f : %4.2f \n", 
					time, time, filterName, updateValue, filterOutputValue);

		}

		/* (non-Javadoc)
		 * @see ngat.rcs.newenv.ReactiveSystemUpdateListener#criterionUpdated(java.lang.String, long, boolean)
		 */
		public void criterionUpdated(String critName, long time, boolean critOutputValue) throws RemoteException {
			System.err.printf("Critrn updated: %tF %tT %6s %4b \n", 
					time, time, critName, critOutputValue);

		}

		/* (non-Javadoc)
		 * @see ngat.rcs.newenv.ReactiveSystemUpdateListener#ruleUpdated(java.lang.String, long, boolean)
		 */
		public void ruleUpdated(String ruleName, long time, boolean ruleOutputValue) throws RemoteException {
			System.err.printf("Rule   updated: %tF %tT %6s %4b \n", 
					time, time, ruleName, ruleOutputValue);

		}

	}

	
	
	
}
