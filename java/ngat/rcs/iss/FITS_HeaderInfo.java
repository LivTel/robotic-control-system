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
package ngat.rcs.iss;

import ngat.phase2.IObservingConstraint;
import ngat.phase2.ITarget;
import ngat.phase2.ITimingConstraint;
import ngat.phase2.XAirmassConstraint;
import ngat.phase2.XEphemerisTarget;
import ngat.phase2.XEphemerisTimingConstraint;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XFixedTimingConstraint;
import ngat.phase2.XFlexibleTimingConstraint;
import ngat.phase2.XHourAngleConstraint;
import ngat.phase2.XSkyBrightnessConstraint;
//import ngat.phase2.XLunarDistanceConstraint;
//import ngat.phase2.XLunarElevationConstraint;
import ngat.phase2.XMinimumIntervalTimingConstraint;
import ngat.phase2.XMonitorTimingConstraint;
import ngat.phase2.XPhotometricityConstraint;
import ngat.phase2.XSeeingConstraint;
import ngat.phase2.XSlaNamedPlanetTarget;
//import ngat.phase2.XSolarElevationConstraint;
import ngat.rcs.*;
import ngat.rcs.emm.*;
import ngat.sms.GroupItem;
import ngat.message.RCS_TCS.*;
import ngat.fits.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.util.*;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Stores globally accessable information to be used in determining the content
 * of the FITS headers for the current Observation/Exposure.
 * 
 * <dl>
 * <dt><b>RCS:</b>
 * <dd>$Id: FITS_HeaderInfo.java,v 1.5 2008/04/11 10:58:33 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/iss/RCS/FITS_HeaderInfo.java,v $
 * </dl>
 * 
 * @author $Author: snf $
 * @version $Revision: 1.5 $
 */
public class FITS_HeaderInfo implements Observer {

	/** Standard ISO8601 DateTime format. */
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd 'T' HH:mm:ss z");

	public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	/** Used for formatting ints. */
	protected static NumberFormat nf2;

	public final static int TELMODE_MANUAL = 1;

	public final static int TELMODE_AUTOMATIC = 2;

	/** ISS Logging. */
	protected static Logger issLog;

	public FITS_HeaderInfo() {
		telMode = TELMODE_AUTOMATIC;
	}

	/** initialize formatters etc. */
	static {
		nf2 = NumberFormat.getInstance();
		nf2.setParseIntegerOnly(true);
		nf2.setMinimumIntegerDigits(2);
		nf2.setMaximumIntegerDigits(2);
		sdf.setTimeZone(UTC);
		issLog = LogManager.getLogger("ISS");
	}

	/** Synchronization lock. */
	protected Object lock = new Object();

	/** 0 degrees C in Kelvins. */
	public static double KELVIN = 273.15;

	// -----------------
	// IDENTITY SECTION.
	// -----------------

	/** The name of the telescope. */
	public static FitsHeaderCardImage current_TELESCOP;

	/**
	 * Operating mode of the telescope. One of: {PLANETARIUM, ROBOTIC, MANUAL,
	 * ENGINEERING}.
	 */
	public static FitsHeaderCardImage current_TELMODE;

	/** Target Allocation Group for the current observation. */
	public static FitsHeaderCardImage current_TAGID;

	/** The user who owns the current observation. */
	public static FitsHeaderCardImage current_USERID;

	/** The program to which the current observation belongs. */
	public static FitsHeaderCardImage current_PROGID;

	/** The proposal to which the current observation belongs. */
	public static FitsHeaderCardImage current_PROPID;

	/** The group to which the current observation belongs. */
	public static FitsHeaderCardImage current_GROUPID;

	/** The id of the current observation. */
	public static FitsHeaderCardImage current_OBSID;

	/**
	 * Group timing constraint type:- One of: {FLEXIBLE, FIXED, MONITOR,
	 * EPHEMERIS}.
	 */
	public static FitsHeaderCardImage current_GRPTIMNG;

	/** Group Unique Integer ID. */
	public static FitsHeaderCardImage current_GRPUID;

	/** Group (monitoring) Repeat period ()(*D). */
	public static FitsHeaderCardImage current_GRPMONP;

	/** Number of observations in a group (*I). */
	public static FitsHeaderCardImage current_GRPNUMOB;

	/** Monitoring window size. */
	public static FitsHeaderCardImage current_GRPMONWN;

	/** Lunar constraint. */
	//public static FitsHeaderCardImage current_GRPLUNCO;

	/** Seeing constraint. */
	public static FitsHeaderCardImage current_GRPSEECO;

	/** Sky brightness constraint.*/
	public static FitsHeaderCardImage current_GRPSKYCO;
	
	/** Hour angle min constraints.*/
	public static FitsHeaderCardImage current_GRPMINHA;
	
	/** Hour angle max constraint.*/
	public static FitsHeaderCardImage current_GRPMAXHA;
	
	/** Lunar distance constraint. */
	//public static FitsHeaderCardImage current_GRPMLDCO;

	/** Sun elevation constraint. */
	//public static FitsHeaderCardImage current_GRPSOLCO;

	/** Extinction category constraint. */
	public static FitsHeaderCardImage current_GRPEXTCO;

	/** Airmass constraint. */
	public static FitsHeaderCardImage current_GRPAIRCO;

	/** Expiry date. */
	public static FitsHeaderCardImage current_GRPEDATE;

	/** Nominal exec time. */
	public static FitsHeaderCardImage current_GRPNOMEX;
	
	/** Group requested defocus.*/
	public static FitsHeaderCardImage current_USRDEFOC;

	/**
	 * Image compression mode. Any of PLANETARIUM, PROFESSIONAL, AMATEUR.
	 */
	//public static FitsHeaderCardImage current_COMPRESS;

	/** GeoXXtic latitude of the observatory. (*D) */
	public static FitsHeaderCardImage current_LATITUDE;

	/** Longitude of the observatory. (*D) */
	public static FitsHeaderCardImage current_LONGITUD;

	// ---------------
	// TARGET SECTION.
	// ---------------

	/** RA of the current observation source (HH:MM:SS.sss). */
	public static FitsHeaderCardImage current_RA;

	/** Declination of the current observation source (DD:MM:SS.sss). */
	public static FitsHeaderCardImage current_DEC;

	/** Apparent RA of the current observation source. (HH:MM:SS.sss). */
	public static FitsHeaderCardImage current_APP_RA;

	/** Apparent Dec of the current observation source. (DD:MM:SS.sss). */
	public static FitsHeaderCardImage current_APP_DEC;

	/**
	 * Fundamental coordinate system of current observation source. One of FK4,
	 * FK5.
	 */
	public static FitsHeaderCardImage current_RADECSYS;

	/**
	 * Date of the coordinate system for current observation source. E.g.
	 * J2000.0 .
	 */
	public static FitsHeaderCardImage current_EQUINOX;

	/** Local Sidereal Time of the observation - approximate from TCS. HH:MM:SS) */
	public static FitsHeaderCardImage current_LST;

	/** Catalog RA of the current observation source (H:M:S). */
	public static FitsHeaderCardImage current_CAT_RA;

	/** Catalog declination of the current observation source (D:M:S)(rads). */
	public static FitsHeaderCardImage current_CAT_DEC;

	/**
	 * Catalog date of the coordinate system for current observation source.
	 * E.g. J2000.0 .
	 */
	public static FitsHeaderCardImage current_CAT_EQUI;

	/** Catalog date of the current epoch (*D) i.e. NOW. */
	public static FitsHeaderCardImage current_CAT_EPOC;

	/** Catalog name of the current observation source. */
	public static FitsHeaderCardImage current_CAT_NAME;

	/** Actual name of the current observation source. */
	public static FitsHeaderCardImage current_OBJECT;

	/**
	 * Observation source type - One of {EXTRASOLAR, MAJORPLANET, MINORPLANET,
	 * COMET, MOON}.
	 */
	public static FitsHeaderCardImage current_SRCTYPE;

	/** Proper motion in RA of the current observation source (*D)(sec/year). */
	public static FitsHeaderCardImage current_PM_RA;

	/**
	 * Proper motion in declination of the current observation source (*D)
	 * (arcsec/year).
	 */
	public static FitsHeaderCardImage current_PM_DEC;

	/** Parallax of the current observation source (arcsec). */
	public static FitsHeaderCardImage current_PARALLAX;

	/** Radial velocity of current observation source. (km/s). */
	public static FitsHeaderCardImage current_RADVEL;

	/** Non-sidereal tracking in RA of the current observation source (sec/sec). */
	public static FitsHeaderCardImage current_RATRACK;

