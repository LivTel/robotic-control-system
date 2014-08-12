/**
 * 
 */
package ngat.rcs.sciops;

import java.util.Iterator;
import java.util.List;

import ngat.icm.DetectorArrayPosition;
import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.IAutoguiderConfig;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.XAutoguiderConfig;
import ngat.phase2.XFocusOffset;
import ngat.phase2.IInstrumentConfig;
import ngat.phase2.IInstrumentConfigSelector;
import ngat.phase2.XPositionOffset;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.ITarget;
import ngat.phase2.ITargetSelector;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XIteratorComponent;

/**
 * Collates changes as it walks round the executing sequence tree.
 * 
 * @author eng
 * 
 */
public class ChangeTracker {

	private String ident;

	/** The most recently selected instrument config. */
	private IInstrumentConfig lastConfig;

	/** The most recently selected target. */
	private ITarget lastTarget;

	/** This should really be an instrument object reference. */
	private String instrumentName;

	/** True if the autoguider is (or should be) locked. */
	private IAutoguiderConfig autoguide;

	/** True if the axes are (or should be) tracking. */
	private boolean tracking;

	/** The cumulative position offset and hence absolute. */
	private XPositionOffset offset;

	/** The cumulative focus offset and hence absolute. */
	private XFocusOffset focusOffset;

	/**
	 * The latest rotator setting. Note: this will need to record the actual SKY
	 * angle if CARDINAL.
	 */
	private IRotatorConfig rotator;

	/**
	 * Indicates for which instrument the current aperture is set. Can be null.
	 */
	private String apertureInstrument;

	/** True if the scope has acquired and not lost the acquisition yet. */
	private boolean acquired;

	/** Name of the latest acquisition image. */
	private String acquireImage;

	/** The latest acquisition config used. */
	private IAcquisitionConfig acqConfig;

	/** The latest acqusisition offsets. */
	private DetectorArrayPosition acquireOffset;

	/** The last instrument used for acquisisition. */
	private String acqInstrument;

	/** True if the scope is NS tracking current target. */
	private boolean nonSiderealTracking;

	
	public ChangeTracker() {
		offset = new XPositionOffset(false, 0.0, 0.0);
		// start off with an absolute offset of (0,0) asec.

		focusOffset = new XFocusOffset(false, 0.0);
		// start off with an absolute offset of 0.0 mm.

		autoguide = new XAutoguiderConfig(IAutoguiderConfig.OFF, "autoOff");
		System.err.println("Collator: Create New collator: Offset=" + offset + ", Foff=" + focusOffset + ", Ag="
				+ autoguide);
		acquireImage = "NONE"; // safest to use this...
	}

	/** Clone this collator and append the clone-name to inherited name. */
	public ChangeTracker clone(String cloneName) {

		// e.g. Collator-gr08B.red-tracker

		ChangeTracker newTracker = new ChangeTracker(ident + "." + cloneName);
		newTracker.setLastConfig(lastConfig);
		newTracker.setLastTarget(lastTarget);
		newTracker.setInstrumentName(instrumentName);
		newTracker.setAutoguide(autoguide);
		newTracker.setTracking(tracking);
		newTracker.applyOffset(offset);
		newTracker.applyFocusOffset(focusOffset);
		newTracker.setRotator(rotator);
		newTracker.setApertureInstrument(apertureInstrument);
		newTracker.setAcquired(acquired);
		newTracker.setAcquireImage(acquireImage);
		newTracker.setAcqConfig(acqConfig);
		newTracker.setAcquireOffset(acquireOffset);
		newTracker.setAcqInstrument(acqInstrument);
		return newTracker;
	}

	/**
	 * @param ident
	 */
	public ChangeTracker(String ident) {
		this();
		this.ident = ident + "/" + System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "Collator: " + ident;
	}

	/**
	 * @return the instrumentName
	 */
	public String getInstrumentName() {
		return instrumentName;
	}

