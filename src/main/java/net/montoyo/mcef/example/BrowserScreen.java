package net.montoyo.mcef.example;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.API;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.MCEFApi;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.client.UnsafeExample;
import org.cef.browser.CefBrowserOsr;
import org.lwjgl.glfw.GLFW;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

public class BrowserScreen extends Screen {

    IBrowser browser = null;
    private Button back = null;
    private Button fwd = null;
    private Button go = null;
    private Button min = null;
    private Button vidMode = null;
    private boolean vidModeState = false;
    private EditBox url = null;
    private String urlToLoad = null;

    private long initTime = System.currentTimeMillis();

    private static final String YT_REGEX1 = "^https?://(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_\\-]+)$";
    private static final String YT_REGEX2 = "^https?://(?:www\\.)?youtu\\.be/([a-zA-Z0-9_\\-]+)$";
    private static final String YT_REGEX3 = "^https?://(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_\\-]+)(\\?.+)?$";

    public BrowserScreen() {
        super(Component.translatable("forgecef.example.screen.title"));
        urlToLoad = MCEF.HOME_PAGE;
    }

    public BrowserScreen(String url) {
        super(Component.translatable("forgecef.example.screen.title"));
        urlToLoad = (url == null) ? MCEF.HOME_PAGE : url;
    }

    @Override
    public void init() {
        super.init(); // narrator trigger lmao
        ExampleMod.INSTANCE.hudBrowser = null;

        if (browser == null) {
            //Grab the API and make sure it isn't null.
            API api = MCEFApi.getAPI();
            if (api == null)
                return;

            //Create a browser and resize it to fit the screen
            browser = api.createBrowser((urlToLoad == null) ? MCEF.HOME_PAGE : urlToLoad, false);
            browser.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight() - scaleY(20));
            urlToLoad = null;
        }

        //Resize the browser if window size changed


        //Create GUI
        // Keyboard.enableRepeatEvents(true);
        // buttonList.clear();

        if (url == null) {
            addWidget(back = (new Button(0, 0, 20, 20, Component.literal("<"), (button -> this.legacyActionPerformed(0)))));
            addWidget(fwd = (new Button(20, 0, 20, 20, Component.literal(">"), (button -> this.legacyActionPerformed(1)))));
            addWidget(go = (new Button(width - 60, 0, 20, 20, Component.translatable("forgecef.example.screen.go"), (button -> this.legacyActionPerformed(2)))));
            addWidget(min = (new Button(width - 20, 0, 20, 20, Component.literal("_"), (button -> this.legacyActionPerformed(3)))));
            addWidget(vidMode = (new Button(width - 40, 0, 20, 20, Component.literal("YT"), (button -> this.legacyActionPerformed(4)))));
            vidModeState = false;

            url = new EditBox(minecraft.font, 40, 0, width - 100, 20, Component.literal(""));
            url.setMaxLength(65535);
            url.setValue(browser.getURL());
        } else {
            addWidget(fwd);
            addWidget(go);
            addWidget(min);
            addWidget(vidMode);

            //Handle resizing
            vidMode.x = width - 40;
            go.x = width - 60;
            min.x = width - 20;

            String old = url.getValue();
            url = new EditBox(minecraft.font, 40, 0, width - 100, 20, Component.literal(""));
            url.setMaxLength(65535);
            url.setValue(old);
        }

