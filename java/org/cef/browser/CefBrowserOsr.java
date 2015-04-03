// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

// Modified by montoyo for MCEF

package org.cef.browser;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IStringVisitor;
import net.montoyo.mcef.client.ClientProxy;
import net.montoyo.mcef.client.StringVisitor;
import net.montoyo.mcef.utilities.Log;

import org.cef.DummyComponent;
import org.cef.callback.CefDragData;
import org.cef.handler.CefClientHandler;
import org.cef.handler.CefRenderHandler;

import com.google.common.base.Preconditions;

/**
 * This class represents an off-screen rendered browser.
 * The visibility of this class is "package". To create a new 
 * CefBrowser instance, please use CefBrowserFactory.
 */
public class CefBrowserOsr extends CefBrowser_N implements CefRenderHandler, IBrowser {
  private CefRenderer renderer_;
  private long window_handle_ = 0;
  private Rectangle browser_rect_ = new Rectangle(0, 0, 1, 1);  // Work around CEF issue #1437.
  private CefClientHandler clientHandler_;
  private String url_;
  private boolean isTransparent_;
  private CefRequestContext context_;
  private CefBrowserOsr parent_ = null;
  private Point inspectAt_ = null;
  private CefBrowserOsr devTools_ = null;
  private DummyComponent dc_ = new DummyComponent();
  
  public static boolean CLEANUP = true;
  
  private int tex = 0;

  CefBrowserOsr(CefClientHandler clientHandler,
                String url,
                boolean transparent,
                CefRequestContext context) {
    this(clientHandler, url, transparent, context, null, null);
  }

  private CefBrowserOsr(CefClientHandler clientHandler,
                        String url,
                        boolean transparent,
                        CefRequestContext context,
                        CefBrowserOsr parent,
                        Point inspectAt) {
    super();
    isTransparent_ = transparent;
    renderer_ = new CefRenderer(transparent);
    clientHandler_ = clientHandler;
    url_ = url;
    context_ = context;
    parent_ = parent;
    inspectAt_ = inspectAt;
    createGLCanvas();
  }
  
