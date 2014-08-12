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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import ngat.net.*;
import ngat.phase2.*;
import ngat.instrument.*;
import ngat.phase2.nonpersist.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;
import ngat.message.POS_RCS.*;
import ngat.rcs.gui.*;

import java.lang.reflect.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.io.*;

/** This Task creates a series of subTasks to carry out the
 * Planetarium Operations plan .
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: PlanetariumControlAgent.java,v 1.2 2007/07/05 11:33:19 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/PlanetariumControlAgent.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class PlanetariumControlAgent extends DefaultModalTask implements Logging {

    protected static final String CLASS = "PlanetariumMCA";

    SimpleDateFormat tdf = new SimpleDateFormat("HH:mm z");
    
    /** ERROR_BASE for this Task type.*/
    public static final int ERROR_BASE = 300;

    public static String DEFAULT_RELAY_HOST = "localhost";
    
    public static int    DEFAULT_RELAY_PORT = 8473;

    public static String DEFAULT_SERVERPC_HOST = "localhost";

    public static int    DEFAULT_SERVERPC_PORT = 6677;

    public static int    DEFAULT_BANDWIDTH  = 50;

    public static int    DEFAULT_BUFFER_LENGTH = 4096;

    public static String DEFAULT_IMAGE_BASE_DIR = "planetarium/images";

    public static String DEFAULT_INSTRUMENT           = "RATCAM";

    public static String DEFAULT_INSTRUMENT_CONFIG_ID = "POS_DEFAULT_INST_CONFIG";
    
    public static String CCD_DEFAULT_LOWER_FILTER = "";
    
    public static String CCD_DEFAULT_UPPER_FILTER = "";

    public static int    CCD_DEFAULT_XBIN = 1;
    
    public static int    CCD_DEFAULT_YBIN = 1;

    public static String DEFAULT_TELESCOPE_CONFIG_ID  = "POS_DEFAULT_TELESCOPE_CONFIG";

    public static String AG_DEFAULT_STAR_SELECTION_MODE_STR = "STAR_SELECTION_RANK";

    public static int    AG_DEFAULT_STAR_SELECTION_MODE = TelescopeConfig.STAR_SELECTION_RANK;
    
    public static int    AG_DEFAULT_STAR_RANK_1 = 0;

    public static int    AG_DEFAULT_STAR_RANK_2 = 0;	   

    public static int    AG_DEFAULT_STAR_RANGE_1 = 15;

    public static int    AG_DEFAULT_STAR_RANGE_2 = 20;
    
    public static int    AG_DEFAULT_STAR_PIXEL_X = 0;

    public static int    AG_DEFAULT_STAR_PIXEL_Y = 0;
    
    public static String AG_DEFAULT_USAGE_MODE_STR = "AGMODE_MANDATORY";
    
    public static int    AG_DEFAULT_USAGE_MODE = TelescopeConfig.AGMODE_MANDATORY;
	
    public static float  DEFAULT_FOCUS_OFFSET = 0.0f;
 
    public static String DEFAULT_ROTATOR_ANGLE_MODE_STR = "ROTATOR_SKY";
    
    public static int    DEFAULT_ROTATOR_ANGLE_MODE = TelescopeConfig.ROTATOR_MODE_SKY;
  
    public static double AG_DEFAULT_GUIDE_PROBE_POSITION = 0.0;
    
    public static double DEFAULT_SKY_ANGLE = 0.0;
   
    /** The singleton instance of PlanetariumControlAgent.*/
    protected static PlanetariumControlAgent instance;

    /** PCA Logger.*/
    protected static Logger pcaLog;

    /** The default InstrumentConfig to use for POS observations.*/
    protected InstrumentConfig defaultInstrumentConfig;

    /** The default TelescopeConfig to use for POS observations.*/
    protected TelescopeConfig defaultTelescopeConfig;

    /** Counts the number of frames exposed so far since start of night's obs.
     * This number must be maintained over reboots during the night.*/
    protected int frameCounter;

    /** The start number for the frameCounter on the current obs-day.*/
    protected long frameCounterStart;

    /** The base directory for storing Planetarium mode images - in fact these are
     * just soft links to files on the Planetarium Instrument's control computer
     * via NFS mount. .*/
    protected File imageBaseDir;

    /** The current WSF File.*/
    protected File scheduleFile;

    /** Image Transfer Relay host.*/
    protected String relayHost;

    /** Image Transfer Relay port.*/
    protected int relayPort;

    /** Keystore containing the PCA's private key.*/
    protected File pcaRelayKeyFile;

    /** PCA's private key password.*/
    protected String pcaRelayPassword;

    /** Keystore containing the Relay hosts' public key(s).*/
    protected File pcaRelayTrustFile;

    /** Image Transfer SERVERPC destination.*/
    protected String serverPcHost;

    /** Image Transfer SERVERPC port.*/
    protected int serverPcPort;

    /** Transfer buffer size.*/
    protected int bufferLength;
    
    /** True if SERVERPC is the local ITR host.*/
    protected boolean serverPcLocal;

    /** Network Authorization information.*/
    protected Map authorization;

    /** Image repository information.*/
    protected Map destinations;

    /** Window Schedule Information.*/
    protected SortedSet schedule;

    /** POS Instrument name.*/
    protected String instrumentName;

    /** POS InstrumentConfig class name.*/
    protected String instConfigClassName;

    /** POS Instrument mount point.*/
    protected File instrumentMountPoint;

    /** Short date format for desiganting lockfile name.*/
    static final SimpleDateFormat adf = new SimpleDateFormat("MMdd");

    /** Implements the lockfile associated with the daily frame counter.*/
    PersistentUniqueInteger puid;

    /** Stores the time of the last broadcast timeout. DEPRECATED ?*/
    protected long lastBCTimeout;

    /** Bandwidth of external image transfer channel (KBytes/sec).*/
    protected int transferBandwidth;

    /** Dome limit - used by POS_Observe commands. (rads)*/
    protected double domeLimit;

    /** Id of current user/school.*/
    protected String currentUserName = "";

    /** Flag set to indicate that a mosaic is being performed.*/
    protected volatile boolean mosaicInProgress = false;

    /** Flag to indicate whether this is the PCA has just been fired up.*/
    protected volatile boolean firstJob = false;

    /** Flag to indicate that PCA is initializing.*/
    protected volatile boolean pcaInit = true;

    /** Flag to indicate whether init completed successfully or failed.*/
    protected volatile boolean initializedStatus;

    /** Client for image transfer.*/
    protected SSLFileTransfer.Client client;

    /** Create a Planetarium_Ops_Task using the supplied settings.
     * @param name The unique name/id for this TaskImpl.
     * @param manager The Task's manager.
     */
    public PlanetariumControlAgent(String      name,
				   TaskManager manager) {
	super(name, manager);	
	authorization  = new HashMap();
	destinations   = new HashMap();
	schedule       = new TreeSet();
	imageBaseDir   = new File(DEFAULT_IMAGE_BASE_DIR);
  	
    }
    
    /** Creates the initial instance of the PlanetariumControlAgent.
     */
    public void initialize(ModalTask mt) {
	instance = (PlanetariumControlAgent)mt;

	// Ready the POS_Queue
	POS_Queue.getInstance();		
    
    }

    /** Configure from File.
     * @param file Configuration file.
     * @exception IOException If any problem occurs reading the file or does not exist.
     * @exception IllegalArgumentException If any config information is dodgy.
     */    
    public void configure(File file) throws IOException, IllegalArgumentException { 
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));

	// Setup logging.
	pcaLog = LogManager.getLogger("PCA");
	pcaLog.setLogLevel(Logging.ALL);
	File logDir = RCS_Controller.getLogDir();
	File logFile = new File(logDir, "rcs_pca");
	LogHandler fh = new FileLogHandler(logFile.getPath(), new BasicLogFormatter(), FileLogHandler.DAILY_ROTATION);
	fh.setLogLevel(ALL);
	fh.setName("PCA_TEXT");
	LogManager.registerHandler(fh);
	pcaLog.addHandler(fh);
	
	pcaLog.log(1, "Config::Loaded Properties");
	
	File imfile = config.getFile("planetarium.image.base.dir", "planetarium/images");	
	setImageBaseDir(imfile.getPath());
	
	POS_ImageProcessor.initialize();
	File pipFile =  config.getFile("image.processing.config.file", 
				       "config/planetarium_image_processing.properties");
	POS_ImageProcessor.configure(pipFile); 
	POS_ImageProcessor.setImageBaseDir(getImageBaseDir());
		
	// Setup an image-transfer client.

	relayHost = config.getProperty("relay.host", DEFAULT_RELAY_HOST);

	relayPort = config.getIntValue("relay.port", DEFAULT_RELAY_PORT);

	// True if the SERVERPC destination is a local destination
	serverPcLocal = config.getBooleanValue("server.pc.local", true);

	if (! serverPcLocal) {
	    serverPcHost = config.getProperty("server.pc.host", DEFAULT_SERVERPC_HOST);
	    serverPcPort = config.getIntValue("server.pc.port", DEFAULT_SERVERPC_PORT);
	}

	pcaRelayKeyFile = config.getFile("pca.key.file", "planetarium/keys/pca.private");

	pcaRelayPassword = config.getProperty("pca.key.password", "geronimo");

	pcaRelayTrustFile = config.getFile("pca.trust.file", "planetarium/keys/itr.public");

	transferBandwidth = config.getIntValue("transfer.bandwidth", DEFAULT_BANDWIDTH);
	
	bufferLength      = config.getIntValue("buffer.length", DEFAULT_BUFFER_LENGTH);

	// ######## START
	
	SSLFileTransfer.setLogger("PCA");
	
	// Create a Secure client.
	client = new SSLFileTransfer.Client("RCS_PCA", relayHost, relayPort, true);
		
	client.setBandWidth(transferBandwidth);
	client.setBufferLength(bufferLength);

	// If we fail to setup we just can't forward images but other stuff will work.
	try {
	    client.initialize(pcaRelayKeyFile, pcaRelayPassword, pcaRelayTrustFile);
	    pcaLog.log(1,"Setup SSL Image Transfer client:"+
		       "Image TransferRelay: "+relayHost+" : "+relayPort);
	} catch (Exception ex) {
	    client = null;
	}
	
	// ######## END


	domeLimit = Math.toRadians(config.getDoubleValue("dome.limit", 0.0));
	
	File netFile = config.getFile("network.config.file", "config/planetarium_network.properties");
	loadNetworkConfig(netFile);

	File destFile= config.getFile("image.repository.config.file", "config/planetarium_destinations.properties");
	loadDestinationConfig(destFile);

	// #### DONT LOAD THIS NOW GET FROM TMM ######

	File wsFile = new File("config/planetarium_window_schedule.properties");
	loadWindowSchedule(wsFile);
	
	File icFile = config.getFile("instrument.config.file", "config/planetarium_instrument.config");	
	loadDefaultInstrumentConfig(icFile);
	
	File tcFile = config.getFile("telescope.config.file", "config/planetarium_telescope.properties"); 	
	loadDefaultTelescopeConfig(tcFile);
	
	pcaLog.log(1, CLASS, name, "configure", 
		   "Loaded Planetarium mode inst and telescope configurations.");		
    }

    /** Returns a reference to the singleton instance.*/
    public static ModalTask getInstance() {
	return instance;
    }

    /** Returns the default InstrumentConfig.*/
    public InstrumentConfig getDefaultInstrumentConfig() {
	return defaultInstrumentConfig;
    }
    
    /** Returns the default TelescopeConfig.*/
    public TelescopeConfig getDefaultTelescopeConfig() { 
	return defaultTelescopeConfig;
    }

    /** Load the default InstrumentConfig from a file.
     * @param file The file to load properties from.
     * @exception IOException If the file does not exist or an error occurs while reading.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadDefaultInstrumentConfig(File file) 
	throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	loadDefaultInstrumentConfig(config);
    }

    /** Load the default InstrumentConfig from a java.util.Properties.
     * @param config The Properties object containing config info.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadDefaultInstrumentConfig(ConfigurationProperties config) 
	throws IllegalArgumentException {

	instrumentName = config.getProperty("instrument", DEFAULT_INSTRUMENT);
	CCDConfig ccdConfig = null;
	instConfigClassName = config.getProperty("instrument.config.class", "ngat.phase2.CCDConfig");
	
	try {
	    Class instConfigClass = Class.forName(instConfigClassName);
	    Constructor       con = instConfigClass.getConstructor(new Class[] {String.class});
	    ccdConfig = (CCDConfig)con.newInstance(new Object[] {DEFAULT_INSTRUMENT_CONFIG_ID});
	} catch (Exception e) {
	    throw new 
		IllegalArgumentException("Planetarium_Agent: "+
					 "Unable to generate POS InstrumentConfig: Class: "+
					 instConfigClassName);
	}
	
	if (ccdConfig == null) {	 
	    throw new IllegalArgumentException("Planetarium_Agent: InstrumentConfig was not generated: "); 
	}
	
	String lowerFilter = config.getProperty("lower.filter", CCD_DEFAULT_LOWER_FILTER);
	String upperFilter = config.getProperty("upper.filter", CCD_DEFAULT_UPPER_FILTER);
	
	int xBin = config.getIntValue("x.bin", CCD_DEFAULT_XBIN);
	int yBin = config.getIntValue("y.bin", CCD_DEFAULT_YBIN);
	
	CCDDetector detector = new CCDDetector();
	
	detector.setXBin(xBin);
	detector.setYBin(yBin);
	
	// No Windows allowed.
	detector.clearAllWindows();
	
	ccdConfig.setDetector(0, detector);
	
	defaultInstrumentConfig = ccdConfig;
	
	// Check the instrument mount point exists.
	String mountPointName = Instruments.findMountPoint(instrumentName);
	if (mountPointName == null)
	    throw new IllegalArgumentException("Planetarium_Agent: Instrument mount point not specified");
	
	instrumentMountPoint = new File(mountPointName);
	if (!instrumentMountPoint.isDirectory())
	    throw new IllegalArgumentException("Planetarium_Agent: Instrument: "+instrumentName+
					       " Mount point not a valid directory or not accessible: "+instrumentMountPoint.getPath());
	
    }

    /** Load the default TelescopeConfig from a file.
     * @param file The file to load properties from.
     * @exception IOException If the file does not exist or an error occurs while reading.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadDefaultTelescopeConfig(File file) 
	throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	loadDefaultTelescopeConfig(config);
    }
    
    /** Load the default TelescopeConfig from a java.util.Properties.
     * @param config The Properties object containing config info.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadDefaultTelescopeConfig(ConfigurationProperties config) 
	throws IllegalArgumentException {
	
	TelescopeConfig telescopeConfig = new TelescopeConfig(DEFAULT_TELESCOPE_CONFIG_ID);

	String agStarSelectionModeStr = config.getProperty("autoguider.star.selection.mode", 
							   AG_DEFAULT_STAR_SELECTION_MODE_STR); 
	int agStarSelectionMode = AG_DEFAULT_STAR_SELECTION_MODE;
	int agStarSelection1 = 0;
	int agStarSelection2 = 0;
	if (agStarSelectionModeStr.equals("STAR_SELECTION_RANK")) {
	    agStarSelectionMode = TelescopeConfig.STAR_SELECTION_RANK;
	    agStarSelection1 = config.getIntValue("autoguider.star.selection.1", AG_DEFAULT_STAR_RANK_1  );
	    agStarSelection2 = config.getIntValue("autoguider.star.selection.2", AG_DEFAULT_STAR_RANK_2  );	   
	} else if
	    (agStarSelectionModeStr.equals("STAR_SELECTION_RANGE")) { 
	    agStarSelectionMode = TelescopeConfig.STAR_SELECTION_RANGE;
	    agStarSelection1 = config.getIntValue("autoguider.star.selection.1", AG_DEFAULT_STAR_RANGE_1  );
	    agStarSelection2 = config.getIntValue("autoguider.star.selection.2", AG_DEFAULT_STAR_RANGE_2  );
	} else if
	    (agStarSelectionModeStr.equals("STAR_SELECTION_PIXEL")) {
	    agStarSelectionMode = TelescopeConfig.STAR_SELECTION_PIXEL;
	    agStarSelection1 = config.getIntValue("autoguider.star.selection.pixel.x", AG_DEFAULT_STAR_PIXEL_X  );
	    agStarSelection2 = config.getIntValue("autoguider.star.selection.pixel.y", AG_DEFAULT_STAR_PIXEL_Y  );
	}
	telescopeConfig.setAutoGuiderStarSelectionMode(agStarSelectionMode);
	telescopeConfig.setAutoGuiderStarSelection1(agStarSelection1);
	telescopeConfig.setAutoGuiderStarSelection2(agStarSelection2);
	
	String agUsageModeStr = config.getProperty("autoguider.usage.mode",  
						   AG_DEFAULT_USAGE_MODE_STR);
	
	int agUsageMode = AG_DEFAULT_USAGE_MODE;
	if (agUsageModeStr.equals("AGMODE_MANDATORY")) {
	    agUsageMode = TelescopeConfig.AGMODE_MANDATORY;
	} else if 
	    (agUsageModeStr.equals("AGMODE_OPTIONAL")) {
	    agUsageMode = TelescopeConfig.AGMODE_OPTIONAL;
	} else if
	    (agUsageModeStr.equals("AGMODE_NEVER")) {
	    agUsageMode = TelescopeConfig.AGMODE_NEVER;
	}
	telescopeConfig.setAutoGuiderUsageMode(agUsageMode);
	
	float focusOffset = config.getFloatValue("telescope.focus.offset",  
						 DEFAULT_FOCUS_OFFSET);
	telescopeConfig.setFocusOffset(focusOffset);
	
	double agGuideProbePosition = config.getDoubleValue("autoguider.probe.position",
							    AG_DEFAULT_GUIDE_PROBE_POSITION);
	telescopeConfig.setGuideProbePosition(agGuideProbePosition);
	
	String rotatorAngleModeStr = config.getProperty("rotator.angle.mode",    
							DEFAULT_ROTATOR_ANGLE_MODE_STR);
	int rotatorAngleMode = DEFAULT_ROTATOR_ANGLE_MODE;
	if (rotatorAngleModeStr.equals("ROTATOR_MODE_MOUNT")) {
	    rotatorAngleMode = TelescopeConfig.ROTATOR_MODE_MOUNT;
	} else if 
	    (rotatorAngleModeStr.equals("ROTATOR_MODE_SKY")) {
	    rotatorAngleMode = TelescopeConfig.ROTATOR_MODE_SKY;
	} else if
	    (rotatorAngleModeStr.equals("ROTATOR_MODE_VFLOAT")) {
	    //   rotatorAngleMode = TelescopeConfig.ROTATOR_MODE_VFLOAT;
	}
	telescopeConfig.setRotatorAngleMode(rotatorAngleMode);
	

	// ##### should this be degress to rads ?
	double skyAngle = config.getDoubleValue("rotator.sky.angle",   
						DEFAULT_SKY_ANGLE);
	telescopeConfig.setSkyAngle(skyAngle);

	defaultTelescopeConfig = telescopeConfig;

    }

    /** Load the Network Authorization Settings from a file.
     * @param file The file to load properties from.
     * @exception IOException If the file does not exist or an error occurs while reading.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.
     */
    protected void loadNetworkConfig(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	loadNetworkConfig(config);
    }
    
    
    /** Load the Network Authorization Settings from a java.util.Properties.
     * @param config The Properties object containing config info.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadNetworkConfig(ConfigurationProperties config) 
	throws IllegalArgumentException {
	
	// Looking for XX<ID>.address = <RTOC Address>
	String name = null;
	String rtoc = null;
	String addr = null;
	Enumeration e = config.propertyNames();
	while (e.hasMoreElements()) {
	    name = (String)e.nextElement();
	    if (name.endsWith(".rtoc.id")) {
		rtoc = name.substring(0, name.indexOf(".rtoc.id"));
		addr = config.getProperty(name);
		System.err.println("RTOC: ["+rtoc+"] has AUTH-CODE ["+addr+"]");
		authorization.put(addr, rtoc);
	    }
	}
    }

   
    /** Load the Image Repository Settings from a file.
     * @param file The file to load properties from.
     * @exception IOException If the file does not exist or an error occurs while reading.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.
     */
    protected void loadDestinationConfig(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	loadDestinationConfig(config);
    }
    
    
    /** Load the Image Repository Settings from a java.util.Properties.
     * @param config The Properties object containing config info.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.*/
    protected void loadDestinationConfig(ConfigurationProperties config) 
	throws IllegalArgumentException {
	// Looking for alt.<n>.host = <Alternative-N WebImageServer Relay host-address>
	//             alt.<n>.port = <Alternative-N WebImageServer Relay port>
	String name = null;
	String dest = null;
	String host = null;
	String alt  = null;
	int    port = 0;
	Enumeration e = config.propertyNames();
	while (e.hasMoreElements()) {
	    name = (String)e.nextElement();
	    if (name.endsWith(".host")) {
		dest = name.substring(0, name.indexOf(".host"));
		host = config.getProperty(dest+".host");
		port = config.getIntValue(dest+".port", DEFAULT_RELAY_PORT);
		if (dest.startsWith("alt.")) {
		    alt = dest.substring(4);
		    try {
			URL url = new URL("http", host, port, "");
			destinations.put(alt, url);
			System.err.println("AltDest: ["+alt+"] at address: "+url.toString());
		    } catch (MalformedURLException mfx) {
			throw new IllegalArgumentException("Bad URL: "+mfx);
		    }		    		    
		}
	    }
	}
		
    }

    /** Load the Window Schedule from a file. This could be called up when the WSF changes during 
     * a session - warning !!!
     * @param file The file to load schedule from.
     * @exception IOException If the file does not exist or an error occurs while reading.
     * @exception IllegalArgumentException If any parameters are incorrect
     * or missing.
     */
    protected void loadWindowSchedule(File file) throws IOException, IllegalArgumentException {
	
	BufferedReader in = new BufferedReader(new FileReader(file));

	LineProcessor  lp = new LineProcessor();
	String line = null;
	long now = System.currentTimeMillis();

	while ( (line = in.readLine()) != null) {
	    if (line.trim().startsWith("#")) continue; // Skip comments.
	    try {
		lp.processLine(line);
		// Ignore any Windows which have already completed or which dont start for >24 hrs.
		if ((lp.startTime > (now+24*3600*1000L)) ||
		    (lp.endTime < now))
		    continue;
		// Add a schedule item.
		System.err.println("WinSched: "+
				   tdf.format(new Date(lp.startTime))+" - "+
				   tdf.format(new Date(lp.endTime))+" For: ["+lp.controllerId+"]");
		schedule.add(new ScheduleInfo(lp.startTime, lp.endTime, lp.controllerId));
	    } catch (IllegalArgumentException iax) {
		System.err.println("WinSched::"+iax);		
	    }

	}

	// Set the file..
	scheduleFile = file;

    }
    
   
    /** Generates a new JOB for the PCA. 
     * If the PCA is just initializing then we expect a POS_Init task to be generated.
     * Otherwise, if a Task can be generated from the lists of Handlers on the 
     * POS_Queue then the first one is returned otherwise null.
     */
    public Task getNextJob() { 
	// First call to getNextJob().
	if (firstJob) {
	    firstJob = false;	    	   
	    return new POS_InitTask(name+"/INIT", this);
	}

	//System.err.println("PCA:: getNextJob()");
	POS_CommandImpl pci = POS_Queue.getInstance().remove();
	//System.err.println("PCA:: Got a PCI: "+pci);
	if (pci == null)
	    return null;
	Task task = pci.createTask();
	//System.err.println("PCA:: created Task: "+task);
	if (task != null)
	    POS_Queue.getInstance().setExecutor(pci);
	// Could be null.
	//System.err.println("PCA:: Returning pci: "+pci);
	return task;
    }

    /** Override to return <i>true</i> under the following circumstances.
     * 
     * Nighttime and Planetarium Mode is active - for now we use a signal
     * of the form PLANETARIUM.ON PLANETARIUM.OFF via EMS.
     *
     */
    public boolean acceptControl() { 	
	Position sun = Astrometry.getSolarPosition(System.currentTimeMillis());
	//System.err.println("PCA:: acceptControl called by "+Thread.currentThread());

	// Check for RTOC Overide.
	if (overridden)
	    return enabled;

	// Check over the WSF.
	if (isScheduledWindow()) {	   
		return true;
	}
	// TimeWindow window = schedule.getCurrentWindow().
	// if (window == null) return false;
	//
	// if (window.mca.equals(agentId)) return true;
	//
	
	return false;

    }


    /** Return true if wants control at time.*/
    public boolean wantsControl(long time) throws RemoteException {
	// we only allow now as time...
	return isScheduledWindow();
    }
    
    /** How long till this controller will definitely want control from time.*/
    public long nextWantsControl(long time) throws RemoteException {

	if (isScheduledWindow())
	    return System.currentTimeMillis();

	long snw = getStartNextWindow();

	// really we want to go a few minutes early if possible and use this for acceptControl() also..
	
	if (snw < 0L)
	    return System.currentTimeMillis() + 24*3600*1000L;

	return snw;
    }





    /** Overriden to return the time at which this ModalControlAgent will next request
     * control. PCA takes control during an RTOC scheduled period only.
     * @return Time when this MCA will next want/be able to take control (millis since 1970).
     */
    public long demandControlAt() { 
	
	if (isScheduledWindow())
	    return System.currentTimeMillis();

	long snw = getStartNextWindow();

	// really we want to go a few minutes early if possible and use this for acceptControl() also..
	
	if (snw < 0L)
	    return System.currentTimeMillis() + 24*3600*1000L;

	return snw;
    }

    /** Overridden to carry out specific work after the init() method is called.
     * Sets a number of FITS headers and subscribes to any required events.
     * Sets up the daily frame counter.
     * Note: We need to obtain either the latest frameCounter for today (obs-day)
     * if the POT has had to restart or the obs-day's startup frame counter.
     * The frameCounter for the current obs-day is just MMdd0000 where MMdd are
     * the month and day-of-month for current obs-day. The lockfile should hold
     * a dangling link containing the last frame number entered for this obs-day.
     * If it contains yesterday's o/d then this is the start of night so we start
     * framecounter as MMdd0000 and save it. We need to check lock.new and lock.old
     * to be sure in case an error occurred while writing the last entry. If there
     * is no lockfile at all then this is the telescope startup or there has been
     * a major failure or config error !.*/
    public void onInit() {
	super.onInit();
	// Initialize POS_Queue and POS_Despatcher.
	// ##### POS_Queue.... POS_Despatcher.... #####
	
        puid = new PersistentUniqueInteger("%%frame");
	//frameCounter = 0;
	try {
	    frameCounter = puid.get();
	} catch (Exception e) {
	    System.err.println("Error reading initial frame counter: "+e);
	    frameCounter = 0;
	}
	
	String currentController = getControllerId();
	opsLog.log(1, "Starting Planetarium-Operations Mode."+
		   "\n   Start Frame Counter: "+frameCounter+
		   "\n   Processed Images:    "+imageBaseDir.getPath()+
		   "\n   Current RTOC:        "+(currentController != null ? 
						 currentController : "NONE_AUTHORIZED")+	
		   (isScheduledWindow() ?
		    "\n   Current RTOC window"+ 
		    "\n           Starts:      "+tdf.format(new Date(getStartCurrentWindow()))+
		    "\n           Ends:        "+tdf.format(new Date(getEndCurrentWindow())) :
		    "\n   NO WINDOW")+

		   "\n    Next window will be: "+tdf.format(new Date(getStartNextWindow()))+
		   "\n                  until: "+tdf.format(new Date(getEndNextWindow()))+
		   
		   "\n   Current username:    "+(currentUserName != null ? currentUserName : "UNKNOWN")+
		   "\n   Instrument Image NFS:"+(instrumentMountPoint.exists() ? instrumentMountPoint.getPath()+
						 (instrumentMountPoint.isDirectory() ? " (D:" : "(F:")+
						 (instrumentMountPoint.canRead() ? "R:" : "NR:")+
						 (instrumentMountPoint.canWrite() ? "W" : "NW")+")" :
						 "NOT_FOUND"));
	
	//POS_Queue.getInstance().setAccept(true);
	FITS_HeaderInfo.current_USERID.setValue(currentUserName); // keep resetting this please
	FITS_HeaderInfo.current_TELMODE.setValue("PLANETARIUM");
	FITS_HeaderInfo.current_COMPRESS.setValue("PLANETARIUM");

	// Always override until we decide not to.
	ngat.rcs.iss.ISS_AG_START_CommandImpl.setOverrideForwarding(true);
	ngat.rcs.iss.ISS_AG_STOP_CommandImpl.setOverrideForwarding(false);

    }
    
    /** Sets a flag 'firstJob' to indicate that nextJob() should return an 
     * initialization (POS_Init) Task.*/
    public void onStartup() {
	super.onStartup();
	firstJob = true;
	POS_Queue.getInstance().setAccept(false);
    }

    public void onCompletion() {
	super.onCompletion();
	opsLog.log(1, "Completed Planetarium-Operations Mode.");
    } 

    /** Overridden to carry out any specific clearing up
     * after the generic clearup has been performed.*/
    public void onDisposal() {
	super.onDisposal();
	POS_Queue.getInstance().setAccept(false);
	currentUserName = null;
    }
    
    /** 
     * THESE DONT WORK ANYMORE SINCE StateModel took over abort control.
     */
    public void onAborting() {
	super.onAborting();
	pcaLog.log(1, CLASS, name, "onAborting", "Abortcode is: "+abortCode);
	
	Track_Task trackOffAz = new Track_Task(name+"/EM_TRKOFF_AZ",
					       this,
					       TRACK.AZIMUTH,
					       TRACK.OFF);
	taskList.addTask(trackOffAz);
	
	Track_Task trackOffAlt = new Track_Task(name+"/EM_TRKOFF_ALT",
						this,
						TRACK.ALTITUDE,
						TRACK.OFF);
	taskList.addTask(trackOffAlt);
	
	Track_Task trackOffRot = new Track_Task(name+"/EM_TRKOFF_ROT",
						this,
						TRACK.ROTATOR,
						TRACK.OFF);
	taskList.addTask(trackOffRot);
	
    }
    

    /** Advances the current exposure frame counter and creates a SymLink to 
     * the file containing the raw FITS image data against the new counter.
     * i.e. The symlink file is: %%frame-nn in the local image base directory.
     *      The remote file is something like "c_e_200202_1_2.fits" or the like
     *      but on the Instrument's ICC computer in its image directory).
     *      The mount point should be to this directory: e.g "/icc/tmp" or the likes.
     *      The file returned is the full path e.g. "/icc/tmp/c_e_2002_02.fits"
     * @param filename The full path of the image filename on the originating system.
     * @return The newly incremented frame counter.
     */
    public long advanceFrameCounter(String filename) throws IOException {
	try {
	    frameCounter = puid.increment();
	} catch (Exception e) {
	   throw new IOException("Building symlink: Failed to increment frame counter: "+e);
	}
	
	File remoteFile = new File(filename);
	File linkFile   = new File(imageBaseDir, "%%frame-"+frameCounter);

	// We need to get the real instrument mount point here.	
	// Something like.. "/mnt/ratcam-images" or the like.
	File imageFile = new File(instrumentMountPoint, remoteFile.getName());
	// imageFile will now be..   "/mnt/ratcam-images/c_e_2002_0_2.fits" or likes.

	try {
	    FileUtilities.createSymbolicLink(imageFile.getPath(),  linkFile.getPath());
	    pcaLog.log(1, CLASS, name, "advanceFrameCounter",
			"Built symlink for image frame: "+frameCounter+
			"\n\tLinks: "+linkFile.getPath()+" -> "+imageFile.getPath()+" ("+remoteFile.getPath()+")");
	    
	} catch (FileUtilitiesNativeException fx) {
	    throw new IOException("Building symlink: Failed to create link to image file: "+fx);
	}

	properties.put("Current Frame", ""+frameCounter);

	return (long)frameCounter;
    }
    
    /** TEMPORARY method to return the current frame start. ###KILL###*/
    public long getCurrentFrameStart() { return frameCounterStart; }
    
    /** Returns the current frame number.*/
    public long getCurrentFrameCounter() { return (long)frameCounter; }

    /** Returns the next available frame counter.*/    
    public long getNextFrameCounter() { return (long)frameCounter+1; }

    /** Causes any executing subtasks (i.e anything currently being managed) to
     * be sent Task.abort() - This method is intended to be called by the
     * RequestHandler for the POS_RCS.ABORT command when ABORT ALL is received.
     * Each task has its abortCode and Message set appropriately before abort()
     * is called.  ## METHOD NOT IMPLEMENTED YET ####*/
    public void abandonAll() {
	// loop over tasklist call task.abort() - hopefully the POS_ABORT_CmdImpl
	// will have called task.setAbortCode(TASK_ABORTED, "ABORTED BY CLIENT");
	

    }

    /** Set the base directory used for storing Planetarium mode images.<br>
     * Defaults to <i>/home/pos/images</i> set up on initialization.
     * @param dirName The full path to the base directory.*/
    public void setImageBaseDir(String dirName) {
	imageBaseDir = new File(dirName);
    }

    /** Return the base directory used for storing Planetarium mode images. This should
     * have been set by setImageBaseDir().*/
    public File getImageBaseDir() {
       return imageBaseDir;
    }

    /** Returns the Host name of the Relay server.*/
    public String getRelayHost() {
	return relayHost;
    }

    /** Returns the port where the Relay is bound.*/
    public int getRelayPort() { 
	return relayPort; 
    }

    /** Returns the host of the remote SERVERPC destination.*/
    public String getServerPcHost() {
	return serverPcHost;
    }

    /** Returns the port used by the remote SERVERPC destination.*/
    public int getServerPcPort() {
	return serverPcPort;
    }
    
    /** Returns an initialized SSLFileTransfer$Client or null if it was not
     * successfully created or initialized.*/
    public SSLFileTransfer.Client getClient() {
	return client;
    }
    
    /** Returns true if the SERVERPC destination is local.*/
    public boolean getServerPcLocal() { 
	return serverPcLocal;
    }

    /** Returns the dome lower limit (rads).*/
    public double getDomeLimit() { return domeLimit; }

    public URL getAlternativeDestination(int alt) {
	return (URL)destinations.get(""+alt);	
    }
    
    public void setInitializedStatus(boolean status) {
	this.initializedStatus = status;
    }

    public boolean getInitializedStatus() {
	return initializedStatus;
    }

    public void setPcaInit(boolean init) {
	this.pcaInit = init;
    }

    public boolean getPcaInit() {
	return pcaInit;
    }

    /** Sets the Window-schedule file.*/
    public void setScheduleFile(String fileName) {
	scheduleFile = new File(fileName);
    }

    /** Returns the Window-schedule file.*/
    public File getScheduleFile() {  return scheduleFile; }

    /** Sets the current (slot) user name.*/
    public void setCurrentUserName(String name) { currentUserName = name; }

    /** Returns the current (slot) user name.*/
    public String getCurrentUserName() { return currentUserName; }

    /** Returns the name of the POS Instrument.*/
    public String getInstrumentName() { return instrumentName; }

    /** Returns the instrument  mount point.*/
    public File getInstrumentMountPoint() { return instrumentMountPoint; }
  
    /** Returns true if the ControlId is authorized - i.e. A valid RTOC.*/
    public boolean isAuthorized(String ctrlId) {
	if (!authorization.containsKey(ctrlId))
	    return false;
	//System.err.println("PCA::Authorization: Controller authorization request from: ["+authorization.get(ctrlId)+"]");
	return true;
    }

    /** Returns true if the ControlId is currently (now) IN_CONTROL according to WSF.*/
    public boolean isInControl(String ctrlId) {
	long now = System.currentTimeMillis();
	return isInControl(ctrlId, now);
    }

    /** Returns true if the ControlId is IN_CONTROL according to WSF
     * at the prescribed time.*/
    public boolean isInControl(String ctrlId, long when) {

	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getWindow(when);
	if (w == null) return false;

	//	if (w.info.equals(ctrlId)) return true;
	return true;
	//	return false;

    }

    /** Returns the name of the current controlling RTOC (or null if None).*/
    public String getControllerId() {
	long now = System.currentTimeMillis();
	return getControllerId(now);
    }

    /** Returns the name of the controlling RTOC (or null if None).*/
    public String getControllerId(long when) {

	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getWindow(when);
	if (w == null) return null;

	return w.info;

    }

    /** Returns the start time for the current window or -1 if none.*/
    public long getStartCurrentWindow() {

	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getCurrentWindow();
	if (w == null) return -1L;

	return w.t1;

    }

    /** Returns the end time for the current window.*/
    public long getEndCurrentWindow() {

	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getCurrentWindow();
	if (w == null) return -1L;

	return w.t2;

    }

    /** Returns true if there is a current window scheduled to any RTOC.*/
    public boolean isScheduledWindow() {


	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getCurrentWindow();
	if (w == null) return false;

	if (w.mca.equals(agentId)) return true;

	return false;

    }

    /** Returns the start time of the next available scheduled window
     *  or NOW if there is a window currently in operation. If there are no windows
     * in the schedule, returns a negative number. This method is typically called to 
     * determine when the Planetarium mode is likely to be called into operation.*/
    public long getStartNextWindow() {

	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getNextMcaWindow(agentId);
	if (w == null) return -1L;

	return w.t1;

    }

    public long getEndNextWindow() {
	WindowSchedule.TimeWindow w = TMM_TaskSequencer.getInstance().getNextMcaWindow(agentId);
	if (w == null) return -1L;
	
	return w.t2;
	
    }

   

    /** EventSubscriber method. Some temp settings.
     * <ul>
     *   <li>PL.ON  - enables PCA and cancels RTOC override.
     *   <li>PL.OFF - disables PCA.
     *   <li>PL.OVR - enables RTOC override.
     * </ul>
     */
    public void notifyEvent(String eventId, Object data) {
	super.notifyEvent(eventId, data);

	if (eventId.equalsIgnoreCase(overrideEnableCode)) {	   
	    POS_Queue.getInstance().setAccept(true);
	} else if 
	    (eventId.equalsIgnoreCase(overrideDisableCode)) {	
	    POS_Queue.getInstance().setAccept(false);
	}
    }
   
    class ScheduleInfo implements Comparable {
	
	public long startTime;
	
	public long endTime;
	
	public String controllerId;
	
	ScheduleInfo(long startTime, long endTime, String controllerId) {
	    this.startTime    = startTime;
	    this.endTime      = endTime;
	    this.controllerId = controllerId;
	}
	
	public int compareTo(Object o) {
	    ScheduleInfo other = (ScheduleInfo)o;
	    if (startTime < other.startTime) return -1;
	    if (startTime > other.startTime) return 1;
	    return 0;
	}

    }

    class LineProcessor {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	SimpleTimeZone   UTC = new SimpleTimeZone(0,"UTC");

	public long startTime;

	public long endTime;

	public String controllerId;

	LineProcessor() {
	    startTime    = 0L;
	    endTime      = 0L;
	    controllerId = null;
	    
	    sdf.setTimeZone(UTC);
	    
	}

	public void processLine(String line) throws IllegalArgumentException {
	    startTime    = 0L;
	    endTime      = 0L;
	    controllerId = null;
	    
	    StringTokenizer tok = new StringTokenizer(line, " ");

	    if (tok.countTokens() < 5) 
		throw new IllegalArgumentException("LineProcessor: Only "+tok.countTokens()+
						   " Should be 5 items.");

	    // Skip the first 2 for now should be: NNN PCA <t1> <t2> <rtoc>
	    tok.nextToken();
	    tok.nextToken();

	    try {
		Date date = sdf.parse(tok.nextToken());
		startTime = date.getTime();
	    } catch (ParseException px) {
		throw new IllegalArgumentException("LineProcessor: Parsing Window start: "+px);
	    }
	    try {
		Date date = sdf.parse(tok.nextToken());
		endTime   = date.getTime();
	    } catch (ParseException px) {
		throw new IllegalArgumentException("LineProcessor: Parsing Window end: "+px);
	    }

	    controllerId = tok.nextToken();

	}

    }

}

/** $Log: PlanetariumControlAgent.java,v $
/** Revision 1.2  2007/07/05 11:33:19  snf
/** checkin
/**
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2002/07/02 09:01:00  snf
/** Initial revision
/** */
