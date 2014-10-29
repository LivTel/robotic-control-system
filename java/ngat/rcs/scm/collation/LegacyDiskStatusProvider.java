package ngat.rcs.scm.collation;

import java.rmi.RemoteException;

import ngat.ims.DiskStatus;
import ngat.ims.DiskStatusUpdateListener;
import ngat.util.StatusCategory;
import ngat.util.StatusProvider;

/**
 * This class is a translation layer between the new format ngat.ims.DiskStatusProvider
 * and the legacy ngat.util.StatusProvider mechanism of status propagation within the rcs.
 * @author Chris Mottram
 * @see ngat.util.StatusProvider
 * @see ngat.ims.DiskStatusProvider
 */
public class LegacyDiskStatusProvider implements StatusProvider, DiskStatusUpdateListener
{
	/**
	 * An instance of MappedStatusCategory to store the disk data.
	 * @see ngat.rcs.scm.collation.MappedStatusCategory
	 */
	private MappedStatusCategory diskStatus;
	
	/**
	 * Default constructor. Creates the diskStatus MappedStatusCategory. The following keywords are added:
	 * <ul>
	 * <li>free.space.occ
	 * <li>disk.usage.occ
	 * <li>free.space.nas2
	 * <li>disk.usage.nas2
	 * <li>free.space.rise
	 * <li>disk.usage.rise
	 * <li>free.space.ringo3-1
	 * <li>disk.usage.ringo3-1
	 * <li>free.space.ringo3-2
	 * <li>disk.usage.ringo3-2
	 * <li>free.space.autoguider
	 * <li>disk.usage.autoguider
	 * </ul>
	 * @see #diskStatus
	 */
	public LegacyDiskStatusProvider() 
	{
		diskStatus = new MappedStatusCategory();
		diskStatus.addKeyword("free.space.occ", MappedStatusCategory.DOUBLE_DATA, "Free space on occ:/", "kilobytes");
		diskStatus.addKeyword("disk.usage.occ", MappedStatusCategory.DOUBLE_DATA, "Disk usage on occ:/", "%");
		diskStatus.addKeyword("free.space.nas2", MappedStatusCategory.DOUBLE_DATA, "Free space on ltnas2:/mnt/archive2", "kilobytes");
		diskStatus.addKeyword("disk.usage.nas2", MappedStatusCategory.DOUBLE_DATA, "Disk usage on ltnas2:/mnt/archive2", "%");
		diskStatus.addKeyword("free.space.rise", MappedStatusCategory.DOUBLE_DATA, "Free space on rise", "kilobytes");
		diskStatus.addKeyword("disk.usage.rise", MappedStatusCategory.DOUBLE_DATA, "Disk usage on rise", "%");
		diskStatus.addKeyword("free.space.ringo3-1", MappedStatusCategory.DOUBLE_DATA, "Free space on ringo3-1", "kilobytes");
		diskStatus.addKeyword("disk.usage.ringo3-1", MappedStatusCategory.DOUBLE_DATA, "Disk usage on ringo3-1", "%");
		diskStatus.addKeyword("free.space.ringo3-2", MappedStatusCategory.DOUBLE_DATA, "Free space on ringo3-2", "kilobytes");
		diskStatus.addKeyword("disk.usage.ringo3-2", MappedStatusCategory.DOUBLE_DATA, "Disk usage on ringo3-2", "%");
		diskStatus.addKeyword("free.space.autoguider", MappedStatusCategory.DOUBLE_DATA, "Free space on autoguider", "kilobytes");
		diskStatus.addKeyword("disk.usage.autoguider", MappedStatusCategory.DOUBLE_DATA, "Disk usage on autoguider", "%");
	}
	
	/**
	 * Return the diskStatus as the current status set.
	 * @see ngat.util.StatusProvider#getStatus()
	 * @see #diskStatus
	 */
	public StatusCategory getStatus() 
	{
		return diskStatus;
	}
	
	/**
	 * Receive an update from the BasicDiskStatusProvider. Based on the machine name and
	 * disk name update the relevant keywords in diskStatus.
	 * @see #diskStatus
	 */
	public void diskStatusUpdate(DiskStatus status) throws RemoteException
	{
		diskStatus.setTimeStamp(status.getStatusTimeStamp());
		if((status.getMachineName().equals("occ"))&&(status.getDiskName().equals("/")))
		{
			diskStatus.addData("free.space.occ",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.occ",status.getDiskPercentUsed());
		}
		else if((status.getMachineName().equals("ltnas2"))&&(status.getDiskName().equals("/mnt/archive2")))
		{
			diskStatus.addData("free.space.nas2",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.nas2",status.getDiskPercentUsed());
		}
		else if((status.getMachineName().equals("rise"))&&(status.getDiskName().equals("/mnt/rise-image")))
		{
			diskStatus.addData("free.space.rise",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.rise",status.getDiskPercentUsed());
		}
		else if((status.getMachineName().equals("ringo3-1"))&&(status.getDiskName().equals("/mnt/ringo3-1-image")))
		{
			diskStatus.addData("free.space.ringo3-1",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.ringo3-1",status.getDiskPercentUsed());
		}
		else if((status.getMachineName().equals("ringo3-2"))&&(status.getDiskName().equals("/mnt/ringo3-2-image")))
		{
			diskStatus.addData("free.space.ringo3-2",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.ringo3-2",status.getDiskPercentUsed());
		}
		else if((status.getMachineName().equals("autoguider1"))&&(status.getDiskName().equals("/mnt/autoguider-image")))
		{
			diskStatus.addData("free.space.autoguider",(double)(status.getDiskFreeSpace()));
			diskStatus.addData("disk.usage.autoguider",status.getDiskPercentUsed());
		}
	}
}
