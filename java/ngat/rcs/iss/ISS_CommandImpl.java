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
package ngat.rcs.iss;

import ngat.rcs.tms.*;
import ngat.rcs.comms.*;
import ngat.net.*;
import ngat.message.base.*;
import ngat.message.ISS_INST.*;
import ngat.message.RCS_TCS.*;
import ngat.util.logging.*;

import java.io.*;

/**
 * Generic Command Implementor. Handles requests to the ISS from various
 * Instrument Control Systems. Note that this class acts as both a Server
 * RequestHandler AND a ClientImpl as it forwards the received command
 * <i>receivedCommand</i> possibly after transforming it from an
 * ngat.message.ISS_INST.INST_TO_ISS to an ngat.message.RCS_TCS.RCS_TO_TCS
 * COMMAND <i>command</i>. <br>
 * <br>
 * $Id: ISS_CommandImpl.java,v 1.1 2006/12/12 08:30:20 snf Exp snf $
 */
public abstract class ISS_CommandImpl extends JMSMA_ClientImpl implements RequestHandler {

	public static final String CLASS = "ISS_CommandImpl";

	public static final long STANDARD_TIMEOUT = 10000L;

	public static final double FUDGE = 1.25;

	/**
	 * The COMMAND which was received from the ICS. This should be distinguished
	 * from COMMAND command inherited from JMSMA_ClientImpl which represents the
	 * command to send to the Proxy layer.
	 */
	protected COMMAND receivedCommand;

	/**
	 * Stores the response for the client. It may be sent from the Proxy layer
	 * or generated internally in subclasses.
	 */
	protected COMMAND_DONE done;

	/** Object to synch on. */
	protected Object synch;

	/** True if we have some sort of response to send back. */
	protected volatile boolean finished;

	/** Counts attempts to send command and get valid response. */
	protected volatile int attempt;

	/** Delay before retrying to send (msec). */
	protected long retryDelay = 5000L;

	/** Delay before sending response when received (msec). */
	protected long responseDelay = 0L;

	/**
	 * The ProtocolImpl which carries out the server function for and which
	 * invoked this handler.
	 */
	protected JMSMA_ProtocolServerImpl serverImpl;

	/**
	 * A period at which the handler should timeout and tell the server to send
	 * an ACK(timeout) to its client to keep it alive. The timeout should be set
	 * by the handler based on the actual command received and any relevant
	 * telescope/TCS/RCS parameters which should allow the handler to
	 * <i>estimate</i> the time the command will actually take.
	 */
	protected long handlingTime;

	/** ISS Logging. */
	Logger issLog;

	/**
	 * Create an ISS_CommandImpl with a connection to the CIL_Proxy layer.
	 * Instances of this class are always created as a result of a the
	 * ISS_Server receiving an INST_TO_ISS COMMAND and thus the COMMAND is passed
	 * in Sets the ProtocolImplFactory to the singleton instance of
	 * JMSMA_ProtocolImplFactory which will return a PCI for this client.<br>
	 * Notes:
	 * <ol>
	 * <li>The value of the <i>forwarding</i> field <b>must</b> be set by
	 * subclasses to indicate whether the command should be forwarded.
	 * <li>The initial timeout sent to the client is set from the value returned
	 * by the overridden calculateTimeToComplete() method.
	 * </ol>
	 * 
	 * @param receivedCommand
	 *            The COMMAND object which has been received from the ICS by the
	 *            server.
	 * @param serverImpl
	 *            The ProtocolImpl instance currently being implemented by the
	 *            ISS_server for this command.
	 */
	public ISS_CommandImpl(JMSMA_ProtocolServerImpl serverImpl, COMMAND receivedCommand) {
		super(new SlotConnection(CIL_Proxy_Server.getInstance(), 1));

		this.receivedCommand = receivedCommand;
		this.serverImpl = serverImpl;
		piFactory = JMSMA_ProtocolImplFactory.getInstance();
		setTimeout(calculateTimeToComplete());
		setHandlingTime((long) (FUDGE * getTimeout()));
		responseDelay = calculateResponseDelay();
		issLog = LogManager.getLogger("ISS");
	}