	/**
	 * Non-sidereal tracking in declination of the current observation source
	 * (arcsec/sec)
	 */
	public static FitsHeaderCardImage current_DECTRACK;

	// ------------------
	// STATE SECTION
	// ------------------
	public static FitsHeaderCardImage current_NETSTATE;
	public static FitsHeaderCardImage current_ENGSTATE;
	public static FitsHeaderCardImage current_TELSTATE;
	public static FitsHeaderCardImage current_TCSSTATE;
	public static FitsHeaderCardImage current_PWRESTRT;
	public static FitsHeaderCardImage current_PWSHUTDN;

	// ------------------
	// MECHANISM SECTION.
	// ------------------

	public static FitsHeaderCardImage current_AZDMD;
	public static FitsHeaderCardImage current_AZPOS;
	public static FitsHeaderCardImage current_AZSTAT;

	public static FitsHeaderCardImage current_ALTDMD;
	public static FitsHeaderCardImage current_ALTPOS;
	public static FitsHeaderCardImage current_ALTSTAT;
	public static FitsHeaderCardImage current_AIRMASS;

	public static FitsHeaderCardImage current_ROTDMD;
	public static FitsHeaderCardImage current_ROTPOS;
	public static FitsHeaderCardImage current_ROTMODE;
	public static FitsHeaderCardImage current_ROTSKYPA;
	public static FitsHeaderCardImage current_ROTSTAT;

	public static FitsHeaderCardImage current_ENC1DMD;
	public static FitsHeaderCardImage current_ENC1POS;
	public static FitsHeaderCardImage current_ENC1STAT;

	public static FitsHeaderCardImage current_ENC2DMD;
	public static FitsHeaderCardImage current_ENC2POS;
	public static FitsHeaderCardImage current_ENC2STAT;

	public static FitsHeaderCardImage current_FOLDDMD;
	public static FitsHeaderCardImage current_FOLDPOS;
	public static FitsHeaderCardImage current_FOLDSTAT;

	public static FitsHeaderCardImage current_PMCDMD;
	public static FitsHeaderCardImage current_PMCPOS;
	public static FitsHeaderCardImage current_PMCSTAT;

	public static FitsHeaderCardImage current_FOCDMD;
	public static FitsHeaderCardImage current_TELFOCUS;
	public static FitsHeaderCardImage current_DFOCUS;
	public static FitsHeaderCardImage current_FOCSTAT;
	public static FitsHeaderCardImage current_MIRSYSST;

	// ------------------
	// METEO SECTION.
	// ------------------

	public static FitsHeaderCardImage current_WMSSTAT;
	public static FitsHeaderCardImage current_WMSRAIN;
	public static FitsHeaderCardImage current_WMSMOIST;

	public static FitsHeaderCardImage current_TEMPTUBE;

	public static FitsHeaderCardImage current_WMOILTMP;
	public static FitsHeaderCardImage current_WMSPMT;
	public static FitsHeaderCardImage current_WMFOCTMP;
	public static FitsHeaderCardImage current_WMAGBTMP;

	public static FitsHeaderCardImage current_WMSTEMP;
	public static FitsHeaderCardImage current_WMSDEWPT;

	public static FitsHeaderCardImage current_WINDSPEE;
	public static FitsHeaderCardImage current_WMSPRES;
	public static FitsHeaderCardImage current_WMSHUMID;
	public static FitsHeaderCardImage current_WINDDIR;

	public static FitsHeaderCardImage current_CLOUD;

	// ------------------
	// ASTROMETRY SECTION.
	// ------------------

	/**
	 * Current pressure used for refraction calculation. (*D)(mbar/hPa
	 * equivalent).
	 */
	public static FitsHeaderCardImage current_REFPRES;

	/**
	 * Current (external) temperature used for refraction calculation.
	 * (*D)(Kelvin).
	 */
	public static FitsHeaderCardImage current_REFTEMP;

	/**
	 * Current percentage humidity used for refraction calculation (*D)(0.00 -
	 * 100.00).
	 */
	public static FitsHeaderCardImage current_REFHUMID;

	// ------------------
	// AUTOGUIDE SECTION.
	// ------------------

	public static FitsHeaderCardImage current_AUTOGUID;
	public static FitsHeaderCardImage current_AGSTATE;
	public static FitsHeaderCardImage current_AGMODE;
	public static FitsHeaderCardImage current_AGGMAG;
	public static FitsHeaderCardImage current_AGFWHM;
	public static FitsHeaderCardImage current_AGMIRDMD;
	public static FitsHeaderCardImage current_AGMIRPOS;
	public static FitsHeaderCardImage current_AGMIRST;
	public static FitsHeaderCardImage current_AGFOCDMD;
	public static FitsHeaderCardImage current_AGFOCUS;
	public static FitsHeaderCardImage current_AGFOCST;
	public static FitsHeaderCardImage current_AGFILDMD;
	public static FitsHeaderCardImage current_AGFILPOS;
	public static FitsHeaderCardImage current_AGFILST;

	// ------------------
	// MOON SECTION.
	// ------------------

	/** Moon position at start of observation - One of {UP, DOWN}. */
	//public static FitsHeaderCardImage current_MOONSTAT;

	/** Moon illumination fraction at start of observation. (0.0 - 1.0) */
	public static FitsHeaderCardImage current_MOONFRAC;

	/** Moon distance from target. (*D)(degs) */
	public static FitsHeaderCardImage current_MOONDIST;

	/** Moon altitude. (*D)(degs) */
	public static FitsHeaderCardImage current_MOONALT;


     	// ------------------
    // SUN SECTION
	// ------------------

    	/** Sun altitude. (*D)(degs) */
	public static FitsHeaderCardImage current_SUNALT;



	// ------------------
	// MISC SECTION.
	// ------------------
	public static FitsHeaderCardImage current_SCHEDSEE;
	public static FitsHeaderCardImage current_SCHEDPHT;
	public static FitsHeaderCardImage current_SCHEDSKY;
	public static FitsHeaderCardImage current_ESTSEE;
    public static FitsHeaderCardImage current_BFOCCTRL;

	public static FitsHeaderCardImage current_ACQIMG;
	public static FitsHeaderCardImage current_ACQMODE;
	public static FitsHeaderCardImage current_ACQXPIX;
	public static FitsHeaderCardImage current_ACQYPIX;
	public static FitsHeaderCardImage current_ACQINST;

	/** Static instance. */
	protected static FITS_HeaderInfo instance;

	/** Create a singleton instance for Observer registration. */
	public static FITS_HeaderInfo getInstance() {
		if (instance == null)
			instance = new FITS_HeaderInfo();
		return instance;
	}

	/**
	 * Carry out configuration using the settings in the specified File.
	 * 
	 * @param configFile
	 *            The file holding the settings.
	 */
	public static void configure(File configFile) {
	}

