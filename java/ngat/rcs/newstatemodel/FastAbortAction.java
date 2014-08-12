package ngat.rcs.newstatemodel;

public class FastAbortAction extends ControlAction {

	int code;
	public int getCode() {
		return code;
	}

	String reason;

	public FastAbortAction(int code, String reason) {
		super(FAST_ABORT_OPERATIONS_ACTION);
		this.code = code;
		this.reason = reason;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return super.toString() + " : "+code+" : " + reason;
	}

}