/**
 * 
 */
package ngat.rcs.ers.test;

import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.Vector;

import javax.swing.text.DateFormatter;

import org.jdom.Element;

import ngat.astrometry.BasicSite;
import ngat.astrometry.ISite;
import ngat.astrometry.SolarCycleStatus;
import ngat.ems.MeteorologyStatus;
import ngat.ems.MeteorologyStatusProvider;
import ngat.ems.MeteorologyStatusUpdateListener;
import ngat.net.telemetry.StatusCategory;
import ngat.rcs.ers.ActualStateFilter;
import ngat.rcs.ers.AverageFilter;
import ngat.rcs.ers.Criterion;
import ngat.rcs.ers.DiscreteEQCriterion;
import ngat.rcs.ers.DiscreteOneOfCriterion;
import ngat.rcs.ers.Filter;
import ngat.rcs.ers.FractionTimeRule;
import ngat.rcs.ers.GTCriterion;
import ngat.rcs.ers.LTCriterion;
import ngat.rcs.ers.MeteoFilterAdapter;
import ngat.rcs.ers.PowerCycleFilter;
import ngat.rcs.ers.PowerCycleFilterAdapter;
import ngat.rcs.ers.ReactiveSystemMonitor;
import ngat.rcs.ers.ReactiveSystemStructureProvider;
import ngat.rcs.ers.ReactiveSystemUpdateListener;
import ngat.rcs.ers.Rule;
import ngat.rcs.ers.SolarCycleFilter;
import ngat.rcs.ers.SolarCycleFilterAdapter;
import ngat.rcs.ers.TelescopeFilterAdapter;
import ngat.rcs.ers.TimeRule;
import ngat.rcs.newstatemodel.PowerCycleStatus;
import ngat.tcm.AutoguiderActiveStatus;
import ngat.tcm.TelescopeStatus;
import ngat.tcm.TelescopeNetworkStatus;
import ngat.tcm.TelescopeStatusProvider;
import ngat.tcm.TelescopeStatusUpdateListener;
import ngat.util.ControlThread;
import ngat.util.XmlConfigurable;
import ngat.util.XmlConfigurator;
import ngat.util.ConfigurationProperties;
import ngat.util.CommandTokenizer;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * Typical setup:
 * 
 *  // Create a BasicReactiveSystem...
 *  BasicReactiveSystem brs = new BasicReactiveSystem(telescopeSite);
 *  
 *  // Configure its rulebase...
 *  XmlConfigurator.use(xmlRulebaseFile).configure(brs);
 *  
 *  // Link it to telescope and meteo providers ...
 *  telescope.addTelescopeStatusUpdateListener(brs);
 *  meteo.addMeteoStatusUpdateListener(brs)
 * 
 *  // Make ourself known..
 *  Naming.bind("ReactiveSystemMonitor", brs);
 *  
 *  // Link any downstream service which will use our telemetry or do things
 *  // based on our received rule triggers.
 *  // (OR) they might link to us themselves via registry lookup.
 *  trs.addReactiveSystemUpdateListener(someImportantService)
 *  
 *  // Start the cache reader thread so updates will be generated, otherwise they just sit in the cache
 *  // forever and no-one ever knows about them...
 *  brs.startCacheReader();
 *
 * @author eng
 * 
 */
