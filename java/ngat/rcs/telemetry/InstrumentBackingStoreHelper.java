/**
 * 
 */
package ngat.rcs.telemetry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.InstrumentStatus;
import ngat.net.telemetry.MysqlBackingStore;
import ngat.net.telemetry.StatusCategory;

/**
 * @author eng
 *
 */
public class InstrumentBackingStoreHelper extends MysqlBackingStore {

	private static final String INSERT = "insert into instrument (time, instname, status, online, ref) values (?,?, ?, ?, ?)";
	
	private static final String INSERT2 = "insert into instref (ref, pkey, dvalue, ivalue) values (?, ?, ?, ?)";
	
	private InstrumentRegistry ireg;
	
	private PreparedStatement storeStatement;
	private PreparedStatement storeStatement2;
	
	/**
	 * @param mysqlUrl
	 */
	public InstrumentBackingStoreHelper(InstrumentRegistry ireg, String mysqlUrl) throws Exception {
		super(mysqlUrl);
		this.ireg = ireg;
	
	}



	@Override
	protected void prepareStatements(Connection connection) throws Exception {

		storeStatement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
		storeStatement2 = connection.prepareStatement(INSERT2, Statement.RETURN_GENERATED_KEYS);
		
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.MysqlBackingStoreHelper#createStoreQuery(ngat.net.telemetry.StatusCategory)
	 */
	@Override
	public void storeStatus(StatusCategory status) throws Exception {
		InstrumentStatus istat = (InstrumentStatus)status;
		
		
		
		int iid = 0;
		String iname = istat.getInstrument().getInstrumentName().toUpperCase();
		if (iname.equals("RATCAM"))
			iid = 1;
		else if
		(iname.equals("IO:O"))
			iid = 2;
		else if
		(iname.equals("IO:THOR"))
			iid = 3;
		else if
		(iname.equals("RINGO3"))
			iid = 4;
		else if
		(iname.equals("RISE"))
			iid = 5;
		else if
		(iname.equals("FRODO"))
			iid = 6;
		
		storeStatement.setDouble(1, istat.getStatusTimeStamp()/1000.0);
		storeStatement.setInt(2, iid);
		storeStatement.setInt(3, (istat.isOnline() ? 1 : 0));
		storeStatement.setInt(3, (istat.isFunctional() ? 1 : 0));
		
		storeStatement.executeUpdate();
		
		  int tid = -1;
          ResultSet results = storeStatement.getGeneratedKeys();
          while (results.next()) {
              tid = results.getInt(1);
              System.err.println("Record number: "+tid);
          }
          
          Map statusMap = istat.getStatus();
          
          if (tid != -1) {
        	  // add ref values
        	  if (iname.equals("RATCAM")) {   			
        		  storeStatement2.setInt(1, tid);
        		  storeStatement2.setString(2, "temperature");
        		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature")).doubleValue()); 	
        		  storeStatement2.setInt(4, 0);
        		  storeStatement2.executeUpdate();
        	  } else if
      		(iname.equals("IO:O")) {
        		  storeStatement2.setInt(1, tid);
        		  storeStatement2.setString(2, "temperature");
        		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature")).doubleValue()); 	
        		  storeStatement2.setInt(4, 0);
        		  storeStatement2.executeUpdate();
      		} else if
      		(iname.equals("IO:THOR")) {
      		  storeStatement2.setInt(1, tid);
    		  storeStatement2.setString(2, "temperature");
    		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature")).doubleValue()); 	
    		  storeStatement2.setInt(4, 0);
    		  storeStatement2.executeUpdate();
      		} else if
      		(iname.equals("RINGO3")) {
      		  storeStatement2.setInt(1, tid);
    		  storeStatement2.setString(2, "temperature.0.0");
    		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature.0.0")).doubleValue()); 	
    		  storeStatement2.setInt(4, 0);
    		  storeStatement2.executeUpdate();
    		  
    		  storeStatement2.setInt(1, tid);
    		  storeStatement2.setString(2, "temperature.1.0");
    		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature.1.0")).doubleValue()); 	
    		  storeStatement2.setInt(4, 0);
    		  storeStatement2.executeUpdate();
    		  
    		  storeStatement2.setInt(1, tid);
    		  storeStatement2.setString(2, "temperature.1.1");
    		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature.1.1")).doubleValue()); 	
    		  storeStatement2.setInt(4, 0);
    		  storeStatement2.executeUpdate();
    		  
      		} else if
      		(iname.equals("RISE")) {
      		  storeStatement2.setInt(1, tid);
    		  storeStatement2.setString(2, "temperature");
    		  storeStatement2.setDouble(3,((Double)statusMap.get("Temperature")).doubleValue()); 	
    		  storeStatement2.setInt(4, 0);
    		  storeStatement2.executeUpdate();
      		} else if
      		(iname.equals("FRODO")) {
      			 storeStatement2.setInt(1, tid);
       		  storeStatement2.setString(2, "red.temperature");
       		  storeStatement2.setDouble(3,((Double)statusMap.get("red.Temperature")).doubleValue()); 	
       		  storeStatement2.setInt(4, 0);
       		  storeStatement2.executeUpdate();
       		  
       		  storeStatement2.setInt(1, tid);
       		  storeStatement2.setString(2, "blue.temperature");
       		  storeStatement2.setDouble(3,((Double)statusMap.get("blue.Temperature")).doubleValue()); 	
       		  storeStatement2.setInt(4, 0);
       		  storeStatement2.executeUpdate();
      			
      		}
      		
        	  
          }

						
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.MysqlBackingStoreHelper#processResults(java.sql.ResultSet)
	 */
	public List<StatusCategory> processResults(ResultSet rs) throws Exception {
		List<StatusCategory> list = new Vector<StatusCategory>();
		while (rs.next()) {
			InstrumentStatus status = new InstrumentStatus();
			status.setStatusTimeStamp(rs.getLong(0));		
			String instrumentName = rs.getString(1);
			
			InstrumentDescriptor id = ireg.getDescriptor(instrumentName);
			status.setInstrument(id);
			
			status.setOnline(rs.getBoolean(2));
			status.setFunctional(rs.getBoolean(4));
			list.add(status);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see ngat.rcs.telemetry.MysqlBackingStoreHelper#createRetrieveQuery(long, long)
	 */
	@Override
	public List<StatusCategory>  retrieveStatus(long t1, long t2) throws Exception {
		// TODO Auto-generated method stub
		//return "SELECT time, instName, onlineStatus, healthStatus from instrument where time > "+t1+
			//	"and time < "+t2+
			//	" order by time"; 
		return null;
	}

}
