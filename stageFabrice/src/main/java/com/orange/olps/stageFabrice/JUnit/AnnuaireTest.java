package com.orange.olps.stageFabrice.JUnit;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.orange.olps.api.webrtc.Annuaire;
import com.orange.olps.api.webrtc.OmsCall;

public class AnnuaireTest {

public static void main(String[] args){
		
		Result result = JUnitCore.runClasses(AnnuaireTest.class);
		for(Failure failure : result.getFailures()){
			System.out.println(failure.toString());
		}
		
		System.out.println(result.wasSuccessful());
	}
	
	private OmsCall omsCall = null;
	private Annuaire annuaire = null;
	
	@Before
	public void setUp() throws Exception {
		annuaire = new Annuaire();
		omsCall = new OmsCall();
	}

	@After
	public void tearDown() throws Exception {
		annuaire = null;
		omsCall = null;
	}

	@Test
	public void testAnnuaire() {
		assertNotNull(annuaire);
	}

	@Test
	public void testCheckOmsCall(){
		assertFalse("omsCall doesn't exist in annuaire",annuaire.checkOmsCall(omsCall));
	}
	
}
