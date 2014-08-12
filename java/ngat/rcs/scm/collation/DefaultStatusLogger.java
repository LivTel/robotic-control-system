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
package ngat.rcs.scm.collation;


import ngat.util.*;
import ngat.util.logging.*;

import java.text.*;
import java.util.*;
import java.io.*;

public class DefaultStatusLogger implements StatusLogger {

    public static SimpleDateFormat     sdf = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    static {
	sdf.setTimeZone(UTC);
    }

    /** A file stream to log to.*/
    private PrintStream fout;

    /** The file to log to.*/
    private File file;

    /** Handles formatting of logs.*/
    private StatusLogFormatter formatter;

    /** Create a TCSStatusLogger logging output to file. 
     */ 
    public DefaultStatusLogger(File file) {
	this.file = file;
    }

    /** Sets the formatter.*/
    public void setFormatter(StatusLogFormatter formatter) {
	this.formatter = formatter;
    }

    /** Prepare the logger.*/
    public void open() throws Exception {
	fout = new PrintStream(new FileOutputStream(file.getPath()+"_"+sdf.format(new Date())+".dat", true));
    }

    /** Publish the supplied StatusCategory.*/
    public void publish(StatusCategory cat) {
	if (formatter != null)
	    formatter.output(cat, fout);
    }

    /** Close this logger.*/
    public void close() throws Exception {
	fout.close();
    }

}
