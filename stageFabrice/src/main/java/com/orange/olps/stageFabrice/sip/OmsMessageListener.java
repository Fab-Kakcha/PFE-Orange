package com.orange.olps.stageFabrice.sip;

import java.util.EventListener;

public interface OmsMessageListener extends EventListener {
	
	
	void OmsMessagePerformed(OmsMessageEvent msgevt);
	

}
