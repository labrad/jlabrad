package org.labrad.grapher.client;

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class DirectoryView extends Composite {
  private final VerticalPanel directoriesPanel = new VerticalPanel();
  private final VerticalPanel datasetsPanel = new VerticalPanel();
  
  public DirectoryView(final List<String> path, DataVaultServiceAsync service) {
    HorizontalPanel container = new HorizontalPanel();
    container.add(directoriesPanel);
    container.add(datasetsPanel);
    initWidget(container);
    
    service.getListing(path, new AsyncCallback<DirectoryListing>() {

      @Override
      public void onFailure(Throwable caught) {
        final DialogBox msg = new DialogBox();
        msg.setModal(true);
        msg.setText("Error occurred while grabbing directory listing:\n\n" + caught.getMessage());
        Button ok = new Button("OK");
        ok.addClickHandler(new ClickHandler() {
          public void onClick(ClickEvent event) {
            msg.hide();
          }
        });
        msg.setWidget(ok);
        msg.show();
      }

      @Override
      public void onSuccess(DirectoryListing result) {
        directoriesPanel.clear();
        for (String dir : result.getDirs()) {
          directoriesPanel.add(new DirectoryItem(path, dir));
        }
        
        datasetsPanel.clear();
        for (String dataset : result.getDatasets()) {
          datasetsPanel.add(new DatasetItem(path, dataset));
        }
      }
      
    });
  }
}
