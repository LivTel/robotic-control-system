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



/** A collection of Rules combined in some way to allow a condition
 * to be enabled/fired. Concrete subclasses should provide a mechanism
 * to allow Rules to be logically combined and the invoke() method
 * should provide the mechanism for logically (or arithmetically)
 * combining the results of individual Rule.invokations. 
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: Ruleset.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/Ruleset.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public abstract class Ruleset {

    /** Indicates whether the Rule is enabled - i.e. allowed to
     * fire. This field may be used to switch on and off the ability
     * of the Rule to be fired.
     */
    protected boolean enabled;

    /** Constructor. Sets enabled true.*/
    public Ruleset() {
	enabled = true;
    }

    /** Enables this Rule.*/
    protected void enable() { enabled = true; }
    
    /** Disables this Rule.*/
    protected void disable() { enabled = false; }
    
    /** @return True if the Rule is enabled.*/
    protected boolean enabled() { return enabled; }
    
    /** @return True if the Rule is <b>not</b> enabled.*/
    protected boolean disabled() { return !enabled; }
    
    /** @return True if the Rules in this Ruleset combined in
     * the appropriate way, when invoked are capable of firing.*/
    public abstract boolean invoke();

}

/** $Log: Ruleset.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
