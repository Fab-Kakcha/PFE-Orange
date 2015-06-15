/**
 * The Event source
 */
package com.orange.olps.api.sip;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JWPN9644
 *
 */


public class OmsDtmf {

	
	private List<OmsDtmfListener> listenersArray = new ArrayList<OmsDtmfListener>();
	
	public synchronized void addEventListener(OmsDtmfListener dtmfListener){
		
		listenersArray.add(dtmfListener);		
	}
	
	public synchronized void removeEventListener(OmsDtmfListener dtmfListener){
		
		listenersArray.remove(dtmfListener);
	}
	
	/*public synchronized void fireEvent(String dtmf) throws OmsException {
		
		OmsDtmfListener dtmfLis = null;
		OmsDtmfEvent dtmfEvt = new OmsDtmfEvent(this, dtmf);
		Iterator<OmsDtmfListener> iter = listenersArray.iterator();
		
		while(iter.hasNext()){
			
			dtmfLis = iter.next();
			dtmfLis.dtmfPerformed(dtmfEvt);
		}
	}*/
	
}
