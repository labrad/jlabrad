package org.labrad.grapher.client;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;

public class BreadcrumbView extends Composite {
  private final HorizontalPanel breadcrumbs = new HorizontalPanel();
  
  public BreadcrumbView() {
    initWidget(breadcrumbs);
  }
  
  public void setPath(List<String> path) {
    setPath(path, false);
  }
  
  public void setPath(List<String> path, boolean linkLast) {
    breadcrumbs.clear();
    if (path.size() == 0) {
      breadcrumbs.add(new Label("Data Vault"));
    } else {
      breadcrumbs.add(new Hyperlink("Data Vault", "/"));
    }
    for (int i = 0; i < path.size(); i++) {
      boolean link = linkLast || (i < path.size() - 1);
      breadcrumbs.add(new Separator());
      breadcrumbs.add(new DirectoryItem(path.subList(0, i), path.get(i), link));
    }
  }
  
  public void setDatasetPath(List<String> path, String dataset) {
    setPath(path, true);
    breadcrumbs.add(new Separator());
    breadcrumbs.add(new DatasetItem(path, dataset, false));
  }
  
  private class Separator extends Label {
    public Separator() {
      super(">>");
      this.addStyleDependentName("separator");
    }
  }
}
