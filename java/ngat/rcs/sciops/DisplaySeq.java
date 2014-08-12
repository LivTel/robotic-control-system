package ngat.rcs.sciops;

import ngat.astrometry.Position;
import ngat.phase2.*;

import java.util.*;

/** Displays details of a sequence component in readable form. THIS SHOULD BE IN A UTILITY PACKAGE NOT IN RCS*/
public class DisplaySeq {

	public static String display(int size, ISequenceComponent seq) {

		StringBuffer buff = new StringBuffer();

		if (seq instanceof XExecutiveComponent) {
			XExecutiveComponent exec = (XExecutiveComponent) seq;
			IExecutiveAction action = exec.getExecutiveAction();

			buff.append("\n" + tab(size * 5));

			if (action instanceof ISlew) {
				ITarget target = ((ISlew) action).getTarget();
				IRotatorConfig rot = ((ISlew) action).getRotatorConfig();
				boolean ns = ((ISlew) action).usesNonSiderealTracking();

				int mode = rot.getRotatorMode();
				double angle = rot.getRotatorAngle();
				String rotm = "";
				switch (mode) {
				case IRotatorConfig.CARDINAL:
					rotm = "CARDINAL";
					break;
				case IRotatorConfig.MOUNT:
					rotm = "MOUNT " + Position.toDegrees(angle, 3);
					break;
				case IRotatorConfig.SKY:
					rotm = "SKY " + Position.toDegrees(angle, 3);
				}

				String tm = "";
				if (target instanceof XExtraSolarTarget) {
					tm = "ExtraSolar: " + Position.toHMSString(((XExtraSolarTarget) target).getRa()) + ","
							+ Position.toDMSString(((XExtraSolarTarget) target).getDec());
				} else if (target instanceof XEphemerisTarget) {
					tm = "Ephemeris: " + ((XEphemerisTarget) target).getEphemerisTrack().size() + " nodes";
				} else if (target instanceof XSlaNamedPlanetTarget) {
					tm = "Cat:" + XSlaNamedPlanetTarget.getCatalogName(((XSlaNamedPlanetTarget) target).getIndex());
				}
				buff.append("Slew (" + target.getName() + " " + tm + " " + rotm + " "
						+ (ns ? " NonSidereal" : " Sidereal") + ")");

			} else if (action instanceof IInstrumentConfigSelector) {
				IInstrumentConfig cfg = ((IInstrumentConfigSelector) action).getInstrumentConfig();
				buff.append("Configure (" + cfg.getInstrumentName() + ", " + cfg.getName() + ")");

			} else if (action instanceof IRotatorConfig) {
				int mode = ((IRotatorConfig) action).getRotatorMode();
				double angle = ((IRotatorConfig) action).getRotatorAngle();
				String rotm = "";
				switch (mode) {
				case IRotatorConfig.CARDINAL:
					rotm = "CARDINAL";
					break;
				case IRotatorConfig.MOUNT:
					rotm = "MOUNT " + Position.toDegrees(angle, 3);
					break;
				}
				buff.append("Rotate (" + rotm + ")");

			} else if (action instanceof IFocusOffset) {
				boolean rel = ((IFocusOffset) action).isRelative();
				double mm = ((IFocusOffset) action).getOffset();
				buff.append("Defocus (" + (rel ? "INCR:" + mm : "FIXED:" + mm) + ")");
				
			} else if (action instanceof IFocusControl) {
				String focusInst = ((IFocusControl)action).getInstrumentName();
				buff.append("FocusControl ("+focusInst+")");
			} else if (action instanceof IBeamSteeringConfig) {
				XBeamSteeringConfig beam = (XBeamSteeringConfig)action;
				XOpticalSlideConfig upper = beam.getUpperSlideConfig();
				XOpticalSlideConfig lower= beam.getLowerSlideConfig();
				// need some tests in here
				buff.append("Beam ("+upper.getElementName()+", "+lower.getElementName()+")");
			} else if (action instanceof IMosaicOffset) {
				boolean rel = ((IMosaicOffset) action).isRelative();
				double dra = ((IMosaicOffset) action).getRAOffset();
				double ddc = ((IMosaicOffset) action).getDecOffset();
				buff.append("Offset (" + (rel ? "INCR:" : "FIXED:") + Math.rint((Math.toDegrees(dra) * 3600.0)) + ","
						+ Math.rint((Math.toDegrees(ddc) * 3600.0)) + ")");
			} else if (action instanceof IAcquisitionConfig) {
				IAcquisitionConfig cfg = (IAcquisitionConfig) action;
				String tinst = cfg.getTargetInstrumentName();
				String ainst = cfg.getAcquisitionInstrumentName();
				int mode = cfg.getMode();

				switch (mode) {
				case IAcquisitionConfig.WCS_FIT:
					buff.append("FineTune (WCS_FIT: " + ainst + " -> " + tinst + ")");
					break;
				case IAcquisitionConfig.BRIGHTEST:
					buff.append("FineTune (Brightest: " + ainst + " -> " + tinst + ")");
					break;
				case IAcquisitionConfig.INSTRUMENT_CHANGE:
					buff.append("ApertureOffset(" + tinst + ")");
					break;
				}
			} else if (action instanceof XMultipleExposure) {
				XMultipleExposure mult = (XMultipleExposure) action;
				double exp = mult.getExposureTime();
				int rc = mult.getRepeatCount();
				boolean std = mult.isStandard();
				buff.append((std ? "Standard" : "Science") + "Multrun (" + rc + "x" + (exp / 1000) + "s)");
			} else if (action instanceof XPeriodExposure) {
				XPeriodExposure perexp = (XPeriodExposure) action;
				double exp = perexp.getExposureTime();
				boolean std = perexp.isStandard();
				buff.append((std ? "Standard" : "Science") + "PeriodTrigex (" + (exp / 1000) + "s)");
				
			} else if (action instanceof XPeriodRunAtExposure) {
				XPeriodRunAtExposure perexp = (XPeriodRunAtExposure) action;
				double exposureTime = perexp.getExposureLength();
				boolean std = perexp.isStandard();
				double exposureDuration = perexp.getTotalExposureDuration();
				long runat = perexp.getRunAtTime();
				buff.append((std ? "Standard" : "Science") + "PeriodRunat (" + 
				(int) (exposureDuration / 1000.0)+"s/"+(int) (exposureTime / 1000.0) + "s @ "+(new Date(runat))+")");


			} else if (action instanceof IApertureConfig) {

			    IApertureConfig aper = (IApertureConfig) action;
			    buff.append("ApertureOffset(NEW)");
				
			} else if (action instanceof IOpticalSlideConfig) {

			    IOpticalSlideConfig optic = (IOpticalSlideConfig) action;
			    buff.append("SetAcquisitionInstrumrnt("+optic.getElementName()+")");
			    
			} else if (action instanceof IAutoguiderConfig) {
				IAutoguiderConfig auto = (IAutoguiderConfig) action;

				int mode = auto.getAutoguiderCommand();
				String cmd = "";
				switch (mode) {
				case IAutoguiderConfig.ON:
					cmd = "Mandatory";
					break;
				case IAutoguiderConfig.ON_IF_AVAILABLE:
					cmd = "Optional";
					break;
				case IAutoguiderConfig.OFF:
					cmd = "Off";
					break;
				}

				buff.append("Autoguider (" + cmd + ")");

			} else if (action instanceof ICalibration) {

				if (action instanceof XDark) {
					double xt = ((XDark) action).getExposureTime();
					buff.append("Dark (" + (xt / 1000) + "s)");
				} else if (action instanceof XArc) {
					ILampDef lamp = ((XArc) action).getLamp();
					String ln = (lamp != null ? lamp.getLampName() : "NO_LAMP");
					buff.append("Arc (" + ln + ")");
				} else if (action instanceof XLampFlat) {
					ILampDef lamp = ((XLampFlat) action).getLamp();
					String ln = (lamp != null ? lamp.getLampName() : "NO_LAMP");
					buff.append("LampFlat (" + ln + ")");
				} else if (action instanceof XBias) {
					buff.append("Bias ()");
				} else if (action instanceof XSkyFlat) {
					String instrumentName = ((XSkyFlat)action).getInstrumentName();
					buff.append("SkyFlat ("+instrumentName+")");
				}

			}

		} else if (seq instanceof XBranchComponent) {
			XBranchComponent bran = (XBranchComponent) seq;
			List branches = bran.listChildComponents();
			Iterator ib = branches.iterator();
			while (ib.hasNext()) {
				ISequenceComponent bc = (ISequenceComponent) ib.next();
				// System.err.print(tab(size*5)+"BRANCH: "+bc.getComponentName());
				buff.append("\n" + tab(size * 5) + "BRANCH: " + bc.getComponentName());
				buff.append("\n" + display(size + 1, bc));
			}
		} else if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cond = (XIteratorRepeatCountCondition) iter.getCondition();
			if (cond.getCount() == 1) {
				// System.err.println(tab(size*5)+iter.getComponentName()+" {");
				buff.append("\n" + tab(size * 5) + iter.getComponentName() + " {");
			} else {
				// System.err.println(tab(size*5)+iter.getComponentName()+" x "+cond.getCount()+" {");
				buff.append("\n" + tab(size * 5) + iter.getComponentName() + " x " + cond.getCount() + " {");
			}
			List comps = iter.listChildComponents();
			Iterator ib = comps.iterator();
			while (ib.hasNext()) {
				ISequenceComponent bc = (ISequenceComponent) ib.next();
				buff.append("\n" + display(size + 1, bc));
			}
			// System.err.println(tab(size*5)+"}");
			buff.append("\n" + tab(size * 5) + "}");
		}

