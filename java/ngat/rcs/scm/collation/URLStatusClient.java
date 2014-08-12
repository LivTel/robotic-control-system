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

/**
 * Client for gathering status informatioon from an URL using a
 * java.net.URLConnection. The status information is expected to be formed of a
 * single line of text, comma seperated values. Later this restriction may be
 * lifted and a supplied parser/extractor used to get the data from the returned
 * information line(s); The URL can refer to a web page, file or other depending
 * on the protocol used. This can be useful for testing purposes. e.g. you can
 * switch in 'sensor.config' between URL and TCS status grabbers.
 * 
 * e.g. URL = http://somewhere.com/cgi-bin/data/wind.cgi&site=haleakala
 * 
 * URL = file:///home/project/sim-data1/wind.txt
 * 
 * 
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: URLStatusClient.java,v 1.1 2006/12/12 08:30:52 snf Exp snf $
 * <dt><b>Source:</b>
 * <dd>$Source:
 * /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/URLStatusClient.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class URLStatusClient implements StatusMonitorClient {

    /**
     * Revision Control System id string, showing the version of the Class.
     */
    public final static String RCSID = new String("$Id: URLStatusClient.java,v 1.1 2006/12/12 08:30:52 snf Exp snf $");

    /**
     * Revision Control System version string, showing the version of the Class.
     */
    public final static String RCSVERSION = new String("$Revision: 1.1 $");

    /** Default TimeStamp format string. */
    public final static String DEFAULT_TIME_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    /** Delay for testing. */
    public static final long TEST_DELAY = 10000L;

    /** Used to format log output. */
    protected static NumberFormat nf;

    /** Used to format log output. */
    protected static SimpleDateFormat sdf;

    protected static SimpleTimeZone UTC;

    /** Polling interval (millis). */
    protected long pollingInterval;

    /** Reverse map of key positions to keywords. */
    protected Map positions;

    /** Data and description map. */
    protected MappedStatusCategory mapping;

    /** Time Stamp format. */
    protected SimpleDateFormat tsf;

    /** Number of keys expected. */
    protected int countKeys;

    /** Delimiter characters. */
    protected String delims;

    /** True if the current status is valid. */
    protected volatile boolean valid;

    /** True if the network resource is available. */
    protected volatile boolean networkAvailable;

    /** The time the latest network status was updated. */
    protected long networkTimestamp;

    /** The time the latest validity data was updated. */
    protected long validityTimestamp;

    static {
	nf = NumberFormat.getInstance();
	nf.setMaximumFractionDigits(3);
	nf.setMinimumFractionDigits(3);
	UTC = new SimpleTimeZone(0, "UTC");
	sdf = new SimpleDateFormat(DEFAULT_TIME_FORMAT_STRING);
	sdf.setTimeZone(UTC);
    }

    /** Client name. */
    protected String name;

    // Status information.

    /** The URL to connect to. */
    protected URL url;

    /** URL Connection object. */
    protected URLConnection uc;

    /** Create a URLStatusClient. */
    public URLStatusClient() {
	positions = Collections.synchronizedMap(new HashMap());
	mapping = new MappedStatusCategory();
    }

    /** Create a URLStatusClient. */
    public URLStatusClient(String name) {
	this();
	setName(name);
    }

    /**
     * Configure from File.
     * 
     * @param file
     *            File to read configuration from.
     * @throws IOException
     *             If there is a problem opening or reading from the file.
     * @throws IllegalArgumentException
     *             If there is a problem with any parameter.
     */
    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	configure(config);
    }

    /**
     * Configure from properties.
     * 
     * @param config
     *            The configuration properties.
     * @throws IllegalArgumentException
     *             If there is a problem with any parameter.
     */
    public void configure(ConfigurationProperties config) throws IllegalArgumentException {

	// url : URL to connect to.
	//
	// number.keys : Number of positions to parse in data.
	//
	// time.format : Format for time-stamp (ALWAYS the first field read in
	// data).
	//
	// pos.XX.keyword : Keyword for position XX in returned data.
	// pos.XX.type : Type of data One of (I = integer, D = double , S =
	// string).
	// pos.XX.desc : Key description (used in logging output).
	// pos.XX.units : Units name (used in logging output).
	//

	String urlString = config.getProperty("url");
	if (urlString == null)
	    throw new IllegalArgumentException("URLStatusClient: " + name + " No URL specified");
	try {
	    url = new URL(urlString);
	} catch (MalformedURLException mux) {
	    throw new IllegalArgumentException("URLStatusClient: " + name + " Bad URL: " + urlString);
	}

	delims = config.getProperty("delims", ", ");

	tsf = new SimpleDateFormat(config.getProperty("time.format", DEFAULT_TIME_FORMAT_STRING));
	tsf.setTimeZone(UTC);

	// Get any numeric codes.
	Enumeration e = config.propertyNames();
	while (e.hasMoreElements()) {

	    String prop = (String) e.nextElement();

	    if (prop.startsWith("code.")) {

		try {
		    String nos = prop.substring(prop.indexOf(".") + 1);
		    int c = Integer.parseInt(nos);
		    mapping.addCode(c, config.getProperty(prop));
		} catch (NumberFormatException nx) {
		    System.err.println("Error parsing code: " + nx);
		} catch (IndexOutOfBoundsException iobx) {
		    System.err.println("Error parsing code: " + iobx);
		}
	    }

	}

	// We need the mapping between keywords and csv line positions.

	countKeys = config.getIntValue("number.keys", -1);
	if (countKeys == -1)
	    throw new IllegalArgumentException("URLStatusClient: " + name + " No key count specified: ");

	for (int i = 1; i <= countKeys; i++) {

	    String keyword = config.getProperty("pos." + i + ".keyword", null);

	    if (keyword == null)
		throw new IllegalArgumentException("URLStatusClient: " + name + " No keyword at position: " + i);

	    positions.put(new Integer(i), keyword);
	    // keys.put(keyword, new Integer(i));

	    String keyTypeStr = config.getProperty("pos." + i + ".type", null);

	    int keyType = 0;
	    if (keyTypeStr == null)
		throw new IllegalArgumentException("URLStatusClient: " + name + " No type at position: " + i);

	    if (keyTypeStr.equals("D"))
		keyType = MappedStatusCategory.DOUBLE_DATA;
	    else if (keyTypeStr.equals("I"))
		keyType = MappedStatusCategory.INTEGER_DATA;
	    else if (keyTypeStr.equals("S"))
		keyType = MappedStatusCategory.STRING_DATA;
	    else
		throw new IllegalArgumentException("URLStatusClient: " + name + " Illegal type " + keyTypeStr
						   + " at position: " + i);

	    String keyDesc = config.getProperty("pos." + i + ".desc", null);

	    String keyUnits = config.getProperty("pos." + i + ".units", "");

	    mapping.addKeyword(keyword, keyType, keyDesc, keyUnits);

	}

    }

    /** Sets the name for this client. */
    public void setName(String name) {
	this.name = name;
    }

    /** Returns the name of this client. */
    public String getName() {
	return name;
    }

    /** Initialize the client. */
    public void initClient() throws ClientInitializationException {
	try {
	    uc = url.openConnection();
	    // System.err.println("URLStatusClient: "+getName()+" Connection open");

	    uc.setDoInput(true);
	    uc.setAllowUserInteraction(false);
	} catch (IOException iox) {
	    long now = System.currentTimeMillis();
	    networkAvailable = false;
	    networkTimestamp = now;
	    throw new ClientInitializationException("URLStatusClient: " + name + " Error opening URL connection to: "
						    + url);
	}
    }

    /**
     * Requests to grab status from the TCS. We use the JMSMA implementor but
     * invoked from this thread i.e from the calling StatusMonitorThread which
     * will block until we get some sort of reply or timeout.
     */
    public void clientGetStatus() {

	long now = System.currentTimeMillis();

	try {
	    InputStream in = uc.getInputStream();
	    BufferedReader din = new BufferedReader(new InputStreamReader(in));
	    // System.err.println("URLStatusClient: "+getName()+" Input stream open");

	    String line = null;
	    String data = "";
	    while ((line = din.readLine()) != null) {
		// System.err.println("URLStatusClient: "+getName()+" Got: ["+line+"]");
		data = data + line;
	    }

	    try {
		in.close();
	    } catch (Exception e) {
		System.err.println("URLStatusClient: " + getName() + " **Warning error closing URL Connection:" + e);
	    }
	    din.close();

	    // System.err.println("URLStatusClient: "+getName()+" Input stream closed");

	    // Network is up.
	    networkAvailable = true;
	    networkTimestamp = now;

	    // System.err.println("URLStatusClient: "+getName()+" Extract Data ["+data+"]");

	    // Parse/extract the line.
	    try {
		extract(data);
	    } catch (IllegalArgumentException iax) {
		if (System.getProperty("log.url.status.client") != null)
		    System.err.println("URLStatusClient: " + getName() + " Error: " + iax);
		valid = false;
		validityTimestamp = now;
		return;
	    }

	    // Data looks ok.
	    valid = true;
	    validityTimestamp = now;

	} catch (IOException iox) {
	    if (System.getProperty("log.url.status.client") != null)
		System.err.println("URLStatusClient: " + getName() + " Error: " + iox);
	    valid = false;
	    validityTimestamp = now;
	    networkAvailable = false;
	    networkTimestamp = now;
	}

    }

    /** Returns true if the current status is valid. */
    public boolean isStatusValid() {
	return valid;
    }

    /** Returns true if the network resource is available. */
    public boolean isNetworkAvailable() {
	return networkAvailable;
    }

    /** Returns the time the latest network status was updated. */
    public long getNetworkTimestamp() {
	return networkTimestamp;
    }

    /** Returns the time the latest validity data was updated. */
    public long getValidityTimestamp() {
	return validityTimestamp;
    }

    /**
     * Subclases may override to extract appropriate data to generate the
     * current status.
     */
    protected void extract(String line) throws IllegalArgumentException {

	// System.err.println("Starting extraction..");

	StringTokenizer st = new StringTokenizer(line, delims);

	if (st.countTokens() < (countKeys + 1))
	    throw new IllegalArgumentException("Missing tokens in data: Found " + st.countTokens() + ", expected "
					       + (countKeys + 1));

	// System.err.println("X:Tokens: "+st.countTokens());

	MappedStatusCategory.MapEntry entry = null;

	String tsu = st.nextToken();

	try {
	    Date td = tsf.parse(tsu);
	    mapping.setTimeStamp(td.getTime());
	} catch (ParseException px) {
	    throw new IllegalArgumentException("First token is not a valid time-stamp");
	}

	// Place the Data in the Mapping.
	for (int i = 1; i <= countKeys; i++) {

	    String item = st.nextToken();
	    // System.err.println("X:Token: "+item);

	    String keyword = (String) positions.get(new Integer(i));
	    // System.err.println("X:Keyword: "+keyword);

	    entry = mapping.getMapEntry(keyword);
	    // System.err.println("X:Entry: "+entry);

	    if (entry == null)
		throw new IllegalArgumentException("No mapping entry defined for position: " + i + " !");

	    switch (entry.type) {
	    case MappedStatusCategory.INTEGER_DATA:
		mapping.addData(keyword, new Integer(item));
		break;
	    case MappedStatusCategory.DOUBLE_DATA:
		mapping.addData(keyword, new Double(item));
		break;
	    case MappedStatusCategory.STRING_DATA:
		mapping.addData(keyword, item);
		break;

	    }

	}

    }

    // /** @return status identified by the supplied key or throw
    // * an IllegalArgumentException if no such status exists. This method is
    // * intended for descriptive (String) status variables.
    // */
    // public String getStatusEntryId(String key) throws
    // IllegalArgumentException {
    // return mapping.getStatusEntryId(key);
    // }

    // /** @return status identified by the supplied key or throw
    // * an IllegalArgumentException if no such status exists. This method is
    // * intended for continuous status variables.
    // */
    // public int getStatusEntryInt(String key) throws IllegalArgumentException
    // {
    // return mapping.getStatusEntryInt(key);
    // }

    // /** @return status identified by the supplied key or throw
    // * an IllegalArgumentException if no such status exists. This method is
    // * intended for discrete status variables.
    // */
    // public double getStatusEntryDouble(String key) throws
    // IllegalArgumentException {
    // return mapping.getStatusEntryDouble(key);
    // }

    // /** @return status identified by the supplied key or throw
    // * an IllegalArgumentException if no such status exists. This method
    // * can be used for any type of variable and does not do type conversion.
    // */
    // public String getStatusEntryRaw(String key) throws
    // IllegalArgumentException {
    // return mapping.getStatusEntryRaw(key);
    // }

    // /** @return the timestamp of the latest readings.*/
    // public long getTimeStamp() {
    // return mapping.getTimeStamp();
    // }

    /** Returns a serializable StatusCategory. */
    public StatusCategory getStatus() {
	return mapping;
    }

    /** Returns a readable String representation. */
    @Override
	public String toString() {
	StringBuffer buffer = new StringBuffer("URLStatusClient: " + name + "(");
	buffer.append("URL=" + url);
	buffer.append(", Data=[" + mapping + "]");
	buffer.append(")");
	return buffer.toString();
    }

    /** Create and test a URLStatusClient. */
    public static void main(String[] args) {

	try {

	    File file = new File(args[0]);

	    URLStatusClient usc = new URLStatusClient(args[1]);

	    usc.configure(file);

	    while (true) {

		try {
		    Thread.sleep(TEST_DELAY);
		} catch (InterruptedException ix) {
		}

		usc.initClient();

		usc.clientGetStatus();

		System.err.println(usc.getStatus());

	    }

	} catch (Exception e) {
	    System.err.println("Error: " + e);
	    return;
	}

    }

}

/**
 * $Log: URLStatusClient.java,v $ /** Revision 1.1 2006/12/12 08:30:52 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:34:57 snf /** Initial
 * revision /** /** Revision 1.2 2004/10/13 06:50:00 snf /** Changed the
 * revision variable content. /** /** Revision 1.1 2004/10/13 06:49:18 snf /**
 * Initial revision /**
 */
