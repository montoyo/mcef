/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.cinemamod.mcef;

import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * An API to create Chromium web browsers in Minecraft. Uses
 * a modified version of java-cef (Java Chromium Embedded Framework).
 */
public final class MCEF {
    private static MCEFSettings settings;
    private static MCEFApp app;
    private static MCEFClient client;

    public static MCEFSettings getSettings() {
        if (settings == null) {
            settings = new MCEFSettings();
            try {
                settings.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return settings;
    }

    /**
     * This gets called by {@link com.cinemamod.mcef.mixins.CefInitMixin}
     * There is no need to call this from your project.
     */
    public static boolean initialize() {
        if (CefUtil.init()) {
            app = new MCEFApp(CefUtil.getCefApp());
            client = new MCEFClient(CefUtil.getCefClient());
            MCEF.getApp().getHandle().registerSchemeHandlerFactory(
                    "mod", "",
                    (browser, frame, url, request) -> {
                        return new ModScheme(request.getURL());
                    }
            );
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
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent);
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }

    public static MCEFBrowser createBrowser(String url, boolean transparent, int width, int height) {
        assertInitialized();
        MCEFBrowser browser = new MCEFBrowser(client, url, transparent);
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
