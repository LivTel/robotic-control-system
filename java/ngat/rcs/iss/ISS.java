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
package ngat.rcs.iss;

import ngat.util.*;
import ngat.phase2.*;

import java.io.*;
import java.util.*;

/**
 * Instrument Support System: Controller class. <br>
 * <br>
 * $Id: ISS.java,v 1.3 2007/09/11 10:01:17 snf Exp $
 */
public class ISS {

	/** Default ISS server port. */
	public static final int DEFAULT_ISS_PORT = 8373;

	/** Stores the currently specified telescope configuration. */
	public static TelescopeConfig currentTelescopeConfig;

	/** Stores the currently specified instrument configuration. */
	public static InstrumentConfig currentInstrumentConfig;

	/**
	 * Stores the current focus offset to add to any requested by instruments
	 * (mm).
	 */
	private static double currentFocusOffset = 0.0;

	/**
	 * Stores the latest instrument focus offset to add to any requested by
	 * observations (mm).
	 */
	private static double currentInstrumentFocusOffset = 0.0;

    /** This is set by an ACQUIRE and ONLY used by OFFSET_X_Y impl to determine rotation.*/
    private static String currentAcquisitionInstrumentName = null;

    private static String beamControlInstrument = null;

	/** Stores the current AG usage requirement. */
	public static int currentAutoguiderUsageMode = TelescopeConfig.AGMODE_OPTIONAL;

	/** Controls adjustments by instruments. */
	public IssAutoguiderAdjustmentController adjustmentController;

	/** The single instance of ISS. */
	private static ISS instance = null;

	/** The single instance of ISS_Server. */
	private static ISS_Server server = null;

	/** Stores details of which commands to forward. */
	protected Map forwarding;

	/**
	 * Create an ISS.
	 */
	private ISS() {
		setDefaultTelescopeConfig();
		forwarding = new HashMap();
		adjustmentController = new IssAutoguiderAdjustmentController();
	}

	/** Configure from file. */
	public void configure(File file) throws IOException, IllegalArgumentException {
		ConfigurationProperties config = new ConfigurationProperties();
		config.load(new FileInputStream(file));
		configure(config);
	}

	/** Configure from properties. */
	public void configure(ConfigurationProperties config) throws IOException, IllegalArgumentException {

		int port = config.getIntValue("iss.port", DEFAULT_ISS_PORT);

		ISS_Server.bindInstance(port);

		server = ISS_Server.getInstance();

	}

	/**
	 * Starts up the server. Just calls start() on its execution thread.
	 */
	public static void launch() {
		server.start();
	}

	/**
	 * @return The single instance of ISS_Server. If no instance has yet been
	 *         created will return null.
	 */
	public static ISS getInstance() {
		if (instance == null)
			instance = new ISS();
		return instance;
	}
	  
	/** Position offset due to mosaicing (RA) (rads).*/
    protected static double xoff = 0.0;

    /** Position offset due to mosaicing (Dec) (rads).*/
    protected static double yoff = 0.0;
    
    /** Set the temporary position offsets (due to mosaicing).*/
    public static void setUserOffsets(double xo, double yo) {
    	xoff = xo;
    	yoff = yo;
    	System.err.println("ISS: setUserOffsets: "+xo+", "+yo);
    }
    
    public static double getUserOffsetX() { return xoff;}
    public static double getUserOffsetY() { return yoff;}

    /** Instrument requested offset (RA) (rads).*/
    //protected static double txoff = 0.0;
    
    /** Instrument requested offset (Dec) (rads).*/
    //protected static double tyoff = 0.0;
    
    /** Set the instrument requested offsets.*/
    //public static void setInstrumentOffsets(double xo, double yo) {
    	//txoff = xo;
    	//tyoff = yo;
    	//System.err.println("ISS: setInstrumentOffsets: "+xo+", "+yo);
    //}

    //public static double getInstrumentOffsetX() { return txoff;}    
    //public static double getInstrumentOffsetY() { return tyoff;}


    public static void setBeamControlInstrument(String in) {
    	beamControlInstrument = in;
    	System.err.println("ISS: setBeamControlInstrument: "+in);
    }
    
    public static String getBeamControlInstrument() {
	return beamControlInstrument;
    }

    public static void setCurrentAcquisitionInstrumentName(String in) {
	currentAcquisitionInstrumentName = in;
	System.err.println("ISS: setCurrentAcquisitionInstrument: "+in);
    }

    public static String getCurrentAcquisitionInstrumentName() {
	return currentAcquisitionInstrumentName; 
    }

    public static void setCurrentFocusOffset(double offset) {
    		currentFocusOffset = offset;
    		System.err.println("ISS: setObservationFocusOffset: "+offset);
    }
    
    public static double getCurrentFocusOffset() {
    		return currentFocusOffset;  	
    }
    
    public static void setInstrumentFocusOffset(double offset) {
    	currentInstrumentFocusOffset = offset;
    	System.err.println("ISS: setInstrumentFocusOffset: "+offset);
    }
    
    public static double getInstrumentFocusOffset() {
    		return currentInstrumentFocusOffset;
    }
    
	/** Returns the currently specified Telescope configuration. */
	public static TelescopeConfig getCurrentTelescopeConfig() {
		return currentTelescopeConfig;
	}

	/** Set the default telescope config. */
	protected static void setDefaultTelescopeConfig() {
		TelescopeConfig telescopeConfig = new TelescopeConfig("ISS_Default");
		telescopeConfig.setAutoGuiderStarSelectionMode(TelescopeConfig.STAR_SELECTION_RANK);
		telescopeConfig.setAutoGuiderStarSelection1(1);
		telescopeConfig.setAutoGuiderStarSelection2(1);
		telescopeConfig.setAutoGuiderUsageMode(TelescopeConfig.AGMODE_OPTIONAL);
		telescopeConfig.setFocusOffset(0.0f);
		telescopeConfig.setGuideProbePosition(0.0);
		telescopeConfig.setRotatorAngleMode(TelescopeConfig.ROTATOR_MODE_SKY);
		telescopeConfig.setSkyAngle(0.0);
		currentTelescopeConfig = telescopeConfig;
	}

	/**
	 * Sets whether to forward the named command class. CommandClassName should
	 * be fully qualified e.g.- <i>ngat.message.ISS_INST.AG_STOP</i>
	 */
	public void setDoForward(String commandClassName, boolean forward) {
		forwarding.put(commandClassName, new Boolean(forward));
	}

	/**
	 * Returns True if the named command (a subclass of ISS_INST) is to be
	 * forwarded or faked. CommandClassName should be fully qualified e.g.-
	 * <i>ngat.message.ISS_INST.AG_STOP</i>
	 */
	public boolean doForward(String commandClassName) {

		Boolean fwd = (Boolean) forwarding.get(commandClassName);

		if (fwd == null)
			return false;
		if (fwd.booleanValue())
			return true;
		return false;

	}

	/** Return the AutoguiderAdjustmentController instance. */
	public IssAutoguiderAdjustmentController getAdjustmentController() {
		return adjustmentController;
	}

}

/**
 * $Log: ISS.java,v $ /** Revision 1.3 2007/09/11 10:01:17 snf /** typo /** /**
 * Revision 1.2 2007/09/11 09:59:51 snf /** added ag adjustment controller
 * instance. /** /** Revision 1.1 2006/12/12 08:30:20 snf /** Initial revision
 * /** /** Revision 1.1 2006/05/17 06:34:28 snf /** Initial revision /**
 */
