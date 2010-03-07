package org.labrad.grapher.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

public class DatasetView extends Composite {
  private List<String> path;
  private int num;
  private DatasetInfo info;
  private final VerticalPanel panel = new VerticalPanel();
  private final DataVaultServiceAsync service;
  
  public DatasetView(List<String> path, int num, DataVaultServiceAsync service) {
    this.path = path;
    this.num = num;
    
    initWidget(panel);
    
    this.service = service;
    this.service.getDatasetInfo(path, num, new AsyncCallback<DatasetInfo>() {

      @Override
      public void onFailure(Throwable caught) {
        DialogBox msg = new DialogBox();
        msg.setText("Error occurred while grabbing directory listing:\n\n" + caught.getMessage());
        msg.show();
      }

      @Override
      public void onSuccess(DatasetInfo result) {
        handleDatasetInfo(result);
      }
      
    });
  }
  
  public void handleDatasetInfo(DatasetInfo info) {
    this.info = info;
    
    panel.clear();
    panel.add(new Label(info.getName().toString()));
    panel.add(new Label(info.getIndependents().toString()));
    panel.add(new Label(info.getDependents().toString()));
    
    Map<String, String> params = info.getParameters();
    Grid t = new Grid(params.size(), 2);
    int i = 0;
    List<String> keyList = new ArrayList<String>(params.keySet());
    String[] keys = new String[keyList.size()];
    for (int j = 0; j < keyList.size(); j++) {
      keys[j] = keyList.get(j);
    }
    Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
    for (String key : keys) {
      t.setText(i, 0, key);
      t.setText(i, 1, params.get(key));
      i++;
    }
    panel.add(t);
    
    service.getData(path, info.getNum(), new AsyncCallback<double[][]>() {

      @Override
      public void onFailure(Throwable caught) {
        DialogBox msg = new DialogBox();
        msg.setText("Error occurred while grabbing data:\n\n" + caught.getMessage());
        msg.show();
      }

      @Override
      public void onSuccess(double[][] result) {
        handleData(result);
      }
      
    });
  }
  
  public void handleData(double[][] data) {
    GWTCanvas canvas = new GWTCanvas(800, 600);
    
    canvas.setLineWidth(1);
    canvas.setStrokeStyle(Color.RED);
    
    canvas.beginPath();
    canvas.moveTo(10, 10);
    canvas.lineTo(100, 10);
    canvas.lineTo(100, 100);
    canvas.lineTo(10, 100);
    canvas.closePath();
    canvas.stroke();
    panel.add(canvas);
  }
}
