/**
 * 
 */
package ngat.rcs.sciops;

import java.util.Iterator;
import java.util.List;

import ngat.phase2.IAcquisitionConfig;
import ngat.phase2.IAutoguiderConfig;
import ngat.phase2.IBeamSteeringConfig;
import ngat.phase2.ICalibration;
import ngat.phase2.IExecutiveAction;
import ngat.phase2.IExposure;
import ngat.phase2.IFocusControl;
import ngat.phase2.IFocusOffset;
import ngat.phase2.IInstrumentConfigSelector;
import ngat.phase2.IMosaicOffset;
import ngat.phase2.ISlew;
import ngat.phase2.XBeamSteeringConfig;
import ngat.phase2.XFocusControl;
import ngat.phase2.XPeriodExposure;
import ngat.phase2.XPeriodRunAtExposure;
import ngat.phase2.XPositionOffset;
import ngat.phase2.IRotatorConfig;
import ngat.phase2.ISequenceComponent;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XExecutiveComponent;
import ngat.phase2.XMultipleExposure;
import ngat.phase2.XInstrumentConfigSelector;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XSlew;
import ngat.phase2.XTargetSelector;
import ngat.phase2.XFocusOffset;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.TaskSequenceException;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.sms.ExecutionResourceUsageEstimationModel;
import ngat.sms.GroupItem;
import ngat.sms.bds.TestResourceUsageEstimator;

/**
 * @author eng
 * 
 */
