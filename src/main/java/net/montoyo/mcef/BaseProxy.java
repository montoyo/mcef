package net.montoyo.mcef;

import net.montoyo.mcef.api.*;
import net.montoyo.mcef.utilities.Log;

public class BaseProxy implements API {

    public void onPreInit() {
    }
    
    public void onInit() {
        Log.info("MCEF is running on server. Nothing to do.");
    }

    @Override
    public IBrowser createBrowser(String url, boolean transparent) {
        Log.warning("A mod called API.createBrowser() from server! Returning null...");
        return null;
    }

    @Override
    public IBrowser createBrowser(String url) {
        return createBrowser(url, false);
    }

    @Override
    public void registerDisplayHandler(IDisplayHandler idh) {
        Log.warning("A mod called API.registerDisplayHandler() from server!");
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public void openExampleBrowser(String url) {
        Log.warning("A mod called API.openExampleBrowser() from server! URL: %s", url);
    }

    @Override
    public void registerJSQueryHandler(IJSQueryHandler iqh) {
        Log.warning("A mod called API.registerJSQueryHandler() from server!");
    }

    @Override
    public String mimeTypeFromExtension(String ext) {
        Log.warning("A mod called API.mimeTypeFromExtension() from server!");
        return null;
    }

    @Override
    public void registerScheme(String name, Class<? extends IScheme> schemeClass, boolean std, boolean local, boolean displayIsolated, boolean secure, boolean corsEnabled, boolean cspBypassing, boolean fetchEnabled) {
        Log.warning("A mod called API.registerScheme() from server!");
    }

    @Override
    public boolean isSchemeRegistered(String name) {
        Log.warning("A mod called API.isSchemeRegistered() from server!");
        return false;
    }

    public void onShutdown() {
    }

    private static final int PUNYCODE_TMIN = 1;
    private static final int PUNYCODE_TMAX = 26;
    private static final int PUNYCODE_SKEW = 38;
    private static final int PUNYCODE_DAMP = 700;
    private static final int PUNYCODE_INITIAL_BIAS = 72;
    private static final int PUNYCODE_INITIAL_N = 128;

    private static int punycodeBiasAdapt(int delta, int numPoints, boolean firstTime) {
        if(firstTime)
            delta /= PUNYCODE_DAMP;
        else
            delta /= 2;

        int k = 0;
        delta = delta + delta / numPoints;

        while(delta > ((36 - PUNYCODE_TMIN) * PUNYCODE_TMAX) / 2) {
            delta /= 36 - PUNYCODE_TMIN;
            k += 36;
        }

        return k + ((36 - PUNYCODE_TMIN + 1) * delta) / (delta + PUNYCODE_SKEW);
    }

    private static void punycodeEncodeNumber(StringBuilder dst, int q, int bias) {
        boolean keepGoing = true;

        for(int k = 36; keepGoing; k += 36) {
            //Compute & clamp threshold
            int t = k - bias;
            if(t < PUNYCODE_TMIN)
                t = PUNYCODE_TMIN;
            else if(t > PUNYCODE_TMAX)
                t = PUNYCODE_TMAX;

            //Compute digit
            int digit;
            if(q < t) {
                digit = q;
                keepGoing = false;
            } else {
                digit = t + (q - t) % (36 - t);
                q = (q - t) / (36 - t);
            }

            //Encode digit
            if(digit < 26)
                dst.append((char) ('a' + digit));
            else
                dst.append((char) ('0' + digit - 26));
        }
    }

    private static String punycodeEncodeString(int[] input) {
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < input.length; i++) {
            if(input[i] < 128)
                output.append((char) input[i]);
        }

        int n = PUNYCODE_INITIAL_N;
        int delta = 0;
        int bias = PUNYCODE_INITIAL_BIAS;
        int h = output.length();
        int b = h;

        if(b > 0)
            output.append('-');

        while(h < input.length) {
            int m = Integer.MAX_VALUE;
            for(int i = 0; i < input.length; i++) {
                if(input[i] >= n && input[i] < m)
                    m = input[i];
            }

            delta = delta + (m - n) * (h + 1);
            n = m;

            for(int i = 0; i < input.length; i++) {
                int c = input[i];

                if(c < n)
                    delta++;

                if(c == n) {
                    punycodeEncodeNumber(output, delta, bias);
                    bias = punycodeBiasAdapt(delta, h + 1, h == b);
                    delta = 0;
                    h++;
                }
            }

            delta++;
            n++;
        }

        return "xn--" + output.toString();
    }

    @Override
    public String punycode(String url) {
        int protoEnd = url.indexOf("://");

        if(protoEnd < 0)
            protoEnd = 0;
        else
            protoEnd += 3;

        int hostEnd = url.indexOf('/', protoEnd);
        if(hostEnd < 0)
            hostEnd = url.length();

        String hostname = url.substring(protoEnd, hostEnd);
        boolean doTransform = false;

        for(int i = 0; i < hostname.length(); i++) {
            if(hostname.charAt(i) >= 128) {
                doTransform = true;
                break;
            }
        }

        if(!doTransform)
            return url;

        String[] parts = hostname.split("\\.");
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append(url, 0, protoEnd);

        for(String p: parts) {
            doTransform = false;

            for(int i = 0; i < p.length(); i++) {
                if(p.charAt(i) >= 128) {
                    doTransform = true;
                    break;
                }
            }

            if(first)
                first = false;
            else
                sb.append('.');

            if(doTransform)
                sb.append(punycodeEncodeString(p.codePoints().toArray()));
            else
                sb.append(p);
        }

        sb.append(url, hostEnd, url.length());
        return sb.toString();
    }

}
