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

import java.io.*;

import ngat.net.*;
import ngat.util.*;
import ngat.message.RCS_TCS.*;
import ngat.message.base.*;

/** 
 * Status grabber client for extracting status from the TCS.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TCSStatusClient.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/TCSStatusClient.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TCSStatusClient implements JMSMA_Client, StatusMonitorClient {

    /** Default timeout for command.*/
    public static final long DEFAULT_TIMEOUT = 30000L;

    /** Command timeout (millis).*/
    protected long timeout;

    /** Holds the comand object.*/
    protected SHOW command;

    /** Name for this client.*/
    protected String name;

    /** The current status value.*/
    protected TCS_Status.Segment status;

    /** True if the current status is valid.*/
    protected volatile boolean valid; 

    /** True if the network resource is available.*/
    protected volatile boolean networkAvailable;

    /** The time the latest network status was updated.*/
    protected long networkTimestamp;

    /** The time the latest validity data was updated.*/
    protected long validityTimestamp;
    
    /** The SHOW status segment key.*/
    protected int key;

    /** Connection factory.*/
    protected static ConnectionFactory connectionFactory;

    /** Network connection resource ID.*/
    protected String networkConnectionId;

    /** Connection.*/
    protected IConnection connection;

    /** Create a TCSStatusClient. Null constructor for
     * conformity with SCM reflection instantiation policy.
     * Use setName() to set the name after construction.
     */
    public TCSStatusClient() {}

    /** Create a TCSStatusClient with given name.*/
    public TCSStatusClient(String name) {	
	this.name = name;	
    }
    
    /** Configure from File.
     * @param file File to read configuration from.
     * @throws IOException If there is a problem opening or reading from the file.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	configure(config);
    }
    
    /** Configure from properties.
     * @param config The configuration properties.
     * @throws IllegalArgumentException If there is a problem with any parameter.
     */
    public void configure(ConfigurationProperties config) throws IllegalArgumentException {
	String keyName = config.getProperty("show.key");
	if (keyName == null)
	    throw new IllegalArgumentException("TCSStatusClient:"+name+" : SHOW key: Not specified");
	
	if 
	    (keyName.equals("ASTROMETRY"))
	    key = SHOW.ASTROMETRY;
	else if 
	    (keyName.equalsIgnoreCase("AUTOGUIDER"))
	    key = SHOW.AUTOGUIDER;
	else if 
	    (keyName.equalsIgnoreCase("CALIBRATE"))
	    key = SHOW.CALIBRATE;
	else if 
	    (keyName.equalsIgnoreCase("FOCUS"))
	    key = SHOW.FOCUS;
	else if 
	    (keyName.equalsIgnoreCase("LIMITS"))
	    key = SHOW.LIMITS;
	else if 
	    (keyName.equalsIgnoreCase("MECHANISMS"))
	    key = SHOW.MECHANISMS;
	else if 
	    (keyName.equalsIgnoreCase("METEOROLOGY"))
	    key = SHOW.METEOROLOGY;
	else if 
	    (keyName.equalsIgnoreCase("SOURCE"))
	    key = SHOW.SOURCE;
	else if 
	    (keyName.equalsIgnoreCase("STATE"))
	    key = SHOW.STATE;
	else if 
	    (keyName.equalsIgnoreCase("TIME"))
	    key = SHOW.TIME;
	else if 
	    (keyName.equalsIgnoreCase("VERSION"))
	    key = SHOW.VERSION;
	else
	    throw new IllegalArgumentException("TCSStatusClient: "+name+" : SHOW key: unknown key: "+keyName);

	timeout = config.getLongValue("timeout", DEFAULT_TIMEOUT);

	networkConnectionId = config.getProperty("network.connection.id");

	if (networkConnectionId == null)
	    throw new IllegalArgumentException("TCSStatusClient: "+name+" : Network connection ID not specified"); 

    }

    /** Set the connection factory.*/
    public static void setConnectionFactory(ConnectionFactory cf) {
	connectionFactory = cf;
    }

    /** Sets the name for this client.*/
    public void setName(String name) {
	this.name = name;
    }

    /** Returns the name.*/
    public String getName() {
	return name;
    }

    
    /** Initialize the client.*/
    public void initClient()  throws ClientInitializationException {	
	command = new SHOW( "show-"+key);
	command.setKey(key);

	//System.err.println("TCSStatusClient: "+name+" initializing with command: "+command);
	long now = System.currentTimeMillis();
	try {
	    connection = connectionFactory.createConnection(networkConnectionId);
	    // System.err.println("TCSStatusClient: "+name+" Connection ready: "+connection);
	} catch (UnknownResourceException urx) { 
	    networkAvailable = false;	
	    networkTimestamp  = now;  
	    throw new ClientInitializationException("TCSStatusClient: "+name+
						    " : Network connectionID: "+networkConnectionId+" was not valid: ");
	}
	
    }
    
    /** Requests to grab status from the TCS.
     * We use the JMSMA implementor but invoked from this thread i.e
     * from the calling StatusMonitorThread which will block until
     * we get some sort of reply or timeout.
     */
    public void clientGetStatus() {
	
	JMSMA_ProtocolClientImpl implementor = new JMSMA_ProtocolClientImpl(this, connection);

	//System.err.println("TCSStatusClient: "+name+" Ready to implement JMSMA protocol");

	implementor.implement();

	implementor = null;
	
	
    }
    
    /** Returns true if the current status is valid.*/
    public boolean isStatusValid() { return valid;}
    
    /** Returns true if the network resource is available.*/
    public boolean isNetworkAvailable() { return networkAvailable; }

    /** Returns the time the latest network status was updated.*/
    public long getNetworkTimestamp() {
	return networkTimestamp;
    }

    /** Returns the time the latest validity data was updated.*/
    public long getValidityTimestamp() {
	return validityTimestamp;
    }

    /** Returns the pre-built command object.*/
    public COMMAND getCommand() { return command;}

    /** Returns the timeout period for the command.*/
    public long getTimeout() { return timeout; }

    /** Handle any ACKs.*/
    public void handleAck(ACK ack) {	
	
    }

    /** Handle the response. 
     */
    public void handleDone(COMMAND_DONE done) {
	//System.err.println("TCSStatusClient: "+name+" Response: "+done+
	//	   "\n Success="+done.getSuccessful()+
	//	   "\n Errcode="+done.getErrorNum()+
	//	   "\n Message="+done.getErrorString());
	
	long now = System.currentTimeMillis(); 
	if (!done.getSuccessful()) {	   
	    valid = false;
	    // This next is dubious - infact we have got first stage of network connection
	    // to the CIL proxy, just not to the TCS !
	    networkAvailable = false;	
	    networkTimestamp  = now;   
	    validityTimestamp = now;
	    return;
	}

	if (! (done instanceof SHOW_DONE)) {
	    valid = false;
	    validityTimestamp = now;
	    networkAvailable = true; 
	    networkTimestamp  = now;   	  
	    return;
	}
	
	status = ((SHOW_DONE)done).getStatus();

	//System.err.println("TCSStatusClient: "+name+" Setting status: "+status);

	valid = true;
	networkAvailable = true;

	networkTimestamp  = now;   
	validityTimestamp = now;
	
    }
    
    /** Handle failure to connect.*/
    public void failedConnect(Exception e) {
	networkAvailable = false;
    }
    
    /** handle failure to despatch.*/
    public void failedDespatch(Exception e) {
	networkAvailable = false;
    }
    
    /** Handle failure to receive ACK.*/
    public void failedAck(Exception e) {
	networkAvailable = false;
    }
    
    /** Handle failure to receive Response - usually a timeout.*/
    public void failedResponse(Exception e) {
	networkAvailable = false;
    }
    
    /** Overridden for conformity with ngat.net.Client - does nothing.*/
    public void sendCommand(COMMAND command) {}

    /** Overridden for conformity with ngat.net.Client - does nothing.*/
    public void despatchRequest() {}

    /** Overridden for conformity with ngat.net.Client - does nothing.*/
    public void exec() {}

    /** Handle general failure of the client execution thread.*/
    public void exceptionOccurred(Object source, Exception e) {	
	e.printStackTrace(System.err);
    }
  
    /** Returns the Status entry.*/
    public StatusCategory getStatus() { return status; }

    /** Returns a readable description.*/
    @Override
	public String toString() {
	return "TCSStatusClient: "+name+" : Status="+status;
    }


}

/** $Log: TCSStatusClient.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.1  2004/02/02 14:10:37  snf
/** Initial revision
/** */