	/**
	 * @param instrumentName
	 *            the instrumentName to set
	 */
	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
		System.err.println("Collator: " + ident + ":setInstrument: " + instrumentName);
	}

	/**
	 * @return the focusOffset
	 */
	public XFocusOffset getFocusOffset() {
		return focusOffset;
	}

	/**
	 * @param focusOffset
	 *            the focusOffset to set
	 */
	public void applyFocusOffset(XFocusOffset newFocusOffset) {
		// either add this to current or replace
		if (newFocusOffset.isRelative()) {
			double df = focusOffset.getOffset() + newFocusOffset.getOffset();
			focusOffset.setOffset(df);
		} else {
			focusOffset.setOffset(newFocusOffset.getOffset());
		}
		System.err.println("Collator: applyFocusOffset: " + newFocusOffset + "-> " + focusOffset);
	}

	/**
	 * @return the lastConfig
	 */
	public IInstrumentConfig getLastConfig() {
		return lastConfig;
	}

	/**
	 * @param lastConfig
	 *            the lastConfig to set
	 */
	public void setLastConfig(IInstrumentConfig lastConfig) {
		this.lastConfig = lastConfig;
		System.err.println("Collator: " + ident + ":setInstConfig: "
				+ (lastConfig == null ? "NULL" : lastConfig.getClass().getSimpleName() + ":" + lastConfig.getName()));
	}

	/**
	 * @return the lastTarget
	 */
	public ITarget getLastTarget() {
		return lastTarget;
	}

	/**
	 * @param lastTarget
	 *            the lastTarget to set
	 */
	public void setLastTarget(ITarget lastTarget) {
		this.lastTarget = lastTarget;
		System.err.println("Collator: setTarget: "
				+ (lastTarget == null ? "NULL" : lastTarget.getClass().getSimpleName() + ":" + lastTarget.getName()));
	}

	/**
	 * @return the autoguiding
	 */
	public IAutoguiderConfig getAutoguide() {
		return autoguide;
	}

	/**
	 * @param autoguiding
	 *            the autoguiding to set
	 */
	public void setAutoguide(IAutoguiderConfig autoguide) {
		this.autoguide = autoguide;
		System.err.println("Collator:: setAutoguider: " + autoguide);
	}
	
	

	/**
	 * @return the acqConfig
	 */
	public IAcquisitionConfig getAcqConfig() {
		return acqConfig;
	}

	/**
	 * @param acqConfig
	 *            the acqConfig to set
	 */
	public void setAcqConfig(IAcquisitionConfig acqConfig) {
		this.acqConfig = acqConfig;
		System.err.println("Collator: setAcqConfig: " + acqConfig);
	}

	/**
	 * @return the acquireOffset
	 */
	public DetectorArrayPosition getAcquireOffset() {
		return acquireOffset;
	}

	/**
	 * @param acquireOffset
	 *            the acquireOffset to set
	 */
	public void setAcquireOffset(DetectorArrayPosition acquireOffset) {
		this.acquireOffset = acquireOffset;
		System.err.println("Collator: setAcquireOffset: " + acquireOffset);
	}

	/**
	 * @return the acqInstrument
	 */
	public String getAcqInstrument() {
		return acqInstrument;
	}

	/**
	 * @param acqInstrument
	 *            the acqInstrument to set
	 */
	public void setAcqInstrument(String acqInstrument) {
		this.acqInstrument = acqInstrument;
		System.err.println("Collator: " + ident + ":setAcquireInstr: " + acqInstrument);
	}

	/**
	 * @return the tracking
	 */
	public boolean isTracking() {
		return tracking;
	}

	/**
	 * @param tracking
	 *            the tracking to set
	 */
	public void setTracking(boolean tracking) {
		this.tracking = tracking;
		System.err.println("Collator: setTracking: " + tracking);
	}

	/**
	 * @return the offset
	 */
	public XPositionOffset getOffset() {
		return offset;
	}

	/**
	 * Append the supplied offset to the cumulative offset (if REL) or replace
	 * (if ABS).
	 * 
	 * @param offset
	 *            the offset to append or replace with.
	 */
	public void applyOffset(XPositionOffset newOffset) {
		// either add this to current or replace
		if (newOffset.isRelative()) {
			double dra = offset.getRAOffset() + newOffset.getRAOffset();
			double ddec = offset.getDecOffset() + newOffset.getDecOffset();
			offset.setRAOffset(dra);
			offset.setDecOffset(ddec);
		} else {
			offset.setRAOffset(newOffset.getRAOffset());
			offset.setDecOffset(newOffset.getDecOffset());
		}
		System.err.println("Collator: applyOffset: " + newOffset + " -> " + offset);
	}

	/** Clear the cumulative position offset - ie set to (0,0) ABS. */
	public void clearOffset() {
		offset.setRAOffset(0.0);
		offset.setDecOffset(0.0);
		System.err.println("Collator: clearOffsets");
	}

	/**
	 * @return the rotator
	 */
	public IRotatorConfig getRotator() {
		return rotator;
	}

	/**
	 * @param rotator
	 *            the rotator to set
	 */
	public void setRotator(IRotatorConfig rotator) {
		this.rotator = rotator;
		System.err.println("Collator: setRotator: " + rotator);
	}

	/**
	 * @return the apertureOffset
	 */
	public String getApertureInstrument() {
		return apertureInstrument;
	}

	/**
	 * @param apertureOffset
	 *            the apertureOffset to set
	 */
	public void setApertureInstrument(String apertureInstrument) {
		this.apertureInstrument = apertureInstrument;
		System.err.println("Collator: setApertureInstrument: " + apertureInstrument);
	}

	/** Convenience method - clears the apertureInstrument. */
	public void clearApertureInstrument() {
		this.apertureInstrument = null;
		System.err.println("Collator: clearApertureInstrument");
	}

	public boolean hasAcquired() {
		return acquired;
	}

	public void setAcquired(boolean acquired) {
		this.acquired = acquired;
		System.err.println("Collator: setAcquired: " + acquired);
	}

	public void setAcquireImage(String acquireImage) {
		this.acquireImage = acquireImage;
		System.err.println("Collator: setAcquireImage: " + acquireImage);
	}

	public String getAcquireImage() {
		return acquireImage;
	}

	/**
	 * @return the nonSiderealTracking
	 */
	public boolean isNonSiderealTracking() {
		return nonSiderealTracking;
	}

	/**
	 * @param nonSiderealTracking
	 *            the nonSiderealTracking to set
	 */
	public void setNonSiderealTracking(boolean nsTracking) {
		this.nonSiderealTracking = nsTracking;
		System.err.println("Collator: setNonSiderealTracking: " + nsTracking);
	}

	/**
	 * Depth-first search down through the given iterator to determine
	 * target/config changes + others TBD.
	 */
	public void collateChanges(XIteratorComponent iterator) {

		List compList = iterator.listChildComponents();
		Iterator comps = compList.iterator();
		while (comps.hasNext()) {
			ISequenceComponent comp = (ISequenceComponent) comps.next();
			if (comp instanceof XExecutiveComponent) {
				IExecutiveAction action = ((XExecutiveComponent) comp).getExecutiveAction();
				if (action instanceof ITargetSelector) {
					setLastTarget(((ITargetSelector) action).getTarget());
				} else if (action instanceof IInstrumentConfigSelector) {
					setLastConfig(((IInstrumentConfigSelector) action).getInstrumentConfig());
					setInstrumentName(((IInstrumentConfigSelector) action).getInstrumentConfig().getInstrumentName());
				}
			} else if (comp instanceof XIteratorComponent) {
				// recursively sweep sub-iterators
				collateChanges(((XIteratorComponent) comp));
			}
		}
		// At this point we should have the last target and config settings and
		// whether we are guiding, tracking
	}

}
