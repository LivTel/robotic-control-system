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
package ngat.rcs.tms;

import ngat.rcs.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
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


/** Class to encapsulate the storage of information relating to the recovery of a ManagerTAs
 * in the event of failure of managed subtasks.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskRecoveryRegistry.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskRecoveryRegistry.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskRecoveryRegistry {

    /** Stores mapping from error-code names to numbers.*/
    protected Map errorCodes;

    /** Stores mapping from ManagerTA classname to task-specific recovery information.*/
    protected Map mtas;

    /** The default recovery strategy.*/
    protected  TaskRecoveryInfo DEFAULT_INFO;

    public TaskRecoveryRegistry() {
	errorCodes = new HashMap();
	mtas       = new HashMap();

	// Default is to fail and pass on the subtask errorcode.
	DEFAULT_INFO = new TaskRecoveryInfo();
	DEFAULT_INFO.setMaxTries(1);
	DEFAULT_INFO.setPassCode(true);
	DEFAULT_INFO.setDefault(true);

    }

    public void configure(File file) throws IOException, IllegalArgumentException {
	
	BufferedReader in = new BufferedReader(new FileReader(file));
	
	// Scanning phase.
	String line = null;
	Vector lineList = new Vector();
	while ((line = in.readLine()) != null) {
	    //System.err.println("Read:"+line);
	    line = line.trim();
	    // Skip blank or comment lines.
	    if ((line != "") && 
		!(line.startsWith("#")))
		lineList.add(line);
	}

	Iterator lines = lineList.iterator();
	StringTokenizer tokenz = null;
	LineTokenizer linez = null;
	
	// Collect error codes.

	// ERROR 560892 TCS_NETDIS
	
	while (lines.hasNext()) {	    
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);	

	    // Convert to LineTokenizer.
	    linez = new LineTokenizer(tokenz);
	    if (line.startsWith("ERROR")) {

		int tc = linez.countTokens();
		
		if ( tc == 3) {
		    
		    linez.readToken(); // Error

		    String strErrCode = linez.readToken();
		    String errName    = linez.readToken();
		    int    errCode    = -1;

		    try {
			errCode = Integer.parseInt(strErrCode);
		    } catch (NumberFormatException nx) {
			throw new IllegalArgumentException("ManagerTask::config: ErrorCode: "+nx);
		    }

		    errorCodes.put(new Integer(errCode), errName);
		   
		}
	    }

	}


	lines = lineList.iterator();
	tokenz = null;
	linez = null;

	// Collect task recovery options; 

	// TASK <mta_class> ON_FAIL <sub_task_class> DUE <error_code> DO [ <action> [ <option>=<value> ]* THEN ]*

	// e.g.
	//
	// TASK SciObsTask ON_FAIL SlewTask DUE TCS_MECH_FAIL DO RETRY MAX=3 DELAY=30 THEN FAIL CODE=555666 LOG=ERROR

	String mtaClassName = null;
	String subClassName = null;
	String errorName    = null;

	while (lines.hasNext()) {	    
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line); 

	    // Convert to LineTokenizer.
	    linez = new LineTokenizer(tokenz);
	    if (line.startsWith("TASK")) {
		int tc = linez.countTokens();

		// Cant be less than 7.
		if (tc > 7) {

		    linez.readToken(); // TASK		    
		    mtaClassName = linez.readToken();	

		    // check its a real class.
		    try {
			Class.forName("ngat.rcs."+mtaClassName);
		    } catch (ClassNotFoundException cx) {
			throw new IllegalArgumentException("ManagerTask::config: "+cx);
		    }
		    
		    linez.readToken(); // ON_FAIL
		    subClassName = linez.readToken();
		   
		    linez.readToken(); // DUE
		    errorName = linez.readToken();
		    linez.readToken(); // DO

		    // Notes: errorName Can be:
		    //           A valid errorName from errorCodes list,
		    //             we then have to check it is a valid code.
		    //           The code: *ANY* to indicate all failures for this subtask
		    //           The code: *OTHER* to indicate any codes not already covered by specific entries
		    //        subClassName: Can be either:
		    //           A valid Task subclass - have to check with Class.forName()
		    //           The code: *ANY* to indicate all subtasks.
		    //           The code: *OTHER* to indicate subtasks which do not have a specific entry.
		    
		    Map mtaMap = getMTARecovery(mtaClassName);
		    
		    TaskRecoveryInfo info = getRecovery(mtaMap, subClassName, errorName);
		    
		    try {
			parseActions(linez, info);
		    } catch (NumberFormatException nx) {
			throw new IllegalArgumentException("ManagerTask::config: "+nx);			
		    }
		}
	    }
	}

	Iterator xx = mtas.keySet().iterator();
	
	while (xx.hasNext()) {
	    
	    String mta = (String)xx.next();
	    
	    Map map = (HashMap)mtas.get(mta);
	    
	    System.err.println("Recovery info for Manager class: "+mta);
	    
	    Iterator yy = map.keySet().iterator();
	    
	    while (yy.hasNext()) {
	    
		String combo = (String)yy.next();
		
		TaskRecoveryInfo info = (TaskRecoveryInfo)map.get(combo);
		
		System.err.println("For: "+combo+" : DO "+info);
	    }
	} 
	
    } 
    
    protected void parseActions(LineTokenizer linez, TaskRecoveryInfo info) throws NumberFormatException {
	
	// [<action> [ <key>=<value> ] * THEN ] *
	
	while (linez.moreTokens() && (isAction(linez.nextToken()) || isSpacer(linez.nextToken()))) {
	    
	    if (isAction(linez.nextToken())) {
		
		String action = linez.readToken();

		if (action.equals("SKIP"))
		    info.setSkipEnabled(true);
		
		while (linez.moreTokens() && isOption(linez.nextToken())) {
		    
		    String option = linez.readToken();
		    		 
		    String key = option.substring(0, option.indexOf("="));

		    String value = option.substring(1+option.indexOf("="));
		    
		    if (key.equals("MAX")) {
			info.setRetryEnabled(true);
			info.setMaxTries(Integer.parseInt(value));
		    } else if
			(key.equals("DELAY"))
			info.setDelay(Long.parseLong(value));
		    else if
			(key.equals("CODE"))
			info.setFailCode(Integer.parseInt(value));
		    else if
			(key.equals("LOG")) 							    
			info.setLogCategory(value);
		    else if
			(key.equals("SUBJECT"))
			info.setCalloutSubject(value);
		    else if
			(key.equals("PASS"))
			info.setPassCode(true);
		   		    
		}
	    } else {
		linez.readToken();
	
	    }
	    
	}

    }

    protected Map getMTARecovery(String mtaClassName) {
	if (! mtas.containsKey(mtaClassName)) {	   
	    mtas.put(mtaClassName, new HashMap());
	    //System.err.println("Created new Map for: "+mtaClassName);
	}
	Map map = (Map)mtas.get(mtaClassName);
	return map;	
    }

    /** Returns the MTA recovery Map for the specified ManagerClass or NULL.*/    
    protected Map findMTARecovery(String mtaClassName) {
	if (! mtas.containsKey(mtaClassName)) 
	    return null;
	return (Map)mtas.get(mtaClassName);
    }

    protected TaskRecoveryInfo getRecovery(Map mtaMap, String subClassName, String codeName) {
	if (! mtaMap.containsKey(subClassName+"."+codeName)) {
	    mtaMap.put(subClassName+"."+codeName, new TaskRecoveryInfo());
	    //System.err.println("Created new Info for: "+subClassName+"."+codeName);
	}
	TaskRecoveryInfo info = (TaskRecoveryInfo) mtaMap.get(subClassName+"."+codeName);
	return info;
    }

    protected TaskRecoveryInfo findRecovery(Map mtaMap, String subClassName, String codeName) {
	if (! mtaMap.containsKey(subClassName+"."+codeName)) 
	    return null;
	return (TaskRecoveryInfo) mtaMap.get(subClassName+"."+codeName);
    }
    
    /** Retruns the recovery information for the specified Manager, SubTAsk and error code.
     * These are deduced in the following order:
     *  <ol>
     *    <li> Try to get specific info for the combination.
     *    <li> Try for specific info for Manager and subtask (other error).
     *    <li> Try for specific info for Manager and subtask (any error).
     *    <li> Try for specific info for Manager (other subtask, any error).
     *    <li> Try for specific info for Manager (any subtask, any error).
     *    <li> Try for any manager (any subtask, any error).
     *    <li> Return default info (fail).
     *  </ol>
     *
     * If either MTA or SUBTASK are null, returns NULL as not real Task objects.
     *
     */
    public TaskRecoveryInfo getTaskRecoveryInfo(Task mgrTask, Task subTask, int code) {

	// Check for real class. - extract the prefix : ngat.rcs.
	if (mgrTask == null) return null;
	String mtaClassName = mgrTask.getClass().getName().substring(9);
	
	if (subTask == null) return null;
	String subClassName = subTask.getClass().getName().substring(9);

	String codeName = (String)errorCodes.get(new Integer(code));

	System.err.println("Locate specific MTA map");

	Map mtaMap = findMTARecovery(mtaClassName); 
	
	TaskRecoveryInfo info = null;

	if (mtaMap != null) {
	    	
	    System.err.println("Found");

	    System.err.println("Trying subtask specific ");

	    info = findRecovery(mtaMap, subClassName, codeName);
	    
	    if (info != null) {
		System.err.println("Found");
		// Got subtask/error;
		return info; 
	    } else {
		System.err.println("Trying subtask/OTHER");		
		info = findRecovery(mtaMap, subClassName, "*OTHER*");		
		if (info != null) {
		    System.err.println("Found");
		    return info;
		} else {
		    System.err.println("Trying subtask/ANY");
		    info = findRecovery(mtaMap, subClassName, "*ANY*");		     
		     if (info != null) {
			 System.err.println("Found");
			 return info;			 
		     } else {
			 System.err.println("Trying other/ANY");
			 info = findRecovery(mtaMap, "*OTHER*", "*ANY*");
			 if (info != null) { 
			     System.err.println("Found");			 
			     return info;			 
			 } else {
			     System.err.println("Trying ANY/ANY");
			     info = findRecovery(mtaMap, "*ANY*", "*ANY*");
			     if (info != null) { 
				 System.err.println("Found");			 
				 return info;			 
			     } 
			 }
		     }
		     
		 }
	    }

	} else {
	    System.err.println("Locate generic MTA map");
	    mtaMap = findMTARecovery("*ANY*"); 
	    if (mtaMap != null) {
		System.err.println("Found");
		System.err.println("Try ANY/ANY");
		info = findRecovery(mtaMap, "*ANY*", "*ANY*");
		if (info != null) {
		    System.err.println("Found");			 
		    return info;			 
		} 
	    }
	}
	System.err.println("No MTA map, Returning Default");
	return DEFAULT_INFO;
    }
    
    protected boolean isAction(String token) {
	//System.err.println("IsAction:["+token+"]");
	if (token.equals("RETRY") ||
	    token.equals("FAIL") ||
	    token.equals("CALLOUT") ||
	    token.equals("SKIP"))
	    return true;
	return false;
    }

    protected boolean isOption(String token) {
	//System.err.println("IsOption:["+token+"]");
	if (token.indexOf("=") == -1)
	    return false;
	return true;
    }

    protected boolean isSpacer(String token) {
	//System.err.println("IsOption:["+token+"]");
	if (token.equals("THEN"))
	    return true;
	return false;
    }


    public static void main(String args[]) {

	TaskRecoveryRegistry reg = new TaskRecoveryRegistry();

	try {
	    reg.configure(new File(args[0]));
	} catch (Exception ex) {
	    System.err.println("Error: "+ex);
	    return;
	}

    }


    class LineTokenizer {

	int index = 0;

	int count = 0;

	Object [] element;

	LineTokenizer(StringTokenizer t) {	    
	    Vector els = new Vector();
	    while (t.hasMoreTokens()) {
		els.add(t.nextToken());
	    }

	    element = els.toArray();

	    count = els.size();

	}

	public boolean moreTokens() { return (index < count); }

	public String nextToken() {
	    return (String)element[index];
	}

	public String readToken() { return (String)element[index++]; }

	public int countTokens() { return count; }

	public void reset() { index = 0; }

    }
 

}

/** $Log: TaskRecoveryRegistry.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/** */
