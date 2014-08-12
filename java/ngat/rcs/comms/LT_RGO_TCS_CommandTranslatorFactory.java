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

import ngat.phase2.*;
import ngat.astrometry.*;
import ngat.message.base.*;
import ngat.message.RCS_TCS.*;
import ngat.util.logging.*;

import java.text.*;
import java.util.*;

/**
 * Implementation of a CommandTranslator for the Liverpool Telescope RGO
 * Telescope Control System. All source command objects must be instances of
 * ngat.message.RCS_TCS.RCS_TO_TCS or subclasses. <br>
 * <br>
 * $Id: LT_RGO_TCS_CommandTranslatorFactory.java,v 1.9 2009/01/26 11:30:22 eng
 * Exp eng $
 */
public class LT_RGO_TCS_CommandTranslatorFactory implements CommandTranslatorFactory {

	public static final int OK = 0;

	public static final String CLASS = "LT(RGO)_TCS_CommandTranslatorFactory";

	public static final int ILLEGAL_CIL_RETURN_TYPE = 606501;

	public static final int CIL_RESPONSE_PARSE_ERROR = 606502;

	public static final int LT_RGO_TRANSLATION_ERROR = 606503;

	/** Telescope/site - specific default value for atmospheric pressure (mbar). */
	public static final double DEFAULT_PRESSURE = 750.0;

	/** Telescope/site - specific default value for air humidity (fraction). */
	public static final double DEFAULT_HUMIDITY = 0.2;

	/** Telescope/site - specific default value for air temperature (C). */
	public static final double DEFAULT_TEMPERATURE = 20.0;

	/** Telescope/site - specific default value for wavelength (nm). */
	public static final double DEFAULT_WAVELENGTH = 500.0;

	/** Indicates not to send real ENC and OPER commands. */
	public static final int TCS_SIM = 2;

	/** Indicates to send special ENC state values. */
	public static final int SYS_SIM = 3;

	/** Full command output. */
	public static final int SYS_AUTO = 0;

	protected static int tcsMode = SYS_AUTO;

	protected NumberFormat nf;
	protected NumberFormat nf8;

	protected Logger logger;

	static {
		tcsMode = TCS_SIM;
		String strMode = System.getProperty("TCS_MODE");
		if (strMode.equals("TCS_SIM")) {
			tcsMode = TCS_SIM;
		} else if (strMode.equals("SYS_SIM")) {
			tcsMode = SYS_SIM;
		} else if (strMode.equals("SYS_AUTO"))
			tcsMode = SYS_AUTO;

		System.err.println("TCS Using Mode; " + strMode + "(" + tcsMode + ")");

	}

	/** The singleton instance. */
	private static LT_RGO_TCS_CommandTranslatorFactory instance = null;

	/** @return A reference to the singleton instance. */
	public static LT_RGO_TCS_CommandTranslatorFactory getInstance() {
		if (instance == null)
			instance = new LT_RGO_TCS_CommandTranslatorFactory();
		return instance;
	}

	/** Creates the singleton instance. */
	private LT_RGO_TCS_CommandTranslatorFactory() {
		nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(3);
		nf.setMaximumFractionDigits(3);
		nf.setGroupingUsed(false);

		nf8 = NumberFormat.getInstance();
		nf8.setMinimumFractionDigits(8);
		nf8.setMaximumFractionDigits(8);
		nf8.setGroupingUsed(false);

		logger = LogManager.getLogger("COMMAND");
	}

