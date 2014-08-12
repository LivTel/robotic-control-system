/**
 * 
 */
package ngat.rcs.newstatemodel.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;

import ngat.rcs.newstatemodel.EnvironmentChangeEvent;
import ngat.rcs.newstatemodel.EnvironmentalChangeListener;

/**
 * @author eng
 * 
 */
public class FireEnvironmentChangeEvent {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			// where is the rcs
			String ehost = args[0];
			EnvironmentalChangeListener ecl = (EnvironmentalChangeListener) Naming.lookup("rmi://" + ehost
					+ "/StateModel");

			BufferedReader bin = new BufferedReader(new InputStreamReader(System.in));
			// keep reading EC events from terminal
			String line = null;
			while ((line = bin.readLine()) != null) {

				int ec = Integer.parseInt(line);
				System.err.println("Sending Event type: "+ec+" "+EnvironmentChangeEvent.typeToString(ec));				
				EnvironmentChangeEvent cev = new EnvironmentChangeEvent(ec);
				ecl.environmentChanged(cev);
				System.err.println("Event was processed");
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
