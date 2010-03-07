package org.labrad.grapher.client;

import java.util.List;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;

public class BreadcrumbView extends Composite implements ValueChangeHandler<String> {
  private final HorizontalPanel breadcrumbs = new HorizontalPanel();
  
  public BreadcrumbView() {
    initWidget(breadcrumbs);
    History.addValueChangeHandler(this);
  }
  
  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    String token = event.getValue();
    List<String> path = TokenParser.tokenToPath(token);
    boolean isDirectory = token.endsWith("/");
    
    breadcrumbs.clear();
    if (path.size() == 0) {
      breadcrumbs.add(new Label("Data Vault"));
    } else {
      breadcrumbs.add(new Hyperlink("Data Vault", "/"));
      for (int i = 0; i < path.size(); i++) {
        boolean link = (i < path.size() - 1);
        breadcrumbs.add(new Label(" >> "));
        if (isDirectory || i < path.size() - 1) {
          breadcrumbs.add(new DirectoryItem(path.subList(0, i), path.get(i), link));
        } else {
          breadcrumbs.add(new DatasetItem(path.subList(0, i-1), path.get(i), false));
        }
      }
    }
  }
}
