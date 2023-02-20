/**
 * 
 */
package ngat.rcs.sciops;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import dev.lt.RATCamConfig;
import ngat.icm.FilterDescriptor;
import ngat.icm.FilterSet;
import ngat.icm.Imager;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.icm.MoptopPolarimeter;
import ngat.phase2.CCDConfig;
import ngat.phase2.CCDDetector;
import ngat.phase2.FrodoSpecConfig;
import ngat.phase2.FrodoSpecDetector;
import ngat.phase2.IDetectorConfig;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.IRCamConfig;
import ngat.phase2.IRCamDetector;
import ngat.phase2.RaptorConfig;
import ngat.phase2.RaptorDetector;
import ngat.phase2.InstrumentConfig;
import ngat.phase2.LOTUSConfig;
import ngat.phase2.LOTUSDetector;
import ngat.phase2.OConfig;
import ngat.phase2.ODetector;
import ngat.phase2.RISEConfig;
import ngat.phase2.RISEDetector;
import ngat.phase2.RaptorConfig;
import ngat.phase2.Ringo2PolarimeterConfig;
import ngat.phase2.Ringo2PolarimeterDetector;
import ngat.phase2.Ringo3PolarimeterConfig;
import ngat.phase2.Ringo3PolarimeterDetector;
import ngat.phase2.MOPTOPPolarimeterConfig;
import ngat.phase2.MOPTOPPolarimeterDetector;
import ngat.phase2.SpratConfig;
import ngat.phase2.SpratDetector;
import ngat.phase2.THORConfig;
import ngat.phase2.THORDetector;
import ngat.phase2.Window;
import ngat.phase2.XDualBeamSpectrographInstrumentConfig;
import ngat.phase2.XFilterDef;
import ngat.phase2.XFilterSpec;
import ngat.phase2.XImagerInstrumentConfig;
import ngat.phase2.XImagingSpectrographInstrumentConfig;
import ngat.phase2.XRaptorInstrumentConfig;
import ngat.phase2.XPolarimeterInstrumentConfig;
import ngat.phase2.XMoptopInstrumentConfig;
import ngat.phase2.XTipTiltImagerInstrumentConfig;
import ngat.phase2.XBlueTwoSlitSpectrographInstrumentConfig;
import ngat.phase2.XWindow;

/**
 * @author eng
 * 
 */
public class ConfigTranslator {

	public static InstrumentRegistry ireg;

