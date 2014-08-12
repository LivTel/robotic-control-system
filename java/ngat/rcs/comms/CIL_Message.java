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


/** Container for messages (responses) returned by the TTL
 * CIL library functions via UDP from the TCS.
 * <br><br>
 * $Id: CIL_Message.java,v 1.1 2006/12/12 08:29:13 snf Exp $
 **/
public class CIL_Message {

    // Source /destination ids.

    /** The RCS ID - as specified in CIL Header Specification Doc.*/
    public static int RCS_ID = 18;
    //public static final int RCS_ID = 74;
    
    /** The TCS ID - as specified in CIL Header Specification Doc.*/
    public static int TCS_ID = 17;
    //public static final int TCS_ID = 17;

    // Message classes.
    /** The Command message class - as specified in CIL Header Specification Doc.*/
    public static final int COMMAND_CLASS  = 1;
    
    /** Indicates Response message class - as specified in CIL Header Specification Doc.*/
    public static final int RESPONSE_CLASS = 2;
    
    /** Indicates Acknowledge message class - as specified in CIL Header Specification Doc.*/
    public static final int ACK_CLASS      = 3;
    
    /** Indicates Actioned message class - as specified in CIL Header Specification Doc.*/
    public static final int ACTION_CLASS = 4;
    
    /** Indicates Completed message class - as specified in CIL Header Specification Doc.*/
    public static final int DONE_CLASS     = 5;
    
    /** Indicates Error message class - as specified in CIL Header Specification Doc.*/
    public static final int ERROR_CLASS    = 6;
    
   
    // ## This stuff should be in a config file.##
    /** The TCS command port - ## TBD to config ##.*/
    public static final int TCS_PORT = 5678;

    /** The TCS host address - ## TBD to config ##.*/
    public static final String TCS_HOST = "ltccd1.livjm.ac.uk";

    /** The Service type -  as specified in CIL Header Specification Doc. ## A DUMMY VALUE ##*/
    public static final int SERVICE_TYPE = 589824;

    
    /** CIL message sequence number.*/
    protected int sequenceNo;

    /** CIL Tx ID from eCilMsg_t.*/
    protected int txId;

    /** CIL Rx ID from eCilMsg_t.*/
    protected int rxId;

    /** CIL Message Class - defined in Cil.h */
    protected int messageClass;

    /** CIL Service class - defined in Cil.h */
    protected int serviceClass;

    /** Data returned from TCS.*/
    protected String data;

    /** The bytes.*/
    protected byte[] bytes;

    /** Create a CIL_Message using the supplied parameters.
     * @param sequenceNo CIL message sequence number.
     * @param txId CIL TxID from eCilMsg_t.
     * @param rxId CIL RxID from eCilMsg_t.
     * @param mClass CIL Message class.
     * @param sClass CIL Service class.
     * @param data data returned from TCS.*/
    CIL_Message(int sequenceNo, int txId, int rxId, int mClass, int sClass, String data) {
	this.sequenceNo   = sequenceNo;
	this.txId         = txId;
	this.rxId         = rxId;
	this.messageClass = mClass;
	this.serviceClass = sClass;
	this.data         = data;
    }
    
    /** Sets the CIL message sequence number.*/
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    /** Returns the CIL message sequence number.*/
    public int  getSequenceNo() { return sequenceNo; }


    /** Sets the CIL Tx ID from eCilMsg_t.*/
    public void setTxId(int txId) { this.txId = txId; }
    
    /** Returns the CIL Tx ID from eCilMsg_t.*/
    public int  getTxId() { return txId; }


    /** Sets the CIL Rx ID from eCilMsg_t.*/
    public void setRxId(int rxId) { this.rxId = rxId; }
    
    /** Returns the CIL Rx ID from eCilMsg_t.*/
    public int  getRxId() { return rxId; }
    

    /** Sets the CIL Message class.*/
    public void setMessageClass(int mClass) { this.messageClass = mClass; }

    /** Returns the CIL Message class.*/
    public int  getMessageClass() { return messageClass; }


    /** Sets the CIL Service class.*/
    public void setServiceClass(int sClass) { this.serviceClass = sClass; }

    /** Returns the CIL Service class.*/
    public int  getServiceClass() { return serviceClass; }


    /** Sets the data returned from TCS.*/
    public void   setData(String data) { this.data = data; }
    
    /** Returns the data returned from TCS.*/
    public String getData() { return data; }

    public void setBytes(byte[] bytes) { this.bytes = bytes; }

    public byte[] getBytes() { return bytes; }

}

/** $Log: CIL_Message.java,v $
/** Revision 1.1  2006/12/12 08:29:13  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:30:59  snf
/** Initial revision
/**
/** Revision 1.3  2002/09/16 09:38:28  snf
/** *** empty log message ***
/**
/** Revision 1.2  2001/06/08 16:27:27  snf
/** Added GRB_ALERT.
/**
/** Revision 1.1  2000/12/12 18:50:00  snf
/** Initial revision
/**
/** Revision 1.7  2000/12/07 10:20:34  snf
/** *** empty log message ***
/**
/** Revision 1.6  2000/12/07 10:19:35  snf
/** Typos.
/**
/** Revision 1.5  2000/12/01 16:46:06  snf
/** Moved the CIL message class from JCIL.java .
/**
/** Revision 1.4  2000/11/30 15:17:16  snf
/** Added RxID TxID ServiceClass.
/**
/** Revision 1.3  2000/11/29 16:04:44  snf
/** Added the CIL message type constants.
/**
/** Revision 1.2  2000/11/28 16:56:13  snf
/** Added messageclass.
/**
/** Revision 1.1  2000/11/28 16:14:46  snf
/** Initial revision
/** */
