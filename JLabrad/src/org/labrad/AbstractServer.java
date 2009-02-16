package org.labrad;

public abstract class AbstractServer implements Server {

	private Connection connection;
	
	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection cxn) {
		connection = cxn;
	}

	public abstract void init();
	public abstract void shutdown();

}
