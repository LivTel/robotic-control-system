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
package ngat.rcs.test;

import ngat.net.*;
import ngat.util.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;

public class TestBias extends JMSMA_ClientImpl{

    public TestBias() {
	super();
    }

    public static void main(String args[]) {

	TestBias test = new TestBias();

	try {

	    // Determine action based on args.
	    CommandTokenizer ct = new CommandTokenizer("--");
	    ct.parse(args);
	    ConfigurationProperties config = ct.getMap();
	    
	    // Run tests.
	    test.run(config);

	} catch (Exception e) {
	    e.printStackTrace();
	    usage();
	    return;
	}
		
    }

    public void run(ConfigurationProperties config) throws Exception {

	String addr = config.getProperty("addr");
	int    port = config.getIntValue("port");
	
	IConnection connection = new SocketConnection(addr, port);

	JMSMA_ProtocolClientImpl protocol = new JMSMA_ProtocolClientImpl(this, connection); 

	command = new BIAS("test-bias");

	protocol.implement();

    }

    /** Handles an ACK response.*/
    public void handleAck  (ACK ack) {
	System.err.println("CMD Client::Ack received");
    }
    
    /** Handles the DONE response. Saves reply and internal parameters and sets error flag if failed.*/
    public void handleDone (COMMAND_DONE response) {

	if (response == null) {
	    System.err.println("Response was null");
	    return;
	}

	if (! response.getSuccessful()) {
	    System.err.println("Error submitting request: "+response.getErrorString()); 
	} else {
	    System.err.println("OSS Command "+command+" accepted");					
	}	

    }
    
    /** Failed to connect.*/    
    public void failedConnect  (Exception e) {
	System.err.println("Internal error while submitting request: Failed to connect to OSS: "+e);
    }
    
    /** Failed to send command.*/
    public void failedDespatch (Exception e) {
	System.err.println("Internal error while submitting request: Failed to despatch command: "+e);
    }
    
    /** Failed to receive reply.*/
    public void failedResponse  (Exception e) {
	System.err.println("Internal error while submitting request: Failed to get reply: "+e);
    }
    
    /** A general exception.*/
    public void exceptionOccurred(Object source, Exception e) {
	System.err.println("Internal error while submitting request: Exception: "+e);
    }
    
    /** Does nothing.*/
    public void sendCommand(COMMAND command) {}

    public static void usage() {
	System.err.println("Usage: java ngat.rcs.test.TestBias --addr <inst-addr>  --port <port>");
    }
    
}
