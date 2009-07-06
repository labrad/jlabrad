package org.labrad.browser.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class RegistryEditor extends Composite
		implements OpenHandler<TreeItem>, SelectionHandler<TreeItem>, CloseHandler<TreeItem> {
	/**
	 * Remote service for getting registry listings
	 */
	private final RegistryServiceAsync registryService = GWT.create(RegistryService.class);
	
	/**
	 * Images for use in the registry tree
	 */
	private static final NodeImageBundle images = GWT.create(NodeImageBundle.class);
	
	private final Tree tree = new Tree();
	private final Map<String, TreeItem> dirs = new HashMap<String, TreeItem>();
	
	static class ItemInfo {
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
		tree.setAnimationEnabled(false);
		initWidget(tree);
		loadDirectory(new ArrayList<String>());
		// don't listen for key events, so they can be caught by edit fields
		tree.unsinkEvents(Event.KEYEVENTS);
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
			//tree.addItem(makeNewDirItem(path));
			//tree.addItem(makeNewKeyItem(path));
			for (String dir : dirs) {
				item = makeDirItem(path, dir);
				this.dirs.put(dir, item);
				tree.addItem(item);
			}
			for (int i = 0; i < keys.size(); i++) {
				item = makeKeyItem(path, keys.get(i), vals.get(i));
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
			//parent.addItem(makeNewDirItem(path));
			//parent.addItem(makeNewKeyItem(path));
			for (String dir : dirs) {
				item = makeDirItem(path, dir);
				dirMap.put(dir, item);
				parent.addItem(item);
			}
			for (int i = 0; i < keys.size(); i++) {
				item = makeKeyItem(path, keys.get(i), vals.get(i));
				parent.addItem(item);
			}
			for (TreeItem oldItem : oldItems) {
				parent.removeItem(oldItem);
			}
		}
	}

	class NewDirWidget extends Composite {
		private final TextBox box = new TextBox();
		private final HorizontalPanel panel = new HorizontalPanel();
		
		public NewDirWidget(final List<String> path) {
			box.setText("<new dir>");
			box.addStyleName("unselected-textbox");
			box.addFocusHandler(new FocusHandler() {
				public void onFocus(FocusEvent event) {
					tree.unsinkEvents(Event.KEYEVENTS);
					box.setText("");
					box.removeStyleName("unselected-textbox");
				}
			});
			box.addBlurHandler(new BlurHandler() {
				public void onBlur(BlurEvent event) {
					tree.sinkEvents(Event.KEYEVENTS);
					box.setText("<new dir>");
					box.addStyleName("unselected-textbox");
				}
			});
			box.addKeyPressHandler(new KeyPressHandler() {
				public void onKeyPress(KeyPressEvent event) {
					if (event.getCharCode() == '\r') {
						registryService.mkdir(path, box.getText(), new AsyncCallback<RegistryListing>() {
							public void onFailure(Throwable caught) {
								Window.alert("Failed to create directory");
								box.setFocus(false);
							}
							public void onSuccess(RegistryListing result) {
								box.setFocus(false);
								populateTree(result);
							}
							
						});
					} else if (event.getCharCode() == (char) 27) {
						box.setFocus(false);
					}
				}
			});
			
			panel.add(images.folderAdd().createImage());
			panel.add(box);
			initWidget(panel);
		}
	}
	
	private TreeItem makeNewDirItem(List<String> path) {
		TreeItem item = new TreeItem(new NewDirWidget(path));
		item.addStyleName("new-item");
		return item;
	}

	private TreeItem makeDirItem(List<String> path, String dir) {
		HorizontalPanel p = new HorizontalPanel();
		p.add(images.folder().createImage());
		p.add(new Label(dir));
		TreeItem item = new TreeItem(p);
		item.setUserObject(new ItemInfo(path, dir));
		item.addItem("");
		return item;
	}

	class NewKeyWidget extends Composite {
		private final TextBox keyBox = new TextBox();
		private boolean keyCleared = false;
		private final TextBox valueBox = new TextBox();
		private boolean valueCleared = false;
		private final HorizontalPanel panel = new HorizontalPanel();
		
		public NewKeyWidget(final List<String> path) {
			keyBox.setText("<new key>");
			keyBox.addStyleName("unselected-textbox");
			keyBox.addFocusHandler(new FocusHandler() {
				public void onFocus(FocusEvent event) {
					tree.unsinkEvents(Event.KEYEVENTS);
					if (!keyCleared) {
						keyBox.setText("");
						keyCleared = true;
					}
					keyBox.removeStyleName("unselected-textbox");
				}
			});
			keyBox.addBlurHandler(new BlurHandler() {
				public void onBlur(BlurEvent event) {
					tree.sinkEvents(Event.KEYEVENTS);
					keyBox.addStyleName("unselected-textbox");
				}
			});
			keyBox.addKeyPressHandler(new KeyPressHandler() {
				public void onKeyPress(KeyPressEvent event) {
					if (event.getCharCode() == '\r') {
						valueBox.setFocus(true);
					} else if (event.getCharCode() == (char) 27) {
						keyBox.setFocus(false);
						valueBox.setFocus(false);
					}
				}
			});
			
			valueBox.setText("<value>");
			valueBox.addStyleName("unselected-textbox");
			valueBox.addFocusHandler(new FocusHandler() {
				public void onFocus(FocusEvent event) {
					tree.unsinkEvents(Event.KEYEVENTS);
					if (!valueCleared) {
						valueBox.setText("");
						valueCleared = true;
					}
					valueBox.removeStyleName("unselected-textbox");
				}
			});
			valueBox.addBlurHandler(new BlurHandler() {
				public void onBlur(BlurEvent event) {
					tree.sinkEvents(Event.KEYEVENTS);
					valueBox.addStyleName("unselected-textbox");
				}
			});
			valueBox.addKeyPressHandler(new KeyPressHandler() {
				public void onKeyPress(KeyPressEvent event) {
					if (event.getCharCode() == '\r') {
						registryService.set(path, keyBox.getText(), valueBox.getText(),
								new AsyncCallback<RegistryListing>() {
							public void onFailure(Throwable caught) {
								Window.alert("Failed to create directory");
								keyBox.setFocus(false);
								valueBox.setFocus(false);
							}
							public void onSuccess(RegistryListing result) {
								keyBox.setFocus(false);
								valueBox.setFocus(true);
								populateTree(result);
							}
							
						});
					} else if (event.getCharCode() == (char) 27) {
						keyBox.setFocus(false);
						valueBox.setFocus(false);
					}
				}
			});
			
			panel.add(images.folderAdd().createImage());
			panel.add(keyBox);
			panel.add(valueBox);
			initWidget(panel);
		}
	}

	private TreeItem makeNewKeyItem(List<String> path) {
		TreeItem item = new TreeItem(new NewKeyWidget(path));
		return item;
	}

	private TreeItem makeKeyItem(final List<String> path, final String key, final String val) {
		HorizontalPanel p = new HorizontalPanel();
		p.add(images.key().createImage());
		p.add(new Label(key + " = "));
		final TextBox t = new TextBox();
		t.setText(val);
		t.setWidth("500px");
		t.addFocusHandler(new FocusHandler() {
			public void onFocus(FocusEvent event) {
				tree.unsinkEvents(Event.KEYEVENTS);
			}
		});
		t.addBlurHandler(new BlurHandler() {
			public void onBlur(BlurEvent event) {
				tree.sinkEvents(Event.KEYEVENTS);
			}
		});
		t.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == '\r') {
					registryService.set(path, key, t.getText(), new AsyncCallback<RegistryListing>() {
						public void onFailure(Throwable caught) {
							Window.alert("Failed!   " + caught.getMessage());
						}
						public void onSuccess(RegistryListing result) {
							populateTree(result);
						}
					});
				} else {
					event.stopPropagation();
				}
			}
		});
		p.add(t);
		TreeItem item = new TreeItem(p);
		return item;
	}

	private void addDirectory(List<String> path, String dir) {
		registryService.mkdir(path, dir, new AsyncCallback<RegistryListing>() {
			public void onFailure(Throwable caught) {
				Window.alert("Failed to create directory");
			}
			public void onSuccess(RegistryListing result) {
				populateTree(result);
			}
		});
	}

	private void setKey(List<String> path, String key, String value) {
		registryService.set(path, key, value, new AsyncCallback<RegistryListing>() {
			public void onFailure(Throwable caught) {
				Window.alert("Failed to create directory");
			}
			public void onSuccess(RegistryListing result) {
				populateTree(result);
			}
		});
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