	/** Carry out default setup. */
	public static void defaultSetup() {

		RCS_Controller controller = RCS_Controller.controller;

		// ---------------
		// IDENTITY 0-20
		// ---------------

		// The name of the telescope.
		current_TELESCOP = new FitsHeaderCardImage("TELESCOP", controller.getTelescopeDesc(),
				"The Name of the Telescope", "", 0);

		// Operating mode of the telescope.
		// Any of PLANETARIUM, ROBOTIC, MANUAL, ENGINEERING.
		current_TELMODE = new FitsHeaderCardImage("TELMODE", "ROBOTIC", "Telescope mode",
				"", 1);

		// Target Allocation Group for the current observation.
		current_TAGID = new FitsHeaderCardImage("TAGID", "UNKNOWN", "Telescope Allocation Committee ID", "", 2);

		// The user who owns the current observation.
		current_USERID = new FitsHeaderCardImage("USERID", "UNKNOWN", "User login ID", "", 3);

		// The program to which the current observation belongs.
		current_PROGID = new FitsHeaderCardImage("PROGID", "UNKNOWN", "Program ID", "", 19);

		// The proposal to which the current observation belongs.
		current_PROPID = new FitsHeaderCardImage("PROPID", "UNKNOWN", "Proposal ID", "", 4);

		// The group to which the current observation belongs.
		current_GROUPID = new FitsHeaderCardImage("GROUPID", "UNKNOWN", "Group Id", "", 5);

		// The id of the current observation/exposure.
		current_OBSID = new FitsHeaderCardImage("OBSID", "UNKNOWN", "Observation Id", "", 6);

		// Group timing constraint type.
		current_GRPTIMNG = new FitsHeaderCardImage("GRPTIMNG", "UNKNOWN", "Group timing constraint class", "", 7);

		// Group unique ID.
		current_GRPUID = new FitsHeaderCardImage("GRPUID", new Integer(0), "Group unique ID", "", 8);

		// Group monitor period if applicable.
		current_GRPMONP = new FitsHeaderCardImage("GRPMONP", new Double(0.0), "Group monitor period", "secs", 9);

		// Number obs in group.
		current_GRPNUMOB = new FitsHeaderCardImage("GRPNUMOB", new Integer(0), "Number of observations in group", "",
				10);

		// Window size.
		current_GRPMONWN = new FitsHeaderCardImage("GRPMONWN", new Double(0.0), "Window size", "secs", 11);

		// Expiry date.
		current_GRPEDATE = new FitsHeaderCardImage("GRPEDATE", "UNKNOWN", "Group expiry date", "date", 12);

		current_GRPNOMEX = new FitsHeaderCardImage("GRPNOMEX", new Double(0.0), "Group nominal exec time", "secs", 13);

		// Lunar constraint.
		//current_GRPLUNCO = new FitsHeaderCardImage("GRPLUNCO", "UNKNOWN", "Maximum lunar brightness", "", 14);

		// Seeing constraint.
		current_GRPSEECO = new FitsHeaderCardImage("GRPSEECO", "UNKNOWN", "Maximum seeing", "arcsec", 15);

		// Lunar distance constraint.
		//current_GRPMLDCO = new FitsHeaderCardImage("GRPMLDCO", "UNKNOWN", "Minimum lunar distance", "degs", 170);

		// Sun elevation constraint
		//current_GRPSOLCO = new FitsHeaderCardImage("GRPSOLCO", "UNKNOWN", "Solar elevation category", "", 171);

		// Sky brightness constraint
		current_GRPSKYCO = new FitsHeaderCardImage("GRPSKYCO", "UNKNOWN", "Sky brightness category", "", 170);
		
		// Extinction category constraint
		current_GRPEXTCO = new FitsHeaderCardImage("GRPEXTCO", "UNKNOWN", "Extinction category", "", 172);

		// Maximum Airmass constraint.
		current_GRPAIRCO = new FitsHeaderCardImage("GRPAIRCO", "UNKNOWN", "Maximum airmass", "", 173);
		
		// Hour angle constraint.
		current_GRPMINHA = new FitsHeaderCardImage("GRPMINHA", "UNKNOWN", "Minimum hour angle", "degs", 170);
		current_GRPMAXHA = new FitsHeaderCardImage("GRPMAXHA", "UNKNOWN", "Maximum hour angle", "degs", 171);
		
		// User requested defocus.*/
		current_USRDEFOC = new FitsHeaderCardImage("USRDEFOC", "UNKNOWN", "Requested defocus", "mm", 174);
		
		// Image compression mode.
		// Any of PLANETARIUM, PROFESSIONAL, AMATEUR.
		//current_COMPRESS = new FitsHeaderCardImage("COMPRESS", "PROFESSIONAL", "Compression Mode",
				//"", 16);

		// GeoXXtic latitude of the observatory.
		current_LATITUDE = new FitsHeaderCardImage("LATITUDE", new Double(Math.toDegrees(RCS_Controller.getLatitude())),
				"Observatory Latitude", "degrees", 17);

		// Longitude of the observatory.
		current_LONGITUD = new FitsHeaderCardImage("LONGITUD", new Double(Math.toDegrees(RCS_Controller.getLongitude())),
				"Observatory Longitude", "degrees West", 18);

		// ---------------
		// TARGET 21-40
		// ---------------

		// RA of the current observation source (HH:MM:SS.sss) - same as APP_RA.
		current_RA = new FitsHeaderCardImage("RA", "00:00:00.000", "Apparent RA where telescope is pointing",
				"HH:MM:SS.ss", 21);

		// Declination of the current observation source (DD:MM:SS.sss) - same
		// as APP_DEC.
		current_DEC = new FitsHeaderCardImage("DEC", "00:00:00.000", "Apparent Dec where telescope is pointing",
				"DD:MM:SS.ss", 22);

		// Apparent RA of the current observation source (HH:MM:SS.sss).
		current_APP_RA = new FitsHeaderCardImage("APP-RA", "00:00:00.000", "Apparent RA where telescope is pointing",
				"HH:MM:SS.sss", 23);

		// Apparent Dec of the current observation source (DD:MM:SS.sss).
		current_APP_DEC = new FitsHeaderCardImage("APP-DEC", "00:00:00.000",
				"Apparent Dec where telescope is pointing", "DD:MM:SS.sss", 24);

		// Fundamental coordinate system of current observation source.
		// One of FK4, FK5.
		current_RADECSYS = new FitsHeaderCardImage("RADECSYS", "FK5",
				"Fundamental coordinate system of current observation source", "{FK4, FK5}", 25);

		// LST at start of observation (HH:MM:SS).
		current_LST = new FitsHeaderCardImage("LST", "00:00:00", "Local sidereal time at start of current observation",
				"HH:MM:SS", 26);

		// Date of the coordinate system for current observation source.
		// E.g. J2000.0 .
		current_EQUINOX = new FitsHeaderCardImage("EQUINOX", new Double(2000.0),
				"Date of the coordinate system for current observation source", "Years", 27);

		// Catalog RA of the current observation source (HH:MM:SS.sss)- what was
		// sent to TCS.
		current_CAT_RA = new FitsHeaderCardImage("CAT-RA", "0:0:0", "RA of the current observation source",
				"HH:MM:SS.sss", 28);

		// Catalog declination of the current observation source (DD:MM:SS.sss)-
		// what was sent to TCS.
		current_CAT_DEC = new FitsHeaderCardImage("CAT-DEC", "0:0:0", "Declination of the current observation source",
				"DD:MM:SS.sss", 29);

		// Catalog date of the coordinate system for current observation source.
		// E.g. J2000.0 .
		current_CAT_EQUI = new FitsHeaderCardImage("CAT-EQUI", new Double(2000.0),
				"Catalog date of the coordinate system for current observation source", "Year", 30);

		// Catalog date of the epoch.
		current_CAT_EPOC = new FitsHeaderCardImage("CAT-EPOC", new Double(2000.0), "Catalog date of the epoch", "Year",
				31);

		// Catalog name of the current observation source.
		current_CAT_NAME = new FitsHeaderCardImage("CAT-NAME", "UNKNOWN",
				"Catalog name of the current observation source", "", 32);

		// Actual name of the current observation source.
		current_OBJECT = new FitsHeaderCardImage("OBJECT", "UNKNOWN", "Actual name of the current observation source",
				"", 33);
		// Source type - One of {MAJORPLANET, MINORPLANET, COMET, MOON,
		// EXTRASOLAR}.
		current_SRCTYPE = new FitsHeaderCardImage("SRCTYPE", "UNKNOWN", "Type of source",
				"", 34);
		
		// Proper motion in RA of the current observation source (sec/year).
		current_PM_RA = new FitsHeaderCardImage("PM-RA", new Double(0.0),
				"Proper motion in RA of the current observation source", "sec/year", 35);

		// Proper motion in declination of the current observation source
		// (arcsec/year).
		current_PM_DEC = new FitsHeaderCardImage("PM-DEC", new Double(0.0),
				"Proper motion in declination  of the current observation source", "arcsec/year", 36);

		// Parallax of the current observation source (arcsec).
		current_PARALLAX = new FitsHeaderCardImage("PARALLAX", new Double(0.0),
				"Parallax of the current observation source", "arcsec", 37);

		// Radial Velocity of current observation source (km/s).
		current_RADVEL = new FitsHeaderCardImage("RADVEL", new Double(0.0),
				"Radial velocity of the current observation source", "km/s", 38);

		// Non-sidereal tracking in RA of the current observation source
		// (sec/sec).
		current_RATRACK = new FitsHeaderCardImage("RATRACK", new Double(0.0),
				"Non-sidereal tracking in RA of the current observation source", "sec/sec", 39);

		// Non-sidereal tracking in declination of the current observation
		// source (arcsec/sec)
		current_DECTRACK = new FitsHeaderCardImage("DECTRACK", new Double(0.0),
				"Non-sidereal tracking in declination of the current observation source", "arcsec/sec", 40);

		// ---------------
		// STATE 41-50
		// ---------------
		// Current telescope status - see status infos.
		current_TELSTATE = new FitsHeaderCardImage("TELSTAT", "UNKNOWN", "Current telescope status", "", 41);
		current_NETSTATE = new FitsHeaderCardImage("NETSTATE", "UNKNOWN", "Network control state", "", 42);
		current_ENGSTATE = new FitsHeaderCardImage("ENGSTATE", "UNKNOWN", "Engineering override state", "", 43);

		current_TCSSTATE = new FitsHeaderCardImage("TCSSTATE", "UNKNOWN", "TCS state", "", 44);
		current_PWRESTRT = new FitsHeaderCardImage("PWRESTRT", "UNKNOWN", "Power will be cycled imminently", "", 45);
		current_PWSHUTDN = new FitsHeaderCardImage("PWSHUTDN", "UNKNOWN", "Power will be shutdown imminently", "", 46);

		// ---------------
		// MECHANISM 61-90
		// ---------------

		current_AZDMD = new FitsHeaderCardImage("AZDMD", new Double(0.0), "Azimuth demand", "degrees", 61);
		current_AZPOS = new FitsHeaderCardImage("AZIMUTH", new Double(0.0), "Azimuth axis position", "degrees", 62);
		current_AZSTAT = new FitsHeaderCardImage("AZSTAT", "UNKNOWN", "Azimuth axis state", "", 63);

		current_ALTDMD = new FitsHeaderCardImage("ALTDMD", new Double(0.0), "Altitude axis demand", "degrees", 64);
		current_ALTPOS = new FitsHeaderCardImage("ALTITUDE", new Double(0.0), "Altitude axis position", "degrees", 65);
		current_ALTSTAT = new FitsHeaderCardImage("ALTSTAT", "UNKNOWN", "Altitude axis state", "", 66);
		// Airmass (no units).
		current_AIRMASS = new FitsHeaderCardImage("AIRMASS", new Double(1.0), "Airmass", "n/a", 67);

		current_ROTDMD = new FitsHeaderCardImage("ROTDMD", new Double(0.0), "Rotator axis demand", "", 68);
		// Cassegrain rotator operating mode.
		current_ROTMODE = new FitsHeaderCardImage("ROTMODE", "UNKNOWN", "Cassegrain rotator operating mode",
				"{SKY, MOUNT, VFLOAT, VERTICAL, FLOAT}", 69);
		// Rotator axis position angle (degs).
		current_ROTSKYPA = new FitsHeaderCardImage("ROTSKYPA", new Double(0.0), "Rotator position angle", "degrees", 70);
		// Rotator mount angle (degs).
		current_ROTPOS = new FitsHeaderCardImage("ROTANGLE", new Double(0.0), "Rotator mount angle", "degrees", 71);
		current_ROTSTAT = new FitsHeaderCardImage("ROTSTATE", "UNKNOWN", "Rotator axis state", "", 72);

		current_ENC1DMD = new FitsHeaderCardImage("ENC1DMD", "UNKNOWN", "Enc 1 demand", "", 73);
		current_ENC1POS = new FitsHeaderCardImage("ENC1POS", "UNKNOWN", "Enc 1 position", "", 74);
		current_ENC1STAT = new FitsHeaderCardImage("ENC1STAT", "UNKNOWN", "Enc 1 state", "", 75);

		current_ENC2DMD = new FitsHeaderCardImage("ENC2DMD", "UNKNOWN", "Enc 2 demand", "", 76);
		current_ENC2POS = new FitsHeaderCardImage("ENC2POS", "UNKNOWN", "Enc 2 position", "", 77);
		current_ENC2STAT = new FitsHeaderCardImage("ENC2STAT", "UNKNOWN", "Enc 2 state", "", 78);

		current_FOLDDMD = new FitsHeaderCardImage("FOLDDMD", "UNKNOWN", "Fold mirror demand", "", 79);
		current_FOLDPOS = new FitsHeaderCardImage("FOLDPOS", "UNKNOWN", "Fold mirror position", "", 80);
		current_FOLDSTAT = new FitsHeaderCardImage("FOLDSTAT", "UNKNOWN", "Fold mirror state", "", 81);

		current_PMCDMD = new FitsHeaderCardImage("PMCDMD", "UNKNOWN", "Primary mirror cover demand", "", 82);
		current_PMCPOS = new FitsHeaderCardImage("PMCPOS", "UNKNOWN", "Primary mirror cover position", "", 83);
		current_PMCSTAT = new FitsHeaderCardImage("PMCSTAT", "UNKNOWN", "Primary mirror cover state", "", 84);

		current_FOCDMD = new FitsHeaderCardImage("FOCDMD", new Double(0.0), "Focus demand", "mm", 85);
		current_TELFOCUS = new FitsHeaderCardImage("TELFOCUS", new Double(0.0), "Focus position", "mm", 86);
		current_DFOCUS = new FitsHeaderCardImage("DFOCUS", new Double(0.0), "Focus offset", "mm", 87);
		current_FOCSTAT = new FitsHeaderCardImage("FOCSTAT", "UNKNOWN", "Focus state", "", 88);
		current_MIRSYSST = new FitsHeaderCardImage("MIRSYSST", "UNKNOWN", "Primary mirror support state", "", 89);

		// ---------------
		// METEO 91-100
		// ---------------

		// Percentage humidity (0.00 - 100.00).
		current_WMSHUMID = new FitsHeaderCardImage("WMSHUMID", new Double(0.0), "Current percentage humidity",
				"0.00% - 100.00%", 91);

		// (external) temperature (K).
		current_WMSTEMP = new FitsHeaderCardImage("WMSTEMP", new Double(0.0), "Current (external) temperature",
				"Kelvin", 92);

		// Pressure (mbar).
		current_WMSPRES = new FitsHeaderCardImage("WMSPRES", new Double(0.0), "Current pressure", "mbar", 93);

		// Windspeed (xx)
		current_WINDSPEE = new FitsHeaderCardImage("WINDSPEE", new Double(0.0), "Windspeed", "m/s", 94);

		// Wind direction (xx).
		current_WINDDIR = new FitsHeaderCardImage("WINDDIR", new Double(0.0), "Wind direction", "degrees E of N", 95);

		// Temperature of the telescope tube (degrees C).
		current_TEMPTUBE = new FitsHeaderCardImage("TEMPTUBE", new Double(0.0), "Temperature of the telescope tube",
				"degrees C", 96);

		current_WMSSTAT = new FitsHeaderCardImage("WMSSTATE", "UNKNOWN", "WMS system state", "", 97);
		current_WMSRAIN = new FitsHeaderCardImage("WMSRAIN", "UNKNOWN", "Rain alert", "", 98);
		current_WMSMOIST = new FitsHeaderCardImage("WMSMOIST", new Double(0.0), "Moisture level", "", 99);
		current_WMOILTMP = new FitsHeaderCardImage("WMOILTMP", new Double(0.0), "Oil temperature", "", 100);
		current_WMSPMT = new FitsHeaderCardImage("WMSPMT", new Double(0.0), "Primary mirror temperature", "", 101);
		current_WMFOCTMP = new FitsHeaderCardImage("WMFOCTMP", new Double(0.0), "Focus temperature", "", 102);
		current_WMAGBTMP = new FitsHeaderCardImage("WMAGBTMP", new Double(0.0), "AG Box temperature", "", 103);
		current_WMSDEWPT = new FitsHeaderCardImage("WMSDEWPT", new Double(0.0), "Dewpoint", "", 104);

		// Boltzwood cloud sensor.
		// Wind direction (xx).
		current_CLOUD = new FitsHeaderCardImage("CLOUD", new Double(0.0), "Cloud tdiff measure", "degrees C", 105);

		// ---------------
		// ASTROMETRY 111-120
		// ---------------

		// Pressure used in refraction calculation (mbar).
		current_REFPRES = new FitsHeaderCardImage("REFPRES", new Double(0.0),
				"Pressure used in refraction calculation", "mbar", 111);

		// (external) temperature used in refraction calculation (K).
		current_REFTEMP = new FitsHeaderCardImage("REFTEMP", new Double(0.0),
				"Temperature used in refraction calculation", "Kelvin", 112);

		// Percentage humidity used in refraction calculation (0.00 - 100.00).
		current_REFHUMID = new FitsHeaderCardImage("REFHUMID", new Double(0.0),
				"Percentage humidity used in refraction calculation", "", 113);

		// ---------------
		// AUTOGUIDE 121-130
		// ---------------

		// Autoguider status
		current_AUTOGUID = new FitsHeaderCardImage("AUTOGUID", "UNKNOWN", "Autoguider lock status",
				"{LOCKED, UNLOCKED SUSPENDED}", 121);
		current_AGSTATE = new FitsHeaderCardImage("AGSTATE", "UNKNOWN", "Autoguider sw state", "", 122);
		current_AGMODE = new FitsHeaderCardImage("AGMODE", "UNKNOWN", "Autoguider mode", "", 123);
		current_AGGMAG = new FitsHeaderCardImage("AGGMAG", new Double(0.0), "Autoguider guide star mag", "mag", 124);
		current_AGFWHM = new FitsHeaderCardImage("AGFWHM", new Double(0.0), "Autoguider FWHM", "arcsec", 125);
		current_AGMIRDMD = new FitsHeaderCardImage("AGMIRDMD", new Double(0.0), "Autoguider mirror demand", "mm", 126);
		current_AGMIRPOS = new FitsHeaderCardImage("AGMIRPOS", new Double(0.0), "Autoguider mirror position", "mm", 127);
		current_AGMIRST = new FitsHeaderCardImage("AGMIRST", "UNKNOWN", "Autoguider mirror state", "", 128);
		current_AGFOCDMD = new FitsHeaderCardImage("AGFOCDMD", new Double(0.0), "Autoguider focus demand", "mm", 129);
		// Autoguider focus position (mm).
		current_AGFOCUS = new FitsHeaderCardImage("AGFOCUS", new Double(0.0), "Autoguider focus position", "mm", 130);
		current_AGFOCST = new FitsHeaderCardImage("AGFOCST", "UNKNOWN", "Autoguider focus state", "", 131);
		current_AGFILDMD = new FitsHeaderCardImage("AGFILDMD", "UNKNOWN", "Autoguider filter demand", "", 132);
		current_AGFILPOS = new FitsHeaderCardImage("AGFILPOS", "UNKNOWN", "Autoguider filter position", "", 133);
		current_AGFILST = new FitsHeaderCardImage("AGFILST", "UNKNOWN", "Autoguider filter state", "", 134);

		// MOON 141-150

		// Moon position at start of current observation. (UP, DOWN).
		//current_MOONSTAT = new FitsHeaderCardImage("MOONSTAT", "UNKNOWN",
		//		"Moon position at start of current observation", "", 141);

		// Moon illumination fraction at start of observation. (0.0 - 1.0)*/
		current_MOONFRAC = new FitsHeaderCardImage("MOONFRAC", new Double(0.0), "Lunar illuminated fraction",
				"(0 - 1)", 142);

		// Moon illumination fraction at start of observation. (0.0 - 1.0)*/
		current_MOONDIST = new FitsHeaderCardImage("MOONDIST", new Double(0.0), "Lunar Distance from Target", "(degs)",
				143);

		// Moon altitude at start of observation. (degs).*/
		current_MOONALT = new FitsHeaderCardImage("MOONALT", new Double(0.0), "Lunar altitude", "(degs)", 144);


		// Sun altitude at start of observation. (degs).*/
		current_SUNALT = new FitsHeaderCardImage("SUNALT", new Double(0.0), "Solar altitude", "(degs)", 145);



		// MISC 151-160

		current_SCHEDSEE = new FitsHeaderCardImage("SCHEDSEE", new Double(0.0),
				"Predicted seeing when group scheduled", "(arcsec)", 161);
		current_SCHEDPHT = new FitsHeaderCardImage("SCHEDPHT", "UNKNOWN", "Predicted photom when group scheduled", "",
				162);
		current_SCHEDSKY = new FitsHeaderCardImage("SCHEDSKY", "UNKNOWN", "Predicted sky when group scheduled", "",
				141);
		
		current_ESTSEE = new FitsHeaderCardImage("ESTSEE", new Double(0.0), "Estimated seeing at start of observation",
				"(arcsec)", 163);

		current_ACQIMG = new FitsHeaderCardImage("ACQIMG", "UNKNOWN", "Acquisitioni image filename", "", 164);

		current_ACQMODE = new FitsHeaderCardImage("ACQMODE", "NONE", "Acquisition mode", "", 165);

		current_ACQXPIX = new FitsHeaderCardImage("ACQXPIX", new Double(0.0), "Acquisition X pixel", "pixels", 166);
		current_ACQYPIX = new FitsHeaderCardImage("ACQYPIX", new Double(0.0), "Acquisition Y pixel", "pixels", 167);
		current_ACQINST = new FitsHeaderCardImage("ACQINST", "NONE", "Acquisition instrument", "", 168);

		current_BFOCCTRL = new FitsHeaderCardImage("BFOCCTRL", "UNKNOWN", "Beam control instrument", "", 169);

	}

