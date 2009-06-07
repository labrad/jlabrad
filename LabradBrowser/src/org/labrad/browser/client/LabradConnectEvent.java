package org.labrad.browser.client;

@SuppressWarnings("serial")
public class LabradConnectEvent implements RemoteEvent {
	private String host;
	
	protected LabradConnectEvent() {}
	
	public LabradConnectEvent(String host) {
		this.host = host;
	}
	
	public String getHost() { return host; }
	
	@Override
	public String toString() { return "host: '" + host + "'"; }
}
