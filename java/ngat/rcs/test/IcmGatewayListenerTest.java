/**
 * 
 */
package ngat.rcs.test;

import ngat.icm.BasicInstrumentUpdateListener;
import ngat.rcs.telemetry.InstrumentArchiveGateway;

/**
 * @author eng
 *
 */
public class IcmGatewayListenerTest {

	private InstrumentArchiveGateway gw;
	
	
	
	/**
	 * @param gw
	 */
	public IcmGatewayListenerTest(InstrumentArchiveGateway gw) {
		super();
		this.gw = gw;
	}

	public void exec() throws Exception {
		
		BasicInstrumentUpdateListener bul = new BasicInstrumentUpdateListener("test");
		gw.addInstrumentStatusUpdateListener(bul);
		
	}
	
	

}
