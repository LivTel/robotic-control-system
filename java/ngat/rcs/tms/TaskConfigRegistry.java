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

import ngat.util.*;


/** Class to encapsulate the storage of information relating to 
 * the configuration of Tasks.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: TaskConfigRegistry.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/TaskConfigRegistry.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class TaskConfigRegistry {

    /** Stores mapping from Task class  names to configs.*/
    protected Map tasks;

    public TaskConfigRegistry() {
	tasks = new HashMap();
    }
    
    public void configure(File file) throws IOException, IllegalArgumentException {
	
	BufferedReader in = new BufferedReader(new FileReader(file));
	
	// Scanning phase.
	String line = null;
	Vector lineList = new Vector();
	while ((line = in.readLine()) != null) {
	   // System.err.println("Read:["+line+"]");
	    line = line.trim();
	    // Skip blank or comment lines.
	    if (  (! line.equals("")) && 
		  (line != null) && 
		  (! (line.startsWith("#")))) {
		lineList.add(line);
		//System.err.println("Adding: [-"+line+"-] of "+line.length()+" chars");
	    }
	}

	String taskClassName = null;
	String key           = null;
	String value         = null;

	ConfigurationProperties config = null;

	Iterator lines = lineList.iterator();
	StringTokenizer tokenz = null;

	// Collect config lines

	while (lines.hasNext()) {	    
	    line = (String)lines.next();
	    tokenz = new StringTokenizer(line);	

	    int tc = tokenz.countTokens();

	    // <TaskClass> <Key> = <Value>

	    if (tc < 3) 
		throw new IllegalArgumentException("TaskConfig::config: Missing args, only ("+tc+") : "+line);
	    
	    taskClassName = tokenz.nextToken();

	     // check its a real class.
	    try {
		Class.forName("ngat.rcs."+taskClassName);
	    } catch (ClassNotFoundException cx) {
		throw new IllegalArgumentException("TaskConfig::config: "+cx);
	    }
	    
	    key = tokenz.nextToken();

	    tokenz.nextToken();

	    value =  tokenz.nextToken();

	    if (key == null)
		throw new IllegalArgumentException("TaskConfig::config: Null key: "+key);

	    if (value == null)
			throw new IllegalArgumentException("TaskConfig::config: Null value: "+value);
	    

	    if (! tasks.containsKey(taskClassName))
		tasks.put(taskClassName, new ConfigurationProperties());
	    
	    config = (ConfigurationProperties)tasks.get(taskClassName);

	    config.put(key,value);

	   // System.err.println("TaskClass: "+taskClassName+" Save: ["+key+"="+value+"]");
	    
	}

    }

    /** Returns the config for the named task - if none, then creates a new empty one.*/
    public ConfigurationProperties getTaskConfig(Task task) {
	
	if (task == null) return null;
	
	String taskClassName = task.getClass().getName().substring(9);
	
	ConfigurationProperties config = getTaskConfig(taskClassName);
	
	if (config == null)
	    config = new ConfigurationProperties();

	return config;
	
    }

    /** Returns the config for the named task if it exists.
     * @param The Task classname (minus "ngat.rcs." package prefix).
     */
    public ConfigurationProperties getTaskConfig(String taskClassName) {
	
	return (ConfigurationProperties)tasks.get(taskClassName);

    }
    
}

	
