package org.labrad.grapher.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.labrad.grapher.client.images.Images;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class LabradGrapher implements EntryPoint, ValueChangeHandler<String> {
  private final DataVaultServiceAsync datavaultService = GWT.create(DataVaultService.class);
  private final Images images = GWT.create(Images.class);
  
  private List<String> path = new ArrayList<String>();
  private int name = 0;
  private boolean pathLoaded = false;
  private boolean datasetLoaded = false;
  
  private final Tree browser = new Tree();
  private final HorizontalPanel breadcrumbs = new HorizontalPanel();
  private final VerticalPanel infoArea = new VerticalPanel();
  
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
//    browser.addOpenHandler(new OpenHandler<TreeItem>() {
//
//      @Override
//      public void onOpen(OpenEvent<TreeItem> event) {
//        String dir = event.getTarget().getText();
//        if (dir.equals("...")) {
//          path.remove(path.size() - 1);
//        } else {
//          path.add(event.getTarget().getText());
//        }
//        //updateListing();
//        History.newItem(pathToToken(path));
//      }
//      
//    });
    
    VerticalPanel navigator = new VerticalPanel();
    navigator.add(breadcrumbs);
    navigator.add(browser);
    
    HorizontalPanel container = new HorizontalPanel();
    container.add(navigator);
    container.add(infoArea);

    RootPanel.get().add(container);
    
    //updateListing();
    History.addValueChangeHandler(this);
    String token = History.getToken();
    if (token == null || token.isEmpty()) {
      History.newItem(pathToToken(path), false);
    } else {
      path = tokenToPath(token);
    }
    History.fireCurrentHistoryState();
  }
  
  private void updateListing() {
    datavaultService.getListing(path, new AsyncCallback<DirectoryListing>() {

      @Override
      public void onFailure(Throwable caught) {
        DialogBox msg = new DialogBox();
        msg.setText("Error occurred while grabbing directory listing:\n\n" + caught.getMessage());
        msg.show();
      }

      @Override
      public void onSuccess(DirectoryListing result) {
        browser.removeItems();
        
        TreeItem it;
        //TreeItem it = new TreeItem(new BackItem(path));
        //if (path.size() > 0) {
        //  it.addItem("");
        //  browser.addItem(it);
        //}
        
        for (String dir : result.getDirs()) {
          it = new TreeItem(new DirectoryItem(path, dir));
          //it.addItem("");
          browser.addItem(it);
        }
        
        for (String dataset : result.getDatasets()) {
          int num = Integer.parseInt(dataset.split(" - ")[0]);
          browser.addItem(new DatasetItem(path, dataset, num));
        }
        
        breadcrumbs.clear();
        breadcrumbs.add(new Hyperlink("root", "/"));
        breadcrumbs.add(new Label(" >> "));
        for (int i = 0; i < path.size() - 1; i++) {
          String addr = directoryLink(path.subList(0, i+1));
          breadcrumbs.add(new Hyperlink(path.get(i), addr));
          breadcrumbs.add(new Label(" >> "));
        }
        breadcrumbs.add(new Label(path.get(path.size() - 1)));
        
        pathLoaded = true;
      }
    });
  }
  
  private class BackItem extends Composite {
    public BackItem(List<String> path) {
      path = new ArrayList<String>(path);
      if (path.size() > 0) {
        path.remove(path.size() - 1);
      }
      initWidget(new Hyperlink("<<<", directoryLink(path)));
    }
  }
  
  private class DirectoryItem extends Composite {
    public DirectoryItem(List<String> path, String dir) {
      path = new ArrayList<String>(path);
      path.add(dir);
      HorizontalPanel p = new HorizontalPanel();
      p.add(new Image(images.directoryIcon()));
      p.add(new Hyperlink(dir, directoryLink(path)));
      initWidget(p);
    }
  }
  
  private class DatasetItem extends Composite {
    public DatasetItem(List<String> path, String name, int num) {
      HorizontalPanel p = new HorizontalPanel();
      p.add(new Image(images.datasetIcon()));
      p.add(new Hyperlink(name, datasetLink(path, num)));
      initWidget(p);
    }
  }
  
  private void getDatasetInfo(int dataset) {
    if (dataset == 0) {
      infoArea.clear();
      return;
    }
    datavaultService.getDatasetInfo(path, dataset, new AsyncCallback<DatasetInfo>() {

      @Override
      public void onFailure(Throwable caught) {
        DialogBox msg = new DialogBox();
        msg.setText("Error occurred while grabbing directory listing:\n\n" + caught.getMessage());
        msg.show();
      }

      @Override
      public void onSuccess(DatasetInfo result) {
        infoArea.clear();
        
        infoArea.add(new Label(result.getPath().toString()));
        infoArea.add(new Label(result.getName().toString()));
        infoArea.add(new Label(result.getIndependents().toString()));
        infoArea.add(new Label(result.getDependents().toString()));
        
        Grid t = new Grid(result.getParameters().size(), 2);
        int i = 0;
        for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
          t.setText(i, 0, entry.getKey());
          t.setText(i, 1, entry.getValue());
          i++;
        }
        infoArea.add(t);
        datasetLoaded = true;
      }
      
    });
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    List<String> newPath = tokenToPath(token);
    int newName = 0;
    if (!token.endsWith("/")) {
      newName = Integer.parseInt(newPath.remove(newPath.size() - 1));
    }
    boolean pathChanged = false;
    if (path.size() != newPath.size()) {
      pathChanged = true;
    } else {
      for (int i = 0; i < path.size(); i++) {
        if (!path.get(i).equals(newPath.get(i))) {
          pathChanged = true;
          break;
        }
      }
    }
    boolean nameChanged = false;
    if (newName != name) {
      nameChanged = true;
    }
    this.path = newPath;
    this.name = newName;
    updateListing();
    getDatasetInfo(this.name);
    
  }
  
  private String directoryLink(List<String> path) {
    return pathToToken(path);
  }
  
  private String datasetLink(List<String> path, int name) {
    String numStr = "" + name;
    if (name < 10) numStr = "0000" + name;
    else if (name < 100) numStr = "000" + name;
    else if (name < 1000) numStr = "00" + name;
    else if (name < 10000) numStr = "0" + name;
    return pathToToken(path) + numStr;
  }
  
  private String pathToToken(List<String> path) {
    StringBuilder sb = new StringBuilder("/");
    for (String segment : path) {
      sb.append(URL.encodeComponent(segment));
      sb.append("/");
    }
    return sb.toString();
  }
  
  private List<String> tokenToPath(String token) {
    String[] segments = token.split("/");
    List<String> path = new ArrayList<String>();
    for (String segment : segments) {
      segment = URL.decodeComponent(segment);
      if (!segment.isEmpty()) {
        path.add(URL.decodeComponent(segment));
      }
    }
    return path;
  }
}
