package org.labrad.browser.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class RegistryEditor extends Composite
		implements OpenHandler<TreeItem>, SelectionHandler<TreeItem>, CloseHandler<TreeItem> {
	/**
	 * Remote service for getting registry listings
	 */
	private final RegistryServiceAsync registryService = GWT.create(RegistryService.class);
	
	private final Tree tree = new Tree();
	private final Map<String, TreeItem> dirs = new HashMap<String, TreeItem>();
	
	class ItemInfo {
		private Map<String, TreeItem> dirs = new HashMap<String, TreeItem>();
		private List<String> path = new ArrayList<String>();
		public ItemInfo(List<String> path, String dir) {
			this.path.addAll(path);
			this.path.add(dir);
		}
		public Map<String, TreeItem> getDirs() { return dirs; }
		public List<String> getPath() { return path; }
	}
	
	public RegistryEditor() {
		tree.addOpenHandler(this);
		tree.addCloseHandler(this);
		tree.addSelectionHandler(this);
		tree.setAnimationEnabled(true);
		initWidget(tree);
		loadDirectory(new ArrayList<String>());
	}
	
	private void loadDirectory(List<String> path) {
		registryService.getListing(path, new AsyncCallback<RegistryListing>() {
			public void onFailure(Throwable caught) {
				tree.addItem("error!");
			}

			public void onSuccess(RegistryListing result) {
				populateTree(result);
			}
		});
	}
	
	private void populateTree(RegistryListing listing) {
		TreeItem item;
		List<TreeItem> oldItems = Util.newArrayList();
		List<String> path = listing.getPath();
		List<String> dirs = listing.getDirs();
		List<String> keys = listing.getKeys();
		List<String> vals = listing.getVals();
		if (path.size() == 0) {
			for (int i = 0; i < tree.getItemCount(); i++) {
				oldItems.add(tree.getItem(i));
			}
			this.dirs.clear();
			for (String dir : dirs) {
				item = new TreeItem(dir);
				item.setUserObject(new ItemInfo(path, dir));
				item.addItem("");
				this.dirs.put(dir, item);
				tree.addItem(item);
			}
			for (int i = 0; i < keys.size(); i++) {
				item = new TreeItem(keys.get(i) + " = " + vals.get(i));
				tree.addItem(item);
			}
			for (TreeItem oldItem : oldItems) {
				tree.removeItem(oldItem);
			}
		} else {
			TreeItem parent = lookupItem(path);
			for (int i = 0; i < parent.getChildCount(); i++) {
				oldItems.add(parent.getChild(i));
			}
			Map<String, TreeItem> dirMap = ((ItemInfo)parent.getUserObject()).getDirs();
			dirMap.clear();
			for (String dir : dirs) {
				item = new TreeItem(dir);
				item.setUserObject(new ItemInfo(path, dir));
				item.addItem("");
				dirMap.put(dir, item);
				parent.addItem(item);
			}
			for (int i = 0; i < keys.size(); i++) {
				item = new TreeItem(keys.get(i) + " = " + vals.get(i));
				parent.addItem(item);
			}
			for (TreeItem oldItem : oldItems) {
				parent.removeItem(oldItem);
			}
		}
	}
	
	private TreeItem lookupItem(List<String> path) {
		Map<String, TreeItem> dirs = this.dirs;
		TreeItem item = null;
		for (String dir : path) {
			item = dirs.get(dir);
			dirs = ((ItemInfo)item.getUserObject()).getDirs();
		}
		return item;
	}

	/**
	 * Load contents of a registry directory when a tree node is opened
	 */
	public void onOpen(OpenEvent<TreeItem> event) {
		ItemInfo info = (ItemInfo)event.getTarget().getUserObject();
		loadDirectory(info.getPath());
	}

	/**
	 * Remove child items when a tree node is closed
	 */
	public void onClose(CloseEvent<TreeItem> event) {
		//event.getTarget().removeItems();
	}
	
	public void onSelection(SelectionEvent<TreeItem> event) {
		// TODO Auto-generated method stub
		
	}
}
