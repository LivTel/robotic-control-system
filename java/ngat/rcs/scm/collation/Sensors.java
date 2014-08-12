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

import ngat.message.RCS_TCS.*;

import java.io.*;

/** Holds references to the various environment and subsystem Sensors.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Sensors.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/Sensors.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Sensors {

    /** Detects the current meteorology.rain state.*/
    public static Sensor rainSensor;

    /** Detects the current meteorology.windSpeed value.*/
    public static Sensor windSensor;

    /** Detects the current meteorology.windDirn value.*/
    public static Sensor windDirnSensor;

    /** Detects the current autoguider.fwhm value.*/
    public static Sensor seeingSensor;

    // Limits.

    /** Detects the current time to limits in Azimuth.*/
    public static Sensor azLimitSensor;

    /** Detects the current time to limits in Altitude.*/
    public static Sensor altLimitSensor;
    
    /** Detects the current time to limits in Cassegrain Rotation.*/
    public static Sensor rotLimitSensor;

    // Primary mechanisms.

    /** Detects the current Azimuth node state.*/
    public static Sensor azStateSensor;

    /** Detects the current Altitude node state.*/
    public static Sensor altStateSensor;
    
    /** Detects the current Cassegrain Rotator node state.*/
    public static Sensor rotStateSensor;

    // Subsidiary mechanisms.

    /** Detects the current Focus node state.*/
    public static Sensor focusStateSensor;
    
    /** Detects the current Primary Mirror Cover node state.*/
    public static Sensor primaryMirrorCoverStateSensor;

    /** Detects the current Fold Mirror node state.*/
    public static Sensor foldMirrorStateSensor;

    /** Detects the current Enclosure node state ( ##combined for now not indiv## ).*/
    public static Sensor enclosureStateSensor;
 
    /** Detects the current Enclosure Position.*/
    public static Sensor enclosurePositionSensor;

    /** Detects the current Enclosure Position.*/
    public static Sensor mirrcoverPositionSensor;

    /** Detects Network state.*/
    public static Sensor networkStateSensor;

    /** Detects Control state.*/
    public static Sensor networkControlStateSensor;

    /** Detects Control state.*/
    public static Sensor engineeringOverrideStateSensor;

    /** Detects Power state.*/
    public static Sensor powerStateSensor;

    /** Detects System (MCP) state.*/
    public static Sensor systemStateSensor;

    /**#### Temp detects RATCAM PSU Low Voltage (+ve) Supply.*/
    public static Sensor ratcamPSULowVoltagePlusSensor;

    /**#### Temp detects RATCAM PSU Low Voltage (-ve) Supply.*/
    public static Sensor ratcamPSULowVoltageMinusSensor;
    
    /**#### Temp detects RATCAM PSU High Voltage Supply.*/
    public static Sensor ratcamPSUHighVoltageSensor;

    /**#### Temp detects RATCAM Dewar Temperature.*/
    public static Sensor ratcamDewarTemperatureSensor;

    /**#### Temp detects RATCAM Utility Board Temperature.*/
    public static Sensor ratcamUtilityBoardTemperatureSensor;

    /**#### Temp detects RATCAM Network State.*/
    public static Sensor ratcamNetworkStateSensor;

    /**#### Temp detects CCD3CAM Network State.*/
    public static Sensor ccd3camNetworkStateSensor;

    /** Carry out configuration using the settings in the specified File.
     * @param configFile The file holding the settings.*/
    public static void configure(File configFile) {}

    /** Carry out default setup.*/
    public static void defaultSetup() {
	
	windSensor = new PoolSensor("SS_WIND_SPEED", false) {
	    
	    // /** @return The latest measured wind speed.*/
	    public void sample() {
		// dr = StatusPool.getDouble("METEO", "WIND_SPEED");
		dr   = StatusPool.latest().meteorology.windSpeed;
		time = StatusPool.latest().meteorology.timeStamp;
		//System.err.println("Readout WindSpeed at: "+time);
	    }
	};

	windDirnSensor = new PoolSensor("SS_WIND_DIRN", false) {
	   
	    // /** @return The latest measured wind direction.*/
	    public void sample() {
		dr   = StatusPool.latest().meteorology.windDirn;
		time = StatusPool.latest().meteorology.timeStamp;
		//System.err.println("Readout WindDirn at: "+time);
	    }
	};

	seeingSensor = new  PoolSensor("SS_AG_SEEING", false) {
	
	    // /** @return The latest measured autoguider fwhm.*/
	    public void sample() {
		dr   = StatusPool.latest().autoguider.fwhm;
		time = StatusPool.latest().autoguider.timeStamp;
		//System.err.println("Readout Ag-Seeing at:"+time);
	    }
	};

	rainSensor = new  PoolSensor("SS_RAIN", true) {
	    
	    //  /** @return The latest rain status.*/
	    public void sample() {
		ir   = StatusPool.latest().meteorology.rainState;
		time = StatusPool.latest().meteorology.timeStamp;
		//System.err.println("Readout Rain at: "+time);
	    }
	};

	azLimitSensor = new  PoolSensor("SS_AZ_LIMIT", false) {

	    //  /** @return The latest time to Azimuth limit.

	    public void sample() {
		dr   = StatusPool.latest().limits.timeToAzLimit;
		time = StatusPool.latest().limits.timeStamp;
		//System.err.println("Readout Az Limits at: "+time);
	    }
	};

	altLimitSensor = new  PoolSensor("SS_ALT_LIMIT", false) {

	    //  /** @return The latest time to Altitude limit.

	    public void sample() {
		dr   = StatusPool.latest().limits.timeToAltLimit;
		time = StatusPool.latest().limits.timeStamp;
		//System.err.println("Readout Alt Limits at: "+time);
	    }
	};
	
	rotLimitSensor = new  PoolSensor("SS_ROT_LIMIT", false) {
	    
	    //  /** @return The latest time to Cass Rotator limit.
	    
	    public void sample() {
		dr   = StatusPool.latest().limits.timeToRotLimit;
		time = StatusPool.latest().limits.timeStamp;
		//System.err.println("Readout Rot Limits at: "+time);
	    }
	};

	azStateSensor = new  PoolSensor("SS_AZ_STATE", true) {
	    
	    //  /** @return The latest Azimuth node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.azStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};
	
	altStateSensor = new  PoolSensor("SS_ALT_STATE", true) {
	    
	    //  /** @return The latest Altitude node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.altStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};

	rotStateSensor = new  PoolSensor("SS_ROT_STATE", true) {
	    
	    //  /** @return The latest Cass Rotator node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.rotStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};

	focusStateSensor = new  PoolSensor("SS_FOCUS_STATE", true) {
	    
	    //  /** @return The latest Focus  node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.secMirrorStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};
    
	primaryMirrorCoverStateSensor= new  PoolSensor("SS_PRIM_MIRR_STATE", true) {
	    
	    //  /** @return The latest Primary Mirror Cover  node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.primMirrorCoverStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};

	foldMirrorStateSensor= new  PoolSensor("SS_FOLD_MIRR_STATE", true) {
	    
	    //  /** @return The latest Fold Mirror node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.foldMirrorStatus;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};

	enclosureStateSensor= new  PoolSensor("SS_ENC_STATE", true) {
	    
	    //  /** @return The latest Enclosure (1) node state.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.encShutter1Status;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};
	
	enclosurePositionSensor =  new  PoolSensor("SS_ENC_POSN", true) {
	    
	    //  /** @return The latest Enclosure Position.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.encShutter1Pos;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};

	
	mirrcoverPositionSensor =  new  PoolSensor("SS_MIRR_COVER_POSN", true) {
	    
	    //  /** @return The latest Enclosure Position.
	    
	    public void sample() {
		ir   = StatusPool.latest().mechanisms.primMirrorCoverPos;
		time = StatusPool.latest().mechanisms.timeStamp;		
	    }
	};
	


	networkStateSensor  = new PoolSensor("SS_NETWORK", true) {

	    //  /** @return The latest network state. */
	    
		public void sample() {
		    ir   = StatusPool.latest().network.networkState;
		    time = StatusPool.latest().network.timeStamp;
		    //System.err.println("NSS-Sample() - Produced NetState "+TCS_Status.codeString(ir)+" at: "+new Date(time));
		}
	    };
	
	powerStateSensor = new PoolSensor("SS_POWER", true) {

	    public void sample() {
		ir   = (StatusPool.latest().state.systemShutdownFlag ? TCS_Status.POWER_STATE_SHUTDOWN :
			(StatusPool.latest().state.systemRestartFlag ? TCS_Status.POWER_STATE_RESTART :
			 TCS_Status.POWER_STATE_OKAY));
		time = StatusPool.latest().state.timeStamp;		
	    }
	};

	//systemStateSensor = new DebugSensor("SS_sys.info", true);

	systemStateSensor = new PoolSensor("SS_SYSTEM", true) {
	    public void sample() {
		ir   = StatusPool.latest().state.telescopeState;
		time = StatusPool.latest().state.timeStamp;		
	    }
	};
	
	networkControlStateSensor = new PoolSensor("SS_NET_CONTROL", true) {

	    public void sample() {
		ir   = StatusPool.latest().state.networkControlState;
		time = StatusPool.latest().state.timeStamp;		
	    }
	};
	
	engineeringOverrideStateSensor = new PoolSensor("SS_ENG_CONTROL", true) {

	    public void sample() {
		ir   = StatusPool.latest().state.engineeringOverrideState;
		time = StatusPool.latest().state.timeStamp;		
	    }
	};
	
	
    }
    
}

/** $Log: Sensors.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
