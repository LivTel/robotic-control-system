package ngat.rcs.newstatemodel;

import java.io.Serializable;

public class TestState implements IState, Serializable {

	int state;
	
	String name;
	
	/**
	 * @param name
	 * @param state
	 */
	public TestState(int state, String name) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.state = state;
	}


public int getState() {
		return state;
}

	public String getStateName() {
		return name;
	}

    @Override
	public String toString() {
	return "STATE:"+state+"["+name+"]";
    }

}
