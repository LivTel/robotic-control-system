/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.IRotatorConfig;
import ngat.phase2.ITarget;
import ngat.phase2.Source;
import ngat.phase2.XPositionOffset;
import ngat.phase2.XSlew;
import ngat.rcs.RCS_Controller;
import ngat.rcs.iss.FITS_HeaderInfo;
import ngat.rcs.iss.ISS;
import ngat.rcs.scm.collation.StatusPool;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.executive.SlewTask;
import ngat.rcs.tms.executive.TweakTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.tcm.BasicTelescopeAlignmentAdjuster;
import ngat.tcm.BasicTelescopeSystem;

/**
 * @author eng
 * 
 */
public class TargettingControlTask extends ParallelTaskImpl {

	// ERROR_BASE: RCS = 6, SCIOPS = 60, TARG_CTRL = 1600

	/** A slew action - selects target, rotator setting and tracking mode. */
	private XSlew slew;

	/** The target. */
	private ITarget target;

	/** The rotator setting. */
	private IRotatorConfig rotator;

	/** The tracking mode. */
	private boolean nstrack;

	/** Tweak X offset (asec). */
	private double tweakOffsetX;

	/** Tweak Y offset (asec). */
	private double tweakOffsetY;

	/** Collates changes. */
	private ChangeTracker collator;
	private ChangeTracker subtracker;

	// TODO this is currently an OVER-ESTIMATE
	/** How long we expect to be tracking the target for. */
	private long runTime;

	/** How long we actually use in the rotator calculator. */
	private long useRunTime;

	/** Set true when the slew part has completed.*/
	private boolean slewDone;
	
	/** Set true when the rotator part has completed.*/
	private boolean rotatorDone;
	
	/**
	 * Time this task would have been created ie when the enclosing iterator
	 * started.
	 */
	private long iteratorStartTime;

