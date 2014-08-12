package ngat.rcs.iss;

import java.util.*;

public class IssAutoguiderAdjustmentController {

    private volatile boolean state;
    
    private List listeners;

    public IssAutoguiderAdjustmentController() {
	listeners = new Vector();
    }

    public void controlAutoguider(boolean state) { this.state = state; }

    public boolean getControlState() { return state; }

    public void addAutoguiderAdjustmentListener(AutoguiderAdjustmentListener l) {
	if (listeners.contains(l))
	    return;
	listeners.add(l);
    }

    public void removeAutoguiderAdjustmentListener(AutoguiderAdjustmentListener l) {
	if (! listeners.contains(l))
	    return;
	listeners.remove(l);
    }

    public void notifyListenersStartingOffset() {
	Iterator it = listeners.iterator();
	while (it.hasNext()) {
	    AutoguiderAdjustmentListener l = (AutoguiderAdjustmentListener)it.next();
	    l.startingOffset();
	}
    }

    public void notifyListenersEndingOffset() {
	Iterator it = listeners.iterator();
	while (it.hasNext()) {
	    AutoguiderAdjustmentListener l = (AutoguiderAdjustmentListener)it.next();
	    l.endingOffset();
	}
    }

    public void notifyListenersGuideReAcquired() {
	Iterator it = listeners.iterator();
	while (it.hasNext()) {
	    AutoguiderAdjustmentListener l = (AutoguiderAdjustmentListener)it.next();
	    l.guideReAcquired();
	}
    }
    
    public void notifyListenersGuideNotReAcquired(String message) {
	Iterator it = listeners.iterator();
	while (it.hasNext()) {
	    AutoguiderAdjustmentListener l = (AutoguiderAdjustmentListener)it.next();
	    l.guideNotReAcquired(message);
	}
    }
    
}
