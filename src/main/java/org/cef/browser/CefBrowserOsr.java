// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
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
    public static final Component dc_ = new Component() {
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
        
        // if I don't have this here, then the text position indicator thing doesn't work
        // using setSafeMode to ensure that it works properly
        setSafeMode(true);
        setFocus(true);
        setSafeMode(false);
    }
    
    public int remapModifiers(int mods) {
        int vkMods = 0;
        if ((mods & GLFW_MOD_CONTROL) != 0) vkMods |= KeyEvent.CTRL_DOWN_MASK;
        if ((mods & GLFW_MOD_ALT) != 0) vkMods |= KeyEvent.ALT_DOWN_MASK;
        if ((mods & GLFW_MOD_SHIFT) != 0) vkMods |= KeyEvent.SHIFT_DOWN_MASK;
        if ((mods & GLFW_MOD_SUPER) != 0) vkMods |= KeyEvent.META_DOWN_MASK;
        return vkMods;
    }
    
    private static final Map<Integer, Character> WORST_HACK = new HashMap<>();
    
    /**
     * Maps keys from GLFW to what CEF expects
     * @param kc the key code
     * @param c the character
     * @param mod any modifiers
     * @return a remapped key code which CEF will accept
     */
    public static int remapKeycode(int kc, char c, int mod) {
        switch (kc) {
            // Unable to remap
            case GLFW_KEY_ESCAPE:
                return KeyEvent.VK_ESCAPE;
            case GLFW_KEY_TAB:
                return KeyEvent.VK_TAB;
            default:
                int ck = getChar(kc, 0, mod);
                if (ck == 0) return c;
                return ck;
        }
    }
    
    /**
     * internal method for {@link CefBrowserOsr#remapKeycode(int, char, int)}
     * @param keyCode the key code
     * @param scanCode (unused)
     * @param mod any modifiers
     * @return a remapped keycode
     */
    public static int getChar(int keyCode, int scanCode, int mod) {
        if (keyCode == GLFW_KEY_LEFT_CONTROL) return '\uFFFF';
        if (keyCode == GLFW_KEY_RIGHT_CONTROL) return '\uFFFF';
        switch (keyCode) {
            case GLFW_KEY_ENTER:
                return 13;
            case GLFW_KEY_SPACE:
                return 32;
            case GLFW_KEY_BACKSPACE:
                return 8;
            case GLFW_KEY_DELETE:
                return '\u007F';
            case GLFW_KEY_LEFT:
            case GLFW_KEY_RIGHT:
            case GLFW_KEY_UP:
            case GLFW_KEY_DOWN:
            case GLFW_KEY_PAGE_DOWN:
            case GLFW_KEY_PAGE_UP:
            case GLFW_KEY_HOME:
            case GLFW_KEY_END:
                return '\uFFFF';
        }
        String keystr = GLFW.glfwGetKeyName(keyCode, scanCode);
        if(keystr == null){
            keystr = "\0";
        } else if(keystr.length() == 0){
            return -1;
        }
//        if((mod & GLFW_MOD_SHIFT) != 0) {
//            keystr = keystr.toUpperCase(Locale.ROOT);
//        }
        return keyCode;
    }
    
    private long mapScanCode(int key, char c) {
        if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) return 29;
        return switch (key) {
            case GLFW_KEY_DELETE -> 83;
            case GLFW_KEY_LEFT -> 75;
            case GLFW_KEY_DOWN -> 80;
            case GLFW_KEY_UP -> 72;
            case GLFW_KEY_RIGHT -> 77;
            case GLFW_KEY_PAGE_DOWN -> 81;
            case GLFW_KEY_PAGE_UP -> 73;
            case GLFW_KEY_END -> 79;
            case GLFW_KEY_HOME -> 71;
            case GLFW_KEY_ENTER -> 28;
            default -> GLFW.glfwGetKeyScancode(key);
        };
    }
    
    @Override
    public void injectKeyPressedByKeyCode(int key, char c, int mods) {
        if (c != '\0') {
            synchronized (WORST_HACK) {
                WORST_HACK.put(key, c);
            }
        }
    
        // keyboard shortcut handling
        if (mods == GLFW_MOD_CONTROL) {
            if (key == GLFW_KEY_R) {
                reload();
                return;
            } else if (key == GLFW_KEY_EQUAL) {
                if (getZoomLevel() < 9) setZoomLevel(getZoomLevel() + 1);
                return;
            } else if (key == GLFW_KEY_MINUS) {
                if (getZoomLevel() > -9) setZoomLevel(getZoomLevel() - 1);
                return;
            } else if (key == GLFW_KEY_0) {
                setZoomLevel(0);
                return;
            }
        } else if (mods == GLFW_MOD_ALT) {
            if (key == GLFW_KEY_LEFT && canGoBack()) {
                goBack();
                return;
            } else if (key == GLFW_KEY_RIGHT && canGoForward()) {
                goForward();
                return;
            }
        }

        switch (key) {
            case GLFW_KEY_BACKSPACE, GLFW_KEY_HOME, GLFW_KEY_END, GLFW_KEY_PAGE_UP, GLFW_KEY_PAGE_DOWN, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_KP_4, GLFW_KEY_KP_8, GLFW_KEY_KP_6, GLFW_KEY_KP_2, GLFW_KEY_PRINT_SCREEN, GLFW_KEY_SCROLL_LOCK, GLFW_KEY_CAPS_LOCK, GLFW_KEY_NUM_LOCK, GLFW_KEY_PAUSE, GLFW_KEY_INSERT -> {
                KeyEvent ev = UnsafeExample.makeEvent(dc_, remapKeycode(key, CHAR_UNDEFINED, mods), CHAR_UNDEFINED, KEY_LOCATION_UNKNOWN, KEY_PRESSED, 0, remapModifiers(mods), mapScanCode(key, c));
                sendKeyEvent(ev);
            }

            default -> {
                KeyEvent ev = UnsafeExample.makeEvent(dc_, remapKeycode(key, c, mods), c, KEY_LOCATION_UNKNOWN, KEY_PRESSED, 0, remapModifiers(mods), mapScanCode(key, c));
                sendKeyEvent(ev);
            }
        }
    }
    
    @Override
    public void injectKeyTyped(int key, int mods) {
        // keyboard shortcuts should not be handled
        if (mods == GLFW_MOD_CONTROL) {
            if (key == GLFW_KEY_R) return;
            else if (key == GLFW_KEY_EQUAL) return;
            else if (key == GLFW_KEY_MINUS) return;
            else if (key == GLFW_KEY_0) return;
        } else if (mods == GLFW_MOD_ALT) {
            if (key == GLFW_KEY_LEFT && canGoBack()) return;
            else if (key == GLFW_KEY_RIGHT && canGoForward()) return;
        }
        
        char c = (char) key;
        key = remapKeycode(key, (char) key, mods);
        if(key != VK_UNDEFINED) {
            switch (key) {
                case GLFW_KEY_BACKSPACE, GLFW_KEY_HOME, GLFW_KEY_END, GLFW_KEY_PAGE_UP, GLFW_KEY_PAGE_DOWN, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_KP_4, GLFW_KEY_KP_8, GLFW_KEY_KP_6, GLFW_KEY_KP_2, GLFW_KEY_PRINT_SCREEN, GLFW_KEY_SCROLL_LOCK, GLFW_KEY_CAPS_LOCK, GLFW_KEY_NUM_LOCK, GLFW_KEY_PAUSE, GLFW_KEY_INSERT -> {
                    KeyEvent ev = UnsafeExample.makeEvent(dc_, key, CHAR_UNDEFINED, KEY_LOCATION_UNKNOWN, KEY_TYPED,0, remapModifiers(mods), mapScanCode(key, c));
                    sendKeyEvent(ev);
                }
                default -> {
                    KeyEvent ev = UnsafeExample.makeEvent(dc_, key, c, KEY_LOCATION_UNKNOWN, KEY_TYPED, 0, remapModifiers(mods), mapScanCode(key, c));
                    sendKeyEvent(ev);
                }
            }
        }
    }

    @Override
    public void injectKeyReleasedByKeyCode(int key, char c, int mods) {
        // keyboard shortcuts should not be handled
        if (mods == GLFW_MOD_CONTROL) {
            if (key == GLFW_KEY_R) return;
            else if (key == GLFW_KEY_EQUAL) return;
            else if (key == GLFW_KEY_MINUS) return;
            else if (key == GLFW_KEY_0) return;
        } else if (mods == GLFW_MOD_ALT) {
            if (key == GLFW_KEY_LEFT && canGoBack()) return;
            else if (key == GLFW_KEY_RIGHT && canGoForward()) return;
        }
        
        if (c == '\0') {
            synchronized (WORST_HACK) {
                c = WORST_HACK.getOrDefault(key, '\0');
            }
        }

        switch (key) {
            case GLFW_KEY_BACKSPACE, GLFW_KEY_HOME, GLFW_KEY_END, GLFW_KEY_PAGE_UP, GLFW_KEY_PAGE_DOWN, GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT, GLFW_KEY_KP_4, GLFW_KEY_KP_8, GLFW_KEY_KP_6, GLFW_KEY_KP_2, GLFW_KEY_PRINT_SCREEN, GLFW_KEY_SCROLL_LOCK, GLFW_KEY_CAPS_LOCK, GLFW_KEY_NUM_LOCK, GLFW_KEY_PAUSE, GLFW_KEY_INSERT -> {
                KeyEvent ev = UnsafeExample.makeEvent(dc_, remapKeycode(key, CHAR_UNDEFINED, mods), c, KEY_LOCATION_UNKNOWN, KEY_RELEASED, 0, remapModifiers(mods), mapScanCode(key, c));
                sendKeyEvent(ev);
            }

            default -> {
                KeyEvent ev = UnsafeExample.makeEvent(dc_, remapKeycode(key, c, mods), c, KEY_LOCATION_UNKNOWN, KEY_RELEASED, 0, remapModifiers(mods), mapScanCode(key, c));
                sendKeyEvent(ev);
            }
        }
    }

    @Override
    public void injectMouseWheel(int x, int y, int mods, int amount, int rot) {
        if (mods == GLFW_MOD_CONTROL) {
            if (amount > 0) {
                if (getZoomLevel() < 9) setZoomLevel(getZoomLevel() + 1);
            } else {
                if (getZoomLevel() > -9) setZoomLevel(getZoomLevel() - 1);
            }
            return;
        }
        
        amount *= 3;
        rot = 32;
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
