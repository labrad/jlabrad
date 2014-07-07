package org.labrad.browser.client;

import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;

public class LabradBrowser implements EntryPoint {
  public void onModuleLoad() {
    // page names
    boolean useRegistry = false;
    final String[] pageNames;
    if (useRegistry) {
      pageNames = new String[] {"nodes", "registry", "security", "log"};
    } else {
      pageNames = new String[] {"nodes", "security", "log"};
    }

    // build mapping from page names to page numbers
    final Map<String, Integer> pageNumbers = Util.newHashMap();
    for (int i = 0; i < pageNames.length; i++) {
      pageNumbers.put(pageNames[i], i);
    }

    // build the tab panel
    final TabPanel tabs = new TabPanel();
    tabs.setWidth("100%");

    // populate the tab panel
    int page = 0;
    tabs.add(new ControlPanel(), pageNames[page++]);
    if (useRegistry) {
      tabs.add(new RegistryEditor(), pageNames[page++]);
    }
    tabs.add(new IpListControl(), pageNames[page++]);        
    tabs.add(LogWindow.getInstance(), pageNames[page++]);

    // when a page is selected, update the history
    tabs.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        History.newItem(pageNames[event.getSelectedItem()]);
      }
    });

    // when history is changed, select the appropriate page
    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        int page = 0;
        if (pageNumbers.containsKey(event.getValue())) {
          page = pageNumbers.get(event.getValue());
        }
        tabs.selectTab(page);
      }
    });

    // fire history status to select the appropriate tab
    History.fireCurrentHistoryState();

    // add tabs to the html page
    RootPanel.get("main").add(tabs);
  }
}
