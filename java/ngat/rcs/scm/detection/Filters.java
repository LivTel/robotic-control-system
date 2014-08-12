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
package ngat.rcs.scm.detection;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;

/** Holds references to the various environment and subsystem Filters.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Filters.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Filters.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Filters {

    /** Filters the rain sensor data.*/
    public static Filter rainFilter;	

    /** Filters the wind speed sensor data.*/
    public static Filter windFilter;

    /** Filters the wind direction sensor data.*/
    public static Filter windDirnFilter;
    
    /** Filters the ag-seeing sensor data.*/
    public static Filter seeingFilter;

    /** Filters the az-limit sensor data.*/
    public static Filter azLimitFilter;
    
    /** Filters the alt-limit sensor data.*/
    public static Filter altLimitFilter; 

    /** Filters the rot-limit sensor data.*/
    public static Filter rotLimitFilter;

    /** Filters the az-state sensor data.*/
    public static Filter azStateFilter;
    
    /** Filters the alt-state sensor data.*/
    public static Filter altStateFilter;
    
    /** Filters the rot-state sensor data.*/
    public static Filter rotStateFilter;

    /** Filters the focus-state sensor data.*/
    public static Filter focusStateFilter;

    /** Filters the primary-mirror-cover-state sensor data.*/
    public static Filter primaryMirrorCoverStateFilter;

    /** Filters the fold-mirror-state sensor data.*/
    public static Filter foldMirrorStateFilter;

    /** Filters the enclosure-state sensor data.*/
    public static Filter enclosureStateFilter;

    /** Filters the enclosure-posn sensor data.*/
    public static Filter enclosurePositionFilter;

    /** Filters the enclosure-posn sensor data.*/
   public static Filter mirrcoverPositionFilter;

    /** Filters the network-state sensor data.*/
    public static Filter networkStateSlowFilter;

    /** Filters the network-state sensor data.*/
    public static Filter networkStateFastFilter;

    
    /** Filters the control-enablement-state data.*/
    public static Filter networkControlStateFilter;

    /** Filters the engineering-override-state data.*/
    public static Filter engineeringOverrideStateFilter;

    /** Filters the power-state sensor data.*/
    public static Filter powerStateFilter; 

    /** Filters the system(MCP)-state sensor data.*/
    public static Filter systemStateFilter; 

    /**#### Temp detects RATCAM PSU Low Voltage (+ve) Supply.*/
    public static Filter ratcamPSULowVoltagePlusFilter;

    
    /** Carry out configuration using the settings in the specified File.
     * @param configFile The file holding the settings.*/
    public static void configure(File configFile) {}
    
    /** Configure the Filter array with default settings.*/
    public static void defaultSetup() {
	// 0=NO_RAIN, 1=RAIN
	List RAIN_READINGS = new Vector();
	RAIN_READINGS.add(new Integer(TCS_Status.RAIN_ALERT));
	RAIN_READINGS.add(new Integer(TCS_Status.RAIN_CLEAR));
	rainFilter     = new ModalFilter    (Sensors.rainSensor,     5 , RAIN_READINGS, 1);
	windFilter     = new AveragingFilter(Sensors.windSensor,     5);
	windDirnFilter = new AveragingFilter(Sensors.windDirnSensor, 20);
	seeingFilter   = new AveragingFilter(Sensors.seeingSensor,   10);
	azLimitFilter  = new AveragingFilter(Sensors.azLimitSensor,  5);
	altLimitFilter = new AveragingFilter(Sensors.altLimitSensor, 5);
	rotLimitFilter = new AveragingFilter(Sensors.rotLimitSensor, 5);

	// Drive node states.
	List NODE_STATES = new Vector();
	NODE_STATES.add(new Integer(TCS_Status.MOTION_INPOSITION));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_STOPPED));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_MOVING));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_TRACKING));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_OFF_LINE));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_OVERRIDE));
	NODE_STATES.add(new Integer(TCS_Status.STATE_ERROR));
	NODE_STATES.add(new Integer(TCS_Status.STATE_UNKNOWN));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_WARNING));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_EXPIRED));
	NODE_STATES.add(new Integer(TCS_Status.MOTION_LIMIT));

	azStateFilter  = new ModalFilter(Sensors.azStateSensor,  5, NODE_STATES, TCS_Status.STATE_UNKNOWN);
	azStateFilter.setName("AZ_STATE_FILTER");
	altStateFilter = new ModalFilter(Sensors.altStateSensor, 5, NODE_STATES, TCS_Status.STATE_UNKNOWN);
	altStateFilter.setName("ALT_STATE_FILTER");
	rotStateFilter = new ModalFilter(Sensors.rotStateSensor, 5, NODE_STATES, TCS_Status.STATE_UNKNOWN);
	rotStateFilter.setName("ROT_STATE_FILTER");
	rotStateFilter.setSpy(true);
	rotStateFilter.setSpyLog("SPY");

	// Move states are 220..234
	focusStateFilter              = new AveragingFilter(Sensors.focusStateSensor,             5);
	primaryMirrorCoverStateFilter = new AveragingFilter(Sensors.primaryMirrorCoverStateSensor,5);
	foldMirrorStateFilter         = new AveragingFilter(Sensors.foldMirrorStateSensor,        5);
	enclosureStateFilter          = new AveragingFilter(Sensors.enclosureStateSensor,         5);

	// TCS STATE_XX codes run from 460 - 470 ... need to look at how ModalFilter stores its values.	
	networkStateFastFilter = 
	    new ModalFilter(Sensors.networkStateSensor,             5, 460, 474, TCS_Status.STATE_UNKNOWN);
	networkStateFastFilter.setName("NETWK_STATE_FILTER_FAST"); 
	
	List NETSTATES = new Vector();
	NETSTATES.add(new Integer(TCS_Status.STATE_OKAY));
	NETSTATES.add(new Integer(TCS_Status.STATE_ERROR));
	networkStateSlowFilter =
	         new ModalFilter(Sensors.networkStateSensor,             5, 460, 474, TCS_Status.STATE_UNKNOWN);
	    //new SteadyStateFilter(Sensors.networkStateSensor, 
			//	  100, 
			//	  NETSTATES,
			//	  TCS_Status.STATE_UNKNOWN);
	networkStateSlowFilter.setName("NETWK_STATE_FILTER_SLOW");
	

	// Mech Posn states. #####
	List MECH_POSITION_STATES = new Vector();
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_IN));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_OUT));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_CLOSED));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_OPEN));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_PARTIAL));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_UNKNOWN));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_STOWED));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_PORT_1));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_PORT_2));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_PORT_3));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_PORT_4));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_INLINE));
	MECH_POSITION_STATES.add(new Integer(TCS_Status.POSITION_RETRACT));
	enclosurePositionFilter =
	     new ModalFilter(Sensors.enclosurePositionSensor, 5, MECH_POSITION_STATES, TCS_Status.POSITION_UNKNOWN);
	enclosurePositionFilter.setName("ENC_POSN_FILTER");
	
	mirrcoverPositionFilter =
	    new ModalFilter(Sensors.mirrcoverPositionSensor, 5, MECH_POSITION_STATES, TCS_Status.POSITION_UNKNOWN);
	mirrcoverPositionFilter.setName("MIRRCOVER_POSN_FILTER");

	// Power states.
	List POWER_STATES = new Vector();
	POWER_STATES.add(new Integer(TCS_Status.POWER_STATE_SHUTDOWN));
	POWER_STATES.add(new Integer(TCS_Status.POWER_STATE_RESTART));
	POWER_STATES.add(new Integer(TCS_Status.POWER_STATE_OKAY));
	powerStateFilter = 
	    new ModalFilter(Sensors.powerStateSensor, 5,  POWER_STATES, TCS_Status.POWER_STATE_OKAY);
	powerStateFilter.setName("POWER_STATE_FILTER");
 
	networkControlStateFilter = 
	    new ModalFilter(Sensors.networkControlStateSensor,      5,  460, 474, TCS_Status.STATE_UNKNOWN);
	networkControlStateFilter.setName("NETWK_CTRL_STATE_FILTER");


	engineeringOverrideStateFilter = 
	    new ModalFilter(Sensors.engineeringOverrideStateSensor, 5,  460, 474, TCS_Status.STATE_UNKNOWN);
	engineeringOverrideStateFilter.setName("ENG_OVR_STATE_FILTER");


	List SYSTEM_STATES = new Vector();
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_OKAY));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_WARN));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_SAFE));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_INIT));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_SUSPENDED));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_FAILED));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_STANDBY));
	SYSTEM_STATES.add(new Integer(TCS_Status.STATE_INVALID));

	systemStateFilter = 
	    new ModalFilter(Sensors.systemStateSensor,  3,  SYSTEM_STATES, TCS_Status.STATE_UNKNOWN); ///???
	systemStateFilter.setName("SYS_STATE_FILTER");

	
    }

}

/** $Log: Filters.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
