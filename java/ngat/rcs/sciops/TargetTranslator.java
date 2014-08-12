/**
 * 
 */
package ngat.rcs.sciops;

import java.util.Iterator;
import java.util.Set;

import ngat.phase2.CatalogSource;
import ngat.phase2.EphemerisSource;
import ngat.phase2.ExtraSolarSource;
import ngat.phase2.ITarget;
import ngat.phase2.Source;
import ngat.phase2.XEphemerisTarget;
import ngat.phase2.XEphemerisTrackNode;
import ngat.phase2.XExtraSolarTarget;
import ngat.phase2.XSlaNamedPlanetTarget;
import ngat.astrometry.*;

/**
 * @author eng
 * 
 */
public class TargetTranslator {

	public static Source translateToOldStyleSource(ITarget target) throws TargetTranslationException {

		if (target == null)
			throw new TargetTranslationException("Target was null");

		if (target instanceof XExtraSolarTarget) {
			XExtraSolarTarget xstar = (XExtraSolarTarget) target;
			ExtraSolarSource star = new ExtraSolarSource(xstar.getName());
			star.setRA(xstar.getRa());
			star.setDec(xstar.getDec());

			switch (xstar.getFrame()) {
			case ReferenceFrame.FK5:
			case ReferenceFrame.ICRF:
				star.setFrame(Source.FK5);
				star.setEquinox(2000.0f);
				star.setEpoch(2000.0f);
				star.setEquinoxLetter('J');
				break;
			case ReferenceFrame.FK4:
				star.setFrame(Source.FK4);
				star.setEquinox(1950.0f);
				star.setEpoch(1950.0f);
				star.setEquinoxLetter('B');
				break;
			default:
				star.setFrame(Source.FK5);
				star.setEquinox(2000.0f);
				star.setEpoch(2000.0f);
				star.setEquinoxLetter('J');
				break;
			}
			return star;

		} else if (target instanceof XEphemerisTarget) {
			XEphemerisTarget xephem = (XEphemerisTarget) target;
			EphemerisSource ephem = new EphemerisSource(xephem.getName());
			Set track = xephem.getEphemerisTrack();
			Iterator nodes = track.iterator();
			while (nodes.hasNext()) {
				XEphemerisTrackNode node = (XEphemerisTrackNode) nodes.next();
				// TODO Those rates are probably in arcsec/hour rather than
				// rad/sec
				ephem.addCoordinate(node.time, node.ra, node.dec, node.raDot, node.decDot);
			}
			ephem.setFrame(Source.FK5);
			ephem.setEpoch(2000.0f);
			ephem.setEquinox(2000.0f);
			ephem.setEquinoxLetter('J');

			return ephem;

		} else if (target instanceof XSlaNamedPlanetTarget) {
			XSlaNamedPlanetTarget xcat = (XSlaNamedPlanetTarget) target;
			CatalogSource cat = new CatalogSource(target.getName());
			cat.setCatalogId(xcat.getIndex());
			cat.setFrame(Source.FK5);
			cat.setEpoch(2000.0f);
			cat.setEquinoxLetter('A');
			cat.setEquinox(Source.EPOCH_CURRENT);

			return cat;

		}

		throw new TargetTranslationException("Unable to determine source type from supplied target: " + target);

	}

}
