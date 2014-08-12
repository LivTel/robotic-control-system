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

import java.util.*;

/** Represents a PetriNet Place.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: PetriNetPlace.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/PetriNetPlace.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/
public abstract class PetriNetPlace {

    String name;

    boolean clamped;

    int marking;

    public PetriNetPlace(String name) {
	this.name = name;
	clamped = false;
	marking = 0;
    }
    
    public void setClamped(boolean clamped) {this.clamped = clamped;}

    public boolean isClamped() { return clamped; }

    public String getName() { return name; }

    /** Set the marking as specified if valid.*/
    public void mark(int marking) {
	if (isValidMarking(marking))
	    this.marking = marking;
    }

    /** Remove the marking from this Place. ALWAYS sets to zero .*/
    public void unMark() {
	this.marking = 0;
    }

    public int getMarking() { return marking; }

    /** Return true if the supplied value is a valid marking for this Place.*/
    public abstract boolean isValidMarking(int marking);

    public String getStateName(int stateCode) {
	return "Code["+stateCode+"]";
    }

}

/** $Log: PetriNetPlace.java,v $
/** Revision 1.1  2006/12/12 08:27:53  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:44  snf
/** Initial revision
/** */