	/**
	 * Makes a best effort attempt to create an old-style config from the
	 * supplied new-style config. CCD configs may be junk as the filter mapping
	 * is not perfect.
	 * 
	 * @param config
	 *            A new style config.
	 * @return An old style config
	 */
	public static InstrumentConfig translateToOldStyleConfig(IInstrumentConfig config) throws Exception {

		if (config == null)
			throw new ConfigTranslationException("Config was null");

		InstrumentDescriptor iid = new InstrumentDescriptor(config.getInstrumentName());

		InstrumentCapabilitiesProvider icp = ireg.getCapabilitiesProvider(iid);
		InstrumentCapabilities icap = icp.getCapabilities();
		if (!icap.isValidConfiguration(config))
			throw new ConfigTranslationException("Supplied instrument config is not valid: " + config);

		if (config instanceof XTipTiltImagerInstrumentConfig) {
			XTipTiltImagerInstrumentConfig xtip = (XTipTiltImagerInstrumentConfig) config;
			IDetectorConfig xdetector = xtip.getDetectorConfig();
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();

			XFilterSpec filterSpec = xtip.getFilterSpec();

			if (xtip.getInstrumentName().equalsIgnoreCase("IO:THOR")) {
				THORConfig thorc = new THORConfig(config.getName());

				THORDetector thordetector = (THORDetector) thorc.getDetector(0);
				thordetector.clearAllWindows();
				thordetector.setXBin(xBin);
				thordetector.setYBin(yBin);

				thorc.setEmGain(xtip.getGain());

				// snf - 6-10-10 transfer any windows across
				List windows = xdetector.listWindows();
				Iterator iwin = windows.iterator();
				int iiw = 0;
				while (iwin.hasNext()) {
					XWindow xwindow = (XWindow) iwin.next();
					int xs = xwindow.getX();
					int ys = xwindow.getY();
					int xe = xs + xwindow.getWidth() - 1;
					int ye = ys + xwindow.getHeight() - 1;
					Window window = new Window(xs, ys, xe, ye);
					window.setActive(true);
					thordetector.setWindow(iiw++, window);
				}

				return thorc;

			}
			throw new ConfigTranslationException("Unable to identify tip-tilt imager from supplied config: " + config);

		} else if (config instanceof XImagerInstrumentConfig) {
			XImagerInstrumentConfig ximager = (XImagerInstrumentConfig) config;
			IDetectorConfig xdetector = ximager.getDetectorConfig();
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();
			XFilterSpec filterSpec = ximager.getFilterSpec();

			if (ximager.getInstrumentName().equalsIgnoreCase("RISE")) {
				// This looks like RISE
				RISEConfig rise = new RISEConfig(config.getName());

				RISEDetector rdetector = (RISEDetector) rise.getDetector(0);
				rdetector.clearAllWindows();
				rdetector.setXBin(xBin);
				rdetector.setYBin(yBin);

				// Instrument inst = Instruments.findInstrument("RISE");

				// what if no fspec ?
				if (filterSpec == null)
					filterSpec = new XFilterSpec();

				List filterList = filterSpec.getFilterList();

				// what if no filters in fspec ?
				if (filterList == null)
					filterList = new Vector();

				return rise;			
				
			} else if (ximager.getInstrumentName().equalsIgnoreCase("IO:I")) {

				IRCamConfig irc = new IRCamConfig(config.getName());
				
				IRCamDetector irdetector = (IRCamDetector)irc.getDetector(0);
				irdetector.clearAllWindows();
				irdetector.setXBin(xBin);
				irdetector.setYBin(yBin);
				
				// what if no fspec ?
				if (filterSpec == null)
					filterSpec = new XFilterSpec();
				
				List filterList = filterSpec.getFilterList();
			
				// what if no filters in fspec ?
				if (filterList == null)
					filterList = new Vector();
				
				// initially we dont care as there is no configurable filter available
				
				String filter0 = ((XFilterDef) filterList.get(0)).getFilterName();
				if (tryIConfig(irc, filter0)) //mutates irc
					return irc;
				
				return irc;
				
			} else if (ximager.getInstrumentName().equalsIgnoreCase("RAPTOR")) {
				XRaptorInstrumentConfig xraptor = (XRaptorInstrumentConfig)config;
				
				int nudgematicOffsetSize = xraptor.getNudgematicOffsetSize();
				int coaddExposureLength = xraptor.getCoaddExposureLength();

				RaptorConfig raptorConfig = new RaptorConfig(config.getName());
				raptorConfig.setNudgematicOffsetSize(nudgematicOffsetSize);
				raptorConfig.setCoaddExposureLength(coaddExposureLength);
				
				RaptorDetector raptorDetector = (RaptorDetector)raptorConfig.getDetector(0);
				raptorDetector.clearAllWindows();
				raptorDetector.setXBin(xBin);
				raptorDetector.setYBin(yBin);
				
				// what if no fspec ?
				if (filterSpec == null)
					filterSpec = new XFilterSpec();
				
				List filterList = filterSpec.getFilterList();
				// what if no filters in fspec ?
				if (filterList == null)
					filterList = new Vector();
				
				String filter0 = ((XFilterDef) filterList.get(0)).getFilterName();
				if (tryRaptorConfig(raptorConfig, filter0)) //mutates irc
					return raptorConfig;
				
				return raptorConfig;
				
			} else if (ximager.getInstrumentName().equalsIgnoreCase("IO:O")) {

				// This looks like Ooooooo
				OConfig oConfig = new OConfig(config.getName());

				ODetector odetector = (ODetector) oConfig.getDetector(0);
				odetector.clearAllWindows();
				odetector.setXBin(xBin);
				odetector.setYBin(yBin);

				// what if no fspec ?
				if (filterSpec == null)
					filterSpec = new XFilterSpec();
				
				List filterList = filterSpec.getFilterList();
			
				// what if no filters in fspec ?
				if (filterList == null)
					filterList = new Vector();

				// TODO will always be 3 filters
				
				String filter1 = ((XFilterDef) filterList.get(0)).getFilterName();
				String filter2 = ((XFilterDef) filterList.get(1)).getFilterName();
				String filter3 = ((XFilterDef) filterList.get(2)).getFilterName();
				
				if (tryOConfig(oConfig, filter1, filter2, filter3))
					return oConfig;
				
				// TODO Later we might try for: (x = clear)
				// 0 x,x,x
				// 1 f,x,x	 			
				// 2 f,a,x  f,x,a
				// 3 f,a,b (as now)
				
				throw new ConfigTranslationException("Unable to convert or verify supplied IO:O config: " + config);

			} else if (ximager.getInstrumentName().equalsIgnoreCase("RATCAM")) {

				RATCamConfig ratcamConfig = new RATCamConfig(config.getName());
				CCDDetector cdetector = (CCDDetector) ratcamConfig.getDetector(0);
				cdetector.clearAllWindows();
				cdetector.setXBin(xBin);
				cdetector.setYBin(yBin);

				// Instrument inst = Instruments.findInstrument("RATCAM");

				// what if no fspec ?
				if (filterSpec == null)
					filterSpec = new XFilterSpec();

				List filterList = filterSpec.getFilterList();

				// what if no filters in fspec ?
				if (filterList == null)
					filterList = new Vector();

				// 0 filters try clear/clear
				// 1 filter try clear/f and f/clear
				// 2 filters try f1/f2 and f2/f1
				switch (filterList.size()) {
				case 0:
					if (tryRatcamConfig(ratcamConfig, "clear", "clear"))
						return ratcamConfig;
					break;
				case 1:
					String filter = ((XFilterDef) filterList.get(0)).getFilterName();
					if (tryRatcamConfig(ratcamConfig, filter, "clear"))
						return ratcamConfig;
					if (tryRatcamConfig(ratcamConfig, "clear", filter))
						return ratcamConfig;
					break;
				case 2:
					String filter1 = ((XFilterDef) filterList.get(0)).getFilterName();
					String filter2 = ((XFilterDef) filterList.get(1)).getFilterName();
					if (tryRatcamConfig(ratcamConfig, filter1, filter2))
						return ratcamConfig;
					if (tryRatcamConfig(ratcamConfig, filter2, filter1))
						return ratcamConfig;
					break;
				}
				throw new ConfigTranslationException("Unable to convert or verify supplied RATCAM config: " + config);
			}

			throw new ConfigTranslationException("Unable to identify imager from supplied config: " + config);

		} else if (config instanceof XDualBeamSpectrographInstrumentConfig) {

			XDualBeamSpectrographInstrumentConfig xdual = (XDualBeamSpectrographInstrumentConfig) config;
			int resolution = xdual.getResolution();

			IDetectorConfig xdetector = xdual.getDetectorConfig();
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();

			// Create an old-p2 FrodoConfig

			FrodoSpecConfig frodo = new FrodoSpecConfig(config.getName());
			// match arm...
			int arm;
			String instId = config.getInstrumentName();
			if (instId.equalsIgnoreCase("FRODO_RED"))
				arm = FrodoSpecConfig.RED_ARM;
			else if (instId.equalsIgnoreCase("FRODO_BLUE"))
				arm = FrodoSpecConfig.BLUE_ARM;
			else
				arm = FrodoSpecConfig.NO_ARM;

			frodo.setArm(arm);

			// match resolution...
			int oldres = 0;
			switch (resolution) {
			case XDualBeamSpectrographInstrumentConfig.LOW_RESOLUTION:
				oldres = FrodoSpecConfig.RESOLUTION_LOW;
				break;
			case XDualBeamSpectrographInstrumentConfig.HIGH_RESOLUTION:
				oldres = FrodoSpecConfig.RESOLUTION_HIGH;
				break;
			default:
				oldres = FrodoSpecConfig.RESOLUTION_UNKNOWN;
				break;
			}

			frodo.setResolution(oldres);

			FrodoSpecDetector fdetector = (FrodoSpecDetector) frodo.getDetector(0);
			fdetector.clearAllWindows();
			fdetector.setXBin(xBin);
			fdetector.setYBin(yBin);

			return frodo;

		} else if (config instanceof XPolarimeterInstrumentConfig) {
			XPolarimeterInstrumentConfig xpolar = (XPolarimeterInstrumentConfig) config;
			IDetectorConfig xdetector = xpolar.getDetectorConfig();
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();

			int gain = xpolar.getGain();

			if (xpolar.getInstrumentName().equalsIgnoreCase("RINGO2")) {

				Ringo2PolarimeterConfig polar2 = new Ringo2PolarimeterConfig(config.getName());
				polar2.setEmGain(gain);
				polar2.setTriggerType(Ringo2PolarimeterConfig.TRIGGER_TYPE_EXTERNAL);

				Ringo2PolarimeterDetector pdetector = (Ringo2PolarimeterDetector) polar2.getDetector(0);
				pdetector.clearAllWindows();
				pdetector.setXBin(xBin);
				pdetector.setYBin(yBin);
				return polar2;
			} else if (xpolar.getInstrumentName().equalsIgnoreCase("RINGO3")) {

				Ringo3PolarimeterConfig polar3 = new Ringo3PolarimeterConfig(config.getName());
				polar3.setEmGain(gain);
				polar3.setTriggerType(Ringo3PolarimeterConfig.TRIGGER_TYPE_EXTERNAL);

				Ringo3PolarimeterDetector pdetector0 = (Ringo3PolarimeterDetector) polar3.getDetector(0);
				pdetector0.clearAllWindows();
				pdetector0.setXBin(xBin);
				pdetector0.setYBin(yBin);

				Ringo3PolarimeterDetector pdetector1 = (Ringo3PolarimeterDetector) polar3.getDetector(1);
				pdetector1.clearAllWindows();
				pdetector1.setXBin(xBin);
				pdetector1.setYBin(yBin);

				Ringo3PolarimeterDetector pdetector2 = (Ringo3PolarimeterDetector) polar3.getDetector(2);
				pdetector2.clearAllWindows();
				pdetector2.setXBin(xBin);
				pdetector2.setYBin(yBin);

				// maybe transfer any windows across
				/*
				 * List windows = detector1-2-3.listWindows(); Iterator iwin =
				 * windows.iterator(); int iiw = 0; while (iwin.hasNext()) {
				 * XWindow xwindow = (XWindow)iwin.next(); int xs =
				 * xwindow.getX(); int ys = xwindow.getY(); int xe = xs +
				 * xwindow.getWidth()-1; int ye = ys + xwindow.getHeight()-1;
				 * Window window = new Window(xs,ys,xe,ye);
				 * window.setActive(true); thordetector.setWindow(iiw++,
				 * window); }
				 */

				return polar3;

			}
			throw new ConfigTranslationException("Unable to identify polarimeter from supplied config: " + config);

		} else if (config instanceof XMoptopInstrumentConfig) {
			XMoptopInstrumentConfig xmoptop = (XMoptopInstrumentConfig)config;
			IDetectorConfig xdetector = xmoptop.getDetectorConfig();
			
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();
			
			int rotorSpeed = xmoptop.getRotorSpeed();
			
			XFilterSpec filterSpec = xmoptop.getFilterSpec();
			if(filterSpec == null)
				throw new ConfigTranslationException("Unable to find filter-spec for MOPTOP config: " + config);
			
			List filterList = filterSpec.getFilterList();

			if (filterList == null)
				throw new ConfigTranslationException("Unable to find filter list for MOPTOP config: " + config);

			if(filterList.size() != 1)
			{
				throw new ConfigTranslationException("MOPTOP config: " + config+ " has wrong number of filters:"+
						filterList.size());
			}
			
			String filter = ((XFilterDef) filterList.get(0)).getFilterName();
			
			MOPTOPPolarimeterConfig moptopConfig = new MOPTOPPolarimeterConfig(config.getName());
			// The filter is set in tryMoptopConfig
			//moptopConfig.setFilterName(filter);
			moptopConfig.setRotorSpeed(rotorSpeed);
			
			MOPTOPPolarimeterDetector moptopDetector0 = (MOPTOPPolarimeterDetector) moptopConfig.getDetector(0);
			MOPTOPPolarimeterDetector moptopDetector1 = (MOPTOPPolarimeterDetector) moptopConfig.getDetector(1);
			moptopDetector0.clearAllWindows();
			moptopDetector0.setXBin(xBin);
			moptopDetector0.setYBin(yBin);
			moptopDetector1.clearAllWindows();
			moptopDetector1.setXBin(xBin);
			moptopDetector1.setYBin(yBin);
			
			if (tryMoptopConfig(moptopConfig, filter))
				return moptopConfig;

			throw new ConfigTranslationException("Unable to convert or verify supplied MOPTOP config: " + config);
			
		} else if (config instanceof XImagingSpectrographInstrumentConfig) {
			
			XImagingSpectrographInstrumentConfig xspec = (XImagingSpectrographInstrumentConfig) config;
			
			IDetectorConfig xdetector = xspec.getDetectorConfig();
			int xBin = xdetector.getXBin();
			int yBin = xdetector.getYBin();

			if (xspec.getInstrumentName().equalsIgnoreCase("SPRAT")) {
				SpratConfig spratc = new SpratConfig(config.getName());

				SpratDetector spratDetector = (SpratDetector) spratc.getDetector(0);
				spratDetector.clearAllWindows();
				spratDetector.setXBin(xBin);
				spratDetector.setYBin(yBin);

				switch (xspec.getGrismPosition()) {
					case XImagingSpectrographInstrumentConfig.GRISM_IN:
						spratc.setGrismPosition(SpratConfig.POSITION_IN);
						break;
					case XImagingSpectrographInstrumentConfig.GRISM_OUT:
						spratc.setGrismPosition(SpratConfig.POSITION_OUT);
						break;
					default:
						break;
					
				}
				
				switch (xspec.getGrismRotation()) {
					case XImagingSpectrographInstrumentConfig.GRISM_ROTATED:
						spratc.setGrismRotation(1);
						break;
					case XImagingSpectrographInstrumentConfig.GRISM_NOT_ROTATED:
						spratc.setGrismRotation(0);
						break;
					default:
						break;
					
				}
				
				switch (xspec.getSlitPosition()) {
					case XImagingSpectrographInstrumentConfig.SLIT_DEPLOYED:
						spratc.setSlitPosition(SpratConfig.POSITION_IN);
						break;
					case XImagingSpectrographInstrumentConfig.SLIT_STOWED:
						spratc.setSlitPosition(SpratConfig.POSITION_OUT);
						break;
					default:
						break;
					
				}
				
				// nrc - 28-8-14 transfer any windows across
				/*List windows = xdetector.listWindows();
				Iterator iwin = windows.iterator();
				int iiw = 0;
				while (iwin.hasNext()) {
					XWindow xwindow = (XWindow) iwin.next();
					int xs = xwindow.getX();
					int ys = xwindow.getY();
					int xe = xs + xwindow.getWidth() - 1;
					int ye = ys + xwindow.getHeight() - 1;
					Window window = new Window(xs, ys, xe, ye);
					window.setActive(true);
					spratDetector.setWindow(iiw++, window);
				}*/
				return spratc;
			} 
			throw new ConfigTranslationException("Unable to identify imaging spectrograph from supplied config:" + config);
		}
		else if (config instanceof XBlueTwoSlitSpectrographInstrumentConfig) 
		{
			XBlueTwoSlitSpectrographInstrumentConfig btsspec = (XBlueTwoSlitSpectrographInstrumentConfig) config;
		
			IDetectorConfig btsdetector = btsspec.getDetectorConfig();
			int xBin = btsdetector.getXBin();
			int yBin = btsdetector.getYBin();

			if (btsspec.getInstrumentName().equalsIgnoreCase("LOTUS")) 
			{
				LOTUSConfig lotusc = new LOTUSConfig(config.getName());

				LOTUSDetector lotusDetector = (LOTUSDetector) lotusc.getDetector(0);
				lotusDetector.clearAllWindows();
				lotusDetector.setXBin(xBin);
				lotusDetector.setYBin(yBin);

				switch (btsspec.getSlitWidth())
				{
					case XBlueTwoSlitSpectrographInstrumentConfig.SLIT_NARROW:
						lotusc.setSlitWidth(LOTUSConfig.SLIT_WIDTH_NARROW);
						break;
					case XBlueTwoSlitSpectrographInstrumentConfig.SLIT_WIDE:
						lotusc.setSlitWidth(LOTUSConfig.SLIT_WIDTH_WIDE);
						break;
					default:
						break;
				}
				return lotusc;
			} 
			throw new ConfigTranslationException("Unable to identify blue two slit spectrograph from supplied config:" + config);
		}
		throw new ConfigTranslationException("Unable to determine required instrument from supplied config: " + config);
	}

