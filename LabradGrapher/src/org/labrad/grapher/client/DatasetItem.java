package org.labrad.grapher.client;

import java.util.List;

import org.labrad.grapher.client.images.Images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class DatasetItem extends Composite {
  private final Images images = GWT.create(Images.class);
  
  public DatasetItem(List<String> path, String name) {
    this(path, name, true);
  }
  
  public DatasetItem(List<String> path, String name, boolean link) {
    String numStr = name.split(" - ")[0];
    int num = Integer.parseInt(numStr);
    HorizontalPanel p = new HorizontalPanel();
    p.add(new Image(images.datasetIcon()));
    if (link) {
      p.add(new Hyperlink(name, TokenParser.datasetLink(path, num)));
    } else {
      p.add(new Label(numStr));
    }
    initWidget(p);
  }
}