		return buff.toString();

	}

	public static String cmd(ISequenceComponent seq) {
		StringBuffer buff = new StringBuffer();
		if (seq instanceof XExecutiveComponent) {
			XExecutiveComponent exec = (XExecutiveComponent) seq;
			IExecutiveAction action = exec.getExecutiveAction();

			if (action instanceof XTarget) {
				buff.append("\nGOTO " + exec.getComponentName());
			} else if (action instanceof XInstrumentConfig) {
				buff.append("\nCONFIG " + ((XInstrumentConfig) action).getInstrumentName() + " "
						+ exec.getComponentName());
			} else {
				buff.append("\n" + (exec != null ? exec.getComponentName() : "NULL_EXEC") + " ("
						+ (action != null ? action.getClass().getName() : "NULL_ACTION") + ")");
			}

		} else if (seq instanceof XBranchComponent) {
			XBranchComponent bran = (XBranchComponent) seq;
			List branches = bran.listChildComponents();
			Iterator ib = branches.iterator();
			while (ib.hasNext()) {
				ISequenceComponent bc = (ISequenceComponent) ib.next();
				buff.append("\n" + cmd(bc));
			}
		} else if (seq instanceof XIteratorComponent) {
			XIteratorComponent iter = (XIteratorComponent) seq;
			XIteratorRepeatCountCondition cond = (XIteratorRepeatCountCondition) iter.getCondition();
			if (cond.getCount() == 1) {
				buff.append("\n" + iter.getComponentName() + " {");
			} else {
				buff.append("\n" + iter.getComponentName() + " x " + cond.getCount() + " {");
			}
			List comps = iter.listChildComponents();
			Iterator ib = comps.iterator();
			while (ib.hasNext()) {
				ISequenceComponent bc = (ISequenceComponent) ib.next();
				buff.append("\n" + cmd(bc));
			}
			buff.append("\n}");
		}

		return buff.toString();

	}

	private static String op(IExecutiveAction action) {

		if (action instanceof ITargetSelector) {
			ITarget target = ((ITargetSelector) action).getTarget();
			return target.getName() + "/" + target.getClass().getSimpleName();
		} else if (action instanceof IInstrumentConfigSelector) {
			IInstrumentConfig config = ((IInstrumentConfigSelector) action).getInstrumentConfig();
			return config.toString();
		} else {
			return action.getClass().getSimpleName();
		}

	}

	private static String tab(int size) {
		StringBuffer st = new StringBuffer();
		for (int i = 0; i < size; i++) {
			st.append(" ");
		}
		return st.toString();
	}

}