	/**
	 * Implementation of the java.util.Observer interface to handle
	 * notifications from the StatusPool on status update. The headers dependant
	 * on the TCS status are updated here.
	 */
	public void update(Observable source, Object args) {
		
		//System.err.println("FITS received update "+args.getClass().getName());
		
		if (!(args instanceof TCS_Status))
			return;

		long now = System.currentTimeMillis();

		
		synchronized (lock) {
			TCS_Status status = (TCS_Status) args;

			// ### We need to have set the target name in the calling task
			// ## Unless MANUAL ops.

			if (telMode == TELMODE_MANUAL) {
				FITS_HeaderInfo.current_TELMODE.setValue("MANUAL");
				// added re main list - arent these set by manual eng command ?
				current_TAGID.setValue("MANUAL");
				current_PROPID.setValue("MANUAL");
				current_PROGID.setValue("MANUAL");
				current_CAT_NAME.setValue(status.source.srcName);
				current_OBJECT.setValue(status.source.srcName);

				current_GRPTIMNG.setValue("MANUAL");
				current_GRPUID.setValue(new Integer(-1)); // means nothing
				current_GRPMONP.setValue(new Double(0.0)); // means nothing
				current_GRPNUMOB.setValue(new Integer(1));

			}

			
			double ra = status.source.srcRa;
			double dec = status.source.srcDec;

			// This is what we actually requested, includes any (target/mosaic) offsets,
			// but not additional offset due to tweak/offby etc
			current_CAT_RA.setValue(Position.formatHMSString(ra, ":"));
			current_CAT_DEC.setValue(Position.formatDMSString(dec, ":"));

			issLog.log(2, "FITS Headers", "-", "update", "Setting Target (CAT) RA: "
					+ Position.formatHMSString(status.source.srcRa, ":"));
			issLog.log(2, "FITS Headers", "-", "update", "Setting Target (CAT) Dec: "
					+ Position.formatDMSString(status.source.srcDec, ":"));

			// // now add any (x,y) offsets (from mosaicing) and requested by an
			// instrument.
			// ra += xoff + txoff;
			// if (ra > Math.PI*2.0)
			// ra -= Math.PI*2.0;
			// if (ra < 0.0)
			// ra += Math.PI*2.0;

			// dec += yoff + tyoff;
			// if (dec > 0.5*Math.PI)
			// dec -= 0.5*Math.PI;
			// if (dec < -0.5*Math.PI)
			// dec += 0.5*Math.PI;

			// This is where the telescope is actually pointing...
			double appRa = status.source.srcActRa;
			double appDec = status.source.srcActDec;

			// ###### TBD TBD see MKB re getting Apparent positions from TCS.
			current_APP_RA.setValue(Position.formatHMSString(appRa, ":"));
			// degs - the actual RA of the telescope.
			current_APP_DEC.setValue(Position.formatDMSString(appDec, ":"));

			issLog.log(2, "FITS Headers", "-", "update", "RA and Dec offsets are enabled");

			current_RA.setValue(Position.formatHMSString(appRa, ":"));
			// degs - the actual RA of the telescope.
			current_DEC.setValue(Position.formatDMSString(appDec, ":"));

			issLog.log(2, "FITS Headers", "-", "update", "Setting Target (Real) RA: "
					+ Position.formatHMSString(appRa, ":"));
			issLog.log(2, "FITS Headers", "-", "update", "Setting Target (Real) Dec: "
					+ Position.formatDMSString(appDec, ":"));

			// ### FORMAT THIS
			// ### units got from TCS are secs !
			current_LST.setValue(Position.formatHMSString(status.time.lst / 13750.987083533, ":"));

			issLog.log(3, "FITS Headers", "-", "update", "Setting LST: "
					+ Position.formatHMSString(status.time.lst / 13750.987083533, ":"));

			// differential tracking rates...check units !!
			current_RATRACK.setValue(new Double(status.source.srcNsTrackRA));
			current_DECTRACK.setValue(new Double(status.source.srcNsTrackDec));
		
			// AG
			current_AUTOGUID.setValue(TCS_Status.codeString(status.autoguider.agStatus));
			current_AGSTATE.setValue(TCS_Status.codeString(status.autoguider.agSwState));
			current_AGMODE.setValue(TCS_Status.codeString(status.autoguider.agMode));
			current_AGGMAG.setValue(new Double(status.autoguider.guideStarMagnitude));
			current_AGFWHM.setValue(new Double(status.autoguider.fwhm));
			current_AGMIRDMD.setValue(new Double(status.autoguider.agMirrorDemand));
			current_AGMIRPOS.setValue(new Double(status.autoguider.agMirrorPos));
			current_AGMIRST.setValue(TCS_Status.codeString(status.autoguider.agMirrorStatus));
			current_AGFOCDMD.setValue(new Double(status.autoguider.agFocusDemand));
			current_AGFOCUS.setValue(new Double(status.autoguider.agFocusPos));
			current_AGFOCST.setValue(TCS_Status.codeString(status.autoguider.agFocusStatus));
			current_AGFILDMD.setValue(TCS_Status.codeString(status.autoguider.agFilterDemand));
			current_AGFILPOS.setValue(TCS_Status.codeString(status.autoguider.agFilterPos));
			current_AGFILST.setValue(TCS_Status.codeString(status.autoguider.agFilterStatus));

			// //LIMIT
			// current_LMAZPOS .setValue(new Double(status.limits.azPosLimit));
			// current_LMAZNEG .setValue(new Double(status.limits.azNegLimit));
			// current_LMALTPOS .setValue(new
			// Double(status.limits.altPosLimit));
			// current_LMALTNEG .setValue(new
			// Double(status.limits.altNegLimit));
			// current_LMROTPOS .setValue(new
			// Double(status.limits.rotPosLimit));
			// current_LMROTNEG .setValue(new
			// Double(status.limits.rotNegLimit));
			// current_LMAZTIM .setValue(new
			// Double(status.limits.timeToAzLimit));
			// current_LMAZDIR
			// .setValue(TCS_Status.codeString(status.limits.azLimitSense));
			// current_LMALTTIM .setValue(new
			// Double(status.limits.timeToAltLimit));
			// current_LMALTDIR
			// .setValue(TCS_Status.codeString(status.limits.altLimitSense));
			// current_LMALTTIM .setValue(new
			// Double(status.limits.timeToRotLimit));
			// current_LMROTDIR
			// .setValue(TCS_Status.codeString(status.limits.rotLimitSense));

			// MECH
			current_AZDMD.setValue(new Double(status.mechanisms.azDemand));
			current_AZPOS.setValue(new Double(status.mechanisms.azPos));
			current_AZSTAT.setValue(TCS_Status.codeString(status.mechanisms.azStatus));

			current_ALTDMD.setValue(new Double(status.mechanisms.altDemand));
			current_ALTPOS.setValue(new Double(status.mechanisms.altPos));
			current_ALTSTAT.setValue(TCS_Status.codeString(status.mechanisms.altStatus));
			current_AIRMASS.setValue(new Double(status.astrometry.airmass));

			current_ROTDMD.setValue(new Double(status.mechanisms.rotDemand));
			current_ROTPOS.setValue(new Double(status.mechanisms.rotPos));
			current_ROTMODE.setValue(TCS_Status.codeString(status.mechanisms.rotMode));

			// Tweak the SKYPA based on correction applied.
			double rsky = Math.toRadians(status.mechanisms.rotSkyAngle);
			rsky += rotatorSkyCorrection;
			if (rsky > Math.PI * 2.0)
				rsky -= Math.PI * 2.0;
			if (rsky < 0.0)
				rsky += Math.PI * 2.0;
			current_ROTSKYPA.setValue(new Double(Math.toDegrees(rsky)));
		
			current_ROTSTAT.setValue(TCS_Status.codeString(status.mechanisms.rotStatus));

			current_ENC1DMD.setValue(TCS_Status.codeString(status.mechanisms.encShutter1Demand));
			current_ENC1POS.setValue(TCS_Status.codeString(status.mechanisms.encShutter1Pos));
			current_ENC1STAT.setValue(TCS_Status.codeString(status.mechanisms.encShutter1Status));

			current_ENC2DMD.setValue(TCS_Status.codeString(status.mechanisms.encShutter2Demand));
			current_ENC2POS.setValue(TCS_Status.codeString(status.mechanisms.encShutter2Pos));
			current_ENC2STAT.setValue(TCS_Status.codeString(status.mechanisms.encShutter2Status));

			current_FOLDDMD.setValue(TCS_Status.codeString(status.mechanisms.foldMirrorDemand));
			current_FOLDPOS.setValue(TCS_Status.codeString(status.mechanisms.foldMirrorPos));
			current_FOLDSTAT.setValue(TCS_Status.codeString(status.mechanisms.foldMirrorStatus));

			current_PMCDMD.setValue(TCS_Status.codeString(status.mechanisms.primMirrorCoverDemand));
			current_PMCPOS.setValue(TCS_Status.codeString(status.mechanisms.primMirrorCoverPos));
			current_PMCSTAT.setValue(TCS_Status.codeString(status.mechanisms.primMirrorCoverStatus));

			current_FOCDMD.setValue(new Double(status.mechanisms.secMirrorDemand));
			current_TELFOCUS.setValue(new Double(status.mechanisms.secMirrorPos));
			issLog.log(2, "FITS Headers", "-", "update", "Setting TELFOCUS value to: "+
					   status.mechanisms.secMirrorPos);			
			current_DFOCUS.setValue(new Double(status.mechanisms.focusOffset));
			issLog.log(2, "FITS Headers", "-", "update", "Setting DFOCUS value to: "+
				   status.mechanisms.focusOffset);			
			current_FOCSTAT.setValue(TCS_Status.codeString(status.mechanisms.secMirrorStatus));

			current_MIRSYSST.setValue(TCS_Status.codeString(status.mechanisms.primMirrorSysStatus));

			// WMS
			current_WMSSTAT.setValue(TCS_Status.codeString(status.meteorology.wmsStatus));
			current_WMSRAIN.setValue(TCS_Status.codeString(status.meteorology.rainState));
			current_WMSMOIST.setValue(new Double(status.meteorology.moistureFraction));
			current_TEMPTUBE.setValue(new Double(status.meteorology.serrurierTrussTemperature));
			current_WMOILTMP.setValue(new Double(status.meteorology.oilTemperature));
			current_WMSPMT.setValue(new Double(status.meteorology.primMirrorTemperature));
			current_WMFOCTMP.setValue(new Double(status.meteorology.secMirrorTemperature));
			current_WMAGBTMP.setValue(new Double(status.meteorology.agBoxTemperature));
			// System.err.println("FITS Headers:: On update setting WMAGBTMP="+status.meteorology.agBoxTemperature);

			current_WMSTEMP.setValue(new Double(status.meteorology.extTemperature + KELVIN));
			current_WMSDEWPT.setValue(new Double(status.meteorology.dewPointTemperature));
			current_WINDSPEE.setValue(new Double(status.meteorology.windSpeed));
			current_WMSPRES.setValue(new Double(status.meteorology.pressure));
			current_WMSHUMID.setValue(new Double(status.meteorology.humidity * 100.0));
			current_WINDDIR.setValue(new Double(status.meteorology.windDirn));

			// Grab the XCLOUD sensor data - this is somewhat of a fudge.
			try {
				LegacyStatusProviderRegistry emm = LegacyStatusProviderRegistry.getInstance();
				if (emm != null) {
					StatusCategory grabber = emm.getStatusCategory("CLOUD");
					if (grabber != null) {
						double tdiff = grabber.getStatusEntryDouble("t.diff");
						current_CLOUD.setValue(new Double(tdiff));
					}
				}

			} catch (Exception cx) {
				current_CLOUD.setValue("UNKNOWN");
			}

			// STATE
			current_NETSTATE.setValue(TCS_Status.codeString(status.state.networkControlState));
			current_ENGSTATE.setValue(TCS_Status.codeString(status.state.engineeringOverrideState));
			current_TELSTATE.setValue(TCS_Status.codeString(status.state.telescopeState));
			current_TCSSTATE.setValue(TCS_Status.codeString(status.state.tcsState));
			current_PWRESTRT.setValue(new Boolean(status.state.systemRestartFlag));
			current_PWSHUTDN.setValue(new Boolean(status.state.systemShutdownFlag));

			// TIME
			// current_TIMEMJD .setValue(status.time.mjd);
			// current_TIMEUT1 .setValue(status.time.ut1);

			current_LST.setValue(Position.formatHMSString(status.time.lst / 13750.987083533, ":"));

			// ASTRO

			current_REFPRES.setValue(new Double(status.astrometry.refractionPressure));
			current_REFTEMP.setValue(new Double(status.astrometry.refractionTemperature + KELVIN));
			current_REFHUMID.setValue(new Double(status.astrometry.refractionHumidity));

			// Moon

			// using old astrometry package
			// Assume target at already calculated ra/dec
			Position target = new Position(ra, dec);
			// Moon dist to target (degs).
			Position moon = Astrometry.getLunarPosition();
			current_MOONDIST.setValue(new Double(Math.toDegrees(target.getAngularDistance(moon))));

			Position sun = Astrometry.getSolarPosition();
			double angle = moon.getAngularDistance(sun);
			// Sun-Earth-Moon angle.
			current_MOONFRAC.setValue(new Double(0.5 * (1.0 + Math.cos(Math.PI - angle))));

			// Using new astrometry package
			/*TargetTrackCalculator moonTrack = new LunarCalculator(site);
			Coordinates moon = moonTrack.getCoordinates(time);
			double mra = moon.getRa();
			double mdec = moon.getDec();

			double angle = Math.acos(Math.cos(mdec) * Math.cos(sdec) * Math.cos(mra - sra) + Math.sin(mdec)
					* Math.sin(sdec));
			double fraction = 0.5 * (1.0 + Math.cos(Math.PI - angle));
			double phase = (angle < Math.PI ? angle / Math.PI : (2.0 * Math.PI - angle) / Math.PI);
            */
			
			// Moon altitude.
			current_MOONALT.setValue(new Double(Math.toDegrees(moon.getAltitude())));

			// Sun altitude
			current_SUNALT.setValue(new Double(Math.toDegrees(sun.getAltitude())));
			

			// Up or down.
			//if (moon.isRisen(0.0)) // isRisen(RERACT_HORIZON = 34 arcsec)
			//	current_MOONSTAT.setValue("UP");
			//else
			//	current_MOONSTAT.setValue("DOWN");
		}

	
	}

