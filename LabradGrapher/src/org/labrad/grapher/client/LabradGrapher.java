package org.labrad.grapher.client;

import java.util.ArrayList;
import java.util.List;

import org.labrad.grapher.client.images.Images;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LabradGrapher implements EntryPoint, ValueChangeHandler<String> {
  private final DataVaultServiceAsync datavaultService = GWT.create(DataVaultService.class);
  private final Images images = GWT.create(Images.class);
  
  private List<String> path = new ArrayList<String>();
  
  private final HorizontalPanel container = new HorizontalPanel();
  
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {

    VerticalPanel page = new VerticalPanel();
    page.add(new BreadcrumbView());
    page.add(container);
    
    RootPanel.get().add(page);
    
    //updateListing();
    History.addValueChangeHandler(this);
    String token = History.getToken();
    if (token == null || token.isEmpty()) {
      History.newItem(TokenParser.pathToToken(path), false);
    } else {
      path = TokenParser.tokenToPath(token);
    }
    History.fireCurrentHistoryState();
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    List<String> newPath = TokenParser.tokenToPath(token);
    if (!token.endsWith("/")) {
      int newName = Integer.parseInt(newPath.remove(newPath.size() - 1));
      container.clear();
      container.add(new DatasetView(newPath, newName, datavaultService));
    } else {
      container.clear();
      container.add(new DirectoryView(newPath, datavaultService));
    }
  }
}
