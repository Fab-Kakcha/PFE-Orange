/**
 * This Java's Class defines all optional parameters available on ConfManager. You can set of the these
 * parameters according to your needs
 */
package com.orange.olps.api.webrtc;

/**
 * @author JWPN9644
 *
 */
public class ConferenceParameters {
	
	private String conferenceid;
	private int maxparticipant = 10;
	private int timeout = 600;
	private int relaydtmf = 0;
	private String type = "normal";
	private String activetone = "true";
	private String entertone = "true";
	private String exittone = "true";
	private String codec = "G711A";
	private String name = "your username";
	private String from;
	private String confrole = "speaker";
	private String exceptlist;
	private String particpantid;
	private boolean mixplay = false;
	private int priority = 0;
	private String repeat = "1";
	private String user = "user";
	
	/**
	 * Construction to initialize the conrference's name name
	 * @param conferenceid the conference name's
	 */
	public ConferenceParameters(String conferenceid){
		this.conferenceid = conferenceid;
	}
	
	/**
	 * Get the conference's name
	 * @return
	 */
	public String getConferenceid() {
		return conferenceid;
	}

	/**
	 * Get the maximum numbers participants in a conference
	 * @return maximum number of participants
	 */
	public int getMaxparticipant() {
		return maxparticipant;
	}

	/**
	 * Set the maximum participant for a conference
	 * @param maxparticipant
	 */
	public void setMaxparticipant(int maxparticipant) {
		this.maxparticipant = maxparticipant;
	}
	
	/**
	 * Get the useful life of a conference in minutes
	 * @return
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the useful life of a conference in minutes
	 * @param timeout
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getRelaydtmf() {
		return relaydtmf;
	}

	public void setRelaydtmf(int relaydtmf) {
		this.relaydtmf = relaydtmf;
	}

	/**
	 * Get the type of conference to create
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the type of conference to create
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Get the activatetone parameter of a conference
	 * @return
	 */
	public String getActivetone() {
		return activetone;
	}
	
	/**
	 * Get the activatetone parameter of a conference
	 * @param activetone
	 */
	public void setActivetone(String activetone) {
		this.activetone = activetone;
	}

	/**
	 * Get the entertone parameter of a conference
	 * @return
	 */
	public String getEntertone() {
		return entertone;
	}

	/**
	 * Set the entertone parameter of a conference
	 * @param entertone
	 */
	public void setEntertone(String entertone) {
		this.entertone = entertone;
	}

	/**
	 * Get exitone
	 * @return
	 */
	public String getExittone() {
		return exittone;
	}

	/**
	 * Set exittone
	 * @param exittone
	 */
	public void setExittone(String exittone) {
		this.exittone = exittone;
	}

	/**
	 * Get the codec being used
	 * @return
	 */
	public String getCodec() {
		return codec;
	}

	/**
	 * Get the codec to use for creating the conference
	 * @param codec
	 */
	public void setCodec(String codec) {
		this.codec = codec;
	}

	/**
	 * Get the userName
	 * @return username
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the userName
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * Get the confrole
	 * @return
	 */
	public String getConfrole() {
		return confrole;
	}

	public void setConfrole(String confrole) {
		this.confrole = confrole;
	}

	public String getExceptlist() {
		return exceptlist;
	}

	public void setExceptlist(String exceptlist) {
		this.exceptlist = exceptlist;
	}

	public String getParticpantid() {
		return particpantid;
	}

	public void setParticpantid(String particpantid) {
		this.particpantid = particpantid;
	}

	public boolean isMixplay() {
		return mixplay;
	}

	public void setMixplay(boolean mixplay) {
		this.mixplay = mixplay;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getRepeat() {
		return repeat;
	}

	public void setRepeat(String repeat) {
		this.repeat = repeat;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
}
