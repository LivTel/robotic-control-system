/**
 * 
 */
package ngat.rcs.sciops;

import java.util.List;

import ngat.phase2.ISequenceComponent;
import ngat.phase2.XBranchComponent;
import ngat.phase2.XIteratorComponent;
import ngat.rcs.tms.ErrorIndicator;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.manager.ParallelTaskImpl;
//import ngat.sms.GroupItem;
import ngat.sms.GroupItem;

/**
 * @author eng
 * 
 */
public class BranchControlTask extends ParallelTaskImpl {
	
	// ERROR_BASE: RCS = 6, SCIOPS = 60, BRANCH = 400
	
	/** The branch. */
	private XBranchComponent branch;
	
	/** The Group for which this branch is being performed.*/
	private GroupItem group;

    private ChangeTracker collator;
    private ChangeTracker trackerA = null;
	private ChangeTracker trackerB = null;
	
	/**
	 * @param name
	 * @param manager
	 */
    public BranchControlTask(String name, TaskManager manager, GroupItem group, XBranchComponent branch, ChangeTracker collator) {
		super(name, manager);
		this.group = group;
		this.branch = branch;
		this.collator = collator;
	}

	/**
	 * @see ngat.rcs.tms.manager.ParallelTaskImpl#onSubTaskDone(ngat.rcs.tms.Task)
	 */
	@Override
	public void onSubTaskDone(Task task) {		
		super.onSubTaskDone(task);// TODO Auto-generated method stub
		// TODO - Coalesce the separate trackers here - in fact arbitrarily pick tracker-A		
		//collator.setInstrumentName(trackerA.getInstrumentName());
		//collator.setLastConfig(trackerA.getLastConfig());
		// NOTE Leave them alone they are sub-instruments not real ones !!!
	}

	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		ErrorIndicator err = task.getErrorIndicator();
		failed(err);
	}

	@Override
	public void onInit() {
		super.onInit();		
		
		// enable fold control by instrument - and may the lord help us		
		//ngat.rcs.iss.ISS_MOVE_FOLD_CommandImpl.setOverrideForwarding(false);
		
	}
	
	@Override
	public void onDisposal() {
		super.onDisposal();	
		
		// disable the fold control by the instrument
		//ngat.rcs.iss.ISS_MOVE_FOLD_CommandImpl.setOverrideForwarding(true);
		
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {
	
		// There are only ever 2 components in a branch

		List components = branch.listChildComponents();
	
		if (components.size() != 2) {
			failed(660401, "Branch has invalid sub-component count: "+components.size());
			return null;
		}
		
		try {
			trackerA = createBranchSequence(taskList, (ISequenceComponent)components.get(0));
			trackerB = createBranchSequence(taskList, (ISequenceComponent)components.get(1));
		} catch (Exception e) {
			failed(660402, "Unable to create branch: "+e);
			return null;
		}
		

		return taskList;
	}

	/** Create a branch sequence and add task to tasklist.
	 * 
	 * @param taskList
	 * @param component
	 * @return
	 * @throws Exception
	 */
	private ChangeTracker createBranchSequence(TaskList taskList, ISequenceComponent component) throws Exception {
		taskLog.log(2, "BranchControlTask["+name+"].createTaskList(): With: "+component);
		ChangeTracker branchTracker = null;
		
		if (component instanceof XIteratorComponent) {
			String branchName = ((XIteratorComponent)component).getComponentName();
			branchTracker = collator.clone(branchName+"-tracker");

			// ?? TODO Need to work out which sub-inst then tell subtracker
			//      that this instrument is selected and aperture acquired.
			//     deduceIntrName needs to walk tree and pull any configs..

			// TODO String subInstrumentName = deduceInstrumentName((XIteratorComponent)component);
			// TODO branchTracker.setLastInstrument(subInstrumentName);
			// TODO branchTracker.setApertureInstrument(subInstrumentName);
			//redTracker.setInstrumentName(redBranchName); 
			IteratorControlTask branchSequenceTask = new IteratorControlTask(name + "/"+branchName, 
										      this, 
										      group,
										      (XIteratorComponent)component, 
										      branchTracker);
			taskList.addTask(branchSequenceTask);

		} else
			throw new IllegalArgumentException("Branch sub-component: "+component.getComponentName()+
					" is not an iterator: " + component.getClass().getName());
			
		return branchTracker;
	}
	
	
}
	