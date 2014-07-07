package org.labrad.grapher.client;

import java.util.ArrayList;
import java.util.List;

import org.labrad.grapher.client.images.Images;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class DirectoryItem extends Composite {
  private final Images images = GWT.create(Images.class);
  
  public DirectoryItem(List<String> path, String dir) {
    this(path, dir, true);
  }
  
  public DirectoryItem(List<String> path, String dir, boolean link) {
    path = new ArrayList<String>(path);
    path.add(dir);
    HorizontalPanel p = new HorizontalPanel();
    p.add(new Image(images.directoryIcon()));
    if (link) {
      p.add(new Hyperlink(dir, TokenParser.directoryLink(path)));
    } else {
      p.add(new Label(dir));
    }
    initWidget(p);
  }
}
