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
package ngat.rcs.comms;

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
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;


public class CILTest {

    public static void main(String args[]) {

	try {
	    JCIL.setup("ltccd1", 5555, 6666);
	} catch (Exception e) {
	    System.err.println("Setup Error: "+e);
	    return;
	}

	System.err.println("setup ok:");
	long start = System.currentTimeMillis();
	for (int i = 0; i < 100000; i++) {
	    try {
		JCIL.send(55,66, CIL_Message.COMMAND_CLASS, CIL_Message.SERVICE_TYPE, 100, "Hello");
	    } catch (Exception e) {
		System.err.println("Send Error: "+i+" : "+e);
	    }	
	    if ((i % 1000) == 0)
		System.err.println("sent 1000");    
	}

	long now = System.currentTimeMillis();

	System.err.println("Sent 1000000 messages in "+(now-start)+" millis"+
			   "\n .. "+(100/(now-start))+" messages per sec."+
			   "\n .. "+((now-start)/100000)+" millis per message.");
	
    }

}
