// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

// Modified by montoyo for MCEF

package org.cef.browser;

import java.awt.Rectangle;
import java.nio.ByteBuffer;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

import org.lwjgl.opengl.EXTBgra;

import static org.lwjgl.opengl.GL11.*;

class CefRenderer {
    private boolean transparent_;
    public int[] texture_id_ = new int[1];
    private int view_width_ = 0;
    private int view_height_ = 0;
    private Rectangle popup_rect_ = new Rectangle(0, 0, 0, 0);
    private Rectangle original_popup_rect_ = new Rectangle(0, 0, 0, 0);

    protected CefRenderer(boolean transparent) {
        transparent_ = transparent;
        initialize();
    }

    protected boolean isTransparent() {
        return transparent_;
    }

    @SuppressWarnings("static-access")
    protected void initialize() {
        glEnable(GL_TEXTURE_2D);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        texture_id_[0] = glGenTextures();
        assert(texture_id_[0] != 0);

        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    protected void cleanup() {
        if (texture_id_[0] != 0)
            glDeleteTextures(texture_id_[0]);
    }

    public void render(double x1, double y1, double x2, double y2) {
        if (view_width_ == 0 || view_height_ == 0)
            return;

        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        int bound = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);

        wr.startDrawingQuads();
        //t.disableColor(); //Doesn't work?
        wr.setColorOpaque(255, 255, 255);

        //                 X   Y  Z          U    V
        wr.addVertexWithUV(x1, y1, 0, 0, 1.f);
        wr.addVertexWithUV(x2, y1, 0, 1.f, 1.f);
        wr.addVertexWithUV(x2, y2, 0, 1.f, 0);
        wr.addVertexWithUV(x1, y2, 0, 0, 0);
        t.draw();

        glBindTexture(GL_TEXTURE_2D, bound);
    }

    protected void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0)
            return;
        original_popup_rect_ = rect;
        popup_rect_ = getPopupRectInWebView(original_popup_rect_);
    }

    protected Rectangle getPopupRectInWebView(Rectangle original_rect) {
        Rectangle rc = original_rect;
        // if x or y are negative, move them to 0.
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        // if popup goes outside the view, try to reposition origin
        if (rc.x + rc.width > view_width_)
            rc.x = view_width_ - rc.width;
        if (rc.y + rc.height > view_height_)
            rc.y = view_height_ - rc.height;
        // if x or y became negative, move them to 0 again.
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        return rc;
    }

    protected void clearPopupRects() {
        popup_rect_.setBounds(0, 0, 0, 0);
        original_popup_rect_.setBounds(0, 0, 0, 0);
    }

    protected void onPaint(boolean popup,
                           Rectangle[] dirtyRects,
                           ByteBuffer buffer,
                           int width,
                           int height) {

        if (transparent_) {
            // Enable alpha blending.
            glEnable(GL_BLEND);
        }

        // Enable 2D textures.
        glEnable(GL_TEXTURE_2D);

        assert(texture_id_[0] != 0);
        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);

        if (!popup) {
            int old_width = view_width_;
            int old_height = view_height_;

            view_width_ = width;
            view_height_ = height;

            glPixelStorei(GL_UNPACK_ROW_LENGTH, view_width_);

            if (old_width != view_width_ || old_height != view_height_) {
                // Update/resize the whole texture.
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, view_width_, view_height_, 0, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
            } else {
                // Update just the dirty rectangles.
                for (int i = 0; i < dirtyRects.length; ++i) {
                    Rectangle rect = dirtyRects[i];
                    glPixelStorei(GL_UNPACK_SKIP_PIXELS, rect.x);
                    glPixelStorei(GL_UNPACK_SKIP_ROWS, rect.y);
                    glTexSubImage2D(GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width, rect.height, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);

                    glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                    glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                }
            }

            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        } else if (popup && popup_rect_.width > 0 && popup_rect_.height > 0) {
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
            glPixelStorei(GL_UNPACK_ROW_LENGTH, width);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, skip_pixels);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, skip_rows);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, EXTBgra.GL_BGRA_EXT,
                    GL_UNSIGNED_BYTE, buffer);
        }

        // Disable 2D textures.
        // glDisable(GL_TEXTURE_2D);

        if (transparent_) {
            // Disable alpha blending.
            glDisable(GL_BLEND);
        }
    }

}