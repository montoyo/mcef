package com.cinemamod.mcef;

import org.cef.browser.CefBrowserOsr;
import org.cef.callback.CefDragData;
import org.cef.misc.CefCursorType;

public class MCEFDragContext {
	private CefDragData dragData = null;
	private int dragMask = 0;
	private int cursorOverride = -1;
	private int actualCursor = -1;
	
	private final CefBrowserOsr browser;
	
	public MCEFDragContext(CefBrowserOsr browser) {
		this.browser = browser;
	}
	
	public int getVirtualModifiers(int btnMask) {
		return dragData != null ? 0 : btnMask;
	}
	
	public int getVirtualCursor(int cursorType) {
		actualCursor = cursorType;
		if (cursorOverride != -1) cursorType = cursorOverride;
		return cursorType;
	}
	
	public CefDragData getDragData() {
		return dragData;
	}
	
	public boolean isDragging() {
		return dragData != null;
	}
	
	public int getMask() {
		return dragMask;
	}
	
	public void stopDragging() {
		dragData.dispose();
		dragData = null;
		dragMask = 0;
		cursorOverride = -1;
		
		browser.onCursorChange(browser, actualCursor);
	}
	
	public void startDragging(CefDragData dragData, int mask) {
		this.dragData = dragData;
		this.dragMask = mask;
	}
	
	public void updateCursor(int operation) {
		if (dragData == null) return;
		
		int currentOverride = cursorOverride;
		
		switch (operation) {
			case 0:
				cursorOverride = CefCursorType.NO_DROP.ordinal();
				break;
			case 1:
				cursorOverride = CefCursorType.COPY.ordinal();
				break;
			// TODO: this is a guess, based off https://magpcss.org/ceforum/apidocs3/projects/(default)/cef_drag_operations_mask_t.html
			// not sure if it's correct
			case 16:
				cursorOverride = CefCursorType.MOVE.ordinal();
				break;
			default: // TODO: I'm not sure of the numbers for these
				cursorOverride = -1;
		}
		
		if (currentOverride != cursorOverride && cursorOverride != -1)
			browser.onCursorChange(browser, cursorOverride);
	}
}
