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

import java.net.*;
import java.io.*;
import java.text.*;

import ngat.util.logging.*;

public class CilTest {
    
    static String host  = "ltccd1";
    
    public static String prompt = "";

    public static LT_RGO_ArgParser parser = new LT_RGO_ArgParser();
    public static int txid = 0;
    public static int rxid = 0;

    public static void main(String args[]) {
	
	int    sport = 5566; 
	int    dport = 5678;
	int    seqno = 1;

	
	host = args[0];
	
	try {
	    sport = Integer.parseInt(args[1]);
	} catch (NumberFormatException e){}
	try {
	    dport = Integer.parseInt(args[2]);
	} catch (NumberFormatException e){}
	try {
	    seqno = Integer.parseInt(args[3]);
	} catch (NumberFormatException e){}
	try {
	    // Transmit Id = RCS.
	    txid = Integer.parseInt(args[4]);
	    CIL_Message.RCS_ID = txid;
	} catch (NumberFormatException e){}
	try {
	    // Receive ID = TCS.
	    rxid = Integer.parseInt(args[5]); 
	    CIL_Message.TCS_ID = rxid;
	} catch (NumberFormatException e){}

	try {
	    JCIL.setup(host, sport, dport);
	    System.err.println(" JCIL is up: local: "+sport+" Remote: "+host+":"+dport+" Start seq: "+seqno);
	} catch (IOException e) {
	    System.err.println("Error setting up: "+e);
	    return;
	}

	JCIL.setLogging("CIL", 1, 3);
	
	prompt = new String("\n("+host+")-TCS-CIL-Test>>");
	
	LogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
	console.setLogLevel(5);
	Logger logger = LogManager.getLogger("CIL");
	logger.addHandler(console);
	logger.setLogLevel(5);

	new Writer(seqno).start();
	new Reader().start();

    }
    
    // Reads command and sends.
    static class Writer extends Thread {
		
	int seqno;

	Writer(int seqno) {this.seqno = seqno;}
	
	@Override
	public void run() {
	    DataInputStream in = new DataInputStream(System.in);
	    String     command = null;
	    //System.err.print(prompt);
	    while (true) {
		System.err.print(prompt);
		try {
		    command = in.readLine(); 
		} catch (IOException e) {
		    System.err.println("Error reading command: "+command+" : "+e);
		}
		try {
		    JCIL.send(CIL_Message.RCS_ID,
			      CIL_Message.TCS_ID,
			      CIL_Message.COMMAND_CLASS,
			      CIL_Message.SERVICE_TYPE,
			      seqno,
			      command);		   
		} catch (IOException e) {
		    System.err.println("Error sending command: "+command+" : "+e);
		}
		seqno++;
	    }
	    
	}    
	
    } // Writer.
    
    // Reads responses.
    static class Reader extends Thread {
	
	@Override
	public void run() {
	    System.err.println("Reader is running");
	    CIL_Message message = null;
	    while (true) {
		try {
		    message = JCIL.receive();
		    String mtype = "UNKNOWN";
		    switch (message.getMessageClass()) {
		    case CIL_Message.ACK_CLASS:
			mtype = "ACKNOWLEDGE";
			break;
		    case CIL_Message.DONE_CLASS:
			mtype = "COMPLETED";
			break;
		    case CIL_Message.ERROR_CLASS:
			mtype = "ERROR";
			break;
		    case CIL_Message.ACTION_CLASS:
			mtype = "ACTIONED";
			break;
		    case CIL_Message.RESPONSE_CLASS:
			mtype = "RESPONSE";
			break;
		    default:
		    }
		  
		    //System.err.println("Byte Array !--");
		    //for (int j = 0; j < message.getBytes().length; j++) {
		    //	System.err.print("{"+message.getBytes()[j]+"}");
		    //}
		    //System.err.println("         --!");
		    
		    switch (message.getMessageClass()) {
		    case CIL_Message.DONE_CLASS:
		    case CIL_Message.RESPONSE_CLASS:
			// Parse the args.	
			if (message.getData().length() > 1) {
			    try {
				parser.parse(message.getData());
				for (int i = 0; i <= parser.getMaxSequence(); i++) {
				    try {
					String tok = parser.getToken(i);
					System.err.print("\n["+i+"]     ! "+tok);
				    } catch (IllegalArgumentException ix) {
					//System.err.println(ix.toString());
				    }
				}
			    } catch (ParseException px) {
				System.err.println("Failed to parse message: "+px+" at: "+px.getErrorOffset());
			    } catch (NumberFormatException nx) {
				System.err.println("Failed to parse message: "+nx);
			    }
			} 
			break;		  
		    }
		} catch (IOException e) {
		    System.err.println("Error reading response: "+e);
		}
		System.err.print(prompt);
	    }
	}
	
    } //Reader.
    
}


