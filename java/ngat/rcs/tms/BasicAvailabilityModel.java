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
package ngat.rcs.tms;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.text.*;
import ngat.message.GUI_RCS.*;

public class BasicAvailabilityModel extends UnicastRemoteObject implements AvailabilityModel {
    
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm");
    
    /** Create a BasicExecTimingModel.*/
    public BasicAvailabilityModel() throws RemoteException {
    	super();
    }
    

    /** Returns true if the specified time is available for scheduled operaitons.
     * @param time when we want to check.
     */
    public boolean isAvailable(long time) throws RemoteException {
	
	//System.err.println("BEXTM: Call isAvailable, checking for current window");
	//WindowSchedule.TimeWindow wif = TMM_TaskSequencer.getInstance().getWindow(time);
	//System.err.println("BEXTM: Call isAvailable found current window: "+wif);
	
	// were in a window right now, so fail.
	//if (wif != null)
	//  return false;
	
	// not a window, so pass.
	return true;
    }

    
    /** Returns true if the specified time interval is fully available for scheduled operations.
     * @param start the start of the period of interest.
     * @param end the end of the period of interest.
     */
    public boolean isAvailable(long start, long end) throws RemoteException {
	
	// all we care about is: Is there a Window In Force (WIF) at time
	// being more pernickity, would like to know if any window overlaps
	// with the execution of this group if started at time.
    
	// Note: overlap = [max(t, ws), min(t+x, we)]
	
	//System.err.println("BEXTM: Call isFeasible, checking for current window");
	//W/indowSchedule.TimeWindow wif = TMM_TaskSequencer.getInstance().getWindow(start);
	//System.err.println("BEXTM: Call isFeasible found current window: "+wif);

	// were in a window right now, so fail.
	//i/f (wif != null)
	//  return false;

	//System.err.println("BEXTM: Call isFeasible, checking for next window");
	//WindowSchedule.TimeWindow nxw = TMM_TaskSequencer.getInstance().getNextMcaWindow("PCA", start);
	//System.err.println("BEXTM: Call isFeasible found next window: "+nxw);

	// no more windows, so pass.
	//if (nxw == null)
	//  return true;

	// work out the overlap if any..
	
	///System.err.println("BEXTM: Call isFeasible, working out overlap if any");
	//System.err.println("BEXTM: Call isFeasible, P.start = "+sdf.format(new Date(start)));
	//System.err.println("BEXTM: Call isFeasible, P.end   = "+sdf.format(new Date(end)));
	//System.err.println("BEXTM: Call isFeasible, w.start = "+sdf.format(new Date(nxw.t1)));
	//System.err.println("BEXTM: Call isFeasible, w.end   = "+sdf.format(new Date(nxw.t2)));

	//long tos = Math.max(start, nxw.t1);
	//long toe = Math.min(end, nxw.t2);

	//System.err.println("BEXTM: Call isFeasible, working out overlap if any...["+
	//	   sdf.format(new Date(tos))+", "+sdf.format(new Date(toe))+"]");
	
	// no overlap, so pass.
	//if (toe > tos)
	//  return false;

	return true;
    
    }
    
 
    /** Returns a list of times which are UNavailable for scheduled operations
     * between start and end - inclusive/overlapping.
     * @param start the start of the period of interest.
     * @param end the end of the period of interest.
     */
    public List getUnavailableTimes(long start, long end) throws RemoteException {
	
	List list = new Vector();
	//TreeSet windows = TMM_TaskSequencer.getInstance().getScheduledWindows();
	
	//Iterator it = windows.iterator();
	//while (it.hasNext()) {	    
	//  WindowSchedule.TimeWindow w = (WindowSchedule.TimeWindow)it.next();
	//  ngat.util.TimeWindow tw = new ngat.util.TimeWindow(w.t1, w.t2);
	//  list.add(tw);
	//}

	return list;

    }

}