  @Override
  public int getTextureID() {
	  return renderer_.texture_id_[0];
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
  public synchronized void close() {
    if (context_ != null)
      context_.dispose();
    if (parent_ != null) {
      parent_.closeDevTools();
      parent_.devTools_ = null;
      parent_ = null;
    }
    
    if(CLEANUP) {
	    ((ClientProxy) MCEF.PROXY).removeBrowser(this);
	    renderer_.cleanup();
    }
    
    synchronized(queue) {
	    while(queue.size() > 0) {
			PaintData del = queue.pop();
			
			//Fix "out of memory errors" on 32bits JVMs...
			try {
			 destroyDirectByteBuffer(del.buffer);
			} catch(Throwable t) {
			 //t.printStackTrace();
			}
		}
    }
    
    super.close();
  }

  @Override
  public synchronized CefBrowser getDevTools() {
    return getDevTools(null);
  }

  @Override
  public synchronized CefBrowser getDevTools(Point inspectAt) {
    if (devTools_ == null) {
      devTools_ = new CefBrowserOsr(clientHandler_,
                                    url_,
                                    isTransparent_,
                                    context_,
                                    this,
                                    inspectAt);
    }
    return devTools_;
  }
  
  public void resize(int width, int height) {
	  browser_rect_.setBounds(0, 0, width, height);
	  dc_.setBounds(browser_rect_);
      dc_.setVisible(true);
      wasResized(width, height);
  }
  
  public void draw(double x1, double y1, double x2, double y2) {
	  renderer_.render(x1, y1, x2, y2);
  }
  
  @SuppressWarnings("serial")
  private void createGLCanvas() {
	  createBrowser(clientHandler_, 0, url_, isTransparent_, null, context_);
  }

  @Override
  public Rectangle getViewRect(CefBrowser browser) {
    return browser_rect_;
  }

  @Override
  public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
    return viewPoint;
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

  private class PaintData {
	  Rectangle[] dirtyRects;
	  ByteBuffer buffer;
	  int width;
	  int height;
  }
  
  private LinkedList<PaintData> queue = new LinkedList<PaintData>();

  @Override
  public void onPaint(CefBrowser browser, 
                      boolean popup,
                      Rectangle[] dirtyRects, 
                      ByteBuffer buffer, 
                      int width, 
                      int height) {
	  
	  ByteBuffer clone = ByteBuffer.allocateDirect(buffer.capacity());
	  buffer.rewind();
	  clone.put(buffer);
	  buffer.rewind();
	  clone.flip();
	  clone.clear();
    
	  PaintData pd = new PaintData();
	  pd.dirtyRects = dirtyRects;
	  pd.buffer = clone;
	  pd.width = width;
	  pd.height = height;
	  
	  synchronized(queue) {
		  if(queue.size() > 16) { //Wayyyy to much; Minecraft must be laggy...
			  Log.warning("Paint queue is big; is Minecraft laggy?");
			  
			  while(queue.size() > 0) {
				  PaintData del = queue.pop();
				  
				  //Fix "out of memory errors" on 32bits JVMs...
				  try {
					  destroyDirectByteBuffer(del.buffer);
				  } catch(Throwable t) {
					  //t.printStackTrace();
				  }
			  }
		  }
		  
		  queue.push(pd);
	  }
  }
  
  public void mcefUpdate() {
	  synchronized(queue) {
		  while(queue.size() > 0) {
			  PaintData pd = queue.pop();
			  renderer_.onPaint(false, pd.dirtyRects, pd.buffer, pd.width, pd.height);
			  
			  //Fix "out of memory errors" on 32bits JVMs...
			  try {
				  destroyDirectByteBuffer(pd.buffer);
			  } catch(Throwable t) {
				  //t.printStackTrace();
			  }
		  }
	  }
  }
  
  //Stolen from http://stackoverflow.com/questions/1854398/how-to-garbage-collect-a-direct-buffer-java
  //Thanks to Li Pi
  public static void destroyDirectByteBuffer(ByteBuffer toBeDestroyed)
		    throws IllegalArgumentException, IllegalAccessException,
		    InvocationTargetException, SecurityException, NoSuchMethodException {

		  Preconditions.checkArgument(toBeDestroyed.isDirect(),
		      "toBeDestroyed isn't direct!");

		  Method cleanerMethod = toBeDestroyed.getClass().getMethod("cleaner");
		  cleanerMethod.setAccessible(true);
		  Object cleaner = cleanerMethod.invoke(toBeDestroyed);
		  Method cleanMethod = cleaner.getClass().getMethod("clean");
		  cleanMethod.setAccessible(true);
		  cleanMethod.invoke(cleaner);

		}
  
  @Override
  public void injectMouseMove(int x, int y, int mods, boolean left) {
	  MouseEvent ev = new MouseEvent(dc_, left ? MouseEvent.MOUSE_EXITED : MouseEvent.MOUSE_MOVED, 0, mods, x, y, 0, false);
	  sendMouseEvent(ev);
  }
  
  @Override
  public void injectMouseButton(int x, int y, int mods, int btn, boolean pressed, int ccnt) {
	  MouseEvent ev = new MouseEvent(dc_, pressed ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED, 0, mods, x, y, ccnt, false, btn);
	  sendMouseEvent(ev);
  }
  
  @Override
  public void injectKeyTyped(char c, int mods) {
	  KeyEvent ev = new KeyEvent(dc_, KeyEvent.KEY_TYPED, 0, mods, 0, c);
	  sendKeyEvent(ev);
  }
  
  @Override
  public void injectKeyPressed(char c, int mods) {
	  KeyEvent ev = new KeyEvent(dc_, KeyEvent.KEY_PRESSED, 0, mods, 0, c);
	  sendKeyEvent(ev);
  }
  
  @Override
  public void injectKeyReleased(char c, int mods) {
	  KeyEvent ev = new KeyEvent(dc_, KeyEvent.KEY_RELEASED, 0, mods, 0, c);
	  sendKeyEvent(ev);
  }
  
  @Override
  public void injectMouseWheel(int x, int y, int mods, int amount, int rot) {
	  MouseWheelEvent ev = new MouseWheelEvent(dc_, MouseEvent.MOUSE_WHEEL, 0, mods, x, y, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, amount, rot);
	  sendMouseWheelEvent(ev);
  }

  @Override
  public void onCursorChange(CefBrowser browser, int cursorType) {
  }

  @Override
  public boolean startDragging(CefBrowser browser,
                               CefDragData dragData,
                               int mask,
                               int x,
                               int y) {
    // TODO(JCEF) Prepared for DnD support using OSR mode.
    return false;
  }

  @Override
  public void updateDragCursor(CefBrowser browser, int operation) {
    // TODO(JCEF) Prepared for DnD support using OSR mode.
  }

	@Override
	public void runJS(String script, String frame) {
		executeJavaScript(script, frame, 0);
	}

	@Override
	public void visitSource(IStringVisitor isv) {
		getSource(new StringVisitor(isv));
	}

}
