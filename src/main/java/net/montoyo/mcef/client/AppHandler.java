package net.montoyo.mcef.client;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.utilities.Log;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

public class AppHandler extends CefAppHandlerAdapter {
	
	public static final String scheme = "mod";

	public AppHandler() {
		super(new String[] {});
	}
	
	@Override
	public void onRegisterCustomSchemes(CefSchemeRegistrar reg) {
		if(reg.addCustomScheme(scheme, true, false, false))
			Log.info("Scheme mod:// registered.");
		else
			Log.error("Scheme mod:// FAILED to register.");
	}
	
	@Override
	public void onContextInitialized() {
		((ClientProxy) MCEF.PROXY).getCefApp().registerSchemeHandlerFactory(scheme, "", new SchemeHandlerFactory());
	}
	
	private static class SchemeHandlerFactory implements CefSchemeHandlerFactory {

		@Override
		public CefResourceHandler create(CefBrowser browser, String schemeName, CefRequest request) {
			if(schemeName.equals(scheme))
				return new ModScheme();
			
			return null;
		}
		
	}

}
