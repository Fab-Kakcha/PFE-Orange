/**
 * This Java's Class defines all optional parameters available on ConfManager. You can set of the these
 * parameters according to your needs
 */
package com.orange.olps.api.sip;

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
	 * Constructor to initialize the conference name
	 * @param conferenceid conference name
	 */
	public ConferenceParameters(String conferenceid){
		this.conferenceid = conferenceid;
	}
	
	
	protected String getConferenceid() {
		return conferenceid;
	}

	/**
	 * To set the conference name
	 * @param conferenceid
	 */
	public void setConferenceid(String conferenceid){
		this.conferenceid = conferenceid;
	}
	
	protected int getMaxparticipant() {
		return maxparticipant;
	}

	/**
	 * To set the maximum participants of a conference, this parameter is optional when creating a
	 * new conference and its default value is 10.
	 * @param maxparticipant maximum number of participant allow in a conference
	 */
	public void setMaxparticipant(int maxparticipant) {
		this.maxparticipant = maxparticipant;
	}
	
	protected int getTimeout() {
		return timeout;
	}

	/**
	 * To set the life expectancy of a conference in minutes, this parameter is optional when creating a
	 * new conference and its default value is 600.
	 * @param timeout life expectancy of a conference in minutes
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	protected int getRelaydtmf() {
		return relaydtmf;
	}
	
	/**
	 * To activate (value 1) or deactivate (value 0) the dtmf transmission, this parameter is optional when
	 * creating a new conference and its default value is 0
	 * @param relaydtmf either 0 or 1
	 */
	public void setRelaydtmf(int relaydtmf) {
		this.relaydtmf = relaydtmf;
	}

	protected String getType() {
		return type;
	}

	/**
	 * Set the type of conference to create (either a normal or a large conference), this parameters is 
	 * optional when creating a new conference and its default value is normal
	 * @param type either normal or large
	 */
	public void setType(String type) {
		this.type = type;
	}

	protected String getActivetone() {
		return activetone;
	}
	
	/**
	 * To activate (value true) or deactivate (value false) for a created conference, the entry and exit tone of 
	 * participants, this parameter is optional when creating a new conference, and its default value is true
	 * @param activetone either false or true
	 */
	public void setActivetone(String activetone) {
		this.activetone = activetone;
	}

	protected String getEntertone() {
		return entertone;
	}

	/**
	 * To activate or deactivate the entry tone of a participant in a conference, this parameters is optional
	 * when joining an existing conference ad its default value is true.
	 * @param entertone either true or false
	 */
	public void setEntertone(String entertone) {
		this.entertone = entertone;
	}

	protected String getExittone() {
		return exittone;
	}

	/**
	 * To activate or deactivate the exit tone of a participant in a conference, this parameters is optional
	 * when joining an existing conference ad its default value is true.
	 * @param exittone either true or false
	 */
	public void setExittone(String exittone) {
		this.exittone = exittone;
	}


	protected String getCodec() {
		return codec;
	}

	/**
	 * To set the codec value, this parameter is optional when joining an existing conference and its default
	 * value is G711A
	 * @param codec its possible values are: G711A, G711MU, G729, G722, AMRWB
	 */
	public void setCodec(String codec) {
		this.codec = codec;
	}

	protected String getName() {
		return name;
	}

	/**
	 * To associate a textual identifier with the participant joining the conference, this parameter is optional 
	 * when joining an existing conference
	 * @param name textual identifier for the participant
	 */
	public void setName(String name) {
		this.name = name;
	}

	protected String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}


	protected String getConfrole() {
		return confrole;
	}

	/**
	 * To set the profile of a participant joining a conference, this parameter is optional and its default value is
	 * speaker
	 * @param confrole four possible values: speaker, mute, coach or student
	 */
	public void setConfrole(String confrole) {
		this.confrole = confrole;
	}

	protected String getExceptlist() {
		return exceptlist;
	}
	
	/**
	 * To set the list of participants (list of participants id) not impacted by muteall or umuteall command, this 
	 * parameter is optional for these two commands and the semicolon character is used as delimiter between two participants id
	 * @param exceptlist list of participants id not impacted by muteall or umuteall command
	 */
	public void setExceptlist(String exceptlist) {
		this.exceptlist = exceptlist;
	}

	protected String getParticipantid() {
		return particpantid;
	}
	
	/**
	 * To set a participant unique identifier, this parameter is optional when playing/stopping an audio file and the 
	 * file to a participant or when requesting the status of a participant
	 * @param particpantid unique identifier of a participant
	 */
	public void setParticipantid(String particpantid) {
		this.particpantid = particpantid;
	}

	
	protected boolean isMixplay() {
		return mixplay;
	}

	/**
	 * To interrupt (value false) or not (value true) the audio mixing of participants when a file is being played 
	 * in a conference, this parameter is optional for play command ant its default value is false
	 * @param mixplay either false or true
	 */
	public void setMixplay(boolean mixplay) {
		this.mixplay = mixplay;
	}

	protected int getPriority() {
		return priority;
	}

	/**
	 * To set the priority of the audio file to be played in a conference, this parameter is optional for play 
	 * command ant its default value is 0
	 * @param priority possible values are: 0 and 1. 0: all the audio files are played one after the other, 1: 
	 * the ongoing play command 
	 * is interrupted to play the new command
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	protected String getRepeat() {
		return repeat;
	}

	/**
	 * To set how many time the audio file is to be played, this parameter is optional for play command ant its 
	 * default value is 1
	 * @param repeat possible values are: n>=0 or forever.
	 */
	public void setRepeat(String repeat) {
		this.repeat = repeat;
	}

	protected String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
}
