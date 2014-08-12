/**
 * 
 */
package ngat.rcs.ers;

import ngat.ems.CloudStatus;
import ngat.ems.MeteorologyStatus;
import ngat.ems.WmsStatus;
import ngat.ems.DustStatus;

/**
 * Receives updates from a status provider, extracts the relevant data and
 * updates the attached filter.
 * 
 * @author eng
 * 
 */
public class MeteoFilterAdapter implements FilterAdapter {

	private String catName;

	private String itemName;

	private Filter filter;

	/**
	 * @param catName
	 * @param itemName
	 */
	public MeteoFilterAdapter(String catName, String itemName, Filter filter) {
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

	/**
	 * Extracts the relevant status entry for which this adapter has been setup.
	 * 
	 * @param status
	 * @return The required status entry or NULL if not available.
	 */
	public Number getStatusItem(MeteorologyStatus status) {

		if (catName.equals("WMS")) {
			return extractWmsStatus(status);
		} else if (catName.equals("CLOUD")) {
			return extractCloudStatus(status);
		} else if (catName.equals("DUST")) {
			return extractDustStatus(status);
		}
		return null;
	}

	private Number extractWmsStatus(MeteorologyStatus status) {
		WmsStatus wms = (WmsStatus) status;

		if (itemName.equals("dewpoint"))
			return new Double(wms.getDewPointTemperature());
		else if (itemName.equals("temperature"))
			return new Double(wms.getExtTemperature());
		else if (itemName.equals("humidity"))
			return new Double(wms.getHumidity());
		else if (itemName.equals("light"))
			return new Double(wms.getLightLevel());
		else if (itemName.equals("moisture"))
			return new Double(wms.getMoistureFraction());
		else if (itemName.equals("pressure"))
			return new Double(wms.getPressure());
		else if (itemName.equals("wind.drin"))
			return new Double(wms.getWindDirn());
		else if (itemName.equals("wind.speed"))
			return new Double(wms.getWindSpeed());
		else if (itemName.equals("rain"))
			return new Integer(wms.getRainState());
		else if (itemName.equals("state"))
			return new Integer(wms.getWmsStatus());
		return null;

	}

	private Number extractCloudStatus(MeteorologyStatus status) {

		CloudStatus cloud = (CloudStatus) status;

		if (itemName.equals("skyamb"))
			return new Double(cloud.getSkyMinusAmb());
		return null;
	}

	private Number extractDustStatus(MeteorologyStatus status) {

		DustStatus dust = (DustStatus) status;
		
		if (itemName.equals("dust"))
			return new Double(dust.getDust());
		return null;
	}

}
