package org.labrad;

import org.labrad.data.Context;

public class Constants {
    /** Default hostname for the manager. */
    public static final String DEFAULT_HOST = "localhost";

    /** Default port to use when connecting to the manager. */
    public static final int DEFAULT_PORT = 7682;

    /** Default password to use when connecting to the manager. */
    public static final String DEFAULT_PASSWORD = "";

	/** ID of the LabRAD manager. */
	public static final long MANAGER = 1;
	
	/** ID of the manager setting to retrieve a list of servers. */
	public static final long SERVERS = 1;
	
	/** ID of the manager setting to retrieve a settings list for a server. */
	public static final long SETTINGS = 2;

	/** ID of the lookup setting on the manager. */
	public static final long LOOKUP = 3;
	
	/** Version number of the LabRAD protocol version implemented here. */
	public static final long PROTOCOL = 1;
	
	/** Default context in which requests will be sent. */
	public static final Context DEFAULT_CONTEXT = new Context(0, 0);
}
