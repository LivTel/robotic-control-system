/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import ngat.phase2.ITimePeriod;

/**
 * @author eng
 *
 */
public interface AdvancedBookingModel extends Remote {

	/** @return The next booked period after time.*/
	public ITimePeriod nextBooking(long time) throws RemoteException;
	
	/** @return A list of booked periods after time.*/
	public List listAdvanceBookings(long time) throws RemoteException;
	
}
