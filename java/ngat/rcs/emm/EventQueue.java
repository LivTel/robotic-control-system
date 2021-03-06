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

import ngat.util.logging.*;
import java.util.*;
import java.text.*;

/**
 * Represents a queue of Events generated by Monitors. The EventDespatcher
 * examines this queue regularly in order to despatch events to subscribers who
 * have registered an interest in the given type of event.
 * 
 * <br>
 * <br>
 * <dl>
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/EventQueue.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.1 $ <br>
 * <br>
 *          $Id: EventQueue.java,v 1.1 2006/12/12 08:29:47 snf Exp $
 */
public class EventQueue {

	protected static final String CLASS = "EventQueue";

	public static final int PRIORITY_LEVEL = 0;

	public static final int DEFAULT_LEVEL = 1;

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	/** Internal structure to hold the events. */
	protected Vector defaultQueue;

	/** Internal structure to hold priority events. */
	protected Vector priorityQueue;

	/** Internal structure to hold time-critical events. */
	protected Vector timeQueue;

	/** Singleton instance. */
	private static EventQueue instance = null;

	/** Event logging. */
	protected static Logger eventLog;

	/** Create an empty EventQueue. */
	private EventQueue() {
		defaultQueue = new Vector();
		priorityQueue = new Vector();
		timeQueue = new Vector();
		eventLog = LogManager.getLogger("EVENT");
	}

	/** Create the singleton instance. */
	public static void initialize() {
		if (instance == null)
			instance = new EventQueue();
	}

	/** Post an event to the default EventQueue. */
	public static void postEvent(String topic) {
		// if (instance.queue.contains(topic)) return;
		Event ev = new Event(topic);
		instance.defaultQueue.add(ev);
		eventLog.log(1, CLASS, "-", "-", "POST: Default: " + ev.toString());

	}

	/** Post an event to the EventQueue at priority level. */
	public static void postEvent(String topic, int level) {
		// if (instance.queue.contains(topic)) return;
		Event ev = new Event(topic);

		if (level == PRIORITY_LEVEL) {
			instance.priorityQueue.add(ev);
			eventLog.log(1, CLASS, "-", "-", "POST: Priority: " + ev.toString());
		} else {
			instance.defaultQueue.add(ev);
			eventLog.log(1, CLASS, "-", "-", "POST: Default: " + ev.toString());
		}
	}

	/** Post an event with data to the default EventQueue. */
	public static void postEvent(String topic, Object data) {
		// if (instance.queue.contains(topic)) return;
		Event ev = new Event(topic, data);
		instance.defaultQueue.add(ev);
		eventLog.log(1, CLASS, "-", "-", "POST: Default: " + ev.toString());

	}

	/** Post an event with data to the EventQueue at priority level. */
	public static void postEvent(String topic, Object data, int level) {
		// if (instance.queue.contains(topic)) return;
		Event ev = new Event(topic, data);
		if (level == PRIORITY_LEVEL) {
			instance.priorityQueue.add(ev);
			eventLog.log(1, CLASS, "-", "-", "POST: Priority: [" + topic + "] : "
					+ (data != null ? data.getClass().getName() : "NULL"));
		} else {
			instance.defaultQueue.add(ev);
			eventLog.log(1, CLASS, "-", "-", "POST: Default: " + ev.toString());
		}
	}

	/**
	 * Post a time-critical event to the specified queue. The after and before
	 * times can be set to zero to indicate whenever. i.e. If after is zero then
	 * the event can be despatched up until 'before'. If the before is zero the
	 * event can be despatched any time after 'after'. Note that after a fixed
	 * 'before' the event cannot be sent. Before a fixed 'after' the event
	 * cannot be sent.
	 * 
	 * @param topic
	 *            The event topic.
	 * @param data
	 *            An optional piece of data.
	 * @param after
	 *            A time after which this event may be despatched.
	 * @param before
	 *            A time before which this event must be despatched.
	 * @param level
	 *            The priority queue from which to despatch this event.
	 */
	public static void postTimedEvent(String topic, Object data, long after, long before, int level) {
		TimedEvent tev = new TimedEvent(topic, data, after, before, level);
		instance.timeQueue.add(tev);
		eventLog.log(1, CLASS, "-", "-", "POST: Timed: " + tev.toString());
	}

