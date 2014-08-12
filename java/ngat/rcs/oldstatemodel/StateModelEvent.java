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
package ngat.rcs.oldstatemodel;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;



/** Represents an Event in a StateModel.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: StateModelEvent.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/StateModelEvent.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public class StateModelEvent {

    /** An id for this event.*/
    protected String eventId;

    /** Optionally carries data relating to the event.*/
    protected String eventData;

    /** Optionally carries timing information (millis).*/
    protected long eventDelta;

    /** Create a StateModelEvent.
     * @param eventID The ID of the event.
    */
    public StateModelEvent(String eventId) {
	this.eventId = eventId;
    }

    /** Create a StateModelEvent.
     * @param eventID The ID of the event.
     * @param event   The associated optional data.
     */
    public StateModelEvent(String eventId, String eventData) {
	this.eventId   = eventId;
	this.eventData = eventData;
    }

    /** Create a StateModelEvent.
     * @param eventID The ID of the event.
     * @param event   The associated optional data.
     * @param event   The delta time (millis).
     */
    public StateModelEvent(String eventId, String eventData, long eventDelta) {
	this.eventId    = eventId;
	this.eventData  = eventData;
	this.eventDelta = eventDelta;
    }
    
    /** Returns the Event Id.*/
    public String getEventId() { return eventId; }

    /** Returns the event data.*/
    public String getEventData() { return eventData; }

    /** Returns delta time (millis).*/
    public long getEventDelta() { return eventDelta; }
    
}

/** $Log: StateModelEvent.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
