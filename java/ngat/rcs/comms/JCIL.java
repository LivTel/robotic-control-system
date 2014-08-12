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

import java.io.*;
import java.net.*;
import java.util.*;

import ngat.util.logging.*;

/** A java (Datagram socket) implementation of the CIL interface.
 *
 * <br>
 * Note: The host and port fields of the server must be set by
 * calling the static method setup(host, port) prior to attempting
 * to send any packets using send(). The host and port can be altered
 * after use but should not be done between a call to send and receive
 * as the receive socket will be stuffed..
 * <br>
 * WARNING: It may be neccessary to disable class garbage
 *          collection when using this class if it has to
 *          maintain state over a long (in gc terms) period.
 * <pre>
 *     e.g. <b>java -Xnoclassgc MyApplicationUsingJCIL</b>
 * </pre>
 * <br><br>
 * $Id: JCIL.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public class JCIL implements CIL {

    /** A reference to the singleton instance.*/
    protected static JCIL instance = null;

    /** The DatagramSocket used for sending and receiving packets.*/
    protected DatagramSocket socket;

    /** The host which will act as server.*/
    protected String host;

    /** The local port to bind to.*/
    protected int sendPort;
    
    /** The port to contact at the server.*/
    protected int destPort;

    /** Indicates the level of logging required for commands.*/
    protected int sendLogLevel;

    /** Indicates the lvele of logging required for responses.*/
    protected int recvLogLevel;
    
    /** Logger.*/
    protected Logger logger;

    protected volatile boolean active = false;

    /** DatagramSocket implementation of eCilSetup().
     * A DatagramSocket is set up for both sending and receiving.
     * No local port is specified. The send and receive methods both
     * make use of this socket. Setup should not be called again until
     * the system using JCIL is rebooted as the port allocated will
     * probably change and any response packets for messages sent
     * prior to setup() will be lost.
     * @exception IOException If the Socket cannot be opened for any reason.*/
    public void doSetup(int sendPort) throws IOException {
	// Open a DatagramSocket.	
	setSendPort(sendPort);
	socket = new DatagramSocket(sendPort);	
	active = true;
    }
    
    /** DatagramSocket implementation of eCilSend().
     * The byte buffer is 28 bytes (CIL header) + message length.
     * Timestamp is generated using java.util.Calendar.
     * Data is put in the buffer using java.io.ByteArrayOutputStream.
     * @param txId The CIL id of the transmitter.
     * @param rxId The CIL id of the reciver.
     * @param mclass The CIL class id of the message.
     * @param sclass The CIL class id of the service.
     * @param seqno The application generated sequence number.
     * @param message The command/request string to send.
     * @exception IOException If the send fails.
     **/
    public void doSend(int txId, int rxId, int mclass, int sclass, int seqno, String message) throws IOException {

	if (! active) 
	    throw new IOException("Socket is not active");
	
	byte[] buffer = new byte[28 + message.length()];
	
	ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);

	DataOutputStream dos = new DataOutputStream(baos);

	// Pack the CIL Routing Header.
	dos.writeInt(txId);
	dos.writeInt(rxId);
	dos.writeInt(mclass);
	dos.writeInt(sclass);
	dos.writeInt(seqno);

	// Make up a timestamp as UTC from 1980 5th Jan.
	// Note: This should be GPS time !
	//      - so leapsecs need subtracting somehow!!

	Calendar start = Calendar.getInstance();
	start.set(1980,0,5,0,0,0);
	long startms = start.getTime().getTime();
		
	Calendar now   = Calendar.getInstance();
	long nowms   = now.getTime().getTime();	
	
	long msecs = (nowms - startms);	
	int secs = (int)(msecs / 1000);	
	int nanos = (1000000)*(int)(msecs - 1000 * secs);
	    
	dos.writeInt(secs);
	dos.writeInt(nanos);

	// Write the message string as a sequence of bytes.
	dos.writeBytes(message);
	// Not forgetting the string terminator for C/C++ server.
	dos.writeByte((byte)0);

	buffer = baos.toByteArray();
	
	InetAddress address = InetAddress.getByName(host);
	
	DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, destPort);
	
	socket.send(packet);
	//System.err.println("JCIL:Sending with transmit: "+txId+" Recieve: "+rxId);

	switch (sendLogLevel) {
	case 1:
	    logger.log(1, "JCIL", "-", "send", 
		       "["+message+"]");
	    break;
	case 2:
	    logger.log(1, "JCIL", "-", "send", 
			   "COMMAND "+seqno+"["+message+"]");
	    break;
	case 3:
	    logger.log(1, "JCIL", "-", "send", 
		       "COMMAND ("+rxId+" "+txId+" "+mclass+" "+sclass+" "+seqno+") ["+message+"]");
	    break;	    
	}
	
    }
    
    /** DatagramSocket implementation of eCilReceive().
     * For now the routing info in the CIL header is ignored
     * on the assumption we would not have received anything
     * not for us. This should be checked either here or by
     * whatever class called receive().
     * @exception IOException If the receive fails.*/
    public CIL_Message doReceive() throws IOException { 

	if (! active) 
	    throw new IOException("Socket is not active");

	// ### TODO Handle when JCIl was closed by CIL_STOP or the likes,
	// ###      we can detect this when doClose() is called.
	// if (socket.isClosed()0 return null or throw exception;

	byte[] buffer = new byte[2000];
	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

	socket.receive(packet);

	// The data length is packet length - CIL header length.
	int length = packet.getLength() - 28;

	ByteArrayInputStream bais = new ByteArrayInputStream(buffer);

	DataInputStream dis = new DataInputStream(bais);

	// Read and ignore the routing info - its for us !
	int txId    = dis.readInt();
	int rxId    = dis.readInt();
	int mClass  = dis.readInt();
	int sClass  = dis.readInt();
	int seqno   = dis.readInt();
	int ts1     = dis.readInt(); // ignore.
	int ts2     = dis.readInt(); // ignore.
	

	byte[] data = new byte[length];

	dis.read(data);

	String responseString = new String(data);

	// Pack the data into a CIL_Message. We ignore timestamps.
	CIL_Message message = new CIL_Message(seqno, 
					      txId, rxId, 
					      mClass, sClass, 
					      responseString);
	message.setBytes(data);
	
	switch (recvLogLevel) {
	case 1:
	    logger.log(1, "JCIL", "-", "receive", 
		       "["+responseString+"]");
	    break;
	case 2:
	    logger.log(2, "JCIL", "-", "receive", 
		       toMClassString(mClass)+" "+seqno+"["+responseString+"]");
	    break;
	case 3:
	    logger.log(3, "JCIL", "-", "receive", 
		       toMClassString(mClass)+" ("+rxId+" "+txId+"+"+sClass+" "+seqno+") ["+responseString+"]");
	case 4:	    
	    for (int j = 0; j < message.getBytes().length; j++) {
		System.err.print("{"+message.getBytes()[j]+"}");
	    }	    
	case 5:
	    // Do interpretation here if required - not generally.
	}
       
	return message;
	
    }

    public void doClose() throws IOException {

	socket.close();
	active = false;
    }
    
    public void doRestart() throws IOException {

	socket = new DatagramSocket(sendPort);	
	active = true;
    }
    
    /** Static convenience method - calls doSend() 
     * @param txId The CIL id of the transmitter.
     * @param rxId The CIL id of the reciver.
     * @param mclass The CIL class id of the message.
     * @param sclass The CIL class id of the service.
     * @param seqno The application generated sequence number.
     * @param message The command/request string to send.*/
    public static void        send(int txId, int rxId, int mclass, int sclass, int seqno, String message) throws IOException { 
	getInstance().doSend(txId, rxId, mclass, sclass, seqno, message); 
    }
    
    /** Returns true if bound.*/
    public boolean doIsBound() {
	return active;
    }


    /** Static convenience method - calls doReceive().*/
    public static CIL_Message receive() throws IOException { 
	return getInstance().doReceive(); 
    }

    /** Static convenience method - calls doSetup().
     * @param host The destination host name/address.
     * @param sendPort The port to bind to locally.
     * @param destPort The destination port to send to. 
     */
    public static void setup(String host, int sendPort, int destPort) throws IOException {
	getInstance().doSetup(sendPort);
	getInstance().setDestPort(destPort);
	getInstance().setHost(host);
    }

    /** Grabs the singleton instance.*/
    public static synchronized JCIL getInstance() { 
	if (instance == null) 
	    instance = new JCIL();
	
	return instance;
    }

    /** Returns true if JCIL is bound.*/
    public static boolean isBound() {
	return  getInstance().doIsBound();
    }

    /** Sets up logging for JCIL instance.*/
    public static void setLogging(String logName, int sendLogLevel, int recvLogLevel) {
	getInstance().setLogger(logName);
	getInstance().setSendLogLevel(sendLogLevel);
	getInstance().setRecvLogLevel(recvLogLevel);
    }
    
    /** Build a JCIL instance with the default parameters.*/
    private JCIL() {}
    
    /** Sets the singleton's host name.*/
    private void setHost(String host) { this.host = host; }
    
    public String getHost() { return host; }

    /** Sets the singleton's sending port field.*/    
    private void setSendPort(int port) { this.sendPort = port; }
   
    public int getSendPort() { return sendPort; }

    /** Sets the singleton's destination port field.*/
    private void setDestPort(int port) { this.destPort = port; }

    public int getDestPort() { return destPort; }

    /** Returns true if the socket exists and is bound.*/
    public boolean isActive() { 
	if (socket == null) return false;
	return active;
    }  
    
    /** Sets the lvele of logging.*/
    private void setSendLogLevel(int logLevel) { this.sendLogLevel = logLevel; }

    /** Sets the lvele of logging.*/
    private void setRecvLogLevel(int logLevel) { this.recvLogLevel = logLevel; }

    /** Sets the logger to use.*/
    private void setLogger(String logName) { this.logger = LogManager.getLogger(logName); }

    public static String toMClassString(int mclass) {
	
	switch (mclass) {
	case CIL_Message.COMMAND_CLASS:
	    return "COMMAND    ";
	case CIL_Message.RESPONSE_CLASS:
	    return "RESPONSE   ";
	case CIL_Message.ACK_CLASS:
	    return "ACKNOWLEDGE";
	case CIL_Message.ACTION_CLASS:
	    return "ACTIONED   ";	    
	case CIL_Message.DONE_CLASS:
	    return "COMPLETED  ";
	case CIL_Message.ERROR_CLASS:
	    return "ERROR      ";
	}	
	return "UNKNOWN";
    }
}
    
/** $Log: JCIL.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:51:31  snf
/** Fixed RCS bollocks.
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.1  2000/12/14 11:53:56  snf
/** Initial revision
/**
/** Revision 1.3  2000/12/01 16:49:45  snf
/** Changed the send doSend to reflect full CIL header info.
/**
/** Revision 1.2  2000/11/30 17:44:13  snf
/** Changed TCS_ADDR to TCS_HOST.
/**
/** Revision 1.1  2000/11/30 15:16:25  snf
/** Initial revision
/** */
