package com.cinemamod.mcef;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Consumer;

public class MCEFDownloader {
    private static final String JAVA_CEF_DOWNLOAD_URL = "https://mcef-download.cinemamod.com/java-cef-builds/${java-cef-commit}/${platform}.tar.gz";
    private static final String JAVA_CEF_CHECKSUM_DOWNLOAD_URL = "https://mcef-download.cinemamod.com/java-cef-builds/${java-cef-commit}/${platform}.tar.gz.sha256";

    private final String javaCefCommitHash;
    private final MCEFPlatform platform;

    public MCEFDownloader(String javaCefCommitHash, MCEFPlatform platform) {
        this.javaCefCommitHash = javaCefCommitHash;
        this.platform = platform;
    }

    public String getJavaCefDownloadUrl() {
        return formatURL(JAVA_CEF_DOWNLOAD_URL);
    }

    public String getJavaCefChecksumDownloadUrl() {
        return formatURL(JAVA_CEF_CHECKSUM_DOWNLOAD_URL);
    }

    private String formatURL(String url) {
        return url
                .replace("${java-cef-commit}", javaCefCommitHash)
                .replace("${platform}", platform.getNormalizedName());
    }

    public void downloadJavaCefBuild() throws IOException {
        downloadFile(getJavaCefDownloadUrl(), new File(System.getProperty("mcef.libraries.path"), platform.getNormalizedName() + ".tar.gz"), System.out::println);
    }

    public void downloadJavaCefChecksum() throws IOException {
        downloadFile(getJavaCefChecksumDownloadUrl(), new File(System.getProperty("mcef.libraries.path"), platform.getNormalizedName() + ".tar.gz.sha256"), System.out::println);
    }

    private static void downloadFile(String urlString, File outputFile, Consumer<Float> percentCompleteConsumer) throws IOException {
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        int fileSize = urlConnection.getContentLength();

        BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        byte[] buffer = new byte[1024];
        int count;
        int readBytes = 0;
        while((count = inputStream.read(buffer, 0, 1024)) != -1)
        {
            outputStream.write(buffer, 0, count);
            readBytes += count;
            float percentComplete = (float) readBytes / fileSize;
            percentCompleteConsumer.accept(percentComplete);
        }

        inputStream.close();
        outputStream.close();
    }
}
