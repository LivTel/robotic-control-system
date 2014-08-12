package ngat.rcs.scm.detection;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import ngat.message.GUI_RCS.*;
import ngat.util.*;

import ngat.rcs.*;
import ngat.rcs.newstatemodel.*;

public class BasicWeatherMonitoring extends UnicastRemoteObject implements WeatherMonitoring, Runnable,
		ControlableThread {

	String goodState;
	String badState;

	String key;

	volatile long badStart = 0L;
	volatile long goodStart = 0L;

	volatile int currentState = WEATHER_UNKNOWN;

	/** Create a BasicWeatherMonitoring. */
	public BasicWeatherMonitoring() throws RemoteException {
		super();
	}

	/** Set the access key for state item. */
	public void setKey(String key) {
		this.key = key;
	}

	/** Set value for GOOD state. */
	public void setGoodState(String gs) {
		this.goodState = gs;
	}

	/** Set value for BAD state. */
	public void setBadState(String bs) {
		this.badState = bs;
	}

	/** Loop grabbing state info. */
	public void run() {

		loadData();

		while (true) {

			// 60 second polling
			try {
				Thread.sleep(60000L);
			} catch (InterruptedException ix) {
			}

			long now = System.currentTimeMillis();

			// Grab the state model info...
			// RCS_ControlTask ca = RCS_ControlTask.getInstance();

			// Map stateMap = ca.getStateInfo();

			// System.err.println("StateMap: "+stateMap);

			// TODO new stuff

			StandardStateModel tsm = RCS_Controller.controller.getTestStateModel();

			// variables
			Map map = tsm.getStateInfo();

			if (map != null) {

				if (map.containsKey("WEATHER")) {

					Integer weatherState = (Integer) map.get("WEATHER");
					int wstate = weatherState.intValue();

					switch (currentState) {
					case WEATHER_BAD:
						if (wstate == EnvironmentChangeEvent.WEATHER_CLEAR) {
							goodStart = now;
							currentState = WEATHER_GOOD;
							//System.err.println("BWM:: Switching to CLEAR");
						} else
							//System.err.println("BWM::BAD since: " + ((now - badStart) / 1000) + "S");
						break;
					case WEATHER_GOOD:
						if (wstate == EnvironmentChangeEvent.WEATHER_ALERT) {
							badStart = now;
							currentState = WEATHER_BAD;
							//System.err.println("BWM:: Switching to THREAT");
						} else
							//System.err.println("BWM::GOOD since: " + ((now - goodStart) / 1000) + "S");
						break;
					default:
						// Unknown initially

						if (wstate == EnvironmentChangeEvent.WEATHER_CLEAR) {
							goodStart = now;
							currentState = WEATHER_GOOD;
							//System.err.println("BWM:: Switching to CLEAR");
						} else {
							badStart = now;
							currentState = WEATHER_BAD;
							//System.err.println("BWM:: Switching to THREAT");
						}
					}

				}

			}

		}

	}

	public void terminate() {
		dumpData();
	}

	/** Dump weather data to file. */
	public void dumpData() {

		Properties data = new Properties();
		data.setProperty("state", "" + currentState);
		long howlong = 0L;
		switch (currentState) {
		case WEATHER_GOOD:
			howlong = System.currentTimeMillis() - goodStart;
		case WEATHER_BAD:
			howlong = System.currentTimeMillis() - badStart;
		}
		data.setProperty("time", "" + howlong);

		try {
			FileOutputStream fout = new FileOutputStream("bwm.dat");
			data.store(fout, "Weather data");
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void loadData() {

		try {

			ConfigurationProperties data = new ConfigurationProperties();
			FileInputStream fin = new FileInputStream("bwm.dat");
			data.load(fin);

			currentState = data.getIntValue("state", WEATHER_BAD);
			long time = data.getLongValue("time", 0L);

			long now = System.currentTimeMillis();
			switch (currentState) {
			case WEATHER_GOOD:
				goodStart = now - time;
			case WEATHER_BAD:
				badStart = now - time;
			}

			//System.err.println("Setting weather state: " + currentState + " for: " + ((now - time) / 3600000) + "H");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** Returns true if the weather is currently good. */
	public int getWeatherState() throws RemoteException {
		return currentState;
	}

	/**
	 * Returns the length of time the weather has been stable (i.e. good or
	 * bad).
	 */
	public long getWeatherStableTime() throws RemoteException {
		switch (currentState) {
		case WEATHER_GOOD:
			return System.currentTimeMillis() - goodStart;
		case WEATHER_BAD:
			return System.currentTimeMillis() - badStart;
		default:
			return 0L;
		}
	}

	public static void main(String args[]) {
		try {
			BasicWeatherMonitoring bwm = new BasicWeatherMonitoring();
			bwm.setGoodState("CLEAR");
			bwm.setBadState("ALERT");
			bwm.setKey("C:THREAT");
			System.err.println("Ready to Bind BasicWeatherMonitoring to local registry");
			Naming.rebind("rmi://localhost:1099/WeatherMonitoring", bwm);
			System.err.println("Bound BasicWeatherMonitoring to registry");

			(new Thread(bwm)).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
