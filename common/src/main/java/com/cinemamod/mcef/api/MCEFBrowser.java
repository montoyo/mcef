package com.cinemamod.mcef.api;

import net.minecraft.client.Minecraft;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefBrowserOsr;
import org.cef.browser.CefRequestContext;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;

public class MCEFBrowser extends CefBrowserOsr {
    public final MCEFRenderer renderer = new MCEFRenderer(true);

    public MCEFBrowser(CefClient client, String url, boolean transparent, CefRequestContext context) {
        super(client, url, transparent, context);
        Minecraft.getInstance().submit(renderer::initialize);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        renderer.onPaint(buffer, width, height);
    }

    // glfw
//    public void sendKeyPress(int keyCode, int modifiers, long scanCode) {
//        CefKeyEvent e = new CefKeyEvent(keyCode, CefKeyEvent.KEY_PRESS, modifiers, (char) keyCode);
//        e.scancode = scanCode;
//        sendKeyEvent(e);
//    }
//
//    public void sendKeyRelease(int keyCode, int modifiers, long scanCode) {
//        CefKeyEvent e = new CefKeyEvent(keyCode, CefKeyEvent.KEY_RELEASE, modifiers, (char) keyCode);
//        e.scancode = scanCode;
//        sendKeyEvent(e);
//    }
//
//    public void sendKeyTyped(char c, int modifiers) {
//        CefKeyEvent e = new CefKeyEvent(c, CefKeyEvent.KEY_TYPE, modifiers, c);
//        sendKeyEvent(e);
//    }
//
//    public void sendMouseMove(int mouseX, int mouseY) {
//        CefMouseEvent e = new CefMouseEvent(503, mouseX, mouseY, CefMouseEvent.BUTTON1_MASK, 0, 0);
//        sendMouseEvent(e);
//    }
//
//    public void sendMousePress(int mouseX, int mouseY, int button) {
//        CefMouseEvent e = new CefMouseEvent(GLFW_PRESS, mouseX, mouseY, CefMouseEvent.BUTTON1_MASK, 1, button);
//        sendMouseEvent(e);
//    }
//
//    public void sendMouseRelease(int mouseX, int mouseY, int button) {
//        CefMouseEvent e = new CefMouseEvent(GLFW_RELEASE, mouseX, mouseY, CefMouseEvent.BUTTON1_MASK, 1, button);
//        sendMouseEvent(e);
//    }
//
//    public void sendMouseWheel(int mouseX, int mouseY, int mods, int amount, int rotation) {
//        CefMouseWheelEvent e = new CefMouseWheelEvent(CefMouseWheelEvent.WHEEL_UNIT_SCROLL, amount, mouseX, mouseY, mods);
//        sendMouseWheelEvent(e);
//    }

    // awt
    class CefKeyEvent extends KeyEvent {
        private long scancode = 0; // https://github.com/CinemaMod/java-cef/blob/6f9ddcb78228fdaac0eacba04f905a6aa97cff9f/native/CefBrowser_N.cpp#L1625

        public CefKeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar, int keyLocation) {
            super(source, id, when, modifiers, keyCode, keyChar, keyLocation);
        }

        public CefKeyEvent(Component source, int id, long when, int modifiers, int keyCode, char keyChar, long scanCode) {
            super(source, id, when, modifiers, keyCode, keyChar);
            this.scancode = scanCode;
        }
    }

    public void sendKeyPress(int keyCode, int modifiers, long scanCode) {
        CefKeyEvent keyEvent = new CefKeyEvent(dummyComponent,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                modifiers,
                keyCode,
                KeyEvent.CHAR_UNDEFINED,
                scanCode);
        sendKeyEvent(keyEvent);
    }

    public void sendKeyRelease(int keyCode, int modifiers, long scanCode) {
        CefKeyEvent keyEvent = new CefKeyEvent(dummyComponent,
                KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                modifiers,
                keyCode,
                KeyEvent.CHAR_UNDEFINED,
                scanCode);
        sendKeyEvent(keyEvent);
    }

    public void sendKeyTyped(char c, int modifiers) {
        KeyEvent keyEvent = new KeyEvent(dummyComponent,
                KeyEvent.KEY_TYPED,
                System.currentTimeMillis(),
                modifiers,
                KeyEvent.VK_UNDEFINED,
                c);
        sendKeyEvent(keyEvent);
    }

    public void sendMouseMove(int mouseX, int mouseY) {
        MouseEvent mouseEvent = new MouseEvent(dummyComponent,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                MouseEvent.BUTTON1_DOWN_MASK, // Allow for mouse dragging
                mouseX,
                mouseY,
                0,
                false);
        sendMouseEvent(mouseEvent);
    }

    public void sendMousePress(int mouseX, int mouseY, int button) {
        MouseEvent mouseEvent = new MouseEvent(dummyComponent,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                mouseX,
                mouseY,
                1,
                false,
                button + 1);
        sendMouseEvent(mouseEvent);
    }

    public void sendMouseRelease(int mouseX, int mouseY, int button) {
        MouseEvent mouseEvent = new MouseEvent(dummyComponent,
                MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                0,
                mouseX,
                mouseY,
                1,
                false,
                button + 1);
        sendMouseEvent(mouseEvent);

        mouseEvent = new MouseEvent(dummyComponent,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                mouseX,
                mouseY,
                1,
                false,
                button + 1);
        sendMouseEvent(mouseEvent);
    }

    public void sendMouseWheel(int mouseX, int mouseY, int mods, int amount, int rotation) {
        MouseWheelEvent mouseWheelEvent = new MouseWheelEvent(dummyComponent,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                mods,
                mouseX,
                mouseY,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                amount,
                rotation);
        sendMouseWheelEvent(mouseWheelEvent);
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
        Minecraft.getInstance().submit(renderer::cleanup);
        super.finalize();
    }
}