	/**
	 * Create a task to perform slew on all axes and set tracking mode.
	 * 
	 * @param name
	 *            Name of the Task.
	 * @param manager
	 *            Manager of task.
	 * @param slew
	 *            A slew object (contains target, rotator setting and tracking
	 *            mode.
	 * @param collator
	 *            Keeps track of changes to date.
	 * @param runTime
	 *            How long are we expecting to track the target.
	 */
	public TargettingControlTask(String name, TaskManager manager, XSlew slew, ChangeTracker collator, long runTime) {
		super(name, manager);
		this.slew = slew;
		this.collator = collator;
		this.runTime = runTime;
		iteratorStartTime = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onCompletion()
	 */
	@Override
	public void onCompletion() {
		super.onCompletion();
		collator.setLastTarget(target);
		collator.setNonSiderealTracking(nstrack);
		collator.clearOffset();
		ISS.setUserOffsets(0.0, 0.0);
		collator.setAcquired(false); // we have definitely lost this
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#onInit()
	 */
	@Override
	public void onInit() {
		// TODO Auto-generated method stub
		super.onInit();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		// TODO Auto-generated method stub
		super.preInit();

		target = slew.getTarget();
		rotator = slew.getRotatorConfig();
		nstrack = slew.usesNonSiderealTracking();

		if (target == null) {
			failed(661601, "No target specified for Slew");
			return;
		}
		FITS_HeaderInfo.fillFitsTargetHeaders(target);
		long timeSinceStart = System.currentTimeMillis() - iteratorStartTime;
		long timeToRun = iteratorStartTime + runTime - System.currentTimeMillis();
		taskLog.log(
				3,
				String.format(
						"Initializing slew. Enclosing iterator started at: %tF %tT, (%4d s ago), Total Exec time: %4d s, Time to run: %4d s",
						iteratorStartTime, iteratorStartTime, (timeSinceStart / 1000), (runTime / 1000),
						(timeToRun / 1000)));

		String strRot = System.getProperty("rotator.runtime.calculator.mode", "normal");

		taskLog.log(WARNING, 1, CLASS, name, "preInit", "Creating initial tasklist: Rotator calculator uses mode: "
				+ strRot);

		if (strRot.equalsIgnoreCase("fraction"))
			useRunTime = timeToRun;
		else
			useRunTime = runTime;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskDone(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskDone(Task task) {
				
		super.onSubTaskDone(task);
		if (task instanceof SlewTask) {
			collator.setLastTarget(target);
			collator.setNonSiderealTracking(nstrack);
			collator.clearOffset();
			ISS.setUserOffsets(0.0, 0.0);
			collator.setAcquired(false); // we have definitely lost this
			collator.clearApertureInstrument();
			
			// record that slew has completed
			slewDone = true;
			
		} else if (task instanceof RotatorControlTask) {
			// collate subtracker rotator info to master
			if (subtracker != null)
				collator.setRotator(subtracker.getRotator());

			// record that rotation has completed
			rotatorDone = true;
			
		} else if (task instanceof TweakTask) {
			return;		
		}
		

		if (slewDone && rotatorDone) {

			// now invoke the tweaker ...
			taskLog.log(3, "Slew and rotator setting completed, calculating tweak offsets");
		
		
			// obtain the current rotator position or demand ? 
			double rotatorPosition = StatusPool.latest().mechanisms.rotPos;
			double rotatorDemand = StatusPool.latest().mechanisms.rotDemand;

			BasicTelescopeSystem bts = null;
			BasicTelescopeAlignmentAdjuster bta;

			try {
				bts = (BasicTelescopeSystem) RCS_Controller.controller.getTelescope().getTelescopeSystem();
			} catch (Exception e) {
				failed = true;

				failed(661603, "Unable to locate telescope system", e);
				return;
			}

			
			try {
				bta = bts.getAdjuster();
			} catch (Exception e) {
				failed(661604, "Unable to locate telescope alignment adjuster", e);
				return;
			}

			try {
				// rotator is in degrees
				XPositionOffset offset = bta.interpolate(rotatorDemand);
				// these are in arcsecs
				tweakOffsetX = offset.getRAOffset();
				tweakOffsetY = offset.getDecOffset();
				taskLog.log(1, CLASS, name, "onInit", 
						"Tweak offsets for rotator: C" + rotatorPosition + " degs"+
								", D: "+rotatorDemand+" degs are: x: "
								+ tweakOffsetX + " asec, y: " + tweakOffsetY + " asec");
				// these are still in arcsecs here
				
				TweakTask tweakTask = new TweakTask(name + "/TWEAK(" + tweakOffsetX + "," + tweakOffsetY + ")",
						this,
						Math.toRadians(tweakOffsetX / 3600.0), 
						Math.toRadians(tweakOffsetY / 3600.0));
				
				taskList.addTask(tweakTask);
				
			} catch (Exception e) {
				failed(661605, "Unable to determine valid alignment solution", e);
				return;
			}

		}
		
		
		
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.rcs.tmm.manager.ParallelTaskImpl#onSubTaskFailed(ngat.rcs.tmm.Task)
	 */
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		failed(err.getErrorCode(), "Slew failure");
	}

	@Override
	protected TaskList createTaskList() {

		// we need to slew and rotate, and we need to tell the rotator task
		// which target we shall be using
		// via a collator but not the one passed in - but a fudged temporary
		// clone.

		subtracker = collator.clone("subtracker");
		subtracker.setLastTarget(target);

		// this will be the old-style source we want...
		Source source = null;

		try {
			source = TargetTranslator.translateToOldStyleSource(target);
		} catch (TargetTranslationException tx) {
			failed(661602, "TargetTranslator failed: " + tx);
			return null;
		}
		SlewTask slewTask = new SlewTask(name + "/SLEW(" + source.getName() + ")", this, source);
		slewTask.setNsTracking(nstrack);
		taskList.addTask(slewTask);

		// NOTE the use of the cloned tracker to pass the target in -it hasnt
		// been slewed onto yet.
		RotatorControlTask rotatorControlTask = new RotatorControlTask(name + "/Rotate", this, rotator, subtracker,
				useRunTime);
		taskList.addTask(rotatorControlTask);

		return taskList;
	}

}
