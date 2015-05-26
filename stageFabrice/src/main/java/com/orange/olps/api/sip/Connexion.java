package com.orange.olps.api.sip;

public class Connexion {
	
	public Connexion() throws OmsException {
		
	}
	
	public String getReponse(String question) throws OmsException {
		String reponse = "Reponse " + question;
		return reponse;
	}
	
	public void send(String question) {
	}
}