	/**
	 * Hook to start a ClientImpl when a command is received. The
	 * processCommand() method is called to give the subclass the chance to do
	 * any specific processing prior to calling the ClientImpl exec() method
	 * which in turn calls despatchRequest() which calls sendCommand(COMMAND).<br>
	 * If a given implementor does not wish to forward to the Proxy then it
	 * <b>must</b> set the value of the <i>forwarding</i> field to false to
	 * avoid creating a client connection thread. <br>
	 * The normal execution template for this method is:-
	 * 
	 * <ol>
	 * <li>Send an ACK to the client using a timeout set by the overriden
	 * calculateTimeToComplete().
	 * <li>Carry out any command specific processing via the overriden
	 * processCommand() method.
	 * <li>If doesForward() is true, forward the command by starting a
	 * ClientConnectionThread to talk to the Proxy layer.
	 * <li>Wait for the ConnectionThread to complete - but only for
	 * <i>timeout</i> (msecs).
	 * </ol>
	 */
	public void handleRequest() {

		// ### Called from: SCT ###

		// 1. Optionally process the command here.
		processReceivedCommand((INST_TO_ISS) receivedCommand);

		// 2. Optionally forward the command via Proxy. The execution
		// thread should have entered a timeout loop. If the client has not
		// completed when this times out but has sent an ACK,
		// another timeout period will be entered. This method returns
		// as soon as exec() has spawned the new CCT.

		if (doesForward()) {
			// Translate command ISS_INST -> RCS_TCS.
			command = translateCommand((INST_TO_ISS) receivedCommand);
			// Spawn CCT and return immediately.

			finished = false;
			attempt = 0;

			// while (! finished) { // && attempt < 3) {
			// attempt++;
			issLog.log(3, CLASS, "-", "handleRequest", "Spawning client connection thread for Attempt: " + attempt);

			exec();
			try {
				// Note we only go into this once - we dont wait for additional
				// ACKs here !
				waitFor(handlingTime);
			} catch (InterruptedException ix) {
			}
			// If the result was OK then clientImpl will have sent the DONE
			// If not we are free to try again. Send an ack first and wait a
			// bit.
			// ACK ack = makeAck(retryDelay);
			// handleAck(ack);
			// try {Thread.sleep(retryDelay);} catch (InterruptedException ix)
			// {}

			// }

		} else {
			// Make up the response.
			COMMAND_DONE response = makeResponse();
			// Send it to client.
			serverImpl.sendDone(response);
		}
	}

	/** Send a command and wait response. */
	protected JMSHandler sendCommand(COMMAND command, long delay) throws IOException {

		JMSHandler handler = new JMSHandler(serverImpl, command);

		JMSMA_ProtocolClientImpl protocol = new JMSMA_ProtocolClientImpl(handler, new SlotConnection(
				CIL_Proxy_Server.getInstance(), 1));

		protocol.implement();

		// Wait while handler is not completed ... this may block longer than
		// timeout ?
		while (!handler.isDone() && !handler.isFailed()) {
			try {
				handler.waitFor(delay);
			} catch (InterruptedException ix) {
			}
		}

		return handler;

	}

	/**
	 * Override to calculate the expected completion time for this command. This
	 * is used to set the initial timeout. Subsequent timeouts are set as a
	 * result of ACKs from the proxy.
	 */
	protected abstract long calculateTimeToComplete();

	/**
	 * Override to calculate a response delay time different from the standard
	 * delay of 0 (zero) msecs.
	 */
	protected long calculateResponseDelay() {
		return 0L;
	}

	/**
	 * Indicates whether this CommandImpl needs to forward its command.
	 */
	protected abstract boolean doesForward();