	/**
	 * Returns a String representation of the supplied angle in ddd:mm:ss.ss
	 * format.
	 * 
	 * @param angle
	 *            The angle to format (rads).
	 */
	public static String toDMSString(double angle) {
		return Position.formatDMSString(angle, ":");
	}

	/**
	 * Returns a String representation of the supplied angle in hh:mm:ss.sss
	 * format.
	 * 
	 * @param angle
	 *            The angle to format (rads).
	 */
	public static String toHMSString(double angle) {
		return Position.formatHMSString(angle, ":");
	}

	/** Correction to apply to rotator sky PA header. */
	protected static double rotatorSkyCorrection = 0.0;

	public static void setRotatorSkyCorrection(double rsc) {
		rotatorSkyCorrection = rsc;
	}

	public static double getRotatorSkyCorrection() {
		return rotatorSkyCorrection;
	}

	/** Telescope usage mode. */
	protected static int telMode;

	/**
	 * Set the telmode. When set to MANUAL, the Catalog and Object name are as
	 * obtained from TCS, otherwise RCS must set them.
	 */
	public static void setTelMode(int tm) {
		telMode = tm;
		switch (tm) {
		case TELMODE_MANUAL:
			// Reset any headers which will be needed in manual ops.
			current_TAGID.setValue("UNKNOWN");
			current_USERID.setValue("UNKNOWN");
			current_PROPID.setValue("UNKNOWN");
			current_GROUPID.setValue("UNKNOWN");
			current_OBSID.setValue("UNKNOWN");
			break;
		}

	}

