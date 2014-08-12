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
import java.io.*;

import ngat.util.*;
import ngat.util.logging.*;

/**  Holds information about all the configured Monitors and Rulebase.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: MonitorsXX.java,v 1.2 2007/07/05 11:26:48 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/detection/RCS/MonitorsXX.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.2 $
 */
public class MonitorsXX {
    
    // Monitors Configuration PLUGIN.

    public static final String CLASS = "MonitorsXX";
    
    /** Stores the list of Rules, indexed by name.*/
    protected static Map ruleMap    = new HashMap();

    /** Stores the list of Rulesets, indexed by name.*/
    protected static Map rulesetMap = new HashMap();

    /** Stores the list of Monitors, indexed by name.*/
    protected static Map monitorMap = new HashMap();

    /** Stores the list of Variables, indexed by name.*/
    protected static Map varMap     = new HashMap();

    /** Carries out Monitor triggering.*/
    protected static MonitorThread monitorThread = new MonitorThread();;
    
    /** Logger.*/
    protected static Logger mlogger = LogManager.getLogger("MONITORS");
    
    protected static String line = null;
    protected static Vector lineList = new Vector();
    
    public static void main(String args[]) {
	try { 
	    new SensorsXX();
		SensorsXX.configure(new File(args[0]));
	    new FiltersXX();
		FiltersXX.configure(new File(args[1]));
	    new MonitorsXX();
		MonitorsXX.configure(new File(args[2]));
	} catch (Exception e) {
	    System.err.println("Error running MonitorsXX: "+e);
	    return;
	}
    }
    
    /** Set the logger to use.*/
    public static void setLogger(String name) {
	mlogger = LogManager.getLogger(name);
    }
    
