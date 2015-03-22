package com.orange.olps.stageFabrice.sip;

import java.util.EventListener;

public interface OmsDtmfListener extends EventListener{

	void dtmfPerformed(OmsDtmfEvent dtmfEvt) throws OmsException;
	
}