	public static int getTelMode() {
		return telMode;
	}

	public static String toModeString(int tm) {
		switch (tm) {
		case TELMODE_MANUAL:
			return "MANUAL";
		case TELMODE_AUTOMATIC:
			return "AUTOMATIC";
		default:
			return "UNKNOWN";
		}
	}

	@Override
	public String toString() {

		return "FITSHeaderInfo: Details not yet available";

		// return "FITSHeaderInfo: "+
		// current_TELESCOP.toString()+", "+
		// current_TELMODE.toString()+ ", "+
		// current_TAGID.toString()+", "+
		// current_USERID.toString()+ ", "+
		// current_PROPID.toString()+", "+
		// current_GROUPID.toString()+", "+
		// current_OBSID.toString()+", "+
		// current_COMPRESS.toString()+", "+
		// current_LATITUDE.toString()+", "+
		// current_LONGITUD.toString()+", "+
		// current_RA.toString()+", "+
		// current_DEC.toString()+", "+
		// current_APP_RA.toString()+", "+
		// current_APP_DEC.toString()+", "+
		// current_RADECSYS.toString()+", "+
		// current_EQUINOX.toString()+", "+
		// current_LST.toString()+", "+
		// current_CAT_RA.toString()+", "+
		// current_CAT_DEC.toString()+", "+
		// current_CAT_EQUI.toString()+", "+
		// current_CAT_EPOC.toString()+", "+
		// current_CAT_NAME.toString()+", "+
		// current_OBJECT.toString()+", "+
		// current_SRCTYPE.toString()+", "+
		// current_PM_RA.toString()+", "+
		// current_PM_DEC.toString()+", "+
		// current_PARALLAX.toString()+", "+
		// current_RADVEL.toString()+", "+
		// current_RATRACK.toString()+", "+
		// current_DECTRACK.toString()+", "+
		// current_TELSTAT.toString()+", "+
		// current_TELFOCUS.toString()+", "+
		// current_FILTERS.toString()+", "+
		// current_AIRMASS.toString()+", "+
		// current_REFHUMID.toString()+", "+
		// current_WMSHUMID.toString()+", "+
		// current_REFTEMP.toString()+", "+
		// current_WMSTEMP.toString()+", "+
		// current_REFPRES.toString()+ ", "+
		// current_WMSPRES.toString()+", "+
		// current_WINDSPEE.toString()+", "+
		// current_WINDDIR.toString()+", "+
		// current_TEMPTUBE.toString()+", "+
		// current_AUTOGUID.toString()+", "+
		// current_AGFOCUS.toString()+", "+
		// current_AGSLIDE.toString()+", "+
		// current_AGFILTER.toString()+", "+
		// current_DFOCUS.toString()+", "+
		// current_ROTMODE.toString()+", "+
		// current_ROTSKYPA.toString()+", "+
		// current_MOONSTAT.toString()+", "+
		// current_MOONFRAC.toString();

	}

