package com.cinemamod.mcef;

import com.cinemamod.mcef.api.MCEFBrowser;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;

public final class CefUtil {
    private CefUtil() {}

    private static boolean init;
    private static CefApp cefAppInstance;
    private static CefClient cefClientInstance;

    public static boolean init() {
        String[] cefSwitches = new String[]{
                "--autoplay-policy=no-user-gesture-required",
                "--disable-web-security"
        };

        if (!CefApp.startup(cefSwitches)) {
            return false;
        }

        CefSettings cefSettings = new CefSettings();
        cefSettings.windowless_rendering_enabled = true;
        cefSettings.background_color = cefSettings.new ColorType(0, 255, 255, 255);

        cefAppInstance = CefApp.getInstance(cefSwitches, cefSettings);
        cefClientInstance = cefAppInstance.createClient();

        return init = true;
    }

    public static boolean isInit() {
        return init;
    }

    public static CefApp getCefApp() {
        return cefAppInstance;
    }

    public static CefClient getCefClient() {
        return cefClientInstance;
    }

    public static MCEFBrowser createBrowser(String startUrl, int widthPx, int heightPx) {
        if (!init) return null;
        MCEFBrowser browser = new MCEFBrowser(cefClientInstance, startUrl, false, null);
        browser.setCloseAllowed();
        browser.createImmediately();
        browser.resize(widthPx, heightPx);
        return browser;
    }
}
