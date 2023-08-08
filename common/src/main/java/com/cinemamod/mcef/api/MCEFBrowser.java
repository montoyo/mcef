package com.cinemamod.mcef.api;

import net.minecraft.client.Minecraft;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserOsr;
import org.cef.browser.CefRequestContext;
import org.cef.event.CefKeyEvent;
import org.cef.event.CefMouseEvent;
import org.cef.event.CefMouseWheelEvent;

import java.awt.*;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class MCEFBrowser extends CefBrowserOsr {
    private final MCEFRenderer renderer = new MCEFRenderer(true);

    public MCEFBrowser(CefClient cefClient, String url, boolean transparent, CefRequestContext context) {
        super(cefClient, url, transparent, context);
        Minecraft.getInstance().submit(renderer::initialize); // TODO: remove this
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        renderer.onPaint(buffer, width, height);
    }

    public int getTexture() {
        return renderer.getTextureID();
    }

    public void sendKeyPress(int keyCode, long scanCode, int modifiers) {
        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_PRESS, keyCode, (char) keyCode, modifiers);
        e.scancode = scanCode;
        sendKeyEvent(e);
    }

    public void sendKeyRelease(int keyCode, long scanCode, int modifiers) {
        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_RELEASE, keyCode, (char) keyCode, modifiers);
        e.scancode = scanCode;
        sendKeyEvent(e);
    }

    public void sendKeyTyped(char c, int modifiers) {
        CefKeyEvent e = new CefKeyEvent(CefKeyEvent.KEY_TYPE, c, c, modifiers);
        sendKeyEvent(e);
    }

    int btnMask = 0;

    public void sendMouseMove(int mouseX, int mouseY) {
        CefMouseEvent e = new CefMouseEvent(CefMouseEvent.MOUSE_MOVED, mouseX, mouseY, 0, 0, btnMask);
        sendMouseEvent(e);
    }

    public void sendMousePress(int mouseX, int mouseY, int button) {
        // for some reason, middle and right are swapped in MC
        if (button == 1) button = 2;
        else if (button == 2) button = 1;

        if (button == 0) btnMask |= CefMouseEvent.BUTTON1_MASK;
        else if (button == 1) btnMask |= CefMouseEvent.BUTTON2_MASK;
        else if (button == 2) btnMask |= CefMouseEvent.BUTTON3_MASK;

        CefMouseEvent e = new CefMouseEvent(GLFW_PRESS, mouseX, mouseY, 1, button, btnMask);
        sendMouseEvent(e);
    }

    public void sendMouseRelease(int mouseX, int mouseY, int button) {
        // for some reason, middle and right are swapped in MC
        if (button == 1) button = 2;
        else if (button == 2) button = 1;

        CefMouseEvent e = new CefMouseEvent(GLFW_RELEASE, mouseX, mouseY, 1, button, btnMask);
        sendMouseEvent(e);

        if (button == 0 && (btnMask & CefMouseEvent.BUTTON1_MASK) != 0) btnMask ^= CefMouseEvent.BUTTON1_MASK;
        else if (button == 1 && (btnMask & CefMouseEvent.BUTTON2_MASK) != 0) btnMask ^= CefMouseEvent.BUTTON2_MASK;
        else if (button == 2 && (btnMask & CefMouseEvent.BUTTON3_MASK) != 0) btnMask ^= CefMouseEvent.BUTTON3_MASK;
    }

    public void sendMouseWheel(int mouseX, int mouseY, int amount, int mods) {
        CefMouseWheelEvent e = new CefMouseWheelEvent(CefMouseWheelEvent.WHEEL_UNIT_SCROLL, mouseX, mouseY, amount, mods);
        sendMouseWheelEvent(e);
    }

    public void resize(int width, int height) {
        browser_rect_.setBounds(0, 0, width, height);
        wasResized(width, height);
    }

    public void close() {
        renderer.cleanup();
        super.close(true);
    }

    @Override
    protected void finalize() throws Throwable {
        Minecraft.getInstance().submit(renderer::cleanup); // TODO: remove this
        super.finalize();
    }
}
