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
package ngat.rcs.comms;

import ngat.astrometry.*;
import ngat.util.logging.*;
import ngat.message.RCS_TCS.*;

import java.util.*;
import java.text.*;

/**
 * As a rule TCS_Status stores its fields in the form (units) they were received
 * from the TCS. E.g. angles (az.pos) etc arre in degrees etc.
 */
public class LT_RGO_SHOW_DONE_Translator implements Translator {

	protected Properties prop;

	/** Stores the key from the actual command sent. */
	protected int key;

	/** Error logging. */
	protected Logger errorLog;

	public LT_RGO_SHOW_DONE_Translator(int key) {
		this.key = key;
		errorLog = LogManager.getLogger("ERROR");
	}

	public Object translate(Object data) throws TranslationException {

		TCS_Status.Segment xstatus = null;

		if (!(data instanceof Properties))
			throw new TranslationException(
					"SHOW_DONE Data was not a java.util.Properties: "
							+ data.getClass().getName());

		prop = (Properties) data;

		try {
			switch (key) {
			// Astrometry.
			case SHOW.ASTROMETRY: {
				TCS_Status.Astrometry status = new TCS_Status.Astrometry();

				status.refractionPressure = parseDouble(1);
				status.refractionTemperature = parseDouble(2);
				status.refractionHumidity = parseDouble(3);
				status.refractionWavelength = parseDouble(4);
				status.ut1_utc = parseDouble(5);
				status.tdt_utc = parseDouble(6);
				status.polarMotion_X = parseDouble(7);
				status.polarMotion_Y = parseDouble(8);
				status.airmass = parseDouble(9);
				status.agwavelength = parseDouble(10);
				xstatus = status;
			}
				break;

			// Autoguider.
			case SHOW.AUTOGUIDER: {
				TCS_Status.Autoguider status = new TCS_Status.Autoguider();
				// System.err.println("SHOWTRANS::Parsing [SHOW AG] Response: at posn 3: "+parseString
				// (3)+
				// " ("+TCS_Status.getCode(parseString (3))+")");
				status.agSelected = parseString(1);
				status.agStatus = TCS_Status.getCode(parseString(2));
				status.agSwState = TCS_Status.getCode(parseString(3));
				status.guideStarMagnitude = parseDouble(4);
				status.fwhm = parseDouble(5);

				status.agMirrorDemand = parseDouble(6);
				status.agMirrorPos = parseDouble(7);
				status.agMirrorStatus = TCS_Status.getCode(parseString(8));

				status.agFocusDemand = parseDouble(9);
				status.agFocusPos = parseDouble(10);
				status.agFocusStatus = TCS_Status.getCode(parseString(11));

				status.agFilterDemand = TCS_Status.getCode(parseString(12));
				status.agFilterPos = TCS_Status.getCode(parseString(13));
				status.agFilterStatus = TCS_Status.getCode(parseString(14));
				xstatus = status;
				// System.err.println("SHOWTRANS::Autoguider state after parsing: "+xstatus);
			}
				break;

			// Calibrate.
			case SHOW.CALIBRATE: {
				TCS_Status.Calibrate status = new TCS_Status.Calibrate();

				status.defAzError = parseDouble(1);
				status.defAltError = parseDouble(2);
				status.defCollError = parseDouble(3);

				status.currAzError = parseDouble(4);
				status.currAltError = parseDouble(5);
				status.currCollError = parseDouble(6);

				status.lastAzError = parseDouble(7);
				status.lastAzRms = parseDouble(8);

				status.lastAltError = parseDouble(9);
				status.lastAltRms = parseDouble(10);

				status.lastCollError = parseDouble(11);
				status.lastCollRms = parseDouble(12);

				status.lastSkyRms = parseDouble(13);
				xstatus = status;
			}
				break;

			// Focus.
			case SHOW.FOCUS: {
				TCS_Status.FocalStation status = new TCS_Status.FocalStation();

				status.station = parseString(1);
				status.instr = parseString(2);
				status.ag = parseString(3);
				xstatus = status;
			}
				break;

			// Limits.
			case SHOW.LIMITS: {
				TCS_Status.Limits status = new TCS_Status.Limits();

				status.azPosLimit = parseDouble(1);
				status.azNegLimit = parseDouble(2);
				// System.err.println("RGO_Transl::Received Limits: "+status.azPosLimit+" - "+status.azNegLimit);
				status.altPosLimit = parseDouble(3);
				status.altNegLimit = parseDouble(4);

				status.rotPosLimit = parseDouble(5);
				status.rotNegLimit = parseDouble(6);

				status.timeToAzLimit = parseDouble(7, "!", 999.0) * 3600.0;
				status.azLimitSense = TCS_Status.getCode(parseString(8));

				status.timeToAltLimit = parseDouble(9, "!", 999.0) * 3600.0;
				status.altLimitSense = TCS_Status.getCode(parseString(10));

				status.timeToRotLimit = parseDouble(11, "!", 999.0) * 3600.0;
				status.rotLimitSense = TCS_Status.getCode(parseString(12));
				xstatus = status;
			}
				break;

			// Mechanisms.
			case SHOW.MECHANISMS: {
				TCS_Status.Mechanisms status = new TCS_Status.Mechanisms();

				status.azName = parseString(0);
				status.azDemand = parseDouble(1);
				status.azPos = parseDouble(2);
				status.azStatus = TCS_Status.getCode(parseString(3));

				status.altName = parseString(0);
				status.altDemand = parseDouble(4);
				status.altPos = parseDouble(5);
				status.altStatus = TCS_Status.getCode(parseString(6));

				status.rotName = parseString(0);
				status.rotDemand = parseDouble(7);
				status.rotPos = parseDouble(8);
				status.rotMode = TCS_Status.getCode(parseString(9));
				status.rotSkyAngle = parseDouble(10);
				status.rotStatus = TCS_Status.getCode(parseString(11));

				status.encShutter1Name = parseString(0);
				status.encShutter1Demand = TCS_Status.getCode(parseString(12));
				status.encShutter1Pos = TCS_Status.getCode(parseString(13));
				status.encShutter1Status = TCS_Status.getCode(parseString(14));

				status.encShutter2Name = parseString(0);
				status.encShutter2Demand = TCS_Status.getCode(parseString(15));
				status.encShutter2Pos = TCS_Status.getCode(parseString(16));
				status.encShutter2Status = TCS_Status.getCode(parseString(17));

				status.foldMirrorName = parseString(0);
				status.foldMirrorDemand = TCS_Status.getCode(parseString(18));
				status.foldMirrorPos = TCS_Status.getCode(parseString(19));
				status.foldMirrorStatus = TCS_Status.getCode(parseString(20));

				status.primMirrorName = parseString(0);
				status.primMirrorCoverDemand = TCS_Status
						.getCode(parseString(21));
				status.primMirrorCoverPos = TCS_Status.getCode(parseString(22));
				status.primMirrorCoverStatus = TCS_Status
						.getCode(parseString(23));

				status.secMirrorName = parseString(0);
				status.secMirrorDemand = parseDouble(24);
				status.secMirrorPos = parseDouble(25);
				status.focusOffset = parseDouble(26);
				System.err.println(this.getClass().getName()+
						   ":SHOW MECHANISMS::Parsing [SHOW MECHANISMS] Response 26 (focus offset): "+
						   parseString(26)+" = "+parseDouble(26));
				status.secMirrorStatus = TCS_Status.getCode(parseString(27));

				status.primMirrorSysName = parseString(0);
				status.primMirrorSysStatus = TCS_Status
						.getCode(parseString(28));

				xstatus = status;
			}
				break;

			// Meteorology.
			case SHOW.METEOROLOGY: {
				TCS_Status.Meteorology status = new TCS_Status.Meteorology();
				// System.err.println("LSDT:: Parsing meteo: ");
				status.wmsStatus = TCS_Status.getCode(parseString(1));
				status.rainState = TCS_Status.getCode(parseString(2));
				// System.err.println("LTRGO::Read rain status: "+getString(2)+" as code: "+status.rainState);
				status.serrurierTrussTemperature = parseDouble(3);
				status.oilTemperature = parseDouble(4);
				status.primMirrorTemperature = parseDouble(5);
				status.secMirrorTemperature = parseDouble(6);
				status.extTemperature = parseDouble(7);

				status.windSpeed = parseDouble(8);
				status.pressure = parseDouble(9);
				status.humidity = parseDouble(10); // fraction

				status.windDirn = parseDouble(11);

				status.moistureFraction = parseDouble(12);
				status.dewPointTemperature = parseDouble(13);
				// System.err.println("LSDT:: Extracting AGB from props");
				status.agBoxTemperature = parseDouble(14);
				// System.err.println("LSDT:: AGB="+status.agBoxTemperature);

				status.lightLevel = parseDouble(15);
				xstatus = status;
			}
				break;

			// Source.
			case SHOW.SOURCE: {
				TCS_Status.SourceBlock status = new TCS_Status.SourceBlock();

				status.srcName = parseString(1);
				status.srcRa = parseHMS(2); // RA string -> rads
				status.srcDec = parseDMS(3); // Dec string -> rads
				status.srcEquinoxLetter = parseEquinoxLetter(4);
				status.srcEquinox = parseEquinox(4); // watch for the start
														// letter. B/J/A
				status.srcEpoch = parseDouble(5);
				status.srcPmRA = parseDouble(6);
				status.srcPmDec = parseDouble(7);
				status.srcNsTrackRA = parseDouble(8);
				status.srcNsTrackDec = parseDouble(9);
				status.srcParallax = parseDouble(10);
				status.srcRadialVelocity = parseDouble(11);
				status.srcActRa = parseHMS(12); // Act RA string -> rads
				status.srcActDec = parseDMS(13); // Act Dec string -> rads
				xstatus = status;
			}
				break;

			// State.
			case SHOW.STATE: {
				TCS_Status.State status = new TCS_Status.State();

				status.networkControlState = TCS_Status.getCode(parseString(1));
				status.engineeringOverrideState = TCS_Status
						.getCode(parseString(2));
				status.telescopeState = TCS_Status.getCode(parseString(3));
				status.tcsState = TCS_Status.getCode(parseString(4));
				status.systemRestartFlag = parseBoolean(5);
				status.systemShutdownFlag = parseBoolean(6);
				xstatus = status;
			}
				break;

			// TCS Version.
			case SHOW.VERSION: {
				TCS_Status.Version status = new TCS_Status.Version();

				status.tcsVersion = parseString(1);
				xstatus = status;
			}
				break;

			// Time.
			case SHOW.TIME: {
				TCS_Status.Time status = new TCS_Status.Time();

				status.mjd = parseDouble(1);
				status.ut1 = parseDouble(2); // seconds of MJD !!!!!!
				status.lst = parseDouble(3);

				status.timeStamp = System.currentTimeMillis(); // // TEMP should
																// be the UT1
																// value really
																// !
				xstatus = status;
			}
				break;
			default:
				break;
			}
		} catch (NumberFormatException pe) {
			System.out.println("RGO_SHOW::parse error returning NULL");
			return null;
			// throw new TranslationException("Error translating data: "+pe);
		}

		// System.out.println("RGO_SHOW::Returning an inst of: "+xstatus.getClass().getName());

		// if (key == SHOW.METEOROLOGY) {
		// System.err.println("LSDT:: I am returning after parsing: "+xstatus);
		// }
		return xstatus;

	}

