package com.cinemamod.mcef;

import com.cinemamod.mcef.api.MCEFBrowser;
import com.cinemamod.mcef.cef.MCEFClient;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;

public final class CefUtil {
    private CefUtil() {}

    private static boolean init;
    private static CefApp cefAppInstance;
    private static CefClient cefClientInstance;
    private static MCEFClient mcefClientInstance;

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
        mcefClientInstance = new MCEFClient(cefClientInstance);

        return init = true;
    }

    public static boolean isInit() {
        return init;
    }

    public static CefApp getCefApp() {
        return cefAppInstance;
    }

    /**
     * Gets the {@link CefClient}
     * For adding handlers, please use {@link MCEFClient}, as that implements handlers as lists instead of single instances
     *
     * @return the cef client instance
     */
    public static CefClient getCefClient() {
        return cefClientInstance;
    }

    public static MCEFClient getMCEFClient() {
        return mcefClientInstance;
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
