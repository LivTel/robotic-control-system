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

import ngat.rcs.scm.collation.*;

import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;

/** Holds references to each of the Monitors associated with the RCS rulesets
 * for environmental and subsystem feedback.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Monitors.java,v 1.2 2007/07/05 11:26:37 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Monitors.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class Monitors {

    /** Stores the List of Monitors.*/
    protected static List monitors;
    
    /** Carries out Monitor triggering.*/
    protected static MonitorThread monitorThread;

    /** Monitors the rain sensor readings.*/
    public static Monitor rainMonitor;
    
    /** Monitors the wind sensor readings.*/
    public static Monitor windMonitor;
    
    /** Monitors the seeing.*/
    public static Monitor seeingMonitor;

    /** Monitors Mechanism Time-to-limits.*/
    public static Monitor mechLimitMonitor;

    /** Monitors Mechanism States.*/
    public static Monitor mechStateMonitor;

    /** Monitors Network State.*/
    public static Monitor networkStateMonitor;
    
    /** Monitors Power State.*/
    public static Monitor powerStateMonitor;

    /** Monitors Control State.*/
    public static Monitor controlStateMonitor;

    /** Monitors System (MCP) State.*/
    public static Monitor systemStateMonitor;
   

    static {
	monitors      = new Vector();
	monitorThread = new MonitorThread();
    }
    
    /** Carry out configuration using the settings in the specified File.
     * @param configFile The file holding the settings.*/
    public static void configure(File configFile) {}

    /** Configure the Monitors with default settings.*/
    public static void defaultSetup() {
	Logger logger = LogManager.getLogger("BOOT");
	
	rainMonitor = new Monitor("RAIN_MONITOR");
	monitors.add(rainMonitor);
	rainMonitor.setPeriod(10000L);

	Rule    rainAlert        = new SelectRule(Filters.rainFilter, TCS_Status.RAIN_CLEAR, false);
	Ruleset rainAlertRules   = new SimpleRuleset(rainAlert);
	rainMonitor.associateRuleset(rainAlertRules, "RAIN_ALERT");
		
	Rule    rainClear        = new SelectRule(Filters.rainFilter, TCS_Status.RAIN_CLEAR, true);
	Ruleset rainClearRules   = new SimpleRuleset(rainClear);
	rainMonitor.associateRuleset(rainClearRules, "RAIN_CLEAR");
	
	logger.log(1, "Monitors","Initialized [RainMonitor]");
	
	Rule    windAlertRule    = new SimpleThresholdRule(Filters.windFilter, 50.0, SimpleThresholdRule.UP);
	Ruleset windAlertRules   = new SimpleRuleset(windAlertRule);
	windMonitor = new Monitor("WIND_MONITOR");
	windMonitor.setPeriod(10000L);
	monitors.add(windMonitor);
	windMonitor.associateRuleset(windAlertRules, "WIND_ALERT");
	
	Rule    windClearRule    = new SimpleThresholdRule(Filters.windFilter, 15.0, SimpleThresholdRule.DOWN);
	Ruleset windClearRules   = new SimpleRuleset(windClearRule);
	windMonitor.associateRuleset(windClearRules, "WIND_CLEAR");
	
	logger.log(1, "Monitors","Initialized [WindMonitor]");
       
	// WIND_NORTH_ALERT

	//Rule    seeingAlert      = new SimpleThresholdRule(Filters.seeingFilter, 0.4, SimpleThresholdRule.UP);
	// Temp -- due to SimTCS returns PIXELS not arcsec at present.
	seeingMonitor    = new Monitor("SEEING_MONITOR");
	seeingMonitor.setPeriod(30000L);
	monitors.add(seeingMonitor);

	Rule    seeingAlert      = new SimpleThresholdRule(Filters.seeingFilter, 40, SimpleThresholdRule.UP);
	Ruleset badSeeingRules   = new SimpleRuleset(seeingAlert);	
	seeingMonitor.associateRuleset(badSeeingRules,  "BAD_SEEING_ALERT");
	
	Rule    seeingClear      = new SimpleThresholdRule(Filters.seeingFilter, 30, SimpleThresholdRule.DOWN);
	Ruleset goodSeeingRules  = new SimpleRuleset(seeingClear);
	seeingMonitor.associateRuleset(goodSeeingRules, "BAD_SEEING_CLEAR");

	logger.log(1, "Monitors", "Initialized [SeeingMonitor]");
	
	// Limits.
	Rule    azLimitSoftRule   = new SimpleThresholdRule(Filters.azLimitFilter,  120.0, SimpleThresholdRule.DOWN);
	Ruleset azLimitSoftRules  = new SimpleRuleset(azLimitSoftRule);
	Rule    altLimitSoftRule  = new SimpleThresholdRule(Filters.altLimitFilter, 120.0, SimpleThresholdRule.DOWN);
	Ruleset altLimitSoftRules = new SimpleRuleset(altLimitSoftRule);
	Rule    rotLimitSoftRule  = new SimpleThresholdRule(Filters.rotLimitFilter, 120.0, SimpleThresholdRule.DOWN);
	Ruleset rotLimitSoftRules = new SimpleRuleset(rotLimitSoftRule);
	
	Rule    azLimitHardRule   = new SimpleThresholdRule(Filters.azLimitFilter,  60.0, SimpleThresholdRule.DOWN);
	Ruleset azLimitHardRules  = new SimpleRuleset(azLimitHardRule);
	Rule    altLimitHardRule  = new SimpleThresholdRule(Filters.altLimitFilter, 60.0, SimpleThresholdRule.DOWN);
	Ruleset altLimitHardRules = new SimpleRuleset(altLimitHardRule);
	Rule    rotLimitHardRule  = new SimpleThresholdRule(Filters.rotLimitFilter, 60.0, SimpleThresholdRule.DOWN);
	Ruleset rotLimitHardRules = new SimpleRuleset(rotLimitHardRule);
	
	Rule    azLimitClearRule  = new SimpleThresholdRule(Filters.azLimitFilter,  300.0, SimpleThresholdRule.UP);
	Ruleset azLimitClearRules = new SimpleRuleset(azLimitClearRule);
	Rule    altLimitClearRule = new SimpleThresholdRule(Filters.altLimitFilter, 300.0, SimpleThresholdRule.UP);
	Ruleset altLimitClearRules= new SimpleRuleset(altLimitClearRule);
	Rule    rotLimitClearRule = new SimpleThresholdRule(Filters.rotLimitFilter, 300.0, SimpleThresholdRule.UP);
	Ruleset rotLimitClearRules= new SimpleRuleset(rotLimitClearRule);
	
	mechLimitMonitor = new Monitor("LIMIT_MONITOR");
	mechLimitMonitor.setPeriod(10000L);
	monitors.add(mechLimitMonitor);
	mechLimitMonitor.associateRuleset(azLimitSoftRules,  "AZ_LIMIT_SOFT_ALERT");
	mechLimitMonitor.associateRuleset(altLimitSoftRules, "ALT_LIMIT_SOFT_ALERT");
	mechLimitMonitor.associateRuleset(rotLimitSoftRules, "ROT_LIMIT_SOFT_ALERT");

	mechLimitMonitor.associateRuleset(azLimitHardRules,  "AZ_LIMIT_HARD_ALERT");
	mechLimitMonitor.associateRuleset(altLimitHardRules, "ALT_LIMIT_HARD_ALERT");
	mechLimitMonitor.associateRuleset(rotLimitHardRules, "ROT_LIMIT_HARD_ALERT");

	mechLimitMonitor.associateRuleset(azLimitClearRules, "AZ_LIMIT_CLEAR");
	mechLimitMonitor.associateRuleset(altLimitClearRules,"ALT_LIMIT_CLEAR");
	mechLimitMonitor.associateRuleset(rotLimitClearRules,"ROT_LIMIT_CLEAR");
	
	logger.log(1, "Monitors", "Initialized [MechanismLimitMonitor]");

	// Mechanisms.

	// Azimuth.
	Rule    azMechInPositionRule = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_INPOSITION, true);
	Rule    azMechStoppedRule    = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_STOPPED,    true);
	Rule    azMechMovingRule     = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_MOVING,     true);
	Rule    azMechTrackingRule   = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_TRACKING,   true);
	Rule    azMechOfflineRule    = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_OFF_LINE,   true);
	Rule    azMechOverrideRule   = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_OVERRIDE,   true);
	Rule    azMechErrorRule      = new SelectRule(Filters.azStateFilter, TCS_Status.STATE_ERROR,       true);
	Rule    azMechUnknownRule    = new SelectRule(Filters.azStateFilter, TCS_Status.STATE_UNKNOWN,     true);   
	Rule    azMechWarningRule    = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_WARNING,    true);
	Rule    azMechExpiredRule    = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_EXPIRED,    true);
	Rule    azMechLimitRule      = new SelectRule(Filters.azStateFilter, TCS_Status.MOTION_LIMIT,      true);

	DisjunctiveRuleset azMechFaultRules  = new DisjunctiveRuleset();
	azMechFaultRules.addRule(azMechOfflineRule);
	azMechFaultRules.addRule(azMechOverrideRule);
	//azMechFaultRules.addRule(azMechErrorRule);
	azMechFaultRules.addRule(azMechUnknownRule);

	DisjunctiveRuleset azMechOkayRules  = new DisjunctiveRuleset();
	azMechOkayRules.addRule(azMechInPositionRule);
	azMechOkayRules.addRule(azMechStoppedRule);
	azMechOkayRules.addRule(azMechMovingRule);
	azMechOkayRules.addRule(azMechTrackingRule);
	azMechOkayRules.addRule(azMechWarningRule);
	azMechOkayRules.addRule(azMechExpiredRule);
	azMechOkayRules.addRule(azMechLimitRule);
	azMechOkayRules.addRule(azMechErrorRule);

	// Altitude.
	Rule    altMechInPositionRule = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_INPOSITION, true);
	Rule    altMechStoppedRule    = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_STOPPED,    true);
	Rule    altMechMovingRule     = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_MOVING,     true);
	Rule    altMechTrackingRule   = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_TRACKING,   true);
	Rule    altMechOfflineRule    = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_OFF_LINE,   true);
	Rule    altMechOverrideRule   = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_OVERRIDE,   true);
	Rule    altMechErrorRule      = new SelectRule(Filters.altStateFilter, TCS_Status.STATE_ERROR,       true);
	Rule    altMechUnknownRule    = new SelectRule(Filters.altStateFilter, TCS_Status.STATE_UNKNOWN,     true);
	Rule    altMechWarningRule    = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_WARNING,    true);
	Rule    altMechExpiredRule    = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_EXPIRED,    true);
	Rule    altMechLimitRule      = new SelectRule(Filters.altStateFilter, TCS_Status.MOTION_LIMIT,      true);

	DisjunctiveRuleset altMechFaultRules  = new DisjunctiveRuleset();
	altMechFaultRules.addRule(altMechOfflineRule);
	altMechFaultRules.addRule(altMechOverrideRule);
	//altMechFaultRules.addRule(altMechErrorRule);
	altMechFaultRules.addRule(altMechUnknownRule);

	DisjunctiveRuleset altMechOkayRules  = new DisjunctiveRuleset();
	altMechOkayRules.addRule(altMechInPositionRule);
	altMechOkayRules.addRule(altMechStoppedRule);
	altMechOkayRules.addRule(altMechMovingRule);
	altMechOkayRules.addRule(altMechTrackingRule);
	altMechOkayRules.addRule(altMechWarningRule);
	altMechOkayRules.addRule(altMechExpiredRule);
	altMechOkayRules.addRule(altMechLimitRule);
	altMechOkayRules.addRule(altMechErrorRule);

	// Rotator.
	Rule    rotMechInPositionRule = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_INPOSITION, true);
	Rule    rotMechStoppedRule    = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_STOPPED,    true);
	Rule    rotMechMovingRule     = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_MOVING,     true);
	Rule    rotMechTrackingRule   = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_TRACKING,   true);
	Rule    rotMechOfflineRule    = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_OFF_LINE,   true);
	Rule    rotMechOverrideRule   = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_OVERRIDE,   true);
	Rule    rotMechErrorRule      = new SelectRule(Filters.rotStateFilter, TCS_Status.STATE_ERROR,       true);
	Rule    rotMechUnknownRule    = new SelectRule(Filters.rotStateFilter, TCS_Status.STATE_UNKNOWN,     true);
	Rule    rotMechWarningRule    = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_WARNING,    true);	
	Rule    rotMechExpiredRule    = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_EXPIRED,    true);
	Rule    rotMechLimitRule      = new SelectRule(Filters.rotStateFilter, TCS_Status.MOTION_LIMIT,      true);

	DisjunctiveRuleset rotMechFaultRules  = new DisjunctiveRuleset();
	rotMechFaultRules.addRule(rotMechOfflineRule);
	rotMechFaultRules.addRule(rotMechOverrideRule);
	//rotMechFaultRules.addRule(rotMechErrorRule);
	rotMechFaultRules.addRule(rotMechUnknownRule);

	DisjunctiveRuleset rotMechOkayRules  = new DisjunctiveRuleset();
	rotMechOkayRules.addRule(rotMechInPositionRule);
	rotMechOkayRules.addRule(rotMechStoppedRule);
	rotMechOkayRules.addRule(rotMechMovingRule);
	rotMechOkayRules.addRule(rotMechTrackingRule);
	rotMechOkayRules.addRule(rotMechWarningRule);
	rotMechOkayRules.addRule(rotMechExpiredRule);
	rotMechOkayRules.addRule(rotMechLimitRule);
	rotMechOkayRules.addRule(rotMechErrorRule);

	// Sub.Mech OFF
	Rule    focusMechOfflineRule  = new SelectRule(Filters.focusStateFilter, TCS_Status.STATE_OKAY, false);
	Ruleset focusMechOfflineRules = new SimpleRuleset(focusMechOfflineRule);
	
	Rule    pMCMechOfflineRule    = 
	    new SelectRule(Filters.primaryMirrorCoverStateFilter, TCS_Status.STATE_OKAY, false);
	Ruleset pMCMechOfflineRules   = new SimpleRuleset(pMCMechOfflineRule);
	
	Rule    foldMirrorMechOfflineRule  = 
	    new SelectRule(Filters.foldMirrorStateFilter, TCS_Status.STATE_OKAY, false);
	Ruleset foldMirrorMechOfflineRules = new SimpleRuleset(foldMirrorMechOfflineRule);
	
	Rule    enclosureMechOfflineRule  = 
	    new SelectRule(Filters.enclosureStateFilter, TCS_Status.STATE_OKAY, false);
	Ruleset enclosureMechOfflineRules = new SimpleRuleset(enclosureMechOfflineRule);

	// Sub.Mech ON.	
	Rule    focusMechOnlineRule  = new SelectRule(Filters.focusStateFilter, TCS_Status.STATE_OKAY, true);
	Ruleset focusMechOnlineRules = new SimpleRuleset(focusMechOnlineRule);
	
	Rule    pMCMechOnlineRule    = 
	    new SelectRule(Filters.primaryMirrorCoverStateFilter, TCS_Status.STATE_OKAY, true);
	Ruleset pMCMechOnlineRules   = new SimpleRuleset(pMCMechOnlineRule);
	
	Rule    foldMirrorMechOnlineRule  = 
	    new SelectRule(Filters.foldMirrorStateFilter, TCS_Status.STATE_OKAY, true);
	Ruleset foldMirrorMechOnlineRules = new SimpleRuleset(foldMirrorMechOnlineRule);
	
	Rule    enclosureMechOnlineRule  = 
	    new SelectRule(Filters.enclosureStateFilter, TCS_Status.STATE_OKAY, true);
	Ruleset enclosureMechOnlineRules = new SimpleRuleset(enclosureMechOnlineRule);


	// Sub. Mech Other States.
	Rule enclosureOpenRule =  
	    new SelectRule(Filters.enclosurePositionFilter, TCS_Status.POSITION_OPEN, true);
	Ruleset enclosureOpenRules = new SimpleRuleset(enclosureOpenRule);
	
	Rule enclosureClosedRule =  
	    new SelectRule(Filters.enclosurePositionFilter, TCS_Status.POSITION_CLOSED, true);
	Ruleset enclosureClosedRules = new SimpleRuleset(enclosureClosedRule);


	// Sub. Mech Other States.
	Rule mirrcoverOpenRule =  
	    new SelectRule(Filters.mirrcoverPositionFilter, TCS_Status.POSITION_OPEN, true);
	Ruleset mirrcoverOpenRules = new SimpleRuleset(mirrcoverOpenRule);
	
	Rule mirrcoverClosedRule =  
	    new SelectRule(Filters.mirrcoverPositionFilter, TCS_Status.POSITION_CLOSED, true);
	Ruleset mirrcoverClosedRules = new SimpleRuleset(mirrcoverClosedRule);


	mechStateMonitor = new Monitor("MECHANISM_STATE");
	mechStateMonitor.setPeriod(10000L); 
	monitors.add(mechStateMonitor);
	mechStateMonitor.associateRuleset(azMechFaultRules,  "AZIMUTH_ERROR");
	mechStateMonitor.associateRuleset(altMechFaultRules, "ALTITUDE_ERROR");
	mechStateMonitor.associateRuleset(rotMechFaultRules, "ROTATOR_ERROR");

	mechStateMonitor.associateRuleset(azMechOkayRules,   "AZIMUTH_CLEAR");
	mechStateMonitor.associateRuleset(altMechOkayRules,  "ALTITUDE_CLEAR");
	mechStateMonitor.associateRuleset(rotMechOkayRules,  "ROTATOR_CLEAR");

	mechStateMonitor.associateRuleset(focusMechOfflineRules,      "FOCUS_ERROR");
	mechStateMonitor.associateRuleset(pMCMechOfflineRules,        "PMC_ERROR");
	mechStateMonitor.associateRuleset(foldMirrorMechOfflineRules, "FOLD_MIRROR_ERROR");
	mechStateMonitor.associateRuleset(enclosureMechOfflineRules,  "ENCLOSURE_ERROR");

	mechStateMonitor.associateRuleset(focusMechOnlineRules,       "FOCUS_CLEAR");
	mechStateMonitor.associateRuleset(pMCMechOnlineRules,         "PMC_CLEAR");
	mechStateMonitor.associateRuleset(foldMirrorMechOnlineRules,  "FOLD_MIRROR_CLEAR");
	mechStateMonitor.associateRuleset(enclosureMechOnlineRules,   "ENCLOSURE_CLEAR");
	mechStateMonitor.associateRuleset(enclosureOpenRules,         "ENCLOSURE_OPEN");
	mechStateMonitor.associateRuleset(enclosureClosedRules,       "ENCLOSURE_CLOSED");
	mechStateMonitor.associateRuleset(mirrcoverOpenRules,         "MIRRCOVER_OPEN");
	mechStateMonitor.associateRuleset(mirrcoverClosedRules,       "MIRRCOVER_CLOSED");


	logger.log(1, "Monitors", "Initialized [MechanismStateMonitor]");

	// Network. - ALERT if NOT state is OKAY !
	Rule    networkNoCommsAlertRule  = new SelectRule(Filters.networkStateFastFilter, TCS_Status.STATE_OKAY, false);
	Ruleset networkNoCommsAlertRules = new SimpleRuleset(networkNoCommsAlertRule);

	Rule    networkNoCommsClearRule  = new SelectRule(Filters.networkStateSlowFilter, TCS_Status.STATE_OKAY, true);
	Ruleset networkNoCommsClearRules = new SimpleRuleset(networkNoCommsClearRule);
	
	networkStateMonitor = new Monitor("NETWORK_MONITOR");
	networkStateMonitor.setPeriod(5000L);
	monitors.add(networkStateMonitor);
	networkStateMonitor.associateRuleset(networkNoCommsAlertRules, "NETWORK_COMMS_ALERT_FAST");
	networkStateMonitor.associateRuleset(networkNoCommsClearRules, "NETWORK_COMMS_CLEAR_SLOW");	
		
	logger.log(1, "Monitors", "Initialized [NetworkStateMonitor]");

	// Power. - One rule per possible value.
	Rule    powerRestartRule   = new SelectRule(Filters.powerStateFilter, TCS_Status.POWER_STATE_RESTART,  true);
	Ruleset powerRestartRules  = new SimpleRuleset(powerRestartRule);
	Rule    powerShutdownRule  = new SelectRule(Filters.powerStateFilter, TCS_Status.POWER_STATE_SHUTDOWN, true);
	Ruleset powerShutdownRules = new SimpleRuleset(powerShutdownRule);
	Rule    powerOkayRule      = new SelectRule(Filters.powerStateFilter, TCS_Status.POWER_STATE_OKAY,     true);
	Ruleset powerOkayRules     = new SimpleRuleset(powerOkayRule);

	powerStateMonitor = new Monitor("POWER_MONITOR");
	powerStateMonitor.setPeriod(5000L);
	monitors.add(powerStateMonitor);
	powerStateMonitor.associateRuleset(powerRestartRules,  "POWER_RESTART");
	powerStateMonitor.associateRuleset(powerShutdownRules, "POWER_SHUTDOWN");
	powerStateMonitor.associateRuleset(powerOkayRules,     "POWER_OKAY");

	logger.log(1, "Monitors", "Initialized [PowerStateMonitor]");
	
	// System (MCP). One rule per possible value.
	Rule    systemOkayRule     = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_OKAY, true);
	Rule    systemWarnRule     = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_WARN, true);
	DisjunctiveRuleset systemOkayRules = new DisjunctiveRuleset();
	// Okay and Warn states are both in a sense OKAY...
	systemOkayRules.addRule(systemOkayRule);
	systemOkayRules.addRule(systemWarnRule);
	Rule    systemSuspendRule  = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_SUSPENDED, true);
	Ruleset systemSuspendRules = new SimpleRuleset(systemSuspendRule);

	Rule    systemStandbyRule  = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_STANDBY, true);
	Ruleset systemStandbyRules = new SimpleRuleset(systemStandbyRule);

	Rule    systemSafeRule     = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_SAFE, true);
	Ruleset systemSafeRules    = new SimpleRuleset(systemSafeRule);

	Rule    systemFailedRule   = new SelectRule(Filters.systemStateFilter, TCS_Status.STATE_FAILED, true);
	Ruleset systemFailedRules  = new SimpleRuleset(systemFailedRule);

	systemStateMonitor = new Monitor("SYSTEM_STATE");
	systemStateMonitor.setPeriod(5000L);
	monitors.add(systemStateMonitor);

	systemStateMonitor.associateRuleset(systemOkayRules,    "SYSTEM_OKAY");	
	systemStateMonitor.associateRuleset(systemSafeRules,    "SYSTEM_SAFE");
	systemStateMonitor.associateRuleset(systemFailedRules,  "SYSTEM_FAILED");
	systemStateMonitor.associateRuleset(systemSuspendRules, "SYSTEM_SUSPEND");
	systemStateMonitor.associateRuleset(systemStandbyRules, "SYSTEM_STANDBY");

	logger.log(1, "Monitors", "Initialized [SystemStateMonitor]");

	// ControlState.
	Rule controlEnabledRule      = 
	    new SelectRule(Filters.networkControlStateFilter, TCS_Status.STATE_ENABLED, true);
	Rule overrideDisabledRule  = 
	    new SelectRule(Filters.engineeringOverrideStateFilter, TCS_Status.STATE_DISABLED, true);

	ConjunctiveRuleset controlEnabledRules = new ConjunctiveRuleset();
	controlEnabledRules.addRule(controlEnabledRule);
	controlEnabledRules.addRule(overrideDisabledRule);
	
	Rule controlDisabledRule = 
	    new SelectRule(Filters.networkControlStateFilter, TCS_Status.STATE_DISABLED, true);
	Rule overrideEngagedRule =
	     new SelectRule(Filters.engineeringOverrideStateFilter, TCS_Status.STATE_ENGAGED, true);
	
	DisjunctiveRuleset controlDisabledRules = new DisjunctiveRuleset();
	controlDisabledRules.addRule(controlDisabledRule);
	controlDisabledRules.addRule(overrideEngagedRule);

	controlStateMonitor = new Monitor("CONTROL_STATE");
	controlStateMonitor.setPeriod(5000L);
	monitors.add(controlStateMonitor);
	controlStateMonitor.associateRuleset(controlEnabledRules,  "CONTROL_INHIBIT_CLEAR");
	controlStateMonitor.associateRuleset(controlDisabledRules, "CONTROL_INHIBIT_ALERT");
	
	logger.log(1, "Monitors", "Initialized [ControlStateMonitor]");


	StatusPool.register(monitorThread, StatusPool.METEOROLOGY_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.AUTOGUIDER_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.NETWORK_UPDATE_EVENT);	
	StatusPool.register(monitorThread, StatusPool.LIMITS_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.STATE_UPDATE_EVENT);

	monitorThread.setInterval(1000L);

    }

    /** Starts the MonitorThread.*/
    public static void startMonitoring() {
	monitorThread.start();
    }
    
    /** Performs regular (at trigger interval) Triggering of Monitors.*/
    protected static class MonitorThread extends ControlThread implements Observer {
    
	/** Interval between triggering monitors.*/
	private long interval;

	/** Create the MonitorThread.*/
	MonitorThread() {
	    super("MONITOR_TRIG", true);	    
	}
	
	@Override
	public void initialise() {}

	/** Loop, test each Monitor per cycle - some may do nothing as they have their
	 * own wakeup period.*/
	@Override
	public void mainTask() {
	  
	    // May get woken up by a notification from the Observable(s) which can
	    // signal when new Sensor data is potentially available.
	    try {
		sleep(interval);
	    } catch (InterruptedException ix) {}
	    //System.err.println("MT::Wakeup");
	    Iterator it = monitors.iterator();
	    while (it.hasNext()) {
		Monitor monitor = (Monitor)it.next();
		monitor.trigger();
	    }

	}

	
	@Override
	public void shutdown() {}
	
	/**Sets the interval between triggering Monitors.*/
	public void setInterval(long interval) { this.interval = interval; }

	/** The implementation of this method from the java.util.Observable
	 * interface just calls interrupt() on this MonitorThread. 
	 * This is called by the Observable(s) to which this MonitorThread is attached 
	 * in order to inform the thread that new data is available for some of the
	 * Monitors it manages.	
	 * @param trigger The Observable (trigger source) which has been
	 * updated soas to force the triggering.
	 * @param arg Some data passed back from the trigger source. Currently
	 * this is ignored but may be used later?
	 */
	public void update(Observable trigger, Object arg) {
	    interrupt();
	}

    }

}

/** $Log: Monitors.java,v $
/** Revision 1.2  2007/07/05 11:26:37  snf
/** checkin
/**
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
