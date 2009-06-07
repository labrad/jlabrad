package org.labrad.browser.client;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

public interface NodeImageBundle extends ImageBundle {
	/**
     * Server information.
     */
	@Resource("org/labrad/browser/images/information.gif")
    public AbstractImagePrototype serverInfoIcon();
    
    /**
     * Server information (disabled).
     */
    @Resource("org/labrad/browser/images/information_gray.gif")
    public AbstractImagePrototype serverInfoIconDisabled();
    
	/**
	 * Start a server.
	 */
    @Resource("org/labrad/browser/images/add.gif")
	public AbstractImagePrototype startServerIcon();

	/**
	 * Start a server (button disabled).
	 */
    @Resource("org/labrad/browser/images/add_gray.gif")
	public AbstractImagePrototype startServerIconDisabled();
	

	/**
	 * Restart a server.
	 */
    @Resource("org/labrad/browser/images/arrow_refresh.gif")
	public AbstractImagePrototype restartServerIcon();

	/**
	 * Restart a server (button disabled).
	 */
    @Resource("org/labrad/browser/images/arrow_refresh_gray.gif")
	public AbstractImagePrototype restartServerIconDisabled();

	
	/**
	 * Stop a server.
	 */
    @Resource("org/labrad/browser/images/delete.gif")
	public AbstractImagePrototype stopServerIcon();

	/**
	 * Stop a server (button disabled).
	 */
    @Resource("org/labrad/browser/images/delete_gray.gif")
	public AbstractImagePrototype stopServerIconDisabled();
    
    /**
     * IP address on the white list
     */
    @Resource("org/labrad/browser/images/tick.gif")
    public AbstractImagePrototype ipAllowed();
    
    /**
     * IP address on the black list
     */
    @Resource("org/labrad/browser/images/cross.gif")
    public AbstractImagePrototype ipDisallowed();
}
