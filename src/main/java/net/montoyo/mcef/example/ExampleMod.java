package net.montoyo.mcef.example;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.montoyo.mcef.utilities.Log;

import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IDisplayHandler;
import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.mcef.api.IJSQueryHandler;
import net.montoyo.mcef.api.MCEFApi;
import org.lwjgl.glfw.GLFW;

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

    public ScreenCfg hudBrowser = null;
    private KeyBinding key = new KeyBinding("Open Browser", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F10, "key.categories.misc");
    private MinecraftClient mc = MinecraftClient.getInstance();
    private BrowserScreen backup = null;
    private API api;

    public API getAPI() {
        return api;
    }

    public void onPreInit() {
        //Grab the API and make sure it isn't null.
        api = MCEFApi.getAPI();
        if(api == null)
            return;

        api.registerScheme("mod", ModScheme.class, true, false, false, true, true, false, false);
    }
    
    public void onInit() {
        INSTANCE = this;
        
        // Register key binding via fabric api
        KeyBindingHelper.registerKeyBinding(key);
        // We used to register to event bus here
        if(api != null) {
            //Register this class to handle onAddressChange and onQuery events
            api.registerDisplayHandler(this);
            api.registerJSQueryHandler(this);
        }

        ClientTickEvents.START_CLIENT_TICK.register(client -> this.onTickStart());
    }
    
    public void setBackup(BrowserScreen bu) {
        backup = bu;
    }
    
    public boolean hasBackup() {
        return (backup != null);
    }
    
    public void showScreen(String url) {
        if(mc.currentScreen instanceof BrowserScreen)
            ((BrowserScreen) mc.currentScreen).loadURL(url);
        else if(hasBackup()) {
            mc.setScreen(backup);
            backup.loadURL(url);
            backup = null;
        } else
            mc.setScreen(new BrowserScreen(url));
    }
    
    public IBrowser getBrowser() {
        if(mc.currentScreen instanceof BrowserScreen)
            return ((BrowserScreen) mc.currentScreen).browser;
        else if(backup != null)
            return backup.browser;
        else
            return null;
    }
    
    public void onTickStart() {
        // Check if our key was pressed
        if(key.isPressed() && !(mc.currentScreen instanceof BrowserScreen)) {
            //Display the web browser UI.
            mc.setScreen(hasBackup() ? backup : new BrowserScreen());
            backup = null;
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
    public void onTooltip(IBrowser browser, String text) {
    }

    @Override
    public void onStatusMessage(IBrowser browser, String value) {
    }

    @Override
    public boolean handleQuery(IBrowser b, long queryId, String query, boolean persistent, IJSQueryCallback cb) {
        if(b != null && query.equalsIgnoreCase("username")) {
            if(b.getURL().startsWith("mod://")) {
                //Only allow MCEF URLs to get the player's username to keep his identity secret

                mc.executeTask(() -> {
                    //Add this to a scheduled task because this is NOT called from the main Minecraft thread...

                    try {
                        String name = mc.getSession().getUsername();
                        cb.success(name);
                    } catch(Throwable t) {
                        cb.failure(500, "Internal error.");
                        Log.warning("Could not get username from JavaScript:");
                        t.printStackTrace();
                    }
                });
            } else
                cb.failure(403, "Can't access username from external page");
            
            return true;
        }
        
        return false;
    }

    @Override
    public void cancelQuery(IBrowser b, long queryId) {
    }

    /*@SubscribeEvent
    public void onDrawHUD(RenderGameOverlayEvent.Post ev) {
        if(hudBrowser != null)
            hudBrowser.drawScreen(0, 0, 0.f);
    }*/

}