	/**
	 * Translates a supplied RCS_TO_TCS command into a String suitable for the
	 * RGO TCS. If command is null or of the wrong (subclass) type, returns
	 * null.
	 * 
	 * @param command
	 *            A subclass of ngat.message.RCS_TCS.RCS_TO_TCS - the command to
	 *            translate.
	 * @return The translated command String.
	 */
	public Object translateCommand(Object command) {
		if ((command == null) || (!(command instanceof RCS_TO_TCS)))
			return null;

		// Calculate the string for an RGO TCS Command.

		if (command instanceof AGCENTROID) {
			return "AGCENTROID";
		}

		if (command instanceof AGFILTER) {

			switch (((AGFILTER) command).getState()) {
			case AGFILTER.IN:
				return "AGFILTER IN";
			case AGFILTER.OUT:
				return "AGFILTER OUT";
			}
		}

		if (command instanceof AGFOCUS) {
			return "AGFOCUS " + ((AGFOCUS) command).getFocus();
		}

		if (command instanceof AGRADIAL) {
			// return "AGMOVE "+((AGMOVE)command).getPosition() +
			// " " + ((AGMOVE)command).getAngle();

			return "AGRADIAL " + nf.format(((AGRADIAL) command).getPosition());
		}

		if (command instanceof AGSELECT) {
			switch (((AGSELECT) command).getAutoguider()) {
			case AGSELECT.CASSEGRAIN:
				return "AGSELECT CASSEGRAIN";
			}
		}

		if (command instanceof AGVIEW) {
			return "AGVIEW";
		}

		if (command instanceof AGWAVELENGTH) {
			double agwave = ((AGWAVELENGTH) command).getWavelength();
			if (agwave < 200.0 || agwave > 2000.0)
				agwave = DEFAULT_WAVELENGTH;
			logger.log(2, CLASS, "", "translateCommand", "AgWavelength: " + nf.format(agwave) + "nm");
			return "AGWAVELENGTH " + nf.format(agwave / 1000.0);
		}

		if (command instanceof ALTITUDE) {
			return "ALTITUDE " + nf.format(Math.toDegrees(((ALTITUDE) command).getAngle()));
		}

		if (command instanceof AUTOGUIDE) {
			String args = "AUTOGUIDE ";

			switch (((AUTOGUIDE) command).getState()) {
			case AUTOGUIDE.ON:
				args += "ON";
				break;
			case AUTOGUIDE.OFF:
				args += "OFF";
				break;
			case AUTOGUIDE.SUSPEND:
				args += "SUSPEND";
				break;
			case AUTOGUIDE.RESUME:
				args += "RESUME";
				break;
			}

			// return "PRESSURE 777.7";

			// return args;

			switch (((AUTOGUIDE) command).getMode()) {
			case AUTOGUIDE.RANK:
				if (((AUTOGUIDE) command).getRank() <= 1)
					return args + " " + "BRIGHTEST";
				return args + " " + "RANK " + ((AUTOGUIDE) command).getRank();
			case AUTOGUIDE.RANGE:
				return args + " " + "RANGE " + ((AUTOGUIDE) command).getRange1() + " "
						+ ((AUTOGUIDE) command).getRange2();
			case AUTOGUIDE.PIXEL:
				return args + " " + "PIXEL " + ((AUTOGUIDE) command).getXPix() + " " + ((AUTOGUIDE) command).getYPix();
			default:
				return args;
			}
		}

		if (command instanceof APERTURE) {

			int number = ((APERTURE) command).getNumber();
			return "APERTURE " + number;

		}

		if (command instanceof AZIMUTH) {
			return "AZIMUTH " + nf.format(Math.toDegrees(((AZIMUTH) command).getAngle())); // rads??
		}

		if (command instanceof BEAMSWITCH) {
			return "BEAMSWITCH " + ((BEAMSWITCH) command).getOffsetX() + " " + ((BEAMSWITCH) command).getOffsetY();
		}

		if (command instanceof CALIBRATE) {
			switch (((CALIBRATE) command).getMode()) {
			case CALIBRATE.DEFAULT:
				return "CALIBRATE DEFAULT";
			case CALIBRATE.NEW:
				return "CALIBRATE NEW";
			}
		}

		if (command instanceof DARKSLIDE) {
			switch (((DARKSLIDE) command).getState()) {
			case DARKSLIDE.OPEN:
				return "DARKSLIDE OPEN";
			case DARKSLIDE.CLOSE:
				return "DARKSLIDE CLOSE";
			}
		}

		if (command instanceof DFOCUS) {
			double dfocus = ((DFOCUS) command).getOffset();
			logger.log(2, CLASS, "", "translateCommand", "Dfocus: " + nf.format(dfocus) + "mm");
			return "DFOCUS " + nf.format(dfocus);
			// return "SHOW VERSION";
		}

		if (command instanceof ENCLOSURE) {
			int pos = ((ENCLOSURE) command).getPos();

			String mech = "";
			switch (((ENCLOSURE) command).getMechanism()) {
			case ENCLOSURE.BOTH:
				mech = "BOTH";
				break;
			case ENCLOSURE.NORTH:
				mech = "NORTH";
				break;
			case ENCLOSURE.SOUTH:
				mech = "SOUTH";
				break;
			}

			String state = "";
			switch (((ENCLOSURE) command).getState()) {
			case ENCLOSURE.OPEN:
				if (tcsMode == TCS_SIM)
					state = "80.0";
				else
					state = "OPEN";
				break;
			case ENCLOSURE.CLOSE:
				if (tcsMode == TCS_SIM)
					state = "0.0";
				else
					state = "CLOSE";
				break;
			}

			if (tcsMode == TCS_SIM)
				return "PRESSURE 777.7";
			// return "ENCLOSURE "+mech+" "+state;
			return "ENCLOSURE " + state;
		}

		if (command instanceof FOCUS) {
			return "FOCUS " + nf.format(((FOCUS) command).getFocus());
		}

		if (command instanceof HUMIDITY) {
			double humidity = ((HUMIDITY) command).getHumidity();
			if (humidity < 0.0 || humidity > 1.0)
				humidity = DEFAULT_HUMIDITY;
			logger.log(2, CLASS, "", "translateCommand", "Humidity: " + nf.format(humidity * 100.0) + "%");
			return "HUMIDITY " + nf.format(humidity);
		}

		if (command instanceof INSTRUMENT) {
			String instAlias = ((INSTRUMENT) command).getInstrumentAlias();
			logger.log(2, CLASS, "", "translateCommand", "Instrument: " + instAlias);
			return "INSTRUMENT " + instAlias;
		}

		if (command instanceof MIRROR_COVER) {

			switch (((MIRROR_COVER) command).getState()) {
			case MIRROR_COVER.OPEN:
				return "MIRROR_COVER OPEN";
			case MIRROR_COVER.CLOSE:
				return "MIRROR_COVER CLOSE";
			}
		}

		if (command instanceof MOVE_FOLD) {

			if (tcsMode == SYS_SIM)
				return "SHOW VERSION";

			switch (((MOVE_FOLD) command).getState()) {
			case MOVE_FOLD.STOWED:
				return "MOVE_FOLD STOW";
			case MOVE_FOLD.POSITION1:
				return "MOVE_FOLD 1";
			case MOVE_FOLD.POSITION2:
				return "MOVE_FOLD 2";
			case MOVE_FOLD.POSITION3:
				return "MOVE_FOLD 3";
			case MOVE_FOLD.POSITION4:
				return "MOVE_FOLD 4";
			case MOVE_FOLD.POSITION5:
				return "MOVE_FOLD 5";
			case MOVE_FOLD.POSITION6:
				return "MOVE_FOLD 6";
			case MOVE_FOLD.POSITION7:
				return "MOVE_FOLD 7";
			case MOVE_FOLD.POSITION8:
				return "MOVE_FOLD 8";

			}

			return "SHOW VERSION";

		}

		if (command instanceof OFFBY) {

			// input Ra,dec offsets are in radians.

			String args = "";

			switch (((OFFBY) command).getMode()) {

			case OFFSET.ARC:
				// Both RA and dec offsets will be in arc-secs.
				args = "OFFBY ARC " + nf.format(((OFFBY) command).getOffsetRA() * 206264.8062) + " "
						+ nf.format(((OFFBY) command).getOffsetDec() * 206264.8062);
				break;
			case OFFSET.TIME:
				// RA will be in time-secs, dec in arc-secs.
				args = "OFFBY TIME " + nf.format(((OFFBY) command).getOffsetRA() * 13750.98708) + " "
						+ nf.format(((OFFBY) command).getOffsetDec() * 206264.8062);
				break;
			}

			return args;
		}

		if (command instanceof OPERATIONAL) {
			// NOTE fudge for TTL_TCS_Simulator which ignores OPER command.
			switch (((OPERATIONAL) command).getState()) {
			case OPERATIONAL.OFF:
				// return "OPERATIONAL OFF";
				// return "PRESSURE 666";
				if (tcsMode == TCS_SIM)
					return "PRESSURE 666";
				else
					return "OPERATIONAL OFF";
			case OPERATIONAL.ON:
				// return "OPERATIONAL ON";
				// return "PRESSURE 666";
				if (tcsMode == TCS_SIM)
					return "PRESSURE 666";
				else
					return "OPERATIONAL ON";
			}
		}

		if (command instanceof PARK) {
			return "PARK ZENITH";
		}

		if (command instanceof POLE) {
			return "POLE " + ((POLE) command).getXPos() + " " + ((POLE) command).getYPos();
		}

		if (command instanceof PRESSURE) {
			double pressure = ((PRESSURE) command).getPressure();
			if (pressure < 500.0 || pressure > 1000.0)
				pressure = DEFAULT_PRESSURE;
			logger.log(2, CLASS, "", "translateCommand", "Pressure: " + nf.format(pressure) + "mbars");
			return "PRESSURE " + nf.format(pressure);
		}

		if (command instanceof ROTATOR) {

			switch (((ROTATOR) command).getMode()) {
			case ROTATOR.SKY:
				return "ROTATOR SKY " + nf.format(Math.toDegrees(((ROTATOR) command).getPosition()));
			case ROTATOR.MOUNT:
				return "ROTATOR MOUNT " + nf.format(Math.toDegrees(((ROTATOR) command).getPosition()));
			case ROTATOR.FLOAT:
				return "ROTATOR FLOAT";
			case ROTATOR.VERTICAL:
				return "ROTATOR VERTICAL";
			case ROTATOR.VFLOAT:
				return "ROTATOR VFLOAT";
			}
		}

		if (command instanceof SET_APERTURE) {

			int number = ((SET_APERTURE) command).getNumber();
			double ox = ((SET_APERTURE) command).getOffsetX();
			double oy = ((SET_APERTURE) command).getOffsetY();

			return "ENTER APERTURE " + number + " " + nf.format(ox) + " " + nf.format(oy);

		}

		if (command instanceof SHOW) {

			switch (((SHOW) command).getKey()) {
			case SHOW.ALL:
				return "SHOW ALL";
			case SHOW.ASTROMETRY:
				return "SHOW ASTROMETRY";
			case SHOW.AUTOGUIDER:
				return "SHOW AUTOGUIDER";
			case SHOW.CALIBRATE:
				return "SHOW CALIBRATE";
			case SHOW.FOCUS:
				return "SHOW FOCUS";
			case SHOW.LIMITS:
				return "SHOW LIMITS";
			case SHOW.MECHANISMS:
				return "SHOW MECHANISMS";
			case SHOW.METEOROLOGY:
				return "SHOW METEOROLOGY";
			case SHOW.SOURCE:
				return "SHOW SOURCE";
			case SHOW.STATE:
				return "SHOW STATE";
			case SHOW.TIME:
				return "SHOW TIME";
			case SHOW.VERSION:
				return "SHOW VERSION";
			}
		}

		if (command instanceof SLEW) {

			String args = "GOTO ";

			Source source = ((SLEW) command).getSource();

			double dra = ((SLEW) command).getOffsetRA();
			double ddec = ((SLEW) command).getOffsetDec();

			// Source name < 20 chars and quoted.
			String src = source.getId();
			if (src.length() > 20)
				src = src.substring(0, 20);

			args = args + " \"" + src + "\"";

			Position position = source.getPosition();

			// Note: This is adding offsets rather poorly here.
			// If end up wrapping the angles there will be
			// "Reet trouble at t'TCS "

			double ra = position.getRA() + Math.toRadians(dra / 3600.0);
			double dec = position.getDec() + Math.toRadians(ddec / 3600.0);

			args = args + " " + Position.formatHMSString(ra, " ");
			args = args + " " + Position.formatDMSString(dec, " ");

			// special handling of all SS targets.
			if (!(source instanceof ExtraSolarSource)) {

				// Bodge to get the Equinox correct for old catalog srcs which
				// do not have the
				// correct value setup.
				if ((source instanceof EphemerisSource)) {
					args = args + " J2000.0";
				} else {
					args = args + " APPARENT";
				}

				// Add the NS tracking and all the other parameters
				if (((SLEW) command).getNstrack()) {

					Tracking tracking = Astrometry.getPlanetTracking(source);
					double nstra = tracking.getNsTrackRA();
					double nsdec = tracking.getNsTrackDec();

					// epoch nsra nsdec pmra pmdec para rv

					Calendar cal0 = Calendar.getInstance();
					cal0.set(Calendar.DAY_OF_YEAR, 1);
					cal0.set(Calendar.HOUR_OF_DAY, 0);
					cal0.set(Calendar.MINUTE, 0);
					cal0.set(Calendar.SECOND, 0);
					long y0 = cal0.getTime().getTime();

					// System.err.println("Year start is: "+cal0.getTime());

					Calendar cal = Calendar.getInstance();

					double diff = cal.getTime().getTime() - y0;
					// System.err.println("Diff(ms) = "+diff);

					double yfrac = diff / (365.25 * 86400.0 * 1000.0);
					// System.err.println("YFrac = "+yfrac);

					// calc year fraction on top of year start - hopefully this
					// is what is wanted !
					double epoch = cal.get(Calendar.YEAR) + yfrac;
					// System.err.println("Epoch now: "+epoch);

					epoch = 2000.0; // TEMP - problem if we are using targets
									// for which there is no epoch...

					args = args + " " + nf.format(epoch) + " " + toSecPerSec(nstra) + " " + toArcSecPerSec(nsdec)
							+ " 0.0 0.0 0.0 0.0 ";

					System.err.println("CmdTrans::Slew to Ephemeris Target. NS track rate now: " + toSecPerSec(nstra)
							+ "s/s " + toArcSecPerSec(nsdec) + "as/s");

				}

			} else {

				args = args + " " + source.getEquinoxLetter() + source.getEquinox();

			}

			return args;
			// E.g. GOTO "EX-SRC_001" 12 22 14.5 +34 24 53.6 J2000.0 [epoch nsra
			// nsdec pmra pmdec p rv]
			// GOTO "asteroid10" 10 11 12.5 +22 33 44.5 APPARENT 2006.9876 1.234
			// -0.567 0.0 0.0 0.0 0.0

		}

		if (command instanceof STOP) {

			switch (((STOP) command).getMechanism()) {
			case STOP.ALL:
				return "STOP ALL";
			case STOP.AGFOCUS:
				return "STOP AGFOCUS";
			case STOP.AGPROBE:
				return "STOP AGPROBE";
			case STOP.ALTITUDE:
				return "STOP ALTITUDE";
			case STOP.AZIMUTH:
				return "STOP AZIMUTH";
			case STOP.ENCLOSURE:
				return "STOP ENCLOSURE";
			case STOP.FOCUS:
				return "STOP FOCUS";
			case STOP.ROTATOR:
				return "STOP ROTATOR";
			}
		}

		if (command instanceof TEMPERATURE) {
			double temperature = ((TEMPERATURE) command).getTemperature();
			if (temperature < -20.0 || temperature > 40.0)
				temperature = DEFAULT_TEMPERATURE;
			logger.log(2, CLASS, "", "translateCommand", "Temperature: " + nf.format(temperature) + "C");
			return "TEMPERATURE " + nf.format(temperature);
		}

		if (command instanceof TWEAK) {
			double twx = 3600.0 * Math.toDegrees(((TWEAK) command).getXOffset());
			double twy = 3600.0 * Math.toDegrees(((TWEAK) command).getYOffset());
			
			return "TWEAK " +  String.format("%6.4f %6.4f", twx, twy);
					
		}

		if (command instanceof TRACK) {

			String args = "TRACK ";

			switch (((TRACK) command).getMechanism()) {
			case TRACK.AGFOCUS:
				args += "AGFOCUS";
				break;
			case TRACK.AZIMUTH:
				args += "AZIMUTH";
				break;
			case TRACK.ALTITUDE:
				args += "ALTITUDE";
				break;
			case TRACK.ROTATOR:
				args += "ROTATOR";
				break;
			case TRACK.FOCUS:
				args += "FOCUS";
				break;
			}

			switch (((TRACK) command).getState()) {
			case TRACK.OFF:
				args += " OFF";
				break;
			case TRACK.ON:
				args += " ON";
				break;
			}
			return args;
		}

		if (command instanceof UNWRAP) {

			switch (((UNWRAP) command).getMechanism()) {
			case UNWRAP.AZIMUTH:
				return "UNWRAP AZIMUTH";
			case UNWRAP.ROTATOR:
				return "UNWRAP ROTATOR";
			}
		}

		if (command instanceof UT1UTC) {
			return "UT1_UTC " + ((UT1UTC) command).getValue();
		}

		if (command instanceof WAVELENGTH) {
			double wavelength = ((WAVELENGTH) command).getWavelength();
			if (wavelength < 200.0 || wavelength > 2000.0)
				wavelength = DEFAULT_WAVELENGTH;
			logger.log(2, CLASS, "", "translateCommand", "Wavelength: " + nf.format(wavelength) + "nm");
			return "WAVELENGTH " + nf.format(wavelength / 1000.0);
		}

		return "NO_COMMAND";
	}

