package net.montoyo.mcef.client;

import net.montoyo.mcef.api.IDisplayHandler;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserOsr;
import org.cef.handler.CefDisplayHandler;

public class DisplayHandler implements CefDisplayHandler {
	
	IDisplayHandler dh;
	
	public DisplayHandler(IDisplayHandler idh) {
		dh = idh;
	}

	@Override
	public void onAddressChange(CefBrowser browser, String url) {
		dh.onAddressChange((CefBrowserOsr) browser, url);
	}

	@Override
	public void onTitleChange(CefBrowser browser, String title) {
		dh.onTitleChange((CefBrowserOsr) browser, title);
	}

	@Override
	public boolean onTooltip(CefBrowser browser, String text) {
		return dh.onTooltip((CefBrowserOsr) browser, text);
	}

	@Override
	public void onStatusMessage(CefBrowser browser, String value) {
		dh.onStatusMessage((CefBrowserOsr) browser, value);
	}

	@Override
	public boolean onConsoleMessage(CefBrowser browser, String message, String source, int line) {
		return dh.onConsoleMessage((CefBrowserOsr) browser, message, source, line);
	}

}