public class BasicReactiveSystem extends UnicastRemoteObject implements ReactiveSystemMonitor,
	ReactiveSystemStructureProvider, TelescopeStatusUpdateListener, MeteorologyStatusUpdateListener, XmlConfigurable {

	private ISite site;

	private Map<String, Filter> filters;
	private Map<String, Criterion> criteria;
	private Map<String, Rule> rules;
	private Map<String, List<TelescopeFilterAdapter>> telescopeCatToFilter;
	private Map<String, List<MeteoFilterAdapter>> meteoCatToFilter;
	private Map<String, List<SolarCycleFilterAdapter>> solarCatToFilter;
	private Map<String, List<PowerCycleFilterAdapter>> powerCatToFilter;
	private Map<String, List<Criterion>> filterToCriterion;
	private Map<String, Rule> criterionToRule;
	private Map<String, Rule> ruleToRule; // forward mapping up the rule hierarchy

	private List<ReactiveSystemUpdateListener> listeners;

	/** Holds a time-ordered list of status objects received by various feeds we are registered for.*/
	private List<StatusCategory> cache;

	/** Cache reader. Processes the time-ordered list of status objects.*/
	private CacheReader reader;
	
	/** True if the cache-reader is running.*/
	private volatile boolean cacheReaderRunning = false;
	
	SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
	
	/** Logger. */
	private LogGenerator slogger;

	/**
     * 
     */
	public BasicReactiveSystem(ISite site) throws RemoteException {
		this.site = site;
		
		filters = new HashMap<String, Filter>();
		criteria = new HashMap<String, Criterion>();
		rules = new HashMap<String, Rule>();
		telescopeCatToFilter = new HashMap<String, List<TelescopeFilterAdapter>>();
		meteoCatToFilter = new HashMap<String, List<MeteoFilterAdapter>>();
		solarCatToFilter = new HashMap<String, List<SolarCycleFilterAdapter>>();
		powerCatToFilter = new HashMap<String, List<PowerCycleFilterAdapter>>();
		filterToCriterion = new HashMap<String, List<Criterion>>();		
		criterionToRule = new HashMap<String, Rule>();
		ruleToRule = new HashMap<String, Rule>();
		cache = new Vector<StatusCategory>();

		listeners = new Vector<ReactiveSystemUpdateListener>();


		Logger alogger = LogManager.getLogger("ERS"); 
		slogger = alogger.generate().system("RCS")
					.subSystem("Reactive")
					.srcCompClass(this.getClass().getSimpleName())
					.srcCompId("TRS");
		
	}

	/** Start the cache reader. We should check it isnt already running first.*/
	public void startCacheReader() {
		if (reader == null)
			reader = new CacheReader();
		if (cacheReaderRunning)
			return;		
		reader.start();
		cacheReaderRunning = true;
	}

	private void addTelescopeMapping(String cat, TelescopeFilterAdapter filter) {
		List<TelescopeFilterAdapter> catlist = telescopeCatToFilter.get(cat);
		if (catlist == null) {
			catlist = new Vector<TelescopeFilterAdapter>();
			telescopeCatToFilter.put(cat, catlist);
		}
		catlist.add(filter);
	}

	private void addMeteoMapping(String cat, MeteoFilterAdapter filter) {
		List<MeteoFilterAdapter> catlist = meteoCatToFilter.get(cat);
		if (catlist == null) {
			catlist = new Vector<MeteoFilterAdapter>();
			meteoCatToFilter.put(cat, catlist);
		}
		catlist.add(filter);
	}
	
	private void addSolarCycleMapping(String cat, SolarCycleFilterAdapter filter) {
		List<SolarCycleFilterAdapter> catlist = solarCatToFilter.get(cat);
		if (catlist == null) {
			catlist = new Vector<SolarCycleFilterAdapter>();
			solarCatToFilter.put(cat, catlist);
		}
		catlist.add(filter);
	}
	
	private void addPowerCycleMapping(String cat, PowerCycleFilterAdapter filter) {
		List<PowerCycleFilterAdapter> catlist = powerCatToFilter.get(cat);
		if (catlist == null) {
			catlist = new Vector<PowerCycleFilterAdapter>();
			powerCatToFilter.put(cat, catlist);
		}
		catlist.add(filter);
	}

	private void addFilterCriterionMapping(String name, Criterion criterion) {
		List<Criterion> clist = filterToCriterion.get(name);
		if (clist == null) {
			clist = new Vector<Criterion>();
			filterToCriterion.put(name, clist);
		}
		clist.add(criterion);
	}

	/**
	 * Configure this test system.
	 * 
	 * @see ngat.util.XmlConfigurable#configure(org.jdom.Element)
	 */
	public void configure(Element node) throws Exception {

		List efilters = node.getChildren("filter");
		Iterator iff = efilters.iterator();
		while (iff.hasNext()) {
			Element fnode = (Element) iff.next();

			// process filter
			Filter f = configureFilter(fnode);			
			filters.put(f.getFilterName(), f);
			slogger.create().info().level(1).extractCallInfo().msg("Add filter: " + f).send();
		}

		List erules = node.getChildren("rule");
		Iterator ir = erules.iterator();
		while (ir.hasNext()) {
			Element rnode = (Element) ir.next();

			// process rule
			// some of these include crit defs, others are joins
			Rule r = configureRule(rnode);
			rules.put(r.getRuleName(), r);
			slogger.create().info().level(1).extractCallInfo().msg("Add rule: " + r).send();
		}
	}

	private Filter configureFilter(Element node) throws Exception {
		String fname = node.getAttributeValue("name").trim();
		String type = node.getAttributeValue("class").trim();

		Filter filter = null;
		String source = node.getChildTextTrim("source");
		String cat = node.getChildTextTrim("category");
		String item = node.getChildTextTrim("item");

		
		if (type.equalsIgnoreCase("AVERAGE")) {
			Element pnode = node.getChild("period");
			long period = getPeriod(pnode, null);
			filter = new AverageFilter(fname, period);
			((AverageFilter)filter).setSourceDescription(source+"/"+cat+"/"+item);
		} else if (type.equalsIgnoreCase("ACTUAL")) {
			filter = new ActualStateFilter(fname);
			((ActualStateFilter)filter).setSourceDescription(source+"/"+cat+"/"+item);
		} else if (type.equalsIgnoreCase("SOLAR")) {
			filter = new SolarCycleFilter(fname, site);
			Element pnode = node.getChild("preSunset");
			long preSunset = getPeriod(pnode, null);
			Element qnode = node.getChild("preSunrise");
			long preSunrise = getPeriod(qnode, null);
			((SolarCycleFilter)filter).setPreSunriseOffset(preSunrise);
			((SolarCycleFilter)filter).setPreSunsetOffset(preSunset);
			((SolarCycleFilter)filter).setSourceDescription(source+"/"+cat+"/"+item);
		} else if (type.equalsIgnoreCase("POWER")) {
			filter = new PowerCycleFilter(fname);
			Element tnode = node.getChild("rebootOffset");
			long rebootOffset = getPeriod(tnode, null);
			((PowerCycleFilter)filter).setRebootOffset(rebootOffset);
			((PowerCycleFilter)filter).setSourceDescription(source+"/"+cat+"/"+item);
		}
		
		// sort out the adapter
		
		
		if (source.equalsIgnoreCase("METEO")) {
			
			// METEO WMS humidity
			
			MeteoFilterAdapter mf = new MeteoFilterAdapter(cat, item, filter);
			addMeteoMapping(cat, mf);			
			slogger.create().info().level(1).extractCallInfo().msg("Link: MET." + cat + "." + item + " -> " + fname).send();
		} else if (source.equalsIgnoreCase("TEL")) {
			
			// TEL AZM axis.status
			
			TelescopeFilterAdapter tf = new TelescopeFilterAdapter(cat, item, filter);
			addTelescopeMapping(cat, tf);			
			slogger.create().info().level(1).extractCallInfo().msg("Link: TEL." + cat + "." + item + " -> " + fname).send();
			
		} else if (source.equalsIgnoreCase("INSTR")) {

		} else if (source.equalsIgnoreCase("ASTRO")) {
			
			// ASTRO SOLAR cycle.state
			
			// NOTE currently this is the only mapping available
			SolarCycleFilterAdapter saf = new SolarCycleFilterAdapter("ASTRO", "SOL", filter);
			addSolarCycleMapping("SOL", saf);
		
		} else if (source.equalsIgnoreCase("POWER")) {
			
			// POWER PWR power.state
			
			// NOTE currently this is the only mapping available
			PowerCycleFilterAdapter saf = new PowerCycleFilterAdapter("POWER", "PWR", filter);
			addPowerCycleMapping("PWR", saf);
		}
		
	
		return filter;

	}

	private Rule configureRule(Element node) throws Exception {

		// rules can be: conjunct, disjunct, timed, fraction etc
		String type = node.getAttributeValue("class"); // trim?
		if (type.equalsIgnoreCase("conjunct"))
			return configureConjunct(node);
		else if (type.equalsIgnoreCase("disjunct"))
			return configureDisjunct(node);
		else if (type.equalsIgnoreCase("fraction"))
			return configureFractionRule(node);
		else if (type.equalsIgnoreCase("fuzzy"))
			return null;
		// return configureFuzzy(node);

		// handle the criterion based rules hereindex

		if (type.equalsIgnoreCase("timed"))
			return configureTimedRule(node);

		return null;
	}

	private Rule configureConjunct(Element node) throws Exception {
		String cname = node.getAttributeValue("name");
		slogger.create().info().level(1).extractCallInfo().msg("Create Conjunct: " + cname).send();
		
		Conjunct con = new Conjunct(cname);
		rules.put(con.getRuleName(), con);
		List erules = node.getChildren("rule");
		Iterator ir = erules.iterator();
		while (ir.hasNext()) {
			Element rnode = (Element) ir.next();
			Rule subrule = configureRule(rnode);
	
			slogger.create().info().level(1).extractCallInfo()
				.msg("Adding Subrule forward mapping: " + subrule.getRuleName() + " and-> " + con.getRuleName()).send();
			ruleToRule.put(subrule.getRuleName(), con);
			con.addSubrule(subrule);
		}
		return con;
	}

	private Rule configureDisjunct(Element node) throws Exception {
		String dname = node.getAttributeValue("name");
	
		slogger.create().info().level(1).extractCallInfo().msg("Create Disjunct: " + dname).send();
		Disjunct dis = new Disjunct(dname);
		rules.put(dis.getRuleName(), dis);
		List erules = node.getChildren("rule");
		Iterator ir = erules.iterator();
		while (ir.hasNext()) {
			Element rnode = (Element) ir.next();
			Rule subrule = configureRule(rnode);
			
			slogger.create().info().level(1).extractCallInfo()
				.msg("Adding Subrule: " + subrule.getRuleName() + " or-> " + dis.getRuleName()).send();
			ruleToRule.put(subrule.getRuleName(), dis);
			dis.addSubrule(subrule);
		}
		return dis;
	}

	private Rule configureTimedRule(Element node) throws Exception {

		Element cnode = node.getChild("criterion");
		Criterion criterion = configureCriterion(cnode);
		criteria.put(criterion.getCriterionName(), criterion);

		String rname = node.getAttributeValue("name");

		Element tnode = node.getChild("period");
		long period = getPeriod(tnode, null); // extract period from node's content
		
		TimeRule rule = new TimeRule(rname, period);
		rules.put(rule.getRuleName(), rule);
		criterionToRule.put(criterion.getCriterionName(), rule);
	
		slogger.create().info().level(1).extractCallInfo()
			.msg("Link: " + criterion.getCriterionName() + " with " + rule.getRuleName()).send();
		return rule;
	}
	
	private Rule configureFractionRule(Element node) throws Exception {
		
		Element cnode = node.getChild("criterion");
		Criterion criterion = configureCriterion(cnode);
		criteria.put(criterion.getCriterionName(), criterion);

		String rname = node.getAttributeValue("name");

		Element tnode = node.getChild("period");
		long period = getPeriod(tnode, null); // extract period from node's content
		
		Element fnode = node.getChild("fraction");
		double stabilityFraction = getDouble(fnode, null);
		
		FractionTimeRule rule = new FractionTimeRule(rname, period, stabilityFraction);
		rules.put(rule.getRuleName(), rule);
		criterionToRule.put(criterion.getCriterionName(), rule);
	
		slogger.create().info().level(1).extractCallInfo()
			.msg("Link: " + criterion.getCriterionName() + " with " + rule.getRuleName()).send();
		return rule;
	}

	private Criterion configureCriterion(Element node) throws Exception {
		String type = node.getAttributeValue("class"); // trim?

		if (type.equalsIgnoreCase("greater")) {
			String cname = node.getAttributeValue("name").trim();
			Element gnode = node.getChild("minimum");
			double minimum = getDouble(gnode, null);
			GTCriterion gtc = new GTCriterion(cname, minimum);
			String fname = node.getChildTextTrim("filter");
			addFilterCriterionMapping(fname, gtc);
			
			slogger.create().info().level(1).extractCallInfo()
				.msg("Link: " + fname + " as input to " + cname).send();
			return gtc;

		} else if (type.equalsIgnoreCase("less")) {
			String cname = node.getAttributeValue("name").trim();
			Element gnode = node.getChild("maximum");
			double maximum = getDouble(gnode, null);
			LTCriterion ltc = new LTCriterion(cname, maximum);
			String fname = node.getChildTextTrim("filter");
			addFilterCriterionMapping(fname, ltc);
		
			slogger.create().info().level(1).extractCallInfo()
				.msg("Link: " + fname + " as input to " + cname).send();
			return ltc;

		} else if (type.equalsIgnoreCase("deq")) {
			String cname = node.getAttributeValue("name").trim();
			Element gnode = node.getChild("equals");
			int test = getInt(gnode, null);
			DiscreteEQCriterion deq = new DiscreteEQCriterion(cname, test);
			String fname = node.getChildTextTrim("filter");
			addFilterCriterionMapping(fname, deq);
		
			slogger.create().info().level(1).extractCallInfo()
				.msg("Link: " + fname + " as input to " + cname).send();
			return deq;

		} else if (type.equalsIgnoreCase("one")) {
			String cname = node.getAttributeValue("name").trim();

			List evalues = node.getChildren("any");
			int[] values = new int[evalues.size()];
			for (int iv = 0; iv < evalues.size(); iv++) {
				Element vnode = (Element) evalues.get(iv);
				values[iv] = getInt(vnode, null);
			}
			DiscreteOneOfCriterion dof = new DiscreteOneOfCriterion(cname, values);
			String fname = node.getChildTextTrim("filter");
			addFilterCriterionMapping(fname, dof);
		
			slogger.create().info().level(1).extractCallInfo()
				.msg("Link: " + fname + " as input to " + cname).send();
			return dof;
		}
		// TODO no others yet but they will follow !!!
		return null;
	}

	/**
	 * Extract a key value as a double.
	 * 
	 * @param node
	 *            The node to process.
	 * @param key
	 *            The key to locate.
	 * @return The key value as a double.
	 * @throws Exception
	 *             If no key is specified.
	 */
	private double getDouble(Element node, String key) throws Exception {
		// System.err.println("getdouble: n="+node+", k="+key);
		String strValue = null;
		if (key == null)
			strValue = node.getTextTrim();
		else
			strValue = node.getAttributeValue(key).trim();

		if (strValue == null || strValue.equals(""))
			throw new IllegalArgumentException("No value associated with: " + key);

		return Double.parseDouble(strValue);

	}
	
	/**
	 * Extract a key value as a time of day (ie millis since midnight of date).
	 * 
	 * @param node
	 *            The node to process.
	 * @param key
	 *            The key to locate.
	 * @param fmt
	 * 			  The date format to parse.
	 * @return The key value as a double.
	 * @throws Exception
	 *             If no key is specified.
	 */
	private long getTimeOfDay(Element node, String key, String fmt) throws Exception {
	
		String strValue = null;
		if (key == null)
			strValue = node.getTextTrim();
		else
			strValue = node.getAttributeValue(key).trim();

		if (strValue == null || strValue.equals(""))
			throw new IllegalArgumentException("No value associated with: " + key);

		SimpleDateFormat sdf = new SimpleDateFormat(fmt);
		sdf.setTimeZone(UTC);
		
		return sdf.parse(strValue).getTime();
	}

	/**
	 * Extract a key value as an int.
	 * 
	 * @param node
	 *            The node to process.
	 * @param key
	 *            The key to locate.
	 * @return The key value as an int.
	 * @throws Exception
	 *             If no key is specified.
	 */
	private int getInt(Element node, String key) throws Exception {
		// System.err.println("getdouble: n="+node+", k="+key);
		String strValue = null;
		if (key == null)
			strValue = node.getTextTrim();
		else
			strValue = node.getAttributeValue(key).trim();

		if (strValue == null || strValue.equals(""))
			throw new IllegalArgumentException("No value associated with: " + key);

		return Integer.parseInt(strValue);

	}

	/**
	 * Extract a period/time value from a node.
	 * 
	 * @param node
	 *            The node to parse.
	 * @param key
	 *            A key to look for as an attribute. If null use the node's
	 *            content.
	 * @return A time (millis).
	 * @throws Exception.
	 */
	private long getPeriod(Element node, String key) throws Exception {

		String strUnit = node.getAttributeValue("unit");

		double mult = 1.0;
		if (strUnit.equalsIgnoreCase("ms") || strUnit.equalsIgnoreCase("milli") || strUnit.equalsIgnoreCase("millis")
				|| strUnit.equalsIgnoreCase("msec") || strUnit.equalsIgnoreCase("msecs")
				|| strUnit.equalsIgnoreCase("millisec") || strUnit.equalsIgnoreCase("millisecs"))
			mult = 1.0;
		else if (strUnit.equalsIgnoreCase("s") || strUnit.equalsIgnoreCase("sec") || strUnit.equalsIgnoreCase("secs"))
			mult = 1000.0;
		else if (strUnit.equalsIgnoreCase("m") || strUnit.equalsIgnoreCase("min") || strUnit.equalsIgnoreCase("mins"))
			mult = 60000.0;
		else if (strUnit.equalsIgnoreCase("h") || strUnit.equalsIgnoreCase("hour") || strUnit.equalsIgnoreCase("hours"))
			mult = 3600000.0;
		else if (strUnit.equalsIgnoreCase("d") || strUnit.equalsIgnoreCase("day") || strUnit.equalsIgnoreCase("days"))
			mult = 86400000.0;
		else if (strUnit.equalsIgnoreCase("sd") || strUnit.equalsIgnoreCase("sidereal"))
			mult = 86164091.0;
		else
			throw new Exception("TRS::getPeriod: Unknown unit: " + strUnit + " in element " + node.getName());

		// Null key means use nodes own contentp2_gen_qsu.eps
		String strTime = null;
		if (key == null)
			strTime = node.getTextTrim();
		else
			strTime = node.getAttributeValue(key).trim();

		double time = Double.parseDouble(strTime);

		return (long) (time * mult);

	}

	/**
	 * @see ngat.rcs.ers.ReactiveSystemMonitor#addReactiveSystemUpdateListener
	 *      (ngat.rcs.ers.ReactiveSystemUpdateListener)
	 */
	public void addReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {
		if (listeners.contains(l))
			return;
		listeners.add(l);	
		slogger.create().info().level(1).extractCallInfo()
			.msg("Adding listener: " + l).send();
	}

	/**
	 * @see ngat.rcs.ers.ReactiveSystemMonitor#removeReactiveSystemUpdateListener
	 *      (ngat.rcs.ers.ReactiveSystemUpdateListener)
	 */
	public void removeReactiveSystemUpdateListener(ReactiveSystemUpdateListener l) throws RemoteException {
		if (!listeners.contains(l))
			return;
		listeners.remove(l);
	}

	private void notifyListenersFilterUpdated(String filterName, long time, Number updateValue, Number filterOutputValue) {
		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();

			try {
				rsul.filterUpdated(filterName, time, updateValue, filterOutputValue);
			} catch (Exception e) {
				e.printStackTrace();			
				slogger.create().info().level(1).extractCallInfo()
					.msg("Removing unresponsive filter update listener: " + rsul).send();
				il.remove();
			}
		}
	}

	private void notifyListenersCriterionUpdated(String critName, long time, boolean critOutputValue) {
		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();

			try {
				rsul.criterionUpdated(critName, time, critOutputValue);
			} catch (Exception e) {
				e.printStackTrace();			
				slogger.create().info().level(1).extractCallInfo()
					.msg("Removing unresponsive criterion update listener: " + rsul).send();
				il.remove();
			}
		}

	}

	private void notifyListenersRuleUpdated(String ruleName, long time, boolean ruleOutputValue) {

		Iterator<ReactiveSystemUpdateListener> il = listeners.iterator();
		while (il.hasNext()) {
			ReactiveSystemUpdateListener rsul = il.next();

			try {
				rsul.ruleUpdated(ruleName, time, ruleOutputValue);
			} catch (Exception e) {
				e.printStackTrace();
				slogger.create().info().level(1).extractCallInfo()
					.msg("Removing unresponsive rule update listener: " + rsul).send();
				il.remove();
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {	
			
			
			BasicReactiveSystem test = new BasicReactiveSystem(new BasicSite("Test", 0.0, 0.0));

			ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
			String xmlFileName = config.getProperty("xml");
			XmlConfigurator.use(new File(xmlFileName)).configure(test);

			// lookup meteorology status provider and link
			String meteoServiceHost = config.getProperty("meteo", "localhost");
			MeteorologyStatusProvider meteo = (MeteorologyStatusProvider) Naming.lookup("rmi://" + meteoServiceHost
					+ "/Meteorology");
		
			System.err.println("TRS::Located Meteo provider: " + meteo);
			meteo.addMeteorologyStatusUpdateListener(test);
			System.err.println("TRS::Bound to meteo provider");
			
			// lookup tcm status provider and link
			String tcmServicehost = config.getProperty("tcm", "localhost");
			TelescopeStatusProvider telescope = (TelescopeStatusProvider) Naming.lookup("rmi://" + tcmServicehost
					+ "/Telescope");
			System.err.println("TRS::Located Telescope provider: " + telescope);
		
			telescope.addTelescopeStatusUpdateListener(test);
			System.err.println("TRS::Bound to tcm provider");

			// start the cache reader, this will start updating the actual rules
			System.err.println("TRS::Starting cache reader...");
		
			test.startCacheReader();

			// ... forever

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void meteorologyStatusUpdate(MeteorologyStatus status) throws RemoteException {
		slogger.create().info().level(3).extractCallInfo().msg("Received meteo update").send();
		cache.add(status);
	}

	public void telescopeNetworkFailure(long time, String arg1) throws RemoteException {
		slogger.create().info().level(3).extractCallInfo().msg("Received cil network status update").send();
		// Add a network OK message but first determine which network
		
		// TODO URGENT we need to determine the source but we dont have this info here, assume CIL
		
		TelescopeNetworkStatus network = new TelescopeNetworkStatus("CIL_NET");
		network.setStatusTimeStamp(time);
		network.setTelescopeNetworkState(TelescopeNetworkStatus.NETWORK_FAIL); // = 2
		cache.add(network);
	}

	public void telescopeStatusUpdate(TelescopeStatus status) throws RemoteException {
		slogger.create().info().level(1).extractCallInfo()
		.msg("Received telescope status update: " + status.getCategoryName())
		.send();
			
		cache.add(status);

		// Add a network OK message at same time but first determine which network
		String networkCategory = "";
		if (status instanceof AutoguiderActiveStatus)
			networkCategory = status.getCategoryName()+"_NET";
		else
			networkCategory = "CIL_NET";
		
		TelescopeNetworkStatus network = new TelescopeNetworkStatus(networkCategory);
		network.setStatusTimeStamp(status.getStatusTimeStamp());
		network.setTelescopeNetworkState(TelescopeNetworkStatus.NETWORK_OKAY); // = 1
		cache.add(network);

	}

	private class CacheReader extends ControlThread {

		/** How often do we update the solar-cycle status.*/
		private static final long SOLAR_CYCLE_UPDATE_INTERVAL = 60*1000L;

		/** How often do we update the power-cycle status.*/
		private static final long POWER_CYCLE_UPDATE_INTERVAL = 10*1000L;

		/** When was the last solar-cycle status update.*/
		private long timeLastSolarStatusUpdate = 0L;
		
		/** When was the last power-cycle status update.*/
		private long timeLastPowerStatusUpdate = 0L;
		
		CacheReader() {
			super("ERS_CACHE_READER", true);
		
		}

		@Override
		protected void initialise() {
			// TODO Auto-generated method stub

		}

		@Override
		protected void mainTask() {
		
			slogger.create().info().level(1).extractCallInfo().msg("Cache size: " + cache.size()).send();
						
			while (cache.size() > 0) {
				
				long now = System.currentTimeMillis();
				
				// Check the solar-cycle status at NOW time, see how long since we last updated
				// We create a solar status object so we can stuff it in the cache and process it later (soon)
				
				if (now - timeLastSolarStatusUpdate > SOLAR_CYCLE_UPDATE_INTERVAL) {
					// Solar-cycle status change at fixed times before sunrise and sunset.
					SolarCycleStatus solarStatus = new SolarCycleStatus(now);
					cache.add(solarStatus);
					timeLastSolarStatusUpdate = now;
					slogger.create().info().level(2).extractCallInfo().msg("Adding solar cycle update: "+solarStatus).send();
				}
				// Check the power-cycle status at NOW time, see how long since we last updated
				// We create a power status object so we can stuff it in the cache and process it later (soon)
				
				if (now - timeLastPowerStatusUpdate > POWER_CYCLE_UPDATE_INTERVAL) {
					// Power-cycle status changes during a brief interval at a fixed time of day.
					PowerCycleStatus powerStatus = new PowerCycleStatus(now);
					cache.add(powerStatus);
					timeLastPowerStatusUpdate = now;
					slogger.create().info().level(2).extractCallInfo().msg("Adding power cycle update: "+powerStatus).send();
				}
				
				StatusCategory status = cache.remove(0);
			
				slogger.create().info().level(3).extractCallInfo()
					.msg("Read status from cache with cat: " + status.getCategoryName()).send();
				String cat = status.getCategoryName();

				Filter filter = null;
				Number value;
				if (status instanceof TelescopeStatus) {
					// TELESCOPE
					TelescopeStatus telstatus = (TelescopeStatus) status;
					List<TelescopeFilterAdapter> tlist = telescopeCatToFilter.get(cat);
					if (tlist == null)
						continue;
				
					for (int i = 0; i < tlist.size(); i++) {
						TelescopeFilterAdapter tf = tlist.get(i);
						Number update = tf.getStatusItem(telstatus);
						long time = telstatus.getStatusTimeStamp();
						filter = tf.getFilter();
						value = filter.filterUpdate(time, update);

						if (value instanceof Double)						
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4.2f -> %4.2f \n", 
									filter.getFilterName(),
									update, value)).send();
						else if (value instanceof Integer)						
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4d -> %4d \n", 
									filter.getFilterName(), update,
									value)).send();
						else							
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %6s -> %6s \n",  
										filter.getFilterName(), update.toString(),
										value.toString())).send();
						
						notifyListenersFilterUpdated(filter.getFilterName(), time, update, value);
						// check associated criteria
						List<Criterion> critlist = filterToCriterion.get(filter.getFilterName());
						if (critlist == null)
							continue;
						for (int ic = 0; ic < critlist.size(); ic++) {
							Criterion crit = critlist.get(ic);
							boolean critValue = crit.criterionUpdate(time, value);
							notifyListenersCriterionUpdated(crit.getCriterionName(), time, critValue);
							// now check which rule is attached to this filter
							Rule rule = criterionToRule.get(crit.getCriterionName());
							double ruleValue = rule.ruleUpdate(time, critValue);
							notifyListenersRuleUpdated(rule.getRuleName(), time, (ruleValue > 0.99));
							// forward scan through feed-in rules
							propagateRuleUpdates(rule, time, ruleValue);

						}
					}

				} else if (status instanceof MeteorologyStatus) {
					// METEOROLOGY
					MeteorologyStatus metstatus = (MeteorologyStatus) status;
					List<MeteoFilterAdapter> mlist = meteoCatToFilter.get(cat);
					if (mlist == null)
						continue;
					for (int i = 0; i < mlist.size(); i++) {
						MeteoFilterAdapter mf = mlist.get(i);
						Number update = mf.getStatusItem(metstatus);
						long time = metstatus.getStatusTimeStamp();
						filter = mf.getFilter();
						value = filter.filterUpdate(time, update);

						if (value instanceof Double)						
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4.2f -> %4.2f \n", 
									filter.getFilterName(),
									update, value)).send();
						else if (value instanceof Integer)							
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4d -> %4d \n", 
									filter.getFilterName(), update,
									value)).send();
						else							
							slogger.create().info().level(1).extractCallInfo()
							.msg(String.format("Update filter: %12s %6s -> %6s \n",  
									filter.getFilterName(), update.toString(),
									value.toString())).send();
						
						notifyListenersFilterUpdated(filter.getFilterName(), time, update, value);
						// check associated criteria
						
						//System.err.println("Checking crits for filter: "+filter.getFilterName());
						List<Criterion> critlist = filterToCriterion.get(filter.getFilterName());
						if (critlist == null)
						    continue;
						for (int ic = 0; ic < critlist.size(); ic++) {
							Criterion crit = critlist.get(ic);
							boolean critValue = crit.criterionUpdate(time, value);
							notifyListenersCriterionUpdated(crit.getCriterionName(), time, critValue);
							// now check which rule is attached to this filter
							Rule rule = criterionToRule.get(crit.getCriterionName());
							double ruleValue = rule.ruleUpdate(time, critValue);
							notifyListenersRuleUpdated(rule.getRuleName(), time, (ruleValue > 0.99));
							// forward scan thro feedin rules
							propagateRuleUpdates(rule, time, ruleValue);

						}
					}
				} else if (status instanceof SolarCycleStatus) {
					// SOLAR-CYCLE
					SolarCycleStatus solarCycleStatus = (SolarCycleStatus)status;
					List<SolarCycleFilterAdapter> slist = solarCatToFilter.get(cat);
					if (slist == null)
						continue;
					for (int i = 0; i < slist.size(); i++) {
						SolarCycleFilterAdapter mf = slist.get(i);
						Number update = mf.getStatusItem(solarCycleStatus);
						long time = solarCycleStatus.getStatusTimeStamp();
						filter = mf.getFilter();
						value = filter.filterUpdate(time, update);

						if (value instanceof Double)						
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4.2f -> %4.2f \n", 
									filter.getFilterName(),
									update, value)).send();
						else if (value instanceof Integer)							
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4d -> %4d \n", 
									filter.getFilterName(), update,
									value)).send();
						else		
							slogger.create().info().level(1).extractCallInfo()
							.msg(String.format("Update filter: %12s %6s -> %6s \n",  
								filter.getFilterName(), update.toString(),
								value.toString())).send();						
							
						
						notifyListenersFilterUpdated(filter.getFilterName(), time, update, value);
						// check associated criteria
						List<Criterion> critlist = filterToCriterion.get(filter.getFilterName());
						if (critlist == null)
						    continue;
						for (int ic = 0; ic < critlist.size(); ic++) {
							Criterion crit = critlist.get(ic);
							boolean critValue = crit.criterionUpdate(time, value);
							notifyListenersCriterionUpdated(crit.getCriterionName(), time, critValue);
							// now check which rule is attached to this filter
							Rule rule = criterionToRule.get(crit.getCriterionName());
							double ruleValue = rule.ruleUpdate(time, critValue);
							notifyListenersRuleUpdated(rule.getRuleName(), time, (ruleValue > 0.99));
							// forward scan thro feedin rules
							propagateRuleUpdates(rule, time, ruleValue);

						}
					}
				} else if (status instanceof PowerCycleStatus) {
					// POWER_CYCLE
					PowerCycleStatus powerCycleStatus = (PowerCycleStatus)status;
					List<PowerCycleFilterAdapter> slist = powerCatToFilter.get(cat);
					if (slist == null)
						continue;
					for (int i = 0; i < slist.size(); i++) {
						PowerCycleFilterAdapter mf = slist.get(i);
						Number update = mf.getStatusItem(powerCycleStatus);
						long time = powerCycleStatus.getStatusTimeStamp();
						filter = mf.getFilter();
						value = filter.filterUpdate(time, update);

						if (value instanceof Double)						
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4.2f -> %4.2f \n", 
									filter.getFilterName(),
									update, value)).send();
						else if (value instanceof Integer)							
							slogger.create().info().level(1).extractCallInfo()
								.msg(String.format("Update filter: %12s %4d -> %4d \n", 
									filter.getFilterName(), update,
									value)).send();
						else	
							slogger.create().info().level(1).extractCallInfo()
							.msg(String.format("Update filter: " + 
								filter.getFilterName() + " " + update + " -> "
								+ value)).send();						
						
						notifyListenersFilterUpdated(filter.getFilterName(), time, update, value);
						// check associated criteria
						List<Criterion> critlist = filterToCriterion.get(filter.getFilterName());
						if (critlist == null)
						    continue;
						for (int ic = 0; ic < critlist.size(); ic++) {
							Criterion crit = critlist.get(ic);
							boolean critValue = crit.criterionUpdate(time, value);
							notifyListenersCriterionUpdated(crit.getCriterionName(), time, critValue);
							// now check which rule is attached to this filter
							Rule rule = criterionToRule.get(crit.getCriterionName());
							double ruleValue = rule.ruleUpdate(time, critValue);
							notifyListenersRuleUpdated(rule.getRuleName(), time, (ruleValue > 0.99));
							// forward scan thro feedin rules
							propagateRuleUpdates(rule, time, ruleValue);

						}
					}
				}

				// lookup rules associated with crits

			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException ix) {
			}
		}

		@Override
		protected void shutdown() {

		}

		/** Propagate sub-rule triggers up the rule hierarchy.*/
		private void propagateRuleUpdates(Rule rule, long time, double value) {
			String ruleName = rule.getRuleName();
			Iterator<String> ir = ruleToRule.keySet().iterator();
			int countMatches = 0;
			while (ir.hasNext()) {
				String testRuleName = ir.next();

				if (testRuleName.equals(ruleName)) {
					countMatches++;
					Rule nextRule = ruleToRule.get(ruleName);
					double outValue = 0.0;
				
					slogger.create().info().level(1).extractCallInfo()
						.msg("Propagate: " + ruleName + " ->  " + nextRule.getRuleName()).send();
					
					if (nextRule instanceof Conjunct) {
						outValue = ((Conjunct) nextRule).subruleUpdate(ruleName, time, value);
						propagateRuleUpdates(nextRule, time, outValue);
						notifyListenersRuleUpdated(nextRule.getRuleName(), time, (outValue > 0.99));
					} else if (nextRule instanceof Disjunct) {
						outValue = ((Disjunct) nextRule).subruleUpdate(ruleName, time, value);
						propagateRuleUpdates(nextRule, time, outValue);
						notifyListenersRuleUpdated(nextRule.getRuleName(), time, (outValue > 0.99));
					}
				}

			}
		}

	}

	/**
	 * @return A list of available filters.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#listFilters()
	 */
	public List<Filter> listFilters() throws RemoteException {
		List<Filter> filterList = new Vector<Filter>();
		filterList.addAll(filters.values());
		return filterList;
	}

	/**
	 * @return A list of criteria.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#listCriteria()
	 */
	public List<Criterion> listCriteria() throws RemoteException {
		List<Criterion> criterionList = new Vector<Criterion>();
		criterionList.addAll(criteria.values());
		return criterionList;
	}

	/**
	 * @return A list of rules.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#listRules()
	 */
	public List<Rule> listRules() throws RemoteException {
		List<Rule> ruleList = new Vector<Rule>();
		ruleList.addAll(rules.values());
		return ruleList;
	}

	/**
	 * @return Mapping from filters to criteria.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#getFilterCriterionMapping()
	 */
	public Map<String, List<Criterion>> getFilterCriterionMapping()  throws RemoteException {
			return filterToCriterion;
	}

	/**
	 * @return Mapping from criteria to low level rules.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#getCriterionRuleMapping()
	 */
	public Map<String, Rule> getCriterionRuleMapping() throws RemoteException {
		return criterionToRule;
	}

	/**
	 * @return Mapping from subrules to higher rules.
	 * @see ngat.rcs.ers.ReactiveSystemStructureProvider#getRuleRuleMapping()
	 */
	public Map<String, Rule> getRuleRuleMapping() throws RemoteException {
		return ruleToRule;
	}

}
