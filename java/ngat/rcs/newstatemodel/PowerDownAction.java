package ngat.rcs.newstatemodel;

public class PowerDownAction extends ControlAction {

    private int powerDownMode;

    public PowerDownAction(int powerDownMode) {
	super(ControlAction.POWERDOWN_ACTION);
	this.powerDownMode = powerDownMode;
    }

    public int getPowerDownMode() {
	return powerDownMode;
    }

    @Override
	public String toString() {
	return super.toString()+": "+powerDownMode;
    }

}