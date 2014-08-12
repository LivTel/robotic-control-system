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
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

/**  Pool for gathering status informatioon from an URL using a
 * java.net.URLConnection. The status information is expected to be
 * formed of a single line of text, comma seperated values. Later this
 * restriction may be lifted and a supplied parser/extractor used to
 * get the data from the returned information line(s);
 * The URL can refer to a web page, file or other depending on the
 * protocol used. This can be useful for testing purposes. e.g.
 * you can switch in 'sensor.config' between URL and SMM status
 * grabbers.
 *
 * e.g.   URL = http://somewhere.com/cgi-bin/data/wind.cgi&site=haleakala
 *
 *        URL = file:///home/project/sim-data1/wind.txt
 *
 *
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: URLStatusGrabber.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/URLStatusGrabber.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public abstract class URLStatusGrabber extends ControlThread implements StatusCategory, Serializable {
   
    /**
     * Revision Control System id string, showing the version of the Class.
     */
    public final static String RCSID = new String("$Id: URLStatusGrabber.java,v 1.1 2006/12/12 08:30:52 snf Exp $");
	
    /**
     * Revision Control System version string, showing the version of the Class.
     */
    public final static String RCSVERSION = new String("$Version$");


    public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    /** Used to format log output.*/
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	
    /** Used to format log output.*/
    protected static NumberFormat nf;

    /** Polling interval (millis).*/
    protected long pollingInterval;


    static { 
	nf = NumberFormat.getInstance();
	nf.setMaximumFractionDigits(3);
	nf.setMinimumFractionDigits(3);
	sdf.setTimeZone(UTC);
    }

    // Status information.
    
    /** The time the current data was obtained.*/
    protected long timeStamp;

    /** The URL to connect to.*/
    protected URL url;

    /** URL Connection object.*/
    protected transient URLConnection uc;

    /** Create a URLStatusGrabber using the supplied URL and parameters.String name
     * @param name Name for this grabber. 
     * @param urlString The URL to grab status from.
     * @throws MalformedURLException if the URL is not valid.
     */ 
    public URLStatusGrabber(String name, String urlString ) throws MalformedURLException {
	super(name, true);
	
	url = new URL(urlString);
	
    }


    /** Performs initialization operations (?WHAT?).*/
    @Override
	public void initialise() {}
    
    /** Make connection each pollingInterval and grab and extract status.*/
    @Override
	public void mainTask() {
	
	try { Thread.sleep(pollingInterval);} catch (InterruptedException e){}

	try {
	   
	    uc = url.openConnection();
	    //System.err.println("URLStatusGrabber: "+getName()+" Connection open");
	
	    uc.setDoInput(true);
	    uc.setAllowUserInteraction(false);
	   
	    DataInputStream dis = new DataInputStream(uc.getInputStream()); 
	    //System.err.println("URLStatusGrabber: "+getName()+" Input stream open");
	    
	    String line = null;
	    String data = "";
	    while ((line = dis.readLine()) != null) {
		//System.err.println("URLStatusGrabber: "+getName()+" Got: ["+line+"]");
		data = data+line;
	    }     
	    
	    dis.close();
	    //System.err.println("URLStatusGrabber: "+getName()+" Input stream closed");	

	    //System.err.println("URLStatusGrabber: "+getName()+" Extract Data ["+data+"]");
	    // Parse/extract the line.
	    extract(data);

	} catch (IOException iox) {
	    //System.err.println("URLStatusGrabber: "+getName()+" Error: "+iox);
	}

    }

    /** Performs shutdown operations (?WHAT?).*/    
    @Override
	public void shutdown() {
	url = null;
    }

    /** Set the polling interval (millis).*/
    public void setPollingInterval(long pollingInterval) { 
	this.pollingInterval= pollingInterval; 
    }

    /** Returns the polling interval (millis).*/
    public long getPollingInterval() { return pollingInterval; }

    /** @return the timestamp of the latest readings.*/
    public long getTimeStamp() {
	return timeStamp;
    }
    
    /** Subclases should override to extract appropriate data to generate
     * the current status.*/
    protected abstract void extract(String line);
    

    /** Returns a readable String representation.*/
    @Override
	public String toString() {
	return "URLStatusGrabber: "+getName()+" on url"+url+", polling="+(pollingInterval/1000)+" secs";
    }

}

/** $Log: URLStatusGrabber.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */
