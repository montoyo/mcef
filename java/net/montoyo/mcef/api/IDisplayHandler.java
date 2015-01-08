package net.montoyo.mcef.api;

public interface IDisplayHandler {
	
	/**
	 * Handle address changes.
	 * @param browser The browser generating the event.
	 * @param url The new URL.
	 */
	public void onAddressChange(IBrowser browser, String url);
	
	/**
	 * Handle title changes.
	 * @param browser The browser generating the event.
	 * @param title The new title.
	 */
	public void onTitleChange(IBrowser browser, String title);
	
	/**
	 * Called when the browser is about to display a tooltip.
	 *
	 * @param browser The browser generating the event.
	 * @param text Contains the text that will be displayed in the tooltip.
	 * @return To handle the display of the tooltip yourself return true.
	 */
	public boolean onTooltip(IBrowser browser, String text);
	
	/**
	 * Called when the browser receives a status message. 
	 *
	 * @param browser The browser generating the event.
	 * @param value Contains the text that will be displayed in the status message.
	 */
	public void onStatusMessage(IBrowser browser, String value);
	
	/**
	 * Called to display a console message.
	 *
	 * @param browser The browser generating the event.
	 * @param message The console message.
	 * @param source The script source.
	 * @param line The script line.
	 * @return true to stop the message from being output to the console.
	 */
	public boolean onConsoleMessage(IBrowser browser, String message, String source, int line);

}
