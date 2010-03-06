package org.labrad.grapher.client.images;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Images extends ClientBundle {
  /**
   * Server information.
   */
  @Source("org/labrad/grapher/client/images/dataset.png")
  public ImageResource datasetIcon();

  /**
   * Server information (disabled).
   */
  @Source("org/labrad/grapher/client/images/dir.png")
  public ImageResource directoryIcon();
}
