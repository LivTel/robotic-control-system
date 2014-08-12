/**
 * 
 */
package ngat.rcs.newstatemodel;

/**
 * @author eng
 *
 */
public class StateChangedEvent extends StateModelEvent {

	IState oldState;
	
	IState newState;
	
	/**
	 * @param statusTimeStamp
	 */
	public StateChangedEvent(long statusTimeStamp) {
		super(statusTimeStamp);
	}

	public IState getOldState() {
		return oldState;
	}

	public void setOldState(IState oldState) {
		this.oldState = oldState;
	}

	public IState getNewState() {
		return newState;
	}

	public void setNewState(IState newState) {
		this.newState = newState;
	}

	@Override
	public String toString() {
		return super.toString()+" StateChanged: "+
				(oldState != null ? oldState.getStateName():"null")+"->"+
				(newState != null ? newState.getStateName():"null");
	}
	
	

}
