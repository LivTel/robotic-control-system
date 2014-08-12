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
package ngat.rcs.tms;



/** Holds information about a significant Task event. This is an internal event.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskEvent.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskEvent.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskEvent {

    /** ID number for TaskEvents - starts per JVM invokation.*/
    protected static int evId = 0;

    /** ID number for a given TaskEvent.*/
    protected int myId;

    /** Task with which the event is associated.*/
    protected Task task;
    
    /** Message code for this event.*/
    protected int messageCode;
    
    static {
	evId = 0;
    }

    /** Create a TaskEvent with the specified Task and messageCode.*/
    public TaskEvent(Task task, int messageCode) {
	this.task = task;
	this.messageCode = messageCode;	
	myId = ++evId;
    }

    /** Returns the Task with which the event is associated.*/
    public Task getTask() {  return task; }

    /** Returns the message code for this event.*/
    public int getMessageCode() { return messageCode; }

    /** Returns a readable representation of this TaskEvent.*/
    @Override
	public String toString() { return "TaskEvent: ["+myId+"]"+
				   " Task: "+task.getClass().getName()+
				   " ID: "+task.getName()+
				   " Msg: "+messageCode; 
    }
    
}

/** $Log: TaskEvent.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
