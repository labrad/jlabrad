package org.labrad.browser.client;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LogMessage extends Composite {
  public LogMessage(String message, String content) {
    Date now = new Date();
    DateTimeFormat format = DateTimeFormat.getMediumDateTimeFormat();
    DisclosurePanel p = new DisclosurePanel(format.format(now) + ": " + message);
    VerticalPanel c = new VerticalPanel();
    for (String line : content.split("[\r\n]")) {
      c.add(new Label(line));
    }
    p.setContent(c);
    initWidget(p);
  }
}
