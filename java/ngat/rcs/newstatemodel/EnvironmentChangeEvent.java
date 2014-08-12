package ngat.rcs.newstatemodel;

import java.io.Serializable;

/**
 * 
 */

/**
 * @author snf
 *
 */
public class EnvironmentChangeEvent implements Serializable {

	public static final int SYSTEM_SUSPEND = 1;
	public static final int SYSTEM_STANDBY = 2;
	public static final int SYSTEM_OKAY = 3;
	public static final int SYSTEM_FAIL = 4;
	
	public static final int CONTROL_ENABLED= 5;
	public static final int CONTROL_DISABLED= 6;
	
	public static final int NETWORK_OKAY = 7;
	public static final int NETWORK_ALERT= 8;
	
	public static final int WEATHER_ALERT= 9;
	public static final int WEATHER_CLEAR= 10;
	
	public static final int ENCLOSURE_OPEN= 11;
	public static final int ENCLOSURE_CLOSED= 12;
	public static final int ENCLOSURE_ERROR= 13;
	
	public static final int AXES_OKAY= 14;
	public static final int AXES_ERROR= 15;

    public static final int INTENT_OPERATIONAL = 16;
    public static final int INTENT_ENGINEERING = 17;
	
    public static final int DAY_TIME = 18;
    public static final int NIGHT_TIME = 19;

    public static final int OP_RUN           = 20;
    public static final int OP_RESTART_ENG   = 21;
    public static final int OP_RESTART_AUTO  = 22;
    public static final int OP_REBOOT        = 23;
    public static final int OP_RESTART_INSTR = 24;

    public static final int MIRR_COVER_OPEN   = 25;
   public static final int MIRR_COVER_CLOSED = 26;
    public static final int MIRR_COVER_ERROR  = 27;
    
	/** The type of event.*/
	private int type;

	/** Create an EnvironmentChangeEvent of specified type.
	 * @param type The type of event.
	 */
	public EnvironmentChangeEvent(int type) {
		// TODO Auto-generated constructor stub
		this.type = type;
	}

	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}
		
	/**
	 * A string describing the specified event.type.
	 * @param type The type of event.
	 * @return A string describing the specified event.type.
	 */
	public static String typeToString(int type) {
			switch (type) {
			case SYSTEM_SUSPEND:
				return "SYSTEM_SUSPEND";
			case SYSTEM_STANDBY:
				return "SYSTEM_STANDBY";
			case SYSTEM_OKAY:
				return "SYSTEM_OKAY";
			case SYSTEM_FAIL:
			return "SYSTEM_FAIL";
			case CONTROL_ENABLED:
				return "CONTROL_ENABLED";
			case CONTROL_DISABLED:
			return "CONTROL_DISABLED";
			case NETWORK_OKAY:
				return "NETWORK_OKAY";
			case NETWORK_ALERT:
			return "NETWORK_ALERT";
			case WEATHER_ALERT:
				return "WEATHER_ALERT";
			case WEATHER_CLEAR:
			return "WEATHER_CLEAR";
			case ENCLOSURE_OPEN:
				return "ENCLOSURE_OPEN";
			case ENCLOSURE_CLOSED:
				return "ENCLOSURE_CLOSED";
			case ENCLOSURE_ERROR:
				return "ENCLOSURE_ERROR";			
			case AXES_OKAY:
				return "AXES_OKAY";
			case AXES_ERROR:
				return "AXES_ERROR";
			case INTENT_OPERATIONAL:
			    return "INTENT_OPERATIONAL";
			case INTENT_ENGINEERING:
			    return "INTENT_ENGINEERING";
			case DAY_TIME:
			    return "DAY_TIME";
			case NIGHT_TIME:
			    return "NIGHT_TIME";
			case OP_RUN:
			    return "OP_RUN";
			case OP_RESTART_ENG:
			    return "OP_RESTART_ENG";
			case OP_RESTART_AUTO:
			    return "OP_RESTART_AUTO";
			case OP_REBOOT:
			    return "OP_REBOOT";
			case OP_RESTART_INSTR:
			    return "OP_RESTART_INSTR";
			case MIRR_COVER_OPEN:
			    return "MIRR_COVER_OPEN";
			case MIRR_COVER_CLOSED:
			    return "MIRR_COVER_CLOSED";
			case MIRR_COVER_ERROR:
			    return "MIRR_COVER_ERROR";			    
			default:
			    return "UNKNOWN";
			}
	}
	
	/**
	 * @return A string describing this event.
	 */
	@Override
	public String toString() {
			return "EnvironmentChangEvent Type="+typeToString(type);
	}
	
}
