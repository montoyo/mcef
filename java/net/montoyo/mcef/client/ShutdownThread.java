package net.montoyo.mcef.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.cef.CefApp;
import org.cef.browser.CefBrowserOsr;

import net.minecraft.client.Minecraft;
import net.montoyo.mcef.utilities.Log;

public class ShutdownThread extends Thread {
	
	private Field running = null;
	private Minecraft mc = Minecraft.getMinecraft();
	private ArrayList<CefBrowserOsr> browsers = null;
	
	public ShutdownThread(ArrayList<CefBrowserOsr> b) {
		super("MCEF-Shutdown");
		browsers = b;
		
		try {
			Field[] fields = Minecraft.class.getDeclaredFields();
			
			for(Field f: fields) {
				if(f.getType().equals(Boolean.TYPE)) {
					//Log.info("Minecraft.%s: %s", f.getName(), Modifier.toString(f.getModifiers()));

					if(f.getModifiers() == Modifier.VOLATILE) {
						f.setAccessible(true);
						running = f;
						Log.info("volatile boolean Minecraft.running => %s", f.getName());
						break;
					}
				}
			}
		} catch(Throwable t) {
			Log.warning("Can't detect Minecraft shutdown:");
			t.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		if(running == null)
			return;
		
		Log.info("Minecraft shutdown detection thread started.");
		
		while(true) {
			try {
				if(!running.getBoolean(mc))
					break;
			} catch(Throwable t) {
				Log.warning("Can't detect Minecraft shutdown:");
				t.printStackTrace();
				return;
			}
			
			try {
				sleep(100);
			} catch(Throwable t) {}
		}
		
		Log.info("Shutting down JCEF...");
		CefBrowserOsr.CLEANUP = false; //Workaround
		
		for(CefBrowserOsr b: browsers)
			b.close();
		
		browsers.clear();
		System.gc();
		
		try {
			//Yea sometimes, this is needed for some reasons.
			sleep(100);
		} catch(Throwable t) {}
		CefApp.getInstance().dispose();
	}

}
