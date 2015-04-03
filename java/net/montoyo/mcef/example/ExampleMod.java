package net.montoyo.mcef.example;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IDisplayHandler;
import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.mcef.api.IJSQueryHandler;
import net.montoyo.mcef.api.MCEFApi;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * An example mod that shows you how to use MCEF.
 * Assuming that it is client-side only and that onInit() is called on initialization.
 * This example shows a simple 2D web browser when pressing F6.
 * 
 * @author montoyo
 *
 */
public class ExampleMod implements IDisplayHandler, IJSQueryHandler {
	
	public static ExampleMod INSTANCE;
	
	private KeyBinding key = new KeyBinding("Open Browser", Keyboard.KEY_F10, "key.categories.misc");
	private Minecraft mc = Minecraft.getMinecraft();
	private BrowserScreen backup = null;
	
	public void onInit() {
		INSTANCE = this;
		
		//Register key binding and listen to the FML event bus for ticks.
		ClientRegistry.registerKeyBinding(key);
		FMLCommonHandler.instance().bus().register(this);
		
		//Grab the API and make sure it isn't null.
		API api = MCEFApi.getAPI();
		if(api == null)
			return;
		
		//Register this class to handle onAddressChange and onQuery events
		api.registerDisplayHandler(this);
		api.registerJSQueryHandler(this);
	}
	
	public void setBackup(BrowserScreen bu) {
		backup = bu;
	}
	
	public boolean hasBackup() {
		return (backup != null);
	}
	
	public void showScreen(String url) {
		BrowserScreen scr;
		
		if(mc.currentScreen instanceof BrowserScreen)
			scr = (BrowserScreen) mc.currentScreen;
		else {
			if(hasBackup())
				scr = backup;
			else
				scr = new BrowserScreen();
			
			mc.displayGuiScreen(scr);
			backup = null;
		}
		
		scr.loadURL(url);
	}
	
	public IBrowser getBrowser() {
		if(mc.currentScreen instanceof BrowserScreen)
			return ((BrowserScreen) mc.currentScreen).browser;
		else if(backup != null)
			return backup.browser;
		else
			return null;
	}
	
	@SubscribeEvent
	public void onTick(TickEvent ev) {
		if(ev.phase == TickEvent.Phase.START && ev.side == Side.CLIENT && ev.type == TickEvent.Type.CLIENT) {
			//Check if our key was pressed
			if(key.isPressed() && !(mc.currentScreen instanceof BrowserScreen)) {
				//Display the web browser UI.
				mc.displayGuiScreen(hasBackup() ? backup : new BrowserScreen());
				backup = null;
			}
		}
	}

	@Override
	public void onAddressChange(IBrowser browser, String url) {
		//Called by MCEF if a browser's URL changes. Forward this event to the screen.
		if(mc.currentScreen instanceof BrowserScreen)
			((BrowserScreen) mc.currentScreen).onUrlChanged(browser, url);
		else if(hasBackup())
			backup.onUrlChanged(browser, url);
	}

	@Override
	public void onTitleChange(IBrowser browser, String title) {
	}

	@Override
	public boolean onTooltip(IBrowser browser, String text) {
		return false;
	}

	@Override
	public void onStatusMessage(IBrowser browser, String value) {
	}

	@Override
	public boolean onConsoleMessage(IBrowser browser, String message, String source, int line) {
		return false;
	}

	@Override
	public boolean handleQuery(IBrowser b, long queryId, String query, boolean persistent, IJSQueryCallback cb) {
		if(b == getBrowser() && query.equalsIgnoreCase("username")) {
			try {
				String name = mc.getSession().getUsername();
				cb.success(name);
			} catch(Throwable t) {
				cb.failure(500, "Internal error.");
				t.printStackTrace();
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public void cancelQuery(IBrowser b, long queryId) {
	}

}
