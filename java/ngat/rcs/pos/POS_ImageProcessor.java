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
package ngat.rcs.pos;

import ngat.rcs.*;

import ngat.rcs.tmm.*;
import ngat.rcs.tmm.executive.*;
import ngat.rcs.tmm.manager.*;

import ngat.rcs.emm.*;

import ngat.rcs.scm.*;
import ngat.rcs.scm.collation.*;
import ngat.rcs.scm.detection.*;

import ngat.rcs.comms.*;
import ngat.rcs.control.*;
import ngat.rcs.statemodel.*;

import ngat.rcs.iss.*;

import ngat.rcs.tocs.*;
import ngat.rcs.science.*;
import ngat.rcs.calib.*;


import java.io.*;
import java.util.*;
import java.text.*;

import ngat.util.*;
import ngat.util.logging.*;

/** This class is used to invoke the Planetarium Mode image reduction software.<br>
 *  ## Currently it just returns some dummy file names - no file is generated ##<br>
 *  ## the execution time is also instant in reality it may have to spawn a   ##<br>
 *  ## new Process to run the C-image processing software. OR call JNI        ##<br>
 *  ## wrappers of native C methods.                                          ##<br>
 *
 * This will need to call the processing libraries using the following formats:<br><br>
 *
 * e.g. int color_jpeg(char* file_list[], int frame1, int frame2, char* err_msg, char* dest_file) 
 *
 * via jni calls to:-<br><br>
 * 
 * JNI_image_color_jpeg(JNIEnv* env, jobject obj, , etc).
 *
 * <dl>	
 * <dt><b>RCS:</b>
 * <dd>$Id: POS_ImageProcessor.java,v 1.1 2006/12/12 08:27:07 snf Exp $
 * <dt><b>Source:</b>
 * <dd>$Source: /home/dev/src/rcs/java/ngat/rcs/pos/RCS/POS_ImageProcessor.java,v $
 * </dl>
 * @author $Author: snf $
 * @version $Revision: 1.1 $
 */
public class POS_ImageProcessor {

    protected static PersistentUniqueInteger puid;
    
    /** Directory where POS images are stored (as symlinks or real processed images).*/
    protected static File imageBaseDir;

    /** Directory where POS image-processing scripts are kept.*/
    protected static File scriptDir;

    /** Short date format for desiganting lockfile name.*/
    static final SimpleDateFormat adf = new SimpleDateFormat("MMdd");
    
    static Logger pcaLog = LogManager.getLogger("PCA");
   
    static String jpegScript;
    static String fitsScript;
    static String bestFitsScript;
    static String mosaicJpegScript;
    static String mosaicFitsScript;
    static String colorJpegScript;

    public static void initialize() {
	
        puid = new PersistentUniqueInteger("%%proc");
	
    }

    public static void configure(File file) throws IOException, IllegalArgumentException {
	ConfigurationProperties config = new ConfigurationProperties();
	config.load(new FileInputStream(file));
	
	scriptDir =  config.getFile("planetarium.script.dir", "planetarium/scripts");	

	jpegScript       = config.getProperty("jpeg.script",         "make_jpeg.csh");
	fitsScript       = config.getProperty("fits.script",         "make_fits.csh");
	bestFitsScript   = config.getProperty("best_fits.script",    "make_best_fits.csh");
	mosaicJpegScript = config.getProperty("mosaic_jpeg.script",  "make_mosaic_jpeg.csh");
	mosaicFitsScript = config.getProperty("mosaic_fits.script",  "make_mosaic_fits.csh");
	colorJpegScript  = config.getProperty("color_jpeg.script",   "make_color_jpeg.csh");

    }

    public static void setImageBaseDir(File base) {
	imageBaseDir = base;
    }
    
