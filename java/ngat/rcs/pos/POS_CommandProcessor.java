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


import ngat.message.base.*;
import ngat.message.POS_RCS.*;
/** Provides a mechanism for implementing the operations associated with
 * a POS_RCS command. A POS_CommandImpl RequestHandler handles the initial
 * request by placing the request into a Queue. A POS_CommandProcessor then
 * carries out the detailed operations which may include attaching Tasks to
 * the Managed Task hierarchy and executing these before sending the results
 * back to the originating client.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_CommandProcessor.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_CommandProcessor.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface POS_CommandProcessor {

    /** Implementors should carry out processing of the command.*/
    public COMMAND_DONE processCommand();

}

/** $Log: POS_CommandProcessor.java,v $
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
