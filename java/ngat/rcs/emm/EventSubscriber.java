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

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;



/** Provides a mechanism for the Event despatcher to pass
 * notifications of events back to classes which have subscribed
 * to them via the EventRegistry.
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: EventSubscriber.java,v 1.1 2006/12/12 08:29:47 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/EventSubscriber.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface EventSubscriber {
    
    /** Implementations should use this method to carry out
     * the actions appropriate to the event notified by the
     * parameter eventId. It is upto the Subscriber to
     * test the eventId to determine the appropriate course
     * of action to take. A sender can include context information
     * via the data parameter - again it is upto the subscriber
     * to interpret this correctly.
     * @param eventId The event topic.
     * @param data Context dependant data.
     */
    public void notifyEvent(String eventId, Object data);
     
    /** Implementations should use this method to return an Id.*/
    public String getSubscriberId();

}

/** $Log: EventSubscriber.java,v $
/** Revision 1.1  2006/12/12 08:29:47  snf
/** Initial revision
/**
/** Revision 1.1  2006/08/01 12:53:22  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:45  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
