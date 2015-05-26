package com.orange.olps.api.sip;

import java.util.EventListener;

public interface OmsCallListener extends EventListener {
	
	
	void omsCallPerformed(OmsCallEvent callEvt);

}
