package com.orange.olps.api.sip;

import java.util.EventListener;

public interface OmsMessageListener extends EventListener {
	
	
	void OmsMessagePerformed(OmsMessageEvent msgevt);
	

}
