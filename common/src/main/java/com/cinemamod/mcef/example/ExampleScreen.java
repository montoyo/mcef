package com.cinemamod.mcef.example;

import com.cinemamod.mcef.CefUtil;
import com.cinemamod.mcef.api.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

public class ExampleScreen extends Screen {
    protected ExampleScreen(Component component) {
        super(component);
    }

    @Override
    protected void init() {
        super.init();
        if (browser == null) {
            browser = CefUtil.createBrowser(
                    "https://www.google.com/",
                    minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight() - scaleY(20)
            );
        }
    }

    public int scaleX(int x) {
        assert minecraft != null;
        double sx = ((double) x) / ((double) width) * ((double) minecraft.getWindow().getWidth());
        return (int) sx;
    }

    public int scaleY(int y) {
        assert minecraft != null;
        double sy = ((double) y) / ((double) height) * ((double) minecraft.getWindow().getHeight());
        return (int) sy;
    }

    MCEFBrowser browser;

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        super.resize(minecraft, i, j);

        browser.resize(
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight() - scaleY(20)
        );
    }

    @Override
    public void onClose() {
        browser.close();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);

        // TODO: clean this up
        Matrix4f positionMatrix = new Matrix4f();
        Tesselator t = Tesselator.getInstance();
        BufferBuilder vb = t.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, browser.getTexture());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        // previously GL_QUADS for drawmode
        vb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vb.vertex(positionMatrix, (float) 0, (float) minecraft.getWindow().getHeight(), 0.0f).uv(0.0f, 2.0f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) minecraft.getWindow().getWidth(), (float) minecraft.getWindow().getHeight(), 0.0f).uv(2.f, 2.0f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) minecraft.getWindow().getWidth(), (float) scaleY(20), 0.0f).uv(2.f, 0.f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) 0, (float) scaleY(20), 0.0f).uv(0.0f, 0.0f).color(255, 255, 255, 255).endVertex();
        t.end();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = scaleX((int) mouseX);
        int y = scaleY((int) mouseY - 40);
        browser.sendMousePress(x, y, button);
        browser.setFocus(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int x = scaleX((int) mouseX);
        int y = scaleY((int) mouseY - 40);
        browser.sendMouseRelease(x, y, button);
        browser.setFocus(true);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int x = scaleX((int) mouseX);
        int y = scaleY((int) mouseY - 40);
        browser.sendMouseMove(x, y);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int x = scaleX((int) mouseX);
        int y = scaleY((int) mouseY - 40);
        browser.sendMouseWheel(x, y, (int) delta * 3, 0);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        browser.sendKeyPress(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        browser.sendKeyRelease(keyCode, scanCode, modifiers);
        browser.setFocus(true);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == (char) 0) return false;

        browser.sendKeyTyped(codePoint, modifiers);
        browser.setFocus(true);
        return super.charTyped(codePoint, modifiers);
    }
}
