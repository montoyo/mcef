package net.montoyo.mcef.example;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.montoyo.mcef.api.IBrowser;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class ScreenCfg extends Screen {

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
        super(new TranslatableText("fabricef.screen.config.title"));
        browser = b;
        if(vId != null)
            b.loadURL("https://www.youtube.com/embed/" + vId + "?autoplay=1");

        b.resize(width, height);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_ESCAPE) {
            drawSquare = false;
            ExampleMod.INSTANCE.hudBrowser = this;
            browser.injectMouseMove(-10, -10, 0, true);
            client.setScreen(null);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int btn) {
        return this.mouseChange(mouseX,mouseY,btn,true) || super.mouseClicked(mouseX, mouseY, btn);
    }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int btn) {
        return this.mouseChange(mouseX,mouseY,btn,false);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int btn, double deltaX, double deltaY) {
        return this.mouseChange(mouseX,mouseY,btn,true);
    }
    public boolean mouseChange(double mouseX, double mouseY, int btn, boolean pressed) {
        int sx = (int) mouseX;
        assert client != null;
        int sy = (int) (client.getWindow().getHeight() - mouseY);

        // send to browser
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
        } else if(pressed && btn == 0 && sx >= x + width && sy >= y + height && sx < x + width + 10 && sy < y + height + 10){ //In resize rect
            resizing = true;
        }
        return false;
    }


    @Override
    public void render(MatrixStack matricies, int i1, int i2, float f) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        browser.draw(matricies, unscaleX(x), unscaleY(height + y), unscaleX(width + x), unscaleY(y));

        if(drawSquare) {
            Tessellator t = Tessellator.getInstance();
            BufferBuilder vb = t.getBuffer();

            // drawMode -> GL11.GL_LINE_LOOP
            vb.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
            vb.vertex(unscaleX(x + width), unscaleY(y + height), 0.0).color(255, 255, 255, 255);
            vb.vertex(unscaleX(x + width + 10), unscaleY(y + height), 0.0).color(255, 255, 255, 255);
            vb.vertex(unscaleX(x + width + 10), unscaleY(y + height + 10), 0.0).color(255, 255, 255, 255);
            vb.vertex(unscaleX(x + width), unscaleY(y + height + 10), 0.0).color(255, 255, 255, 255);
            t.draw();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public double unscaleX(int x) {
        return ((double) x) / ((double) client.getWindow().getWidth()) * ((double) super.width);
    }

    public double unscaleY(int y) {
        return ((double) y) / ((double) client.getWindow().getHeight()) * ((double) super.height);
    }

}
