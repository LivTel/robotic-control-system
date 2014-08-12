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
package ngat.rcs.scm.collation;

import ngat.util.*;

import java.io.*;
import java.util.*;
import java.text.*;

/** MappedStatusCategory is a Map representation of status information.
 * The keys are the status keys and the entries the (possibly wrapped)
 * numeric/string values for the statii.
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: MappedStatusCategory.java,v 1.1 2006/12/12 08:30:52 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/scm/collation/RCS/MappedStatusCategory.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */

public class MappedStatusCategory implements SerializableStatusCategory {

    /** Code to represent Integer data type.*/
    public static final int INTEGER_DATA = 1;
    
    /** Code to represent Double data type.*/
    public static final int DOUBLE_DATA  = 2;
    
    /** Code to represent String data type.*/
    public static final int STRING_DATA  = 3;

    /** For integers.*/
    static NumberFormat nf;

    /** For Doubles.*/
    static NumberFormat df;

    /** For time.*/
    static SimpleDateFormat sdf;

    /** UTC Timezone.*/
    static final SimpleTimeZone UTC;

    /** Mapping from keys to data.*/
    Map data;

    /** Mapping from keys to data-description.*/
    Map entries;

    /** Mapping of integer codes to names.*/
    Map codes;

    /** Current timestamp.*/
    protected long timeStamp;

    static {
	nf = NumberFormat.getInstance();
	nf.setParseIntegerOnly(true);
	nf.setGroupingUsed(false);

	df = NumberFormat.getInstance();
	df.setMaximumFractionDigits(5);  

	UTC = new SimpleTimeZone(0, "UTC");
	sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss z");
	sdf.setTimeZone(UTC);
    }


    /** Create a MappedStatusCategory.*/
    public MappedStatusCategory() {
	data    = Collections.synchronizedMap(new HashMap());
	entries = Collections.synchronizedMap(new HashMap());
	codes   = Collections.synchronizedMap(new HashMap());
    }

    /** Add an entry to the code table.
     * @param code The numeric code.
     * @param name The name to assocaite with the code number.
     */
    public void addCode(int code, String name) {
	codes.put(new Integer(code), name);
    }

    /** Add an entry for keyword of selected data-type.
     * @param keyword The Key used to extract the data.
     * @param type    Data-type (INT, DBLE, STRG).
     */
    public void addKeyword(String keyword, int type, String desc, String units) {
	entries.put(keyword, new MapEntry(keyword, type, desc, units));	
	System.err.println("MappedStatusCategory: Added Key: "+keyword);
    }

    /** Returns the MapEntry for specified keyword or null if not found
     * @param keyword The keyword.
     */
    public MapEntry getMapEntry(String keyword) {
	if (! entries.containsKey(keyword))
	    return null;
	return (MapEntry)entries.get(keyword);
    }

    /** Update the data entry for keyword.
     * @param keyword The Key used to extract the data.
     * @param value   The data for the selected keyword.
     */
    public void addData(String keyword, Object value) throws IllegalArgumentException {
	if (! entries.containsKey(keyword))
	    throw new IllegalArgumentException("MappedStatusCategory: Unknown key: "+keyword);

	int t = ((MapEntry)entries.get(keyword)).type;
	switch (t) {
	case INTEGER_DATA:
	    if (! (value instanceof Integer))
		throw new 
		    IllegalArgumentException("MappedStatusCategory:"+
					     " Key: "+keyword+
					     " Expected Integer, found: "+value);
	    data.put(keyword, value);
	    break;
	case DOUBLE_DATA:
	    if (! (value instanceof Double))
		throw new 
		    IllegalArgumentException("MappedStatusCategory:"+
					     " Key: "+keyword+
					     " Expected Double, found: "+value);
	    data.put(keyword, value);
	    break;
	case STRING_DATA:
	    if (! (value instanceof String))
		throw new 
		    IllegalArgumentException("MappedStatusCategory:"+
					     " Key: "+keyword+
					     " Expected String, found: "+value);
	    data.put(keyword, value);
	    break;
	default:
	    // Illegal data type !
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " Key: "+keyword+
					 " Data-type incompatible");
	}

