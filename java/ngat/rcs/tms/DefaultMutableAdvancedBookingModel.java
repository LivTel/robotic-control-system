/**
 * 
 */
package ngat.rcs.tms;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import ngat.phase2.ITimePeriod;

/**
 * @author eng
 * 
 */
public class DefaultMutableAdvancedBookingModel extends UnicastRemoteObject implements AdvancedBookingModel {

	private List bookings;

	/**
	 * @throws RemoteException
	 */
	public DefaultMutableAdvancedBookingModel() throws RemoteException {
		super();
		bookings = new Vector();
	}

	
	/** Add a new time period to the bookings list. 
	 * If it matches an existing period, dont bother.
	 * If its more than 18 hours ahead, dont bother.
	 * @param period
	 */
	public void addBooking(ITimePeriod period) {
		if (period.getStart() > System.currentTimeMillis() + 18*3600*1000L)
			return;
		Iterator ib = bookings.iterator();
		while (ib.hasNext()) {
			ITimePeriod aperiod = (ITimePeriod) ib.next();
			if (aperiod.getStart() == period.getStart() &&
				aperiod.getEnd() == period.getEnd())
				return;
		}
		bookings.add(period);
	}
	
	/**
	 * Returns a list of bookings at or after time.
	 * 
	 * @param time
	 *            The time after which we want bookings.
	 * @see ngat.rcs.tms.AdvancedBookingModel#listAdvanceBookings(long)
	 */
	public List listAdvanceBookings(long time) throws RemoteException {
		List temp = new Vector();
		Iterator ib = bookings.iterator();
		while (ib.hasNext()) {
			ITimePeriod period = (ITimePeriod) ib.next();
			if (period.getStart() > time)
				temp.add(period);
		}
		return temp;
	}

	/**
	 * Returns the next booking starting at or after time. Will be null if none.
	 * 
	 * @param time
	 *            The time after which we want bookings.
	 * @see ngat.rcs.tms.AdvancedBookingModel#nextBooking(long)
	 */
	public ITimePeriod nextBooking(long time) throws RemoteException {

		ITimePeriod best = null;
		Iterator ib = bookings.iterator();
		while (ib.hasNext()) {
			ITimePeriod period = (ITimePeriod) ib.next();
			if ((best == null) || ((period.getStart() >= time) && (period.getStart() < best.getStart())))
				best = period;
		}
		return best;
	}

}
