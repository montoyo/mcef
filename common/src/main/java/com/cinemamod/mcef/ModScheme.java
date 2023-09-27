package com.cinemamod.mcef;

import java.io.IOException;
import java.io.InputStream;

import com.mojang.logging.LogUtils;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

// https://github.com/CinemaMod/mcef/blob/master-1.19.2/src/main/java/net/montoyo/mcef/example/ModScheme.java
public class ModScheme implements CefResourceHandler {
    private String contentType = null;
    private InputStream is = null;

    String url;

    public ModScheme(String url) {
        this.url = url;
    }

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback cefCallback) {
        String url = this.url.substring("mod://".length());

        int pos = url.indexOf('/');
        if (pos < 0) {
            cefCallback.cancel();
            return false;
        }

        String mod = removeSlashes(url.substring(0, pos));
        String loc = removeSlashes(url.substring(pos + 1));

        if (mod.length() <= 0 || loc.length() <= 0 || mod.charAt(0) == '.' || loc.charAt(0) == '.') {
            LOGGER.warn("Invalid URL " + url);
            cefCallback.cancel();
            return false;
        }

        // TODO: this may or may not require forge/fabric specific code?
//        is = ModList.get().getModContainerById(mod).get().getMod().getClass().getResourceAsStream("/assets/" + mod.toLowerCase() + "/html/" + loc.toLowerCase());
        is = ModScheme.class.getClassLoader().getResourceAsStream("/assets/" + mod.toLowerCase() + "/html/" + loc.toLowerCase());
        if (is == null) {
            LOGGER.warn("Resource " + url + " NOT found!");
            cefCallback.cancel();
            return false; // TODO: 404?
        }

        contentType = null;
        pos = loc.lastIndexOf('.');
        if (pos >= 0 && pos < loc.length() - 2)
            contentType = CefUtil.mimeFromExtension(loc.substring(pos + 1));

        cefCallback.Continue();
        return true;
    }

    private String removeSlashes(String loc) {
        int i = 0;
        while (i < loc.length() && loc.charAt(i) == '/')
            i++;

        return loc.substring(i);
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef contentLength, StringRef redir) {
        if (contentType != null)
            cefResponse.setMimeType(contentType);

        cefResponse.setStatus(200);
        cefResponse.setStatusText("OK");
        contentLength.set(0);
    }

    @Override
    public boolean readResponse(byte[] output, int bytesToRead, IntRef bytesRead, CefCallback cefCallback) {
        try {
            int ret = is.read(output, 0, bytesToRead);
            if (ret <= 0) {
                is.close();
                // 0 bytes read indicates to CEF/JCEF that there is no more data to read
                bytesRead.set(0);
                return false;
            }

            // tell CEF/JCEF how many bytes were read
            bytesRead.set(ret);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            // attempt to close the stream if possible
            try {
                is.close();
            } catch (Throwable ignored) {
            }

            return false;
        }
    }

    @Override
    public void cancel() {
        // attempt to free resources, just incase
        try {
            is.close();
        } catch (Throwable ignored) {
        }
    }
}
