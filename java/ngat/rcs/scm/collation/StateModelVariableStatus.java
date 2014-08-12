package ngat.rcs.scm.collation;

import java.util.Map;

import ngat.util.SerializableStatusCategory;

public class StateModelVariableStatus implements SerializableStatusCategory {

    public static final long serialVersionUID = 703414316739444401L;

    /** Status time stamp.*/
   long timeStamp;

   /** Stores the status key/value pairs.*/
   Map map;

   private int weatherState = -99;
   
    //map.put("SYSTEM",   new Integer(system));
    // map.put("AXES",     new Integer(axes));
    //map.put("WEATHER",  new Integer(weather));
    //map.put("ENCLOSURE",new Integer(enclosure));
    //map.put("CONTROL",  new Integer(control));
    //map.put("NETWORK",  new Integer(network));
    //map.put("INTENT",   new Integer(intent));
    //map.put("PERIOD",   new Integer(tod));
    //map.put("SHUTDOWN", new Integer(shutdown));
    //map.put("MIRRCOVER", new Integer(mirrcover));
    //map.put("STABILITY", new Integer(stability));

   
/**
 * @param hash
 */
public StateModelVariableStatus(Map map) {
	super();
	this.map = map;

	try{
		weatherState = ((Integer)map.get("WEATHER")).intValue();
	} catch (Exception e) {
		e.printStackTrace();
	}

	
	
}

    /**
     * @return the named state variable.
     */
    public int getVariable(String name) throws Exception {
	if (!map.containsKey(name))
	    throw new IllegalArgumentException("Unknown key: "+name);
	int state = ((Integer)map.get(name)).intValue();
	return state;
    }


/**
 * @return the weatherState
 */
public int getWeatherState() {
	return weatherState;
}





/**
 * @param weatherState the weatherState to set
 */
public void setWeatherState(int weatherState) {
	this.weatherState = weatherState;
}





/**
 * @param timeStamp the timeStamp to set
 */
public void setTimeStamp(long timeStamp) {
	this.timeStamp = timeStamp;
}


public Map getMap() {
	return map;
}


public double getStatusEntryDouble(String arg0) throws IllegalArgumentException {
	// TODO Auto-generated method stub
	return 0;
}

public String getStatusEntryId(String arg0) throws IllegalArgumentException {
	// TODO Auto-generated method stub
	return null;
}

public int getStatusEntryInt(String arg0) throws IllegalArgumentException {
	// TODO Auto-generated method stub
	return 0;
}

public String getStatusEntryRaw(String arg0) throws IllegalArgumentException {
	// TODO Auto-generated method stub
	return null;
}

public long getTimeStamp() {
	return timeStamp;
}

   
   
}
