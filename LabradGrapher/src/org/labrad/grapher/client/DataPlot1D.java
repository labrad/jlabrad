package org.labrad.grapher.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

public class DataPlot1D extends Composite {
  public DataPlot1D(DatasetInfo info, double[][] data) {
    GWTCanvas canvas = new GWTCanvas(800, 600);
    
    initWidget(canvas);
  }

}
