package net.montoyo.mcef.api;

import net.minecraftforge.eventbus.api.Event;
import org.cef.CefClient;

public class CefClientCreationEvent extends Event {
	private final CefClient client;
	
	public CefClientCreationEvent(CefClient client) {
		this.client = client;
	}
	
	public CefClient getClient() {
		return client;
	}
}