	/**
	 * Checks for any enabled TimedEvents and places them appropriately in the
	 * required priority queue.
	 */
	public static void checkForTimedEvents() {
		// System.err.println("EQ::Checking for TimedEvents");
		long now = System.currentTimeMillis();
		TimedEvent t = null;
		Iterator tevs = instance.timeQueue.iterator();
		while (tevs.hasNext()) {
			t = (TimedEvent) tevs.next();
			if (t == null)
				continue;
			if (((t.after <= 0) || (t.after < now)) && ((t.before <= 0) || (t.before > now))) {
				postEvent(t.topic, t.data, t.level);
				tevs.remove();
			}
		}
	}

	/**
	 * @return Remove an event from the EventQueue and return it. If both lists
	 *         are empty, returns null.
	 */
	public static Event getNextEvent() {
		if (instance.priorityQueue.isEmpty()) {
			if (instance.defaultQueue.isEmpty())
				return null;
			else {
				Event event = (Event) instance.defaultQueue.firstElement();
				instance.defaultQueue.remove(event);
				return event;
			}
		}
		Event event = (Event) instance.priorityQueue.firstElement();
		instance.priorityQueue.remove(event);
		return event;
	}

	/**
	 * *@return The first event on the EventQueue without removing it. If the
	 * list is empty, returns null.
	 */
	public static Event peekEvent() {
		if (instance.priorityQueue.isEmpty()) {
			if (instance.defaultQueue.isEmpty())
				return null;
			else {
				return (Event) instance.defaultQueue.firstElement();
			}
		}
		return (Event) instance.priorityQueue.firstElement();
	}

	/** Clear the entire queue of <b>all</b> events without despatching. */
	public static void clear() {
		instance.priorityQueue.clear();
		instance.defaultQueue.clear();
	}

	/**
	 * Clear the entire queue of all events with the specified id without
	 * despatching.
	 */
	public static void clear(String topic) {
		Iterator it = instance.defaultQueue.iterator();
		while (it.hasNext()) {
			Event ev = (Event) it.next();
			if (ev.topic.equals(topic))
				instance.defaultQueue.remove(ev);
		}
		it = instance.priorityQueue.iterator();
		while (it.hasNext()) {
			Event ev = (Event) it.next();
			if (ev.topic.equals(topic))
				instance.priorityQueue.remove(ev);
		}
	}

	public static class Event {

		public String topic;

		public Object data;

		Event(String topic, Object data) {
			this.topic = topic;
			this.data = data;
		}

		Event(String topic) {
			this(topic, null);
		}

		@Override
		public String toString() {
			return "[Event: Topic=" + topic + (data != null ? ", Data=" + data.getClass().getName() + ":" + data : "")
					+ "]";
		}
	}

	public static class TimedEvent extends Event {

		long before;

		long after;

		int level;

		TimedEvent(String topic, Object data, long after, long before, int level) {
			super(topic, data);
			this.before = before;
			this.after = after;
			this.level = level;
		}

		TimedEvent(String topic, long after, long before, int level) {
			this(topic, null, after, before, level);
		}

		@Override
		public String toString() {
			return "[TimedEvent: Topic=" + topic + ", After="
					+ (after <= 0 ? "(Whenever)" : sdf.format(new Date(after))) + ", Before="
					+ (before <= 0 ? "(Whenever)" : sdf.format(new Date(before))) + ", Level="
					+ (level == PRIORITY_LEVEL ? "PRIORITY" : "DEFAULT")
					+ (data != null ? ", Data=" + data.getClass().getName() + ":" + data : "") + "]";
		}

	}

}

/**
 * $Log: EventQueue.java,v $ /** Revision 1.1 2006/12/12 08:29:47 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:31:45 snf /** Initial
 * revision /** /** Revision 1.2 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.1 2000/12/22 14:40:37 snf /** Initial revision
 * /**
 */
