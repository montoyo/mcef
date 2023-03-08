package net.montoyo.mcef.api;

import net.minecraftforge.eventbus.api.Event;

public class CefInitEvent extends Event {
	private API api = MCEFApi.getAPI();
	private boolean didInit;
	
	public CefInitEvent(boolean didInit) {
		this.didInit = didInit;
	}
	
	public API getApi() {
		return api;
	}
	
	public boolean ranInit() {
		return didInit;
	}
}
