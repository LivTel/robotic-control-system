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



/** 
*
* <dl>
* <dt><b>RCS:</b>
* <dd>$Id: TaskManager.java,v 1.1 2006/12/12 08:28:09 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskManager.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public interface TaskManager {

    /** Called by a subTask to indicate to this (Manager) Task that the subTask
     * has now completed and that the manager can safely dispose of it and move 
     * onto the next Task. The subTask should have set its done flag to indicate
     * that it is complete along with any done-data.
     * @param task The subTask which has completed.
     */
    public void sigTaskDone(Task task);
    
    /** Called by a subTask to indicate to this (Manager) Task that the subTask
     * has failed for some reason. The subTask should have set its
     * failed flag to indicate that this is the case and also its ErrorIndicator to
     * reveal the nature of the problem.
     * @param task The subTask which has failed.
     */
    public void sigTaskFailed(Task task);
    
    /** Called by a subTask to indicate to this (Manager) Task that the subTask
     * has 'successfully' aborted. The subTask should have set its aborted flag to
     * indicate that this is so.
     * @param task The subTask which has aborted.
     */
    public void sigTaskAborted(Task task);

    /** Called by subTask to signal a message to its manager. The manager may use
     * the information or just pass it on up the hierarchy or even throw it away.
     * @param task The subTask which originated the message. 
     * @param category An identifier to distinguish the type of message.
     * @param message The object carrying the message.
     */
    public void sigMessage(Task source, int category, Object message);
    
    public String getManagerName();
    
    public TaskDescriptor getDescriptor();
    
}

/** $Log: TaskManager.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Added reset().
/**
/** Revision 1.1  2001/02/16 17:44:27  snf
/** Initial revision
/** */