    /** For now we just hope for the best that the images are start, start+1, start+2.
     *
     * NOTE: We currently return the basic filename (relative to BASEDIR) NOT the full path.
     *
     *
     **/
    public static File processCOLORJPEG(long startFrame, long endFrame, int type) throws ImageProcessingException {
	String filename = "process-"+advanceProcessCounter()+".jpg";
	File   output   = new File(imageBaseDir, filename);

	// Something like..   $RCS_HOME/planetarium/images/process-555.jpg
	
	//
	// We really need to get the symlink to pass to the Processing software.
	//
	String redfile   = null;
	String greenfile = null;
	String bluefile  = null;
	File   script    = null;
	try {
	  redfile   = FileUtilities.readSymbolicLink((
				new File(imageBaseDir, "%%frame-"+startFrame)).getPath());

	  greenfile = FileUtilities.readSymbolicLink((
				new File(imageBaseDir, "%%frame-"+(startFrame+1))).getPath());
	  
	  bluefile  = FileUtilities.readSymbolicLink((
				new File(imageBaseDir, "%%frame-"+(startFrame+2))).getPath()); 

	  script = new File(scriptDir, colorJpegScript);
	  
	  pcaLog.log(1,"Preparing ImageProcessor Script with:"+
			     "\n\tProcessing Script: "+script.getPath()+
			     "\n\tRed   file: "+redfile+
			     "\n\tGreen file: "+greenfile+
			     "\n\tBlue  file: "+bluefile);
	} catch (Exception e) {
	    throw new ImageProcessingException("Reading symlinks failed: "+e);
	}

	// $MAKE_COLOR_JPEG <red> <green> <blue> <output> <type>
	final String [] args = new String[] {
	    script.getPath(),
	    redfile,
	    greenfile,
	    bluefile,
	    output.getPath(),
	    ""+type};
	Process process =null;
	long start = 0L;
	try {
	    pcaLog.log(1,"Firing off ImageProcessing Script:"+
		       args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]+" "+args[5]);
	    start = System.currentTimeMillis();
	    process = Runtime.getRuntime().exec(args);
	} catch (IOException iox) {
	    throw new ImageProcessingException("Make_Color_Jpeg operation failed: "+iox);
	}

	try { 
	    pcaLog.log(1,"Waiting for IP Process to complete or fail.");
	    process.waitFor();
	} catch (InterruptedException ix) {
	    pcaLog.log(1,"Interrupted: "+ix);
	}

	int status = process.exitValue();
	if ( status != 0) 
	    throw new ImageProcessingException("Make_Color_Jpeg script exited with status: "+status);

	long time = System.currentTimeMillis()-start;
	pcaLog.log(1,"Image processing completed successfully in "+(time/1000)+" secs.");

