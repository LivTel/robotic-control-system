/**
 * 
 */
package ngat.rcs.sciops;

import ngat.astrometry.BasicCardinalPointingCalculator;
import ngat.astrometry.ISite;
import ngat.astrometry.Position;
import ngat.icm.InstrumentCapabilities;
import ngat.icm.InstrumentCapabilitiesProvider;
import ngat.icm.InstrumentDescriptor;
import ngat.icm.InstrumentRegistry;
import ngat.message.RCS_TCS.ROTATOR;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.XRotatorConfig;
import ngat.phase2.ITarget;
import ngat.rcs.RCS_Controller;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.executive.RotatorTask;
import ngat.rcs.tms.manager.ParallelTaskImpl;

/**
 * @author eng
 * 
 */
public class RotatorControlTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, ROT_CTRL = 1200
	
	/** Rotator base offset from science payload info.*/
	private double rotatorBaseOffset = Math.toRadians(56.6);

	/** The rotator configuration. */
	private IRotatorConfig rotatorConfig;

	/** Keeps track of changes. */
	private ChangeTracker collator;

	/** How long we expect to run on this rotator setting (ms). */
	private long runTime;

	/** The angle to use (rads). */
	private double angle;

	/**
	 * Name of instrument whose rotator plane we are using (if sky or cardinal).
	 */
	private String rotInstName;

	/** Instrument's specific offset (relative to nominal RATCAM offset). */
	private double instOffset;

	private ngat.astrometry.CardinalPointingCalculator cpc;

	/**
	 * @param name
	 *            Name of this task.
	 * @param manager
	 *            The task's manager.
	 * @param rotatorConfig
	 *            The rotator config to apply.
	 * @param collator
	 *            Tracks changes in execution to this point.
	 * @param runTime
	 *            How long we expect to run on this rotator setting.
	 */
	public RotatorControlTask(String name, TaskManager manager, IRotatorConfig rotatorConfig, ChangeTracker collator,
			long runTime) {
		super(name, manager);
		this.rotatorConfig = rotatorConfig;
		this.collator = collator;
		this.runTime = runTime;
	}

	@Override
	public void preInit() {
		super.preInit();

		ITarget target = collator.getLastTarget();
	
		try {
			//IOFF = Math.toRadians(Double.parseDouble(System.getProperty("rotator.sky.base.offset")));
			
			rotatorBaseOffset = payload.getRotatorBaseOffset();
			System.err.println("RotatorControl:: Using instrument offset: "+Math.toDegrees(rotatorBaseOffset));

		} catch (Exception x) {
			x.printStackTrace();
			failed(661201, "Unable to determine rotator base offset");
			return;
		}
		
		long t1 = System.currentTimeMillis();
		long t2 = t1 + runTime;

		// now try to see if this rotator config is valid...
		switch (rotatorConfig.getRotatorMode()) {
		case IRotatorConfig.MOUNT:

			try {
				angle = rotatorConfig.getRotatorAngle();

				ISite site = RCS_Controller.controller.getSite();
				cpc = new BasicCardinalPointingCalculator(site);

				// convert mount to sky at start of obs, use IOFF here though
				// any offset will do !
				double sky = cpc.getSkyAngle(angle, target, rotatorBaseOffset, t1);
				boolean okangle = cpc.isFeasibleSkyAngle(sky, target, rotatorBaseOffset, t1, t2);

				if (!okangle) {
					failed(661202, "Requested mount angle not currently feasible for target");
					return;
				}
			} catch (Exception csx) {
				csx.printStackTrace();
				failed(661203, "Unable to obtain valid rotator mount angle solution for target");
				return;
			}
			break;
		case IRotatorConfig.CARDINAL:

			try {
				ISite site = RCS_Controller.controller.getSite();
				cpc = new BasicCardinalPointingCalculator(site);
				rotInstName = rotatorConfig.getInstrumentName().trim().toUpperCase();
				InstrumentRegistry ireg = RCS_Controller.controller.getInstrumentRegistry();
				InstrumentDescriptor id = new InstrumentDescriptor(rotInstName);
				InstrumentCapabilitiesProvider icp = ireg.getCapabilitiesProvider(id);
				InstrumentCapabilities icap = icp.getCapabilities();
				instOffset = icap.getRotatorOffset();

				boolean ok000 = cpc.isFeasibleSkyAngle(0.0, target, rotatorBaseOffset - instOffset, t1, t2);
				boolean ok090 = cpc.isFeasibleSkyAngle(0.5 * Math.PI, target, rotatorBaseOffset - instOffset, t1, t2);
				boolean ok180 = cpc.isFeasibleSkyAngle(1.0 * Math.PI, target, rotatorBaseOffset - instOffset, t1, t2);
				boolean ok270 = cpc.isFeasibleSkyAngle(1.5 * Math.PI, target, rotatorBaseOffset - instOffset, t1, t2);
				// now work out which is best...

				angle = cpc.getBestCardinalAngle(target, rotatorBaseOffset - instOffset, t1, t2);
			} catch (Exception csx) {
				csx.printStackTrace();
				failed(661204, "Unable to obtain valid rotator CP angle solution for target");
				return;
			}

			break;
		case IRotatorConfig.SKY:
			try {
				angle = rotatorConfig.getRotatorAngle();
				ISite site = RCS_Controller.controller.getSite();
				cpc = new BasicCardinalPointingCalculator(site);
				rotInstName = rotatorConfig.getInstrumentName().trim().toUpperCase();
				InstrumentRegistry ireg = RCS_Controller.controller.getInstrumentRegistry();
				InstrumentDescriptor id = new InstrumentDescriptor(rotInstName);
				InstrumentCapabilitiesProvider icp = ireg.getCapabilitiesProvider(id);
				InstrumentCapabilities icap = icp.getCapabilities();
				instOffset = icap.getRotatorOffset();

				boolean okangle = cpc.isFeasibleSkyAngle(angle, target, rotatorBaseOffset - instOffset, t1, t2);

				if (!okangle) {
					failed(661205, "Requested sky angle not currently feasible for target");
					return;
				}

			} catch (Exception csx) {
				failed(661206, "Unable to obtain valid rotator sky angle solution for target");
				return;
			}

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {

		// switch rotator tracking on
		EnableAxisTrackingTask enableTrackingTask = new EnableAxisTrackingTask(name + "/TRACK_ENABLE_ROT", this, false,
				false, true);
		taskList.addTask(enableTrackingTask);

		int rotMode = ROTATOR.SKY;
		String rotDesc = "";
		double useAngle = angle;
		switch (rotatorConfig.getRotatorMode()) {
		case IRotatorConfig.MOUNT:
			rotMode = ROTATOR.MOUNT;
			rotDesc = "Mount(" + Position.toDegrees(angle, 2) + ")";

			RotatorTask rotatorMountTask = new RotatorTask(name + "/ROTATE(" + rotDesc + ")", this, angle, rotMode);
			taskList.addTask(rotatorMountTask);

			RotatorTask rotatorFloatTask = new RotatorTask(name + "/ROTATE(Float)", this, 0.0, ROTATOR.FLOAT);
			taskList.addTask(rotatorFloatTask);

			try {
				rotatorMountTask.setDelay(3000L);
				taskList.sequence(enableTrackingTask, rotatorMountTask);
				taskList.sequence(rotatorMountTask, rotatorFloatTask);
			} catch (TaskSequenceException tsx) {
				failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error: " + tsx);
				return null;
			}

			break;
		case IRotatorConfig.CARDINAL:
			rotMode = ROTATOR.SKY;
			// TODO IMPORTANT
			// TODO IMPORTANT note we need to recall both the instrument angle
			// as supplied and the ratcam angle to send
			// TODO IMPORTANT

			useAngle = angle - instOffset;
			if (useAngle < 0.0)
				useAngle += Math.PI * 2.0;

			if (useAngle > Math.PI * 2.0)
				useAngle -= Math.PI * 2.0;

			rotDesc = "Cardinal@Sky(" + rotInstName + ":" + Position.toDegrees(angle, 2) + ", TCS:"
					+ Position.toDegrees(useAngle, 2) + ")";
			// Cardinal@Sky(RISE:270.0, TCS:225.0))
			RotatorTask rotatorCardinalTask = new RotatorTask(name + "/ROTATE(" + rotDesc + ")", this, useAngle,
					rotMode);
			taskList.addTask(rotatorCardinalTask);

			try {
				rotatorCardinalTask.setDelay(3000L);
				taskList.sequence(enableTrackingTask, rotatorCardinalTask);
			} catch (TaskSequenceException tsx) {
				failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error: " + tsx);
				return null;
			}
			break;
		case IRotatorConfig.SKY:
			rotMode = ROTATOR.SKY;
			useAngle = angle - instOffset;
			if (useAngle < 0.0)
				useAngle += Math.PI * 2.0;

			if (useAngle > Math.PI * 2.0)
				useAngle -= Math.PI * 2.0;
			rotDesc = "Sky(" + rotInstName + ":" + Position.toDegrees(angle, 2) + ", TCS:"
					+ Position.toDegrees(useAngle, 2) + ")";
			// Sky(RISE:270.0, TCS:225.0))
			RotatorTask rotatorSkyTask = new RotatorTask(name + "/ROTATE(" + rotDesc + ")", this, useAngle, rotMode);
			taskList.addTask(rotatorSkyTask);

			try {
				rotatorSkyTask.setDelay(3000L);
				taskList.sequence(enableTrackingTask, rotatorSkyTask);
			} catch (TaskSequenceException tsx) {
				failed(TaskList.TASK_SEQUENCE_ERROR, "Task sequencing error: " + tsx);
				return null;
			}

			break;
		}

		return taskList;

	}

	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);
		// if this was a CP then we record the angle used, otherwise its what
		// was set anyway
		((XRotatorConfig) rotatorConfig).setRotatorAngle(angle);
		collator.setRotator(rotatorConfig);
		collator.setAcquired(false); // we have definitely lost this
		collator.clearApertureInstrument();
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		collator.clearApertureInstrument();
		// TODO - IS THIS A GOOD IDEA ! skip this if it fails ?
		if (task instanceof EnableAxisTrackingTask) {
			taskList.skip(task);
		} else {
			ErrorIndicator err = task.getErrorIndicator();
			failed(err.getErrorCode(), "Rotator selection failure");
		}
	}

}
