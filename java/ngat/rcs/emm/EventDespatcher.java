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

import java.util.*;

import ngat.util.*;
import ngat.util.logging.*;

/** Carries out the event despatching task. Examines the EventQueue
 * at a regular interval (config-parameter), retrieves codes of any
 * Events fired by the Status Monitoring mechanism, looks up the
 * list of EventSubscribers and despatches notifications to them.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: EventDespatcher.java,v 1.1 2006/12/12 08:29:47 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/EventDespatcher.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class EventDespatcher extends ControlThread {

    protected static final String CLASS = "EventDespatcher";

    /** Default value for the EventQueue poll interval (1sec).*/
    public static final long DEFAULT_INTERVAL = 1000L;
    
    /** The interval (msecs) between checks of the EventQueue.*/
    protected long interval;

    /** The current EventID.*/
    protected Integer id;

    /** Stores the current list of EventSubscribers.*/
    protected List list;

    protected Logger eventLog;
    
    /** Stores the current EventSubscriber.*/
    protected EventSubscriber subscriber;
    
    /** Create an EventDespatcher with default interval.
     * Runs repeatedly - i.e. PERMANENT.*/
    public EventDespatcher() {	
	this(DEFAULT_INTERVAL);
    }
    
    /** Create an EventDespatcher with specified cycle interval.
     * Runs repeatedly - i.e. PERMANENT.*/
    public EventDespatcher(long interval) {
	super("EventDespatcher:", true);
	this.interval = interval;
	setPriority(Thread.MAX_PRIORITY);
	eventLog = LogManager.getLogger("EVENT");
    }

    /** Initialiser - does nothing.*/
    @Override
	public void initialise() {}
    
    /** On each cycle:- get event, locate Subscribers, notify, sleep for interval.*/
    @Override
	public void mainTask() {
	EventQueue.Event ev = null;

	// Look for timed events which are now valid.
	EventQueue.checkForTimedEvents();

	// No event on queue - skip to sleep.
	if (EventQueue.peekEvent() != null) {
	    // Get next event from queue.
	    ev = EventQueue.getNextEvent();
	    // Locate any subscribers to the event.
	    list = EventRegistry.findSubscribers(ev.topic);
	    if (list != null) {
		//synchronized (list) {
		    Iterator it = list.iterator();
		    while (it.hasNext()) {
			subscriber = (EventSubscriber)it.next();
			// Notify subscriber.
			subscriber.notifyEvent(ev.topic, ev.data);
			eventLog.log(1, CLASS, "-", "main",
				     "DESPATCH: To: "+subscriber.getSubscriberId()+ " :Topic: "+ev.topic+" :Data: "+ev.data);
		    }
		//}
	    }
	}
	// Sleep for interval.
	try { Thread.sleep(interval);} catch (InterruptedException e){}
    }
    
    /** Shutdown method - does nothing yet.*/
    @Override
	public void shutdown() {}
    
    
    /** Sets the interval between status requests.
     * @param interval The interval (msecs).*/
    public void setInterval(long interval) { this.interval = interval; }

    /** @return The interval between status requests.*/
    public long getInterval() { return interval; }
    
}

/** $Log: EventDespatcher.java,v $
/** Revision 1.1  2006/12/12 08:29:47  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:45  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/04/27 17:14:32  snf
/** backup
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
