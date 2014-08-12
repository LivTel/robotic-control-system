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
package ngat.rcs.emm;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.util.*;
import java.text.*;
/** A set of constants defining the possible EVENTS which
 * can be fired in the RCS as part of the reactive aspect
 * of its operation. Events are classified and 
 * labelled under the following main categories:-
 * <table>
 * <td>ALERT</td>   <td>A condition which is likely to result
 * in some form of recovery action being taken. </td>
 *
 * <td>CLEAR</td>   <td>A condition indicating that an ALERT 
 * state no longer applies - operations can proceed as before 
 * the preceding ALERT was generated.</td>
 * </table>
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Events.java,v 1.1 2006/12/12 08:29:47 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/Events.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class Events {

    /** Holds the table of code strings.*/
    protected static Hashtable codes;
    
    protected static NumberFormat nf6;

    /** Create the code table.*/
    static {
	codes = new Hashtable();
	mapCodes(); 
	nf6 = NumberFormat.getInstance();
	nf6.setMaximumIntegerDigits(6);
	nf6.setMinimumIntegerDigits(6);
	nf6.setParseIntegerOnly(true);
	nf6.setGroupingUsed(false);
    }
    
    /** Event code (-1) indicating an unknown event type, may be returned
     * by the static method getEventCode(String).*/
    public static final int UNKNOWN_EVENT = -1;

    // --------------
    // Test Messages.
    // --------------

    /** Gamma Ray Burst Detection notified via GCN (or other). */
    public static final int GAMMA_BURST_ALERT = 6;
    
    /** Gamma Ray Burst Alert DONE. */
    public static final int GAMMA_BURST_CLEAR = 8;

    /** Gamma Ray Burst Position Updatenotified via GCN (or other). */
    public static final int GAMMA_BURST_POSITION_UPDATE = 7;

    /** Test Message Class #1.*/
    public static final int  TEST_MESSAGE_1 = 1;
    
    /** Test Message Class #2.*/
    public static final int  TEST_MESSAGE_2 = 2;
    
    /** Test Message Class #3.*/
    public static final int  TEST_MESSAGE_3 = 3; 
    
    /** Test Message Class #4.*/
    public static final int  TEST_MESSAGE_4 = 4;
    
    /** Test Message Class #5.*/
    public static final int  TEST_MESSAGE_5 = 5;

    // -----------------    
    // 100. Meteorology.
    // -----------------
    
    /** Rain has been detected.*/
    public static final int RAIN_ALERT = 101;
    
    /** Wind has exceeded threshold.*/
    public static final int WIND_ALERT = 102;
    
    /** Wind has exceeded threshold but steady from North.*/
    public static final int WIND_NORTH_ALERT = 103;

    /** Wind has exceeded threshold but steady from South.*/
    public static final int WIND_SOUTH_ALERT = 104;

    /** Wind is gusting too strongly.*/
    public static final int WIND_GUST_ALERT  = 105; 

    // ----------------
    // 200. Mechanisms.
    // ----------------
   
    /** The azimuth drive is in state RECOVERABLE_ERROR.*/
    public static final int AZIMUTH_ERROR = 201;
    
    /** The azimuth drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int AZIMUTH_FATAL = 202;

    /** The altitude drive is in state RECOVERABLE_ERROR.*/
    public static final int ALTITUDE_ERROR = 203;
    
    /** The altitude drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int ALTITUDE_FATAL = 204;
    
    /** The rotator drive is in state RECOVERABLE_ERROR.*/
    public static final int ROTATOR_ERROR = 205;
    
    /** The rotator drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int ROTATOR_FATAL = 206;
    
    /** The focus drive is in state RECOVERABLE_ERROR.*/
    public static final int FOCUS_ERROR = 207;
    
    /** The focus drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int FOCUS_FATAL = 208;
    
    /** The autoguider is in state RECOVERABLE_ERROR.*/
    public static final int AUTOGUIDER_ERROR = 209;

    /** The autoguider is in state NON_RECOVERABLE_ERROR.*/
    public static final int AUTOGUIDER_FATAL = 210;
    
    /** The ag-mirror drive is in state RECOVERABLE_ERROR.*/
    public static final int AG_MIRROR_ERROR = 211;
    
    /** The ag-mirror drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int AG_MIRROR_FATAL = 212;
    
    /** The ag-focus drive is in state RECOVERABLE_ERROR.*/
    public static final int AG_FOCUS_ERROR = 213;
    
    /** The ag-focus drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int AG_FOCUS_FATAL = 214;

    /** The ag-filter drive is in state RECOVERABLE_ERROR.*/
    public static final int AG_FILTER_ERROR = 215;
    
    /** The ag-filter drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int AG_FILTER_FATAL = 216;
    
    /** The enclosure drive is in state RECOVERABLE_ERROR.*/
    public static final int ENCLOSURE_ERROR = 217;
    
    /** The enclosure drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int ENCLOSURE_FATAL = 218;
    
    /** The fold-mirror drive is in state RECOVERABLE_ERROR.*/
    public static final int FOLD_MIRROR_ERROR = 219;
    
    /** The fold-mirror drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int FOLD_MIRROR_FATAL = 220;
    
    /** The mirror-cover drive is in state RECOVERABLE_ERROR.*/
    public static final int MIRROR_COVER_ERROR = 221;
    
    /** The mirror-cover drive is in state NON_RECOVERABLE_ERROR.*/
    public static final int MIRROR_COVER_FATAL = 222;
    
    /** The mirror-support system is in state RECOVERABLE_ERROR.*/
    public static final int MIRROR_SUPPORT_ERROR = 223;
    
    /** The mirror-support system is in state NON_RECOVERABLE_ERROR.*/
    public static final int MIRROR_SUPPORT_FATAL = 224;
    
    // ------------------------------
    // 300. Unexpected state changes.
    // ------------------------------
    // ## These need further definition/ expansion ##
    
    /** The Telescope state has changed from TRACKING 
     *or AUTOGUIDING to anything else.*/
    public static final int TRACKING_LOST_ALERT = 301;
    
    /** The TCS state has gone from OPERATIONAL to anything else.*/
    public static final int TCS_STATUS_ALERT = 302;

    /** The AG state is other than expected e.g. not LOCKED when autoguiding..*/
    public static final int GUIDE_STATUS_ALERT = 303;
    
    /** The AG guide mode is other than expected.*/
    public static final int GUIDE_MODE_ALERT = 304;
 
    /** The AG mirror state is other than expected.*/
    public static final int GUIDE_MIRROR_ALERT = 305;
    
    /** The AG focus state is other than expected.*/
    public static final int GUIDE_FOCUS_ALERT = 306;
    
    /** The AG filter state is other than expected.*/
    public static final int GUIDE_FILTER_ALERT = 307;
    
    /** The selected instrument is not as expected.*/
    public static final int INSTRUMENT_ALERT = 308;
    
    /** The azimuth drive state is other than expected.*/
    public static final int AZIMUTH_ALERT = 309;

    /** The altitude drive state is other than expected.*/
    public static final int ALTITUDE_ALERT = 310;
    
    /** The rotator drive state is other than expected.*/
    public static final int ROTATOR_ALERT = 311;
    
    /** The enclosure state is other than expected. - This
     * could happen e.g. if the MCP decided to close the
     * dome due to its perception of deteriorating weather
     * while the RCS had not yet made that decision.*/
    public static final int ENCLOSURE_ALERT = 312;

    public static final int FOLD_MIRROR_ALERT = 313;

    public static final int PRIMARY_MIRROR_ALERT = 314;

    public static final int FOCUS_ALERT = 316;

    public static final int WMS_STATUS_ALERT = 317;

    /** The system is not in NETWORK state when expected.*/
    public static final int CONTROL_STATE_ALERT = 318;
    
    /** The telescope is in an unexpected state.*/
    public static final int TELESCOPE_STATE_ALERT = 319;
    
    /** Power failure is imminent.*/
    public static final int POWER_FAILURE_ALERT = 320;

    // ---------------------------
    // 400. Instrument subsystems.
    // ---------------------------

    /** The RATCam Control System cannot be contacted - reboot?*/
    public static final int RAT_CAM_CCS_OFFLINE_ALERT = 401;

    public static final String ICS_RATCAM_NETWORK_OFFLINE = "ics.network.offline.RATCAM";
    
    /** The SupIRCam Control System cannot be contacted - reboot?*/
    public static final int SUPIR_CAM_CCS_OFFLINE_ALERT = 402;
    
    /** The NuView II Control System cannot be contacted.*/
    public static final int NUVIEW_SPEC_SCS_OFFLINE_ALERT = 403;

    /** The MES Control System cannot be contacted.*/
    public static final int MES_SPEC_SCS_OFFLINE_ALERT = 404;
    
    // -----------------
    // 500. Operational.
    // -----------------

    /** Seeing conditions have dropped below the required level.
     * This level needs setting for a given observation.*/
    public static final int BAD_SEEING_ALERT = 501;
    
    
    // --------------
    // CLEARS. (1000)
    // --------------
  
    /** The RAIN_ALERT has cleared.*/
    public static final int RAIN_CLEAR = 1101;

    /** The WIND_ALERT has cleared.*/
    public static final int WIND_CLEAR = 1102;
    
    /** The AZIMUTH_ALERT or ERROR has cleared.*/
    public static final int AZIMUTH_CLEAR = 1103;

    /** The ALTITUDE__ALERT or ERROR has cleared.*/
    public static final int ALTITUDE_CLEAR = 1104;
    
    /** The ROTATOR_ALERT or ERROR has cleared.*/
    public static final int ROTATOR_CLEAR = 1105;
    
    /** The FOCUS_ALERT or ERROR has cleared.*/
    public static final int FOCUS_CLEAR = 1106;
    
    /** The AUTOGUIDER_ALERT or ERROR has cleared.*/
    public static final int AUTOGUIDER_CLEAR = 1107;
    
    /** The AG_MIRROR_ALERT or ERROR has cleared.*/
    public static final int AG_MIRROR_CLEAR = 1108;
    
    /** The AG_FOCUS_ALERT or ERROR has cleared.*/
    public static final int AG_FOCUS_CLEAR = 1109;
    
    /** The AG_FILTER_ALERT or ERROR has cleared.*/
    public static final int AG_FILTER_CLEAR = 1110;
    
    /** The ENCLOSURE_ALERT or ERROR has cleared.*/
    public static final int ENCLOSURE_CLEAR = 1111;
    
    /** The FOLD_MIRROR_ALERT or ERROR has cleared.*/
    public static final int FOLD_MIRROR_CLEAR = 1112;
    
    /** The MIRROR_COVER_ALERT or ERROR has cleared.*/
    public static final int MIRROR_COVER_CLEAR = 1113;
    
    /** The  MIRROR_SUPPORT_ALERT or ERROR has cleared.*/
    public static final int MIRROR_SUPPORT_CLEAR = 1114;
    
    /** The TRACKING_LOST_ALERT has cleared.*/
    public static final int TRACKING_LOST_CLEAR = 1115;
    
    /** The TCS state has returned to OPERATIONAL.*/
    public static final int TCS_STATUS_CLEAR = 1116;
    
    /** The AG state is as expected.*/
    public static final int GUIDE_STATUS_CLEAR = 1117;
    
    /** The AG guide mode is as expected.*/
    public static final int GUIDE_MODE_CLEAR = 1118;
    
    /** The AG mirror state is as expected.*/
    public static final int GUIDE_MIRROR_CLEAR = 1119;
    
    /** The AG focus state is as expected.*/
    public static final int GUIDE_FOCUS_CLEAR = 1120;
    
    /** The AG filter state is as expected.*/
    public static final int GUIDE_FILTER_CLEAR = 1121;
    
    /** The selected instrument is as expected.*/
    public static final int INSTRUMENT_CLEAR = 1122;

    /** The WMS is functioning as expected.*/
    public static final int WMS_STATUS_CLEAR = 1123;
    
    /** The system has returned to NETWORK state.*/
    public static final int CONTROL_STATE_CLEAR = 1124;
    
    /** The telescope is in the expected state.*/
    public static final int TELESCOPE_STATE_CLEAR = 1125;

    /** The power failure has cleared.*/
     public static final int POWER_FAILURE_CLEAR  = 1126;
    
    /** The RATCam Control System can now be contacted.*/
    public static final int RAT_CAM_CCS_OFFLINE_CLEAR = 1127;
    
    /** The SupIRCam Control System can now be contacted.*/
    public static final int SUPIR_CAM_CCS_OFFLINE_CLEAR = 1128;
    
    /** The NuView II Control System can now be contacted.*/
    public static final int NUVIEW_SPEC_SCS_OFFLINE_CLEAR = 1129;

    /** The MES Control System can now be contacted.*/
    public static final int MES_SPEC_SCS_OFFLINE_CLEAR = 1130;
    
    /** Maps the event codes to their readable names.*/
    protected static void mapCodes() { 
	
	
	// -------------------
	// 000. Test Messages.
	// -------------------

	codes.put(new Integer(GAMMA_BURST_ALERT), "GAMMA_BURST_ALERT");
	codes.put(new Integer(GAMMA_BURST_CLEAR), "GAMMA_BURST_CLEAR");
	codes.put(new Integer(GAMMA_BURST_POSITION_UPDATE), "GAMMA_BURST_POSITION_UPDATE");
	codes.put(new Integer(TEST_MESSAGE_1),    "TEST_MESSAGE_1");
	codes.put(new Integer(TEST_MESSAGE_2),    "TEST_MESSAGE_2");
	codes.put(new Integer(TEST_MESSAGE_3),    "TEST_MESSAGE_3");
	codes.put(new Integer(TEST_MESSAGE_4),    "TEST_MESSAGE_4");
	codes.put(new Integer(TEST_MESSAGE_5),    "TEST_MESSAGE_5");
	
	// --------------
	// ALERTS. (0000)
	// --------------
	
	// -----------------    
	// 100. Meteorology.
	// -----------------
	
	codes.put(new Integer(RAIN_ALERT),       "RAIN_ALERT");	
	codes.put(new Integer(WIND_ALERT),       "WIND_ALERT");	
	codes.put(new Integer(WIND_NORTH_ALERT), "WIND_NORTH_ALERT");	
	codes.put(new Integer(WIND_SOUTH_ALERT), "WIND_SOUTH_ALERT");	
	codes.put(new Integer(WIND_GUST_ALERT),  "WIND_GUST_ALERT");
	
	// ----------------
	// 200. Mechanisms.
	// ----------------
	
	codes.put(new Integer(AZIMUTH_ERROR),  "AZIMUTH_ERROR");	
	codes.put(new Integer(AZIMUTH_FATAL),  "AZIMUTH_FATAL");	
	codes.put(new Integer(ALTITUDE_ERROR), "ALTITUDE_ERROR");	
	codes.put(new Integer(ALTITUDE_FATAL), "ALTITUDE_FATAL");	
	codes.put(new Integer(ROTATOR_ERROR),  "ROTATOR_ERROR");	
	codes.put(new Integer(ROTATOR_FATAL),  "ROTATOR_FATAL");
	
	codes.put(new Integer(FOCUS_ERROR),          "FOCUS_ERROR");	
	codes.put(new Integer(FOCUS_FATAL),          "FOCUS_FATAL");	
	codes.put(new Integer(AUTOGUIDER_ERROR),     "AUTOGUIDER_ERROR");	
	codes.put(new Integer(AUTOGUIDER_FATAL),     "AUTOGUIDER_FATAL");	
	codes.put(new Integer(AG_MIRROR_ERROR),      "AG_MIRROR_ERROR");	
	codes.put(new Integer(AG_MIRROR_FATAL),      "AG_MIRROR_FATAL");	
	codes.put(new Integer(AG_FOCUS_ERROR),       "AG_FOCUS_ERROR");	
	codes.put(new Integer(AG_FOCUS_FATAL),       "AG_FOCUS_FATAL");	
	codes.put(new Integer(AG_FILTER_ERROR),      "AG_FILTER_ERROR");	
	codes.put(new Integer(AG_FILTER_FATAL),      "AG_FILTER_FATAL");	
	codes.put(new Integer(ENCLOSURE_ERROR),      "ENCLOSURE_ERROR");	
	codes.put(new Integer(ENCLOSURE_FATAL),      "ENCLOSURE_FATAL");	
	codes.put(new Integer(FOLD_MIRROR_ERROR),    "FOLD_MIRROR_ERROR");	
	codes.put(new Integer(FOLD_MIRROR_FATAL),    "FOLD_MIRROR_FATAL");	
	codes.put(new Integer(MIRROR_COVER_ERROR),   "MIRROR_COVER_ERROR");	
	codes.put(new Integer(MIRROR_COVER_FATAL),   "MIRROR_COVER_FATAL");	
	codes.put(new Integer(MIRROR_SUPPORT_ERROR), "MIRROR_SUPPORT_ERROR");	
	codes.put(new Integer(MIRROR_SUPPORT_FATAL), "MIRROR_SUPPORT_FATAL");
	
	// ------------------------------
	// 300. Unexpected state changes.
	// ------------------------------
	codes.put(new Integer(TRACKING_LOST_ALERT),   "TRACKING_LOST_ALERT");	
	codes.put(new Integer(TCS_STATUS_ALERT),      "TCS_STATUS_ALERT");	
	codes.put(new Integer(GUIDE_STATUS_ALERT),    "GUIDE_STATUS_ALERT");	
	codes.put(new Integer(GUIDE_MODE_ALERT),      "GUIDE_MODE_ALERT");	
	codes.put(new Integer(GUIDE_MIRROR_ALERT),    "GUIDE_MIRROR_ALERT");	
	codes.put(new Integer(GUIDE_FOCUS_ALERT),     "GUIDE_FOCUS_ALERT");	
	codes.put(new Integer(GUIDE_FILTER_ALERT),    "GUIDE_FILTER_ALERT"); 	
	codes.put(new Integer(INSTRUMENT_ALERT),      "INSTRUMENT_ALERT");	
	codes.put(new Integer(AZIMUTH_ALERT),         "AZIMUTH_ALERT");	
	codes.put(new Integer(ALTITUDE_ALERT),        "ALTITUDE_ALERT");	
	codes.put(new Integer(ROTATOR_ALERT),         "ROTATOR_ALERT");	
	codes.put(new Integer(ENCLOSURE_ALERT),       "ENCLOSURE_ALERT");	
	codes.put(new Integer(FOLD_MIRROR_ALERT),     "FOLD_MIRROR_ALERT");	
	codes.put(new Integer(PRIMARY_MIRROR_ALERT),  "PRIMARY_MIRROR_ALERT");	
	codes.put(new Integer(FOCUS_ALERT),           "FOCUS_ALERT");	
	codes.put(new Integer(WMS_STATUS_ALERT),      "WMS_STATUS_ALERT");	
	codes.put(new Integer(CONTROL_STATE_ALERT),   "CONTROL_STATE_ALERT");	
	codes.put(new Integer(TELESCOPE_STATE_ALERT), "TELESCOPE_STATE_ALERT");
	codes.put(new Integer(POWER_FAILURE_ALERT),   "POWER_FAILURE_ALERT");
	// ---------------------------
	// 400. Instrument subsystems.
	// ---------------------------
	
	codes.put(new Integer(RAT_CAM_CCS_OFFLINE_ALERT),     "RAT_CAM_CCS_OFFLINE_ALERT"); 	
	codes.put(new Integer(SUPIR_CAM_CCS_OFFLINE_ALERT),   "SUPIR_CAM_CCS_OFFLINE_ALERT");	
	codes.put(new Integer(NUVIEW_SPEC_SCS_OFFLINE_ALERT), "NUVIEW_SPEC_SCS_OFFLINE_ALERT");	
	codes.put(new Integer(MES_SPEC_SCS_OFFLINE_ALERT),    "MES_SPEC_SCS_OFFLINE_ALERT"); 
	
	// -----------------
	// 500. Operational.
	// -----------------
	
	codes.put(new Integer( BAD_SEEING_ALERT), "BAD_SEEING_ALERT");
	
	
	// --------------
	// CLEARS. (1000+)
	// --------------
	
	codes.put(new Integer(RAIN_CLEAR),                    "RAIN_CLEAR");	
	codes.put(new Integer(WIND_CLEAR),                    "WIND_CLEAR");	
	codes.put(new Integer(AZIMUTH_CLEAR),                 "AZIMUTH_CLEAR"); 	
	codes.put(new Integer(ALTITUDE_CLEAR),                "ALTITUDE_CLEAR"); 	
	codes.put(new Integer(ROTATOR_CLEAR),                 "ROTATOR_CLEAR"); 	
	codes.put(new Integer(FOCUS_CLEAR),                   "FOCUS_CLEAR"); 	
	codes.put(new Integer(AUTOGUIDER_CLEAR),              "AUTOGUIDER_CLEAR");	
	codes.put(new Integer(AG_MIRROR_CLEAR),               "AG_MIRROR_CLEAR"); 	
	codes.put(new Integer(AG_FOCUS_CLEAR),                "AG_FOCUS_CLEAR");	
	codes.put(new Integer(AG_FILTER_CLEAR),               "AG_FILTER_CLEAR");	
	codes.put(new Integer(ENCLOSURE_CLEAR),               "ENCLOSURE_CLEAR");	
	codes.put(new Integer(FOLD_MIRROR_CLEAR),             "FOLD_MIRROR_CLEAR");	
	codes.put(new Integer(MIRROR_COVER_CLEAR),            "MIRROR_COVER_CLEAR"); 	
	codes.put(new Integer(MIRROR_SUPPORT_CLEAR),          "MIRROR_SUPPORT_CLEAR");	
	codes.put(new Integer(TRACKING_LOST_CLEAR),           "TRACKING_LOST_CLEAR");	
	codes.put(new Integer(TCS_STATUS_CLEAR),              "TCS_STATUS_CLEAR");	
	codes.put(new Integer(GUIDE_STATUS_CLEAR),            "GUIDE_STATUS_CLEAR"); 	
	codes.put(new Integer(GUIDE_MODE_CLEAR),              "GUIDE_MODE_CLEAR");	
	codes.put(new Integer(GUIDE_MIRROR_CLEAR),            "GUIDE_MIRROR_CLEAR"); 	
	codes.put(new Integer(GUIDE_FOCUS_CLEAR),             "GUIDE_FOCUS_CLEAR"); 	
	codes.put(new Integer(GUIDE_FILTER_CLEAR),            "GUIDE_FILTER_CLEAR");	
	codes.put(new Integer(INSTRUMENT_CLEAR),              "INSTRUMENT_CLEAR");	
	codes.put(new Integer(WMS_STATUS_CLEAR),              "WMS_STATUS_CLEAR");	
	codes.put(new Integer(CONTROL_STATE_CLEAR),           "CONTROL_STATE_CLEAR");	
	codes.put(new Integer(TELESCOPE_STATE_CLEAR),         "TELESCOPE_STATE_CLEAR");
	codes.put(new Integer(POWER_FAILURE_CLEAR),           "POWER_FAILURE_CLEAR");
	codes.put(new Integer(RAT_CAM_CCS_OFFLINE_CLEAR),     "RAT_CAM_CCS_OFFLINE_CLEAR");	
	codes.put(new Integer(SUPIR_CAM_CCS_OFFLINE_CLEAR),   "SUPIR_CAM_CCS_OFFLINE_CLEAR");	
	codes.put(new Integer(NUVIEW_SPEC_SCS_OFFLINE_CLEAR), "NUVIEW_SPEC_SCS_OFFLINE_CLEAR");	
	codes.put(new Integer(MES_SPEC_SCS_OFFLINE_CLEAR),    "MES_SPEC_SCS_OFFLINE_CLEAR");	
    }
    
    /** Returns the readable name associated with an event code.
     * @param eventCode The event code number.
     */
    public static String toCodeString(int eventCode) {
	Integer ik = new Integer(eventCode);
	if (codes.containsKey(ik))
	    return (String)codes.get(ik);
	return "Unknown event code ["+eventCode+"]";
    }

    /** Returns the event-code corresponding to the supplied Keyword.
     * This method is used in parsing config files or command lines.
     * If the supplied key does not correspond to a known event-code
     * the value <b>-1</b> Events.UNKNOWN_EVENT is returned and should
     * be tested for.
     * @param key The readable name of an event-code.
     */
    public static int getEventCode(String key) {
	if ( ! codes.containsValue(key)) 
	    return UNKNOWN_EVENT;
	Iterator it = codes.keySet().iterator();
	Integer  ik = null;
	while (it.hasNext()) {
	    ik = (Integer)it.next();
	    if ( ((String)codes.get(ik)).equals(key) )
		return ik.intValue();
	}
	return UNKNOWN_EVENT;
    }

    /** Returns a String containing the list of EventCode EventLabel pairs ascending ordered.*/
    public static String toStringList() {
	Integer  ik = null;
	StringBuffer buff = new StringBuffer();
	for (int i = 0; i < 1200; i++) {
	    ik = new Integer(i);
	    if (codes.containsKey(ik))
		buff.append("\n"+nf6.format(i)+" : "+(String)codes.get(ik));
	}
	return buff.toString();
    }

}

/** $Log: Events.java,v $
/** Revision 1.1  2006/12/12 08:29:47  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:45  snf
/** Initial revision
/**
/** Revision 1.5  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.4  2001/06/12 10:25:39  snf
/** Added codeString and getEventCode() methods.
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
