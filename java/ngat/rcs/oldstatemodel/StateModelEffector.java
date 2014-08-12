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

import java.io.*;
import java.util.*;

/** 
* Acts to implement the network representation of a state model.
* 
* StateModelEffector Implementations are expected to supply a constructor of
* the form <code>EffectorImpl(StateModel model)</code>.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: StateModelEffector.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/StateModelEffector.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/

public interface StateModelEffector {

    /** Handle notifaication that a StateModel Variable has changed.
     * @param var The Variable which changed.
     */
    public void stateChanged(StateModel.Variable var);

    /** Configure this StateModelEffector from the content of the supplied file.    
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any configuration problems.
     */
    public void configure(File file) throws IOException, IllegalArgumentException;

    /** Returns a readable description of the Effector's current state.*/
    public String stateToString();

    /** Returns a state model disposition - mapping from model items to settings.*/
    public Map getStateInfo();

    /** Causes the Effector to start its network simulation. It may want to start a
     * new Thread to carry this out.
     * @param updateInterval Specifies some interval at which updates should occur. 
     * It is upto the specific implementation how this value is applied if at all.
     */
    public void start(long updateInterval);

}

/** $Log: StateModelEffector.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
