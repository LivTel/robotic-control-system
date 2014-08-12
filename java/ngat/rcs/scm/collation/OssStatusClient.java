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
import java.rmi.Naming;
import ngat.oss.transport.RemotelyPingable;
import ngat.util.*;
import ngat.message.base.*;

/**
 * Status grabber client for extracting status from instruments.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id$
 * <dt><b>Source:</b>
 * <dd>$Source$
 * </dl>
 * 
 * @author $Author$
 * @version $Revision$
 */
public class OssStatusClient implements StatusMonitorClient {

	/** Default timeout for command. */
	public static final long DEFAULT_TIMEOUT = 30000L;

	/** Default status level. */
	// public static final int DEFAULT_LEVEL = GET_STATUS.LEVEL_INTERMEDIATE;

	/** Command timeout (millis). */
	protected long timeout;

	/** Name for this client. */
	protected String name;

	/** ICS Status wrapper. */
	protected OssStatus status;

	/** Status level. */
	protected int level;

	/** True if the current status is valid. */
	protected volatile boolean valid;

	/** True if the network resource is available. */
	protected volatile boolean networkAvailable;

	/** The time the latest network status was updated. */
	protected long networkTimestamp;

	/** The time the latest validity data was updated. */
	protected long validityTimestamp;

	/** Network connection resource ID. */
	protected String networkConnectionId;
	
	private String schedulerUrl;
	private String baseModelUrlBase;
	private String synopticModelUrl;

	/**
	 * Create an OssStatusClient. Null constructor for conformity with SCM
	 * reflection instantiation policy. Use setName() to set the name after
	 * construction.
	 */
	public OssStatusClient() {
		status = new OssStatus();
	}

	/**
	 * Create an OssStatusClient. We use JMSMA as the protocol implementor.
	 */
	public OssStatusClient(String name) {
		this();
		this.name = name;
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
		// TODO need hosts and names for each service
		schedulerUrl     = config.getProperty("scheduler.url", "rmi://spare.lt.com/Scheduler");
		baseModelUrlBase = config.getProperty("base.model.url", "rmi://oss.lt.com");
		synopticModelUrl = config.getProperty("synoptic.model.url", "rmi://spare.lt.com/SynopticModelProvider");
		
		// level = config.getIntValue("level", DEFAULT_LEVEL);
		//System.err.println("OSS STATUS CLIENT: Configured");
	}

	/** Sets the name for this client. */
	public void setName(String name) {
		this.name = name;
	}

	/** Returns the name. */
	public String getName() {
		return name;
	}

	/** Initialize the client. */
	public void initClient() throws ClientInitializationException {
		
		long now = System.currentTimeMillis();

	}

	/**
	 * Requests to grab status from the ICS. We use the JMSMA implementor but
	 * invoked from this thread i.e from the calling StatusMonitorThread which
	 * will block until we get some sort of reply or timeout.
	 */
	public void clientGetStatus() {
		long now = System.currentTimeMillis();
		valid = true;
		validityTimestamp = now;
		networkAvailable = true;
		status.setTimeStamp(now);
		
		try {
			RemotelyPingable pinger = (RemotelyPingable)Naming.lookup(schedulerUrl);
			pinger.ping();
			status.setSchedulerOnline(true);
			//System.err.println("OssStatusClient: " + name + " Scheduler is pingable");
		} catch (Exception e){
			//System.err.println("OssStatusClient: " + name + " Scheduler is not bound at: "+schedulerUrl);
			e.printStackTrace();
		}
		boolean p2online = false;
		String p2Url = baseModelUrlBase+"/Phase2Model";
		try {
			RemotelyPingable pinger = (RemotelyPingable)Naming.lookup(p2Url);
			pinger.ping();
			//System.err.println("OssStatusClient: " + name + " Phase2Model is pingable");
			p2online = true;
		} catch (Exception e){
			//System.err.println("OssStatusClient: " + name + " Phase2Model is not bound at: "+p2Url);
			e.printStackTrace();
		}
		
		boolean accOnline = false;
		String accUrl = baseModelUrlBase+"/ProposalAccountModel";
		try {
			RemotelyPingable pinger = (RemotelyPingable)Naming.lookup(accUrl);
			pinger.ping();
			//System.err.println("OssStatusClient: " + name + " AccModel is pingable");
			accOnline = true;
		} catch (Exception e){
			//System.err.println("OssStatusClient: " + name + " AccModel is not bound at: "+accUrl);
			e.printStackTrace();
		}
		
		boolean histOnline = false;
		String histUrl = baseModelUrlBase+"/HistoryModel";
		try {
			RemotelyPingable pinger = (RemotelyPingable)Naming.lookup(histUrl);
			pinger.ping();
			//System.err.println("OssStatusClient: " + name + " HistModel is pingable");
			histOnline = true;
		} catch (Exception e){
			//System.err.println("OssStatusClient: " + name + " HistModel is not bound at: "+histUrl);
			e.printStackTrace();
		}
		if (p2online && accOnline && histOnline) {
			status.setBaseModelsOnline(true);
		}
		
		try {
			RemotelyPingable pinger = (RemotelyPingable)Naming.lookup(synopticModelUrl);
			pinger.ping();
			status.setSynopticModelsOnline(true);
			//System.err.println("OssStatusClient: " + name + " SynopticModel is pingable");
		} catch (Exception e){
			//System.err.println("OssStatusClient: " + name + " SynopticModels not bound at: "+synopticModelUrl);
			e.printStackTrace();
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

	/** Returns the timeout period for the command. */
	public long getTimeout() {
		return timeout;
	}

	/** Handle any ACKs. */
	public void handleAck(ACK ack) {

	}

	/** Returns the time the latest validity data was updated. */
	public long getValidityTimestamp() {
		return System.currentTimeMillis();
	}

	/** Returns the time the latest network status was updated. */
	public long getNetworkTimestamp() {
		return System.currentTimeMillis();
	}

	/** Returns the Status entry. */
	public StatusCategory getStatus() {
		return status;
	}

	/** Returns a readable description. */
	@Override
	public String toString() {
		return "OssStatusClient: " + name + " : Status=" + status;
	}

}

/** $Log$ */
