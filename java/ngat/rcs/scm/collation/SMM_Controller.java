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
package ngat.rcs.scm.collation;

import ngat.rcs.*;

import ngat.rcs.emm.*;

import ngat.tcm.BasicTelescope;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.ems.DefaultMutableSkyModel;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.io.*;

/** Control class for the Status Monitoring Module (SMM).
 * Provides general information point and methods for controlling
 * the various status monitor threads.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: SMM_Controller.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/SMM_Controller.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class SMM_Controller implements EventSubscriber {

    protected static final String CLASS = "SMM_Controller";
    
    protected Map monitors;
    
    protected static SMM_Controller controller;

    protected static Logger logger;

    private SMM_Controller() {
	monitors = Collections.synchronizedMap(new HashMap());
	if (logger != null)
	    logger = LogManager.getLogger(this);
    }
    
    public static SMM_Controller getController() {
	if (controller == null)
	    controller = new SMM_Controller();
	return controller;
    } 

    public static void setLogger(String name) {
	logger = LogManager.getLogger(name);
    }

    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));

	String  prop     = "";
	String  name     = "";
	String  keyword  = "";
	boolean enabled  = false;
	long    interval = 0L;
	long    timeout  = 0L;
	String  desc     = "";

	Enumeration e = config.propertyNames();
	while (e.hasMoreElements()) {
	    prop = (String)e.nextElement();
	    
	    if (prop.indexOf(".ID") != -1) {
		// This is how we refer to it in config file.
		name     = prop.substring(0, prop.indexOf(".ID"));
		System.err.println("Searching for:["+name+"] and additions.");
		keyword  = config.getProperty(prop);
		enabled  = config.getBooleanValue(name+".enabled", false);
		interval = config.getLongValue(name+".interval", SMM_MonitorThread.DEFAULT_INTERVAL);
		timeout  = config.getLongValue(name+".timeout",  SMM_MonitorThread.DEFAULT_TIMEOUT);
		desc     = config.getProperty("desc");

		int key  = keyfor(keyword);
		if (key == -1)
		    throw new IllegalArgumentException("Unknown Status Monitor key: "+keyword);
		
		SMM_MonitorThread monitor = new SMM_MonitorThread(key, interval);
		monitor.setDesc(desc);
		monitor.setEnabled(enabled);
		monitors.put(keyword, monitor);
	    }
	    
	}

	
    }
    
    public void start(int key) {
	start(nameof(key));
    }
    
    public void start(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	if (monitor.isEnabled())
	    monitor.start();
	logger.log(1, CLASS, "-", "init", "TCS_["+id+"]_MonitorThread started: Updating at: "+
		   (monitor.getInterval()/1000)+" secs.");
    }
    
    public void startAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    start((String)it.next());
	}
	
	// special FUDGE
	/*System.err.println("Special FUDGE for Sky data via RCS live status feed...");
	try {
		// NOTE back and forth references - eeek!!
		DefaultMutableSkyModel skyModel = (DefaultMutableSkyModel)RCS_Controller.controller.getSkyModel();
		SkyModelProvider smp = new SkyModelProvider(skyModel);
		skyModel.addSkyModelUpdateListener(smp);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("SKY", smp);
	} catch (Exception e) {
		e.printStackTrace();
	}*/
	
	System.err.println("Special FUDGE for AGtemp data via RCS live status feed...");
	try {
		
		BasicTelescope scope = (BasicTelescope)RCS_Controller.controller.getTelescope();
		AgActiveProvider amp = new AgActiveProvider();
		scope.addTelescopeStatusUpdateListener(amp);
		LegacyStatusProviderRegistry.getInstance().addStatusCategory("AGACTIVE", amp);
		
	} catch (Exception e) {
		e.printStackTrace();
	}
	
	
    }

    public void stop(int key) {
	stop(nameof(key));
    }

    public void stop(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.terminate();
    }
    
    public void stopAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    stop((String)it.next());
	}
    }
    
    public void pause(int key) {
	pause(nameof(key));
    }
    
    public void pause(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.linger();
    }
    
    public void pause(int key, long timeout) {
	pause(nameof(key), timeout);
    }
    
    public void pause(String id, long timeout) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.linger(timeout);
    }
    
    public void pauseAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    pause((String)it.next());
	}
    }
    
    public void pauseAll(long timeout) {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    pause((String)it.next(), timeout);
	}
    }
    
    public void resume(int key) {
	resume(nameof(key));
    }
    
    public void resume(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.awaken();
    }

    public void resumeAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    resume((String)it.next());
	}
    }
    
    public void backoff(int key) {
	backoff(nameof(key));
    }
    
    public void backoff(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.backoff();
    }
    
    public void backoffAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    backoff((String)it.next());
	}
    }

    public void normal(int key) {
	 normal(nameof(key));
    }

    public void normal(String id) {
	SMM_MonitorThread monitor = (SMM_MonitorThread)monitors.get(id);
	if (monitor == null) return;
	
	monitor.normal();
    }
    
    public void normalAll() {
	Iterator it = monitors.keySet().iterator();
	while (it.hasNext()) {
	    normal((String)it.next());
	}
    }
    

    /** Returns the String name for a specified key.
     * @param key The (SHOW) key for which the name is to be returned.
     */
    public static String nameof(int key) {
	switch (key) {
	case SHOW.ASTROMETRY:
	    return "ASTROMETRY";	  
	case SHOW.AUTOGUIDER:
	    return "AUTOGUIDER";  	  
	case SHOW.CALIBRATE: 
	    return "CALIBRATE";   	   
	case SHOW.FOCUS:
	    return "FOCUS";   	  
	case SHOW.LIMITS:
	    return "LIMITS";   	  
	case SHOW.MECHANISMS:
	    return "MECHANISMS";   	  
	case SHOW.METEOROLOGY:
	    return "METEOROLOGY";   	   
	case SHOW.SOURCE:
	    return "SOURCE";   	   
	case SHOW.STATE:
	    return "STATE";   	  
	case SHOW.TIME:
	    return "TIME";  	  
	case SHOW.VERSION:
	    return "VERSION";  	  
	default:
	    return "UNKNOWN";
	}
    }

    /** Returns the key for a given name.
     * @param name The name to get the key for.*/
    public static int keyfor(String name) {

	if (name.equals("ASTROMETRY"))
	    return SHOW.ASTROMETRY;	  
	else if (name.equals("AUTOGUIDER"))
	    return SHOW.AUTOGUIDER;  	  
	else if (name.equals("CALIBRATE")) 
	    return SHOW.CALIBRATE;   	   
	else if (name.equals("FOCUS"))
	    return SHOW.FOCUS;   	  
	else if (name.equals("LIMITS"))
	    return SHOW.LIMITS;   	  
	else if (name.equals("MECHANISMS"))
	    return SHOW.MECHANISMS;   	  
	else if (name.equals("METEOROLOGY"))
	    return SHOW.METEOROLOGY;   	   
	else if (name.equals("SOURCE"))
	    return SHOW.SOURCE;   	   
	else if (name.equals("STATE"))
	    return SHOW.STATE;   	  
	else if (name.equals("TIME"))
	    return SHOW.TIME;  	  
	else if (name.equals("VERSION"))
	    return SHOW.VERSION;		
        else
	    return -1;
    }
    
    /** Returns the SMM_MonitorThread registered against the specified (SHOW) key.
     * If no such monitor is currently registered - one is created and registered
     * against the specified key using the DEFAULT_INTERVAL.
     * @param key The (SHOW) key to register against.
     * @return An existing or new StatusMonitor registered against key.
     */
    public SMM_MonitorThread getMonitor(int key) {
	return getMonitor(nameof(key));
    }
    
    /** Returns the SMM_MonitorThread registered against the specified (SHOW) key.
     * If no such monitor is currently registered - one is created and registered
     * against the specified key using the DEFAULT_INTERVAL.
     * @param id The (SHOW) key, translated to keyword, to register against.
     * @return An existing or new StatusMonitor registered against key.
     */
    public SMM_MonitorThread getMonitor(String id) {
	if (monitors.containsKey(id))
	    return (SMM_MonitorThread)monitors.get(id);
	// None found - make one.
	SMM_MonitorThread monitor = new SMM_MonitorThread(keyfor(id),  SMM_MonitorThread.DEFAULT_INTERVAL);
	monitors.put(id, monitor);
	return monitor;   
    }
    
    /** Returns the SMM_MonitorThread registered against the specified (SHOW) key.
     * If no such monitor is currently registered - one is created and registered
     * against the specified key using the DEFAULT_INTERVAL.
     * @param key The (SHOW) key to register against.
     * @return An existing or new StatusMonitor registered against key.
     */
    public static SMM_MonitorThread findMonitor(int key) {
	return controller.getMonitor(key);
    }

    /** Handle Event notifications.*/
    public void notifyEvent(String eventTopic, Object data) {
	if (eventTopic.equals("NETWORK_COMMS_ALERT")) {
	    logger.log(3, CLASS, "-", "notify",
		       "Backing off monitors on: "+eventTopic);
	    //backoffAll();
	} else if
	    (eventTopic.equals("NETWORK_COMMS_CLEAR")) { 
	    logger.log(3, CLASS, "-", "notify",
		       "Restoring monitors on: "+eventTopic);
	    //normalAll();
	}
    }

    /** EventSubscriber method.
     */
    public String getSubscriberId() { return CLASS; } 

}

/** $Log: SMM_Controller.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
