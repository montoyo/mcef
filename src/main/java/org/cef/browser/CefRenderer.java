// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.utilities.Log;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.EXTBGRA.GL_BGRA_EXT;
import static org.lwjgl.opengl.GL11.*;

public class CefRenderer {

    //montoyo: debug tool
    //montoyo: debug tool
    private static final ArrayList<Integer> GL_TEXTURES = new ArrayList<>();

    public static void dumpVRAMLeak() {
        Log.info(">>>>> MCEF: Beginning VRAM leak report");
        GL_TEXTURES.forEach(tex -> Log.warning(">>>>> MCEF: This texture has not been freed: " + tex));
        Log.info(">>>>> MCEF: End of VRAM leak report");
    }

    private boolean transparent_;
    public int[] texture_id_ = new int[1];
    private int view_width_ = 0;
    private int view_height_ = 0;
    private float spin_x_ = 0f;
    private float spin_y_ = 0f;
    private Rectangle popup_rect_ = new Rectangle(0, 0, 0, 0);
    private Rectangle original_popup_rect_ = new Rectangle(0, 0, 0, 0);
    private boolean use_draw_pixels_ = false;

    protected CefRenderer(boolean transparent) {
        transparent_ = transparent;
        initialize();
    }

    protected boolean isTransparent() {
        return transparent_;
    }

    protected int getTextureID() {
        return texture_id_[0];
    }

    protected void initialize() {
        GlStateManager._enableTexture();
        texture_id_[0] = GlStateManager._genTexture();

        if (MCEF.CHECK_VRAM_LEAK)
            GL_TEXTURES.add(texture_id_[0]);

        GlStateManager._bindTexture(texture_id_[0]);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // GL21.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        GlStateManager._bindTexture(0);
    }

    protected void cleanup() {
        if (texture_id_[0] != 0) {
            if (MCEF.CHECK_VRAM_LEAK)
                GL_TEXTURES.remove((Object) texture_id_[0]);

            GlStateManager._deleteTexture(texture_id_[0]);
        }
    }

    protected void render(PoseStack matrix, double x1, double y1, double x2, double y2) {
        Matrix4f positionMatrix = matrix.last().pose();
        Tesselator t = Tesselator.getInstance();
        BufferBuilder vb = t.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture_id_[0]);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        // previously GL_QUADS for drawmode
        vb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        vb.vertex(positionMatrix, (float) x1, (float) y1, 0.0f).uv(0.0f, 1.0f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) x2, (float) y1, 0.0f).uv(1.f, 1.f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) x2, (float) y2, 0.0f).uv(1.f, 0.0f).color(255, 255, 255, 255).endVertex();
        vb.vertex(positionMatrix, (float) x1, (float) y2, 0.0f).uv(0.0f, 0.0f).color(255, 255, 255, 255).endVertex();
        t.end();
    }

    protected void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0) return;
        original_popup_rect_ = rect;
        popup_rect_ = getPopupRectInWebView(original_popup_rect_);
    }

    protected Rectangle getPopupRect() {
        return (Rectangle) popup_rect_.clone();
    }

    protected Rectangle getPopupRectInWebView(Rectangle original_rect) {
        Rectangle rc = original_rect;
        // if x or y are negative, move them to 0.
        if (rc.x < 0) rc.x = 0;
        if (rc.y < 0) rc.y = 0;
        // if popup goes outside the view, try to reposition origin
        if (rc.x + rc.width > view_width_) rc.x = view_width_ - rc.width;
        if (rc.y + rc.height > view_height_) rc.y = view_height_ - rc.height;
        // if x or y became negative, move them to 0 again.
        if (rc.x < 0) rc.x = 0;
        if (rc.y < 0) rc.y = 0;
        return rc;
    }

    protected void clearPopupRects() {
        popup_rect_.setBounds(0, 0, 0, 0);
        original_popup_rect_.setBounds(0, 0, 0, 0);
    }

    protected void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
        if (transparent_) // Enable alpha blending.
            GlStateManager._enableBlend();

        final int size = (width * height) << 2;
        if (size > buffer.limit()) {
            Log.warning("Bad data passed to CefRenderer.onPaint() triggered safe guards... (1)");
            return;
        }

        // Enable 2D textures.
        GlStateManager._enableTexture();
        GlStateManager._bindTexture(texture_id_[0]);

        //System.out.println("ON BROWSER PAINT");

        int oldAlignement = GlStateManager._getInteger(GL_UNPACK_ALIGNMENT);
        //System.out.println("glGetInteger ok");
        GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, 1);
        //System.out.println("glPixelStore ok");
        if (!popup) {
            if (completeReRender || width != view_width_ || height != view_height_) {
                // Update/resize the whole texture.
                view_width_ = width;
                view_height_ = height;
//                System.out.println("going to glTexImage2D " + width + " " + height + " " + buffer.limit());
                GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS,0);
                GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS,0);
                GlStateManager._texImage2D(GL_TEXTURE_2D, 0, GL_RGBA, view_width_, view_height_, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer.asIntBuffer());
                //System.out.println("glTexImage2D ok");
            } else {
                // System.out.println("Noncomplete rerender processing pixel store");
                GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, view_width_);

                // Update just the dirty rectangles.
                for (Rectangle rect : dirtyRects) {
                    //System.out.println("Updating rect");
                    if (rect.x < 0 || rect.y < 0 || rect.x + rect.width > view_width_ || rect.y + rect.height > view_height_)
                        Log.warning("Bad data passed to CefRenderer.onPaint() triggered safe guards... (2)");
                    else {
                        GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, rect.x);
                        GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, rect.y);
                        glTexSubImage2D(GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width, rect.height, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
                    }
                }
                //System.out.println("More GLPixel store stuff");
                GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
                GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
                GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, 0);
            }
        } else if (popup_rect_.width > 0 && popup_rect_.height > 0) {
            System.out.println("Processing popup");
            int skip_pixels = 0, x = popup_rect_.x;
            int skip_rows = 0, y = popup_rect_.y;
            int w = width;
            int h = height;

            // Adjust the popup to fit inside the view.
            if (x < 0) {
                skip_pixels = -x;
                x = 0;
            }
            if (y < 0) {
                skip_rows = -y;
                y = 0;
            }
            if (x + w > view_width_)
                w -= x + w - view_width_;
            if (y + h > view_height_)
                h -= y + h - view_height_;

            // Update the popup rectangle.
            //System.out.println("glPixelStorei...");
            GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, width);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, skip_pixels);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, skip_rows);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
            GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
            GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        }
        //System.out.println("glPixelStorei final...");
        GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, oldAlignement);
        GlStateManager._bindTexture(0);
    }

    protected void setSpin(float spinX, float spinY) {
        spin_x_ = spinX;
        spin_y_ = spinY;
    }

    protected void incrementSpin(float spinDX, float spinDY) {
        spin_x_ -= spinDX;
        spin_y_ -= spinDY;
    }
}
