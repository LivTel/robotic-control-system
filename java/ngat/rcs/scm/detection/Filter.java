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
package ngat.rcs.scm.detection;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;

/** Filters are attached to Sensors to carry out some form of
 * filtering operation on the received samples. Filters can 
 * buffer Sensor readings in order to carry out averaging etc.
 * Multiplexors can combine readouts from multiple filters.
 *
 * <br><br>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 * Source $Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Filter.java,v $
 * <br><br>
 * $Id: Filter.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 */
public interface Filter {
    
    /** REturn the filter reading.*/
    public Number readout();

    /** Set the name of this Filter.
     * @param name The anme to set.*/
    public void setName(String name);

    /** Returns the name/id of this Filter.
     * @return The anme/id of this Filter.*/
    public String getName();
    
    /** Set true to enable 'spy' logging.*/
    public void setSpy(boolean spy);

    public void setSpyLog(String spyLog);



}

/** $Log: Filter.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/** */
