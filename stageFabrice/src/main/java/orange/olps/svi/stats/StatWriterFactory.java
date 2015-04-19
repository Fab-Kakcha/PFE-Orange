package orange.olps.svi.stats;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import orange.olps.svi.config.Config;
import orange.olps.svi.stats.impl.StatWriterBdd;
import orange.olps.svi.stats.impl.StatWriterFic;
import orange.olps.svi.stats.impl.StatWriterHttp;

public class StatWriterFactory {

	protected static Log logger = LogFactory.getLog(StatWriterFactory.class.getName());
	
	public static ArrayList<IStatWriter> createWriter() {
		String[] modes = Config.getInstance().getProperty(Config.STAT_MODE,"LOCAL").trim().split(",");
		ArrayList<IStatWriter> writers = new  ArrayList<IStatWriter>(1);
		
		for (String m : modes) {
			if (m.equals("LOCAL")) {
				boolean creationWriter = true;
				for (IStatWriter isw : writers) {
					if (isw instanceof StatWriterFic) {
						creationWriter = false;
						break;
					}
				}
				if (creationWriter) {
					logger.debug("createWriter - LOCAL");
					writers.add(new StatWriterFic());
				}
			}
			else if (m.equals("BDD")) {
				logger.debug("createWriter - BDD");				
				if(verifierCreation(writers)) writers.add( new StatWriterBdd());
			}
			else if (m.equals("HTTP")) {
				logger.debug("createWriter - HTTP");				
				if(verifierCreation(writers)) {
					writers.add( new StatWriterHttp());
				}
			}
		}
		
		return writers;
	}
	/**
	 * verifie que le mode HTTP ou BDD ne soit pas déjà instancié
	 * @param writers
	 * @return
	 */
	private static boolean verifierCreation(ArrayList<IStatWriter> writers) {
		boolean creationWriter = true;
		for (IStatWriter isw : writers) {
			if (isw instanceof StatWriterHttp) {
				creationWriter = false;
				break;
			}
			else if (isw instanceof StatWriterBdd) {
				creationWriter = false;
				break;
			}
		}
		return creationWriter;
	}

}
