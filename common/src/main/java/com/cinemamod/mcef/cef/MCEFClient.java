package com.cinemamod.mcef.cef;

import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandler;
import org.cef.handler.CefDisplayHandler;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;

import java.util.ArrayList;
import java.util.List;

public class MCEFClient
    implements CefLoadHandler, CefContextMenuHandler, CefDisplayHandler
{
    CefClient instance;

    public MCEFClient(CefClient instance) {
        this.instance = instance;
        instance.addLoadHandler(this);
        instance.addContextMenuHandler(this);
        instance.addDisplayHandler(this);
    }

    /* LoadHandler */
    List<CefLoadHandler> loadHandlers = new ArrayList<>();

    public void addLoadHandler(CefLoadHandler handler) {
        loadHandlers.add(handler);
    }

    @Override
    public void onLoadingStateChange(CefBrowser browser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        for (CefLoadHandler loadHandler : loadHandlers) loadHandler.onLoadingStateChange(browser, isLoading, canGoBack, canGoForward);
    }

    @Override
    public void onLoadStart(CefBrowser browser, CefFrame frame, CefRequest.TransitionType transitionType) {
        for (CefLoadHandler loadHandler : loadHandlers) loadHandler.onLoadStart(browser, frame, transitionType);
    }

    @Override
    public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
        for (CefLoadHandler loadHandler : loadHandlers) loadHandler.onLoadEnd(browser, frame, httpStatusCode);
    }

    @Override
    public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
        for (CefLoadHandler loadHandler : loadHandlers) loadHandler.onLoadError(browser, frame, errorCode, errorText, failedUrl);
    }

    /* ContextMenuHandler */
    List<CefContextMenuHandler> contextMenuHandlers = new ArrayList<>();

    public void addContextMenuHandler(CefContextMenuHandler handler) {
        contextMenuHandlers.add(handler);
    }

    @Override
    public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        for (CefContextMenuHandler contextMenuHandler : contextMenuHandlers) contextMenuHandler.onBeforeContextMenu(browser, frame, params, model);
    }

    @Override
    public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame, CefContextMenuParams params, int commandId, int eventFlags) {
        for (CefContextMenuHandler contextMenuHandler : contextMenuHandlers)
            if (contextMenuHandler.onContextMenuCommand(browser, frame, params, commandId, eventFlags))
                return true;

        return false;
    }

    @Override
    public void onContextMenuDismissed(CefBrowser browser, CefFrame frame) {
        for (CefContextMenuHandler contextMenuHandler : contextMenuHandlers) contextMenuHandler.onContextMenuDismissed(browser, frame);
    }

    /* DisplayHandler */
    List<CefDisplayHandler> displayHandlers = new ArrayList<>();

    public void addDisplayHandler(CefDisplayHandler handler) {
        displayHandlers.add(handler);
    }

    @Override
    public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
        for (CefDisplayHandler displayHandler : displayHandlers) displayHandler.onAddressChange(browser, frame, url);
    }

    @Override
    public void onTitleChange(CefBrowser browser, String title) {
        for (CefDisplayHandler displayHandler : displayHandlers) displayHandler.onTitleChange(browser, title);
    }

    @Override
    public boolean onTooltip(CefBrowser browser, String text) {
        for (CefDisplayHandler displayHandler : displayHandlers)
            if (displayHandler.onTooltip(browser, text))
                return true;
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser browser, String value) {
        for (CefDisplayHandler displayHandler : displayHandlers) displayHandler.onStatusMessage(browser, value);
    }

    // TODO: should this just log to console?
    @Override
    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
        for (CefDisplayHandler displayHandler : displayHandlers)
            if (displayHandler.onConsoleMessage(browser, level, message, source, line))
                return true;
        return false;
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        for (CefDisplayHandler displayHandler : displayHandlers)
            if (displayHandler.onCursorChange(browser, cursorType))
                return true;
        return false;
    }
}
