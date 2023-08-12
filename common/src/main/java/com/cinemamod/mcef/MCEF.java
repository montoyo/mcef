package com.cinemamod.mcef;

import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public final class MCEF {
    private static MCEFApp app;
    private static MCEFClient client;

    /**
     * This gets called by {@link com.cinemamod.mcef.mixins.CefInitMixin}
     * There is no need to call this from your project.
     */
    public static boolean initialize() {
        if (CefUtil.init()) {
            app = new MCEFApp(CefUtil.getCefApp());
            client = new MCEFClient(CefUtil.getCefClient());
            return true;
        }
        return false;
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

    public static boolean isInitialized() {
        return client != null;
    }

    public static void shutdown() {
        if (isInitialized()) {
            CefUtil.shutdown();
            client = null;
            app = null;
        }
    }

    private static void assertInitialized() {
        if (!isInitialized())
            throw new RuntimeException("Chromium Embedded Framework was never initialized.");
    }

    public static String getJavaCefCommit() throws IOException {
        // Try to get from resources (if loading from a jar)
        InputStream inputStream = MCEF.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
            if (properties.containsKey("java-cef-commit")) {
                return properties.getProperty("java-cef-commit");
            }
        } catch (IOException ignored) {
        }

        // Try to get from the git submodule (if loading from development environment)
        ProcessBuilder processBuilder = new ProcessBuilder("git", "submodule", "status", "common/java-cef");
        processBuilder.directory(new File("../../"));
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.trim().split(" ");
            return parts[0].replace("+", "");
        }

        return null;
    }

    private static final HashMap<CefCursorType, Long> CEF_TO_GLFW_CURSORS = new HashMap<>();

    /**
     * Helper method to get a GLFW cursor handle for the given {@link CefCursorType} cursor type
     */
    static long getGLFWCursorHandle(CefCursorType cursorType) {
        if (CEF_TO_GLFW_CURSORS.containsKey(cursorType)) return CEF_TO_GLFW_CURSORS.get(cursorType);
        long glfwCursorHandle = GLFW.glfwCreateStandardCursor(cursorType.glfwId);
        CEF_TO_GLFW_CURSORS.put(cursorType, glfwCursorHandle);
        return glfwCursorHandle;
    }
}
