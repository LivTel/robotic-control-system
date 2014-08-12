/**
 * 
 */
package ngat.rcs.sciops;

import ngat.astrometry.Position;
import ngat.astrometry.Astrometry;
import ngat.astrometry.Tracking;
import ngat.phase2.ITarget;
import ngat.phase2.ITargetSelector;
import ngat.phase2.Source;
import ngat.phase2.XEphemerisTarget;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.iss.ISS;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.SlewTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class TargetSelectorTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, TARG_SEL = 1500
	
	/** Threshold for NS tracking 30 as/hr = 2.314814815e-6 rad/sec. */
	public static final double NS_TRACK_THRESH = Math.toRadians(30.0 / (3600.0 * 3600.0));

	/** The target selection. */
	private ITargetSelector targetSelector;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/** Record if we will be NS tracking. */
	private boolean nsTrackTarget;

	/**
	 * @param name
	 * @param manager
	 */
	public TargetSelectorTask(String name, TaskManager manager, ITargetSelector targetSelector, ChangeTracker collator) {
		super(name, manager);
		this.targetSelector = targetSelector;
		this.collator = collator;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		super.preInit();

		ITarget target = targetSelector.getTarget();
		if (target == null) {
			failed(661501, "No target specified for TargetSelector");
			return;
		}
		FITS_HeaderInfo.fillFitsTargetHeaders(target);

	}

	@Override
	protected TaskList createTaskList() {

		ITarget target = targetSelector.getTarget();

		taskLog.log(2, "TargetSelectTask:createTaskList(): with " + target);

		// this will be the old-style source we want...
		Source source = null;

		try {
			source = TargetTranslator.translateToOldStyleSource(target);
		} catch (TargetTranslationException tx) {
			failed(661502, "TargetTranslator failed: " + tx);
			return null;
		}

		SlewTask slewTask = new SlewTask(name + "/SLEW(" + source.getName() + ")", this, source);
		if (target instanceof XEphemerisTarget) {

			Tracking tracking = Astrometry.getPlanetTracking(source);
			double nstra = tracking.getNsTrackRA();
			double nsdec = tracking.getNsTrackDec();

			Position pos = Astrometry.getPlanetPosition(source);
			double cdec = Math.cos(pos.getDec());
			// calculate overall tracking rate over ground (rad/sec)
			double nsTrackRate = Math.sqrt(nstra * nstra * cdec * cdec + nsdec * nsdec);
			taskLog.log(2, "TargetSelectTask:createTaskList(): Ephemeris Target. At: (" + 
					Position.toHMSString(pos.getRA()) + ", "
					+ Position.toDMSString(pos.getDec()) + ") Estimated NS track rate now: (" + (Math.toDegrees(nstra) * 240.0) + " s/s "
					+ (Math.toDegrees(nsdec) * 3600.0) + " as/s), Overall rate: "
					+ (3600.0 * 3600.0 * Math.toDegrees(nsTrackRate)) + " as/h");

			// TargetSelectTask:createTaskList(): Ephemeris Target. At: (10:10:10, +22:22:33) 
			// Estimated NS track rate now: (3.2 s/s, -12.4 as/s), Overall rate: 34.3 as/h
			
			if (nsTrackRate > NS_TRACK_THRESH) {
				taskLog.log(2,
						"TargetSelectTask:createTaskList(): Track rate exceeds threshold, will track non-sidereally");
				slewTask.setNsTracking(true);
				nsTrackTarget = true; // needed to update collator
			} else {
				taskLog.log(2, "TargetSelectTask:createTaskList(): Track rate below threshold, will track sidereally");
			}

		}
		taskList.addTask(slewTask);

		return taskList;

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		collator.setLastTarget(targetSelector.getTarget());
		collator.setNonSiderealTracking(nsTrackTarget);
		collator.clearOffset(); 
		ISS.setUserOffsets(0.0, 0.0);
		collator.setAcquired(false); // we have definitely lost this
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err  = task.getErrorIndicator();
		failed(err.getErrorCode(), "Slew failure");	
	}

}
