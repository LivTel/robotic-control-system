/**
 * 
 */
package ngat.rcs.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import ngat.message.base.ACK;
import ngat.message.base.COMMAND;
import ngat.message.base.COMMAND_DONE;


/**
 * @author eng
 * 
 */
public class SendCommand {

	private String serverHost;

	private int serverPort;

	/**
	 * @param serverPort
	 * @param command
	 */
	public SendCommand(String serverHost, int serverPort) {
		super();
		this.serverHost = serverHost;
		this.serverPort = serverPort;
	}

	public COMMAND_DONE sendCommand(COMMAND command) throws Exception {

		System.err.println("Creating socket to: " + serverHost + ":" + serverPort);

		Socket socket = new Socket(serverHost, serverPort);
		socket.setSoTimeout(30000);

		System.err.println("Opening output stream...");
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		out.flush();
		System.err.println("Flushed output stream");

		System.err.println("Opening input stream...");
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

		System.err.println("Sending: " + command);

		out.writeObject(command);
		out.flush();
		System.err.println("Command sent, getting reply");

		boolean completed = false;

		long timetogo = 10000L;
		long start = System.currentTimeMillis();

		while (!completed) {
			System.err.println("Waiting for reply: upto: "+(timetogo/1000)+"s");
			
			Object reply = in.readObject();

			System.err.println("Recieved: " + reply.getClass().getName());

			if (reply instanceof ACK) {
				ACK ack = (ACK) reply;
				int addtimeout = ack.getTimeToComplete();
				System.err.println("Ack: Timeout: " + addtimeout);
				socket.setSoTimeout(addtimeout + 10000);
				timetogo = addtimeout+10000;
			} else if (reply instanceof COMMAND_DONE) {
				COMMAND_DONE done = (COMMAND_DONE) reply;
				System.err.println("Command: " + (done.getSuccessful() ? "okay" : "fail") + ", " + done.getErrorNum()
						+ ", " + done.getErrorString());
				completed = true;
				return done;
			}
		}
		return null;
	}

}