        this.initTime = System.currentTimeMillis();
    }

    public int scaleY(int y) {
        assert minecraft != null;
        double sy = ((double) y) / ((double) height) * ((double) minecraft.getWindow().getHeight());
        return (int) sy;
    }

    public int scaleX(int x) {
        assert minecraft != null;
        double sx = ((double) x) / ((double) width) * ((double) minecraft.getWindow().getWidth());
        return (int) sx;
    }

    public void loadURL(String url) {
        if (browser == null)
            urlToLoad = url;
        else
            browser.loadURL(url);
    }

    // formerly updateScreen
    public void preRender() {
        if (urlToLoad != null && browser != null) {
            browser.loadURL(urlToLoad);
            urlToLoad = null;
        }
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        //Render the URL box first because it overflows a bit
        this.preRender();
        url.render(matrices, mouseX, mouseY, delta);

        //Render buttons
        super.render(matrices, mouseX, mouseY, delta);

        //Renders the browser if itsn't null
        if (browser != null) {
            GlStateManager._disableDepthTest();
            GlStateManager._disableTexture();
            // GlStateManager._clearColor(1.0f,1.0f,1.0f,1.0f);
            // GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            browser.draw(matrices, .0d, height, width, 20.d); //Don't forget to flip Y axis.
            GlStateManager._enableDepthTest();
        }
    }

    @Override
    public void onClose() {
        //Make sure to close the browser when you don't need it anymore.
        if (!ExampleMod.INSTANCE.hasBackup() && browser != null) {
            browser.close();
        }

        // Keyboard.enableRepeatEvents(false);
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.keyChanged(keyCode, scanCode, modifiers, true) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.keyChanged(keyCode, scanCode, modifiers, false) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (browser != null && !url.isFocused()) {
            browser.injectKeyTyped((int) codePoint, modifiers);
            return true;
        } else {
            return super.charTyped(codePoint, modifiers);
        }
    }

    public boolean keyChanged(int keyCode, int scanCode, int modifiers, boolean pressed) {
        assert minecraft != null;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.setScreen(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F10) {
            System.out.println("Early term F10");
            if (pressed && System.currentTimeMillis() - this.initTime > 1000L) {
                url.setFocus(!url.isFocused());
            }
            return true;
        }

        boolean focused = url.isFocused();
        InputConstants.Key iuKey = InputConstants.getKey(keyCode, scanCode);
        String keystr = iuKey.getDisplayName().getString();
        // String keystr = GLFW.glfwGetKeyName(keyCode, scanCode);
        System.out.println("KEY STR " + keystr);
        if (keystr.length() == 0) {
            return false;
        }

        char key = keystr.charAt(keystr.length() - 1);

        if (browser != null && !focused) { //Inject events into browser
            System.out.println("Sent keystroke " + keystr);
            if (pressed)
                browser.injectKeyPressedByKeyCode(keyCode, key, modifiers);
            else
                browser.injectKeyReleasedByKeyCode(keyCode, key, modifiers);

            return true; // Something did happen
        }

        // Legacy Forwarding
        /*if(!pressed && focused && num == GLFW.GLFW_KEY_ENTER)
            actionPerformed(go);
        else if(pressed)
            url.textboxKeyTyped(key, num);*/
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseChanged(mouseX, mouseY, button, 0, 0, 0, true) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.mouseChanged(mouseX, mouseY, button, 0, 0, 0, false) || super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.mouseChanged(mouseX, mouseY, -1, 0, 0, 0, false);
        super.mouseMoved(mouseX, mouseY);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.mouseChanged(mouseX, mouseY, button, deltaX, deltaY, 0, true) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.mouseChanged(mouseX, mouseY, -1, 0, 0, amount, false) || super.mouseScrolled(mouseX, mouseY, amount);
    }

    public boolean mouseChanged(double mouseX, double mouseY, int btn, double deltaX, double deltaY, double scrollAmount, boolean pressed) {
        int sx = scaleX((int) mouseX);
        int sy = (int) mouseY;
        int wheel = (int) scrollAmount;

        if (browser != null) { //Inject events into browser. TODO: Handle mods & leaving.
            int y = scaleY(sy - 20); //Don't forget to flip Y axis.

//            System.out.println("Dest coords " + sx + " " + y + " button " + btn + " " + pressed);

            if (wheel != 0)
                browser.injectMouseWheel(sx, y, (hasControlDown() && ! hasAltDown() && !hasShiftDown()) ? GLFW.GLFW_MOD_CONTROL : 0, wheel, 0);
            else if (btn == -1)
                browser.injectMouseMove(sx, y, 0, y < 0);
            else
                browser.injectMouseButton(sx, y, 0, btn + 1, pressed, 1);
        }

        if (mouseY <= 20) { //Forward events to GUI.
            return false;
        }
        return true;
    }

    //Called by ExampleMod when the current browser's URL changes.
    public void onUrlChanged(IBrowser b, String nurl) {
        if (b == browser && url != null) {
            url.setValue(nurl);
            vidModeState = nurl.matches(YT_REGEX1) || nurl.matches(YT_REGEX2) || nurl.matches(YT_REGEX3);
        }
    }

    //Handle button clicks the old way...
    protected void legacyActionPerformed(int id) {
        if (browser == null)
            return;

        if (id == 0)
            browser.goBack();
        else if (id == 1)
            browser.goForward();
        else if (id == 2) {
            String fixedURL = ExampleMod.INSTANCE.getAPI().punycode(url.getValue());
            browser.loadURL(fixedURL);
        } else if (id == 3) {
            ExampleMod.INSTANCE.setBackup(this);
            assert minecraft != null;
            minecraft.setScreen(null);
        } else if (id == 4) {
            String loc = browser.getURL();
            String vId = null;
            boolean redo = false;

            if (loc.matches(YT_REGEX1))
                vId = loc.replaceFirst(YT_REGEX1, "$1");
            else if (loc.matches(YT_REGEX2))
                vId = loc.replaceFirst(YT_REGEX2, "$1");
            else if (loc.matches(YT_REGEX3))
                redo = true;

            if (vId != null || redo) {
                ExampleMod.INSTANCE.setBackup(this);
                minecraft.setScreen(new ScreenCfg(browser, vId));
            }
        }
    }

}
