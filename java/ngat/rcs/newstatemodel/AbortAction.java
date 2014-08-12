package ngat.rcs.newstatemodel;

public class AbortAction extends ControlAction {

	public static final int SYS_ALERT = 600112;
	public static final String SYS_ALERT_STR = "SYS_ALERT";

	public static final int BAD_WEATHER = 600113;
	public static final String BAD_WEATHER_STR = "BAD_WEATHER";

	public static final int CTRL_DISABLED = 600114;
	public static final String CTRL_DISABLED_STR = "CTRL_DISABLED";

	public static final int CIL_NET_ALERT = 600115;
	public static final String CIL_NET_AKERT_STR = "CIL_NET_ALERT";

	public static final int ENC_NOT_OPEN = 600116;
	public static final String ENC_NOT_OPEN_STR = "ENC_NOT_OPEN";

	public static final int DAYTIME = 600117;
	public static final String DAYTIME_STR = "DAYTIME";

	public static final int AXES_ALERT = 600118;
	public static final String AXES_ALERT_STR = "AXES_ALERT";

	public static final int ENG_REQUEST = 600119;
	public static final String ENG_REQUEST_STR = "ENG_REQUEST";

	public static final int RCS_SHUTDOWN = 600120;
	public static final String RCS_SHUTDOWN_STR = "RCS_SHUTDOWN";

	public static final int PMC_CLOSED = 600121;
	public static final String PMC_CLOSED_STR = "PMC_CLOSED";

	public static final int TRACKING_LOST = 600122;
	public static final String TRACKING_LOST_STR = "TRACKING_LOST";
	
	public static final int GUIDE_LOST = 600123;
	public static final String GUIDE_LOST_STR = "GUIDE_LOST";
	
	/** Abort action code. */
	private int code;

	/** Abort action description. */
	private String reason;

	public AbortAction(int code, String reason) {
		super(ControlAction.ABORT_OPERATIONS_ACTION);
		this.code = code;
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}
	
	

	public int getCode() {
		return code;
	}

	@Override
	public String toString() {
		return super.toString() + " : "+code+" : " + reason;
	}

}