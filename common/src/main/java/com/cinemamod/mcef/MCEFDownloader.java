package com.cinemamod.mcef;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

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

    public void downloadJavaCefBuild(Consumer<Float> percentCompleteConsumer) throws IOException {
        File mcefLibrariesPath = new File(System.getProperty("mcef.libraries.path"));
        downloadFile(getJavaCefDownloadUrl(), new File(mcefLibrariesPath, platform.getNormalizedName() + ".tar.gz"), percentCompleteConsumer);
    }

    /**
     * @return  true if the jcef build checksum file matches the remote checksum file (for the {@link MCEFDownloader#javaCefCommitHash}),
     *          false if the jcef build checksum file did not exist or did not match; this means we should redownload JCEF
     * @throws IOException
     */
    public boolean downloadJavaCefChecksum() throws IOException {
        File mcefLibrariesPath = new File(System.getProperty("mcef.libraries.path"));
        File jcefBuildHashFileTemp = new File(mcefLibrariesPath, platform.getNormalizedName() + ".tar.gz.sha256.temp");
        File jcefBuildHashFile = new File(mcefLibrariesPath, platform.getNormalizedName() + ".tar.gz.sha256");

        downloadFile(getJavaCefChecksumDownloadUrl(), jcefBuildHashFileTemp, percentComplete -> {});

        if (jcefBuildHashFile.exists()) {
            boolean sameContent = Files.mismatch(jcefBuildHashFile.toPath(), jcefBuildHashFile.toPath()) == -1;
            if (sameContent) {
                System.out.println("Checksums match");
                jcefBuildHashFileTemp.delete();
                return true;
            }
        }

        jcefBuildHashFileTemp.renameTo(jcefBuildHashFile);

        return false;
    }

    public void extractJavaCefBuild(boolean delete, Consumer<Float> percentCompleteConsumer) {
        File mcefLibrariesPath = new File(System.getProperty("mcef.libraries.path"));
        File tarGzArchive = new File(mcefLibrariesPath, platform.getNormalizedName() + ".tar.gz");
        extractTarGz(tarGzArchive, mcefLibrariesPath, percentCompleteConsumer);
        if (delete) {
            if (tarGzArchive.exists()) {
                tarGzArchive.delete();
            }
        }
    }

    private static void downloadFile(String urlString, File outputFile, Consumer<Float> percentCompleteConsumer) throws IOException {
        System.out.println(urlString + " -> " + outputFile.getCanonicalPath());

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

    private static void extractTarGz(File tarGzFile, File outputDirectory, Consumer<Float> percentCompleteConsumer) {
        outputDirectory.mkdirs();

        long fileSize = tarGzFile.length();
        long totalBytesRead = 0;

        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(tarGzFile)))) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File outputFile = new File(outputDirectory, entry.getName());
                outputFile.getParentFile().mkdirs();

                try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = tarInput.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        float percentComplete = (((float) totalBytesRead / fileSize) / 2.6158204f); // Roughly the compression ratio
                        percentCompleteConsumer.accept(percentComplete);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        percentCompleteConsumer.accept(1.0f);
    }
}