	public static String determineInstrumentFromConfig(InstrumentConfig config) throws ConfigTranslationException {

		if (config instanceof CCDConfig)
			return "RATCAM";
		else if (config instanceof OConfig)
			return "O";
		else if (config instanceof IRCamConfig)
			return "SUPIRCAM";
		else if (config instanceof Ringo2PolarimeterConfig)
			return "RINGO2";
		else if (config instanceof Ringo3PolarimeterConfig)
			return "RINGO3";
		else if (config instanceof MOPTOPPolarimeterConfig)
			return "MOPTOP";
		else if (config instanceof RISEConfig)
			return "RISE";
		else if (config instanceof THORConfig)
			return "THOR";
		else if (config instanceof SpratConfig)
			return "SPRAT";
		else if (config instanceof LOTUSConfig)
			return "LOTUS";
		else if (config instanceof FrodoSpecConfig) {
			if (((FrodoSpecConfig) config).getArm() == FrodoSpecConfig.RED_ARM)
				return "FRODO_RED";
			else if (((FrodoSpecConfig) config).getArm() == FrodoSpecConfig.BLUE_ARM)
				return "FRODO_BLUE";
			else
				return "FRODO";
		} else
			throw new ConfigTranslationException("Unable to determine instrument name from supplied config: " + config);

	}

