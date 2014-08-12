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

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.net.*;
import ngat.util.*;
import ngat.phase2.*;

import java.io.*;

/** Implementation of the ISS command relay server for
 * handling ISS_INST.INST_TO_ISS commands sent by the
 * various Instruement Control Systems (ICS).
 * <br><br>
 * $Id: ISS_Server.java,v 1.1 2006/12/12 08:30:20 snf Exp $
 */
public class ISS_Server extends SocketServer {

    /** Stores the currently specified telescope configuration.*/
    public static TelescopeConfig currentTelescopeConfig;


    /** The single instance of ISS_Server.*/
    private static ISS_Server instance = null;

    /** Create an ISS_Server bound to the specified port. <br>
     * This should have been defined in an RCS configuration file 
     * as <b>iss.command.port</b>.
     * @param port The port to bind to.
     */
    public ISS_Server(int port) throws IOException {
	super(port);
	rhFactory = ISS_CommandImplFactory.getInstance();
	piFactory = JMSMA_ProtocolImplFactory.getInstance();
	setDefaultTelescopeConfig();
    }
    
    /** Create the single instance of the ISS server. If the
     * single instance already exists returns silently. 
     * @param port The port to listen on.
     * @exception IOException If the ServerSocket fails to bind 
     * to the specified port for any reason.*/
    public static void bindInstance(int port) throws IOException {
	if (instance == null)
	    instance = new ISS_Server(port);
    }
    
    /** Configure from file.*/
    public void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	configure(config);
    }
    
    /** Configure from properties.*/
    public void configure(ConfigurationProperties config) throws IllegalArgumentException {
	


    }

    /** Starts up the server. Just calls start() on its execution
     * thread.*/
    public static void launch() {
	instance.start();
    }

    /** @return The single instance of ISS_Server. If no instance
     * has yet been created will return null.*/
    public static ISS_Server getInstance() {
	return instance;
    }

    /** When set (by TelFocus task) indicates that the next OFFSET_FOCUS
     * command is probably the one which will be used for TelFocus and so
     * this should be subtracted from any future OFFSET_FOCUS requests
     * before passing onto the TCS. The ISS_OFFSET_FOCUS_CommandImpl
     * needs to check this flag when it receives a command.*/
    public static volatile boolean expectTelFocusOffsetSoon = false;

    public static void setExpectTelFocusOffsetSoon(boolean in) {
	expectTelFocusOffsetSoon = in;
    }

    public static boolean getExpectTelFocusOffsetSoon() {
	return expectTelFocusOffsetSoon;
    }

    /** Returns the currently specified Telescope configuration.*/
    public static TelescopeConfig getCurrentTelescopeConfig() { return currentTelescopeConfig; }

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
   
}

/** $Log: ISS_Server.java,v $
/** Revision 1.1  2006/12/12 08:30:20  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:28  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */
