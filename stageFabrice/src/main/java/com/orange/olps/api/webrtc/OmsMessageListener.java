package com.orange.olps.api.webrtc;

import java.util.EventListener;

public interface OmsMessageListener extends EventListener {
	
	
	void omsMessagePerformed(OmsMessageEvent msgevt) throws OmsException;
	
}