	return new File(filename);
    }

    public static File processBESTJPEG(long startFrame, long endFrame)throws ImageProcessingException {
	throw new ImageProcessingException("BEST_JPEG is not yet available");
    }

    public static File processJPEG(long startFrame, long endFrame, int type) 
	throws ImageProcessingException {
	
	String filename = "process-"+advanceProcessCounter()+".jpg";
	File   output   = new File(imageBaseDir, filename);
	
	// Something like..   $RCS_HOME/planetarium/images/process-555.jpg
	
	//
	// We really need to get the symlink to pass to the Processing software.
	//
	String infile   = null;
	File   script    = null;
	try {
	    infile   = FileUtilities.readSymbolicLink((
						       new File(imageBaseDir, "%%frame-"+startFrame)).getPath());
	    
	    script = new File(scriptDir, jpegScript);
	  
	    pcaLog.log(1,"Preparing ImageProcessor Script with:"+
		       "\n\tProcessing Script: "+script.getPath()+
		       "\n\tFile: "+infile);
	    
	} catch (Exception e) {
	    throw new ImageProcessingException("Reading symlinks failed: "+e);
	}
	
	// $MAKE_JPEG <infile> <output> <type>
	final String [] args = new String[] {
	    script.getPath(),
	    infile,	   
	    output.getPath(),
	    ""+type};
	Process process =null;
	long start = 0L;
	try {
	    pcaLog.log(1,"Firing off ImageProcessing Script:"+
		       args[0]+" "+args[1]+" "+args[2]+" "+args[3]);
	    start = System.currentTimeMillis();
	    process = Runtime.getRuntime().exec(args);
	} catch (IOException iox) {
	    throw new ImageProcessingException("Make_Jpeg operation failed: "+iox);
	}

	try { 
	    pcaLog.log(1,"Waiting for IP Process to complete or fail.");
	    process.waitFor();
	} catch (InterruptedException ix) {
	    pcaLog.log(1,"Interrupted: "+ix);
	}

	int status = process.exitValue();
	if ( status != 0) 
	    throw new ImageProcessingException("Make_Jpeg script exited with status: "+status);

	long time = System.currentTimeMillis()-start;
	pcaLog.log(1,"Image processing completed successfully in "+(time/1000)+" secs.");
	
	return new File(filename);
    }
    
    public static File processBESTFITS(long startFrame, long endFrame) throws ImageProcessingException {
	throw new ImageProcessingException("BEST_FITS is not yet available");
    }

    public static File processFITS(long startFrame, long endFrame) throws ImageProcessingException {
	String filename = "process-"+advanceProcessCounter()+".fits.H";
	File   output   = new File(imageBaseDir, filename);

	//
	// We really need to get the symlink to pass to the Processing software.
	//
	String fitsfile  = null;
	
	File   script    = null;
	try {
	  fitsfile   = FileUtilities.readSymbolicLink((
				new File(imageBaseDir, "%%frame-"+startFrame)).getPath());

	  script = new File(scriptDir, fitsScript);
	  
	  pcaLog.log(1,"Preparing ImageProcessor Script with:"+
			     "\n\tProcessing Script: "+script.getPath()+
			     "\n\tFITS   file: "+fitsfile);
	} catch (Exception e) {
	    throw new ImageProcessingException("Reading symlinks failed: "+e);
	}

	// $MAKE_FITS <fits> <output>
	final String [] args = new String[] {
	    script.getPath(),
	    fitsfile,
	    output.getPath()};
	Process process =null;
	long start = 0L;
	try {
	    pcaLog.log(1,"Firing off ImageProcessing Script:"+args[0]);
	    start = System.currentTimeMillis();
	    process = Runtime.getRuntime().exec(args);
	} catch (IOException iox) {
	    throw new ImageProcessingException("Make_Fits operation failed: "+iox);
	}

	try { 
	    pcaLog.log(1,"Waiting for IP Process to complete or fail.");
	    process.waitFor();
	} catch (InterruptedException ix) {
	    pcaLog.log(1,"Interrupted: "+ix);
	}

	int status = process.exitValue();
	if ( status != 0) 
	    throw new ImageProcessingException("Make_Fits script exited with status: "+status);

	long time = System.currentTimeMillis()-start;
	pcaLog.log(1,"Image processing completed successfully in "+(time/1000)+" secs.");

	return new File(filename);
    }

    public static File processMOSAICFITS(long startFrame, long endFrame) throws ImageProcessingException {
	
	String filename = "process-"+advanceProcessCounter()+".fits";
	File   output   = new File(imageBaseDir, filename);

	//
	// We really need to get the symlink to pass to the Processing software.
	//
	if (endFrame < startFrame)
	    throw new ImageProcessingException("StarFrame "+startFrame+" is > EndFrame "+endFrame);

	int nf = (int)(endFrame - startFrame + 1);
	String nextfile = null;
	String [] files = new String[nf];
	File   script   = null;
	int    ii       = 0;

	try {
	    for (int fn = (int)startFrame; fn < (int)endFrame; fn++) {
		
		nextfile = FileUtilities.readSymbolicLink((
		    new File(imageBaseDir, "%%frame-"+(startFrame+fn))).getPath());
		files[ii] = nextfile;
		ii++;
	    }
	    
	    script = new File(scriptDir, mosaicFitsScript);
	    
	    pcaLog.log(1,"Preparing ImageProcessor Script with:"+
		       "\n\tProcessing Script: "+script.getPath()+
		       "\n\t     Image Frames: "+startFrame+" -> "+endFrame);
	} catch (Exception e) {
	    throw new ImageProcessingException("Reading symlinks failed: "+e);
	}
	
	// $MAKE_COLOR_JPEG <red> <green> <blue> <output> <type>
	String args = new String(script.getPath());
	for (int jj = 0; jj < nf; jj++) {
	    args = args+files[jj];
	}
	
	args = args + output.getPath();

	Process process =null;
	long start = 0L;
	try {
	    pcaLog.log(1,"Firing off ImageProcessing Script:"+args);
	    start = System.currentTimeMillis();
	    process = Runtime.getRuntime().exec(args);
	} catch (IOException iox) {
	    throw new ImageProcessingException("Make_Mosaic_Fits operation failed: "+iox);
	}

	try { 
	    pcaLog.log(1,"Waiting indefinitely for IP Process to complete or fail .");
	    process.waitFor();
	} catch (InterruptedException ix) {
	    pcaLog.log(1,"Interrupted: "+ix);
	}

	int status = process.exitValue();
	if ( status != 0) 
	    throw new ImageProcessingException("Make_Mosaic_Fits script exited with status: "+status);

	long time = System.currentTimeMillis()-start;
	pcaLog.log(1,"Image processing completed successfully in "+(time/1000)+" secs.");

	return new File(filename);
	
	//throw new ImageProcessingException("MOSAIC_FITS is not yet available");
    }
    
    public static File processMOSAICJPEG(long startFrame, long endFrame) throws ImageProcessingException {

	String filename = "process-"+advanceProcessCounter()+".jpg";
	File   output   = new File(imageBaseDir, filename);

	//
	// We really need to get the symlink to pass to the Processing software.
	//
	if (endFrame < startFrame)
	    throw new ImageProcessingException("StartFrame "+startFrame+" > EndFrame "+endFrame);

	int nf = (int)(endFrame - startFrame + 1);
	String nextfile = null;
	String [] files = new String[nf];
	File   script   = null;
	int    ii       = 0;
	int    fid      = 0;

	try {
	    for (int fn = (int)startFrame; fn < (int)endFrame; fn++) {
		fid = fn;
		nextfile = FileUtilities.readSymbolicLink((
			     new File(imageBaseDir, "%%frame-"+fn)).getPath());
		files[ii] = nextfile;
		ii++;
	    }
	    
	    script = new File(scriptDir, mosaicJpegScript);
	    
	    pcaLog.log(1,"Preparing ImageProcessor Script with:"+
		       "\n\tProcessing Script: "+script.getPath()+
		       "\n\t     Image Frames: "+startFrame+" -> "+endFrame);
	} catch (Exception e) {
	    throw new ImageProcessingException("Reading symlinks failed for frame: "+fid+" : "+e);
	}
	
	// 
	String args = new String(script.getPath());
	for (int jj = 0; jj < nf; jj++) {
	    args = args+files[jj];
	}
	
	args = args + output.getPath();

	Process process =null;
	long start = 0L;
	try {
	    pcaLog.log(1,"Firing off ImageProcessing Script:"+args);
	    start = System.currentTimeMillis();
	    process = Runtime.getRuntime().exec(args);
	} catch (IOException iox) {
	    throw new ImageProcessingException("Make_Mosaic_Jpeg operation failed: "+iox);
	}

	try { 
	    pcaLog.log(1,"Waiting indefinitely for IP Process to complete or fail .");
	    process.waitFor();
	} catch (InterruptedException ix) {
	    pcaLog.log(1,"Interrupted: "+ix);
	}

	int status = process.exitValue();
	if ( status != 0) 
	    throw new ImageProcessingException("Make_Mosaic_Jpeg script exited with status: "+status);

	long time = System.currentTimeMillis()-start;
	pcaLog.log(1,"Image processing completed successfully in "+(time/1000)+" secs.");

	return new File(filename);

	//throw new ImageProcessingException("MOSAIC_JPEG is not yet available");
    }


    protected static int advanceProcessCounter() {
	try {
	    return puid.increment();
	} catch (Exception e) {
	    return 0;
	}
    }

    public static class ImageProcessingException extends Exception {

	public ImageProcessingException() { super(); }

	public ImageProcessingException(String message) { super(message); }

    }

}

/** $Log: POS_ImageProcessor.java,v $
/** Revision 1.1  2006/12/12 08:27:07  snf
/** Initial revision
/**
/** Revision 1.1  2006/05/17 06:31:58  snf
/** Initial revision
/**
/** Revision 1.1  2002/09/16 09:38:28  snf
/** Initial revision
/** */
