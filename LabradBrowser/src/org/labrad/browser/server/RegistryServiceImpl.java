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
	
	private List<String> getAbsPath(List<String> path) {
		List<String> absPath = Util.newArrayList();
		absPath.add("");
		absPath.addAll(path);
		return absPath;
	}
	
	private Request startPacket(List<String> path) {
		return startPacket(path, false);
	}
	
	/**
	 * Create a new packet for the registry server.  The first thing
	 * we do in all cases is to cd into the appropriate directory.
	 * @param path
	 * @param create
	 * @return
	 */
	private Request startPacket(List<String> path, boolean create) {
		List<String> absPath = getAbsPath(path);
		Request req = Request.to("Registry");
		req.add("cd", Data.clusterOf(Data.listOf(absPath, Setters.stringSetter),
				                     Data.valueOf(create)));
		return req;
	}
	
	public RegistryListing getListing(List<String> path) {
		return getListing(path, false);
	}

	private RegistryListing getListing(List<String> path, boolean create) {
		// get directory listing
		Request req = startPacket(path);
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
			req = startPacket(path);
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
	
	/**
	 * Set a key in the registry at a particular path.
	 */
	public RegistryListing set(List<String> path, String key, String value) throws RegistryError {
		Request req;
		Data val;
		
		// use the manager to convert to data
		req = Request.to("Manager");
		req.add("String To Data", Data.valueOf(value));
		try {
			val = LabradConnection.get().sendAndWait(req).get(0);
		} catch (InterruptedException e) {
			throw new RegistryError("Interrupted while converting string to data.");
		} catch (ExecutionException e) {
			throw new RegistryError("Failed to convert string to data: " + e.getCause().getMessage());
		}
		
		// set the key in the registry
		req = startPacket(path);
		req.add("set", Data.clusterOf(Data.valueOf(key), val));
		try {
			LabradConnection.get().sendAndWait(req);
		} catch (InterruptedException e) {
			throw new RegistryError("Interrupted while setting key.");
		} catch (ExecutionException e) {
			throw new RegistryError("Error while setting key: " + e.getCause().getMessage());
		}
		return getListing(path);
	}

	/**
	 * Remove a key.
	 */
	public RegistryListing del(List<String> path, String key) throws RegistryError {
		Request req = startPacket(path);
		req.add("del", Data.valueOf(key));
		try {
			LabradConnection.get().sendAndWait(req);
		} catch (InterruptedException e) {
			throw new RegistryError("Interrupted while deleting key.");
		} catch (ExecutionException e) {
			throw new RegistryError("Error while deleting key: " + e.getCause().getMessage());
		}
		return getListing(path);
	}

	/**
	 * Make a new directory.
	 */
	public RegistryListing mkdir(List<String> path, String dir) throws RegistryError {
		Request req = startPacket(path);
		req.add("mkdir", Data.valueOf(dir));
		try {
			LabradConnection.get().sendAndWait(req);
		} catch (InterruptedException e) {
			throw new RegistryError("Interrupted while creating directory.");
		} catch (ExecutionException e) {
			throw new RegistryError("Error while making dir: " + e.getCause().getMessage());
		}
		return getListing(path);
	}

	/**
	 * Remove a directory.
	 */
	public RegistryListing rmdir(List<String> path, String dir) throws RegistryError {
		// TODO make this recursive so it empties the directory first
		Request req = startPacket(path);
		req.add("rmdir", Data.valueOf(dir));
		try {
			LabradConnection.get().sendAndWait(req);
		} catch (InterruptedException e) {
			throw new RegistryError("Interrupted while creating directory.");
		} catch (ExecutionException e) {
			throw new RegistryError("Error while making dir: " + e.getCause().getMessage());
		}
		return getListing(path);
	}
}
