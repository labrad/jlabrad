package org.labrad.browser.client;

import java.util.List;

import org.labrad.browser.client.event.NodeRequestFailedException;
import org.labrad.browser.client.event.NodeServerEvent;
import org.labrad.browser.client.event.RemoteEventService;
import org.labrad.browser.client.event.RemoteEventServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;

public class InstanceController extends HorizontalPanel
    implements ClickHandler, AsyncCallback<String> {
  /**
   * Service for starting/stopping servers on the node
   */
  //private final static NodeServiceAsync nodeService = GWT.create(NodeService.class);
  private final static RemoteEventServiceAsync eventService = GWT.create(RemoteEventService.class);

  /**
   * Image bundle for button images, etc.
   */
  private final static NodeImageBundle images = GWT.create(NodeImageBundle.class);

  private final ControlPanel parent;
  private final String node;
  private final String server;
  private String instance;
  private final String version;

  private final Label statusLabel;
  private final PushButton info;
  private final PushButton start;
  private final PushButton stop;
  private final PushButton restart;
  private final DeckPanel controls;

  private boolean isLocal;

  private final static int BUTTONS = 0;
  private final static int THROBBER = 1;

  private enum Color { GRAY, GREEN, RED }
  private Color color;

  private InstanceStatus status;

  //private void showInfoPopup() {
  //  PopupPanel pp = new PopupPanel(true);
  //  RichTextArea rta = new RichTextArea();
  //  rta.setText(version);
  //  pp.add(rta);
  //  pp.setPopupPosition(info.getAbsoluteLeft(), info.getAbsoluteTop());
  //  pp.show();
  //}

  public InstanceController(final ControlPanel parent,
      final String node, final String server, String instance, String version,
      List<String> runningInstances, List<String> environmentVars) {
    this.parent = parent;
    this.node = node;
    this.server = server;
    if (runningInstances.size() > 0) {
      this.instance = runningInstances.get(0);
    } else {
      this.instance = instance;
    }
    this.version = version;
    this.isLocal = environmentVars.size() > 0;
    this.color = Color.GRAY;

    // build widget
    statusLabel = new Label();
    statusLabel.addStyleDependentName("status");

    // build info button
    info = new PushButton(new Image(images.serverInfoIcon()));
    info.getUpDisabledFace().setImage(new Image(images.serverInfoIconDisabled()));
    info.addClickHandler(this);
    info.setEnabled(version != null);
    info.setTitle(version);

    // build control button
    start = new PushButton(new Image(images.startServerIcon()));
    start.getUpDisabledFace().setImage(new Image(images.startServerIconDisabled()));
    start.addClickHandler(this);

    stop = new PushButton(new Image(images.stopServerIcon()));
    stop.getUpDisabledFace().setImage(new Image(images.stopServerIconDisabled()));
    stop.addClickHandler(this);

    restart = new PushButton(new Image(images.restartServerIcon()));
    restart.getUpDisabledFace().setImage(new Image(images.restartServerIconDisabled()));
    restart.addClickHandler(this);

    // put control buttons in a panel
    HorizontalPanel buttons = new HorizontalPanel();
    buttons.add(start);
    buttons.add(stop);
    buttons.add(restart);

    // build a panel to hold the throbber
    VerticalPanel throbber = new VerticalPanel();
    throbber.add(new Throbber());
    throbber.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    // build deck that will show either control buttons or throbber
    controls = new DeckPanel();
    controls.add(buttons);
    controls.add(throbber);

    // put together the whole widget
    add(statusLabel);
    add(info);
    add(controls);

    // set the initial status of the widget
    boolean running = runningInstances.size() > 0;
    setStatus(running ? InstanceStatus.STARTED : InstanceStatus.STOPPED, true);

    // register all event handlers
    registerHandlers();
  }

  /**
   * Keep track of all event registrations so we can remove them later
   */
  private final List<HandlerRegistration> registrations = Util.newArrayList();

  /**
   * Register event handlers for events we care about
   */
  private void registerHandlers() {
    // highlight the row for this server when the user mouses over
    registrations.add(
      this.addDomHandler(new MouseOverHandler() {
        public void onMouseOver(MouseOverEvent event) {
          parent.highlight(server);
        }
      }, MouseOverEvent.getType())
    );

    // unhighlight the row for this server when the user mouses out
    registrations.add(
      this.addDomHandler(new MouseOutHandler() {
        public void onMouseOut(MouseOutEvent event) {
          parent.unhighlight(server);
        }
      }, MouseOutEvent.getType())
    );
  }

  /**
   * Remove all registered event handlers
   */
  public void unregisterHandlers() {
    for (HandlerRegistration reg : registrations) {
      reg.removeHandler();
    }
  }

  /**
   * Handle a server status change event
   * @param e
   * @param status
   */
  public void onStatusEvent(NodeServerEvent e, InstanceStatus status) {
    boolean here = e.getNode().equals(node);
    String inst = e.getInstance();
    if (here && inst != null) {
      instance = inst;
    }
    setStatus(status, here);
  }

  /**
   * Return whether this instance is currently running
   * @return
   */
  public boolean isRunning() {
    return status == InstanceStatus.STARTED;
  }

  /**
   * Whether this is a local server (ie one that can run multiple instances)
   * @return
   */
  public boolean isLocal() {
    return isLocal;
  }

  public String getVersion() {
    return version;
  }

  /**
   * Handle button clicks by invoking the appropriate action on the server
   */
  public void onClick(ClickEvent e) {
    if (e.getSource() == start) {
      //nodeService.startServer(node, server, this);
      eventService.startServer(node, server, this);
    } else if (e.getSource() == restart) {
      //nodeService.restartServer(node, instance, this);
      eventService.restartServer(node, instance, this);
    } else if (e.getSource() == stop) {
      //nodeService.stopServer(node, instance, this);
      eventService.stopServer(node, instance, this);
    } else if (e.getSource() == info) {
      // do nothing for now
    }
  }

  /**
   * Log a failure when a request to start/stop/restart a server fails
   */
  public void onFailure(Throwable caught) {
    if (caught instanceof NodeRequestFailedException) {
      NodeRequestFailedException e = (NodeRequestFailedException)caught;
      LogWindow.log("[Error] failed to " + e.getAction() + " " +
          "server '" + e.getServer() + "' " +
          "on node '" + e.getNode() + "'",
          e.getDetails());
    } else {
      LogWindow.log("[Error] instanceController", caught);
    }
  }

  /**
   * Start/stop/restart requests return the server instance name
   */
  public void onSuccess(String result) {}

  /**
   * Set the widget status in response to state changes
   * @param status
   * @param here
   */
  private void setStatus(InstanceStatus status, boolean here) {
    if (here) {
      switch (status) {
        case STARTING: configure("starting...", Color.RED, false, false, false, THROBBER); break;
        case STARTED: configure("started", Color.GREEN, false, true, true, BUTTONS); break;
        case STOPPING: configure("stopping...", Color.RED, false, false, false, THROBBER); break;
        case STOPPED: configure("stopped", Color.RED, true, false, false, BUTTONS); break;
      }
      this.status = status;
    } else {
      if (isLocal) return;
      switch (status) {
        case STARTING: configure("starting", Color.GRAY, false, false, false, BUTTONS); break;
        case STARTED: configure("started", Color.GRAY, false, false, false, BUTTONS); break;
        case STOPPED: configure("stopped", Color.RED, true, false, false, BUTTONS); break;
      }
    }
  }

  /**
   * Configure the visual appearance of the widget in response to state changes
   * @param state
   * @param newColor
   * @param canStart
   * @param canRestart
   * @param canStop
   * @param buttonPage
   */
  private void configure(String state, Color newColor,
      boolean canStart, boolean canRestart, boolean canStop,
      int buttonPage) {
    // set the status text
    statusLabel.setText(state);

    // remove old color style
    switch (color) {
      case GREEN: statusLabel.removeStyleDependentName("green"); break;
      case RED: statusLabel.removeStyleDependentName("red"); break;
    }
    // add new color style
    switch (newColor) {
      case GREEN: statusLabel.addStyleDependentName("green"); break;
      case RED: statusLabel.addStyleDependentName("red"); break;
    }
    color = newColor;

    // set button state
    start.setEnabled(canStart);
    restart.setEnabled(canRestart);
    stop.setEnabled(canStop);

    // show either buttons or throbber
    controls.showWidget(buttonPage);
  }
}
