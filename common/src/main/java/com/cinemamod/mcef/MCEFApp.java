package com.cinemamod.mcef;

import org.cef.CefApp;

public class MCEFApp {
    private final CefApp handle;

    public MCEFApp(CefApp handle) {
        this.handle = handle;
    }

    public CefApp getHandle() {
        return handle;
    }
}
