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

/** Represents a Switched PetriNet Place.
*
* <dl>	
* <dt><b>RCS:</b>
* <dd>$Id: PetriNetSwitchedPlace.java,v 1.1 2006/12/12 08:27:53 snf Exp $
* <dt><b>Source:</b>
* <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/statemodel/RCS/PetriNetSwitchedPlace.java,v $
* </dl>
* @author $Author: snf $
* @version $Revision: 1.1 $
*/
public class PetriNetSwitchedPlace extends  PetriNetPlace {

    Map values;

    Map names;

    public PetriNetSwitchedPlace(String name) {
	super(name);
	values = new HashMap();
	names  = new HashMap();
	System.err.println("PN-SWP::Create new SwiP: "+name);
    }

    public void addValue(String valueName, int value) {
	values.put(valueName, new Integer(value));
	System.err.println("PN-SWP::"+name+" : Adding const: "+valueName+" : "+value);
	// Reverse map.
	names.put(new Integer(value), valueName); 
    }
       
    public int getStateCode(String stateName) throws NoSuchElementException {
	if (values.containsKey(stateName))
	     return ((Integer)values.get(stateName)).intValue();
	throw new NoSuchElementException((stateName == null ? 
					  "No state specified" : 
					  "Unknown state: "+stateName));	
    }
    
    @Override
	public String getStateName(int stateCode) {
	Integer icode = new Integer(stateCode);
	if (names.containsKey(icode))
	    return (String)names.get(icode);
	return "UNKNOWN";
    }
    
    @Override
	public boolean isValidMarking(int marking) {
	if (marking == 0) return true;
	if (values.containsValue(new Integer(marking)))
	    return true;
	return false;
    }

    @Override
	public String toString() {
	StringBuffer buff = new StringBuffer();
	buff.append("SwitchedPlace: "+name);
	buff.append("\n\t"+(clamped ? "Clamped" : "Free"));
	buff.append("\n\tMarking: "+marking);

	String vname = null;

	Iterator v = values.keySet().iterator();
	while (v.hasNext()) {
	    vname = (String)v.next();
	    buff.append("\n\tConstant: "+vname+" = "+values.get(vname));
	}
	return buff.toString();
    }

}

/* $Log: PetriNetSwitchedPlace.java,v $
 * Revision 1.1  2006/12/12 08:27:53  snf
 * Initial revision
 *
 * Revision 1.1  2006/05/17 06:32:44  snf
 * Initial revision
 * */
