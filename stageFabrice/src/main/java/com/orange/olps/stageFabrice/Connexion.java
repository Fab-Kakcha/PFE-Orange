package com.orange.olps.stageFabrice;

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