	//System.err.println("MappedStatusCategory: Added data: "+value+" For: "+keyword);
	
    }

    /** This method is intended for descriptive (String) status variables.
     */
    public String getStatusEntryId(String keyword) throws IllegalArgumentException {
	
	if (! data.containsKey(keyword))
	    throw new IllegalArgumentException("MappedStatusCategory:"+
					       " Unknown key: "+keyword);
	int t = ((MapEntry)entries.get(keyword)).type;

	if (t != STRING_DATA)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " Key: "+keyword+" Not an integer");
	
	Object value = data.get(keyword);
	
	if (data == null)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " No data available for key: "+keyword);
	
	return (String)value;
	
    }
    
    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for continuous status variables.
     */
    public int getStatusEntryInt(String keyword) throws IllegalArgumentException {
	
	if (! data.containsKey(keyword))
	    throw new IllegalArgumentException("MappedStatusCategory:"+
					       " Unknown key: "+keyword);
	int t = ((MapEntry)entries.get(keyword)).type;

	if (t != INTEGER_DATA)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " Key: "+keyword+" Not an integer");
	
	Object value = data.get(keyword);
	
	if (data == null)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " No data available for key: "+keyword);
	
	return ((Integer)value).intValue();
	
    }
     
    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. This method is
     * intended for discrete status variables.
     */
    public double getStatusEntryDouble(String keyword) throws IllegalArgumentException {
	
	if (! data.containsKey(keyword))
	    throw new IllegalArgumentException("MappedStatusCategory:"+
					       " Unknown key: "+keyword);
	int t = ((MapEntry)entries.get(keyword)).type;

	if (t != DOUBLE_DATA)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " Key: "+keyword+" Not a double");
	
	Object value = data.get(keyword);
	
	if (data == null)
	    throw new 
		IllegalArgumentException("MappedStatusCategory:"+
					 " No data available for key: "+keyword);
	
	return ((Double)value).doubleValue();
	
    }

    /** Implementors should return status identified by the supplied key or throw 
     * an IllegalArgumentException if no such status exists. No type conversion 
     * should be attempted.
     */
    public String getStatusEntryRaw(String keyword) throws IllegalArgumentException {
	
	if (! data.containsKey(keyword))
	    throw new IllegalArgumentException("MappedStatusCategory:"+
					       " Unknown key: "+keyword);	
	Object value = data.get(keyword);
	
	return ""+value;
	
    }
     
    /** Used to set the timestamp.*/  
    public void setTimeStamp(long timeStamp) {
	this.timeStamp = timeStamp;
    }

    /** Implementors should return the timestamp of the latest readings.*/
    public long getTimeStamp() {	
	return timeStamp;
    }

    /** Returns a readable version of the MappedStatusCategory.*/
    @Override
	public String toString() {

	StringBuffer buffer = new StringBuffer("TimeStamp="+sdf.format(new Date(timeStamp)));

	String   dval  = "";
	String   key   = null;
	MapEntry entry = null;

	Iterator it = entries.keySet().iterator();
	while (it.hasNext()) {

	    key   = (String)it.next();	    
	    entry = (MapEntry)entries.get(key);
	  
	    try {	
		switch (entry.type) {
		case DOUBLE_DATA:
		    dval = df.format(getStatusEntryDouble(key));
		    break;
		case INTEGER_DATA:
		    int c = getStatusEntryInt(key);
		    Integer cc = new Integer(c);
		    if (codes.containsKey(cc)) 
			dval = (String)codes.get(cc);
		    else
			dval = "["+nf.format(c)+"]";
		    break;
		case STRING_DATA:
		    dval = getStatusEntryId(key);
		    break;		 
		default:
		    dval = getStatusEntryRaw(key);
		}
	    } catch (IllegalArgumentException iax) {
		dval = "?ERROR?";
		//iax.printStackTrace();
	    }
	    
	
	    buffer.append(", "+entry.desc+"="+dval+" "+entry.units);
	    
	}

	return buffer.toString();
    }

    /** Stores an entry.*/
    public static class MapEntry implements Serializable {

	public String keyword;

	public int type;

	public String desc;

	public String units;

	MapEntry() { }

	/** Create a MapEntry.*/
	MapEntry(String keyword, int type, String desc, String units) {
	    this.keyword = keyword;
	    this.type    = type;
	    this.desc    = desc;
	    this.units   = units;
	}

    }

}

/** $Log: MappedStatusCategory.java,v $
/** Revision 1.1  2006/12/12 08:30:52  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:34:57  snf
/** Initial revision
/** */
