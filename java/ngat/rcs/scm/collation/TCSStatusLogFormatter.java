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
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.io.*;
import java.util.*;
import java.text.*;

public class TCSStatusLogFormatter implements StatusLogFormatter {

    public static SimpleDateFormat     sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
    public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    static {
	sdf.setTimeZone(UTC);
    }

    String delim = " ";

    public TCSStatusLogFormatter() {}

    public void output(StatusCategory cat, PrintStream out) {

	if (cat == null)
	    return;

	if (cat instanceof TCS_Status.Mechanisms) 
	    outputMechanisms(cat,out);
	else if
	    (cat instanceof TCS_Status.Meteorology)
	    outputMeteorology(cat,out);
	else
	    out.println(""+cat);
    }

    private void outputMechanisms(StatusCategory cat, PrintStream out) {
	TCS_Status.Mechanisms mech = (TCS_Status.Mechanisms)cat;
	StringBuffer buff = new StringBuffer();
	buff.append(sdf.format(new Date(mech.getTimeStamp()))+delim);

	buff.append(""+mech.azDemand+delim);
	buff.append(""+mech.azPos+delim);

	buff.append(""+mech.altDemand+delim);
	buff.append(""+mech.altPos+delim);

	buff.append(""+mech.rotDemand+delim);
	buff.append(""+mech.rotPos+delim);
	buff.append(""+TCS_Status.codeString(mech.rotMode)+delim);
	buff.append(""+mech.rotSkyAngle+delim);
	buff.append(""+TCS_Status.codeString(mech.rotStatus)+delim);

	buff.append(""+TCS_Status.codeString(mech.encShutter1Demand)+delim);
	buff.append(""+TCS_Status.codeString(mech.encShutter1Pos)+delim);
	buff.append(""+TCS_Status.codeString(mech.encShutter1Status)+delim);
	buff.append(""+TCS_Status.codeString(mech.encShutter2Demand)+delim);
	buff.append(""+TCS_Status.codeString(mech.encShutter2Pos)+delim);
	buff.append(""+TCS_Status.codeString(mech.encShutter2Status)+delim);

	buff.append(""+TCS_Status.codeString(mech.foldMirrorDemand)+delim);
	buff.append(""+TCS_Status.codeString(mech.foldMirrorPos)+delim);
	buff.append(""+TCS_Status.codeString(mech.foldMirrorStatus)+delim);

	buff.append(""+TCS_Status.codeString(mech.primMirrorCoverDemand)+delim);
	buff.append(""+TCS_Status.codeString(mech.primMirrorCoverPos)+delim);
	buff.append(""+TCS_Status.codeString(mech.primMirrorCoverStatus)+delim);

	buff.append(""+mech.secMirrorDemand+delim);
	buff.append(""+mech.secMirrorPos+delim);
	buff.append(""+mech.focusOffset+delim);
	buff.append(""+TCS_Status.codeString(mech.secMirrorStatus)+delim);

	buff.append(""+TCS_Status.codeString(mech.primMirrorSysStatus)+delim);

	out.println(buff.toString());
	buff = null;

    }

     private void outputMeteorology(StatusCategory cat, PrintStream out) {
	TCS_Status.Meteorology meteo = (TCS_Status.Meteorology)cat;
	StringBuffer buff = new StringBuffer();
	buff.append(sdf.format(new Date(meteo.getTimeStamp()))+delim);

	buff.append(""+TCS_Status.codeString(meteo.wmsStatus)+delim);
	buff.append(""+TCS_Status.codeString(meteo.rainState)+delim);
	buff.append(""+meteo.moistureFraction+delim);
	buff.append(""+meteo.serrurierTrussTemperature+delim);

	buff.append(""+meteo.oilTemperature+delim);
	buff.append(""+meteo.primMirrorTemperature+delim);
	buff.append(""+meteo.secMirrorTemperature+delim);
	buff.append(""+meteo.agBoxTemperature+delim);

	buff.append(""+meteo.extTemperature+delim);
	buff.append(""+meteo.dewPointTemperature+delim);
	buff.append(""+meteo.windSpeed+delim);
	buff.append(""+meteo.pressure+delim);	
	buff.append(""+(meteo.humidity*100.0)+delim);
	buff.append(""+meteo.windDirn+delim);
	buff.append(""+meteo.lightLevel+delim);

	out.println(buff.toString());
	buff = null;

    }

    

}
