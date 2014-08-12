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
package ngat.rcs.scm.test;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;
import ngat.util.*;
import ngat.util.logging.*;

import java.text.*;
import java.util.*;

/** Framework to control the simulation SCM detection and collation packages.
 * The following items are required to be simulated:-
 * <ul>
 *  <li>Set of Sensors. Some new classes may be generated for test purposes.
 *  <li>Set of filters to attach to sensors. 
 *  <li>Rules to attach to filters.
 *  <li>Rulesets to encapsulate the rules.
 *  <li>Monitors to trigger ruleset invokation.
 *  <li>MonitorThreads to run monitors.
 * </ul>
 * The existing classes, Sensors, Filters, MonitorsXX can be used to provide
 * much of the framework and already have all the required configuration
 * infrastructure, they do however contain references to external classes
 * concerned with event processing etc. 
 */
public class ScmTestFramework {
    
    /** Name for this simulation.*/
    private String name;

    /** Create a ScmTestFramework with supplied name.*/
    public ScmTestFramework(String name) {
	this.name = name;	
    }

    /** Configure.*/
    protected void configure(ConfigurationProperties config) throws Exception {

    }

    /** Start the simulation.*/
    protected void start() {
	System.err.println("SCM Test Framework: "+name+" Starting..");
    }

    /** Launch the SCM test framework.*/
    public static void main(String[] args) {

	CommandTokenizer ct = new CommandTokenizer("--");
	ct.parse(args);
	ConfigurationProperties config = ct.getMap();
	
	String name = config.getProperty("name", "SCM");
	
	ScmTestFramework fw = new ScmTestFramework(name);
	
	try {
	    fw.configure(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Error configuring fw: "+e);
	    return;
	}

	// Now start it.
	// fw.start();

    }

}