	/**
	 * Subclasses must override this method to carry out any processing specific
	 * to the command they are designed to handle.
	 * 
	 * @param receivedCommand
	 *            The ISS_INST command received from ICS.
	 */
	public void processReceivedCommand(INST_TO_ISS receeivedCommand) {
		issLog.log(2, CLASS, "-", "processReceivedCommand", "Processing received command: " + receivedCommand);
	}

	/**
	 * Translate received command from ISS_INST to RCS_TCS.
	 * 
	 * @param receivedCommand
	 *            The ISS_INST command received from ICS.
	 * @return The RCS_TCS command to forward to the TCS.
	 */
	protected RCS_TO_TCS translateCommand(INST_TO_ISS receivedCommand) {
		issLog.log(3, CLASS, "-", "translateCommand", "Translating command: " + receivedCommand);
		return null;
	}

	/**
	 * Non-forwarding handlers can make up a response (DONE) message for the ICS
	 * client using this method.
	 * 
	 * @return An appropriate response.
	 */
	protected COMMAND_DONE makeResponse() {
		issLog.log(3, CLASS, "-", "makeResponse", "Building response");
		return null;
	}

	/**
	 * Subclasses may override to carry out any processing of the response
	 * received from the TCS.
	 * 
	 * @param proxyResponse
	 *            The response (COMMAND_DONE) received. from the TCS.
	 */
	protected void processResponse(COMMAND_DONE proxyResponse) {
		issLog.log(3, CLASS, "-", "processResponse", "Processing response: " + proxyResponse);
	}

	/**
	 * Translate the response from the TCS from RCS_TCS to ISS_INST DONE.
	 * 
	 * @param proxyResponse
	 *            The response (COMMAND_DONE) from the TCS.
	 * @return The appropriate response for the ICS.
	 */
	protected COMMAND_DONE translateResponse(COMMAND_DONE proxyResponse) {
		issLog.log(3, CLASS, "-", "translateResponse", "Translating response: " + proxyResponse);
		return null;
	}

	/** Make an ACK with timeout. */
	protected ACK makeAck(long timeout) {
		ACK ack = new ACK("test");
		ack.setTimeToComplete((int) timeout);
		return ack;
	}

	/**
	 * The received ACK is sent back to the ICS client to stop it from timing
	 * out. This handler's acked flag is reset to keep it from timing out, with
	 * a new timeout period.
	 */
	public void handleAck(ACK ack) {
		setTimeout((long) (FUDGE * ack.getTimeToComplete()));
		setHandlingTime((long) (FUDGE * getTimeout()));
		ack.setTimeToComplete((int) (FUDGE * getHandlingTime()));
		serverImpl.sendAck(ack);
	}

	/**
	 * The received COMMAND_DONE passed back from the proxy layer is sent
	 * straight back to the ICS client. Subclasses for specific ISS COMMANDS
	 * should override processDone() to do any processing of the DONE prior to
	 * forwarding to the ICS.
	 */
	public void handleDone(COMMAND_DONE proxyDone) {

		// ### Called from CCT ###

		// 1. Carry out any processing of the Done from the Proxy.
		processResponse(proxyDone);
		// 2. Translate from RCS_TCS to ISS_INST DONE.
		COMMAND_DONE response = translateResponse(proxyDone);

		// 3. Maybe Send on to ICS client.
		// if (response.getSuccessful()) {
		finished = true;

		// This is where to insert a temp delay in sending the reply - need to
		// watch we dont timeout the handleReq() waitfor which doesnt get reset
		// on ACK !

		try {
			Thread.sleep(responseDelay);
		} catch (InterruptedException ix) {
		}

		serverImpl.sendDone(response);
		// } else {
		// issLog.log(3, CLASS, "-", "handleDone",
		// "Error response from TCS: "+proxyDone.getErrorString());
		// if (attempt >= 3) {
		// finished = true;
		// issLog.log(3, CLASS, "-", "handleDone",
		// "Sending error response after "+attempt+" failed attempts");
		// response.setErrorString("After "+attempt+" failed attempts: "+response.getErrorString());
		// serverImpl.sendDone(response);
		// }
		// }
	}

