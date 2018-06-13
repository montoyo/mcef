// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

// Modified by montoyo for MCEF

package org.cef.browser;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

// import javax.media.opengl.GL2; // OLD,reemove

import java.util.ArrayList;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.utilities.Log;
import org.lwjgl.opengl.EXTBgra;

import static org.lwjgl.opengl.GL11.*;

public class CefRenderer {

    //montoyo: debug tool
    private static final ArrayList<Integer> GL_TEXTURES = new ArrayList<>();
    public static void dumpVRAMLeak() {
        Log.info(">>>>> MCEF: Beginning VRAM leak report");
        GL_TEXTURES.forEach(tex -> Log.warning(">>>>> MCEF: This texture has not been freed: " + tex));
        Log.info(">>>>> MCEF: End of VRAM leak report");
    }
    private boolean transparent_;
    // MCEF// private GL2 initialized_context_ = null;
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
    }

    protected boolean isTransparent() {
        return transparent_;
    }

    @SuppressWarnings("static-access")
    protected void initialize(/*GL2 gl2*/) { // MCEF
        GlStateManager.enableTexture2D();
        texture_id_[0] = glGenTextures();

        if(MCEF.CHECK_VRAM_LEAK)
            GL_TEXTURES.add(texture_id_[0]);

        GlStateManager.bindTexture(texture_id_[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        GlStateManager.bindTexture(0);

        /*gl2.glHint(gl2.GL_POLYGON_SMOOTH_HINT, gl2.GL_NICEST);

        gl2.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Necessary for non-power-of-2 textures to render correctly.
        gl2.glPixelStorei(gl2.GL_UNPACK_ALIGNMENT, 1);

        // Create the texture.
        gl2.glGenTextures(1, texture_id_, 0);
        assert(texture_id_[0] != 0);

        gl2.glBindTexture(gl2.GL_TEXTURE_2D, texture_id_[0]);
        gl2.glTexParameteri(gl2.GL_TEXTURE_2D, gl2.GL_TEXTURE_MIN_FILTER, gl2.GL_NEAREST);
        gl2.glTexParameteri(gl2.GL_TEXTURE_2D, gl2.GL_TEXTURE_MAG_FILTER, gl2.GL_NEAREST);
        gl2.glTexEnvf(gl2.GL_TEXTURE_ENV, gl2.GL_TEXTURE_ENV_MODE, gl2.GL_MODULATE);*/
    }

    protected void cleanup() {
        if(texture_id_[0] != 0) {
            if(MCEF.CHECK_VRAM_LEAK)
                GL_TEXTURES.remove((Object) texture_id_[0]);

            glDeleteTextures(texture_id_[0]);
        }
    }

    @SuppressWarnings("static-access")
    public void render(double x1, double y1, double x2, double y2) {
        if (use_draw_pixels_ || view_width_ == 0 || view_height_ == 0)
            return;

        // assert(initialized_context_ != null);

        /*final float[] vertex_data = {// tu,   tv,     x,     y,    z
                0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f};
        FloatBuffer vertices = FloatBuffer.wrap(vertex_data);

        gl2.glClear(gl2.GL_COLOR_BUFFER_BIT | gl2.GL_DEPTH_BUFFER_BIT);

        gl2.glMatrixMode(gl2.GL_MODELVIEW);
        gl2.glLoadIdentity();

        // Match GL units to screen coordinates.
        gl2.glViewport(0, 0, view_width_, view_height_);
        gl2.glMatrixMode(gl2.GL_PROJECTION);
        gl2.glLoadIdentity();

        // Draw the background gradient.
        gl2.glPushAttrib(gl2.GL_ALL_ATTRIB_BITS);
        gl2.glBegin(gl2.GL_QUADS);
        gl2.glColor4f(1.0f, 0.0f, 0.0f, 1.0f); // red
        gl2.glVertex2f(-1.0f, -1.0f);
        gl2.glVertex2f(1.0f, -1.0f);
        gl2.glColor4f(0.0f, 0.0f, 1.0f, 1.0f); // blue
        gl2.glVertex2f(1.0f, 1.0f);
        gl2.glVertex2f(-1.0f, 1.0f);
        gl2.glEnd();
        gl2.glPopAttrib();

        // Rotate the view based on the mouse spin.
        if (spin_x_ != 0) gl2.glRotatef(-spin_x_, 1.0f, 0.0f, 0.0f);
        if (spin_y_ != 0) gl2.glRotatef(-spin_y_, 0.0f, 1.0f, 0.0f);

        if (transparent_) {
            // Alpha blending style. Texture values have premultiplied alpha.
            gl2.glBlendFunc(gl2.GL_ONE, gl2.GL_ONE_MINUS_SRC_ALPHA);

            // Enable alpha blending.
            gl2.glEnable(gl2.GL_BLEND);
        }

        // Enable 2D textures.
        gl2.glEnable(gl2.GL_TEXTURE_2D);

        // Draw the facets with the texture.
        assert(texture_id_[0] != 0);
        gl2.glBindTexture(gl2.GL_TEXTURE_2D, texture_id_[0]);
        gl2.glInterleavedArrays(gl2.GL_T2F_V3F, 0, vertices);
        gl2.glDrawArrays(gl2.GL_QUADS, 0, 4);

        // Disable 2D textures.
        gl2.glDisable(gl2.GL_TEXTURE_2D);

        if (transparent_) {
            // Disable alpha blending.
            gl2.glDisable(gl2.GL_BLEND);
        }*/

        Tessellator t = Tessellator.getInstance();
        BufferBuilder vb = t.getBuffer();

        GlStateManager.bindTexture(texture_id_[0]);
        vb.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vb.pos(x1, y1, 0.0).tex(0.0, 1.0).color(255, 255, 255, 255).endVertex();
        vb.pos(x2, y1, 0.0).tex(1.f, 1.f).color(255, 255, 255, 255).endVertex();
        vb.pos(x2, y2, 0.0).tex(1.f, 0.0).color(255, 255, 255, 255).endVertex();
        vb.pos(x1, y2, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex();
        t.draw();
        GlStateManager.bindTexture(0);
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

    @SuppressWarnings("static-access")
    protected void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
        // initialize(gl2); // TODO?
        if(transparent_) // Enable alpha blending.
            GlStateManager.enableBlend();

        final int size = (width * height) << 2;
        if(size > buffer.limit()) {
            Log.warning("Bad data passed to CefRenderer.onPaint() triggered safe guards... (1)");
            return;
        }

        /*if (use_draw_pixels_) {
            // MCEF: TODO: IMPLEMENT THIS
            gl2.glRasterPos2f(-1, 1);
            gl2.glPixelZoom(1, -1);
            gl2.glDrawPixels(width, height, GL2.GL_BGRA, GL2.GL_UNSIGNED_BYTE, buffer);
            return;
        }*/

        // Enable 2D textures.
        // original JCEF
        /* gl2.glEnable(gl2.GL_TEXTURE_2D);

        assert(texture_id_[0] != 0);
        gl2.glBindTexture(gl2.GL_TEXTURE_2D, texture_id_[0]);*/

        // new MCEF
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(texture_id_[0]);

        int oldAlignement = glGetInteger(GL_UNPACK_ALIGNMENT);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        if (!popup) {
            if(completeReRender || width != view_width_ || height != view_height_) {
                assert(false); // not implemented yet
                /*
                // Update/resize the whole texture.
                view_width_ = width;
                view_height_ = height;
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, view_width_, view_height_, 0, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);*/
            } else {
                int old_width = view_width_;
                int old_height = view_height_;

                view_width_ = width;
                view_height_ = height;

                //TODO implement //gl2.glPixelStorei(gl2.GL_UNPACK_ROW_LENGTH, view_width_);

                if (old_width != view_width_ || old_height != view_height_) {
                    // Update/resize the whole texture.
                    /*TODOgl2.glPixelStorei(gl2.GL_UNPACK_SKIP_PIXELS, 0);
                    gl2.glPixelStorei(gl2.GL_UNPACK_SKIP_ROWS, 0);
                    gl2.glTexImage2D(gl2.GL_TEXTURE_2D, 0, gl2.GL_RGBA, view_width_, view_height_, 0,
                            gl2.GL_BGRA, gl2.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);*/
                } else {
                    // Update just the dirty rectangles.
                    /*for (int i = 0; i < dirtyRects.length; ++i) {
                        Rectangle rect = dirtyRects[i];
                        gl2.glPixelStorei(gl2.GL_UNPACK_SKIP_PIXELS, rect.x);
                        gl2.glPixelStorei(gl2.GL_UNPACK_SKIP_ROWS, rect.y);
                        gl2.glTexSubImage2D(gl2.GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width,
                                rect.height, gl2.GL_BGRA, gl2.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
                    }*/
                    glPixelStorei(GL_UNPACK_ROW_LENGTH, view_width_);

                    // Update just the dirty rectangles.
                    for(Rectangle rect: dirtyRects) {
                        if(rect.x < 0 || rect.y < 0 || rect.x + rect.width > view_width_ || rect.y + rect.height > view_height_)
                            Log.warning("Bad data passed to CefRenderer.onPaint() triggered safe guards... (2)");
                        else {
                            glPixelStorei(GL_UNPACK_SKIP_PIXELS, rect.x);
                            glPixelStorei(GL_UNPACK_SKIP_ROWS, rect.y);
                            glTexSubImage2D(GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width, rect.height, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
                        }
                    }

                    glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                    glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                    glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
                }
            }
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
            if (x + w > view_width_) w -= x + w - view_width_;
            if (y + h > view_height_) h -= y + h - view_height_;

            // Update the popup rectangle.
            /* OLD gl2.glPixelStorei(gl2.GL_UNPACK_ROW_LENGTH, width);
            gl2.glPixelStorei(gl2.GL_UNPACK_SKIP_PIXELS, skip_pixels);
            gl2.glPixelStorei(gl2.GL_UNPACK_SKIP_ROWS, skip_rows);
            gl2.glTexSubImage2D(gl2.GL_TEXTURE_2D, 0, x, y, w, h, gl2.GL_BGRA,
                    gl2.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
                    */
            glPixelStorei(GL_UNPACK_ROW_LENGTH, width);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, skip_pixels);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, skip_rows);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, EXTBgra.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        }

        glPixelStorei(GL_UNPACK_ALIGNMENT, oldAlignement);
        GlStateManager.bindTexture(0);

        /*
        // Disable 2D textures.
        gl2.glDisable(gl2.GL_TEXTURE_2D);

        if (transparent_) {
            // Disable alpha blending.
            gl2.glDisable(gl2.GL_BLEND);
        } TODO: implement? */
    }

    protected void setSpin(float spinX, float spinY) {
        spin_x_ = spinX;
        spin_y_ = spinY;
    }

    protected void incrementSpin(float spinDX, float spinDY) {
        spin_x_ -= spinDX;
        spin_y_ -= spinDY;
    }

    public int getViewWidth() {
        return view_width_;
    }

    public int getViewHeight() {
        return view_height_;
    }
}
