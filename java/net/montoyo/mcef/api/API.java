package net.montoyo.mcef.api;

public interface API {
	
	/**
	 * Creates a web view and loads the specified URL.
	 * 
	 * @param url The URL to start from.
	 * @return The created web view, or null if this is run on server.
	 */
	public IBrowser createBrowser(String url);
	
	/**
	 * Registers a display handler.
	 * @param idh The display handler to register.
	 */
	public void registerDisplayHandler(IDisplayHandler idh);
	
	/**
	 * Call this to know if MCEF is in virtual mode.
	 * MCEF switches in virtual mode if something failed to load.
	 * When in virtual mode, createBrowser() will generate fake browsers that does nothing.
	 * 
	 * @return true if MCEF is in virtual mode.
	 */
	public boolean isVirtual();
	
	/**
	 * Opens the example browser UI.
	 * @param url The URL to load.
	 * @see net.montoyo.mcef.example.ExampleMod
	 */
	public void openExampleBrowser(String url);

}