	public static void clearAcquisitionHeaders() {
		issLog.log(2, "FITS Headers", "-", "clearAcquisitionHeaders", "Clearing acquisition headers");
		current_ACQIMG.setValue(new String("NONE"));
		current_ACQMODE.setValue("NONE");
		current_ACQINST.setValue("NONE");
		current_ACQXPIX.setValue("NONE");
		current_ACQYPIX.setValue("NONE");
	}

	public static void fillIdentHeaders(GroupItem group) {
		current_TAGID.setValue((group.getTag() != null ? group.getTag().getName() : "UNKNOWN"));
		current_USERID.setValue((group.getUser() != null ? group.getUser().getName() : "UNKNOWN"));
		current_PROGID.setValue((group.getProgram() != null ? group.getProgram().getName() : "UNKNOWN"));
		current_PROPID.setValue((group.getProposal() != null ? group.getProposal().getName() : "UNKNOWN"));
		current_GROUPID.setValue(group.getName());
	}
	
	public static void fillFitsTargetHeaders(ITarget target) {

		FITS_HeaderInfo.current_CAT_NAME.setValue(target.getName());
		FITS_HeaderInfo.current_OBJECT.setValue(target.getName());

		// some defaults
		FITS_HeaderInfo.current_PM_RA.setValue(new Double(0.0));
		FITS_HeaderInfo.current_PM_DEC.setValue(new Double(0.0));
		FITS_HeaderInfo.current_PARALLAX.setValue(new Double(0.0));
		FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(2000.0));
		FITS_HeaderInfo.current_CAT_EQUI.setValue(new Double(2000.0));
		FITS_HeaderInfo.current_EQUINOX.setValue(new Double(2000.0));
		
		FITS_HeaderInfo.current_RATRACK.setValue(new Double(0.0));
		FITS_HeaderInfo.current_DECTRACK.setValue(new Double(0.0));

