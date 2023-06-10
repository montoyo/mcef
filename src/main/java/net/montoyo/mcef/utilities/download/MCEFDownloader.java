package net.montoyo.mcef.utilities.download;

import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.ui.FileUI;
import net.montoyo.mcef.MCEF;
import net.montoyo.mcef.client.init.CefInitMenu;
import net.montoyo.mcef.utilities.IProgressListener;
import net.montoyo.mcef.utilities.Resource;
import net.montoyo.mcef.utilities.Util;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MCEFDownloader {

    private Properties versions;
    private final IProgressListener listener;

    public MCEFDownloader(IProgressListener listener) {
        this.listener = listener;
    }

    private Map<String, String> fetchFileManifest(String url) throws IOException {
        FileOutputStream outputStream = null;
        if (MCEF.writeMirrorData) {
            String path = url.substring("https://cinemamod-libraries.ewr1.vultrobjects.com/".length());
            File dst = new File("data/" + path);
            if (!dst.exists()) dst.getParentFile().mkdirs();
            outputStream = new FileOutputStream(dst);
        }
        
        // sha1sum, filename
        Map<String, String> manifest = new HashMap<>();
        try (InputStream inputStream = new URL(url).openStream()) {
            try (Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    
                    if (outputStream != null) {
                        outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                    
                    String sha1hash = line.split(" ")[0];
                    String filePath = line.split(" ")[2].substring(1); // substring to remove the leading "."
                    manifest.put(sha1hash, filePath);
                }
            }
        }
        
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
        
        return manifest;
    }

    private void fetchVersions() throws IOException {
        versions = new Properties();
    
        FileOutputStream outputStream = null;
        if (MCEF.writeMirrorData) {
            File dst = new File("data/versions.txt");
            if (!dst.exists()) dst.getParentFile().mkdirs();
            outputStream = new FileOutputStream(dst);
        }
        
        URL versionsURL = new URL(Resource.CINEMAMOD_VERSIONS_URL);
        try (InputStream inputStream = versionsURL.openStream()) {
            try (Scanner scanner = new Scanner(inputStream)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (outputStream != null)
                        outputStream.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    
                    try {
                        String library = line.split(" ")[0];
                        String version = line.split(" ")[1];
                        versions.put(library, version);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }
        }
        
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
    }

    // checks if the file at filePath is on disk and the hash matches
    private boolean ensureLibFile(String sha1hash, String relPath) {
        Path librariesPath = Paths.get(System.getProperty("cinemamod.libraries.path"));
        File libFile = new File(librariesPath + relPath);

        boolean result = false;

        if (libFile.exists()) {
            // check hash of existing file on disk
            try {
                String onDiskHash = Util.sha1Hash(libFile);

                if (sha1hash.equals(onDiskHash)) {
                    // file is good
                    result = true;
                } else {
                    System.out.println(libFile + " hash mismatch, will update");
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
    
    String osProp = System.getProperty("os.name");

    private void downloadLibFile(String remotePath, String relPath) throws IOException {
        Path librariesPath = Paths.get(System.getProperty("cinemamod.libraries.path"));
        listener.onTaskChanged("3:Downloading " + remotePath);
        File localFile = new File(librariesPath + relPath);
        FileUtils.copyURLToFile(new URL(remotePath), localFile);
        
        if (MCEF.writeMirrorData)
            FileUtils.copyFile(localFile, new File("data/" + remotePath.substring("https://cinemamod-libraries.ewr1.vultrobjects.com/".length())));

        // set appropriate files as executable on linux
        if (osProp.toLowerCase(Locale.ROOT).contains("linux")) {
            if (localFile.toString().contains("chrome-sandbox") || localFile.toString().contains("jcef_helper")) {
                Util.makeExecNix(localFile);
            }
        }
    }

    private void patchLibFile(String relPath) throws CompressorException, IOException, InvalidHeaderException {
        Path librariesPath = Paths.get(System.getProperty("cinemamod.libraries.path"));
        listener.onTaskChanged("3:Patching " + relPath);
        File libFile = new File(librariesPath + relPath);
        File patchFile = new File(libFile + ".diff");
        FileUI.patch(libFile, libFile, patchFile);
    }

    private void ensureJcef(String cefBranch, String platform) throws IOException {
        // manifest of the unpatched JCEF files
        String jcefManifestUrlString = Resource.getJcefUrl(cefBranch, platform) + "/manifest.txt";
        // manifest of the patched JCEF files
        String jcefPatchedManifestUrlString = Resource.getJcefPatchesUrl(cefBranch, platform) + "/patched-manifest.txt";
        // manifest of the .diff JCEF patch files
        String jcefPatchesManifestUrlString = Resource.getJcefPatchesUrl(cefBranch, platform) + "/manifest.txt";
    
        Map<String, String> jcefManifest;
        Map<String, String> jcefPatchedManifest;
        Map<String, String> patchesManifest;
        if (!MCEF.skipVersionCheck) {
            jcefManifest = fetchFileManifest(jcefManifestUrlString);
            jcefPatchedManifest = fetchFileManifest(jcefPatchedManifestUrlString);
            patchesManifest = fetchFileManifest(jcefPatchesManifestUrlString);
        } else {
            jcefManifest = new HashMap<>();
            jcefPatchedManifest = new HashMap<>();
            patchesManifest = new HashMap<>();
        }

        int fileCount = 0;
        for (Map.Entry<String, String> entry : jcefPatchedManifest.entrySet()) {
            fileCount++;

            double value = ((fileCount / (double) jcefPatchedManifest.size()));

            listener.onProgressed(value);

            String sha1hash = entry.getKey();
            String filePath = entry.getValue();

            listener.onTaskChanged("2:Found " + filePath.substring(1)); // substring to remove leading "/"

            if (!ensureLibFile(sha1hash, filePath)) {
                // Download the unpatched JCEF library file
                String remotePath = Resource.getJcefUrl(cefBranch, platform) + filePath;
                downloadLibFile(remotePath, filePath);

                // Check if the file has a patch
                for (String patchFileName : patchesManifest.values()) {
                    if (patchFileName.startsWith(filePath)) {
                        // Download the patch .diff file
                        String patchRemotePath = Resource.getJcefPatchesUrl(cefBranch, platform) + patchFileName;
                        downloadLibFile(patchRemotePath, patchFileName);

                        // Patch the JCEF file
                        try {
                            patchLibFile(filePath);
                        } catch (CompressorException | InvalidHeaderException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void run() {
        if (MCEF.FAVOR_GIT) {
            if (ModernDownload.download(listener)) {
                MCEF.downloadedFromGit = true;
                return;
            }
        }
        
        listener.onTaskChanged("1:Fetching mod version info...");

        try {
            if (!MCEF.skipVersionCheck) {
                fetchVersions();
            } else {
                versions = new Properties();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("MCEF library versions " + versions.toString());

        listener.onTaskChanged("2:Current MCEF branch: " + versions.getProperty("jcef"));

        final String platform;
    
        String os = osProp.toLowerCase(Locale.ROOT);
    
        if (os.contains("win")) {
            platform = "win64";
        } else if (os.contains("mac")) {
            platform = "mac64";
        } else if (os.contains("linux")) {
            platform = "linux64";
        } else {
            platform = "unknown";
        }

        listener.onTaskChanged("3:Verifying library files...");

        try {
            ensureJcef(versions.getProperty("jcef"), platform);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (System.getProperty("cinemamod.libraries.path") == null) {
            System.out.println("Not running inside Minecraft");
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        MCEFDownloader downloader = new MCEFDownloader(CefInitMenu.listener);
        downloader.run(); // already runs on another thread; doesn't need a third one, especially not one that it's just gonna join to immediately after
        downloader.listener.onProgressEnd();
    }
}