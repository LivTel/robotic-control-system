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

import java.util.*;

/** Ruleset which combines Rules conjunctively. If any one of 
 * the Rules is incapable of firing the Ruleset does not fire.
 * This is equivalent to ANDing the Rules.
 *
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: ConjunctiveRuleset.java,v 1.1 2006/12/12 08:31:16 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/ConjunctiveRuleset.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class ConjunctiveRuleset extends Ruleset {
    
    /** Holds the set of Rules.*/
    protected Set rules;
    
    /** Create a ConjunctiveRuleset. Initializes and synchronizes
     * a Set to contain the rules.*/
    public ConjunctiveRuleset() {
	super();
	rules = Collections.synchronizedSet(new HashSet());
    }
    
    /** Add a Rule to the Ruleset.
     * @param rule The Rule to add.*/
    public void addRule(Rule rule) {
	rules.add(rule);
    }

    /** Remove a Rule from the Ruleset.
     * @param rule The Rule to remove.*/
    public void removeRule(Rule rule) {
	rules.remove(rule);
    }

    /** @return A reference to the set of rules.*/
    public Set getRuleset() { return rules; }

    /** @return False if ANY one of the rules fails.*/
    @Override
	public boolean invoke() {
	Iterator it = rules.iterator();
	Rule    rule = null;
	boolean fire = true;
	while (it.hasNext()) {
	    rule = (Rule)it.next();	  
	    fire = fire & rule.invoke();
	}
	// Checked all - True only if All have fired.
	if (enabled() && fire) {
	    disable();
	    return true;
	} 
	else if
	    (disabled() && !fire) {
	    enable();
	}	
	return false;
    }
    
}

/** $Log: ConjunctiveRuleset.java,v $
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/**
/** Revision 1.2  2001/02/16 17:44:27  snf
/** *** empty log message ***
/**
/** Revision 1.1  2000/12/22 14:40:37  snf
/** Initial revision
/** */
