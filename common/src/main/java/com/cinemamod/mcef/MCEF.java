package com.cinemamod.mcef;

public final class MCEF {
    private static MCEFApp app;
    private static MCEFClient client;

    public static boolean initialize() {
        if (CefUtil.init()) {
            app = new MCEFApp(CefUtil.getCefApp());
            client = new MCEFClient(CefUtil.getCefClient());
            return true;
        }
        return false;
    }

    public static boolean isInitialized() {
        return CefUtil.isInit();
    }

    public static MCEFApp getApp() {
        assertInitialized();
        return app;
    }

    public static MCEFClient getClient() {
        assertInitialized();
        return client;
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, null);
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent, int width, int height) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent, null);
        browser.setCloseAllowed();
        browser.createImmediately();
        browser.resize(width, height);
        return browser;
    }

    private static void assertInitialized() {
        if (!CefUtil.isInit())
            throw new RuntimeException("MCEF#initialize() must be called before you can run this.");
    }
}
