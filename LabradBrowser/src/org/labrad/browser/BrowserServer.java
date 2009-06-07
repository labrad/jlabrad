package org.labrad.browser;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.labrad.Connection;
import org.labrad.data.Data;
import org.labrad.data.Request;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.webapp.WebAppContext;

public class BrowserServer {
	private static final String[] REGISTRY_PATH = {"", "Nodes", "__controller__"};
	
	public static void main(String[] args) throws Exception {
		String jetty_default = new java.io.File("./LabradBrowser.war").exists()
								? "./LabradBrowser.war"
								: "./war";
	    String jetty_home = System.getProperty("jetty.home", jetty_default);
	    
	    Server server = new Server();
	    
	    Connector connector = new SelectChannelConnector();
	    connector.setPort(Integer.getInteger("jetty.port", 7667).intValue());
	    server.setConnectors(new Connector[] {connector});
	    
	    Constraint constraint = new Constraint();
	    constraint.setName(Constraint.__DIGEST_AUTH);
	    constraint.setRoles(new String[] {"user"});
	    constraint.setAuthenticate(true);

	    ConstraintMapping cm = new ConstraintMapping();
	    cm.setConstraint(constraint);
	    cm.setPathSpec("/*");

	    HashUserRealm realm;
	    while (true) {
	    	try {
	    		realm = loadUserInfo();
	    		break;
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    		System.out.println("Unable to load user information from registry.");
	    		System.out.println("Will retry in 10 seconds...");
	    		Thread.sleep(10*1000);
	    	}
	    }
	    
	    SecurityHandler sh = new SecurityHandler();
	    sh.setUserRealm(realm);
	    sh.setConstraintMappings(new ConstraintMapping[] {cm});
	    
	    WebAppContext webapp = new WebAppContext();
	    webapp.setContextPath("/");
	    webapp.setWar(jetty_home);
	    webapp.addHandler(sh);
	    
	    server.setHandler(webapp);
	    
	    server.start();
	    server.join();
	}
	
	private static HashUserRealm loadUserInfo() throws InterruptedException, ExecutionException {
		Connection cxn = LabradConnection.get();
		Request req = Request.to("Registry");
		// change into the correct directory
		req.add("cd", Data.valueOf(REGISTRY_PATH));
		Data defaultUsers = Data.ofType("*(ss)");
		defaultUsers.setArraySize(1);
		defaultUsers.setString("webuser", 0, 0);
		defaultUsers.setString(String.valueOf(new Random().nextInt(1000000000)), 0, 1);
		// load the user info, setting a default value if it doesn't exist
		req.add("get", Data.clusterOf(Data.valueOf("users"),
				                      Data.valueOf("*(ss)"),
				                      Data.valueOf(true),
				                      defaultUsers));
		List<Data> ans;
		ans = cxn.sendAndWait(req).get(1).getDataList();
		HashUserRealm realm = new HashUserRealm("LabRAD Controller");
		for (Data userPair : ans) {
			String user = userPair.get(0).getString();
			String pw = userPair.get(1).getString();
			realm.put(user, pw);
			realm.addUserToRole(user, "user");
		}
		return realm;
	}
}