public class IteratorSequenceTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, ITER_SEQ = 1100
	
	/** The IteratorComponent to execute. */
	XIteratorComponent iterator;

	/** The Group for which this iterator is being performed. */
	private GroupItem group;

	private ChangeTracker collator;

	//private ExecutionTimingCalculator execTimingCalc;
	private ExecutionResourceUsageEstimationModel execTimingCalc;
	
	/** Records the time the iterator was started.*/
	private long iteratorStartTime;
	
	/**
	 * @param name
	 * @param manager
	 */
	public IteratorSequenceTask(String name, TaskManager manager, GroupItem group, XIteratorComponent iterator,
			ChangeTracker collator) {
		super(name, manager);
		this.group = group;
		this.iterator = iterator;
		this.collator = collator;
		//execTimingCalc = new ExecutionTimingCalculator();
		execTimingCalc = new TestResourceUsageEstimator();
		
	}

	/** Called before the tasklist is created during initialization
	 * @see ngat.rcs.tms.manager.ParallelTaskImpl#preInit()
	 */
	@Override
	public void preInit() {
		super.preInit();
		iteratorStartTime = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList(ngat.rcs.tmm.
	 * TaskMonitorFactory)
	 */
	@Override
	protected TaskList createTaskList() {
		
		String strScm= System.getProperty("slew.sequence.control.mode", "normal");

		taskLog.log(WARNING, 1, CLASS, name, "CreateTaskList", 
				"Creating initial tasklist: Sequence control mode: "+strScm);

		Task ctask = null;

		List components = iterator.listChildComponents();
		Iterator icomp = components.iterator();
		// components.

		while (icomp.hasNext()) {

			// for (int index = 0; index < components.size(); index++) {

			ISequenceComponent comp = (ISequenceComponent) icomp.next();
			// ISequenceComponent comp = (ISequenceComponent)
			// components.get(index);

			taskLog.log(2, "IteratorSeqTask[" + name + "].createTaskList(): With: " + comp);

			if (comp instanceof XIteratorComponent) {
				XIteratorComponent xit = (XIteratorComponent) comp;
				IteratorControlTask itask = new IteratorControlTask(name + "/IC(" + xit.getComponentName() + ")", this,
						group, xit, collator);
				taskList.addTask(itask);

				if (ctask != null) {
					try {
						taskList.sequence(ctask, itask);
					} catch (TaskSequenceException tx) {
						failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
					}
				}
				ctask = itask;

			} else if (comp instanceof XBranchComponent) {

				// TODO create a branchtask
				XBranchComponent xbran = (XBranchComponent) comp;

				BranchControlTask brantask = new BranchControlTask(name + "/Branch-" + xbran.getComponentName(), this,
						group, xbran, collator);
				taskList.addTask(brantask);

				if (ctask != null) {
					try {
						taskList.sequence(ctask, brantask);
					} catch (TaskSequenceException tx) {
						failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
					}
				}
				ctask = brantask;

			} else if (comp instanceof XExecutiveComponent) {

				XExecutiveComponent xec = (XExecutiveComponent) comp;
				IExecutiveAction action = xec.getExecutiveAction();

				// TODO build a suitable task based on the action type..
				// e.g. Slew, Config, Rotate, Autoguide,.....

				if (action instanceof ISlew) {

					XSlew slew = (XSlew) action;

					// TODO this is currently an OVER-ESTIMATE
					// long runTime = execTimingCalc.calcExecTimeOfSequence(iterator);
					long runTime = (long)execTimingCalc.getExecTime(iterator);
				
					taskLog.log(3, "Creating slew task. Calculated execution time for sequence: " + runTime + "ms"+
						"Iterator started at: "+iteratorStartTime);

					Task stask = null;
					TargettingControlTask targettingControlTask = null;
					SlewControlTask slewControlTask = null;
					if (strScm.equalsIgnoreCase("normal")) {
						targettingControlTask = new TargettingControlTask(name + "/Targetting(TCT)", this, slew, collator,
						runTime);					
						taskList.addTask(targettingControlTask);
						stask = targettingControlTask;
					} else {
						slewControlTask = new SlewControlTask(name+ "/Targetting(SCT)", this, slew, collator, runTime);					
						taskList.addTask(slewControlTask);
						stask = slewControlTask;
					}
					
					if (ctask != null) {
						try {
							// REAL taskList.sequence(ctask, targettingControlTask);
							taskList.sequence(ctask, stask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					
					if (strScm.equalsIgnoreCase("normal")) {
						ctask = targettingControlTask;
					} else {
						ctask = slewControlTask;
					}
					
					// TESTING now lets do some clever forward searching stuff........
					//System.err.println("Starting look-ahead to locate which instrument the slew will be using...");
					//ConfigFinder cf = new ConfigFinder(slew);
					//cf.locate(iterator);
					//System.err.println("After the forward scan, I believe this slew is for: "+cf.getConfig());					
					// TESTING that was fun !

				} else if (action instanceof XTargetSelector) {
					XTargetSelector targetSelect = (XTargetSelector) action;

					TargetSelectorTask targetTask = new TargetSelectorTask(name + "/TargetSelect", this, targetSelect,
							collator);
					taskList.addTask(targetTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, targetTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					
					ctask = targetTask;
					
				} else if (action instanceof IInstrumentConfigSelector) {

					XInstrumentConfigSelector instConfigSelect = (XInstrumentConfigSelector) action;

					InstrumentConfigSelectorTask instrumentTask = new InstrumentConfigSelectorTask(name
							+ "/InstConfigure", this, instConfigSelect, collator);

					taskList.addTask(instrumentTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, instrumentTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = instrumentTask;

					
				} else if (action instanceof IBeamSteeringConfig) {
					
					XBeamSteeringConfig beam = (XBeamSteeringConfig)action;
					
					BeamSteeringTask beamSteerTask = new BeamSteeringTask(name+"/BeamSteer", this, beam, collator);
					
					taskList.addTask(beamSteerTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, beamSteerTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = beamSteerTask;
					
				} else if (action instanceof IExposure) {

					IExposure exposure = (IExposure) action;

					// the collator passes down the latest setup information
					// TODO fake the exposure name for headers
					// TODO Need to make this a generic feature for exposures of different types...
					if (exposure instanceof XMultipleExposure)
						((XMultipleExposure) exposure).setName(xec.getComponentName());
					else if (exposure instanceof XPeriodExposure)
						((XPeriodExposure)exposure).setName(xec.getComponentName());
					else if (exposure instanceof XPeriodRunAtExposure)
						((XPeriodRunAtExposure)exposure).setName(xec.getComponentName());
					
					ExposureTask exposeTask = new ExposureTask(name + "/Exposure", this, group, exposure, collator);
					taskList.addTask(exposeTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, exposeTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = exposeTask;

				} else if (action instanceof IMosaicOffset) {

					XPositionOffset offset = (XPositionOffset) action;
					// the collator passes down the latest setup information

					PositionOffsetTask offsetTask = new PositionOffsetTask(name + "/Offset", this, offset, collator);
					taskList.addTask(offsetTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, offsetTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = offsetTask;

				} else if
				(action instanceof IFocusControl) {
					
					XFocusControl control = (XFocusControl)action;
					
					FocusControlTask fctrlTask = new FocusControlTask(name+"/FocusControl", this, control, collator);
					taskList.addTask(fctrlTask);
					if (ctask != null) {
						try {
							taskList.sequence(ctask, fctrlTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = fctrlTask;
					
				} else if (action instanceof IFocusOffset) {

					XFocusOffset foff = (XFocusOffset) action;

					// TODO this will only work for single offset in whole group
					// ISS.currentFocusOffset = foff.getOffset();

					FocusOffsetTask foffTask = new FocusOffsetTask(name + "/FocOffset", this, foff, collator);
					taskList.addTask(foffTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, foffTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = foffTask;

				} else if (action instanceof IAutoguiderConfig) {

					IAutoguiderConfig auto = (IAutoguiderConfig) action;

					// the collator passes down the latest setup information
					AutoguiderControlTask autoTask = new AutoguiderControlTask(name + "/Autoguide", this, auto,
							collator);
					taskList.addTask(autoTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, autoTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = autoTask;

				} else if (action instanceof ICalibration) {

					ICalibration calib = (ICalibration) action;

					CalibrationTask calibTask = new CalibrationTask(name + "/Calib", this, calib, collator);
					taskList.addTask(calibTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, calibTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = calibTask;

				} else if (action instanceof IAcquisitionConfig) {

					IAcquisitionConfig acquire = (IAcquisitionConfig) action;
					AcquisitionTask acquireTask = null;
				
					if (acquire.getMode() == IAcquisitionConfig.INSTRUMENT_CHANGE) {
						acquireTask = new AcquisitionTask(name + "/FocalPlane", this, acquire, collator);
						taskList.addTask(acquireTask);
					} else {
						acquireTask = new AcquisitionTask(name + "/FineTune", this, acquire, collator);
						taskList.addTask(acquireTask);	
					}
					
						if (ctask != null) {
							try {
								taskList.sequence(ctask, acquireTask);
							} catch (TaskSequenceException tx) {
								failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
							}
						}
						ctask = acquireTask;
					

				} else if (action instanceof IRotatorConfig) {

					IRotatorConfig rotatorConfig = (IRotatorConfig) action;

					// TODO this is currently an OVER-ESTIMATE
					// TODO need to work out length of time till next rotation
					//long runTime = execTimingCalc.calcExecTimeOfSequence(iterator);
					long runTime = (long)execTimingCalc.getExecTime(iterator);
					
					taskLog.log(3, "Calculated execution time for sequence: " + runTime + "ms");
					RotatorControlTask rotatorControlTask = new RotatorControlTask(name + "/Rotate", this, rotatorConfig, collator, runTime);
					taskList.addTask(rotatorControlTask);

					if (ctask != null) {
						try {
							taskList.sequence(ctask, rotatorControlTask);
						} catch (TaskSequenceException tx) {
							failed(TaskList.TASK_SEQUENCE_ERROR, "Unable to create task sequence: " + tx);
						}
					}
					ctask = rotatorControlTask;
				}

			} else {
				failed(661101, "Unknown sequence component class: " + comp.getClass().getName());
			}

		}
		return taskList;
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		failed(task.getErrorIndicator());
	}

}