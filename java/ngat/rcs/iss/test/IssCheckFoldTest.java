/**
 * 
 */
package ngat.rcs.iss.test;

import ngat.rcs.iss.IssScienceFoldPlugin;
import ngat.util.logging.BasicLogFormatter;
import ngat.util.logging.ConsoleLogHandler;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 *
 */
public class IssCheckFoldTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger logger = LogManager.getLogger("TASK");
		ConsoleLogHandler con = new ConsoleLogHandler(new BasicLogFormatter(150));
		con.setLogLevel(5);
		logger.addExtendedHandler(con);	
		logger.setLogLevel(5);

		try {		  
		    IssScienceFoldPlugin sfd = new IssScienceFoldPlugin();
		    sfd.checkPosition();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

}
