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



/** 
*
* <dl>
* <dt><b>RCS:</b>
* <dd>$Id: TimeSequencer.java,v 1.1 2006/12/12 08:29:47 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/emm/RCS/TimeSequencer.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public interface TimeSequencer {

    /** Insert a TimeEvent at the specified time.
     * @param eventCode A code to represent the type of event.
     * @param time The time of the event.
     */
    public void insert(int eventCode, long time);

    /** Returns the time of the next event of the specified type.
     * @param eventCode A code to represent the type of event.
     */
    public long getNext(int eventCode);

    /** Returns the time of the next event of the specified type
     * and removes it from the list.  
     * @param eventCode A code to represent the type of event.
     */
    public long useNext(int eventCode);

    /** Removes all events of the specified type from the list.
     * @param eventCode A code to represent the type of event.
     */
    public void erase(int eventCode);

    /** Removes all events irrespective of type from the list.
     */
    public void eraseAll();

    /** Set the period during which a particular mode of operation
     * is to be defined. Within the specified period a call to
     * getMode(modeCode) or isMode(modeCode) will return true, outside
     * of this period these methods will return false.
     * It is possible to set a given mode for several contiguous or 
     * overlapping periods.
     * @param modeCode A code to represent the 'type' of mode in force.
     */
    public void setMode(int modeCode, long time1, long time2);
    

    /** Set the start time for entering a specified mode. No end time
     * is specified so that a call to getMode() or isMode() for any time
     * after the specifed time will return true. The method endMode() with
     * matching parameters is used to cancel this call. Implementors should
     * ensure that an appropriate mechanism is employed so that a call to
     * endMode() cancels the correct startMode() and not others.
    public void startMode(int modeCode, long time);
   
    /** Returns true if the specified mode is in force at the specified time.
     * @param modeCode A code to represent the 'type' of mode in force.
     * @param time The time at which the mode is to be checked.
     */
    public boolean isMode(int modeCode, long time);

    /** Returns true if the specified mode is in force at this instant.
     * @param modeCode A code to represent the 'type' of mode in force.
     */
    public boolean isMode(int modeCode);

    /** Returns an array of codes to represent the mode(s) in force at the
     * spcified time. The choice of codes is left to the application employing
     * the mode settings. If there is no mode in force i.e. none has
     * been set the implementing class should return an appropriate code.
     * @param time The time at which the mode is to be checked.
     */ 
    public int[] getModes(long time);

    /** Returns an array of codes to represent the mode(s) in force at this 
     * instant. The choice of codes is left to the application employing
     * the mode settings. If there is no mode in force i.e. none has
     * been set the implementing class should return an appropriate code.
     */ 
    public int[] getModes();

}

/** $Log: TimeSequencer.java,v $
/** Revision 1.1  2006/12/12 08:29:47  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:45  snf
/** Initial revision
/**
/** Revision 1.1  2001/04/27 17:14:32  snf
/** Initial revision
/** */
