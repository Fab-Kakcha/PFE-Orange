package com.orange.olps.stageFabrice.JUnit;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.orange.olps.api.webrtc.OmsCall;
import com.orange.olps.api.webrtc.OmsConference;
import com.orange.olps.api.webrtc.OmsException;

public class OmsConferenceTest {
	
	
	public static void main(String[] args){
		
		Result result = JUnitCore.runClasses(OmsConferenceTest.class);
		for(Failure failure : result.getFailures()){
			System.out.println(failure.toString());
		}
		
		System.out.println(result.wasSuccessful());
	}
	

	private OmsConference omsConference = null;
	private OmsCall omsCall = null;
	
	@Before
	public void initialiser() throws OmsException, IOException{
			
		omsConference = new OmsConference("10.184.48.159","10000");	
		omsCall = new OmsCall();
		omsCall.connect("10.184.48.159","2470");
	}
	
	
	@After
	public void nettoyerOmsConference(){
		omsConference = null;
		omsCall = null;
	}

	
	@Test //@Test(expected=IllegalArgumentException.class) 
	public void testOmsConference() {
		assertNotNull("The instance was created", omsConference);
	}
	
	@Test
	public void testOmsCall(){
		assertNotNull("The instance was created", omsCall);
	}
	
	@Ignore
	@Test(expected=OmsException.class)
	public void testCreate() {
		
		try {
			String param = "t:speaker:conf1";
			omsConference.create(omsCall, param);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Test(expected=OmsException.class)
	public void testAdd() {
			
		try {
			String param = "t:speaker:conf1";
			omsConference.create(omsCall, param);
			omsCall.setConfName("conf1");
			omsConference.add(omsCall, param);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Ignore
	@Test(expected=OmsException.class)
	public void testDelete() {
		
		try {
			//omsCall.setConfName("conf1");
			omsConference.delete(omsCall);
		} catch (OmsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Ignore
	@Test
	public void testDestroy() {
		fail("Not yet implemented");
	}
	
	@Ignore
	@Test(expected=OmsException.class)
	public void testMute() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test(expected=OmsException.class)
	public void testUnmute() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test(expected=OmsException.class)
	public void testMuteAll() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test(expected=OmsException.class)
	public void testUnmuteAll() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test
	public void testStatus() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test
	public void testInfos() {
		fail("Not yet implemented");
	}
	
	@Ignore
	@Test
	public void testGetParticipantsNumber() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test
	public void testGetVipConnexion() {
		fail("Not yet implemented");
	}

	@Ignore
	@Test
	public void testGetName() {

	}

	@Ignore
	@Test
	public void testRun() {
		fail("Not yet implemented");
	}

}
