/**
 * 
 */
package ngat.rcs.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.TimeZone;

import ngat.message.GUI_RCS.GET_STATUS;
import ngat.message.GUI_RCS.GET_STATUS_DONE;
import ngat.message.GUI_RCS.GUI_TO_RCS_DONE;
import ngat.message.base.COMMAND;
import ngat.net.IConnection;
import ngat.net.camp.CAMPRequestHandler;
import ngat.net.camp.CAMPRequestHandlerFactory;
import ngat.net.camp.CAMPServer;
import ngat.rcs.scm.collation.MappedStatusCategory;
import ngat.util.SerializableStatusCategory;
import ngat.util.StatusCategory;

/**
 * @author eng
 * 
 */
public class RunLiveStatusServer {

	static SimpleDateFormat cdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	static SimpleDateFormat adf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TimeZone.setDefault(UTC);
		cdf.setTimeZone(UTC);
		adf.setTimeZone(UTC);
		
		try {

			// Create and start a Control server - responds to GET_STATUS
			// requests.
			// This needs to access the following statuses

			// X_CLOUD - via datafile: /occ/data/cloud.dat
			// X_AGTEMP - via datafile: /occ/data/agtemp.dat
			// X_SYSTEM - via datafile: /occ/data/system.dat

			CAMPServer ssc = new CAMPServer("LiveStatusServer");
			ssc.setRequestHandlerFactory(new LiveStatusHandlerFactory());
			ssc.bind(6565);
			System.err.println("SSC bound");
			ssc.start();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	public static class LiveStatusHandlerFactory implements CAMPRequestHandlerFactory {

		public CAMPRequestHandler createHandler(IConnection connection,
				COMMAND command) {

			System.err.println("RunLiveStatus: Create New Handler for request: "+command);
			
			return new LiveStatusHandler(connection, command);
		}

	}

	public static class LiveStatusHandler implements CAMPRequestHandler {

		private IConnection connection;
		private COMMAND command;

		public LiveStatusHandler(IConnection connection, COMMAND command) {
			super();
			this.connection = connection;
			this.command = command;
		}

		public void dispose() {
			System.err.println("RunLiveStatus: Dispose connection");
			
			if (connection != null) {
				connection.close();
			}
			connection = null;
			command = null;
		}

		public long getHandlingTime() {
			return 0;
		}

		public void handleRequest() {

			GET_STATUS get = (GET_STATUS) command;

			GET_STATUS_DONE done = new GET_STATUS_DONE(command.getId());

			String cat = get.getCategory();

			if (cat == null) {
				sendError(done, GET_STATUS.UNKNOWN_CATEGORY,
						"No category supplied");
				return;
			}
			System.err.println("RunLiveStatus: Process request with cat: "+cat);
			
			StatusCategory status = null;
			try {
				if (cat.equals("X_CLOUD")) {
					status = getCloudStatus();
				} else if (cat.equals("X_AGTEMP")) {
					status = getAgTempStatus();
				} else if (cat.equals("X_SYSTEM")) {
					status = getSystemStatus();
				}
			} catch (Exception e) {
				e.printStackTrace();
				sendError(done, GET_STATUS.NOT_AVAILABLE,
						"Error getting status: " + e.getMessage());
				return;
			}

			done.setSuccessful(true);
			done.setStatus(status);
			sendDone(done);

		}

		private StatusCategory getCloudStatus() throws Exception {

			BufferedReader bin = new BufferedReader(new FileReader(
					"/home/eng/cloud.dat"));
			String line = bin.readLine();
			StringTokenizer st = new StringTokenizer(line);

			if (st.countTokens() < 7)
				throw new Exception(
						"Wrong data item count in data file: cloud.dat");

			// extract time stamp
			long timestamp = cdf.parse(st.nextToken()).getTime();

			double skyamb = Double.parseDouble(st.nextToken());
			double ambient = Double.parseDouble(st.nextToken());
			double sensor = Double.parseDouble(st.nextToken());
			double heater = Double.parseDouble(st.nextToken());
			double wetflag = Double.parseDouble(st.nextToken());
			double tdiff = Double.parseDouble(st.nextToken());

			MappedStatusCategory cloud = new MappedStatusCategory();
			cloud.addKeyword("t.diff", MappedStatusCategory.DOUBLE_DATA,
					"Sky-Amb", "C");
			cloud.addKeyword("t.ambient", MappedStatusCategory.DOUBLE_DATA, "Ambient",
					"C");
			cloud.addKeyword("t.sensor", MappedStatusCategory.DOUBLE_DATA, "Sensor",
					"C");
			cloud.addKeyword("heater", MappedStatusCategory.DOUBLE_DATA, "Heater", "ADU");
			cloud.addKeyword("wet.flag", MappedStatusCategory.DOUBLE_DATA, "Wetness",
					"");
			cloud.addKeyword("dt", MappedStatusCategory.DOUBLE_DATA, "Last Rdng", "sec");

			cloud.setTimeStamp(timestamp);
			cloud.addData("t.diff", new Double(skyamb));
			cloud.addData("t.ambient", new Double(ambient));
			cloud.addData("t.sensor", new Double(sensor));
			cloud.addData("heater", new Double(heater));
			cloud.addData("wet.flag", new Double(wetflag));
			cloud.addData("dt", new Double(tdiff));

			return cloud;
		}

		private StatusCategory getAgTempStatus() throws Exception {
			BufferedReader bin = new BufferedReader(new FileReader(
					"/home/eng/agtemp.dat"));
			String line = bin.readLine();
			StringTokenizer st = new StringTokenizer(line);

			if (st.countTokens() < 3)
				throw new Exception(
						"Wrong data item count in data file: agtemp.dat");

			// extract time stamp
			long timestamp = adf.parse(st.nextToken()).getTime();

			double unk = Double.parseDouble(st.nextToken());
			double agtemp = Double.parseDouble(st.nextToken());
			
			MappedStatusCategory agstatus = new MappedStatusCategory();
			agstatus.addKeyword("unk", MappedStatusCategory.DOUBLE_DATA, "Unk", "");
			agstatus.addKeyword("t.ag", MappedStatusCategory.DOUBLE_DATA, "Ag Temp", "C");
		
			agstatus.setTimeStamp(timestamp);
			agstatus.addData("unk", new Double(unk));
			agstatus.addData("t.ag", new Double(agtemp));
			
			return agstatus;
		}

		private StatusCategory getSystemStatus() throws Exception {
			BufferedReader bin = new BufferedReader(new FileReader(
					"/home/eng/system.dat"));
			String line = bin.readLine();
			StringTokenizer st = new StringTokenizer(line);

			if (st.countTokens() < 3)
				throw new Exception(
						"Wrong data item count in data file: system.dat");

			// extract time stamp
			long timestamp = cdf.parse(st.nextToken()).getTime();

			MappedStatusCategory sys = new MappedStatusCategory();
			
			sys.addKeyword("free.space.occ",  MappedStatusCategory.DOUBLE_DATA, "OCC Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.occ",  MappedStatusCategory.DOUBLE_DATA, "OCC Disk Usage", "%");
			sys.addKeyword("free.space.nas2", MappedStatusCategory.DOUBLE_DATA, "NAS2 Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.nas2", MappedStatusCategory.DOUBLE_DATA, "NAS2 Disk Usage", "%");
			sys.addKeyword("free.space.rise", MappedStatusCategory.DOUBLE_DATA, "RISE Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.rise", MappedStatusCategory.DOUBLE_DATA, "RISE Disk Usage", "%");
			sys.addKeyword("free.space.ringo3-1", MappedStatusCategory.DOUBLE_DATA, "Ringo3-1 Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.ringo3-1", MappedStatusCategory.DOUBLE_DATA, "Ringo3-1 Disk Usage", "%");
			sys.addKeyword("free.space.ringo3-2", MappedStatusCategory.DOUBLE_DATA, "Ringo3-2 Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.ringo3-2", MappedStatusCategory.DOUBLE_DATA, "Ringo3-2 Disk Usage", "%");
			sys.addKeyword("free.space.autoguider", MappedStatusCategory.DOUBLE_DATA, "Autoguider Disk Free Space", "kilobytes");
			sys.addKeyword("disk.usage.autoguider", MappedStatusCategory.DOUBLE_DATA, "Autoguider Disk Usage", "%");
			
			sys.setTimeStamp(timestamp);
			sys.addData("free.space.occ", new Double(st.nextToken()));
			sys.addData("disk.usage.occ", new Double(st.nextToken()));
			sys.addData("free.space.nas2", new Double(st.nextToken()));
			sys.addData("disk.usage.nas2", new Double(st.nextToken()));
			sys.addData("free.space.rise", new Double(st.nextToken()));
			sys.addData("disk.usage.rise", new Double(st.nextToken()));
			sys.addData("free.space.ringo3-1", new Double(st.nextToken()));
			sys.addData("disk.usage.ringo3-1", new Double(st.nextToken()));
			sys.addData("free.space.ringo3-2", new Double(st.nextToken()));
			sys.addData("disk.usage.ringo3-2", new Double(st.nextToken()));
			sys.addData("free.space.autoguider", new Double(st.nextToken()));
			sys.addData("disk.usage.autoguider", new Double(st.nextToken()));
			
			return sys;
		
		}

		/**
		 * Sends a done message back to client. Breaks conection if any IO
		 * errors.
		 */
		private void sendDone(GUI_TO_RCS_DONE done) {
			try {
				connection.send(done);
			} catch (Exception iox) {
				System.err.println("Error sending done: " + iox);
				dispose();
			}
		}

		/** Sends an error message back to client. */
		private void sendError(GUI_TO_RCS_DONE done, int errNo, String errMsg) {
			done.setSuccessful(false);
			done.setErrorNum(errNo);
			done.setErrorString(errMsg);
			sendDone(done);
		}

	}

}
