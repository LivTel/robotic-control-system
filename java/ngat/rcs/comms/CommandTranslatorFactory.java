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
package ngat.rcs.comms;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;


/** Classes which wish to act as translators between sets of
 * objects representing commands in different implementations
 * should implement this interface. The translateCommand() 
 * method should be overridden to return an appropriate object
 * in the second system's command set given a command object
 * from the first system's command set.
 * <br><br>
 * $Id: CommandTranslatorFactory.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public interface CommandTranslatorFactory {
    
    /** Override to return an object which represents a command
     * in the target system given an object which rpresents a
     * command in the originating system. Should return null
     * if the object does not represent a command or has no 
     * representation in the target system.
     * @param command An object representing a command in the
     * originating system.
     * @return An object representing a command in the target
     * system.
     */
    public Object translateCommand(Object command);
    
    /** Override to return an object which represents a response
     * to a command sent from the originating system using the supplied
     * object which represents a response from the target system. 
     * @param command The command class (originating system) 
     * which generated this response - needed in order to parse
     * the response which may not be a command-specific class.
     * @param data An object representing a response from the
     * target system to a command from the originating system.
     * @return An object representing a response at the originating
     * system.
     */
    public Object translateResponse(Object command, Object data);
}

