package orange.olps.svi.stats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import orange.olps.svi.config.Config;
import orange.olps.svi.stats.impl.StatWriterBdd;
import orange.olps.svi.stats.impl.StatWriterHttp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

	

public class StatManager  implements Runnable {
	public static final String PROMPT = "prompt";
	public static final String NAVIGATION = "navigation";
	public static final String CLIENT = "client";
	public static final String OVP = "OVP";
	public static final String HANGUP = "hangup";
	public static final String SMS = "sms";
	public static final String HNGP_CLIENT = "Client";
	
	public static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	
	 /**
	  *  queue de messages a envoyer au serveur de stat
	  */
	private BlockingQueue<String> queue;
	
	
	/**
	 * singleton
	 */
	private static StatManager statManager = null;
	/**
	 * thread d'emission  des requetes http au serveur de stat
	 */
	private  Thread thread = null;
	
	private ArrayList<IStatWriter> writers = null;
	
	protected static Log logger = LogFactory.getLog(StatManager.class.getName());
	
	/**
	 * Constructeur du Singleton
	 */
	private StatManager () {
		
	}
	/**
	 * Methode retournant le singleton
	 * @return statManager
	 */
	public static StatManager getInstance() {
		if (statManager == null) {
			synchronized (StatManager.class) {
				if (statManager == null) {
					statManager = new StatManager();					
				}
			}
		}
		return statManager;
	}
	public  void initialiser() {
		logger.info("Initialiser - Entrée");
		
		synchronized (StatManager.class) {
			if (thread != null) {
				stop();
			}
			int tailleQueue;
			try {
				tailleQueue = Integer.parseInt(Config.getInstance().getProperty(Config.TAILLE_QUEUE));
			} catch (NumberFormatException e) {
				logger.error("initialiser - parametre non numerique (defaut positionne a 5) "+Config.TAILLE_QUEUE);
				tailleQueue = 5;
			}
			logger.debug("initialiser - taille fille attente = "+tailleQueue);
			if (queue == null) {
				queue = new ArrayBlockingQueue<String>(tailleQueue);
			}
			else {
				BlockingQueue<String> queueSav = queue;
				queue = new ArrayBlockingQueue<String>(tailleQueue);
				queue.addAll(queueSav);
			}
			if (writers != null) {
				for (IStatWriter w : writers) {
					w.fermerWriter();
				}
			}
			writers = StatWriterFactory.createWriter();
			lancerThread();
		}
	}
	public void run() {
		String don;
	
		try {
			while (true) {
				/* attente lecture de la queue */
				don = queue.take();
				if (don != null) {	
					for (IStatWriter w : writers) {
						w.ecrireStat(don);
					}
				}
				else {
					logger.error("run - données nulles,  fin de thread...");
					break;
				}
			}
		} catch (InterruptedException e) {
			logger.debug("run - fin de thread");	
			
		} 
		finally {
			thread = null;
		}
		
	}
	/**
	 * Ecriture des donnees statistiques dans la queue de traitement
	 * @param id : identifiant du client
	 * @param donnee : donnees a mettre en log
	 * @param time : temps en seconde de l'evenement
	 * @param type : type de l'evenement
	 */
	public void posterStatistiques(String id, String donnee, long time, String type) {

		posterStatistiques(id,donnee,df.format(new Date(time)),type);
		return;
	}
	/**
	 * Ecriture des donnees statistiques dans la queue de traitement
	 * @param id : identifiant du client
	 * @param donnee : donnees a mettre en log
	 * @param time : chaine au format YYYY/MM/DD HH:mi:ss
	 * @param type : type de l'evenement
	 */
	public void posterStatistiques(String id, String donnee, String time, String type) {
		/* on ecrit un message dans la file */

		StringBuffer s = new StringBuffer(id);
		s.append(";");
		s.append(donnee);
		s.append(";");		
		s.append(time);
		s.append(";");
		s.append(type);
		
		if (!queue.offer(s.toString())) {
			logger.error("posterStatistiques - Queue pleine ("+queue.size()+") ("+s+")");
		}

		if (thread == null) {
			lancerThread();
		}
	}
	/**
	 * Lancement du thread de stats
	 */
	private void lancerThread () {
		logger.debug("lancerThread - Entree");
		if(thread == null || !thread.isAlive()) {
			synchronized (StatManager.class) {
				if(thread == null) {	
					thread = new Thread(statManager);
					thread.start();
				}
			}
		}
	}
	public void stop() {
		logger.debug("stop - entree");
		if(thread != null && thread.isAlive()) {
			synchronized (StatManager.class) {
				if(thread != null) {
					thread.interrupt();
					thread = null;
					logger.debug("stop - thread stoppe");
				}
			}
		}
	}
	public static boolean isWriterHttp() {
		for (IStatWriter w : getInstance().writers) {
			if (w instanceof StatWriterHttp) return true;
		}
		return false;
	}
	public static boolean isWriterBdd() {
		for (IStatWriter w : getInstance().writers) {
			if (w instanceof StatWriterBdd) return true;
		}
		return false;
	}
}
