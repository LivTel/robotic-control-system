/**
 * 
 */
package ngat.rcs.newstatemodel.test;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import ngat.rcs.ers.ReactiveSystemUpdateListener;
import ngat.rcs.newstatemodel.EnvironmentChangeEvent;
import ngat.rcs.newstatemodel.StandardStateModel;
import ngat.util.ConfigurationProperties;
import ngat.util.PropertiesConfigurable;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class BasicStateModelReactiveInput implements
		ReactiveSystemUpdateListener, PropertiesConfigurable {

	private Map<String, Integer> ruleEventMap;
	
	private StandardStateModel tsm;
	
	/** Logger. */
	private LogGenerator slogger;
	
	public BasicStateModelReactiveInput(StandardStateModel tsm) {	
		this.tsm = tsm;
		ruleEventMap = new HashMap<String, Integer>();
		
		Logger alogger = LogManager.getLogger("ERS"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("Reactive")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId("SMRI");
	}

	public void filterUpdated(String filterName, long time, Number updateValue,
			Number filterOutputValue) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	public void criterionUpdated(String critName, long time,
			boolean critOutputValue) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	public void ruleUpdated(String ruleName, long time, boolean ruleOutputValue)
			throws RemoteException {
		
		if (ruleEventMap.containsKey(ruleName) && ruleOutputValue) {
		
			slogger.create().info().level(3).extractCallInfo()
				.msg("Known rule: "+ruleName).send();
			
			int ev = ruleEventMap.get(ruleName);
			EnvironmentChangeEvent event = new EnvironmentChangeEvent(ev);
			tsm.environmentChanged(event);
		}// else {
		//	System.err.println("TSMR: RuleUpdated() : : UNKnown rule: "+ruleName);
		//}	
	
	}
	public void configure(File file) throws Exception {

		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));
		configure(config);
		
	}
	
	public void configure(ConfigurationProperties config) throws Exception {
		
		Enumeration e = config.keys();
		while (e.hasMoreElements()) {
			String ruleName = (String)e.nextElement();
			int ev = config.getIntValue(ruleName);
		
			slogger.create().info().level(1).extractCallInfo()
			.msg("Add mapping : "+ruleName+" -> "+ev).send();
			
			ruleEventMap.put(ruleName, ev);
			
		}
		
	}

}
