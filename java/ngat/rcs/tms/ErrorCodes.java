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

import ngat.rcs.*;
import ngat.rcs.tms.executive.*;
import ngat.rcs.tms.manager.*;
import ngat.rcs.emm.*;
import ngat.rcs.oldscience.*;
import ngat.rcs.oldstatemodel.*;
import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;
import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.iss.*;
import ngat.rcs.tocs.*;
import ngat.rcs.calib.*;



/** Holds the code numbers (and strings) for the various Subsystem
 * error messages.
 * The subsystem codes are defined as follows:-
 *
 *
 *
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: ErrorCodes.java,v 1.1 2006/12/12 08:28:09 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/tmm/RCS/ErrorCodes.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public interface ErrorCodes {

    // ----------------
    // CCS Error Codes. 
    // ----------------




    // -------------------
    // CCS Generic Errors.   
    // -------------------

    /** No command was set.*/
    public final int CCS_GENERIC_NETWORK_NO_COMMAND_ERROR      = 100100;

    /** A command is already running.*/
    public final int CCS_GENERIC_NETWORK_ALREADY_RUNNING_ERROR = 100101;
    
    /** Some uncaught exception.*/
    public final int CCS_GENERIC_NETWORK_GENERIC_ERROR         = 100102;
 
    // ---------------------------
    // CCS Generic Command Errors.
    // ---------------------------
   
    public final int CCS_GENERIC_COMMAND_ABORTED = 200; ////?????
 
    // --------------------------
    // CCS EXPOSE Command Errors.
    // --------------------------
    
    /** DPRT failed to reduce science image.*/
    public final int CCS_EXPOSE_DPRT_FAILED = 100600;
    
    // ---------------------------
    // CCS MULTRUN Command Errors.
    // ---------------------------
    
    /** Failure to save FITS headers.*/
    public final int CCS_MULTRUN_SAVING_FITS_FAILED = 101200;
    
    /** Exposure failed for some reason.*/
    public final int CCS_MULTRUN_EXPOSURE_FAILED    = 101201;
    
    // --------------------------
    // CCS CONFIG Command Errors.
    // --------------------------
    
    /** No config was sent.*/
    public final int CCS_CONFIG_NO_CONFIG           = 100800;
    
    /** Config was of the wrong class for this ICS.*/
    public final int CCS_CONFIG_BAD_CLASS           = 100801 ;
    
    /** One or more of the filters specified was illegal.*/
    public final int CCS_CONFIG_BAD_FILTER          = 100802;
    
    /** One or more detector windowing parameters was incorrect.*/
    public final int CCS_CONFIG_BAD_WINDOW          = 100803;
    
    /** Controller failed to move the filter wheel (may be stuck). */
    public final int CCS_CONFIG_FILTER_MOVE_FAILED  = 100804;
    
    /** ISS FOCUS_OFFSET failed for some reason.*/
    public final int CCS_CONFIG_FOCUS_OFFSET_FAILED = 100805;
    
    /** The config PUID was not generated correctly.*/
    public final int CCS_CONFIG_UNIQUE_ID_FAILED    = 100806;
    
    /** Binning factors specified incorrectly - ICS dependant.*/
    public final int CCS_CONFIG_BAD_BINNING         = 100807;

    // ----------------
    // CCS FITS Errors.
    // ----------------
    
    /** Internal (FITSImpl) configuration error for fold position.*/
    public final int CCS_FITS_MOVE_FOLD_BAD_CONFIG     = 100300;
    
    /** ISS MOVE_FOLD failed for some reason.*/
    public final int CCS_FITS_MOVE_FOLD_FAILED         = 100301;;
    
    /** ISS AG_START failed for some reason.*/
    public final int CCS_FITS_AG_START_FAILED          = 100302;

    /** ISS AG_STOP failed for some reason.*/
    public final int CCS_FITS_AGSTOP_FAILED            = 100303;
    
    /** Internal (FITSImpl) configuration error saving first set of info.*/
    public final int CCS_FITS_SETTING_HEADERS_1_FAILED = 100304;

    /** ISS GET_FITS failed for some reason.*/
    public final int CCS_FITS_GETTING_FITS_ISS_FAILED  = 100305;
    
    /** Internal (FITSImpl) configuration error saving second set of info.*/
    public final int CCS_FITS_SETTING_HEADERS_2_FAILED = 100306;
    
    /** Error writing to disc - either an IO Error or CFITSIO error.*/
    public final int CCS_FITS_SAVING_FITS_FAILED       = 100307;
    
    /** May occur if an exposure is done before any config has been set
     * or a failed config is ignored !.*/
    public final int CCS_FITS_ZERO_BIN                 = 100308;

    // -----------------------------
    // CCS CALIBRATE Command Errors.
    // -----------------------------
    
    /** DPRT failed to reduce calibration image.*/
    public final int CCS_CALIBRATE_DPRT_FAILED = 100500 ;

    // ----------------------------
    // CCS TELFOCUS Command Errors.
    // ----------------------------
    
    /** CCD Native library error during exposure/readout/write-to-disc. */
    public final int CCS_TELFOCUS_EXPOSURE_FAILED          = 102000;
    
    /** ISS SET_FOCUS failed for some reason.*/
    public final int CCS_TELFOCUS_SET_FOCUS_FAILED         = 102001;
    
    /** Low level failure of DPRT.*/
    public final int CCS_TELFOCUS_DPRT_FAILED_LOW          = 102002;
    
    /** High level failure of DPRT.*/
    public final int CCS_TELFOCUS_DPRT_FAILED_HIGH         = 102003;
    
    /** Reduction produced an illegal optimum focus setting.*/
    public final int CCS_TELFOCUS_REDUCTION_BAD_FOCUS_FIT  = 102004;
    
    /**  Reduction produced an illegal seeing fit.*/
    public final int CCS_TELFOCUS_REDUCTION_BAD_SEEING_FIT = 102005;

    /** IO Error sending Frame-ACK. ## NEVER? ##*/
    public final int CCS_TELFOCUS_FRAME_ACK_FAILED         = 102006;
    
    /** IO Error sending DP ACK. ## NEVER? ##*/
    public final int CCS_TELFOCUS_DP_ACK_FAILED            = 102007;

    /** The supplied focus step was too small.*/
    public final int CCS_TELFOCUS_SMALL_STEP_SIZE          = 102008;
  
    // --------------------------
    // CCS REBOOT Command Errors.
    // --------------------------

    /** Illegal REBOOT level setting.*/
    public final int CCS_REBOOT_BAD_LEVEL        = 101400;
    
    /** A native library exception occurred during shutdown or reboot of CCS.*/
    public final int CCS_REBOOT_CCD_NATIVE_ERROR = 101401;

    /** An IO error ocurred during despatch of a REBOOT to the ICSD or DPRT process.*/
    public final int CCS_REBOOT_IO_ERROR         = 101402;
    
    /** The Command Thread was interrupted while running.*/
    public final int CCS_REBOOT_INTERRUPTED      = 101403;
    
    /** Any other general/non-specific process error.*/
    public final int CCS_REBOOT_GENERAL_ERROR    = 101404;

    // ---------------------------
    // CCS SKYFLAT Command Errors.
    // ---------------------------

    /** A native library exception occurred during exposure etc.*/
    public final int CCS_SKYFLAT_CCD_NATIVE_ERROR = 101800;

    // ---------------------------------
    // CCS DAY_CALIBRATE Command Errors.
    // ---------------------------------

    public final int CCS_DAY_CALIBRATE__ERROR = 1022;



    // --------------------------------------
    // CCS TWILIGHT_CALIBRATE Command Errors.
    // --------------------------------------
    
    /** Internal config error reading config file.*/
    public final int CCS_TWILIGHT_CALIBRATE_MISSING_CONFIG_ERROR = 102300;
    
    /** Internal config error loading calibration.*/
    public final int CCS_TWILIGHT_CALIBRATE_1_ERROR = 102301;

    /** Internal config error loading calibration.*/
    public final int CCS_TWILIGHT_CALIBRATE_2_ERROR = 102302;

    /** Internal config error loading calibration state.*/
    public final int CCS_TWILIGHT_CALIBRATE_3_ERROR = 102303;
    
    /** Internal config error loading calibration state.*/
    public final int CCS_TWILIGHT_CALIBRATE_4_ERROR = 102304;
    
    /** Internal config error loading calibration state.*/
    public final int CCS_TWILIGHT_CALIBRATE_5_ERROR = 102305;
    
    /** Internal config error loading calibration state.*/
    public final int CCS_TWILIGHT_CALIBRATE_6_ERROR = 102306;
    
    /** Failed to save state file.*/
    public final int CCS_TWILIGHT_CALIBRATE_7_ERROR = 102307;
    
    /** Internal config error loading calibration.*/
    public final int CCS_TWILIGHT_CALIBRATE_8_ERROR = 102308;

    /** Internal config error loading calibration.*/    
    public final int CCS_TWILIGHT_CALIBRATE_9_ERROR = 102309;
    
    /** Incrementing config ID failed.*/
    public final int CCS_TWILIGHT_CALIBRATE_10_ERROR = 102310;
    
    /** ISS OFFSET_FOCUS failed.*/
    public final int CCS_TWILIGHT_CALIBRATE_OFFSET_FOCUS_ERROR = 102311;
    
    /** ISS OFFSET_RA_DEC failed.*/
    public final int CCS_TWILIGHT_CALIBRATE_OFFSET_RA_DEC_ERROR = 102312;
    
    /** Exposure failed.*/
    public final int CCS_TWILIGHT_CALIBRATE_EXPOSURE_ERROR = 102313;

    /** Saving temporary FITS file.*/
    public final int CCS_TWILIGHT_CALIBRATE_SAVING_FITS_ERROR = 102314;
    
    /** Renaming temporary FITS file.*/
    public final int CCS_TWILIGHT_CALIBRATE_15_ERROR = 102315;
    
    /** Renaming temporary processed file.*/
    public final int CCS_TWILIGHT_CALIBRATE_16_ERROR = 102316;

    /** Sending basic ACK.*/
    public final int CCS_TWILIGHT_CALIBRATE_ACK_ERROR = 102317;

    /** Sending frame ACK.*/
    public final int CCS_TWILIGHT_CALIBRATE_FRAME_ACK_ERROR = 102318;

    /** Sending DP ACK*/
    public final int CCS_TWILIGHT_CALIBRATE_DP_ACK_ERROR = 102319;
    


    // --------------------
    // NETWORK Error Codes.
    // --------------------

    public final int RCS_CONNECTION_RESOURCE_ERROR = 600001;

    public final int NET_JMS_CONNECTION_FAILED     = 600002;

    public final int NET_JMS_GENERAL_COMMS_ERROR   = 600003;

    public final int NET_JMS_DESPATCH_ERROR        = 600005;

    public final int NET_JMS_RESPONSE_ERROR        = 600004;

    public final int NET_JMS_TIMEOUT_ERROR         = 600006;
  
    // ----------------
    // TCS Error Codes.
    // ----------------

    public final int TCS_BADIO       = 589909;
    
    public final int TCS_BADKEYW     = 589910;
    
    public final int TCS_BADPARAM    = 589911;
    
    public final int TCS_BADRANGE    = 589912;
    
    public final int TCS_ENGREJECT   = 589916;
    
    public final int TCS_STATEREJECT = 589924;
    
    public final int TCS_TIMEOUT     = 589926;
    
    public final int TCS_NETPARSE    = 589927;
    
    public final int TCS_NETUNK      = 589928;
    
}

/** $Log: ErrorCodes.java,v $
/** Revision 1.1  2006/12/12 08:28:09  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:32:59  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