	private String getString(int key) {
		String data = "NOT_SET";
		Integer ik = new Integer(key);
		String args = (String) prop.get(ik);
		if (args == null)
			return "NOT_SET";
		return args;
	}

	private boolean parseBoolean(int key) {
		Integer ik = new Integer(key);
		String args = ((String) prop.get(ik)).trim();
		if (args.equals("true"))
			return true;
		return false;
	}

	private String parseString(int key) {
		String data = "NOT_SET";
		Integer ik = new Integer(key);
		String args = (String) prop.get(ik);
		if (args == null) {
			errorLog.log(3, "LT_SHOW_DONE", "parseString",
					"String was not set for key " + key);
			args = data;
		}
		return args.trim();
	}

	private long parseLong(int key) {
		long data = 0L;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		try {
			data = Long.parseLong(val);
		} catch (NumberFormatException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseLong",
					"Error parsing Long from key: [" + key + "] = [" + val
							+ "]");
		}
		return data;
	}

	private double parseDouble(int key) {
		double data = 0.0;
		Integer ik = new Integer(key);
		String str = ((String) prop.get(ik)).trim();
		if (str == null)
			return 0.0;
		String val = str.trim();
		try {
			data = Double.parseDouble(val);
		} catch (NumberFormatException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseDouble",
					"Error parsing Double from key: [" + key + "] = [" + val
							+ "]");
		}
		return data;
	}

	/**
	 * Parse the String referenced by the specified arg number and if the String
	 * starts with char return the option value else the parsed value.
	 */
	private double parseDouble(int key, String optchar, double option) {
		double data = 0.0;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		if (val.startsWith(optchar))
			return option;
		try {
			data = Double.parseDouble(val);
		} catch (NumberFormatException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseDouble",
					"Error parsing Double from key: [" + key + "] = [" + val
							+ "]");
		}
		return data;
	}

	private int parseInt(int key) {
		int data = 0;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		try {
			data = Integer.parseInt(val);
		} catch (NumberFormatException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseInt",
					"Error parsing Int from key: [" + key + "] = [" + val + "]");
		}
		return data;
	}

	/** Returns an RA in degrees from sexagesimal form. */
	private double parseHMS(int key) {
		double data = 0.0;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		// System.err.println("StatusTranslator:: Parsing received HMS: "+val);
		try {
			data = Position.parseHMS(val, " ");
			// System.err.println("StatusTranslator:: Returning parsed HMS rads: "+data);
		} catch (ParseException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseHMS",
					"Error parsing HMS from key: [" + key + "] = [" + val + "]");
		}
		return data;
	}

	/** Returns a Dec in degrees from sexagesimal form. */
	private double parseDMS(int key) {
		double data = 0.0;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		// System.err.println("StatusTranslator:: Parsing received DMS: "+val);
		try {
			data = Position.parseDMS(val, " ");
			// System.err.println("StatusTranslator:: Returning parsed DMS rads: "+data);
		} catch (ParseException e) {
			errorLog.log(3, "LT_SHOW_DONE", "parseDMS",
					"Error parsing DMS from key: [" + key + "] = [" + val + "]");
		}
		return data;
	}

	private String parseEquinoxLetter(int key) {
		String data = "X";
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		if (val.startsWith("J"))
			return "J";
		else if (val.startsWith("B"))
			return "B";
		else if (val.startsWith("A"))
			return "A";
		else
			return "X";
	}

	/** NOte if the Start letter is A or something weird we get 0.0 returned. */
	private double parseEquinox(int key) {
		double data = 0.0;
		Integer ik = new Integer(key);
		String val = ((String) prop.get(ik)).trim();
		if (val.startsWith("J") || val.startsWith("B")) {
			val = val.substring(1);
			try {
				data = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				errorLog.log(3, "LT_SHOW_DONE", "parseEquinox",
						"Error parsing Equinox from key: [" + key + "] = ["
								+ val + "]");
			}
			return data;
		} else if (val.startsWith("A"))
			return 0.0;
		else
			return 0.0;
	}
}
