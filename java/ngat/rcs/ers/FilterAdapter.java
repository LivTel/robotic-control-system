/**
 * 
 */
package ngat.rcs.ers;

/** Adapter: Takes a status object and returns the value associated with a specific category-key and item-key.
 * @author eng
 *
 */
public interface FilterAdapter {
	
	public String getCatName();
	
	public String getItemName();
	
}
