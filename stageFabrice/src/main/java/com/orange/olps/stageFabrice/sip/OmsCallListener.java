package com.orange.olps.stageFabrice.sip;

import java.util.EventListener;

public interface OmsCallListener extends EventListener {
	
	
	void omsCallPerformed(OmsCallEvent callEvt);

}
