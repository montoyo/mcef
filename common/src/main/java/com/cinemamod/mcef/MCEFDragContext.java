package com.cinemamod.mcef;

import org.cef.callback.CefDragData;
import org.cef.misc.CefCursorType;

public class MCEFDragContext {
	private CefDragData dragData = null;
	private int dragMask = 0;
	private int cursorOverride = -1;
	private int actualCursor = -1;
	
	/**
	 * Used to prevent re-selecting stuff while dragging
	 * If the user is dragging, emulate having no buttons pressed
	 * @param btnMask the actual mask
	 * @return a mask modified based on if the user is dragging
	 */
	public int getVirtualModifiers(int btnMask) {
		return dragData != null ? 0 : btnMask;
	}
	
	/**
	 * When the user is dragging, the browser-set cursor shouldn't be used
	 * Instead the cursor should change based on what action would be performed when they release at the given location
	 * However, the browser-set cursor also needs to be tracked, so this handles that as well
	 * @param cursorType the actual cursor type (should be the result of {@link MCEFDragContext#getActualCursor()} if you're just trying to see the current cursor)
	 * @return the drag operation modified cursor if dragging, or the actual cursor if not
	 */
	public int getVirtualCursor(int cursorType) {
		actualCursor = cursorType;
		if (cursorOverride != -1) cursorType = cursorOverride;
		return cursorType;
	}
	
	/**
	 * Checks if a drag operation is currently happening
	 * @return true if the user is dragging, elsewise false
	 */
	public boolean isDragging() {
		return dragData != null;
	}
	
	/**
	 * Gets the {@link CefDragData} of the current drag operation
	 * @return the current drag operation's data
	 */
	public CefDragData getDragData() {
		return dragData;
	}
	
	/**
	 * Gets the allowed operation mask for this drag event
	 * @return -1 for any, 0 for none, 1 for copy (TODO: others)
	 */
	public int getMask() {
		return dragMask;
	}
	
	/**
	 * Gets the browser-set cursor
	 * @return the cursor that has been set by the browser, disregarding drag operations
	 */
	public int getActualCursor() {
		return actualCursor;
	}
	
	public void startDragging(CefDragData dragData, int mask) {
		this.dragData = dragData;
		this.dragMask = mask;
	}
	
	public void stopDragging() {
		dragData.dispose();
		dragData = null;
		dragMask = 0;
		cursorOverride = -1;
	}
	
	public boolean updateCursor(int operation) {
		if (dragData == null) return false;
		
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
		
		return currentOverride != cursorOverride && cursorOverride != -1;
	}
}
