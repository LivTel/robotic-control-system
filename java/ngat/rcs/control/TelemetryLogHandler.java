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
package ngat.rcs.control;

import ngat.rcs.*;
import ngat.rcs.tms.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;

import java.io.*;
import java.util.*;

import ngat.net.*;
import ngat.util.*;
import ngat.util.logging.*;
import ngat.message.GUI_RCS.*;

public class TelemetryLogHandler extends LogHandler {

    /** Name of the logger this is attached to.*/
    String logName;

    /** Create a ConsoleLogHandler using the specified formatter.*/
    public TelemetryLogHandler(String logName, LogFormatter formatter) {
	super(formatter);
	this.logName = logName;
	System.err.println(formatter.getHead());
    }

    /** Publish a LogRecord to Telemetry .*/
    @Override
	public void publish(LogRecord record) {
	System.err.println(formatter.format(record));
	LogInfo info = new LogInfo(record.getTime(), 
				   record.getSource()+":"+record.getLevel(), 
				   formatter.format(record));
	try {
	    Telemetry.getInstance().publish("LOG", info);
	} catch (Exception e) {
	    System.err.println("Telemetry log error...");
	    e.printStackTrace();
	}
    }
    
    /** Write the tail.*/
    @Override
	public void close() {
	System.err.println(formatter.getTail());
    }

    @Override
	public String toString() {
	return "[TelemetryLogHandler: Attached to: "+logName+"]";
    }

}