    /** Configure the Reactive Control Subsystem: Monitors rulebase.
     * @param file The configuration file.
     * @exception IOException if any problem opening/reading file.
     * @exception IllegalArgumentException if any syntax problems.
     */
    public static void configure(File file) throws IOException, IllegalArgumentException {
	BufferedReader in = new BufferedReader(new FileReader(file));
	
	// Scanning phase.
	pretty("Scanning phase");
	while ((line = in.readLine()) != null) {
	    //System.err.println("Read:"+line);
	    line = line.trim();
	    // Skip blank or comment lines.
	    if ((line != "") && 
		!(line.startsWith("#")))
		lineList.add(line);
	}
	mlogger.log(1, CLASS, "-", "configure",
		   "Read "+lineList.size()+" lines of configuration.");

	pretty("Monitor definition phase.");
	// Monitor definition phase.
	// Format: MONITOR <name> <cycle-time-millis>
	// 	
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("MONITOR")) {
		if (tokenz.countTokens() == 3) {
		    tokenz.nextToken();		
		    String name = tokenz.nextToken();
		    long   delay = 0L;
		    try {
			delay = Long.parseLong(tokenz.nextToken());
		    } catch (NumberFormatException nx) {
			throw new IllegalArgumentException("Monitors::config: Error parsing Monitor: "+name+" : "+nx);
		    }
		    mlogger.log(1, CLASS, "-", "configure",
			       "Creating Monitor: "+name+" Cycle time: "+(delay/1000)+" secs.");
		    Monitor monitor = new Monitor(name);
		    monitor.setPeriod(delay);		   
		    if (monitor != null)
			monitorMap.put(name, monitor);
		}
	    }
	} // end Monitors.
	
	pretty("Rule definition phase.");
	// Rule definition pahse.
	// Format: RULE <name> <cat> ( WHEN | FROM ) <filter-reading> <op> <value> 
	// Extra for DELINV..  [INV <inv> DELTA <delta>]
	//
	// Operators: { < | > } for averaging (continuous filters).
	//            { IS | NOT } for modal (discrete filters).
	//
	
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("RULE")) {
		int tc = tokenz.countTokens();
		if (tc >= 7) {
		    tokenz.nextToken();	// RULE	
		    String name = tokenz.nextToken();
		    String cat  = tokenz.nextToken(); 
		    tokenz.nextToken();	// WHEN / FROM
		    String fname= tokenz.nextToken();
		    String op   = tokenz.nextToken();
		    String sval = tokenz.nextToken();
		    //System.err.println("Parsing Rule: "+name+" Category: "+cat+" using filter: "+fname+
		    //	       " Oper: ["+op+"] on value: "+sval);
		    
		    // Locate the Filter to monitor.
		    Filter filter = FiltersXX.getFilter(fname);
		    if (filter == null) 
			throw new IllegalArgumentException("Monitors::config: No such Filter: "+fname);
		    
		    Rule rule = null;
		    if (cat.equals("THRESH")) {
			rule = processThresholdRule(name, filter, op, sval);
			// Rule rule = ..
		    } else if
			(cat.equals("SELECT")) {
			rule = processSelectRule(name, filter, op, sval);
			// Rule rule = ..
		    } else if
			(cat.equals("DELINV")) {
			if (tc == 11) {
			    tokenz.nextToken(); //INVERT
			    String sinv = tokenz.nextToken();
			    tokenz.nextToken(); // DELTA
			    String sdel = tokenz.nextToken();
			    rule = processDelayedInvertableRule(name, filter, op, sval, sinv, sdel);
			    // Rule rule = ..
			}

		    } else if
			(cat.equals("ANYOF")) {
			
			// RULE A1 ANYOF WHERE F1 AFTER 300000 ONEOFF a b c d
			// op is AFTER
			// sval is the delay			    
			tokenz.nextToken(); //ONEOFF
			// pass the rest of the tokens in as params
			rule = processAnyOfRule(name, filter, sval, tokenz);
			
		    } else
			System.err.println("Unknown Rule Category: "+cat);
		    if (rule != null) 
			ruleMap.put(name, rule);
		} 
	    } 
	} // end rules.
	
	pretty("Ruleset definition phase.");
	// Ruleset definition phase.	   
	// Format: RULESET <name> <encapsulation> [<rule>]
	//
	// Encapsulation:           
	//  simple rule-wrapping   - SIMPLE <rule>
	//  add (conjunctive rule) - CONJ   plus N x { <name> and <rule> }     on subsequent lines   
	//  or  (disjunctive rule) - DISJ   plus N x { <name> or  <rule> }     ''  ''   ''   ''    
	//  fuzzy rule. ???????    - FUZZY  plus N x { <name> <fuzzy> <rule> } ''   ''  ''   ''
	//
	// Non-simple rulesets require additional lines to add the contained rules.
	//
	// e.g. RULESET XX CONJUNCTIVE  |   RULESET YY FUZZY
	//        XX and Rule1          |     YY 0.3 Rule3
	//        XX and Rule2          |     YY 0.7 Rule4
	
	tokenz = null;
	lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith("RULESET")) {
		int tc = tokenz.countTokens();
		if (tc > 2) {
		    tokenz.nextToken();	// RULESET
		    String name = tokenz.nextToken();
		    String type = tokenz.nextToken();
		    Ruleset ruleset = null;
		    if (type.equals("SIMPLE")) {
			if (tc == 4) {				
			    String rname = tokenz.nextToken();
			    ruleset = processSimpleRuleset(name, rname);
			} else
			    throw new 
				IllegalArgumentException("Monitors::config: No rule specified for SRS: "+name);
		    } else if
			(type.startsWith("CONJ")) {
			ruleset = processConjunctiveRuleset(name);
		    } else if
			(type.startsWith("DISJ")) {
			ruleset = processDisjunctiveRuleset(name);
		    } else if
			(type.equals("FUZZY")) {
			ruleset = processFuzzyRuleset(name);
		    } else
			System.err.println("Unknown Ruleset Class: "+type);
		    rulesetMap.put(name, ruleset);
		}
	    }
	} // end rulesets.
	
	pretty("Monitor event-binding phase.");
	// Monitor - event binding.
	// Format: <monitor-name> ASSOC <ruleset> <event>
	//
	// e.g POWER_MONITOR assoc POWER_SHUTDOWN_RULES fires POWER_SHUTDOWN
	//
	
	Iterator monitors = monitorMap.keySet().iterator();
	while (monitors.hasNext()) {		  
	    String mname = (String)monitors.next();  
	    Monitor monitor = (Monitor)monitorMap.get(mname);
	    tokenz = null;
	    lines = lineList.iterator();
	    while (lines.hasNext()) {
		line = (String)lines.next();
		tokenz = new StringTokenizer(line);
		if (line.startsWith(mname)) {
		    int tc = tokenz.countTokens();
		    if (tc == 5) {
			tokenz.nextToken(); // name
			tokenz.nextToken(); // assoc
			String rsname = tokenz.nextToken();
			
			// look for Ruleset in rulesetMap..
			Ruleset rs = (Ruleset)rulesetMap.get(rsname);
			if (rs == null)
			    throw 
				new IllegalArgumentException("Monitors::config: No rule specified for SRS: "+rsname);
			
			tokenz.nextToken(); // fires
			String evTopic = tokenz.nextToken();
			monitor.associateRuleset(rs, evTopic);
			mlogger.log(1, CLASS, "-", "configure",
				   "Binding:Monitor: "+mname+
				   " fires *["+evTopic+"]* when Ruleset: "+rsname+" is triggerred");
		    }
		}
	    }
	} // monitor event bindings.
	
	// link status pools to monitorthread.
	StatusPool.register(monitorThread, StatusPool.METEOROLOGY_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.AUTOGUIDER_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.NETWORK_UPDATE_EVENT);	
	StatusPool.register(monitorThread, StatusPool.LIMITS_UPDATE_EVENT);
	StatusPool.register(monitorThread, StatusPool.STATE_UPDATE_EVENT);

	
	monitorThread.setInterval(1000L);
    }
    
    protected static Rule processThresholdRule(String name, Filter filter, String op, String sval) 
	throws IllegalArgumentException {  
	Rule rule = null;
	try {
	    double val = Double.parseDouble(sval);
	    
	    if (op.equals("<")) {
		mlogger.log(1, CLASS, "-", "configure",
			    "ThresholdRule: "+name+" Trigger when filter: "+
			    filter+" output drops below threshold: "+val);
		rule = new SimpleThresholdRule(filter, val, SimpleThresholdRule.DOWN);
		rule.setName(name);
	    } else if
		(op.equals(">")) {
		mlogger.log(1, CLASS, "-", "configure",
			    "ThresholdRule: "+name+" Trigger when filter: "+
			    filter+" output rises above threshold: "+val); 
		rule = new SimpleThresholdRule(filter, val, SimpleThresholdRule.UP);
		rule.setName(name);
	    } else
		throw new 
		    IllegalArgumentException("Monitors::config: Illegal operation ["+op+"] for Threshold rule");
	} catch (NumberFormatException nx) {
	    throw new 
		IllegalArgumentException("Monitors::config: Error parsing Threshold value: "+sval);
	}
	return rule;
    }


    protected static Rule  processAnyOfRule(String name, Filter filter, String sdelta, StringTokenizer tokenz) {
	Rule rule = null;
	try {
	    long delay = Long.parseLong(sdelta);
	    List list = new Vector();
	    while (tokenz.hasMoreTokens()) {
		//	Integer ii = new Integer(tokenz.nextToken());
		//	list.add(ii);


		// ALTERNATIVELY START --- use VarMap.VarName and extract from mappings
		String token = tokenz.nextToken();
		if (token.indexOf(".") == -1)
		    throw new 
			IllegalArgumentException("Monitors::config: Selection criterion not valid: ["+token+
						 "] Should be: <varname>.<value>");
		String varname = token.substring(0,token.indexOf("."));
		String varval  = token.substring(token.indexOf(".")+1);
		Map varMap = SensorsXX.getList(varname);
		if (varMap == null)
		    throw new 
			IllegalArgumentException("Monitors::config: No such DSM: "+varname);
		Integer val = (Integer)varMap.get(varval);
		if (val == null)
		    throw new 
			IllegalArgumentException("Monitors::config: No such DSM state: "+varval);
		list.add(val);
		// --- ALTERNATIVELY END 


	    }
	    rule = new AnyOfRule(filter, list, delay);
	    rule.setName(name);
	} catch (NumberFormatException nx) {
	    throw new 
		IllegalArgumentException("Monitors::config: Error parsing Anyof value: "+nx);
	}
	return rule;
    }

    
    protected static Rule  processDelayedInvertableRule(String name, Filter filter, String op, 
							String sval, String sinv, String sdelta) 
	throws IllegalArgumentException {  
	Rule rule = null;
	try {
	    double val = Double.parseDouble(sval);
	    double inv = Double.parseDouble(sinv);
	    long   del = Long.parseLong(sdelta);

	    if (op.equals("<")) {
		mlogger.log(1, CLASS, "-", "configure",
			   "DIThresholdRule: "+name+" Trigger when filter: "+
			    filter+" output drops below threshold: "+val+ 
			    " with inversion above: "+inv+
			    " for at least "+(del/1000.0)+" secs");
		rule = new DelayedInvertableThresholdRule(filter, val, SimpleThresholdRule.DOWN, inv, del);
		rule.setName(name);
	    } else if
		(op.equals(">")) {
		mlogger.log(1, CLASS, "-", "configure",
			    "DIThresholdRule: "+name+" Trigger when filter: "+
			    filter+" output rises above threshold: "+val+ 
			    " with inversion below: "+inv+
			    " for at least "+(del/1000.0)+" secs");
		rule = new DelayedInvertableThresholdRule(filter, val, SimpleThresholdRule.UP, inv, del);
		rule.setName(name);
	    } else
		throw new 
		    IllegalArgumentException("Monitors::config: Illegal operation ["+op+"] for DIThreshold rule");
	} catch (NumberFormatException nx) {
	    throw new 
		IllegalArgumentException("Monitors::config: Error parsing DI value: "+nx);
	}
	return rule;
    }


    protected static Rule processSelectRule(String name, Filter filter, String op, String sval) 
	throws IllegalArgumentException {
	// op should be     IS or NOT
	// sval should be   VAR.VAL
	Rule rule = null;
	if (sval.indexOf(".") == -1)
	    throw new 
		IllegalArgumentException("Monitors::config: Selection criterion not valid: ["+sval+
					 "] Should be: <varname>.<value>");
	String varname = sval.substring(0,sval.indexOf("."));
	String varval  = sval.substring(sval.indexOf(".")+1);
	Map varMap = SensorsXX.getList(varname);
	if (varMap == null)
	    throw new 
		IllegalArgumentException("Monitors::config: No such DSM: "+varname);
	Integer val = (Integer)varMap.get(varval);
	if (val == null)
	    throw new 
		IllegalArgumentException("Monitors::config: No such DSM state: "+varval);
	int ival = val.intValue();
	
	if (op.equals("IS")) {
	    mlogger.log(1, CLASS, "-", "configure",
		       "SelectRule: "+name+" Trigger when filter: "+
		       filter+" output equals "+varname+":"+varval+" ("+ival+")");
	    rule = new SelectRule(filter, ival, true);
	    rule.setName(name);
	} else if
	    (op.equals("NOT")) {
	    mlogger.log(1, CLASS, "-", "configure",
		       "SelectRule: "+name+" Trigger when filter: "+
		       filter+" output not equal "+varname+":"+varval+" ("+ival+")"); 
	    rule = new SelectRule(filter, ival, false);
	    rule.setName(name);
	} else
	    throw new 
		IllegalArgumentException("Monitors::config: Illegal operation ["+op+"] for Selection rule");
	return rule;
    }
    
    
    protected static Ruleset processSimpleRuleset(String name, String rulename) throws IllegalArgumentException {
	Rule rule = (Rule)ruleMap.get(rulename);
	if (rule == null)	
	    throw new IllegalArgumentException("Monitors::config: No such rule: "+rulename);
	Ruleset ruleset = new SimpleRuleset(rule);
	mlogger.log(1, CLASS, "-", "configure",
		   "Simple Ruleset: "+name+" wrapping: "+rulename);
	return ruleset;
    }
    
    protected static Ruleset processConjunctiveRuleset(String name) throws IllegalArgumentException {
	ConjunctiveRuleset ruleset = new ConjunctiveRuleset();
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();	
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith(name)) {
		int tc = tokenz.countTokens();
		if (tc == 3) {
		    tokenz.nextToken();	
		    tokenz.nextToken();	
		    String rulename = tokenz.nextToken(); 
		    Rule rule = (Rule)ruleMap.get(rulename);
		    if (rule == null)
			throw new IllegalArgumentException("Monitors::config: No such rule: "+rulename);
		    ruleset.addRule(rule);
		    mlogger.log(1, CLASS, "-", "configure",
			       "Conjunctive Ruleset: "+name+" AND-ing: "+rulename);
		}
	    }
	}
	return ruleset;
    }

    
    protected static Ruleset processDisjunctiveRuleset(String name) throws IllegalArgumentException {
	DisjunctiveRuleset ruleset = new DisjunctiveRuleset();
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith(name)) {
		int tc = tokenz.countTokens();
		if (tc == 3) {
		    tokenz.nextToken();	
		    tokenz.nextToken();	
		    String rulename = tokenz.nextToken(); 
		    Rule rule = (Rule)ruleMap.get(rulename);
		    if (rule == null)
			throw new IllegalArgumentException("Monitors::config: No such rule: "+rulename);
		    ruleset.addRule(rule);
		    mlogger.log(1, CLASS, "-", "configure",
			       "Disjunctive Ruleset: "+name+" OR-ing: "+rulename);
		}
	    }
	}
	return ruleset;
    }
    
    protected static Ruleset processFuzzyRuleset(String name) throws IllegalArgumentException {
	StringTokenizer tokenz = null;
	Iterator lines = lineList.iterator();
	while (lines.hasNext()) {
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);
	    if (line.startsWith(name)) {
		int tc = tokenz.countTokens();
		if (tc == 3) {
		    tokenz.nextToken();	
		    String swgt = tokenz.nextToken();	
		    double wgt = 0.0;
		    try {
			wgt = Double.parseDouble(swgt);
			String rulename = tokenz.nextToken(); 
			//if (! ruleMap.containsKey(rulename))
			//  throw new IllegalArgumentException("Monitors::config: No such rule: "+rulename);
			mlogger.log(1, CLASS, "-", "configure",
				   "Fuzzy Ruleset: "+name+
				   " adding: "+rulename+
				   " with fuzzy weight: "+wgt);
		    } catch (NumberFormatException nx) {
			throw new 
			    IllegalArgumentException("Monitors::config: Ruleset: "+name+
						     " Parsing weight: "+swgt+" : "+nx);
		    }
		}
	    }
	}
	return null;
    }
    

    private static void pretty(String s) {
	mlogger.log(1, CLASS, "-", "configure",
		   "============================================================"+
		   "\n"+s+"\n"+
		   "============================================================");
    }

    /** Starts the MonitorThread.*/
    public static void startMonitoring() {
	monitorThread.start();
    }
    
    /** Stops the MonitorThread.*/
    public static void stopMonitoring() {
	monitorThread.terminate();
    }

    /** Returns a Monitor, identified by name or null if not found.*/
    public static Monitor getMonitor(String name) {
	if (monitorMap.containsKey(name))
	    return (Monitor)monitorMap.get(name);
	return null;
    }

     /** Returns a Rule, identified by name or null if not found.*/
    public static Rule getRule(String name) {
	if (ruleMap.containsKey(name))
	    return (Rule)ruleMap.get(name);
	return null;
    }

    /** Returns a Ruleset, identified by name or null if not found.*/
    public static Ruleset getRuleset(String name) {
	if (rulesetMap.containsKey(name))
	    return (Ruleset)rulesetMap.get(name);
	return null;
    }

    /** Performs regular (at trigger interval) Triggering of Monitors.*/
    protected static class MonitorThread extends ControlThread implements Observer {
	
	/** Interval between triggering monitors.*/
	private long interval;
	
	/** Create the MonitorThread.*/
	MonitorThread() {
	    super("XMONITOR_TRIG", true);	    
	}
	
	@Override
	public void initialise() {}
	
	/** Loop, test each Monitor per cycle - some may do nothing as they have their
	 * own wakeup period.*/
	@Override
	public void mainTask() {
	    
	    // May get woken up by a notification from the Observable(s) which can
	    // signal when new Sensor data is potentially available. Otherwise we
	    // poll them at specified interval anyway !
	    try {
		sleep(interval);
	    } catch (InterruptedException ix) {}
	    //System.err.println("MTXX::Wakeup");
	    String mname = null;
	    Iterator it = monitorMap.keySet().iterator();
	    while (it.hasNext()) {
		mname = (String)it.next();
		Monitor monitor = (Monitor)monitorMap.get(mname);
		mlogger.log(2, CLASS, "-", "mainTask",
			   "MonitorThreadXX About to Trigger: "+mname);
		monitor.trigger();
	    }
	}

	
	@Override
	public void shutdown() {}
	
	/**Sets the interval between triggering Monitors.*/
	public void setInterval(long interval) { this.interval = interval; }

	/** The implementation of this method from the java.util.Observable
	 * interface just calls interrupt() on this MonitorThread. 
	 * This is called by the Observable(s) to which this MonitorThread is attached 
	 * in order to inform the thread that new data is available for some of the
	 * Monitors it manages.	
	 * @param trigger The Observable (trigger source) which has been
	 * updated soas to force the triggering.
	 * @param arg Some data passed back from the trigger source. Currently
	 * this is ignored but may be used later?
	 */
	public void update(Observable trigger, Object arg) {
	    mlogger.log(2, CLASS, "-", "update",
		       "MonitorThreadXX Updated by: "+trigger+" Using: "+(arg != null ? arg.getClass().getName() : "NULL"));
	    interrupt();
	}

    }



}

/** $Log: MonitorsXX.java,v $
/** Revision 1.2  2007/07/05 11:26:48  snf
/** checking
/**
/** Revision 1.1  2006/12/12 08:31:16  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:35:17  snf
/** Initial revision
/** */










