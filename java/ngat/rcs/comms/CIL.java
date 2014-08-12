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

/** Provides a standard interface to the TTL CIL library
 * for internode communications between the telescope 
 * subsystems. Further method signatures will be added
 * as required. Any class may implement this interface by
 * providing a mechanism for encapsulation of the CIL's UDP
 * transport layer. A more generic version of this
 * will include methods to pass a full eCilMsg_t like struct
 * into the doSend() method.
 * In particular it is suggested that singleton classes,
 * conforming to the standard Singleton pattern,
 * could provide static accessors - typically send()
 * and receive() which grab the singleton instance and
 * call its doSend() and doReceive() methods. e.g.<br>
 * <pre>
 *
 * // Implementation of CIL.doSend().
 * public void doSend(int seq, String msg) {
 *       
 *        // 1. Pack the data into a byte array
 *        //    in network byte order.
 *        // 2. Open a UDP stream.
 *        // 3. Send the datagram.
 *        
 * // Convenience method via singleton.
 * // Grab the singleton and dispatch message.
 * public static void send(int seq, String msg) {
 *        getInstance().doSend(seq, msg);
 * }
 *
 *
 *
 * <br><br><img src = "doc-files/snft.gif"><br><br>
 * // Singleton grabber method.
 * public static Singleton getInstance() {
 *        if (theInstance == null) {
 *              theInstance = new Singleton();
 *        return theInstance;
 * }
 *
 * .. other methods.
 *  
 * }
 * </pre>
 * <br><br>
 * $Id: CIL.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 */
public interface CIL {

    /** Implementation of CIL function eCilSetup(). 
     * Classes which wish to implement this method should perform
     * any initialization of sockets etc here. This method should 
     * generally be called in the constructor of some handler
     * in the target system and should only be called once.
     * @param sendPort The local port to bind to.
     * @exception IOException If any problem occurs during setup.
     */
    public void doSetup(int sendPort) throws IOException;


    /** Implementation of CIL function eCilSend().
     * Classes which wish to implement this method should perform
     * the following actions:-<br>
     * <ol>
     * <li>Grab any neccessary information regarding
     *     Rx and Tx IDs and Class / Service categories
     *     (as specified in CIL Header Specification).
     * <li>Pack the ids and other data into a byte array conforming 
     *     to the <b>eCilMsg_t</b> structure.
     * <li>Push the byte array into a datagram.
     * <li>Open a UDP socket to the required destination/port
     *     - if not already open.
     * <li>Send the datagram via UDP.
     * </ol>
     * @param txId The CIL id of the transmitter.
     * @param rxId The CIL id of the reciver.
     * @param mclass The CIL class id of the message.
     * @param sclass The CIL class id of the service.
     * @param seqno The application generated sequence number.
     * @param message The command/request/response string to send.
     * @exception IOException If any problem occurs while sending.
     */
    public void doSend(int txId, int rxId, int mclass, int sclass, int seqno, String message) throws IOException;

    /** Implementation of CIL function eCilReceive().
     * Classes which wish to implement this method should perform
     * the following actions:-<br>
     * <ol>
     * <li>Open a UDP socket on a specified port
     *     - if not already open.
     * <li>Listen for a UDP datagram from the server.
     * <li>Unpack the bytes into an equivalent of the
     *     <b>eCilMsg_t</b> structure (CIL_Message).
     * <li>Return the structure.
     * </ol>
     * @exception IOException If any problem occurs while reading.
     */
    public CIL_Message doReceive() throws IOException;

}

/** $Log: CIL.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.3  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.2  2000/12/14 11:53:56  snf
/** Updated.
/**
/** Revision 1.1  2000/12/12 18:50:00  snf
/** Initial revision
/**
/** Revision 1.3  2000/12/01 16:50:18  snf
/** Changed doSend to reflect the full CIL header info.
/**
/** Revision 1.2  2000/11/30 15:16:50  snf
/** Added setup.
/** */
