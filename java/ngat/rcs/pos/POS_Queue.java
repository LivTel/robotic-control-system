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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import ngat.net.*;
import ngat.math.*;
import ngat.util.*;
import ngat.message.POS_RCS.*;

import java.util.*;

/** This multilevel queue holds commands relayed from the POS client. Access to the
 * singleton instance is synchronized to allow safe concurrent access. The read
 * method <i>get()</i> blocks until data is available.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_Queue.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_Queue.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_Queue {

    public static int NUMBER_LEVELS = 4;

    private List[] queue;

    private POS_CommandImpl executor;

    protected static POS_Queue instance;

    protected boolean accept;

    private POS_Queue() {
	queue = new Vector[NUMBER_LEVELS];
	for (int i = 0; i < NUMBER_LEVELS; i++) {
	     queue[i] = new Vector();
	} 
	executor = null;
	accept = false;
    }

    /** Returns True if the POS_Q will accept push requests. This is used to
     * implement specific periods when requests can be accepted.*/
    public boolean accept() { return accept; }
    
    /** Enters a POS_CommandImpl at the specified level. 
     * @param level The level to extract the ProcessDescriptor from.
     * @param command The POS_TO_RCS to push into the queue.
     * @param handler The POS Command handler which will be notified when the 
     * command has been implemented.
     * @return True if the POS_Q was willing to accept the request and took it
     * false otherwise.
     */
    public synchronized boolean push(int level, POS_CommandImpl handler) {
	if (!accept) return false;
	queue[level].add(handler);
	
	return true;
    }

    /** Set the acceptance state - If true the POS_Q will accept push requests.*/
    public void setAccept(boolean accept) { this.accept = accept; }

    /** Returns the ProcessDescriptor at the head of the queue 
     * at the highest available level. If no entries are found at any level
     * then null is returned.
     */
    public synchronized POS_CommandImpl get() {		
	int     i   = 0;
	boolean got = false;
	Object  obj = null;
	while (i < NUMBER_LEVELS && !got) {
	    if (queue[i].size() != 0) {
		obj = queue[i].get(0);
		got = true;
	    } 
	    i++;
	}

	if (got)
	    return (POS_CommandImpl)obj;
	
	return null;
    } 

    /** Returns (and removes) the POS_CommandImpl at the head of the queue 
     *  at the highest available level. If no entries are found at any level
     * then null is returned.
     */
    public synchronized POS_CommandImpl remove() {	
	int     i   = 0;
	boolean got = false;
	Object  obj = null;
	while (i < NUMBER_LEVELS && !got) {
	    if (queue[i].size() != 0) {
		obj = queue[i].remove(0);
		got = true;
	    } 
	    i++;
	}

	if (got) {
	    System.err.println("PQ::Removing and returning: "+(POS_CommandImpl)obj);
	    return (POS_CommandImpl)obj;
	}
	return null;
    }
	
    /** Attempts to remove the Handler specified. If found and removed this
     * method returns true. Otherwise if not found returns false.
     * @param  POS_CommandImpl The handler to find and remove if possible.
     */
    public synchronized boolean removeRequest(POS_CommandImpl handler) {
	POS_CommandImpl aHandler = null;
	Object obj = null;
	for (int i = 0; i < NUMBER_LEVELS; i++) {
	    for (int j = 0; j < queue[i].size(); j++) {
		obj = queue[i].get(j);
		if (obj != null) {
		    aHandler = (POS_CommandImpl)obj;
		    if (aHandler == handler) {
			queue[i].remove(j);
			return true;
		    }
		}
	    }
	}
	return false;
    }

    /** Returns the number of entries at the selected level.
     * @param level The level whose size is to be returned.
     */
    public synchronized int getSize(int level) {
	return queue[level].size();
    }

    /** Returns the number of levels in the Queue.*/
    public synchronized int getLevels() {
	return queue.length;
    }

    /** Returns a list of all POS_CommandImpl in the queue at whatever level. 
     * They are not guaranteed to be in any particular order
     * though they should appear as head-to-tail order. WHen using this method
     * it would be very wise to synch on the POS_Queue instance for the duration
     * of any changes made to the elements.
     */
    public synchronized Vector listElements() {
	Vector vec = new Vector();
	POS_CommandImpl handler = null;
	Object obj = null;
	for (int i = 0; i < NUMBER_LEVELS; i++) {
	    for (int j = 0; j < queue[i].size(); j++) {
		obj = queue[i].get(j);
		if (obj != null) {
		    handler = (POS_CommandImpl)(obj);
		    vec.add(handler);
		}
	    }
	}
	return vec;
    }

    /** Returns the total expected duration of tasks at this level.
     * @param level The level to totalize.
     * @return Total duration at this level (millis).
     */
    public long getTotalDuration(int level) {
	long total = 0L;
	for (int j = 0; j < queue[level].size(); j++) {
	    total = total + ((POS_CommandImpl)queue[level].get(j)).getDuration();
	    System.err.println("PQ:getTotDuratn:: at Level: "+level+" With J: "+j+" Sum: "+total);
	}
	return total;
    }

    /** Returns the POS_CommandImpl for the command with the specified requestNumber.
     * If the request is not in the queue at any level returns null.
     * @param requestNumber  The requestNumber of the command to find.
     */
    public synchronized POS_CommandImpl findRequest(int requestNumber) throws NoSuchElementException {
	POS_CommandImpl handler = null;
	Object obj = null;
	for (int i = 0; i < NUMBER_LEVELS; i++) {
	    for (int j = 0; j < queue[i].size(); j++) {
		obj = queue[i].get(j);
		if (obj != null) {
		    handler = (POS_CommandImpl)obj;
		    if (handler.getCommand().getRequestNumber() == requestNumber)
			return handler;
		}
	    }
	}
	throw new NoSuchElementException("Not found");
    }

    /** Sets the specified handler to be the currently executing.
     * @param handler The handler to set as executing.
     */
    public synchronized void setExecutor(POS_CommandImpl handler) {
	executor = handler;
	System.err.println("PQ::Setting executor to: "+handler);
    }


    /** Returns the currently executing request/processor - if any.*/
    public synchronized POS_CommandImpl getExecutor() { return executor; }


    /** Returns an Iterator over all the ProcessDescriptors in specified level.
     * When using the returned Iterator it is wise to synchronize on the enclosing
     * POS_Queue as in ... synchronized(POS_Queue.getInstance()) {  ...*/
    public Iterator listAll(int level) {
	return queue[level].iterator();
    }

    public static POS_Queue getInstance() {
	if (instance == null)
	    instance = new POS_Queue();
	return instance;
    }    
   
}

/** $Log: POS_Queue.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.2  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.1  2001/06/08 16:27:27  snf
/** Initial revision
/** */