	/**
	 * Translates a supplied response String from the RGO TCS into a
	 * ngat.message.COMMAND_DONE. If data is null or not a String, returns an
	 * empty String.
	 * 
	 * @param command
	 *            The command (class) - subclass of RCS_TO_TCS which generated
	 *            this response - needed in order to parse the response (a
	 *            String) which gives no indication of the command which
	 *            originated it.
	 * @param data
	 *            The response String formatted in accordance with that
	 *            specified in the RGO TCS User Manual.
	 * @return A COMMAND_DONE encapsulating the response data.
	 */
	public Object translateResponse(Object command, Object data) {
		boolean success = true;
		int errorCode = OK;
		String errorString = "";
		String response = "";
		COMMAND_DONE done = new COMMAND_DONE(((COMMAND) command).getId() + "-ProxyResponse");
		done.setSuccessful(true);
		done.setErrorNum(0);
		done.setErrorString("");
		// Test for unexpected response type.
		if (!(data instanceof String)) {
			// success = false;
			// errorCode = ILLEGAL_CIL_RETURN_TYPE;
			// errorString =
			// "Unknown response type: "+data.getClass().getName();
			done.setSuccessful(false);
			done.setErrorNum(ILLEGAL_CIL_RETURN_TYPE);
			done.setErrorString("Unknown CIL response type: " + data.getClass().getName());
			return done;
		} else {
			response = ((String) data).trim();
			if (response.trim().startsWith("<<090")) {
				done.setSuccessful(false);
				done.setErrorNum(589911);
				done.setErrorString(response);
				return done;
			}
		}

		if (command instanceof SHOW) {

			done = new SHOW_DONE(((COMMAND) command).getId() + "-ProxySHOWResponse");

			if (!success) {
				done.setSuccessful(false);
				done.setErrorNum(errorCode);
				done.setErrorString("SHOW_DONE: " + errorString);
				return done;
			}
			LT_RGO_ArgParser parser = new LT_RGO_ArgParser();
			Properties map = null;

			// Try to parse the response.
			try {
				parser.parse(response);
				map = parser.getMap();
			} catch (ParseException px) {
				done.setSuccessful(false);
				done.setErrorNum(589008);
				done.setErrorString("Parsex: " + response + " : " + px);
				System.err.println("Parsex: " + response + " : " + px);
				return done;
			} catch (NumberFormatException nx) {
				done.setSuccessful(false);
				done.setErrorNum(589009);
				done.setErrorString("Nmfmtx: " + response + " : " + nx);
				System.err.println("Parsex: " + response + " : " + nx);
				return done;
			}

			SHOW show = (SHOW) command;
			int key = show.getKey();

			// if (key == SHOW.METEOROLOGY) {
			// System.err.println("LTCT:: Meteo: Repsonse = "+response);
			// System.err.println("LTCT:: Meteo: Props = "+ map);
			// }

			// Build a translator for it.
			LT_RGO_SHOW_DONE_Translator translator = new LT_RGO_SHOW_DONE_Translator(key);

			// Translate, returning an appropriate TCS_Status$Segment.
			TCS_Status.Segment tcs_statusSegment = null;
			try {
				tcs_statusSegment = (TCS_Status.Segment) translator.translate(map);
				// System.err.println("GOT:: ["+tcs_statusSegment.toString()+"]");
			} catch (TranslationException tx) {
				done.setSuccessful(false);
				done.setErrorNum(LT_RGO_TRANSLATION_ERROR);
				done.setErrorString("Error translating SHOW_DONE data: " + tx);
				System.err.println("Transex: " + response + " : " + tx);
				return done;
			}

			done.setSuccessful(true);
			done.setErrorNum(OK);
			done.setErrorString("");
			((SHOW_DONE) done).setStatus(tcs_statusSegment);
			return done;
		}

		return done;
	}

	/**
	 * Translate a tracking rate into sec/sec.
	 * 
	 * @param Tracking
	 *            rate (RA/dec) in rad/sec.
	 * @return The rate in sec-time/sec - (usually RA).
	 */
	public String toSecPerSec(double rate) {
		return nf8.format(rate * 180 * 240 / Math.PI);
	}

	/**
	 * Translate a tracking rate into arcsec/sec.
	 * 
	 * @param Tracking
	 *            rate (RA/dec) in rad/sec.
	 * @return The rate in arcsec/sec (Ra or Dec).
	 */
	public String toArcSecPerSec(double rate) {
		return nf8.format(rate * 180 * 3600 / Math.PI);
	}

}
