package net.montoyo.mcef;

import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IDisplayHandler;
import net.montoyo.mcef.utilities.Log;

public class BaseProxy implements API {
	
	public void onInit() {
		Log.info("MCEF is running on server. Nothing to do.");
	}

	@Override
	public IBrowser createBrowser(String url) {
		Log.warning("A mod called API.createBrowser() from server! Returning null...");
		return null;
	}

	@Override
	public void registerDisplayHandler(IDisplayHandler idh) {
		Log.warning("A mod called API.registerDisplayHandler() from server!");
	}

	@Override
	public boolean isVirtual() {
		return true;
	}

	@Override
	public void openExampleBrowser(String url) {
		Log.warning("A mod called API.openExampleBrowser() from server! URL: %s", url);
	}

}
