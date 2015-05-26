package com.orange.olps.api.sip;

import java.util.EventListener;

public interface OmsDtmfListener extends EventListener{

	void omsDtmfPerformed(OmsDtmfEvent dtmfEvt);
	
}
