package net.montoyo.mcef.example;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.montoyo.mcef.api.IBrowser;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ScreenCfg extends GuiScreen {

    private IBrowser browser;
    private int width = 320;
    private int height = 180;
    private int x = 10;
    private int y = 10;
    private int offsetX = 0;
    private int offsetY = 0;
    private boolean dragging = false;
    private boolean resizing = false;
    private boolean drawSquare = true;

    public ScreenCfg(IBrowser b, String vId) {
        browser = b;
        if(vId != null)
            b.loadURL("https://www.youtube.com/embed/" + vId + "?autoplay=1");

        b.resize(width, height);
    }

    @Override
    public void handleInput() {
        while(Keyboard.next()) {
            if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                drawSquare = false;
                ExampleMod.INSTANCE.hudBrowser = this;
                browser.injectMouseMove(-10, -10, 0, true);
                mc.displayGuiScreen(null);
                return;
            }
        }

        while(Mouse.next()) {
            int btn = Mouse.getEventButton();
            boolean pressed = Mouse.getEventButtonState();
            int sx = Mouse.getEventX();
            int sy = mc.displayHeight - Mouse.getEventY();

            if(btn == 1 && pressed && sx >= x && sy >= y && sx < x + width && sy < y + height) {
                browser.injectMouseMove(sx - x, sy - y, 0, false);
                browser.injectMouseButton(sx - x, sy - y, 0, 1, true, 1);
                browser.injectMouseButton(sx - x, sy - y, 0, 1, false, 1);
            } else if(dragging) {
                if(btn == 0 && !pressed)
                    dragging = false;
                else {
                    x = sx + offsetX;
                    y = sy + offsetY;
                }
            } else if(resizing) {
                if(btn == 0 && !pressed) {
                    resizing = false;
                    browser.resize(width, height);
                } else {
                    int w = sx - x;
                    int h = sy - y;

                    if(w >= 32 && h >= 18) {
                        if(h >= w) {
                            double dw = ((double) h) * (16.0 / 9.0);
                            width = (int) dw;
                            height = h;
                        } else {
                            double dh = ((double) w) * (9.0 / 16.0);
                            width = w;
                            height = (int) dh;
                        }
                    }
                }
            } else if(pressed && btn == 0 && sx >= x && sy >= y && sx < x + width && sy < y + height) { //In browser rect
                dragging = true;
                offsetX = x - sx;
                offsetY = y - sy;
            } else if(pressed && btn == 0 && sx >= x + width && sy >= y + height && sx < x + width + 10 && sy < y + height + 10) //In resize rect
                resizing = true;
        }
    }

    @Override
    public void drawScreen(int i1, int i2, float f) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        browser.draw(unscaleX(x), unscaleY(height + y), unscaleX(width + x), unscaleY(y));

        if(drawSquare) {
            Tessellator t = Tessellator.getInstance();
            BufferBuilder vb = t.getBuffer();

            vb.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            vb.pos(unscaleX(x + width), unscaleY(y + height), 0.0).color(255, 255, 255, 255).endVertex();
            vb.pos(unscaleX(x + width + 10), unscaleY(y + height), 0.0).color(255, 255, 255, 255).endVertex();
            vb.pos(unscaleX(x + width + 10), unscaleY(y + height + 10), 0.0).color(255, 255, 255, 255).endVertex();
            vb.pos(unscaleX(x + width), unscaleY(y + height + 10), 0.0).color(255, 255, 255, 255).endVertex();
            t.draw();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public double unscaleX(int x) {
        return ((double) x) / ((double) mc.displayWidth) * ((double) super.width);
    }

    public double unscaleY(int y) {
        return ((double) y) / ((double) mc.displayHeight) * ((double) super.height);
    }

}
