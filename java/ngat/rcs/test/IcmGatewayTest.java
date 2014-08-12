/**
 * 
 */
package ngat.rcs.test;

import java.rmi.Naming;
import java.util.List;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatusProvider;
import ngat.net.telemetry.SecondaryCache;
import ngat.rcs.telemetry.InstrumentArchiveGateway;
import ngat.rcs.telemetry.InstrumentBackingStoreHelper;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class IcmGatewayTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			String host = args[0];

			Logger logger = LogManager.getLogger("ICM");
			ConsoleLogHandler console = new ConsoleLogHandler(new BasicLogFormatter(150));
			console.setLogLevel(5);
			logger.addExtendedHandler(console);
			logger.setLogLevel(5);

			InstrumentRegistry ireg = (InstrumentRegistry) Naming.lookup("rmi://" + host + "/InstrumentRegistry");
			System.err.println("Found ireg: " + ireg);

			//TextFileBackingStore bs = new TextFileBackingStore(new File("/home/eng/test.status"));
			
			String url = "jdbc:mysql://localhost/telemetry?user=data&password=banoffeelogger67";
		
			SecondaryCache msb = new InstrumentBackingStoreHelper(ireg, url);
			
			InstrumentArchiveGateway iag = new InstrumentArchiveGateway(ireg);
			//iag.setBackingStore(msb);
			iag.setBackingStore(msb);
			iag.setBackingStoreAgeLimit(10 * 60 * 1000L);
			iag.setProcessInterval(10000L);
			iag.setCullInterval(2*60*1000L);

			List insts = ireg.listInstruments();
			for (int ii = 0; ii < insts.size(); ii++) {
				InstrumentDescriptor id = (InstrumentDescriptor) insts.get(ii);
				InstrumentStatusProvider isp = ireg.getStatusProvider(id);
				System.err.println("Attaching to status provider for: " + id.getInstrumentName());
				isp.addInstrumentStatusUpdateListener(iag);
			}
			
			iag.startProcessor();
			
			IcmGatewayListenerTest test = new IcmGatewayListenerTest(iag);
			test.exec();
			

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
