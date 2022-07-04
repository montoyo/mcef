// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IStringVisitor;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.client.StringVisitor;
import net.montoyo.mcef.client.UnsafeExample;
import net.montoyo.mcef.utilities.Log;
import org.apache.commons.lang3.NotImplementedException;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.awt.event.KeyEvent.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * This class represents an off-screen rendered browser.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
public class CefBrowserOsr extends CefBrowser_N implements CefRenderHandler, IBrowser {
    private CefRenderer renderer_;
    private long window_handle_ = 0;
    private boolean justCreated_ = false;
    private Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
    private Point screenPoint_ = new Point(0, 0);
    private double scaleFactor_ = 1.0;
    private int depth = 32;
    private int depth_per_component = 8;
    private boolean isTransparent_;
    private final Component dc_ = new Component() {
        @Override
        public Point getLocationOnScreen() {
            return new Point(0, 0);
        }
    };
    private MouseEvent lastMouseEvent = new MouseEvent(dc_, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false);
    public static boolean CLEANUP = true;

    CefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context) {
        this(client, url, transparent, context, null, null);
    }

    private CefBrowserOsr(CefClient client, String url, boolean transparent,
                          CefRequestContext context, CefBrowserOsr parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt);
        isTransparent_ = transparent;
        renderer_ = new CefRenderer(transparent);
    }

    @Override
    public void createImmediately() {
        justCreated_ = true;
        // Create the browser immediately.
        createBrowserIfRequired(false);
    }

    @Override
    public Component getUIComponent() {
        return dc_;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
                                                 CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new CefBrowserOsr(
                client, url, isTransparent_, context, this, inspectAt);
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browser_rect_;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point screenPoint = new Point(screenPoint_);
        screenPoint.translate(viewPoint.x, viewPoint.y);
        return screenPoint;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            renderer_.clearPopupRects();
            invalidate();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        renderer_.onPopupSize(size);
    }

    @Override
    public void close() {
        if (CLEANUP) {
            ((ClientProxy) MCEF.PROXY).removeBrowser(this);
            renderer_.cleanup();
        }

        super.close(true); //true to ignore confirmation popups
    }

    @Override
    public void resize(int width, int height) {
        browser_rect_.setBounds(0, 0, width, height);
        dc_.setBounds(browser_rect_);
        dc_.setVisible(true);
        wasResized(width, height);
    }

    @Override
    public void draw(PoseStack matrixStack, double x1, double y1, double x2, double y2) {
        renderer_.render(matrixStack, x1, y1, x2, y2);
    }

    @Override
    public int getTextureID() {
        return renderer_.texture_id_[0];
    }

    @Override
    public void injectMouseMove(int x, int y, int mods, boolean left) {
        //FIXME: 'left' is not used as it causes bugs since MCEF 1.11

        MouseEvent ev = new MouseEvent(dc_, MouseEvent.MOUSE_MOVED, 0, mods, x, y, 0, false);
        lastMouseEvent = ev;
        sendMouseEvent(ev);
    }

    @Override
    public void injectMouseButton(int x, int y, int mods, int btn, boolean pressed, int ccnt) {
        MouseEvent ev = new MouseEvent(dc_, pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED, 0, mods, x, y, ccnt, false, btn);
        sendMouseEvent(ev);
    }

    @Override
    public void injectKeyTyped(int key, int mods) {
        if( key != GLFW_KEY_BACKSPACE && key != VK_UNDEFINED) {
            KeyEvent ev = new UnsafeExample().makeEvent(dc_, key, (char) key, KEY_LOCATION_UNKNOWN, KEY_TYPED, 0, mods);
            sendKeyEvent(ev);
        } else {
            switch (key) {
                case GLFW_KEY_HOME, VK_END, VK_PAGE_UP, VK_PAGE_DOWN, VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT, VK_BEGIN, VK_KP_LEFT, VK_KP_UP, VK_KP_RIGHT, VK_KP_DOWN, VK_F1, VK_F2, VK_F3, VK_F4, VK_F5, VK_F6, VK_F7, VK_F8, VK_F9, VK_F10, VK_F11, VK_F12, VK_F13, VK_F14, VK_F15, VK_F16, VK_F17, VK_F18, VK_F19, VK_F20, VK_F21, VK_F22, VK_F23, VK_F24, VK_PRINTSCREEN, VK_SCROLL_LOCK, VK_CAPS_LOCK, VK_NUM_LOCK, VK_PAUSE, GLFW_KEY_INSERT, GLFW_KEY_BACKSPACE -> {
                    KeyEvent ev = new UnsafeExample().makeEvent(dc_, key, '\0', KEY_LOCATION_UNKNOWN, KEY_TYPED,0, mods);
                    sendKeyEvent(ev);
                }
            }
        }
    }

    public static int remapKeycode(int kc, char c) {
        switch (kc) {
            // Unable to remap
            case GLFW_KEY_BACKSPACE:
                return KeyEvent.VK_BACK_SPACE;
            case GLFW_KEY_DELETE:
                return KeyEvent.VK_DELETE;
            case GLFW_KEY_DOWN:
                return KeyEvent.VK_DOWN;
            case GLFW_KEY_ENTER:
                return KeyEvent.VK_ENTER;
            case GLFW_KEY_ESCAPE:
                return KeyEvent.VK_ESCAPE;
            case GLFW_KEY_LEFT:
                return KeyEvent.VK_LEFT;
            case GLFW_KEY_RIGHT:
                return KeyEvent.VK_RIGHT;
            case GLFW_KEY_TAB:
                return KeyEvent.VK_TAB;
            case GLFW_KEY_UP:
                return KeyEvent.VK_UP;
            case GLFW_KEY_PAGE_UP:
                return KeyEvent.VK_PAGE_UP;
            case GLFW_KEY_PAGE_DOWN:
                return KeyEvent.VK_PAGE_DOWN;
            case GLFW_KEY_END:
                return GLFW_KEY_END;
            case GLFW_KEY_HOME:
                return GLFW_KEY_HOME;
            default:
                return c;
        }
    }

    @Override
    public void injectKeyPressedByKeyCode(int keyCode, char c, int mods) {
        if (c != '\0') {
            synchronized (WORST_HACK) {
                WORST_HACK.put(keyCode, c);
            }
        }

        KeyEvent ev = new KeyEvent(dc_, KeyEvent.KEY_PRESSED, 0, mods, remapKeycode(keyCode, c), c);
        sendKeyEvent(ev);
    }

    private static final Map<Integer, Character> WORST_HACK = new HashMap<>();

    @Override
    public void injectKeyReleasedByKeyCode(int keyCode, char c, int mods) {
        if (c == '\0') {
            synchronized (WORST_HACK) {
                c = WORST_HACK.getOrDefault(keyCode, '\0');
            }
        }

        KeyEvent ev = new KeyEvent(dc_, KeyEvent.KEY_RELEASED, 0, mods, remapKeycode(keyCode, c), c);
        sendKeyEvent(ev);
    }

    @Override
    public void injectMouseWheel(int x, int y, int mods, int amount, int rot) {
        MouseWheelEvent ev = new MouseWheelEvent(dc_, MouseEvent.MOUSE_WHEEL, 0, mods, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, amount, rot);
        sendMouseWheelEvent(ev);
    }

    @Override
    public void runJS(String script, String frame) {
        executeJavaScript(script, frame, 0);
    }

    @Override
    public void visitSource(IStringVisitor isv) {
        getSource(new StringVisitor(isv));
    }

    @Override
    public boolean isPageLoading() {
        return isLoading();
    }

    private static class PaintData {
        private ByteBuffer buffer;
        private int width;
        private int height;
        private Rectangle[] dirtyRects;
        private boolean hasFrame;
        private boolean fullReRender;
    }

    private final PaintData paintData = new PaintData();

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        if (popup) {
            return;
        }

        final int size = (width * height) << 2;

        synchronized (paintData) {
            if (buffer.limit() > size)
                Log.warning("Skipping MCEF browser frame, data is too heavy"); //TODO: Don't spam
            else {
                if (paintData.hasFrame) //The previous frame was not uploaded to GL texture, so we skip it and render this on instead
                    paintData.fullReRender = true;

                if (paintData.buffer == null || size != paintData.buffer.capacity()) //This only happens when the browser gets resized
                    paintData.buffer = BufferUtils.createByteBuffer(size);

                BufferUtils.zeroBuffer(paintData.buffer);

                paintData.buffer.position(0);
                paintData.buffer.limit(buffer.limit());
                buffer.position(0);
                paintData.buffer.put(buffer);
                paintData.buffer.position(0);

                paintData.width = width;
                paintData.height = height;
                paintData.dirtyRects = dirtyRects;
                paintData.hasFrame = true;
            }
        }
    }

    public void mcefUpdate() {
        synchronized (paintData) {
            if (paintData.hasFrame) {
                //System.out.println("New frame!");
                renderer_.onPaint(false, paintData.dirtyRects, paintData.buffer, paintData.width, paintData.height, paintData.fullReRender);
                paintData.hasFrame = false;
                paintData.fullReRender = false;
            }else {

            }
        }

        //So sadly this is the only way I could get around the "youtube not rendering video if the mouse doesn't move bug"
        //Even the test browser from the original JCEF library doesn't fix this...
        //What I hope, however, is that it doesn't redraw the entire browser... otherwise I could just call "invalidate"
        sendMouseEvent(lastMouseEvent);
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, final int cursorType) {
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        // TODO(JCEF) Prepared for DnD support using OSR mode.
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        // TODO(JCEF) Prepared for DnD support using OSR mode.
    }

    private void createBrowserIfRequired(boolean hasParent) {
        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), 0, true, isTransparent_,
                        null, getInspectAt());
            } else {
                createBrowser(getClient(), 0, getUrl(), true, isTransparent_, null,
                        getRequestContext());
            }
        } else {
            // OSR windows cannot be reparented after creation.
            setFocus(true);
        }
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        screenInfo.Set(scaleFactor_, depth, depth_per_component, false, browser_rect_.getBounds(),
                browser_rect_.getBounds());

        return true;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        throw new NotImplementedException("createScreenshot not implemented on MCEF");
    }
}