	/**
	 * Deal with failure of client connection. Generic method is to send a DONE
	 * with error code to the ICS client.
	 */
	public void failedConnect(Exception e) {
		issLog.log(2, CLASS, "-", "-", "Failed connect: " + e);
		COMMAND_DONE error = new COMMAND_DONE(receivedCommand.getId());
		error.setSuccessful(false);
		error.setErrorNum(ErrorCodes.NET_JMS_CONNECTION_FAILED);
		error.setErrorString("Failed to connect to CIL Proxy:" + e);
		finished = true;
		serverImpl.sendDone(error);
	}

	/**
	 * Deal with failure of client to despatch the command.<br>
	 * Currently does nothing.
	 */
	public void failedDespatch(Exception e) {
		issLog.log(2, CLASS, "-", "-", "Failed despatch: " + e);
		COMMAND_DONE error = new COMMAND_DONE(receivedCommand.getId());
		error.setSuccessful(false);
		error.setErrorNum(ErrorCodes.NET_JMS_DESPATCH_ERROR);
		error.setErrorString("Failed to connect to CIL Proxy:" + e);
		finished = true;
		serverImpl.sendDone(error);
	}

	/**
	 * Deal with failure of client response. Generic method is to send a DONE
	 * with error code to the ICS client.
	 */

	public void failedResponse(Exception e) {
		issLog.log(2, CLASS, "-", "-", "Failed response:" + e);
		COMMAND_DONE error = new COMMAND_DONE(receivedCommand.getId());
		error.setSuccessful(false);
		error.setErrorNum(ErrorCodes.NET_JMS_RESPONSE_ERROR);
		error.setErrorString("Error reading response from CIL Proxy:" + e);
		finished = true;
		serverImpl.sendDone(error);
	}

	public void exceptionOccurred(Object source, Exception e) {
		// issLog.log(2, CLASS, "-", "-",
		// "JMS(MA) Exception handler: src="+source+", Ex="+e);
		System.err.println("ISS_Command::JMS Exception handler: src=" + source + ", Ex=" + e);
		finished = true;
	}

	public void sendCommand(COMMAND command) {
	}

	/**
	 * Sets (or resets) the timeout period for this handler. The method obtains
	 * a synch lock before accessing the timeout field as this Thread may want
	 * to test it while it is being set by e.g. a ServerConnectionThread.
	 * 
	 * @param handlingTime
	 *            The value to set for timeout.
	 */
	public void setHandlingTime(long handlingTime) {
		this.handlingTime = handlingTime;
	}

	/**
	 * Returns the timeout period for this handler. The method obtains a synch
	 * lock before accessing the timeout field as other Threads may want to
	 * reset it while this Thread is reading it.
	 */
	public long getHandlingTime() {
		return handlingTime;
	}

	/**
	 * Called to allow the handler to clear up resources etc. Deregisters from
	 * the Registry.
	 */
	public void dispose() {
		serverImpl = null;
		receivedCommand = null;
	}

}

/**
 * $Log: ISS_CommandImpl.java,v $ /** Revision 1.1 2006/12/12 08:30:20 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:34:28 snf /** Initial
 * revision /** /** Revision 1.5 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.4 2001/06/08 16:27:27 snf /** Changed to
 * PArallel impl. /** /** Revision 1.3 2001/04/27 17:14:32 snf /** backup /**
 * /** Revision 1.2 2001/02/16 17:44:27 snf /** *** empty log message *** /**
 * /** Revision 1.1 2000/12/14 11:53:56 snf /** Initial revision /**
 */
