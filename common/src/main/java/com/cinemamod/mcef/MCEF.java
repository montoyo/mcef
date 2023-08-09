package com.cinemamod.mcef;

import net.minecraft.client.Minecraft;
import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public final class MCEF {
    private static MCEFApp app;
    private static MCEFClient client;

    static {
        if (CefUtil.init()) {
            app = new MCEFApp(CefUtil.getCefApp());
            client = new MCEFClient(CefUtil.getCefClient());
        }
    }

    public static MCEFApp getApp() {
        assertInitialized();
        return app;
    }

    public static MCEFClient getClient() {
        assertInitialized();
        return client;
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, null);
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent, int width, int height) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, null);
        browser.setCloseAllowed();
        browser.createImmediately();
        browser.resize(width, height);
        return browser;
    }

    public static boolean isInitialized() {
        return client != null;
    }

    private static void assertInitialized() {
        if (!isInitialized())
            throw new RuntimeException("Chromium Embedded Framework was never initialized.");
    }
    
    private static HashMap<Integer, Long> CURSORS = new HashMap<>();
    
    /**
     * gets the glfw cursor handle for the given {@link CefCursorType} id
     * @param type the id of the type, should match with the ordinal of the {@link CefCursorType} you want to use
     * @return the glfw cursor handle that can be used with {@link GLFW#glfwSetCursor(long, long)}
     */
    public static long getCursor(int type) {
        if (CURSORS.containsKey(type)) return CURSORS.get(type);
        
        long cur = GLFW.glfwCreateStandardCursor(type);
        CURSORS.put(type, cur);
        return cur;
    }
    
    public static final Consumer<Integer> setGlfwCursor = (cursor) -> {
        CefCursorType type = CefCursorType.fromId(cursor);
        
        if (type == CefCursorType.NONE) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            return;
        } else GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        
        int c = type.glfwId;
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), getCursor(c));
    };
}
