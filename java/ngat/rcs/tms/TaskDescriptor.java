/**
 * 
 */
package ngat.rcs.tms;

import java.io.Serializable;

/**
 * @author eng
 *
 */
public class TaskDescriptor implements Serializable {

	private long id;
	
	private String name;
	
	private String typeName;
	
	private String description;

    private boolean isManager;

    private boolean isModal;

	/** A unique UID for each task.*/
	private static long uid = 0;
	
	/** Create a new TaskDecriptor and assign an id.*/
	public TaskDescriptor() {	
		id = ++uid;
	}

	/**
	 * @param name
	 * @param typeName
	 */
	public TaskDescriptor(String name, String typeName) {
		this();
		this.name = name;
		this.typeName = typeName;
	}

	/**
	 * @param name
	 * @param typeName
	 * @param description
	 */
	public TaskDescriptor(String name, String typeName, String description) {
		this(name, typeName);
		this.description = description;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}

	/**
	 * @return the uid
	 */
	public long getUid() {
		return id;
	}

	/**
	 * @param uid the uid to set
	 */
	public void setUid(long id) {
		this.id = id;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

    /**
     * @return true if manager
     */
    public boolean isManager() {
	return isManager;
    }

    /**
     * @param isManager true if manager
     */
    public void setIsManager(boolean isManager) {
	this.isManager = isManager;
    }

    /**
     * @return true if modal
     */
    public boolean isModal() {
        return isModal;
    }

    /**
     * @param isModal true if manager
     */
    public void setIsModal(boolean isModal) {
        this.isModal = isModal;
    }



	@Override
	public String toString() {
	    return (isModal ? "Modal-":"")+"Task: ("+
		(isManager ? "M":"E")+")"+
		name+
		" ["+uid+"] -> "+
		typeName+
		(description != null ? "("+description+")":"");
	}
    
}
