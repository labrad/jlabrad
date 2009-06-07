package org.labrad.browser.server;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.labrad.browser.LabradConnection;
import org.labrad.browser.client.RegistryError;
import org.labrad.browser.client.RegistryListing;
import org.labrad.browser.client.RegistryService;
import org.labrad.browser.client.Util;
import org.labrad.data.Data;
import org.labrad.data.Request;
import org.labrad.data.Setters;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class RegistryServiceImpl extends RemoteServiceServlet implements RegistryService {
	
	public RegistryListing getListing(List<String> path) {
		return getListing(path, false);
	}
	
	public RegistryListing createDirectory(List<String> path) {
		return getListing(path, true);
	}

	private RegistryListing getListing(List<String> path, boolean create) {
		// make an absolute path
		List<String> absPath = Util.newArrayList();
		absPath.add("");
		absPath.addAll(path);
		
		// get directory listing
		Request req = Request.to("Registry");
		req.add("cd", Data.clusterOf(Data.listOf(absPath, Setters.stringSetter),
				                     Data.valueOf(create)));
		req.add("dir");
		Data listing;
		try {
			listing = LabradConnection.get().sendAndWait(req).get(1);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		List<String> dirs = listing.get(0).getStringList();
		List<String> keys = listing.get(1).getStringList();
		
		List<String> vals = Util.newArrayList();
		if (keys.size() > 0) {
			// get the values of all keys
			req = Request.to("Registry");
			req.add("cd", Data.listOf(absPath, Setters.stringSetter));
			for (String key : keys) {
				req.add("get", Data.valueOf(key));
			}
			List<Data> data;
			try {
				data = LabradConnection.get().sendAndWait(req);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			
			// convert all values to strings using the manager
			req = Request.to("Manager");
			for (Data d : data.subList(1, data.size())) {
				req.add("Data To String", d);
			}
			try {
				data = LabradConnection.get().sendAndWait(req);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
			for (Data d : data) {
				vals.add(d.getString());
			}
		}
		
		return new RegistryListing(path, dirs, keys, vals);
	}
	
	public String set(List<String> path, String key, String value) throws RegistryError {
		// TODO Auto-generated method stub
		return null;
	}

}
