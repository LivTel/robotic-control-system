/**
 * 
 */
package ngat.rcs.sciops;

import ngat.phase2.IIteratorCondition;
import ngat.phase2.XIteratorComponent;
import ngat.phase2.XIteratorRepeatCountCondition;
import ngat.rcs.tms.Task;
import ngat.rcs.tms.TaskList;
import ngat.rcs.tms.TaskManager;
import ngat.rcs.tms.manager.ParallelTaskImpl;
import ngat.sms.GroupItem;


/**
 * @author eng
 *
 */
public class IteratorControlTask extends ParallelTaskImpl {

	/** The IteratorComponent to control. */
	private XIteratorComponent iterator;

	/** The iterator condition.*/
	private IIteratorCondition condition;
	
	/** The Group for which this iterator is being performed.*/
	private GroupItem group;
	
	/** Counts the number of iterations performed so-far. */
	private volatile int iterCount = 0;

	/** Time the first iteration started.*/
	private long iterStart;
	
    private ChangeTracker collator;

	/**
	 * @param name Name of this task.
	 * @param manager Reference to task's manager.
	 * @param group The group which is being executed.
	 * @param iterator 
	 */
    public IteratorControlTask(String name, TaskManager manager, GroupItem group, XIteratorComponent iterator, ChangeTracker collator) {
		super(name, manager);
		this.group = group;
		this.iterator = iterator;
		this.collator = collator;
		condition = iterator.getCondition();
	}
	
	@Override
	public void preInit() {
		super.preInit();
		// setup any variables we might need to allow the iterator condition to be tested
		iterCount = 0;
		iterStart = System.currentTimeMillis();
	}
	
	@Override
	public void onSubTaskFailed(Task task) {
		super.onSubTaskFailed(task);
		failed(task.getErrorIndicator());
	}
	
	@Override
	public void onSubTaskDone(Task task) {
		super.onSubTaskDone(task);

		// check iterator condition - once its reached - no more iterations
		if (condition instanceof XIteratorRepeatCountCondition) {
			int repeatCount = ((XIteratorRepeatCountCondition)condition).getCount();
			taskLog.log(1, CLASS, name, "onSubTaskDone", 
					"Testing iterator completion count "+iterCount+" against repeat condition: "+repeatCount);
			if (iterCount >= repeatCount) {
				taskLog.log(1, CLASS, name, "onSubTaskDone",
						"All iterations are complete");
				return;
			}
		}	
		// TODO consider other types of condition...example below...
/*		else if (condition instanceof XIteratorMaximumTimeCondition) {
		
			long timeSoFar = System.currentTimeMillis() - iterStart;
			long maxTime = ((XIteratorMaximumTimeCondition)condition).getMaximumTime();
			taskLog.log(1, CLASS, name, "onSubTaskDone", 
					"Testing iterator time elapsed "+timeSoFar+" against maximum time condition: "+maxTime);
			if (timeSoFar >= maxTime) {
				taskLog.log(1, CLASS, name, "onSubTaskDone",
				"All iterations are complete");
				return;
			}*/
			
		
		if (task instanceof IteratorSequenceTask) {

			IteratorSequenceTask currentObservationSequence = createNextSequenceTask(iterator);

			// got one - stick it on the tasklist
			if (currentObservationSequence != null)
				taskList.addTask(currentObservationSequence);

		}

	}

	private IteratorSequenceTask createNextSequenceTask(XIteratorComponent iterator) {
		iterCount++;
		// TODO NOTE This label wont work for other types of condition than RepeatCountCondition
		int repeatCount = ((XIteratorRepeatCountCondition)condition).getCount();
		IteratorSequenceTask iteratorSeqTask = new IteratorSequenceTask(name + "/IS-" + iterCount+"of"+repeatCount, this, group, iterator, collator);
		return iteratorSeqTask;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.rcs.tmm.manager.ParallelTaskImpl#createTaskList()
	 */
	@Override
	protected TaskList createTaskList() {

		IteratorSequenceTask currentObservationSequence = createNextSequenceTask(iterator);

		// got one - stick it on the tasklist
		if (currentObservationSequence != null)
			taskList.addTask(currentObservationSequence);

		return taskList;
	}

}
