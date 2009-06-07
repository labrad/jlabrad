package org.labrad.browser.client;

@SuppressWarnings("serial")
public class LabradDisconnectEvent implements RemoteEvent {
	private String host;
	
	protected LabradDisconnectEvent() {}
	
	public LabradDisconnectEvent(String host) {
		this.host = host;
	}
	
	public String getHost() { return host; }
	
	@Override
	public String toString() { return "host: '" + host + "'"; }
}
