package net.montoyo.mcef.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An object representing an HTTP(S) mirror to download the resources from.
 *
 * @author montoyo
 * @see {@link net.montoyo.mcef.remote.MirrorManager}
 */
public final class Mirror {

    /**
     * Whether the mirror is HTTPS or not
     */
    public static final int FLAG_SECURE = 1;

    /**
     * Whether the embedded Let's Encrypt certificate should be used to establish a secure connection to this host
     */
    public static final int FLAG_LETSENCRYPT = 2;

    /**
     * Whether this mirror has been forced by the user in the MCEF configuration file
     */
    public static final int FLAG_FORCED = 4;

    private final String name;
    private final String url;
    private final int flags;

    /**
     * Constructs a Mirror from its name, URL, and flags.
     *
     * @param name The name of the mirror
     * @param url The corresponding URL
     * @param flags Its flags
     */
    public Mirror(String name, String url, int flags) {
        this.name = name;
        this.url = url;
        this.flags = flags;
    }

    /**
     * @return The name of the mirror
     */
    public String getName() {
        return name;
    }

    /**
     * @return The URL of the mirror
     */
    public String getURL() {
        return url;
    }

    /**
     * @return The flags of this mirror
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return Whether the secure flag is set
     * @see #FLAG_SECURE
     */
    public boolean isSecure() {
        return (flags & FLAG_SECURE) != 0;
    }

    /**
     * @return Whether the Let's Encrypt flag is set
     * @see #FLAG_LETSENCRYPT
     */
    public boolean usesLetsEncryptCertificate() {
        return (flags & FLAG_LETSENCRYPT) != 0;
    }

    /**
     * @return Whether this mirror has been forced by the user
     * @see #FLAG_FORCED
     */
    public boolean isForced() {
        return (flags & FLAG_FORCED) != 0;
    }

    /**
     * @return A string informing the user of which mirror was selected
     */
    public String getInformationString() {
        return isForced() ? ("Mirror location forced by user to: " + url) : ("Selected mirror: " + name);
    }

    /**
     * Opens a connection to the mirror's resource corresponding to the URL.
     *
     * @param name The URL of the resource, relative to the root of the mirror website.
     * @return A connection to this resource, with timeout set up.
     * @throws MalformedURLException if the mirror's URL is invalid or if name is invalid.
     * @throws IOException if an I/O exception occurs.
     */
    public HttpURLConnection getResource(String name) throws MalformedURLException, IOException {
        HttpURLConnection ret = (HttpURLConnection) (new URL(url + '/' + name)).openConnection();
        ret.setConnectTimeout(30000);
        ret.setReadTimeout(15000);
        ret.setRequestProperty("User-Agent", "MCEF");

        return ret;
    }

}