	/**
	 * Test a supplied configuration with the supplied filters.
	 * 
	 * @param inst
	 *            The instrument to try the config on.
	 * @param config
	 *            The (CCD) config to try.
	 * @param lfilter
	 *            The lower filter to try (in the supplied config).
	 * @param ufilter
	 *            The upper filter to try (in the supplied config).
	 * @return True if the config is valid.
	 */
	private static boolean tryRatcamConfig(CCDConfig config, String lfilter, String ufilter) {
		System.err.println("Try ratcam config: " + lfilter + ", " + ufilter);
		try {
			FilterDescriptor flow = new FilterDescriptor(lfilter, "ccdfilter");
			FilterDescriptor fupp = new FilterDescriptor(ufilter, "ccdfilter");
			InstrumentDescriptor rid = new InstrumentDescriptor("RATCAM");
			Imager ccd = (Imager) (ireg.getCapabilitiesProvider(rid).getCapabilities());
			// System.err.println("FS contains: "+ccd.get
			// FilterSet f0 = ccd.getFilterSet(1);
			FilterSet fsLower = ccd.getFilterSet("lower");
			System.err.println("FLower=" + fsLower);
			// FilterSet f1 = ccd.getFilterSet(2);
			FilterSet fsUpper = ccd.getFilterSet("upper");
			System.err.println("FUpper=" + fsUpper);
			if (fsLower.containsFilter(flow) && fsUpper.containsFilter(fupp)) {
				config.setLowerFilterWheel(lfilter);
				config.setUpperFilterWheel(ufilter);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	/**
	 * Test a supplied configuration with the supplied filters.
	 * 
	 * @param inst
	 *            The instrument to try the config on.
	 * @param config
	 *            The config to try.
	 * @param filter1
	 *            The main filter to try (in the supplied config).
	 * @param filter2
	 *            The second filter to try (in the supplied config).
	 * @param filter3
	 *            The third filter to try (in the supplied config).
	 *            
	 * @return True if the config is valid.
	 */
	private static boolean tryOConfig(OConfig config, String filter1, String filter2, String filter3) {
		System.err.println("Try o config: " + filter1 + ", " + filter2+" "+filter3);
		try {
			FilterDescriptor fwheel    = new FilterDescriptor(filter1, "ccdfilter");
			FilterDescriptor flowslide = new FilterDescriptor(filter2, "ccdfilter");
			FilterDescriptor fuppslide = new FilterDescriptor(filter3, "ccdfilter");
			InstrumentDescriptor rid = new InstrumentDescriptor("IO:O");
			Imager ccd = (Imager) (ireg.getCapabilitiesProvider(rid).getCapabilities());
		
			FilterSet fsWheel = ccd.getFilterSet("wheel");
			System.err.println("FWheel=" + fwheel);
			FilterSet fsLower = ccd.getFilterSet("lower_slide");
			System.err.println("FLower=" + flowslide);
			FilterSet fsUpper = ccd.getFilterSet("upper_slide");
			System.err.println("FUpper=" + fuppslide);
			if (fsWheel.containsFilter(fwheel) && fsLower.containsFilter(flowslide) && fsUpper.containsFilter(fuppslide)) {
				config.setFilterName(OConfig.O_FILTER_INDEX_FILTER_WHEEL, filter1);
				config.setFilterName(OConfig.O_FILTER_INDEX_FILTER_SLIDE_LOWER, filter2);
				config.setFilterName(OConfig.O_FILTER_INDEX_FILTER_SLIDE_UPPER, filter3);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	
	private static boolean tryIConfig(IRCamConfig config , String filter0) {
		System.err.println("Try i config: " + filter0 );
		try {
			FilterDescriptor fwheel = new FilterDescriptor(filter0, "irfilter");
			
			InstrumentDescriptor rid = ireg.getDescriptor("IO:I");
			
			Imager irArray = (Imager) (ireg.getCapabilitiesProvider(rid).getCapabilities());
		
			FilterSet fsWheel = irArray.getFilterSet("wheel");
			System.err.println("FWheel=" + fwheel);
			
			if (fsWheel.containsFilter(fwheel)) {
				config.setFilterWheel(filter0);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return false;
		
	}
	
	/**
	 * This method checks that the specified filter exists in the Moptop filter set as supplied
	 * by ireg.getCapabilitiesProvider(rid).getCapabilities(). If it does, it inserts the specified
	 * filter into the specified config.
	 * @param config The moptop polarimeter configuration.
	 * @param filter The filter to insert into the config, if it exists in MOPTOP's filter set.
	 * @return true if the filter exists in MOPTOP's filter set, false if it does not.
	 */
	private static boolean tryMoptopConfig(MOPTOPPolarimeterConfig config , String filter)
	{
		System.err.println("Try Moptop config: " + filter );
		try
		{
			FilterDescriptor filterDescriptor = new FilterDescriptor(filter, "moptopfilter");
			
			InstrumentDescriptor rid = ireg.getDescriptor("MOPTOP");
			// TODO MOPTOP is not an imager, change this to something else
			MoptopPolarimeter moptop = (MoptopPolarimeter) (ireg.getCapabilitiesProvider(rid).getCapabilities());
		
			FilterSet filterSet = moptop.getFilterSet("wheel");
			System.err.println("filter descriptor =" + filterDescriptor);
			System.err.println("filter wheel filter set =" + filterSet);
			
			if (filterSet.containsFilter(filterDescriptor)) 
			{
				config.setFilterName(filter);
				return true;
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
}
