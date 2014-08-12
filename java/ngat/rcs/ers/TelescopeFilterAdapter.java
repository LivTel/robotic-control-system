/**
 * 
 */
package ngat.rcs.ers;

import ngat.tcm.AutoguiderStatus;
import ngat.tcm.AuxilliaryMechanismStatus;
import ngat.tcm.FocusStatus;
import ngat.tcm.PrimaryAxisStatus;
import ngat.tcm.RotatorAxisStatus;
import ngat.tcm.TelescopeControlSystemStatus;
import ngat.tcm.TelescopeEnvironmentStatus;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeNetworkStatus;

/**
 * Receives updates from a status provider, extracts the relevant data and
 * updates the attached filter.
 * 
 * @author eng
 * 
 */
public class TelescopeFilterAdapter implements FilterAdapter {

	private String catName;

	private String itemName;

	private Filter filter;
	
	/**
	 * @param catName
	 * @param itemName
	 */
	public TelescopeFilterAdapter(String catName, String itemName, Filter filter) {
		super();
		this.catName = catName;
		this.itemName = itemName;
		this.filter = filter;
	}
	
	
	
	public String getCatName() {
		return catName;
	}



	public String getItemName() {
		return itemName;
	}


	/** @return The attached filter. */
	public Filter getFilter() {
		return filter;
	}
	
	/** Extracts the relevant status entry for which this adapter has been setup.
	 * @param status
	 * @return The required status entry or NULL if not available.
	 */
	public Number getStatusItem(TelescopeStatus status) {

		if (catName.equals("AZM")) {
			return extractAxisStatus(status);
		} else if (catName.equals("ALT")) {
			return extractAxisStatus(status);
		} else if (catName.equals("ROT")) {
			return extractRotStatus(status);
		} else if (catName.equals("SMF")) {
			return extractFocusStatus(status);
		} else if (catName.equals("AGF")) {
			return extractFocusStatus(status);
		} else if (catName.equals("AFI")) {
			return extractAuxStatus(status);
		} else if (catName.equals("AMD")) {
			return extractAuxStatus(status);
		} else if (catName.equals("AGG")) {
			return extractAgStatus(status);
		} else if (catName.equals("ENV")) {
			return extractEnvStatus(status);
		} else if (catName.equals("TCS")) {
			return extractTcsStatus(status);
		} else if (catName.equals("PMC")) {
		    return extractAuxStatus(status);
		} else if (catName.equals("EN1")) {
			return extractAuxStatus(status);
		} else if (catName.equals("EN2")) {
			return extractAuxStatus(status);
		} else if (catName.equals("CIL_NET")) {
		    return extractNetworkStatus(status);
		}
		return null;
	}

    private Number extractNetworkStatus(TelescopeStatus status) {
	TelescopeNetworkStatus network = (TelescopeNetworkStatus)status;
	if (itemName.equals("network.state"))
	    return new Integer(network.getTelescopeNetworkState());
	return null;
    }

	private Number extractAxisStatus(TelescopeStatus status) {
		PrimaryAxisStatus axis = (PrimaryAxisStatus) status;
		if (itemName.equals("axis.position"))
			return new Double(axis.getCurrentPosition());
		else if (itemName.equals("axis.demand"))
			return new Double(axis.getDemandPosition());
		else if (itemName.equals("axis.state"))
			return new Integer(axis.getMechanismState());
		return null;
	}

	private Number extractAuxStatus(TelescopeStatus status) {
		AuxilliaryMechanismStatus aux = (AuxilliaryMechanismStatus) status;
		if (itemName.equals("axis.position"))
			return new Integer(aux.getCurrentPosition());
		else if (itemName.equals("axis.demand"))
			return new Integer(aux.getDemandPosition());
		else if (itemName.equals("axis.state"))
			return new Integer(aux.getMechanismState());
		return null;
	}

	private Number extractRotStatus(TelescopeStatus status) {
		RotatorAxisStatus rot = (RotatorAxisStatus) status;
		if (itemName.equals("rotator.skyangle"))
			return new Double(rot.getSkyAngle());
		else if (itemName.equals("rotator.mode"))
			return new Integer(rot.getRotatorMode());
		else
			return extractAxisStatus(status);
	}

	private Number extractFocusStatus(TelescopeStatus status) {
		FocusStatus focus = (FocusStatus) status;
		if (itemName.equals("focus.offset"))
			return new Double(focus.getFocusOffset());
		else
			return extractAxisStatus(status);
	}

	private Number extractAgStatus(TelescopeStatus status) {
		AutoguiderStatus ag = (AutoguiderStatus) status;
		if (itemName.equals("autoguider.temperature"))
			return new Double(ag.getAutoguiderTemperature());
		else if (itemName.equals("autoguider.fwhm"))
			return new Double(ag.getGuideFwhm());
		else if (itemName.equals("autoguider.mode"))
			return new Integer(ag.getGuideMode());
		else if (itemName.equals("autoguider.magnitude"))
			return new Double(ag.getGuideStarMagnitude());
		else if (itemName.equals("autoguider.state"))
			return new Integer(ag.getGuideState());
		else if (itemName.equals("autoguider.software"))
			return new Integer(ag.getSoftwareState());
		return null;
	}

	private Number extractEnvStatus(TelescopeStatus status) {
		TelescopeEnvironmentStatus env = (TelescopeEnvironmentStatus) status;
		if (itemName.equals("env.agbox.temperature"))
			return new Double(env.getAgBoxTemperature());
		else if (itemName.equals("env.oil.temperature"))
			return new Double(env.getOilTemperature());
		else if (itemName.equals("env.primary.mirror.temperature"))
			return new Double(env.getPrimaryMirrorTemperature());
		else if (itemName.equals("env.secondary.mirror.temperature"))
			return new Double(env.getSecondaryMirrorTemperature());
		else if (itemName.equals("env.truss.temperature"))
			return new Double(env.getTrussTemperature());
		return null;
	}

	private Number extractTcsStatus(TelescopeStatus status) {
		TelescopeControlSystemStatus tcs = (TelescopeControlSystemStatus) status;
		if (itemName.equals("tcs.control.state"))
			return new Integer(tcs.getTelescopeControlSystemState());
		else if (itemName.equals("tcs.engineering.state"))
			return new Integer(tcs.getTelescopeEngineeringControlState());
		else if (itemName.equals("tcs.network.state"))
			return new Integer(tcs.getTelescopeNetworkControlState());
		else if (itemName.equals("tcs.system.state"))
			return new Integer(tcs.getTelescopeSystemState());
		return null;
	}




}
