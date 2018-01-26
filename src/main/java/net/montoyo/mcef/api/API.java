package net.montoyo.mcef.api;

public interface API {
    
    /**
     * Creates a web view and loads the specified URL.
     * 
     * @param url The URL to start from.
     * @param transp True is the web view can be transparent
     * @return The created web view, or null if this is run on server.
     */
    IBrowser createBrowser(String url, boolean transp);

    /**
     * Same as {@link #createBrowser(String, boolean) createBrowser} but with transp set to false.
     *
     * @param url The URL to start from.
     * @return The created web view, or null if this is run on server.
     */
    IBrowser createBrowser(String url);
    
    /**
     * Registers a display handler.
     * @param idh The display handler to register.
     * @see IDisplayHandler
     */
    void registerDisplayHandler(IDisplayHandler idh);
    
    /**
     * Registers a JavaScript query handler.
     * @param iqh The JavaScript query handler to register.
     * @see IJSQueryHandler
     */
    void registerJSQueryHandler(IJSQueryHandler iqh);
    
    /**
     * Call this to know if MCEF is in virtual mode.
     * MCEF switches in virtual mode if something failed to load.
     * When in virtual mode, {@link #createBrowser(String, boolean) createBrowser} will generate fake browsers that does nothing.
     * 
     * @return true if MCEF is in virtual mode.
     */
    boolean isVirtual();
    
    /**
     * Opens the example browser UI.
     * @param url The URL to load.
     * @see net.montoyo.mcef.example.ExampleMod
     */
    void openExampleBrowser(String url);

}