		if (target instanceof XExtraSolarTarget) {
			// -----------------------
			// EXTRASOLAR Source type.
			// -----------------------
			FITS_HeaderInfo.current_SRCTYPE.setValue("EXTRASOLAR");

			FITS_HeaderInfo.current_CAT_EPOC.setValue(new Double(((XExtraSolarTarget) target).getEpoch()));
			FITS_HeaderInfo.current_PM_RA.setValue(new Double(((XExtraSolarTarget) target).getPmRA()));
			FITS_HeaderInfo.current_PM_DEC.setValue(new Double(((XExtraSolarTarget) target).getPmDec()));
			FITS_HeaderInfo.current_PARALLAX.setValue(new Double(((XExtraSolarTarget) target).getParallax()));
			// FITS_HeaderInfo.current_RADVEL =

			switch (((XExtraSolarTarget) target).getFrame()) {
			case ReferenceFrame.FK4:
				FITS_HeaderInfo.current_RADECSYS.setValue("FK4");
				FITS_HeaderInfo.current_EQUINOX.setValue(new Double(1950.0));
				FITS_HeaderInfo.current_CAT_EQUI.setValue(new Double(1950.0));
				break;
			case ReferenceFrame.FK5:
			case ReferenceFrame.ICRF:
				FITS_HeaderInfo.current_RADECSYS.setValue("FK5");
				FITS_HeaderInfo.current_EQUINOX.setValue(new Double(2000.0));
				FITS_HeaderInfo.current_CAT_EQUI.setValue(new Double(2000.0));
				break;
			default:
				FITS_HeaderInfo.current_RADECSYS.setValue("UNKNOWN(" + ((XExtraSolarTarget) target).getFrame() + ")");
			}

		} else if (target instanceof XSlaNamedPlanetTarget) {
			FITS_HeaderInfo.current_SRCTYPE.setValue("CATALOG");
			int catid = ((XSlaNamedPlanetTarget) target).getIndex();

		} else if (target instanceof XEphemerisTarget) {
			FITS_HeaderInfo.current_SRCTYPE.setValue("EPHEMERIS");

		}
		/*try {
		long now = System.currentTimeMillis();
		ISite site = RCS_Controller.controller.getSite();
		AstrometrySiteCalculator astro = RCS_Controller.controller.getSiteCalculator();
		TargetTrackCalculator track = new BasicTargetCalculator(target, site);
		Coordinates c = track.getCoordinates(now);
		double parangle = astro.getParalacticAngle(c, now);
		FITS_HeaderInfo.current_PARANGLE.setValue(new Double(parangle));
		FITS_HeaderInfo.current_PARANGLE.setComment("Parallactic angle");
		FITS_HeaderInfo.current_PARALLAX.setUnits("degs");
		} catch (Exception e) {
			FITS_HeaderInfo.current_PARANGLE.setValue(-99);
			FITS_HeaderInfo.current_PARALLAX.setComment("Unable to calculate parallactic angle");
			System.err.println("Unable to calculate Parangle for target: "+e);
		}*/
	}
	public static void fillFitsTimingHeaders(GroupItem group) {

		if (group == null)
			return;

		ITimingConstraint timing = group.getTimingConstraint();
		if (timing == null)
			return;

		if (timing instanceof XFlexibleTimingConstraint) {
			XFlexibleTimingConstraint xlex = (XFlexibleTimingConstraint) timing;
			current_GRPTIMNG.setValue("FLEXIBLE");
			current_GRPMONP.setValue(new Double(0.0));
			current_GRPMONWN.setValue(new Double(0.0));
		} else if (timing instanceof XFixedTimingConstraint) {
			XFixedTimingConstraint xfix = (XFixedTimingConstraint) timing;

			current_GRPTIMNG.setValue("FIXED");
			current_GRPMONP.setValue(new Double(0.0));
			current_GRPMONWN.setValue(new Double(0.0));
		} else if (timing instanceof XMonitorTimingConstraint) {
			XMonitorTimingConstraint xmon = (XMonitorTimingConstraint) timing;
			current_GRPTIMNG.setValue("MONITOR");
			long period = xmon.getPeriod();
			long window = xmon.getWindow();
			current_GRPMONP.setValue(new Double(period / 1000));
			current_GRPMONWN.setValue(new Double(window / 1000));
		} else if (timing instanceof XMinimumIntervalTimingConstraint) {
			XMinimumIntervalTimingConstraint xmin = (XMinimumIntervalTimingConstraint) timing;
			long period = xmin.getMinimumInterval();
			current_GRPTIMNG.setValue("REPEATABLE");
			current_GRPMONP.setValue(new Double(period / 1000));
			current_GRPMONWN.setValue(new Double(0.0));
		} else if (timing instanceof XEphemerisTimingConstraint) {
			XEphemerisTimingConstraint xephem = (XEphemerisTimingConstraint) timing;
			current_GRPTIMNG.setValue("EPHEMERIS");
			current_GRPMONP.setValue(new Double(0.0));
			current_GRPMONWN.setValue(new Double(0.0));
		}
		// some generic info
		current_GRPEDATE.setValue(sdf.format(new Date(timing.getEndTime())));

	}

	public static void fillFitsConstraintHeaders(GroupItem group) {

		if (group == null)
			return;

		// first fill them out unknown
		current_GRPSEECO.setValue("NONE");
		current_GRPSKYCO.setValue("NONE");
		current_GRPEXTCO.setValue("NONE");
		current_GRPAIRCO.setValue("NONE");
		current_GRPMINHA.setValue("NONE");
		current_GRPMINHA.setValue("NONE");
		
		List ocs = group.listObservingConstraints();
		if (ocs == null)
			return;

		Iterator ioc = ocs.iterator();
		while (ioc.hasNext()) {
			IObservingConstraint oc = (IObservingConstraint) ioc.next();

			if (oc instanceof XSeeingConstraint) {
				XSeeingConstraint xsee = (XSeeingConstraint) oc;			
				current_GRPSEECO.setValue(new Double(xsee.getSeeingValue()));
				
			//} else if (oc instanceof XLunarElevationConstraint) {
			//	XLunarElevationConstraint xlun = (XLunarElevationConstraint) oc;			
			//	current_GRPLUNCO.setValue(xlun.getLunarElevationCategoryName(xlun.getLunarElevationCategory()));
			//} else if (oc instanceof XLunarDistanceConstraint) {
			//	XLunarDistanceConstraint xldc = (XLunarDistanceConstraint) oc;
			//	current_GRPMLDCO.setValue(new Double(Math.toDegrees(xldc.getMinimumLunarDistance())));
			//} else if (oc instanceof XSolarElevationConstraint) {
			//	XSolarElevationConstraint xsol = (XSolarElevationConstraint) oc;
			//	current_GRPSOLCO.setValue(XSolarElevationConstraint.getSolarElevationCategoryName(xsol
			//			.getMaximumSolarElevationCategory()));
				
			} else if (oc instanceof XSkyBrightnessConstraint) {	
				XSkyBrightnessConstraint xsky = (XSkyBrightnessConstraint)oc;
				int skyb = xsky.getSkyBrightnessCategory();
				current_GRPSKYCO.setValue(SkyBrightnessCalculator.getSkyBrightnessCategoryName(skyb));				
			} else if (oc instanceof XPhotometricityConstraint) {
				XPhotometricityConstraint xphot = (XPhotometricityConstraint) oc;
				current_GRPEXTCO.setValue(XPhotometricityConstraint.getPhotometricityCategoryName(xphot
						.getPhotometricityCategory()));
			} else if (oc instanceof XAirmassConstraint) {
				XAirmassConstraint xair = (XAirmassConstraint) oc;
				current_GRPAIRCO.setValue(new Double(xair.getMaximumAirmass()));
			} else if (oc instanceof XHourAngleConstraint) {
					XHourAngleConstraint xhaco = (XHourAngleConstraint)oc;
					current_GRPMINHA.setValue(new Double(xhaco.getMinimumHourAngle()));
					current_GRPMAXHA.setValue(new Double(xhaco.getMaximumHourAngle()));
			}
				
			// GRPHACO

		}

	}
}

/**
 * $Log: FITS_HeaderInfo.java,v $ /** Revision 1.5 2008/04/11 10:58:33 snf /**
 * changed TAG and PROP to manual in eng mode.. /** /** Revision 1.4 2008/04/10
 * 07:05:32 snf /** added sky pa corrrection /** /** Revision 1.3 2008/01/07
 * 10:44:32 snf /** added ACQIMG /** /** Revision 1.2 2007/07/05 11:32:44 snf
 * /** checkin agbox tmp /** /** Revision 1.1 2006/12/12 08:30:20 snf /**
 * Initial revision /** /** Revision 1.1 2006/05/17 06:34:28 snf /** Initial
 * revision /** /** Revision 1.4 2002/09/16 09:38:28 snf /** *** empty log
 * message *** /** /** Revision 1.3 2001/06/08 16:39:01 snf /** fixed bug in ra
 * and dec. /** /** Revision 1.2 2001/06/08 16:27:27 snf /** Modified to use
 * FitsHeaderCardImages. /** /** Revision 1.1 2001/04/27 17:14:32 snf /**
 * Initial revision /**
 */
