package com.cinemamod.mcef.api;

import com.mojang.blaze3d.systems.RenderSystem;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL12.*;

public class MCEFRenderer {
    private final boolean transparent;
    private final int[] textureID = new int[1];

    protected MCEFRenderer(boolean transparent) {
        this.transparent = transparent;
    }

    public void initialize() {
        textureID[0] = glGenTextures();
        RenderSystem.bindTexture(textureID[0]);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    public int getTextureID() {
        return textureID[0];
    }

    protected void cleanup() {
        if (textureID[0] != 0) {
            glDeleteTextures(textureID[0]);
            textureID[0] = 0;
        }
    }

    protected void onPaint(ByteBuffer buffer, int width, int height) {
        if (textureID[0] == 0) return;
        if (transparent) RenderSystem.enableBlend();
        RenderSystem.bindTexture(textureID[0]);
        RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, width);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
    }
}
