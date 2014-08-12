/**
 * 
 */
package ngat.rcs.ers;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * @author eng
 *
 */
public interface ReactiveSystemStructureProvider extends Remote {

	/**
	 * @return A list of filters.
	 * @throws RemoteException
	 */
	public List<Filter> listFilters() throws RemoteException;
	
	/**
	 * @return A list of criteria.
	 * @throws RemoteException
	 */
	public List<Criterion> listCriteria() throws RemoteException;
	
	/**
	 * @return A list of Rules.
	 * @throws RemoteException
	 */
	public List<Rule> listRules() throws RemoteException;
	
	/**
	 * @return A mapping between filters and criteria.
	 * @throws RemoteException
	 */
	public Map<String, List<Criterion>> getFilterCriterionMapping() throws RemoteException;
	
	/**
	 * @return A mapping between criteria and rules.
	 * @throws RemoteException
	 */
	public Map<String, Rule> getCriterionRuleMapping() throws RemoteException;

	
	/**
	 * @return A mapping between rules and rules.
	 * @throws RemoteException
	 */
	public Map<String, Rule> getRuleRuleMapping() throws RemoteException; 


}
